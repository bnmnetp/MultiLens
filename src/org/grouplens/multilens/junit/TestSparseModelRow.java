package org.grouplens.multilens.junit;

import org.grouplens.multilens.JCCell;
import org.grouplens.multilens.SparseModelRow;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestSparseModelRow extends TestCase {
    public TestSparseModelRow(String arg0) {
        super(arg0);
    }
    
    public void testClone() throws CloneNotSupportedException {
        //// Test empty clone
        SparseModelRow a = new SparseModelRow(0, 1);
        SparseModelRow b = (SparseModelRow) a.clone();
	assertTrue("same after cloning", a.equals(b));

        JCCell c = new JCCell();
        c.setCount(10);
        int cKey = 30;
        b.insertCell(cKey, c);
	// Different after changing b
        assertEquals("a has 0", 0, a.cardinality());
        assertEquals("b has 1", 1, b.cardinality());
        assertTrue("b has c", c.equals(b.getCell(cKey)));
        
        //// Test clone with stuff
        a.insertCell(cKey, c);
        b = (SparseModelRow) a.clone();
	assertTrue("same after cloning", a.equals(b));
        assertTrue("a has c", c.equals(a.getCell(cKey)));
        assertTrue("b has c", c.equals(b.getCell(cKey)));

        JCCell c2 = new JCCell();
        int c2Key = 40;
        c2.setCount(20);
        b.insertCell(c2Key, c2);
	// a same changing b
        assertEquals("a has 1", 1, a.cardinality());
	// b different after changing b
        assertEquals("b has 2", 2, b.cardinality());
        // b has a's stuff
	// System.out.println("a is " + a + "    b is " + b);
        assertTrue("b has c", c.equals(b.getCell(cKey)));
        // b has extra
        assertTrue("b has c2", c2.equals(b.getCell(c2Key)));
    }

    public static Test suite() {
        return new TestSuite(TestSparseModelRow.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
