package org.grouplens.multilens.junit;

import java.io.IOException;

import org.grouplens.multilens.JustCountModel;

import junit.framework.*;

/**
 * @author dfrankow
 *
 */
public class TestJustCountModel extends TestCase {
    public TestJustCountModel(String s) {
        super(s);
    }

    private JustCountModel aModel() {
        JustCountModel m = new JustCountModel();
        assertEquals("empty", 0, m.cardinality());
        m.incCoCount(10, 20);
        m.incCoCount(10, 20);
        m.incCoCount(30, 40);
        m.incCoCount(50, 60);
        m.setCoCount(70, 80, 11);
        m.setCoCount(30, 40, 8);

        return m;
    }

    /**
     * Test that we can set and get things
     */
    public void testBasicAccess() {
        JustCountModel m = aModel();

        float delta = 0.0000001f;

        assertEquals("2 incs",        2f, m.getCoCount(10, 20), delta);
        assertEquals("inc then set",  8f, m.getCoCount(30, 40), delta);
        assertEquals("inc",           1f, m.getCoCount(50, 60), delta);
        assertEquals("set",          11f, m.getCoCount(70, 80), delta);
        assertEquals("not set",       0f, m.getCoCount(99999, 9999), delta);
    }

    /**
     * Test equals operator.
     */
    public void testEquals() {
        JustCountModel cm = aModel();

        assertTrue("empty equals, reflexive", cm.equals(cm));
        JustCountModel cm2 = aModel();
        assertTrue("empty equals", cm.equals(cm2));

        cm.incCoCount(4, 4);
        assertTrue("not equals", !cm.equals(cm2));
        cm2.incCoCount(4, 4);
        assertTrue("equals, reflexive", cm.equals(cm));
        assertTrue("equals", cm.equals(cm2));
    }

    public void testBuild() throws IOException {
        JustCountModel foo;
        foo = new JustCountModel();
        foo.buildFromFile(TestCosineModel.rats2,null);
        Assert.assertEquals(2, (int)foo.getDBRepSimScore(1,9));
        Assert.assertEquals(3, (int)foo.getDBRepSimScore(1,92));
        Assert.assertEquals(3, (int)foo.getDBRepSimScore(1,92));
        Assert.assertEquals(1, (int)foo.getDBRepSimScore(590,592));
        Assert.assertEquals(1, foo.getDBRepSimScore(2,4));
        Assert.assertEquals(0, foo.getDBRepSimScore(17,1407));
    }

    public void XtestFullDB() throws IOException {
        JustCountModel foo;
        foo = new JustCountModel();
        foo.buildFromFile("/home/bmiller/Projects/sortedratings.dmp",null);
        // by my perl program there should be a cooccurnce of 589 for items 17 and 28
        Assert.assertEquals(foo.getDBRepSimScore(17,28),589);
    }

    public void XtestDBBuild() {
        JustCountModel foo;
        foo = new JustCountModel();
        foo.build(false,"ratings",null);
        // by my perl program there should be a cooccurnce of 589 for items 17 and 28
        Assert.assertEquals(foo.getDBRepSimScore(17,28),589);
    }

    public static Test suite() {
        return new TestSuite(TestJustCountModel.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
