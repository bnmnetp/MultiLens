package org.grouplens.multilens;


import java.util.Date;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Iterator;
import java.util.Arrays;
import java.io.*;

public class CosineModel extends GenericModel implements ItemModel {
    private boolean finalCalled = false;
    private boolean buildUpper = false;

    public CosineModel(boolean read) {
        if(read) {
            readModel(200,false);
        }
    }

    public  CosineModel() {
    }

    public CosineModel(int i, int j) {
        super(i,j);
    }

    public boolean getBuildUpperOnly() {
        return buildUpper;
    }

    /**
     * Have model build only build the upper triangle of the similarity matrix.
     * This is useful when you want to save time and memory in your build
     * process and you are going to write out the model to a file or the
     * database for later reloading.  It is NOT recommended to use an upper
     * triangular model for recommendations.
     * 
     * @param bu value to which to set buildUpper
     */
    public void setBuildUpperOnly(boolean bu) {
        buildUpper = bu;
    }

    /**
     * Get the model cell for given row and column.
     * If it doesn't exist, create it.
     * 
     * @param row   row to get
     * @param col   column to get
     * @return CosineModelCell
     */
    public CosineModelCell getModelCell(int row, int col) {
        SparseModelRow r = getRowOrCreate(row);
        CosineModelCell myCell = (CosineModelCell)r.getCell(col);
        if (myCell == null) {
            myCell = new CosineModelCell();
            r.insertCell(col,myCell);
        }
        return myCell;
    }

    public void buildFinal() {
        // Now walk the matrix and calculate the final values for cosine similarity score.
        Iterator rmit = rowIterator();
        while( rmit.hasNext() ) {
            SparseModelRow r1 = (SparseModelRow)rmit.next();
            // Note SparseModelRows support both iterator() which returns the next id or
            // cellIterator() which returns the actual cell.
            Iterator it = r1.cellIterator();
            while ( it.hasNext() ) {
                CosineModelCell cmc = (CosineModelCell)it.next();
                float finalDot = cmc.getPartialDot();
                float coCount = cmc.getCount();
                float fudgeFactor = (float)1.0;
                if (coCount < 20) {
                   fudgeFactor = (float) (coCount / 20.0);
                }
                if ( finalDot != 0.0) {
                    //                 i . j           /     ||i||  * ||j||
                    float ilen = cmc.getLenI();
                    float jlen = cmc.getLenJ();
                    //System.out.println("length i,j =" + jx + " " + " " + ix + " " + ilen + " " + jlen);
                    float tsim = (float) ((finalDot / (Math.sqrt(ilen) * Math.sqrt(jlen)) ) * fudgeFactor);
                    //System.out.println("similarity for " + jx + "," + ix + " = " + tsim + " * " + fudgeFactor);
                        cmc.setPartialDot(tsim);
                    /*
                    System.out.println("Cell: row " + r1.getKey() + " ilen=" + ilen + " jlen=" + jlen
                        + " finalDot=" + finalDot
                        + " fudge=" + fudgeFactor + " tsim="+tsim);
                    */
                }

            }
        }
        finalCalled = true;
    }

