package org.grouplens.multilens.junit;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;

import org.grouplens.multilens.CosineModel;
import org.grouplens.multilens.CosineRec;
import org.grouplens.multilens.CosineRecModel;
import org.grouplens.multilens.ItemVectorModifier;
import org.grouplens.multilens.Recommendation;
import org.grouplens.multilens.SparseModelRow;
import org.grouplens.multilens.User;
import org.grouplens.multilens.ZScore;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestCosineRec extends TestCase {
    public TestCosineRec(String arg0) {
        super(arg0);
    }

    public static CosineRecModel getCRM(String rats,
        ItemVectorModifier myFilt) throws IOException {
        File t1 = File.createTempFile("model", "bin");
        t1.deleteOnExit();
        {
            CosineModel cm = new CosineModel(100, 100);
            cm.setBuildUpperOnly(true);
            cm.buildFromFile(TestCosineModel.rats1, myFilt);
    
            // Write it out
            cm.writeCosineRecModelBinary(t1.getAbsolutePath(), 0);
        }

        CosineRecModel crm = new CosineRecModel(100, 100);
        crm.readBinaryModel(t1.getAbsolutePath());
        return crm;
    }

    /**
     * Test some basic recommendation stuff
     */
    public void testRecs() throws IOException {
        CosineRecModel crm = getCRM(TestCosineModel.rats1, null);
        CosineRecModel crm2 = getCRM(TestCosineModel.rats1, new ZScore());
        CosineRec cr = new CosineRec(crm);
        CosineRec cr2 = new CosineRec(crm2);
        TreeSet results = null;
        
        // Test that a user with no ratings gets no recs
        // Believe it or not, this test used to fail
        User nullu = new User();
        results = cr.getRecs(nullu, 10, false, null);
        assertEquals("null user", 0, results.size());
        results = cr2.getRecs(nullu, 10, false, null);
        assertEquals("null user 2", 0, results.size());

        // Test that a user with rats gets some recs
        User u2 = new User();
        u2.addRating(1, 5);
        u2.addRating(2, 3);
        u2.addRating(92, 4);
        
        results = cr.getRecs(u2, 10, true, null);
        //System.err.println("RESULTS (all): " + results);
        assertEquals("u2 all", 5, results.size());
        results = cr.getRecs(u2, 10, false, null);
        //System.err.println("RESULTS (select-unrated): " + results);
        assertEquals("u2 select-unrated", 3, results.size());

        results = cr2.getRecs(u2, 10, true, null);
        //System.err.println("RESULTS (all): " + results);
        assertEquals("u2 all 2", 1, results.size());
        results = cr2.getRecs(u2, 10, false, null);
        //System.err.println("RESULTS (select-unrated): " + results);
        assertEquals("u2 select-unrated 2", 0, results.size());
    }

    public void testExplain() throws IOException {
        User u = new User();
        //u.addRating(1, 3);
        u.addRating(22, 4);
        
        // Get recs
        CosineRecModel crm = getCRM(TestCosineModel.rats1, null);
        CosineRec cr = new CosineRec(crm);
        TreeSet results = cr.getRecs(u, 10, false, null);
        assertTrue("results not empty", results.size() > 0);

        // Ask for explanation.  It should be empty
        CosineRecModel explainModel = cr.getExplanation();
        assertEquals("empty", null, explainModel);

        // Turn on explain, get recs.  Ask for explanation.
        cr.setExplain(true);
        results = cr.getRecs(u, 10, false, null);
        assertTrue("results not empty", results.size() > 0);
        explainModel = cr.getExplanation();
        // It should have stuff
        assertTrue("empty", explainModel.cardinality() > 0);

        // Check that everything in the recs was in the model
        Iterator iter = results.iterator();
        HashMap foo = new HashMap();
        while (iter.hasNext()) {
            Recommendation r = (Recommendation)iter.next();
            assertTrue("in #2, rec " + r, explainModel.getModelRow(r.getItemID()) != null);
            foo.put(new Integer(r.getItemID()), new Integer(r.getItemID()));
        }

        // Check that everything in the model was in the recs
        iter = explainModel.rowIterator();
        while (iter.hasNext()) {
            SparseModelRow r = (SparseModelRow)iter.next();
            assertTrue("in #1, row " + r.getKey(), foo.containsKey(new Integer(r.getKey())));
        }

        // Turn off explain, get recs
        // Ask for explanation.  It should be empty
        cr.setExplain(false);
        results = cr.getRecs(u, 10, false, null);
        assertTrue("results not empty", results.size() > 0);
        explainModel = cr.getExplanation();
        assertEquals("empty", null, explainModel);
    }

    public static Test suite() {
        return new TestSuite(TestCosineRec.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
