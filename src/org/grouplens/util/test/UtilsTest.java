package org.grouplens.util.test;


import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.grouplens.util.Utils;

/**
 * @author dfrankow
 */
public class UtilsTest extends TestCase {
    public void testThrowableToString() {
        String s = Utils.throwableToString(new Exception("Here's an exception"));
        assertTrue(s.length() > 0);
        // A stack trace has " at .... foo.java:103 "
        // so this should match
        assertTrue(s.indexOf(".java:") > -1);
        //System.err.println("throwableToString returns " + s);
    }

    public static Test suite() {
        return new TestSuite(UtilsTest.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
