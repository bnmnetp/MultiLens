package org.grouplens.multilens;


import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.io.*;

public abstract class GenericModel extends SparseModel {
    private static String defaultModelTable = "cooked_model";
    String modelTable = "cooked_model";
    /**
     * minPairs:  What is the minimum number of cocount that a pair of items should have before we include
     * it in the final model written to the database.
     */
    int minPairs = 1;


    /**
     * Create a new SparseModel with specified number of expected rows and columns
     *
     * @param i  Expected number of rows
     * @param j  Expected number of columns
     */
    public GenericModel(int i, int j) {
        super(i,j);
        modelTable = defaultModelTable;
    }

    public GenericModel() {

    }

    public static String getDefaultModelTable() {
        return defaultModelTable;
    }

    public static void setDefaultModelTable(String mt) {
        defaultModelTable = mt;
    }

    /**
     * setModelTable
     *
     * @param mt  Set the name of the database table containing the model
     * The Database table must be defined to have the folowing columns
     * <code>
     * CREATE TABLE cooked_model (  <br>
     *  item_row bigint(20) DEFAULT '0' NOT NULL, <br>
     *  item_col bigint(20) DEFAULT '0' NOT NULL, <br>
     *  sim_score int(11) DEFAULT '0' NOT NULL, <br>
     *  UNIQUE i_i_pair(item_row,item_col) <br>
     *  ); <br>
     * </code>
     */
    public void setModelTable(String mt) {
        modelTable = mt;
    }
    /**
     * Return the name of the database table containing the model to use
     *
     * @return name of the model table.
     */
    public String getModelTable() {
        return modelTable;
    }
    /**
     * Return the current value of minPairs
     * @return minPairs
     */
    public int getMinPairs() {
        return minPairs;
    }
    /**
     * Set the minPairs parameter:  Set this to filter out item pairs that have a low co-count.
     * @param minPairs
     */
    public void setMinPairs(int minPairs) {
        this.minPairs = minPairs;
    }

    /**
     * Read user ratings from a file, and construct the corresponding item-item model.
     * 
     * Current file format:
     * 
     * <pre>
     *    user item rating
     * </pre>
     * 
     * where user is a int, item is an int, and rating is a float, and the tokens
     * are white-space separated.
     * 
     * This method may be used by derived classes by overriding insertUserItems().
     * 
     * Other notes:
     * 
     * By adapting the code for the incremental item-item algorithm it's easy to just
     * build a big global model with all the items for a general purpose recommender.
     * 
     * <pre>
     * TODO:  Convert this to an xml format like the following:
     * &lt; db>
     *    &lt; user>
     *      &lt; item id=itemid rating=rate />
     *    &lt; /user>
     * &lt; /db>
     * </pre>
     *
     * @param dbsource     File that contains ratings
     * @param myFilt       This filter is run on a user's items
     * 
     * @throws IOException
     * @see StringTokenizer#StringTokenizer(String)
     * @see #insertUserItems(Vector)
     * @see ItemVectorModifier
     * 
     * @deprecated
     * @see #build(RatingsSource) 
     * 
    **/
    public void buildFromFile(String dbsource, ItemVectorModifier myFilt) throws IOException {
        build(new FileRatingsSource(dbsource, myFilt),
              true /* cache for backward compatibility */);
    }

    /**
     * Build a model using a RatingsSource.
     * The RatingsSource might come from a file, the database, or somewhere else.
     * 
     * @param source      A RatingsSource
     * @param cacheUsers  If true, cache users in the User cache.
     */
    public void build(RatingsSource source, boolean cacheUsers) {
        Iterator iter = source.user_iterator();
        while (iter.hasNext()) {
            RatingsSource.RatingRow row = (RatingsSource.RatingRow) iter.next();

            // This does the work in a derived class.
            // NOTE: This interface should change if we wish to get more
            // efficient, in order not to create Vector-s of objects.
            // See row.getElements().
            insertUserItems(row.getElements());

            // This caches the user
            if (cacheUsers) {
                User newUser = new User(row.getElements());
                newUser.setUserId("" + row.getId());
                User.addUserToCache(newUser);
            }
        }
    }