    public void truncate(int rowLimit) {
        Iterator rowit = rowIterator();
	    int deleteCount = 0;
        while ( rowit.hasNext()) {
            SparseModelRow r1 = (SparseModelRow)rowit.next();
            Iterator it = r1.cellIterator();
            float mySims[] = new float[r1.cardinality()];
            int i = 0;
            while(it.hasNext()) {
                CosineModelCell cmc = (CosineModelCell)it.next();
                mySims[i] = cmc.getSimilarity();
                i++;
            }
            Arrays.sort(mySims);
            if (rowLimit < mySims.length) {
                float limit = mySims[mySims.length-rowLimit];
                it = r1.iterator();
                while(it.hasNext()) {
                    int key = ((SparseModelRow.SparseRowIterator)it).inext();
                    float value = ((CosineModelCell)r1.getCell(key)).getSimilarity();
                    if (value < limit) {
                        r1.deleteCell(key);
			            deleteCount++;
                    }
                }
            }

        }
	    System.out.println("Deleted " + deleteCount + " cells");
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
                new Date() +
                ": Wrote out "
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
     * Represent a (user, rating) pair for ratingsArray.
     * This is very similar to Item.  TODO: Merge with Item?
     *  
     * @author dfrankow
     */
    private static class UserRating implements Comparable {
        int user;
        float modRating;

        UserRating(int user, float modRating) {
            this.user = user;
            this.modRating = modRating;
        }

        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            return "user " + user + " modRating " + modRating;
        }

        /**
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Object o) {
            UserRating u2 = (UserRating)o;
            return user - u2.user;
        }

        /**
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object obj) {
            if (null == obj) return false;
            UserRating u2 = (UserRating)obj;
            return user == u2.user;
        }
    }

    /**
     * Yet another representation of a ratings row.
     * 
     * TODO: Merge with RatingSource.ratingsRow and SparseModelRow.
     * Unique features of this one:
     * 
     * - it doesn't grow; it starts with a fixed size
     * - it has a low-level array of RatingUsers, for speed.
     * 
     * @author dfrankow
     */
    private class ratingsArray implements Comparable {
        private UserRating userRating[];
        private int id;
        private int numEntries;

        ratingsArray(int id, int len) {
            this.id = id;
            userRating = new UserRating[len];
            numEntries = 0;

            //System.out.println("New ratingsArray id " + id + " len " + len);
        }

        int size() {
            return userRating.length;
        }

        /**
         * @return
         */
        public int getId() {
            return id;
        }

        /**
         * @return
         */
        public UserRating[] getUserRatings() {
            return userRating;
        }

        public void add(UserRating userRating) {
            //System.out.println("Add to id " + id + " numEntries " + numEntries + " user " + user + " value " + value);
            this.userRating[numEntries] = userRating;
            numEntries++;
        }
        
        public void sort() {
            Arrays.sort(userRating);
        }

        /**
         * NOTE: Compares only on id, not values !!
         * @see java.lang.Comparable#compareTo(java.lang.Object)
         */
        public int compareTo(Object o) {
            ratingsArray ra = (ratingsArray) o;
            return id - ra.id;
        }

        /**
         * NOTE: Compares only on id, not values !!
         * @see java.lang.Object#equals(java.lang.Object)
         */
        public boolean equals(Object obj) {
            if (null == obj) return false;
            ratingsArray ra = (ratingsArray) obj;
            return id == ra.id;
        }
    }

    /**
     * Build and write a model in CosineRecModel binary format.
     * 
     * Combining these two steps allows us to hit the disk without ever
     * putting the entire model in memory, which might allow important
     * optimizations.
     * 
     * NOTE: After this function, the model is NOT necessarily in memory,
     * as it is with build().
     * 
     * @param source       RatingSource to get ratings from
     * @param fname        Filename to which to write.
     * @param countThresh  Only write cells with count > countThresh.
     * @throws IOException
     */
    public void buildAndWriteCosineRecModelBinary(
        RatingsSource source,
        String fname,
        int countThresh)
        throws IOException {

        // Count how many items, and rats per item
        if (showDebug) {
            System.out.println(new Date() + ": Count ratings/item ..");
        }
        HashMap itemStats = new HashMap();
        Iterator iter = source.user_iterator();
        while (iter.hasNext()) {
            RatingsSource.RatingRow urow = (RatingsSource.RatingRow) iter.next();
            int uKey = urow.getId();

            Iterator riter = urow.getElements().iterator();
            while (riter.hasNext()) {
                Item item = (Item) riter.next();
                Integer itemKey = new Integer(item.getItemID());
                Integer icell = (Integer) itemStats.get(itemKey);
                if (null == icell) {
                    icell = new Integer(0);
                }
                itemStats.put(itemKey, new Integer(icell.intValue()+1));
            }
        }

        //
        // Make a rating map organized by item
        //
        if (showDebug) {
            System.out.println(new Date() + ": Store ratings by organized by item ..");
        }

        // Init itemRatingsMap with empty arrays
        HashMap itemRatingsMap = new HashMap();
        iter = itemStats.keySet().iterator();
        while (iter.hasNext()) {
            Integer itemKey = (Integer) iter.next();
            
            int numRats = ((Integer)itemStats.get(itemKey)).intValue();
            ratingsArray r = new ratingsArray(itemKey.intValue(), numRats);
            itemRatingsMap.put(itemKey, r);
        }

        // Put ratings into the arrays
        iter = source.user_iterator();
        while (iter.hasNext()) {
            RatingsSource.RatingRow urow = (RatingsSource.RatingRow) iter.next();
            int uKey = urow.getId();

            Iterator riter = urow.getElements().iterator();
            while (riter.hasNext()) {
                Item item = (Item) riter.next();
                Integer itemKey = new Integer(item.getItemID());
                ratingsArray irow = (ratingsArray) itemRatingsMap.get(itemKey);
                irow.add(new UserRating(uKey, item.getModRating()));
            }
        }

        // NOTE: At this point, we could throw away the RatingsSource and
        // any data it is keeping in it.  That might save a bunch of memory,
        // but to do so RatingsSource has to be smarter.

        // We want itemMapValues in row-id order so CosineRecModel.readBinaryModel
        // reads it in row-by-row.  Hence the TreeSet, to sort the values.
        TreeSet itemMapValues = new TreeSet(itemRatingsMap.values());

        // Sort each item row, so that it is quick to find intersections
        iter = itemMapValues.iterator();
        while (iter.hasNext()) {
            ratingsArray irow = (ratingsArray) iter.next();
            //System.out.println("itemMap irow: " + irow);
            irow.sort();
        }

        // Compute diagonal statistics we'll need later (sum-squared modRating)
        // This is commented out because we do not use any stats that are constant
        // per-item.
        /* 
        HashMap diagStats = new HashMap();
        iter = itemMapValues.iterator();
        while (iter.hasNext()) {
            RatingsSource.RatingRow irow = (RatingsSource.RatingRow) iter.next();
            Vector irowRats = irow.getElements();

            Iterator jter = irowRats.iterator();
            float sumsq = 0;
            while (jter.hasNext()) {
                Item item = (Item) jter.next();
                float rat = item.getModRating();
                sumsq += rat * rat;
                System.out.println(" row " + irow.getId()
                    + " sumsq += " + (rat * rat) + " = " + sumsq);
            }
            diagStats.put(new Integer(irow.getId()), new Float(sumsq));
        }
        */

        // Write out one model row at a time
        if (showDebug) {
            System.out.println(new Date() + ": Write out the model rows ..");
        }
        long t1 = new Date().getTime();

        int numCells = 0;
        FileOutputStream fos = new FileOutputStream(fname);
        BufferedOutputStream bos = new BufferedOutputStream(fos,1024);
        DataOutputStream modelOut = new DataOutputStream(bos);

        iter = itemMapValues.iterator();
        while (iter.hasNext()) {
            ratingsArray irow = (ratingsArray) iter.next();
            UserRating iuser[] = irow.getUserRatings();
            int isize = irow.size();
            int iKey = irow.getId();

            Iterator jter = itemMapValues.iterator();
            while (jter.hasNext()) {
                ratingsArray jrow = (ratingsArray) jter.next();
                UserRating juser[] = jrow.getUserRatings();
                int jsize = jrow.size();
                int jKey = jrow.getId();

                // Find intersection
                int i = 0;
                int j = 0;
                int coCount = 0;          

                float sumij = 0;
                float sumii = 0;
                float sumjj = 0;
                while ((i < isize) && (j < jsize)) {
                    int iid = iuser[i].user;
                    int jid = juser[j].user;

                    if (iid == jid) {
                        // Same user rated items irow.getId() and jrow.getId()
                        //System.out.println("Corating: item " + irow.getId() + " and " + jrow.getId() + " user " + itemi.getItemID() + " (i=" + i + ") and user " + itemj.getItemID() + " (j=" + j + ")");
                        float irat = iuser[i].modRating;
                        float jrat = juser[j].modRating;
                        coCount++;
                        sumij += irat * jrat;

                        // NOTE: sumii and sumjj are computed only over users
                        // who also corated i and j.  It is unclear to me if
                        // this is best.  Brad Miller says this is by design,
                        // that he, John, and Prof. Karypis evaluated it somewhat,
                        // and also that Badrul's item-item paper is ambiguous.
                        sumii += irat * irat;
                        sumjj += jrat * jrat;
                        i++;
                        j++; 
                    }
                    else if (iid < jid) {
                        i++;
                    }
                    else {
                        j++;
                    }
                }
                if (coCount > countThresh) {
                    float fudgeFactor = (float)1.0;
                    if (coCount < 20) {
                       fudgeFactor = (float) (coCount / 20.0);
                    }
                    //                 i . j           /     ||i||  * ||j||
                    float sim = 0;
                    if (sumij != 0) {
                        sim = (float) ((sumij / (Math.sqrt(sumii) * Math.sqrt(sumjj)) ) * fudgeFactor);
                    }

                    /*
                    System.out.println("Item i=" + iKey.intValue() + " j=" + jKey.intValue()
                        + " coCount=" + coCount
                        + " sumij=" + sumij + " sumii=" + sumii + " sumjj=" + sumjj
                        + " fudge=" + fudgeFactor + " sim="+sim);
                        */
    
                    // Write out iKey, jKey, sim
                    modelOut.writeInt(iKey);
                    modelOut.writeInt(jKey);
                    modelOut.writeFloat(sim);
                    numCells++;

                    /*
                    if (numCells > 200000) {
                        // Ditch early for the profiler
                        modelOut.close();
                        return;
                    }
                    */
    
                    if ((numCells % 1000000) == 0) {
                        writeDiffLine(System.out, numCells, t1);
                    }
                }
            }
        }
        writeDiffLine(System.out, numCells, t1);
        modelOut.close();
    }

    /**
     * Write a model in CosineRecModel binary format.
     * 
     * @param fname        Filename to which to write.
     * @param countThresh  Only write cells with count > countThresh.  
     */
    public void writeCosineRecModelBinary(String fname, int countThresh)
        throws IOException {
        DataOutputStream modelOut = null;
        int numCells = 0;

        FileOutputStream fos = new FileOutputStream(fname);
        BufferedOutputStream bos = new BufferedOutputStream(fos,1024);
        modelOut = new DataOutputStream(bos);

        long t1 = new Date().getTime();

        Iterator rmit = rowIterator();
        while( rmit.hasNext() ) {
            SparseModelRow r1 = (SparseModelRow)rmit.next();
            // Note SparseModelRows support both iterator() which returns the next id or
            // cellIterator() which returns the actual cell.
            Iterator it = r1.iterator();
            int rowId = r1.getKey();
            while ( it.hasNext() ) {
                int colId = ((SparseModelRow.SparseRowIterator)it).inext();
                CosineModelCell cmc = ((CosineModelCell)r1.getCell(colId));
                float sim = cmc.getSimilarity();
                if (cmc.getCount() > countThresh) {
/*
                    if (numCells < 50) {
                        System.out.println("Write out " + rowId + " " + colId +  " " + sim);
                    }
*/
                    modelOut.writeInt(rowId);
                    modelOut.writeInt(colId);
                    modelOut.writeFloat(sim);
                    numCells++;
                    
                    if (buildUpper && (colId != rowId)) {
/*
                        if (numCells < 50) {
                            System.out.println("Write out " + colId + " " + rowId +  " " + sim);
                        }
*/
                        // We didn't store this cell, but we wish to
                        // write the whole model, so write the transpose
                        modelOut.writeInt(colId);
                        modelOut.writeInt(rowId);
                        modelOut.writeFloat(sim);
                        numCells++;
                    }

                    if ((numCells % 1000000) == 0) {
                        writeDiffLine(System.out, numCells, t1);
                    }
                }
            }
        }

        writeDiffLine(System.out, numCells, t1);
        modelOut.close();
    }

    /**
     * insertUserItems
     * @param userItems  Vector of Item -- Any filtering should be done outside this function.
     */
    protected void insertUserItems(Vector userItems) {
        int limit = userItems.size();
        int startj;
        for(int i = 0; i< limit; i++) {
            Item itemI =  (Item)userItems.elementAt(i);
            float ti = itemI.modRating;
            // starting with j=i will just build the upper triangle .. maybe not the best...?
            if (buildUpper) {
                startj = i;
            } else {
                startj = 0;
            }
            for (int j = startj; j< limit; j++) {
                //System.out.println("Inserting: " + userItems.elementAt(i) + "," + userItems.elementAt(j));
                Item itemJ = (Item)userItems.elementAt(j);
                float tj = itemJ.modRating;
                CosineModelCell myCell = getModelCell(itemI.itemID, itemJ.itemID);
                myCell.incCoCount();

                float newVal = myCell.getPartialDot();
                newVal += (ti * tj);
                myCell.setPartialDot(newVal);

                float newPartLenI = myCell.getLenI() + (ti * ti);
                float newPartLenJ = myCell.getLenJ() + (tj * tj);
                /*
                if (showDebug) {
                    System.out.print("newPartLenI (i=" + itemI.itemID + ") += " + (ti * ti) + " = " + newPartLenI);
                    System.out.println("  newPartLenJ (j=" + itemJ.itemID + ") += " + (tj * tj) + " = " + newPartLenJ);
                }
                */
                myCell.setLenI(newPartLenI);
                myCell.setLenJ(newPartLenJ);
            }
        }
        finalCalled = false;   // Must call buildFinal() some time
    }

    /**
     * Get the similarity score for the cell at the i-th row and j-th column..
     * The similarity score is represented by the partial dot product.
     * If buildFinal() has not been called, this will call it.
     *
     * @param i  Model row.
     * @param j  Model column.
     * @return   float.
     */
    public float getSim(int i, int j) {
        if (!finalCalled) {
            buildFinal();
        }
        return getModelCell(i,j).getPartialDot();
    }

    /**
     * This function is meant for use with db store/retrieve only.
     * We store the similarity score in the db as an integer so convert it.
     *
     * @param i
     * @param j
     * @return the similarity score (* 1000)
     */
    public int getDBRepSimScore(int i, int j) {
        return (int)(getSim(i,j) * 1000.0);
    }

    /**
     * Set the database score, converting it to an integer.
     *
     * @param i
     * @param j
     * @param val
     */
    public void setDBRepSimScore(int i, int j, int val) {
        getModelCell(i,j).setPartialDot((float)(val/1000.0));
    }

    /**
     * Combine two cells.  First convert the cell values to their least common denominator (db value)
     * This was kind of a hack to get combineModel stuff to work quickly.
     *
     * @param i
     * @param j
     * @param val
     * @param myOp
     */
    public void addDBRepSimScore(int i, int j, int val, CellCombiner myOp) {
        float current = getSim(i,j);
        if (current > 0.0) {
            float newval = (float)val / (float)1000.0;
            current = myOp.combine( current, newval );
            getModelCell(i,j).setPartialDot(current);
        } else {
            current = (float)(val/1000.0);
            getModelCell(i,j).setPartialDot(current);
        }
    }
}
