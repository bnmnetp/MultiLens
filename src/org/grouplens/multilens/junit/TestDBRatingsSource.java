package org.grouplens.multilens.junit;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Iterator;

import org.grouplens.multilens.DBConnection;
import org.grouplens.multilens.DBRatingsSource;
import org.grouplens.multilens.FileRatingsSource;
import org.grouplens.multilens.RatingsSource;
import org.grouplens.multilens.ZScore;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestDBRatingsSource extends TestCase {
    public void testEmptyTable() throws SQLException {
        boolean caughtExcep = false;

        try {
            DBRatingsSource reader = new DBRatingsSource(false, null, new ZScore(), false);        
        }
        catch (IllegalArgumentException a) {
            caughtExcep = true;
        }
        assertTrue("throw exception if null table", caughtExcep);
    }

    public void testDBRatingsReader() throws SQLException {
        String testTable = "test_ratings_234";

        // Put some ratings in a table to get out
        DBConnection myConn = new DBConnection();
        PreparedStatement stmt = myConn.getConn().prepareStatement(
            "CREATE TABLE "+testTable+" (" +
              "userId int(11) NOT NULL," +
              "movieId int(11) NOT NULL default '0'," +
              "rating float NOT NULL default '0'," +
              "tstamp timestamp(14) NOT NULL)");
        stmt.executeUpdate();

        boolean noException = true;
        try {
            FileRatingsSource frs = new FileRatingsSource(TestCosineModel.rats1, null);
            Iterator iter = frs.user_iterator();
            stmt = myConn.getConn().prepareStatement("insert into "+testTable+" (userId, movieId, rating, tstamp) values (?, ?, ?, ?)");
            stmt.setInt(4, 0);
            while (iter.hasNext()) {
                RatingsSource.RatingRow row = (RatingsSource.RatingRow) iter.next();
                stmt.setInt(1, row.getId());
                for (int i=0; i<row.size(); i++) {
                    stmt.setInt(2, row.getElementId(i));
                    stmt.setFloat(3, row.getValue(i));
                    stmt.execute();
                }
            }

            // Get ratings out
            DBRatingsSource reader = new DBRatingsSource(false, testTable, new ZScore(), false);
            iter = reader.user_iterator();
            assertTrue("stuff in there", iter.hasNext());
            for (int i=0; i<3; i++) {
                RatingsSource.RatingRow row = (RatingsSource.RatingRow) iter.next();
                //System.out.println("row: " + row);

                // Each row is length 3
                assertEquals("row #" + i, 3, row.size());

                // Check values for user 4
                if (4 == row.getId()) {
                    for (int j=0; j<row.size(); j++) {
                        int id = row.getElementId(j);
                        float value = row.getValue(j);
                        assertTrue("right ids", (9 == id) || (1 == id) || (92 == id));
                        float delta = 0.000001f;
                        assertEquals("right values", 3.0, value, delta);
                        assertFalse("filter applied", Math.abs(row.getValue(j) - row.getModValue(j)) < delta); 
                    }
                }
            }
            assertTrue("no stuff left", !iter.hasNext());
        }
        catch (Exception e) {
            System.err.println("Exception : " + e);
            noException = false;
        }
        finally {
            // Drop the table
            stmt = myConn.getConn().prepareStatement("DROP TABLE "+testTable);
            stmt.execute();
            myConn.dbClose();
        }
        assertTrue("no exceptions", noException);
    }

    public static Test suite() {
        return new TestSuite(TestDBRatingsSource.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
