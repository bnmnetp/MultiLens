package org.grouplens.multilens.junit;

import org.grouplens.util.test.UtilsTest;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 *
 * Group together all the unit tests into one suite.
 */
public class AllTests extends TestCase {
    public AllTests(String arg0) {
        super(arg0);
    }

    public static Test suite() {
        TestSuite suite = new TestSuite();

        suite.addTest(TestAdaptiveRec.suite());
        suite.addTest(TestAverageModel.suite());
        suite.addTest(TestCosine.suite());
        suite.addTest(TestCosineModel.suite());
        suite.addTest(TestCosineModelCell.suite());
        suite.addTest(TestCosineRec.suite());
        suite.addTest(TestCosineRecCell.suite());
        suite.addTest(TestCosineRecModel.suite());
        suite.addTest(TestDBRatingsSource.suite());
        suite.addTest(TestFileRatingsSource.suite());
        suite.addTest(TestGlConfigVars.suite());
        suite.addTest(TestItem.suite());
        suite.addTest(TestJCCell.suite());
        suite.addTest(TestJustCountModel.suite());
        suite.addTest(TestRecommendation.suite());
        suite.addTest(TestSparseModel.suite());
        suite.addTest(TestSparseModelRow.suite());
        suite.addTest(TestUser.suite());
        suite.addTest(UtilsTest.suite());
        return suite;
    }

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }
}

