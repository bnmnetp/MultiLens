package org.grouplens.multilens.junit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.grouplens.multilens.CosineRecCell;
import org.grouplens.multilens.CosineRecModel;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestCosineRecModel extends TestCase {
    public TestCosineRecModel(String arg0) {
        super(arg0);
    }

    /**
     * Test getCellOrCreate()
     *
     */
    public void testGetCellOrCreate() {
        CosineRecModel m = new CosineRecModel();

        // Create a cell with 0
        CosineRecCell cell = m.getCellOrCreate(2, 2);
        float delta = 0.00001f;
        assertEquals("has cell with 0", 0.0f, cell.getSimilarity(), delta);
        
        // Verify it's the same cell gotten through other means
        cell.setSimilarity(0.3f);
        cell = (CosineRecCell) m.getModelRow(2).getCell(2);
        assertEquals("same cell", 0.3f, cell.getSimilarity(), delta);

        // No cell if not created
        cell = (CosineRecCell) m.getModelRow(2).getCell(3);
    }

    /** Test that this bug is no longer true:
     * 
     * "Right now if you don't initialize the CosineRecModel with some parms
     *  read will die due to a null rowMap."
     **/
    public void testEmptyConstructor() throws IOException {
        CosineRecModel m = new CosineRecModel();
        m.insertNewCell(1, 1, 4f);
        m.insertNewCell(2, 2, 8f);
        File t1 = File.createTempFile("model", "bin");
        t1.deleteOnExit();
        m.writeBinaryModel(t1.getAbsolutePath());

        CosineRecModel m2 = new CosineRecModel();
        m2.readBinaryModel(t1.getAbsolutePath());
        assertTrue("m==m2", m.equals(m2));
    }

    /**
     * Test that we throw an exception if bad things happen while reading.
     *
     */
    public void testReadBinaryExceptions() throws IOException {
        boolean ex = false;
        CosineRecModel crm = new CosineRecModel();
        try {
            crm.readBinaryModel("/doesnt.exist");
        } catch (IOException e) {
            ex = true;
        }
        assertEquals("doesn't exist", true, ex);

        File t1 = File.createTempFile("test", "txt");
        BufferedWriter bw = new BufferedWriter(new FileWriter(t1));
        bw.write("foo");
        bw.close();
        // Now t1 has some stuff that is unlikely to be what readBinaryModel wants
        ex = false;
        try {
            crm.readBinaryModel(t1.getAbsolutePath());
        } catch (IOException e1) {
            ex = true;
        }
        assertEquals("read bogus file", true, ex);
        t1.delete();
    }

    public static Test suite() {
        return new TestSuite(TestCosineRecModel.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
