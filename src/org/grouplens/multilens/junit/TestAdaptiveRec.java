package org.grouplens.multilens.junit;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

import org.grouplens.multilens.AdaptiveRec;
import org.grouplens.multilens.AverageModel;
import org.grouplens.multilens.CosineRecModel;
import org.grouplens.multilens.ItemVectorModifier;
import org.grouplens.multilens.Prediction;
import org.grouplens.multilens.Recommendation;
import org.grouplens.multilens.User;
import org.grouplens.multilens.ZScore;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestAdaptiveRec extends TestCase {
    public TestAdaptiveRec(String arg0) {
        super(arg0);
    }

    static public AverageModel getAM(String file, ItemVectorModifier myFilt) throws IOException {
        AverageModel am = new AverageModel();
        am.setAverageRatingThreshold(0);
        am.buildFromFile(file, myFilt);
        return am;
    }

    /**
     * Test some basic recommendation stuff
     */
    public void testRecs() throws IOException {
        CosineRecModel crm = TestCosineRec.getCRM(TestCosineModel.rats1, null);
        AverageModel am = getAM(TestCosineModel.rats1, new ZScore());
        //System.err.println("average model is " + am.toString());
        AdaptiveRec ar = new AdaptiveRec(crm, am);
        final int ratThresh = 1;
        ar.setRatingThresh(ratThresh);
        TreeSet results = null;

        // Test that a user with no ratings gets no recs
        // Believe it or not, this test used to fail
        User nullu = new User();
        results = ar.getRecs(nullu, 10, false, null);
        assertEquals("null user", 0, results.size());

        // Test that a user gets
        // - non-average recs if item rated > ratThresh,
        // - average rec if item rated <= ratThresh
        //
        // For this test, I happen to know
        // - 9 was rated once (<=ratThresh)
        // - 1 rated 3 times (>ratThresh)
        // and 92 can recommend either
        User u2 = new User();
        float rat = 5;
        u2.addRating(92, rat);
        results = ar.getRecs(u2, 10, true, null);
        //System.err.println("RESULTS (all): " + results);
        float epsilon = 0.00001f;
        int pos = 1;
        for (Iterator iter = results.iterator(); iter.hasNext(); pos++) {
            Recommendation r = (Recommendation) iter.next();
            
            assertTrue("9 is below thresh", r.getItemID() != 9);

            if (r.getItemID() == 1) {
                assertEquals("1st on list", 1, pos);
                // Not average
                assertFalse("1", StrictMath.abs(r.getPrediction() - am.getSim(1, 1)) < epsilon);

                // Test exact values
                assertEquals("1 sim", 5.0, r.getSimilarityScore(), epsilon);
                assertEquals("1 pred", 29.0, r.getPrediction(), epsilon);
            }
            else if (r.getItemID() == 2) {
                assertEquals("3rd on list", 3, pos);
                // Test exact values
                assertEquals("2 sim", 5.0, r.getSimilarityScore(), epsilon);
                assertEquals("2 pred", 12.0, r.getPrediction(), epsilon);
            }
            else if (r.getItemID() == 92) {
                assertEquals("2nd on list", 2, pos);
                // Test exact values
                assertEquals("2 sim", 5.0, r.getSimilarityScore(), epsilon);
                assertEquals("2 pred", 25.0, r.getPrediction(), epsilon);
            }
        }

        // Should test above-ratings-thresh, but below-sim-thresh prediction
        // and recommendation.

        // Test below-ratings-thresh prediction from AdaptiveRec.ipredict()
        if (false) {
            // This will succeed when we put thresholding in properly in
            // AdaptiveRec.ipredict()
            Prediction p = ar.ipredict(u2, 9);
            // (User-average adjusted) average
            assertEquals("9 pred type", Prediction.NO_PRED, p.getType());
            // This will fail because ipredict is CosineRec.ipredict() no matter what the threshold:
            assertEquals("9 pred", 0, p.getPred(), epsilon);
            // This will fail because ipredict is CosineRec.ipredict() no matter what the threshold:
            // This will succeed when we put thresholding in properly
            assertEquals("9 sim", 0, p.getAverageSim(), epsilon);
        }

        assertEquals("u2 all", 3, results.size());
        results = ar.getRecs(u2, 10, false, null);
        //System.err.println("RESULTS (select-unrated): " + results);
        assertEquals("u2 select-unrated", 2, results.size());
    }

    public static Test suite() {
        return new TestSuite(TestAdaptiveRec.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
