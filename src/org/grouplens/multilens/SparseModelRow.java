package org.grouplens.multilens;

import java.util.*;

/** A simple, yet expandable sparse vector. */
// once this is working in FloatModel, we can expand the
// object to hold all three matrices.  This will save memory,
// overhead, and would allow memoization of the last access.
public class SparseModelRow implements Cloneable {
    private int myKey;
    /** rowMap contains the cell keys of the row */
    private int rowMap[];
    /** cell contains the cells of the row */
    private SparseCell cell[];
    /** currentCapacity is the number of cells that have stuff in them */
    private int currentCapacity;
    /** maxCapacity is the number of cells waiting to possibly contain stuff.
     * maxCapacity >= currentCapacity. */
    private int maxCapacity;
    /** lastidx is the index of the cell with key lastkey, if >= 0 */
    private int lastidx;
    /** lastkey is the most recent key passed in to getColIndex(), if >= 0 */
    private int lastkey;

    /**
     * Create a SparseModelRow with a given initial capacity.
     * RowKey ensures that we have quick access to the key corresponding to
     * this particular row.
     *
     * @param initialCapacity   initial capacity of this row  
     * @param rowKey            key of this row
     */
    public SparseModelRow(int initialCapacity,int rowKey) {
        rowMap = new int[initialCapacity+1];
        myKey = rowKey;
        Arrays.fill(rowMap,(short)-1);
        Arrays.fill(rowMap,Integer.MAX_VALUE);

        // initialize the array of sparse cells here
        cell = new SparseCell[initialCapacity+1];

        currentCapacity = 0;
        maxCapacity = initialCapacity;
        lastkey = -1;
        lastidx = -1;
    }

    // just return the appropriate value from the sparse cell

    /**
     * Find and return a cell from this row.  The Actual kind of SparseCell
     * returned is dependent on the kind of model we are creating.
     *
     * @param  key         Key of cell to get.
     * @return SparseCell  Cell with key key.
     */
    public SparseCell getCell(int key) {
        SparseCell c = null;

        int ix = getColIndex(key);
        if (ix >= 0) {
            c = cell[ix];
        }
        //System.out.println("SparseModelRow.getCell(" + key + ") returns " + c);
        return c;
    }

    /**
     * Insert myCell with key key.
     * If the cell already exists, replace its contents.
     *
     * @param key     key of cell
     * @param myCell  cell to insert
     */
    public void insertCell(int key, SparseCell myCell) {
        //System.out.println("SparseModelRow.insertCell row key "+getKey()+" key " +key+ " cell " + myCell);
        setCell(getColIndex(key), key, myCell);
    }

    /**
     * Delete a cell (if it exists), given the cell key.
     *
     * @param i   key of the cell
     */
    public void deleteCell(int i) {
        int ix = getColIndex(i);
        if (ix < 0) {
            System.out.println("Error: Trying to delete a non-existant key" );
        } else {
            delete(ix);
        }

    }

    /**
     * Delete a cell, given the cell index.
     * @param ix  index of the cell
     */
    private void delete(int ix) {
        assert(currentCapacity > 0);
        assert(ix <= currentCapacity);

        for(int i = ix; i<currentCapacity; i++) {
            rowMap[i] = rowMap[i+1];
            cell[i] = cell[i+1];
        }
        currentCapacity--;
        // invalidate cached key and idx
        lastkey = -1;
        lastidx = -1;
    }

    /**
     * Return the number of cells in this row that are currently occupied.
     * @return int
     */
    public int cardinality() {
        return currentCapacity;
    }

    public SparseRowIterator iterator() {
        return new SparseRowIterator();
    }

    public SparseRowCellIterator cellIterator() {
        return new SparseRowCellIterator();
    }

    public SparseRowIterator iterator(HashSet filterSet) {
        return new SparseRowIterator(filterSet);
    }

