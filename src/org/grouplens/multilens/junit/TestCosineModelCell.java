package org.grouplens.multilens.junit;

import org.grouplens.multilens.CosineModelCell;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestCosineModelCell extends TestCase {
    public TestCosineModelCell(String arg0) {
        super(arg0);
    }
    
    public void testClone() throws CloneNotSupportedException {
        //// Test clone, no change
        CosineModelCell a = new CosineModelCell();
        a.setCount(0);

        // Test setters and getters
        float d = 0.0000001f;

        a.setCount(3);
        assertEquals("co", 3, a.getCount());
        a.incCoCount();
        assertEquals("inc", 4, a.getCount());
        a.setPartialDot(0.3f);
        assertEquals("dot", 0.3f, a.getPartialDot(), d);
        a.setLenI(0.2f);
        assertEquals("i", 0.2f, a.getLenI(), d);
        a.setLenJ(0.1f);
        CosineModelCell b = (CosineModelCell) a.clone();
	assertTrue("same after cloning", a.equals(b));

        //// Test change doesn't affect original
        b.incCoCount();
	assertTrue("different", !a.equals(b));
    }

    public static Test suite() {
        return new TestSuite(TestCosineModelCell.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
