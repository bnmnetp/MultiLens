package org.grouplens.multilens;

/*
 * Created by IntelliJ IDEA.
 * org.grouplens.multilens.User: bmiller
 * Date: Jul 2, 2002
 * Time: 9:54:59 AM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 * TODO: Modify User Class to store ratings as Items -- makes it easier to use true modifiers consistently
 */

import org.grouplens.util.GlConfigVars;

import java.util.*;
import java.util.Date;
import java.sql.*;

/**
 * Class User captures a user and a user's ratings
 */
public class User {
    /** myRatings are ratings that are != -1 */
    private HashMap myRatings;
    /** minus1Ratings are any user ratings that are -1s */
    private Set minus1Ratings;
    /** Average of ratings in myRatings */
    private float average = (float)0.0;
    /** Standard deviation of ratings in myRatings */
    private float stdev = (float)0.0;
    private String userId;
    private int numRatings;
    private boolean stdevCalculated = false;
    private float totRat;
    private static DBConnection myConn = null;
    private Connection conn;

    /**
     * the userDB hashmap contains user objects for all users that we have read in.
     * This hashmap is inteneded for use with the buildFromFile method to create a
     * pseudodatabase of users.
     */
    private static HashMap userDB;
    private static String userDBURL = null;
    private static String userDBuser = null;
    private static String userDBpw = null;
    private static String myRatingTable = null;
    
    private SimilarityHeap nbrhood;

    public SimilarityHeap getNbrhood() {
        return nbrhood;
    }

    public void setNbrhood(SimilarityHeap nbrhood) {
        this.nbrhood = nbrhood;
    }

    public boolean hasNbrhood() {
        boolean retval = false;
        if(nbrhood != null) {
            retval = true;
        }
        return retval;
    }

    /**
     * Create a new User and read in ratings from the database for the User ID.
     * @param userID
     */
    public User(String userID) {
        userDBURL = DBConnection.getDefaultURL();
        userDBuser = DBConnection.getDefaultUser();
        userDBpw = DBConnection.getDefaultPW();
        myRatingTable = GlConfigVars.getConfigVar("ratingTable", GlConfigVars.MULTILENS);

        myRatings = new HashMap();
        minus1Ratings = new TreeSet();
        if (myConn == null) {
            myConn = new DBConnection(userDBURL,userDBuser,userDBpw);
        }
        conn = myConn.getConn();
        numRatings = readRatings(userID);
        userId = userID;
        nbrhood = null;
    }

    /**
     * Create a new user, initialize ratings map, but do not read from the database.
     */
    public User() {
        myRatings = new HashMap();
        minus1Ratings = new TreeSet();
        numRatings = 0;
        totRat = (float)0.0;
        average = (float)0.0;
        nbrhood = null;
    }


    /**
     * Create a new user, get ratings from userItems
     * 
     * @param userItems
     */
    public User(Vector userItems) {
        myRatings = new HashMap(userItems.size());
        minus1Ratings = new TreeSet();
        Iterator it = userItems.iterator();
        while (it.hasNext() ) {
            Item currentItem = (Item)it.next();
            addRating(currentItem.getItemID(), currentItem.getRating());
        }
        nbrhood = null;
    }

    private void calcSD() {
        Iterator it = myRatings.entrySet().iterator();
        float sumDiff = 0;
        while (it.hasNext() ) {
            Float currentItem = (Float)((Map.Entry)it.next()).getValue();
            float f = (currentItem.floatValue() - average);
            sumDiff += f * f;
        }
        stdev = (float)Math.sqrt( sumDiff / (numRatings - 1));
    }


    /**
     *
     * @return the name of the database table that contains user ratings
     */
    public static String getMyRatingTable() {
        return myRatingTable;
    }

    /**
     * Set the database table.
     * @param myRatingTable
     * The table should be defined as follows:
     *  CREATE TABLE ratings (
     *  gl_id int(11) DEFAULT '0' NOT NULL,
     *  mlid bigint(20) DEFAULT '0' NOT NULL,
     *  rating int(11) DEFAULT '0' NOT NULL,
     *  UNIQUE u_i_pair(gl_id,mlid)
     *  );
     */
    public static void setMyRatingTable(String myRatingTable) {
        User.myRatingTable = myRatingTable;
    }