    // my own private binary search function
    private int findKey(int key) {
        int start, end, midPt;
        start = 0;
        end = currentCapacity-1;

        while (start <= end) {
            midPt = (start + end) / 2;
            if (rowMap[midPt] == key) {
                //System.out.println("SparseModelRow.findKey found key " + key + " and returns index " +  midPt);
                return midPt;
            } else if (rowMap[midPt] < key) {
                start = midPt + 1;
            } else {
                end = midPt - 1;
            }
        }
        return (-start - 1);
    }

    /**
     * Get the index of a cell given its key.
     * If no such cell exists, return < 0.
     *
     * @param key  key of cell to find
     * @return     index >= 0 of cell with given key, if it exists, else < 0
     */
    private int getColIndex(int key) {
        int colIx;
        if (lastkey == key) {
            //System.out.println("SparseModelRow.getColIndex: return cached index " + lastidx + " for key " + lastkey);
            colIx = lastidx;
        } else {
//            colIx = Arrays.binarySearch(rowMap,key);
            colIx = findKey(key);
            if (colIx >= 0) {
                lastidx = (short)colIx;
                lastkey = key;
                //System.out.println("SparseModelRow.getColIndex: set cached index " + lastidx + " for key " + lastkey);
            }
        }

        assert((colIx < 0) || (rowMap[colIx] == key));
        return colIx;
    }

    /**
     * If colIx >= 0, replace the contents of an existing cell at colIx
     * with newCell, else insert newCell at -(colIx+1).
     *
     * @param colIx   Index of cell to set.
     * @param iid     Key of cell
     * @param newCell Cell
     */
    private void setCell(int colIx, int iid, SparseCell newCell) {
        //System.out.println("bsearch found: " + colIx);
        if (colIx >= 0) {
            //System.out.println("SparseModelRow.setCell: row key " + getKey() + " cell[" + colIx + "]=" + newCell);
            cell[colIx] = newCell;
        } else { // insert new column
            //System.out.println("SparseModelRow.setCell: row key " + getKey() + " Insert new cell at " + -(colIx+1) + ": " + newCell);
            insert(Math.abs(colIx+1), iid, newCell);
        }
    }

    // This array of SparseCells is just expanded.  Don't worry about the rest
    private void expandArrays() {
        int expandBy = maxCapacity / 2 + 1;
        assert(expandBy > 0);

        // System.out.println("SparseModelRow.expandArrays from " + maxCapacity + " to " + (maxCapacity+expandBy));

        // Save current Stuff
        SparseCell tss[];
        int saveMap[];
        tss = cell;
        saveMap = rowMap;
        // allocate new

        maxCapacity = maxCapacity+expandBy;
        cell = new SparseCell[maxCapacity+1];
        rowMap = new int[maxCapacity+1];
        Arrays.fill(rowMap,Integer.MAX_VALUE);
        // copy
        for(int i=0;i<currentCapacity;i++) {
            cell[i] = tss[i];
            rowMap[i] = saveMap[i];
        }

    }

    /**
     * Insert newCell with key rowKey at index ip.
     *
     * @param ip       Index at which to insert
     * @param rowKey   Key of cell to insert
     * @param newCell  Cell to insert
     * @return         Index where cell now is
     */
    protected int insert(int ip, int rowKey, SparseCell newCell) {
        while (currentCapacity >= maxCapacity) {
            // We're trying to insert past the end, so expand
            expandArrays();
        }

        //System.out.println("SparseModelRow.insert at index " + ip + " key " + rowKey + " cell " + newCell + " (currentCapacity=" + currentCapacity + ")");

        for(int i = currentCapacity; i>ip; i--) {
            //System.out.println("SparseModelRow.insert rowmap[" + i + "]=rowMap[" + (i-1) + "]");
            rowMap[i] = rowMap[i-1];
            cell[i] = cell[i-1];
        }
        rowMap[ip] = rowKey;
        cell[ip] = newCell;
        currentCapacity++;

        //System.out.println("SparseModelRow.insert (currentCapacity now " + currentCapacity + ")");

        assert(currentCapacity <= maxCapacity);

        lastidx = ip;
        lastkey = rowKey;

        return ip;
    }

