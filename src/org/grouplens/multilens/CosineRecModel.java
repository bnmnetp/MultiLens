package org.grouplens.multilens;


import java.util.Date;
import java.util.Vector;
import java.util.Iterator;
import java.io.*;

public class CosineRecModel extends GenericModel implements ItemModel {

    public CosineRecModel() {
        super();
    }

    public CosineRecModel(boolean read) {
        if(read) {
            readModel(300,false);
        }
    }
    public CosineRecModel(int i, int j) {
        super(i,j);
    }

    /**
     * Construct a CosineRecModel from buildModel.
     * @param buildModel  The CosineModel from which to copy innards
     */
    public CosineRecModel(CosineModel buildModel) {
        super(buildModel.getNumRows(), buildModel.getColMax());

        // Walk the matrix and get the cells
        Iterator rmit = buildModel.rowIterator();
        while( rmit.hasNext() ) {
            SparseModelRow r1 = (SparseModelRow)rmit.next();
            SparseModelRow newRow = getRowOrCreate(r1.getKey());
            // Note SparseModelRows support both iterator() which returns
            // the next id or cellIterator() which returns the actual cell.
            Iterator it = r1.iterator();
            while ( it.hasNext() ) {
                int key = ((SparseModelRow.SparseRowIterator)it).inext();
                float sim = ((CosineModelCell)r1.getCell(key)).getSimilarity();
                newRow.insertCell(key,new CosineRecCell(sim));
            }
        }
    }

    public void walkModel(String fname) {
        int numCells = 0;
        Iterator rmit = rowIterator();
        while( rmit.hasNext() ) {
            SparseModelRow r1 = (SparseModelRow)rmit.next();
            // Note SparseModelRows support both iterator() which returns the next id or
            // cellIterator() which returns the actual cell.
            Iterator it = r1.iterator();
            int rowId = r1.getKey();
            while ( it.hasNext() ) {
                int colId = ((SparseModelRow.SparseRowIterator)it).inext();
                float sim = ((CosineRecCell)r1.getCell(colId)).getSimilarity();
                numCells++;
            }
        }
        System.err.println("visited " + numCells + " cells");
    }

    /**
     * Write a model out in a binary file format.
     * The format of the file is a sequence of triples
     * 
     * rowid colid simValue
     * 
     * where rowid is an int, colid an int, and simValue a float.
     * 
     * @param fname   Filename to which to write.
     */
    public void writeBinaryModel(String fname) throws IOException {
        DataOutputStream modelOut = null;
        int numCells = 0;

        FileOutputStream fos = new FileOutputStream(fname);
        BufferedOutputStream bos = new BufferedOutputStream(fos,1024);
        modelOut = new DataOutputStream(bos);

        Iterator rmit = rowIterator();
        while( rmit.hasNext() ) {
            SparseModelRow r1 = (SparseModelRow)rmit.next();
            // Note SparseModelRows support both iterator() which returns the next id or
            // cellIterator() which returns the actual cell.
            Iterator it = r1.iterator();
            int rowId = r1.getKey();
            while ( it.hasNext() ) {
                int colId = ((SparseModelRow.SparseRowIterator)it).inext();
                float sim = ((CosineRecCell)r1.getCell(colId)).getSimilarity();
                modelOut.writeInt(rowId);
                modelOut.writeInt(colId);
                modelOut.writeFloat(sim);
                numCells++;
            }
        }
        if (showDebug) {
            System.err.println("Wrote " + numCells + " cells");
        }
        modelOut.close();
    }

    /**
     * Delete rows from lastRowId+1 .. newRow.getKey()-1, then
     * replaceRowOrCreate newRow.
     * 
     * @param lastRowId
     * @param newRow
     */
    private void localInsertRow(int lastRowId, SparseModelRow newRow) {
        int rowId = newRow.getKey();
        assert(lastRowId < rowId);

        // Delete any rows which no longer exist-- but leave the last
        // row put in!
        deleteRows(lastRowId+1, rowId-1);

        // Put in the row we've already read
        try {
            replaceRowOrCreate(newRow);
        } catch (CloneNotSupportedException e) {
            // Not sure what to do with this
            // I don't expect it to happen
            e.printStackTrace();
        }
    }

    private void writeDiffLine(PrintStream pw, int num, long t1) {
        if (showDebug) {
            long t2 = new Date().getTime();
            long diff = (t2 - t1)/1000;
            if (0 == diff) {
                // Avoid divide by zero
                diff = 1;
            }
            long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
            long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;
            long usedMemory = totalMemory - freeMemory;
    
            pw.println(
                new Date() + ": Read "
                    + num
                    + " cells, "
                    + diff
                    + " seconds ("
                    + (num / diff)
                    + " cells/s), "
                    + " total memory "
                    + totalMemory
                    + "M  free memory "
                    + freeMemory
                    + "M  used memory "
                    + usedMemory
                    + "M");
        }
    }

