package org.grouplens.multilens.junit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.grouplens.multilens.AverageModel;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestAverageModel extends TestCase {
    public TestAverageModel(String arg0) {
        super(arg0);
    }

    public void testBuildFromFile() throws IOException {
        AverageModel am = new AverageModel();
        // This is a small example-- allow everything to have an average rating 
        am.setAverageRatingThreshold(0);

        am.buildFromFile(TestCosineModel.rats1, null);

        assertEquals("has 5 items", 5, am.getNumItems());
        int dummy = -99;
        float delta = 0.000001f;

        float u2 = (5+3+4)/3f;
        float u4 = (3+3+3)/3f;
        float u204 = (3+3+4)/3f;
        
        float avg1 = (5-u2+3-u4+3-u204)/3;
        float avg2 = (3-u2+3-u204)/2f;
        float avg9 = (3-u4)/1f;
        float avg22 = (4-u204)/1f;
        float avg92 = (4-u2+3-u4)/2f;
        assertEquals("1 avg", avg1, am.getSim(dummy, 1), delta);
        assertEquals("2 avg", avg2,     am.getSim(dummy, 2), delta);
        assertEquals("9 avg", avg9,     am.getSim(dummy, 9), delta);
        assertEquals("22 avg", avg22,    am.getSim(dummy, 22), delta);
        assertEquals("92 avg", avg92,  am.getSim(dummy, 92), delta);

        assertTrue("nonexistent", Float.isNaN(am.getSim(dummy, 9092)));

        // Test that items with # ratings < threshold are -Inf
        // Also test that buildFromFile works a 2nd time properly
        // (this used to fail)
        am.setAverageRatingThreshold(2);
        am.buildFromFile(TestCosineModel.rats1, null);
        assertEquals("1 avg", avg1, am.getSim(dummy, 1), delta);
        assertEquals("2 avg", avg2,     am.getSim(dummy, 2), delta);
        assertEquals("9 avg", Float.NEGATIVE_INFINITY,     am.getSim(dummy, 9), delta);
        assertEquals("22 avg", Float.NEGATIVE_INFINITY,    am.getSim(dummy, 22), delta);
        assertEquals("92 avg", avg92,  am.getSim(dummy, 92), delta);

        // Should also test that a ratings filter works ..
    }

    /**
     * Test that we throw some exceptions if bad things happen while writing.
     */
    public void testWriteBinaryExceptions() {
        boolean ex = false;
        AverageModel am = new AverageModel();
        try {
            am.writeBinaryModel("/doesnt.exist/foo/bar");
        } catch (IOException e) {
            ex = true;
        }
        assertEquals("caught exception", true, ex);
    }
    
    /**
     * Test that we throw an exception if bad things happen while reading.
     *
     */
    public void testReadBinaryExceptions() throws IOException {
        boolean ex = false;
        AverageModel am = new AverageModel();
        try {
            am.readBinaryModel("/doesnt.exist/foo/bar");
        } catch (IOException e) {
            ex = true;
        }
        assertEquals("doesn't exist", true, ex);

        File t1 = File.createTempFile("test", "txt");
        BufferedWriter bw = new BufferedWriter(new FileWriter(t1));
        bw.write("foo");
        bw.close();
        // Now t1 has some stuff that is unlikely to be what readBinaryModel wants
        ex = false;
        try {
            am.readBinaryModel(t1.getAbsolutePath());
        } catch (IOException e1) {
            ex = true;
        }
        assertEquals("read bogus file", true, ex);
        t1.delete();
    }


    public static Test suite() {
        return new TestSuite(TestAverageModel.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
