package org.grouplens.multilens;

import java.io.PrintStream;
import java.util.*;

/**
 * A collection of SparseModelRows.
 * 
 * NOTE: There is an attempt to be thread-safe with this class, but it is a
 * very weak guarantee.  We simply try to protect enough to avoid null pointer
 * exceptions or asking for row #3 and getting row #4.
 */
public class SparseModel {
    /** If this is true, write out some timings, memory measurements, and other info */
    protected boolean showDebug = false;

    /** myModelRow contains the rows of the model */
    private SparseModelRow[] myModelRow;
    /** rowMap contains the row keys of the model */
    private int[] rowMap;
    /** rowMax is the number of rows waiting to possibly contain stuff.
     * rowMax >= currentRows. */
    private int rowMax;
    /** colMax is a hint to each SparseModelRow about how big to start out */
    private int colMax;
    /** lastkey is the most recent row key passed in to getRow(), if >= 0 */
    private int lastkey;
    /** lastidx is the index of the row with key lastkey, if >= 0 */
    private int lastidx;
    /** currentRows is the number of rows that have stuff in them */
    private int currentRows;
    /** maxKey is the largest row key seen by insertRow() */
    private int maxKey;

    private synchronized void setGuts(int rows, int cols) {
        rowMax = rows+1;
        myModelRow = new SparseModelRow[rowMax];
        rowMap = new int[rowMax];  // contains keys

        colMax = cols;
        // Sort the keys in myItems
        Arrays.fill(rowMap,Integer.MAX_VALUE);
        for (int i = 0; i < rowMax; i++) {
            myModelRow[i] = null;
        }
        currentRows = 0;

        lastkey = -1;
        lastidx = -1;

        Arrays.sort(rowMap);
    }

    public SparseModel() {
        //System.out.println("SparseModel.SparseModel() called");
        setGuts(0, 0);
    }

    /**
     * Clear out model of old stuff.
     */
    public synchronized void clear() {
        setGuts(0, 0);
    }

    /** Create a SparseModel with the initial number of rows and columns.
     *
     * @param rows  Initial number of rows
     * @param cols  Initial number of columns
     */
    public SparseModel(int rows, int cols) {
        setGuts(rows, cols);
    }

    /**
     * Get a row by key.  Create it if it doesn't exist.
     * 
     * @param  rowKey          The key of the desired row
     * @return SparseModelRow  The desired row
     **/
    public synchronized SparseModelRow getRowOrCreate(int rowKey) {
        int rowIx = getRow(rowKey);
        //System.out.println("SparseModel.getRowOrCreate: getRow returned " + rowIx);
        if (rowIx < 0) {
            //System.out.println("SparseModel.getRowOrCreate() insert row key " + rowKey + " index " + Math.abs(rowIx+1));
            rowIx = insertRow(Math.abs(rowIx+1),rowKey);
        }
        else {
            //System.out.println("SparseModel.getRowOrCreate() found row key " + myModelRow[rowIx].getKey() +" when you asked for " + rowKey + " index " + rowIx);
        }
        
        assert(myModelRow[rowIx].getKey() == rowKey);
        return myModelRow[rowIx];
    }

    /**
     * Remove a row by key.
     * 
     * @param rowKey
     * @throws IllegalArgumentException if you ask to delete a non-existent row
     */
    public synchronized void removeRowByKey(int rowKey) throws IllegalArgumentException {
        int rowIx = getRow(rowKey);
        if (rowIx < 0) {
            // No row to delete
            throw new IllegalArgumentException("Tried to delete non-existent row with key " + rowKey);
        }
        removeRowByIdx(rowIx);
    }