    public int getKey() {
        return myKey;
    }


    /**
     * Iterator that returns cell keys.
     */
    public class SparseRowIterator implements Iterator {
        private int currentKey = 0;
        private HashSet goodItem = null;

        SparseRowIterator () {
            currentKey = 0;
        }

        SparseRowIterator (HashSet itMap) {
            currentKey = 0;
            goodItem = itMap;
        }

        public boolean hasNext() {
             if (currentKey < currentCapacity &&
                 rowMap[currentKey] != Integer.MAX_VALUE ) {
                 return true;
                 }
             else {
                 return false;
             }
         }

        /**
         * Return the next cell key, or null if none left.
         * @return Integer that is the next cell key, or null if none left.
         **/
        public Object next() {
            int i = currentKey;
            if (i < currentCapacity ) {
               currentKey = i + 1;
               return new Integer(rowMap[i]);
            }
            else {
                return null;
            }
        }

        /**
         * Return the next cell key, or -1 if none left.
         * @return int that is the next cell key, or -1 if none left.
         **/
        public int inext() {
            int i = currentKey;
            if (i < currentCapacity ) {
               currentKey = i + 1;
               return rowMap[i];
            }
            else {
                return -1;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("No support for remove");
        }
    }

    /**
     * Iterator that returns cells.
     */
    class SparseRowCellIterator implements Iterator {
        private int currentKey = 0;
        private HashSet goodItem = null;

        SparseRowCellIterator () {
            currentKey = 0;
        }

        public boolean hasNext() {
             if (currentKey < currentCapacity &&
                 rowMap[currentKey] != Integer.MAX_VALUE ) {
                 return true;
                 }
             else {
                 return false;
             }
         }

        /**
         * Return the next cell, or null if none left.
         * @return the next cell, or null if none left.
         **/
        public Object next() {
            int i = currentKey;
            if (i < currentCapacity ) {
               currentKey = i + 1;
               return cell[i];
            }
            else {
                return null;
            }
        }

        public void remove() {
            throw new UnsupportedOperationException("No support for remove");
        }
    }

    /** Compare two SparseModelRow-s.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (null == obj) return false;
        SparseModelRow that = (SparseModelRow) obj;
        if (getKey() != that.getKey()) {
            System.err.println("SparseModelRow.equals: getKey() "
                + getKey() + " != that.getKey() " + that.getKey());
            return false;
        }
        if (cardinality() != that.cardinality()) {
            System.err.println("SparseModelRow.equals (key " +getKey()
                +": cardinality() " + cardinality()
                + " != that.cardinality " + that.cardinality());
            return false;
        } 
        
        Iterator it = iterator();
        while ( it.hasNext() ) {
           int j = ((Integer)it.next()).intValue();
           if (!getCell(j).equals(that.getCell(j))) {
               return false;
           }
        }

        return true;
    }

    /**
     * Deep copy.
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        SparseModelRow r = (SparseModelRow)super.clone();
        r.myKey = myKey;
        r.rowMap = (int[])rowMap.clone();
        r.cell = (SparseCell[])cell.clone();
        r.currentCapacity = currentCapacity;
        r.maxCapacity = maxCapacity;
        r.lastidx = lastidx;
        r.lastkey = lastkey;

        return r;
    }

    /**
     * String representation for debugging.
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String s = "";
        if (currentCapacity > 0) {
            for(int i = 0; i<currentCapacity; i++) {
                s += "cell idx " + i + " key " + rowMap[i]
                    + " cell " + cell[i] + "   ";
            }
        }
        else {
            s = "empty";
        }
        return s;
    }
}
