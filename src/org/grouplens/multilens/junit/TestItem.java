package org.grouplens.multilens.junit;

import org.grouplens.multilens.Item;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestItem extends TestCase {
    public TestItem(String arg0) {
        super(arg0);
    }

    public void testEquals() {
        //// Test clone, no change
        Item a = new Item(3, 4.0f);
        Item b = new Item(3, 4.0f);
        Item c = new Item(5, 4.0f);

        // Equal id
        assertEquals("equal", a, b);
        assertTrue("compare", 0 == a.compareTo(b));

        // Not same id
        assertTrue("not equal", ! a.equals(c));
        assertTrue("compare #2", 0 > a.compareTo(c));

        // What if id is same, but values different?  I haven't decided, so no test
    }

    public static Test suite() {
        return new TestSuite(TestItem.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