    public static String getUserDBURL() {
        return userDBURL;
    }

    public static void setUserDBURL(String userDBURL) {
        User.userDBURL = userDBURL;
    }

    public static String getUserDBuser() {
        return userDBuser;
    }

    public static void setUserDBuser(String userDBuser) {
        User.userDBuser = userDBuser;
    }

    public static String getUserDBpw() {
        return userDBpw;
    }

    public static void setUserDBpw(String userDBpw) {
        User.userDBpw = userDBpw;
    }

    /**
     * Get the userID for this user.
     * @return userID
     */
    public String getUserId() {
        return userId;
    }

    /**
     * Set the userid for this user
     * @param userId
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     *
     * @return How many items has the user rated (that are != -1)?
     */
    public int getNumRatings() {
        return numRatings;
    }

    /**
     * The average of all rating values >= 0, undefined if user has no ratings.
     * @return The average of all rating values >= 0, undefined if user has no ratings.
     */
    public float getAverage() {
        return average;
    }

    /**
     * The standard deviation of all rating values >= 0, undefined if user has no ratings.
     * @return The standard deviation of all rating values >= 0, undefined if user has no ratings.
     */
    public float getStdev() {
        if (!stdevCalculated) {
            calcSD();
            stdevCalculated = true;
        }
        return stdev;
    }

    /**
     * Read in all a users ratings.   This function is called by the constructor User(userName).
     * @param userID
     * @return number of ratings read in from the database.
     */
    public int readRatings(String userID) {
        userId = userID;
        boolean done = false;

        while (! done) {
            if (conn == null) {
                myConn = new DBConnection(userDBURL,userDBuser,userDBpw);
                conn = myConn.getConn();
            }
            try {

                // Select the desired user ratings
                String SQLStmt;
                SQLStmt = "SELECT movieId,rating  FROM " + myRatingTable
                                    + " WHERE userId = " + userID
                            + " AND ((rating > 0) OR (rating = -1))";

                //System.out.println("SQL SELECT Statement: " + SQLStmt);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(SQLStmt);
                Integer mlid;
                Float rating;
                float ratTot = (float)0.0;
                while (rs.next()) {
                    mlid = new Integer(rs.getInt("movieId"));
                    rating = new Float(rs.getFloat("rating"));
                    addRating(mlid.intValue(), rating.floatValue());
                }
                stmt.close();
                done = true;
            }
            catch (SQLException E) {
                System.err.println("SQLException in readRatings: " + E.getMessage());
                System.err.println("SQLState:     " + E.getSQLState());
                System.err.println("VendorError:  " + E.getErrorCode());
                System.err.println("resetting connection to mysql");
                conn = null;
            }
        }

        return numRatings;
    }

    /**
     *
     * @return A hashmap containing all ratings for this user that were not -1s.
     */
    public HashMap getAllRatings() {
        return myRatings;
    }

    /**
     *
     * @return A Set containing all items for this user that were rated -1.
     */
    public Set getMinus1Ratings() {
        return minus1Ratings;
    }