    /**
     * Build a model using a RatingsSource.
     * The RatingsSource might come from a file, the database, or somewhere else.
     * 
     * @param source
     */
    public void build(RatingsSource source) {
        build(source, false /* don't cache */);
    }

    /**
     * Build a new model by processing ratings from each user.
     * 
     * @param filterBad  If true, the model will ignore ratings < 3.
     * @param inputTable Use the specified database table as source of ratings
     * @param myFilt Interface to modify users ratings.
     *
     * @deprecated
     * @see #build(RatingsSource) 
     */
    public void build(boolean filterBad, String inputTable, ItemVectorModifier myFilt) {
        try {
            build(new DBRatingsSource(filterBad, inputTable, myFilt, false /* don't showDebug */),
                  true /* cache for backward compatibility */);
        }
        catch (SQLException E) {
            System.err.println("SQLException: " + E.getMessage());
            System.err.println("SQLState:     " + E.getSQLState());
            System.err.println("VendorError:  " + E.getErrorCode());
        }
    }

    /*
     * Build a new model by processing ratings from a random selection of users
     * @param filterBad  If this flag is set, the model will ignore ratings < 3.
     * @param numUsers Number of users to sample.
     * @param inputTable Use the specified database table as source of ratings
     * @param myFilt Interface to modify users ratings.
     * The every user in this model will have one item left out of the model.  The user,item,rating triple left
     * out will be saved to a file.
     */
    // DSF, 6/18/2003: This code may be useful, but output is currently
    // hard-coded to "/home/bmiller/Projects/Jrec/cooked_users_"+numUsers.
    // Thus, some review is probably necessary and whoever wants this function
    // can revive it. 
    /*
    public void sampleBuild(boolean filterBad, int numUsers, String inputTable, ItemVectorModifier myFilt) throws FileNotFoundException {
        Vector currentItems;

        try {
            DBConnection myConn = new DBConnection();
            Vector userPopulation = new Vector(10000);
            Statement xstmt = myConn.getConn().createStatement();

            // Select the desired user ratings
            String SQLStmt = "SELECT distinct gl_id FROM " + inputTable;

            ResultSet rs = xstmt.executeQuery(SQLStmt);
            Integer glid;
            while (rs.next()) {
                glid = new Integer(rs.getInt(1));
                //System.out.println("Adding " + mlid + " : " + rating );
                userPopulation.addElement(glid);
            }

            SQLStmt = "";
            if (filterBad) {
                SQLStmt = "SELECT mlid,rating FROM " + inputTable + " WHERE gl_id = ? AND rating >= 3 ORDER BY mlid";
            } else {
                SQLStmt = "SELECT mlid,rating FROM " + inputTable + " WHERE gl_id = ? ORDER BY mlid";
            }
            PreparedStatement stmt = myConn.getConn().prepareStatement(SQLStmt);
            int itemID;
            Item theItem;
            float rating;
            // Create a vector containing all the ratings for this user.
            if (numUsers == 0) {
                numUsers = userPopulation.size();
            }
            boolean used[] = new boolean[userPopulation.size()];
            Arrays.fill(used,false);
            Random r = new Random();
            PrintStream predOut = null;
            predOut = new PrintStream(new FileOutputStream("/home/bmiller/Projects/Jrec/cooked_users_"+numUsers));
            int u = 0;
            while ( u < numUsers  ) {
                int nextUser = r.nextInt(userPopulation.size());
                while (used[nextUser]) {
                    nextUser = r.nextInt(userPopulation.size());
                }
                used[nextUser] = true;
                // Need to save this user for use in later testing!
                int userId = ((Integer)userPopulation.elementAt(nextUser)).intValue();
                currentItems = new Vector(100);

                stmt.setInt(1,userId);
                rs = stmt.executeQuery();
                int numRows = 0;
                while (rs.next()) {
                    itemID = rs.getInt(1);
                    rating = rs.getFloat(2);
                    numRows++;
                    theItem = new Item(itemID,rating);
                    currentItems.add(theItem);
                }
                if (myFilt != null) {
                    myFilt.filter(currentItems);
                }

                if (numRows > 0) {
                    int testRat = r.nextInt(numRows);
                    Item testItem = (Item)currentItems.remove(testRat);
                    predOut.println(userId + "\t" + testItem.itemID + "\t" + testItem.rating);
                    u++;
                }

                insertUserItems(currentItems);

            }

        }
        catch (SQLException E) {
            System.err.println("SQLException: " + E.getMessage());
            System.err.println("SQLState:     " + E.getSQLState());
            System.err.println("VendorError:  " + E.getErrorCode());
        }


    }
    */