    /**
     * Remove a row by index.
     * 
     * @param rowIx
     * @throws IllegalArgumentException
     */
    private synchronized void removeRowByIdx(int rowIx) throws IllegalArgumentException {
        if ((rowIx < 0) || (rowIx > currentRows)) {
            // No row to delete
            throw new IllegalArgumentException("Tried to delete non-existent row with index " + rowIx);
        }

        // Possibly we could preserve these, but don't bother for now
        lastidx = -1;
        lastkey = -1;

        // Squash this row out
        for (rowIx++; rowIx < currentRows; rowIx++) {        
            myModelRow[rowIx-1] = myModelRow[rowIx];
            rowMap[rowIx-1] = rowMap[rowIx];
        }
        rowMap[currentRows] = Integer.MAX_VALUE;
        myModelRow[currentRows] = null;
        currentRows--;

        // Compute a new maxKey
        maxKey = -1;
        for (rowIx = 0; rowIx < currentRows; rowIx++) {
            int key = myModelRow[rowIx].getKey(); 
            if (key > maxKey) maxKey = key;
        }
    }

    /**
     * Delete rows with from <= rowKey <= to.
     * 
     * @param from Row key to start deleting from (inclusive)
     * @param to   Row key to delete to (inclusive)
     */
    public synchronized void deleteRows(int from, int to) {
        // NOTE: this algorithm is very expensive if the
        // row keys aren't mostly contiguous.  For MovieLens, they are.
        // If that changes, we should iterate along the rows themselves,
        // not the row keys.  That might require more interface.  For
        // example, the ability to create an iterator at a certain row,
        // and iterate in row-key order.
        while (from <= to) {
            SparseModelRow r = getModelRow(from);
            if (null != r) {
                removeRowByKey(r.getKey());                       
            }
            from++;
        }
    }

    /**
     * Replace a row.  Create it if it doesn't exist.
     * "Replace" means take the row passed in, and perform a deep copy
     * into this matrix.  Deep copy might be expensive, but is less easily
     * misused.
     *  
     * @param  row             The row to insert (must have the right key)
     * @return SparseModelRow  The desired row
     **/
    public synchronized void replaceRowOrCreate(SparseModelRow row) throws CloneNotSupportedException {
        // Make the new row
        int rowKey = row.getKey();

        int rowIx = getRow(rowKey);
        if (rowIx < 0) {
            rowIx = insertRow(Math.abs(rowIx+1),rowKey);
        }
        
        myModelRow[rowIx] = (SparseModelRow)row.clone();

        assert(myModelRow[rowIx].equals(row));
    }

    /**
     * Insert a row with row key rowKey at index ip.
     * 
     * @param ip        Index at which to insert
     * @param rowKey    Key of the row
     * @return          Index at which inserted
     */
    protected synchronized int insertRow(int ip, int rowKey) {
        assert(ip >= 0);
//        System.out.println("SparseModel.insertRow: rowMap.length=" + rowMap.length + " currentRows=" + currentRows + " ip=" + ip + " rowMax=" +rowMax);
        assert(rowMap.length > currentRows); 
        assert(myModelRow.length > currentRows); 
        assert(getRow(rowKey) < 0);   // Row doesn't exist

        while (currentRows >= rowMax) {
            // We're trying to insert past the end, so expand
            expandRows();
        }

        for(int i = currentRows; i>ip; i--) {
            assert(rowMap[i] == myModelRow[i].getKey());
            assert(rowMap[i-1] < rowMap[i]);

            rowMap[i] = rowMap[i-1];
            myModelRow[i] = myModelRow[i-1];
        }
        if (rowKey > maxKey) {
            maxKey = rowKey;
        }

        rowMap[ip] = rowKey;
        myModelRow[ip] = new SparseModelRow(colMax,rowKey);
        currentRows++;
        assert(rowMap.length > currentRows);
        assert(myModelRow.length > currentRows);
        assert(rowMap.length == myModelRow.length);

        lastidx = ip;
        lastkey = rowKey;

        return ip;
    }

