package org.grouplens.multilens;

import java.util.*;
import java.io.*;

import org.grouplens.util.GlConfigVars;

/**
 * AverageModel holds the user-adjusted average rating value for each item, i.e.
 * sum((user-item rating)-(user average), over all users) for each item.
 * 
 * Currently, we extend GenericModel in order to get some of the functionality
 * (e.g., build() to build from the database).  However, much of the other
 * interface is not implemented. 
 */
public class AverageModel extends GenericModel implements ItemModel {
    /** A class to hold statistics about a given item */
    private static class AverageModelCell {
        private float total;
        private int count;
        AverageModelCell() {
            total = 0;
            count = 0;
        }
    }

    private HashMap myMap = new HashMap();
    private int numUsers = 0;
    /** Maximum item key seen in buildFromFile, insertUserItems, readBinaryModel */
    private int maxItem = 0;
    /** Number of ratings an item must have before it may be considered
     * to have an average, when using buildFromFile() */
    private int averageRatingThresh;

    public AverageModel() {
        // TODO:  This is a bit arbitrary, but if an item has less than 5 scores overall I don't
        // think it should be a candidate for recommendation.  I don't know what this number really
        // should be??
        averageRatingThresh = 5;
    }

    /**
     * Set number of ratings an item must have before it may be considered
     * to have an average, when using buildFromFile().
     * @param a
     */
    public void setAverageRatingThreshold(int a) {
        averageRatingThresh = a;
    }

    public int getMaxKey() {
        return maxItem;
    }

    public void buildFromFile(String ratingFile, ItemVectorModifier myFilt) throws IOException {
        clear();

        BufferedReader in = null;
        Integer currentUser = new Integer(0);
        Vector currentItems;
        LinkedList userList = new LinkedList();

        in = new BufferedReader(new FileReader(ratingFile));

        currentItems = new Vector(100);

        String line = in.readLine();
        while(line != null) {
            StringTokenizer tokens = new StringTokenizer(line);
            Integer userID = new Integer(tokens.nextToken());
            int itemID = Integer.parseInt(tokens.nextToken());
            if (itemID > maxItem) {
                maxItem = itemID;
            }
            float rating = Float.parseFloat(tokens.nextToken());
            Item theItem = new Item(itemID,rating);
            if (! userID.equals(currentUser) ) {
                // Take care of adding the previous user to the model.
                numUsers++;
                ItemComparator myComp = new ItemComparator();
                Collections.sort(currentItems,myComp);
                if (myFilt != null) {
                    myFilt.filter(currentItems);
                }
                User newUser = new User(currentItems);
                newUser.setUserId(currentUser.toString());
                User.addUserToCache(newUser);
                userList.add(newUser);
                currentItems = new Vector(50);
                currentUser = userID;
            }
            currentItems.add(theItem);
            line = in.readLine();
        }
        // Take care of the last user
        numUsers++;
        if (myFilt != null) {
            myFilt.filter(currentItems);
        }
        User newUser = new User(currentItems);
        newUser.setUserId(currentUser.toString());
        User.addUserToCache(newUser);
        userList.add(newUser);
        in.close();

        // Now that we have all of the users in the cache, calculate the Average and count for all items.
        Iterator i = userList.iterator();
        while(i.hasNext()) {
            User iUser = (User)i.next();
            HashMap myRatings = iUser.getAllRatings();
            Iterator itemIt = myRatings.keySet().iterator();
            while(itemIt.hasNext()) {
                Integer key = (Integer)itemIt.next();
                Float rating = (Float)myRatings.get(key);
                AverageModelCell cell = getOrCreateCell(key.intValue());
                cell.count++;
                float val = rating.floatValue() - iUser.getAverage();
                //System.err.println("item " + key + " rat " + rating.floatValue()
                //    + "- user " + iUser + " avg " + iUser.getAverage() + " = " + val);
                cell.total += val;
            }
        }

        for (Iterator iter = myMap.values().iterator(); iter.hasNext(); ) {
            AverageModelCell cell = (AverageModelCell) iter.next();
            if (cell.count < averageRatingThresh) {
                cell.total = Float.NEGATIVE_INFINITY;
            }
        }
    }

    /**
     * Get the right cell, or create it if it doesn't exist
     * 
     * @param key   Key of cell to look for
     * @return      Cell with that key
     */
    private AverageModelCell getOrCreateCell(int key) {
        Integer theKey = new Integer(key);
        AverageModelCell cell = (AverageModelCell) myMap.get(theKey);
        if (null == cell) {
            cell = new AverageModelCell();
            myMap.put(theKey, cell);
        }
        return cell;
    }

    public int getDBRepSimScore(int i, int j) {
        return 0;
    }

    public void setDBRepSimScore(int i, int j, int val) {
    }

    public void addDBRepSimScore(int i, int j, int val, CellCombiner myOp) {
    }

    protected void insertUserItems(Vector userItems) {
        int limit = userItems.size();
         for(int i = 0; i< limit; i++) {
             Item itemI =  (Item)userItems.elementAt(i);
             if (itemI != null) {
                 AverageModelCell cell = (AverageModelCell) getOrCreateCell(itemI.itemID);
                 cell.total += itemI.modRating;
                 cell.count++;
                 if (itemI.itemID > maxItem) {
                     maxItem = itemI.itemID;
                 }
             }
         }

    }