    /**
     * Read the binary format written by writeBinaryModel().
     * 
     * @param fname  Filename from which to read.
     */
    public void readBinaryModel(String fname) throws IOException {
        if (null == fname) {
            throw new IllegalArgumentException("CosineRecModel.readBinaryModel: filename passed in was null");
        }
        DataInputStream modelIn = null;
        int numCells = 0;

        FileInputStream fis = new FileInputStream(fname);
        BufferedInputStream bis = new BufferedInputStream(fis,1024);
        modelIn = new DataInputStream(bis);

        int rowId = -1, colId = -1;
        float sim;

        long t1 = new Date().getTime();
        boolean inOrder = rowsInOrder(fname, showDebug);
        if (showDebug) {   
            System.err.println((new Date().getTime() - t1)/1000 + " seconds to check order");
        }

        long t2 = new Date().getTime();
        if (inOrder) {
            // Read the model row-by-row, even if it is currently full
            // and being used.  This can save memory.

            int lastRowId = -1;
            SparseModelRow newRow = new SparseModelRow(getColMax(), -1);
            while(modelIn.available() > 0) {
                rowId = modelIn.readInt();
                colId = modelIn.readInt();
                sim = modelIn.readFloat();

                if (showDebug && (0 == (++numCells % 1000000))) {
                    writeDiffLine(System.err, numCells, t2);
                }

                if (rowId != newRow.getKey()) {
                    // We have encountered the beginning of a new row

                    if (newRow.getKey() > -1) {
                        // There is a row waiting to be put in
                        localInsertRow(lastRowId, newRow);
                    }

                    // Clear out the row for more stuff
                    lastRowId = newRow.getKey();
                    newRow = new SparseModelRow(getColMax(), rowId);
                }

                CosineRecCell myCell = new CosineRecCell(sim);
                try {
                    //System.out.println("CosineRecModel.insertNewCell before insertCell: row key " + r.getKey() + " col key " + j + " cell " + myCell);
                    newRow.insertCell(colId,myCell);
                } catch (Exception e) {
                    System.err.println("Error: Failed to insert cell row key " + rowId + " col key "+ colId);
                    e.printStackTrace();  //To change body of catch statement use Options | File Templates.
                }
            }

            // Last row
            if (newRow.getKey() > -1) {
                localInsertRow(lastRowId, newRow);
            }

            // Clear out any excess past the last row
            if (getMaxKey() > newRow.getKey()) {
                deleteRows(newRow.getKey()+1, getMaxKey());
            }
        }
        else {
            // Read the model all at once

            // Clear out old stuff
            clear();
            //System.out.println("CosineRecModel.readBinaryModel: stats after clear: " + getStats());
    
            while(modelIn.available() > 0) {
                rowId = modelIn.readInt();
                colId = modelIn.readInt();
                sim = modelIn.readFloat();
    
                insertNewCell(rowId,colId,sim);
    
    /*
                if (numCells < 50) {
                    System.out.println("Read in " + rowId + " " + colId +  " " + sim);
    //                printModel(System.out);
                }
    */
    
                if (showDebug && (0 == (++numCells % 1000000))) {
                    writeDiffLine(System.err, numCells, t2);
                }
            }
        }
        if (showDebug) {
            writeDiffLine(System.err, numCells, t2);
        }

        assert(numCells == cardinality());

        //Done reading, close the file.
        modelIn.close();
    }

    /**
     * Check if the file contains cells in such an order that the
     *   row # is always nondecreasing.
     * The file format is the same as that for writeBinaryModel().
     * 
     * @param fname       File to check
     * @param showDebug   Print some info to System.out if true
     * @return true iff the file contains cells in such an order that the
     *   row # is always nondecreasing.
     */
    static public boolean rowsInOrder(String fname, boolean showDebug)
        throws FileNotFoundException, IOException {
        if (null == fname) {
            throw new IllegalArgumentException("CosineRecModel.readBinaryModel: filename passed in was null");
        }
        DataInputStream modelIn = null;
        int numCells = 0;

        FileInputStream fis = new FileInputStream(fname);
        BufferedInputStream bis = new BufferedInputStream(fis,1024);
        modelIn = new DataInputStream(bis);

        if (showDebug) {
            System.out.println("Checking if rows are in order in " + fname);
        }

        int rowId = -1;
        int lastRowId = -1;
        while(modelIn.available() > 0) {
            rowId = modelIn.readInt();
            modelIn.readInt();  // colId
            modelIn.readFloat();  // sim
            
            if (rowId < lastRowId) {
                if (showDebug) {
                    System.out.println(fname + " NOT in row order");
                }
                return false;
            } 
            lastRowId = rowId;
        }
        if (showDebug) {
            System.out.println(fname + " in row order");
        }
        return true;
    }

