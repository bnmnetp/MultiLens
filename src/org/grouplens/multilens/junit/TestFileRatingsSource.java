package org.grouplens.multilens.junit;

import java.io.IOException;

import org.grouplens.multilens.FileRatingsSource;
import org.grouplens.multilens.RatingsSource;
import org.grouplens.multilens.ZScore;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestFileRatingsSource extends TestCase {
    public void testFileRatingsReader() throws IOException {
        FileRatingsSource reader = new FileRatingsSource(TestCosineModel.rats1, new ZScore());
        RatingsSource.RatingRowIterator iter = reader.user_iterator();
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

    public void testOutOfOrder() {
        boolean gotException = false;
        try {
            FileRatingsSource reader = new FileRatingsSource("src/org/grouplens/multilens/junit/outoforder-rats.txt", null);
        } catch (IOException e) {
            gotException = true;
        }
        assertTrue("out of order caused exception", gotException);
    }

    public static Test suite() {
        return new TestSuite(TestFileRatingsSource.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