    protected synchronized void expandRows() {
        SparseModelRow saveRows[];
        int saveMap[];
        saveRows = myModelRow;
        saveMap = rowMap;

        int expandBy = rowMax / 2 + 1;
        assert(expandBy > 0);

        // System.out.println("SparseModel.expandRows: Expand from " + rowMax + " to " + (rowMax+expandBy));

        rowMax = rowMax + expandBy;
        assert(rowMax > 0);

        myModelRow = new SparseModelRow[rowMax+1];
        rowMap = new int[rowMax+1];
        Arrays.fill(rowMap,Integer.MAX_VALUE);

        assert(currentRows <= rowMax);
        for(int i = 0; i< currentRows; i++) {
            myModelRow[i] = saveRows[i];
            rowMap[i] = saveMap[i];
        }

        assert(rowMap.length == myModelRow.length);
        assert(currentRows < rowMax);
    }

    /**
     * Print out the model
     * @param out   The PrintStream to which to print.
     */
    public void printModel(PrintStream out) {
        int j;
        Iterator rmit = rowIterator();
        while( rmit.hasNext() ) {
            SparseModelRow r1 = (SparseModelRow)rmit.next();
            Iterator it = r1.iterator();
            while ( it.hasNext() ) {
               j = ((Integer)it.next()).intValue();
               out.println("model[" + r1.getKey() + "][" + j + "]= " + r1.getCell(j).toString());
            }
        }
    }

    /**
     * Given a key, return
     * <ul>
     * <li>the index of the row, iff a row with such a key exists
     * <li>-(insertion_point-1), where insertion_point is where the key
     *     would go.
     * </ul>
     * @param key  The key of the row.
     * @return 
     * <ul>
     * <li>the index of the row, iff a row with such a key exists
     * <li>-(insertion_point-1), where insertion_point is where the key
     *     would go.
     * </ul>
     * @see java.util.Arrays#binarySearch(int[], int)
     */
    protected synchronized int getRow(int key) {
        int rowIx;
        if (key == lastkey ) {
            //System.out.println("SparseModel.getRow cached lastkey=" + lastkey + " lastidx=" + lastidx);
            rowIx = lastidx;
        } else {
            rowIx = Arrays.binarySearch(rowMap, key);
            if(rowIx >= 0) {
                lastidx = rowIx;
                lastkey = key;
            }
            //System.out.println("SparseModel.getRow binary search key=" + i + " idx=" + rowIx);
        }
        return rowIx;
    }

    /**
     *  Get the sum of cardinality of the rows
     * 
     * @return Sum of row cardinalities
     */
    public synchronized int cardinality() {
        int sum = 0;
        Iterator it = rowIterator();
        while (it.hasNext()) {
            SparseModelRow r = (SparseModelRow)it.next();
            sum += r.cardinality();
        }
        return sum;
    }

    /**
     * Return a row given its key, or null if no such row.
     * 
     * @param key   The key of the row.
     * @return SparseModelRow  Row with that key, or null if no such row
     */
    public synchronized SparseModelRow getModelRow(int key) {
        //
        // NOTE: This function is thread-safe if myModelRow never shrinks,
        // because then myModelRow[rowIx] always exists.  Currently, that is
        // true.
        //
        int rowIx = getRow(key);

        if (rowIx >= 0) {
            return myModelRow[rowIx];
        } else {
            return null;
        }
    }

    public int getMaxKey() {
        return maxKey;
    }

    public SparseModelIterator rowIterator() {
        return new SparseModelIterator();
    }

    /**
     * Inner class implementation of an iterator for sparse rows;
     */
    class SparseModelIterator implements Iterator {
        private int currentRowIdx = 0;

        SparseModelIterator () {
            currentRowIdx = 0;
        }

        public boolean hasNext() {
            if (currentRowIdx < currentRows &&
                rowMap[currentRowIdx] != Integer.MAX_VALUE ) {
                return true;
                }
            else {
                return false;
            }
        }

        public Object next() {
            int i = currentRowIdx;
            if (i < currentRows ) {
               currentRowIdx = i + 1;
               return myModelRow[i];
            }
            else {
                return null;
            }
        }