    /**
     * Write a completed model out to the database.  Use the field modelTable as the name of the database table
     * to write to.
      */
    public void writeModel() {
        try {
            DBConnection myConn = new DBConnection();
            // Walk the table and update.
            String SQLStmt;
            int j;
            Iterator rmit = rowIterator();
            Statement stmt = myConn.getConn().createStatement();
            while( rmit.hasNext() ) {
                SparseModelRow r1 = (SparseModelRow)rmit.next();
                Iterator it = r1.iterator();
                while (it.hasNext()) {
                    j = ((Integer)it.next()).intValue();
                    // TODO  We should expose this little check somehow
                    if (((CosineModelCell)r1.getCell(j)).getCount() > minPairs) {
                        SQLStmt = "INSERT INTO " + modelTable + " VALUES (" +
                            r1.getKey() + "," +
                            j + "," +
                            getDBRepSimScore(r1.getKey(),j) + ")";
                        stmt.executeUpdate(SQLStmt);
                    }
                }
            }

        }
        catch (SQLException E) {
            System.err.println("SQLException: " + E.getMessage());
            System.err.println("SQLState:     " + E.getSQLState());
            System.err.println("VendorError:  " + E.getErrorCode());
        }

    }

   /**
    * Initialize a model by reading it from the database.
    * @param trunc  Hown many items from each row in the matrix should we read?  Truncating the model to the trunc most
    * similar items can be a big win in speed, with little cost to getting the best topn.
    * @param addLowerTriangle  We only compute and store the upper triangle of the similarity matrix.  In cases where
    * we cant to do market basket analysis this does not work so good.  If this flag is set we will store identical sim
    * scores, but swap the row and column indexes.  Of course this doubles the internal storage requirements.
    */
    public void readModel(int trunc, boolean addLowerTriangle) {
        try {
            DBConnection myConn = new DBConnection();
            String SQLStmt = "SELECT item_row, item_col, sim_score FROM " + modelTable + " " +
            " WHERE item_row = ? ORDER BY sim_score DESC ";
            if (trunc > 0) {
                SQLStmt += " LIMIT " + trunc;
            }

            PreparedStatement pstmt = myConn.getConn().prepareStatement(SQLStmt);

            Statement smin = myConn.getConn().createStatement();
            ResultSet rsmin = smin.executeQuery("SELECT min(item_row),max(item_row) FROM " + modelTable);
            rsmin.next();
            int firstRow = rsmin.getInt(1);
            int lastRow = rsmin.getInt(2);
            for (int it_row = firstRow; it_row<= lastRow;it_row++) {
                int i,j,ct;
                pstmt.clearParameters();
                pstmt.setInt(1,it_row);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    i = rs.getInt("item_row");
                    j = rs.getInt("item_col");
                    ct = rs.getInt("sim_score");
                    setDBRepSimScore(i, j, ct);
                    if (addLowerTriangle) {
                        setDBRepSimScore(j,i,ct);
                    }
                }
            }

        }
        catch (SQLException E) {
            System.err.println("SQLException: " + E.getMessage());
            System.err.println("SQLState:     " + E.getSQLState());
            System.err.println("VendorError:  " + E.getErrorCode());
        }
    }

    /**
     * Combine a model from the database with an existing model.
     * @param trunc  Hown many items from each row in the matrix should we read?  Truncating the model to the trunc most
     * similar items can be a big win in speed, with little cost to getting the best topn.
     * @param addLowerTriangle  We only compute and store the upper triangle of the similarity matrix.  In cases where
     * we cant to do market basket analysis this does not work so good.  If this flag is set we will store identical sim
     * scores, but swap the row and column indexes.  Of course this doubles the internal storage requirements.
     * @param modelTable
     * @param myOp
     */
    public void combineModel(int trunc, boolean addLowerTriangle,
                             String modelTable, CellCombiner myOp) {
        try {
            DBConnection myConn = new DBConnection();
            String SQLStmt = "SELECT item_row, item_col, sim_score FROM " + modelTable + " " +
                    " WHERE item_row = ? ORDER BY sim_score DESC ";
            if (trunc > 0) {
                SQLStmt += " LIMIT " + trunc;
            }

            PreparedStatement pstmt = myConn.getConn().prepareStatement(SQLStmt);

            Statement smin = myConn.getConn().createStatement();
            ResultSet rsmin = smin.executeQuery("SELECT min(item_row),max(item_row) FROM " + modelTable);
            rsmin.next();
            int firstRow = rsmin.getInt(1);
            int lastRow = rsmin.getInt(2);
            for (int it_row = firstRow; it_row<= lastRow;it_row++) {
                int i,j,ct;
                pstmt.clearParameters();
                pstmt.setInt(1,it_row);
                ResultSet rs = pstmt.executeQuery();
                while (rs.next()) {
                    i = rs.getInt("item_row");
                    j = rs.getInt("item_col");
                    ct = rs.getInt("sim_score");
                    addDBRepSimScore(i, j, ct,myOp);
                    if (addLowerTriangle) {
                        addDBRepSimScore(j,i,ct,myOp);
                    }
                }
            }

        }
        catch (SQLException E) {
            System.err.println("SQLException: " + E.getMessage());
            System.err.println("SQLState:     " + E.getSQLState());
            System.err.println("VendorError:  " + E.getErrorCode());
        }


    }
    /**
     * combine the given model the the current model.
     *
     * Use the given cell combiner to make incorporate one model into another.
     * Should work with any model that is a subclass of GenericModel.
     *
     * @param newModel
     * @param myOp
     */
    public void combineModel(GenericModel newModel, CellCombiner myOp) {
        // Iterate over all the cells in newModel
        // Get their DBRepSimScore
        // calladdDBRepSimScore on the new model.
        int j;
        Iterator rmit = newModel.rowIterator();
        while( rmit.hasNext() ) {
            SparseModelRow r1 = (SparseModelRow)rmit.next();
            Iterator it = r1.iterator();
            while ( it.hasNext() ) {
               j = ((Integer)it.next()).intValue();
               CosineModelCell myCell = (CosineModelCell)r1.getCell(j);
               int rowKey = r1.getKey();
               int newScore;
               newScore = newModel.getDBRepSimScore(rowKey,j);
               this.addDBRepSimScore(rowKey,j,newScore,myOp);
            }
        }

    }

    /**
     * Get and convert the similarity score to an integer.  Each subclass must implement this function to convert from
     * its internal similarity score representation to an integer.  For example the Cosine model stores the similarity
     * internally as a double, but mulitplies by 1000 and rounds before returning. the integer value.  The purpose of
     * this funciton is to provide a model independent way of storing similarity in the database as an integer value.
     * @param i
     * @param j
     * @return An integer representation of the similarity score.
     */
    public abstract int getDBRepSimScore(int i, int j);

    /**
     * Set the similarity score.  Each subclass must implement its own version of this becuase the similarity score
     * may be transformed from the value read from the database into a more complicated internal representation.
     * For example, the cosine model internally stores the similarity score as a Double.
     * @param i
     * @param j
     * @param val
     */
    public abstract void setDBRepSimScore(int i, int j, int val);

    /**
     * Set the similarity score.  Each subclass must implement its own version of this becuase the similarity score
     * may be transformed from the value read from the database into a more complicated internal representation.
     * For example, the cosine model internally stores the similarity score as a Double.
     * @param i
     * @param j
     * @param val
     */
    public abstract void addDBRepSimScore(int i, int j, int val, CellCombiner myOp);

    /**
     * Take a sorted vector of (item,rating) pairs and update the similarity scores in the model.
     * @param userItems
     */
    protected abstract void insertUserItems(Vector userItems);
}