    public float getCosineSim(int i, int j) {
        int rowIx;
        float retval = (float)0.0;
        SparseModelRow r = getModelRow(i);
        if (null != r) {
            CosineRecCell myCell = (CosineRecCell)r.getCell(j);
            if (myCell != null) {
                retval = myCell.getSimilarity();
            }
        }
        return retval;
    }

    public void setCosineSim(int i, int j, float sim) {
        SparseModelRow r = getRowOrCreate(i);

        CosineRecCell myCell = (CosineRecCell)r.getCell(j);
        if (myCell != null) {
            myCell.setSimilarity(sim);
        } else {
            myCell = new CosineRecCell(sim);
            r.insertCell(j,myCell);
        }

    }

    /**
     * Return an existing cell with given row and column, or if one doesn't exist,
     * create it. 
     * 
     * @param i    Row key
     * @param j    Column key
     * @return CosineRecCell
     */
    public CosineRecCell getCellOrCreate(int i, int j) {
        SparseModelRow r = getRowOrCreate(i);
        CosineRecCell myCell = (CosineRecCell)r.getCell(j);
        if (null == myCell) {
            myCell = new CosineRecCell();
            r.insertCell(j, myCell);
        }

        return myCell;
    }

    /**
     * Insert a new cell.
     * 
     * @param i    Row key
     * @param j    Column key
     * @param sim  Similarity to insert into the cell.
     */
    public void insertNewCell(int i, int j, float sim) {
        //System.out.println("CosineRecModel.insertNewCell("+ i +","+j+","+sim+")");
        SparseModelRow r = getRowOrCreate(i);
        //System.out.println("CosineRecModel.insertNewCell after getRowOrCreate: i " + i+ " row key " + r.getKey());
        assert(r.getKey() == i);

        CosineRecCell myCell = new CosineRecCell(sim);
        try {
            //System.out.println("CosineRecModel.insertNewCell before insertCell: row key " + r.getKey() + " col key " + j + " cell " + myCell);
            r.insertCell(j,myCell);
        } catch (Exception e) {
            System.err.println("Error: Failed to insert cell row key " + i + " col key "+j);
            e.printStackTrace();  //To change body of catch statement use Options | File Templates.
        }
    }

    public int getDBRepSimScore(int i, int j) {
        return (int)(getCosineSim(i,j) * 1000.0);
    }

    public void setDBRepSimScore(int i, int j, int val) {
        insertNewCell(i, j, (float)val/(float)1000.0);
//        ((SparseCosineRecModel)myModelRow).setCosineSim(i, j, (float)val/1000.0);

    }

    public void addDBRepSimScore(int i, int j, int val, CellCombiner myOp) {
        float current = getCosineSim(i,j);
        if (current > 0.0) {
            current = myOp.combine(current, (float)val/(float)1000.0);
            setCosineSim(i,j,current);
        } else {
            current = (float)(val/1000.0);
            insertNewCell(i,j,current);
        }
        //setCosineSim(i,j,current);
    }

    public void insertUserItems(Vector u) {
        throw new UnsupportedOperationException("No support for inserting new items");
    }

    public float getSim(int i, int j) {
        return getCosineSim(i,j);
    }

    /**
     * Read in a binary model, and show things about it.
     * @param args
     */
    public static void main(String args[]) throws IOException {
        boolean verbose = false;
        String fname = null;
        int arg = 0;

        if ((args.length < 1) || (args.length > 2)) {
            System.err.println("Usage: java org.grouplens.multilens.CosineModel [-verbose] modelfile");
        }
        else {
            if (args[arg].equals("-verbose")) {
                verbose = true;
                arg++;
            }
            fname = args[arg++];
        }


        if (verbose) {
            // Print the entries verbatim as they are in the file
            if (null == fname) {
                throw new IllegalArgumentException("CosineRecModel.readBinaryModel: filename passed in was null");
            }
            FileInputStream fis = new FileInputStream(fname);
            BufferedInputStream bis = new BufferedInputStream(fis,1024);
            DataInputStream modelIn = new DataInputStream(bis);
            while(modelIn.available() > 0) {
                System.out.println(
                    modelIn.readInt()             // rowId
                    + " " + modelIn.readInt()     // colId
                    + " " + modelIn.readFloat()   // sim
                    );
            }
            modelIn.close();
        }
        else {
            CosineRecModel aModel = new CosineRecModel(6000,6000);
            aModel.setShowDebug(true);
            aModel.readBinaryModel(fname);
            aModel.setShowDebug(false);

            // Print out stats
            System.out.println(aModel.getStats().toString());
            // Print out row lengths
            System.out.println("");
            SparseModelIterator iter = aModel.rowIterator();
            while (iter.hasNext()) {
                SparseModelRow r1 = (SparseModelRow)iter.next();

                System.out.println("Row " + r1.getKey() + " length " + r1.cardinality());
            }
        }
    }
}