    /**
     * Write this model in binary format to disk.
     * @param fname   The name of the file to which to write
     */
    public void writeBinaryModel(String fname) throws IOException {
        DataOutputStream modelOut = null;
        int numCells = 0;

        FileOutputStream fos = new FileOutputStream(fname);
        BufferedOutputStream bos = new BufferedOutputStream(fos,1024);
        modelOut = new DataOutputStream(bos);

        for(Iterator iter = myMap.keySet().iterator(); iter.hasNext();) {
            Integer key = (Integer) iter.next();
            AverageModelCell cell = (AverageModelCell) myMap.get(key);
            modelOut.writeInt(key.intValue());
            modelOut.writeInt(cell.count);
            modelOut.writeFloat(cell.total);
            numCells++;
        }
        System.err.println("Wrote " + numCells + " cells");
        modelOut.close();
    }
    
    public String toString() {
        String str = new String();
        for(Iterator iter = myMap.keySet().iterator(); iter.hasNext();) {
            Integer key = (Integer) iter.next();
            AverageModelCell cell = (AverageModelCell) myMap.get(key);
            str += "key " + key.intValue() + " count " + cell.count
                + " total " + cell.total + "\n";
        }        
        return str;
    }

    /**
     * Read this binary model from disk.
     * @param fname  The file to read in.
     */
    public void readBinaryModel(String fname) throws IOException {
        clear();

        DataInputStream modelIn = null;
        int numCells = 0;

        FileInputStream fis = new FileInputStream(fname);
        BufferedInputStream bis = new BufferedInputStream(fis,1024);
        modelIn = new DataInputStream(bis);

        int item, ct;
        float tot;
        while(modelIn.available() > 0) {
            item = modelIn.readInt();
            ct = modelIn.readInt();
            tot = modelIn.readFloat();
            AverageModelCell cell = (AverageModelCell) getOrCreateCell(item);
            cell.count = ct;
            cell.total = tot;
            if(item > maxItem) {
                maxItem = item;
            }
            numCells++;
        }
        System.err.println("Read " + numCells + " cells");
        //Done reading, close the file.
        modelIn.close();
    }

    public SparseModelRow getModelRow(int i) {
        throw new UnsupportedOperationException("No support for SparseModelRow in Average");
    }

    /** Get user-adjusted item average for item j.
     * 
     * @param i    ignored
     * @param j    item key for which to get user-adjusted average
     * @return     user-adjusted item average for item j
     */
    public float getSim(int i, int j) {
        //System.err.println("myTot[" + j + "]=" +myTot[j] + " / myCount[j]=" + myCount[j]);
        AverageModelCell cell = (AverageModelCell) getOrCreateCell(j);
        if (null == cell) {
            return Float.NaN;
        }
        return ((float)cell.total / cell.count);
    }

    public int getNumItems() {
        return myMap.size();
    }

    public int getNumRatings(int itemId) {
        AverageModelCell cell = (AverageModelCell) getOrCreateCell(itemId);
        if (null == cell) {
            return 0;
        }
        return cell.count;
    }

    public static void main(String args[]) throws IOException {
        if (args.length != 1) {
            System.out.println("usage:   java -Xmx1600m -DGL_CONFIG_FILE=/foo/gl.properties org.grouplens.multilens.AverageModel avgmodelfile");
            System.out.println("example: java -Xmx1600m -DGL_CONFIG_FILE=/home/vfac01/dfrankow/windows/work/MultiLens/src/org/grouplens/util/test/gl.properties org.grouplens.multilens.AverageModel avgmodelfile");
            System.exit(1);
        }
        String avgModelFile = args[0];
        String ratingTable = GlConfigVars.getConfigVar("ratingTable", GlConfigVars.MULTILENS);
    
        ZScore myFilt = new ZScore();

        {
            AverageModel newAvg = new AverageModel();

            long sTime = new Date().getTime();
            newAvg.build(false, ratingTable, myFilt);
            System.out.println("Finished building average model: total time = " + ((new Date().getTime()-sTime) / 1000) + " seconds");

            sTime = new Date().getTime();
            newAvg.writeBinaryModel(avgModelFile);
            System.out.println("Finished writing average model: total time = " + ((new Date().getTime()-sTime) / 1000) + " seconds");
        }
    }

    /* (non-Javadoc)
     * @see org.grouplens.multilens.SparseModel#cardinality()
     */
    public int cardinality() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * Clear the model to use anew.
     */
    public void clear() {
        // Clear the map of cells
        myMap = new HashMap();
        maxItem = 0;
        numUsers = 0;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /* (non-Javadoc)
     * @see org.grouplens.multilens.SparseModel#expandRows()
     */
    protected void expandRows() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /* (non-Javadoc)
     * @see org.grouplens.multilens.SparseModel#getColMax()
     */
    public int getColMax() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /* (non-Javadoc)
     * @see org.grouplens.multilens.SparseModel#getNumRows()
     */
    public int getNumRows() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /* (non-Javadoc)
     * @see org.grouplens.multilens.SparseModel#getRow(int)
     */
    protected int getRow(int i) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /* (non-Javadoc)
     * @see org.grouplens.multilens.SparseModel#getRowOrCreate(int)
     */
    public SparseModelRow getRowOrCreate(int rowKey) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /* (non-Javadoc)
     * @see org.grouplens.multilens.SparseModel#getStats()
     */
    public Stats getStats() {
        throw new UnsupportedOperationException("Not implemented");
    }

    /* (non-Javadoc)
     * @see org.grouplens.multilens.SparseModel#insertRow(int, int)
     */
    protected int insertRow(int ip, int rowKey) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /* (non-Javadoc)
     * @see org.grouplens.multilens.SparseModel#printModel(java.io.PrintStream)
     */
    public void printModel(PrintStream out) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /* (non-Javadoc)
     * @see org.grouplens.multilens.SparseModel#rowIterator()
     */
    public SparseModelIterator rowIterator() {
        throw new UnsupportedOperationException("Not implemented");
    }
}

