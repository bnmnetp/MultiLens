package org.grouplens.multilens.junit;

/*
 * Created by IntelliJ IDEA.
 * org.grouplens.multilens.User: bmiller
 * Date: Jul 1, 2002
 * Time: 11:26:23 AM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */

import java.io.IOException;

import junit.framework.*;
import org.grouplens.multilens.*;

public class TestCosine extends junit.framework.TestCase{
    public TestCosine(String s) {
        super(s);
    }

    public void testDummy() {
        // This test exists so that even if all other tests are commented out,
        // there will be a test in here, and the junit framework won't fail an assert
    }

    public void XtestBuild() throws IOException {
        JustCountModel foo;
        foo = new JustCountModel();
        foo.buildFromFile("/home/bmiller/Projects/org.grouplens.multilens.jre.servlet.Jrec/testratings.dmp",null);
        Assert.assertEquals((int)foo.getDBRepSimScore(1,9),2);
        Assert.assertEquals((int)foo.getDBRepSimScore(1,92),3);
        Assert.assertEquals((int)foo.getDBRepSimScore(1,92),3);
        Assert.assertEquals((int)foo.getDBRepSimScore(590,592),1);
        Assert.assertEquals(foo.getDBRepSimScore(2,4),1);
        Assert.assertEquals(foo.getDBRepSimScore(92,1),0);
    }

    public void XtestSimCalc() throws IOException {
        CosineModel foo;
        foo = new CosineModel();
        foo.buildFromFile("/home/bmiller/Projects/org.grouplens.multilens.jre.servlet.Jrec/cosineratings.dat",null);
        Assert.assertEquals(50.0, foo.getSim(2,3), 0.1);
        foo.buildFinal();
        Assert.assertEquals(1.0, foo.getSim(2,3), 0.01);
    }

    public void XtestDBBuild() {
        CosineModel foo;
        foo = new CosineModel();
        foo.build(false,"ratings",null);
        // by my perl program there should be a cooccurnce of 589 for items 17 and 28
        Assert.assertEquals(((CosineModelCell)foo.getModelCell(17,28)).getCount(),589);
    }

    public static Test suite() {
        return new TestSuite(TestCosine.class);
    }

}
