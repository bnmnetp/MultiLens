/*
 * Created on May 19, 2003
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package org.grouplens.multilens.junit;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import org.grouplens.util.GlConfigException;
import org.grouplens.util.GlConfigVars;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class TestGlConfigVars extends TestCase {
    public TestGlConfigVars(String s) {
        super(s);
    }

    public void testReadFile() throws GlConfigException, IOException {
        GlConfigVars.setShowDebug(false);

        // Ensure this test loads this config file
        GlConfigVars.unloadConfig();

        StringWriter sw = new StringWriter();
        BufferedWriter bw = new BufferedWriter(sw);
        bw.write("var1=value");
        bw.newLine();
        bw.write("subcat1.var1=value2");
        bw.newLine();
        bw.close();
        sw.close();
        
        GlConfigVars.loadConfig(new ByteArrayInputStream(sw.toString().getBytes()));
        
        assertEquals("basic get", "value", GlConfigVars.getConfigVar("var1"));
        assertEquals("subcat get", "value2", GlConfigVars.getConfigVar("var1", "subcat1"));
        assertEquals("nonexistent subcat get",
                     "value", GlConfigVars.getConfigVar("var1", "nonexistentcat"));

        // Ensure that future tests load a better config file
        GlConfigVars.unloadConfig();
    }

    public static Test suite() {
        return new TestSuite(TestGlConfigVars.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
