package org.grouplens.multilens.junit;

import java.util.Iterator;
import java.util.TreeSet;

import org.grouplens.multilens.Recommendation;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestRecommendation extends TestCase {
    public TestRecommendation(String arg0) {
        super(arg0);
    }

    public void testEquals() {
        Recommendation r1 = new Recommendation(1, 2.0f);
        Recommendation r2 = new Recommendation(2, 2.0f);
        Recommendation r3 = new Recommendation(2, 2.0f);
        Recommendation r4 = new Recommendation(2, 3.0f);

        assertTrue("not equal", !r1.equals(r2));

        // This test used to break because the signature of equals() was wrong (bug#130)
        assertTrue("equal", r2.equals(r3));

        // Differing on rec value matters
        assertTrue("not equal", !r3.equals(r4));
    }

    public void testCompare() {
        Recommendation r1 = new Recommendation(1, 2.0f, 2.0f);
        Recommendation r2 = new Recommendation(2, 2.0f, 2.0f);
        Recommendation r3 = new Recommendation(3, 3.0f, 3.0f);
        Recommendation r4 = new Recommendation(4, 3.0f, 3.0f);

        // Test that a TreeSet full of these things gets sorted properly
        TreeSet s = new TreeSet();
        s.add(r1);
        s.add(r2);
        s.add(r3);
        s.add(r4);

        // This test used to break because compareTo() was not consistent-with-equals (bug#130)
        assertEquals("4 recs", 4, s.size());

        // Should contain all
        assertTrue("contains r1", s.contains(r1));
        assertTrue("contains r2", s.contains(r2));
        assertTrue("contains r3", s.contains(r3));
        assertTrue("contains r4", s.contains(r4));

        Iterator iter = s.iterator();
        float delta = 0.0000001f;
        assertEquals("r4" , 3.0f, ((Recommendation)iter.next()).getSimilarityScore(), delta);
        assertEquals("r3" , 3.0f, ((Recommendation)iter.next()).getSimilarityScore(), delta);
        assertEquals("r2" , 2.0f, ((Recommendation)iter.next()).getSimilarityScore(), delta);
        assertEquals("r1" , 2.0f, ((Recommendation)iter.next()).getSimilarityScore(), delta);
    }

    public static Test suite() {
        return new TestSuite(TestRecommendation.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
