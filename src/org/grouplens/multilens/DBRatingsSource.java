package org.grouplens.multilens;

import java.io.PrintStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.Vector;

/**
 * @author dfrankow
 */
public class DBRatingsSource extends RatingsSource {
    private boolean showDebug = false;

    /**
     * @param filterBad  If true, the model will ignore ratings < 3.
     * @param inputTable Use the specified database table as source of ratings
     * @param myFilt Interface to modify users ratings.
     * @param showDebug  true means to print various messages to System.out
     * 
     * @throws SQLException
     */
    public DBRatingsSource(
        boolean filterBad,
        String inputTable,
        ItemVectorModifier myFilt,
        boolean showDebug) throws SQLException {
        if (null == inputTable) {
            throw new IllegalArgumentException("DBRatingsSource: filename passed in was null");
        }

        this.showDebug = showDebug;
        Vector currentItems;

        //
        // Read in all users. This is simplest, and works well as long as
        // a) All the ratings fit in memory
        // b) A client usually wants to read all the ratings
        //
        DBConnection myConn = new DBConnection();
        Vector userPopulation = new Vector(10000);
        Statement xstmt = myConn.getConn().createStatement();

        // Select the desired user ratings
        long t1 = new Date().getTime();
        String SQLStmt = "SELECT distinct userId FROM " + inputTable;

        ResultSet rs = xstmt.executeQuery(SQLStmt);
        Integer glid;
        while (rs.next()) {
            glid = new Integer(rs.getInt(1));
            userPopulation.addElement(glid);
        }
        rs.close();
        long t2 = new Date().getTime();
        if (showDebug) {
            System.out.println(
               (t2 - t1) / 1000 + " seconds to get the list of users.");
        }

        SQLStmt = "";
        if (filterBad) {
            SQLStmt =
                "SELECT movieId,rating FROM "
                    + inputTable
                    + " WHERE userId = ? AND rating >= 3 ORDER BY movieId";
        } else {
            SQLStmt =
                "SELECT movieId,rating FROM "
                    + inputTable
                    + " WHERE userId = ? and rating > 0 ORDER BY movieId";
        }
        PreparedStatement stmt = myConn.getConn().prepareStatement(SQLStmt);
        int itemID;
        Item theItem;
        float rating;
        // Create a vector containing all the ratings for this user.
        int u = 0;
        for (u = 0; u < userPopulation.size(); u++) {
            Integer currentUser = (Integer) userPopulation.elementAt(u);
            currentItems = new Vector(100);
            stmt.setInt(1, currentUser.intValue());
            rs = stmt.executeQuery();
            while (rs.next()) {
                itemID = rs.getInt(1);
                rating = rs.getFloat(2);
                theItem = new Item(itemID, rating);
                currentItems.add(theItem);
            }
            if (myFilt != null) {
                myFilt.filter(currentItems);
            }
            userRatingMap.put(currentUser, currentItems);
            if (((u % 10000) == 0) && (u > 0)) {
                writeDiffLine(System.out, u, t2);
            }
        }

        writeDiffLine(System.out, u, t2);
    }

    /**
     * @see org.grouplens.multilens.RatingsSource#user_iterator()
     */
    public RatingRowIterator user_iterator() {
        return new RatingsSource.MapRatingRowIterator();
    }

    private void writeDiffLine(PrintStream pw, int num, long t1) {
        if (showDebug) {
            long t3 = new Date().getTime();
            long diff = (t3 - t1) / 1000;
            if (0 == diff) {
                // Avoid divide by zero
                diff = 1;
            }
            long totalMemory = Runtime.getRuntime().totalMemory() / 1024 / 1024;
            long freeMemory = Runtime.getRuntime().freeMemory() / 1024 / 1024;
            long usedMemory = totalMemory - freeMemory;

            pw.println(
                new Date() +
                ": Read user "
                    + num
                    + ", "
                    + diff
                    + " seconds ("
                    + (num / diff)
                    + " users/s), "
                    + " total memory "
                    + totalMemory
                    + "M  free memory "
                    + freeMemory
                    + "M  used memory "
                    + usedMemory
                    + "M");
        }
    }
}