        /**
         * Remove the element that was just returned by next().
         * If next() has not been called, behavior is undefined!
         * If remove() is called twice in a row, behavior is undefined!
         */
        public void remove() {
            assert(currentRowIdx <= currentRows);
            removeRowByIdx(currentRowIdx-1);

            // Move the row pointer back
            currentRowIdx--;
        }
    }

    /**
     * Get some simple statistics about this model.
     * @return Stats
     */
    public Stats getStats() {
        return new Stats();
    }

    /** Compute simple statistics about this model */
    public class Stats {
        /** Maximum length of a model row */
        private int maxRowLength = 0;
        /** Number of cells in the model */
        private int numCells = 0;
        /** Number of unique consequents */
        private int numCons = 0;

        /** Walk the model and calculate stats
         * TODO: Determine if the model is upper triangular or not.
         */
        private void walk() {
            // Map of all the consequents
            HashMap consMap = new HashMap();

            SparseModelIterator rmit = rowIterator();
            while( rmit.hasNext() ) {
                SparseModelRow r1 = (SparseModelRow)rmit.next();
                // Note SparseModelRows support both iterator() which returns the next id or
                // cellIterator() which returns the actual cell.
                Iterator it = r1.iterator();
                if (r1.cardinality() > maxRowLength) {
                    maxRowLength = r1.cardinality();
                }
                int rowId = r1.getKey();
                while ( it.hasNext() ) {
                    int colId = ((SparseModelRow.SparseRowIterator)it).inext();
                    consMap.put(new Integer(colId), new Integer(colId));
                    numCells++;
                    if (showDebug && (0 == (numCells % 1000000))) {
                        System.err.println("Walked " + numCells + " cells");
                    }
                    
                }
            }
            numCons = consMap.size();
        }

        public Stats() {
            walk();
        }

        /**
         * @return Number of populated rows in the model.
         */
        public int getNumRows() {
            return currentRows;
        }

        /**
         * @return Number of columns in the (sparse) model with at least one non-zero entry.
         */
        public int getNumCols() {
            return numCons;
        }
        
        /**
         * @return Number of cells in the (sparse) model.
         */
        public int getNumCells() {
            return numCells;
        }

        public String toString() {
            String s = new String();
            s += "# antecedents: " + currentRows
                 + " # consequents: " + numCons
                 + " # cells: " + numCells
// This is the same as the number of cells
//                 + " cardinality: " + cardinality()
                 + " max row length: " + maxRowLength
                 + " max key: " + getMaxKey();
            return s;
        }
    }

    /**
     * @return Number of populated rows in the model.
     */
    public int getNumRows() {
        return currentRows;
    }

    /**
     * @return A hint about column size
     */
    public int getColMax() {
        return colMax;
    }

    /**
     * Return true iff this model has the same entries as obj.
     * 
     * @param obj   SparseModel to which to compare
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public synchronized boolean equals(Object obj) {
        if (null == obj) return false;
        SparseModel that = (SparseModel) obj;
        if (currentRows != that.currentRows) {
            if (showDebug) {
                System.err.println("SparseModel.equals: currentRows "
                    + currentRows + " (maxKey = " + maxKey + ") != that.currentRows " + that.currentRows + " (that.maxKey = " + that.getMaxKey() + ")");
            }
            return false;
        } 

        Iterator rmit = rowIterator();
        while( rmit.hasNext() ) {
            SparseModelRow r1 = (SparseModelRow)rmit.next();
            SparseModelRow r2 = (SparseModelRow)that.getModelRow(r1.getKey());
            if (!r1.equals(r2)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Turn on or off showing of timing, memory measurements, and other info
     * @param show
     */
    public void setShowDebug(boolean show) {
        showDebug = show;
    }

    /**
     * Return the current capacity of this container.  That is, not how much it
     * has in it, but how much space it has to put stuff in without growing.
     * 
     * @return The capacity 
     */
    public int getCapacity() {
        return rowMax;
    }

}