    public Vector getRatingVector() {
        Vector ratingVec = new Vector(myRatings.size());
        Iterator it = myRatings.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer)it.next();
            Float val = (Float)myRatings.get(key);
            ratingVec.add(new Item(key.intValue(),val.floatValue()));
        }
        return ratingVec;
    }

    /**
    * Add a rating for this user.
    * 
    * @param itemID
    * @param rating
    */
    public void addRating(int itemID, float rating) {
        if (rating == -1) {
            minus1Ratings.add(new Integer(itemID));
        } else { 
            myRatings.put(new Integer(itemID), new Float(rating));
            numRatings++;
            totRat += rating;
            if (numRatings > 0) {
                average = totRat / (float)numRatings;
            }
        }
        stdevCalculated = false;
    }    

    /**
     * Delete a rating in this users set of ratings.
     * Useful when you are creating test cases.
     * TODO: Doesn't currently work with -1 ratings.
     * 
     * @param itemID
     */
    public void deleteRating(int itemID) {
        float rating = ((Float)myRatings.get(new Integer(itemID))).floatValue(); 
        myRatings.remove(new Integer(itemID));
        numRatings--;
        totRat -= rating;
        average = totRat / (float)numRatings;
        stdevCalculated = false;
    }

    /**
     * Get a user's rating for an item (if not -1).
     * @param item
     * @return user's rating
     */
    public float getRating(Integer item) {
        Float dRat = (Float)myRatings.get(item);
        float foo = (float)-1.0;
        if (dRat != null) {
            foo = dRat.floatValue();
        }
        return foo;
    }

    /**
     * Add a User to the internal User cache.
     *
     * @param cUser
     */
    public static void addUserToCache(User cUser) {
        if (userDB == null) {
            userDB = new HashMap(5000);
        }
        if (! userDB.containsKey(cUser.getUserId())) {
            userDB.put(cUser.getUserId(),cUser);
        }
    }

    public static void resetCache() {
        userDB = null;
    }
    /**
     * Get an instance of a user with the given userid.
     *
     * This method is primarily set to be used when you are testing using files rather than a database.
     * Although any time you want to add users to the userDB cache you can use this method to possibly avoid
     * the cost of a database access.
     *
     * @param uid
     * @return the User
     */
    public static User getInstance(String uid) {
        // uh, this looks *very* wrong.
        // if (userDB != null ) {
        //    return (User)userDB.get(uid);
        // }
        if (userDB != null ) {
           User cachedUser = (User)userDB.get(uid);
           if(cachedUser != null)
             return cachedUser;
        }
        return new User(uid);
    }

    //TODO good/bad can be rewritten in terms of Set interface only.
    /**
     * Get a HashMap of 'good' items.  Good is defined as any item with a rating >= threshold
     * @param threshold
     * @return HashMap
     */
    public HashMap getGoodItems(int threshold) {
        Iterator it = myRatings.keySet().iterator();
        HashMap goodMap = new HashMap(50);
        while (it.hasNext()) {
        Integer key = (Integer)it.next();
        Float value = (Float)myRatings.get(key);
            if (value.intValue() >= threshold) {
                goodMap.put(key,value);
            }
        }
        return goodMap;
    }

    /**
     * Get a HashMap of 'bad' items.  Bad is defined as any item with a rating <= threshold
     * @param threshold
     * @return HashMap
     */
    public HashMap getBadItems(int threshold) {
        Iterator it = myRatings.keySet().iterator();
        HashMap badMap = new HashMap(50);
        while (it.hasNext()) {
        Integer key = (Integer)it.next();
        Float value = (Float)myRatings.get(key);
            if (value.intValue() <= threshold) {
                badMap.put(key,value);
            }
        }
        return badMap;

    }

    /**
     * This method selects one good item and deletes it from the list.  The key for the item is returned.
     * This method is mainly used for setting up test cases.
     * @return  Key for item deleted
     */
    public Integer deleteOneGood() {
        Random r = new Random();
        HashMap goodMap = getGoodItems(3);
        int d = r.nextInt(goodMap.size());
        Iterator it = goodMap.keySet().iterator();
        int i = 0;
        while (it.hasNext() && i < d) {
            i++;
            it.next();
        }
        Integer key = (Integer)it.next();
        myRatings.remove(key);
        return key;
    }

    /**
     * Test driver.
     * @param args
     */
    public static void main(String args[]) {
        Date d1 = new Date();
        Random r = new Random();
        int tcount = 0;
        long total_time = 0;
        while(tcount  <100) {
            int user = r.nextInt(122000);
            String userID = Integer.toString(user);
            d1 = new Date();
            long sTime = d1.getTime();
            User foo = new User();
            int ct = foo.readRatings(userID);
            d1 = new Date();
            long fTime = d1.getTime();
            if (ct > 0) {
                //System.out.println("Create user time = " + (fTime-sTime) );
                total_time += (fTime-sTime);
                tcount++;
            }
        }
        float avg_time = (float)total_time / (float)100.0;
        System.out.println("Average Time to retrieve a user = " + avg_time);

    }

    /** String representation for debugging.
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getUserId();
    }
}
