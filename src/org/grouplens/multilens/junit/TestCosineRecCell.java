package org.grouplens.multilens.junit;

import org.grouplens.multilens.CosineRecCell;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestCosineRecCell extends TestCase {
    public TestCosineRecCell(String arg0) {
        super(arg0);
    }
    
    public void testClone() throws CloneNotSupportedException {
        //// Test clone, no change
        CosineRecCell a = new CosineRecCell();

        // Test setters and getters
        float d = 0.0000001f;

        a.setSimilarity(0.3f);
        assertEquals("dot", 0.3f, a.getSimilarity(), d);
        CosineRecCell b = (CosineRecCell) a.clone();
        assertTrue("same after cloning", a.equals(b));

        //// Test change doesn't affect original
        b.setSimilarity(0.6f);
        assertTrue("different", !a.equals(b));
    }

    public static Test suite() {
        return new TestSuite(TestCosineRecCell.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
