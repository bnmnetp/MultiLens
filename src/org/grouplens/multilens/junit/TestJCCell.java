package org.grouplens.multilens.junit;

import org.grouplens.multilens.JCCell;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestJCCell extends TestCase {
    public TestJCCell(String arg0) {
        super(arg0);
    }

    public void testEquals() throws CloneNotSupportedException {
        //// Test clone, no change
        JCCell a = new JCCell();
        a.setCount(2);
        
        JCCell b = new JCCell();
        b.setCount(2);
        
        assertTrue("equals", a.equals(b));
        
        b.incCoCount();
        
        assertTrue("not equals", !a.equals(b));
    }

    public void testClone() throws CloneNotSupportedException {
        //// Test clone, no change
        JCCell a = new JCCell();
        a.setCount(0);

        // Test setters and getters
        float d = 0.0000001f;

        a.setCount(3);
        assertEquals("co", 3, a.getCount());
        a.incCoCount();
        assertEquals("inc", 4, a.getCount());

        JCCell b = (JCCell) a.clone();
	assertTrue("same after cloning", a.equals(b));

        //// Test change doesn't affect original
        b.incCoCount();
	assertTrue("different", !a.equals(b));
    }

    public static Test suite() {
        return new TestSuite(TestJCCell.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
