package org.grouplens.multilens.junit;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Iterator;

import org.grouplens.multilens.CosineRecCell;
import org.grouplens.multilens.SparseModel;
import org.grouplens.multilens.SparseModelRow;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author dfrankow
 */
public class TestSparseModel extends TestCase {
    public TestSparseModel(String arg0) {
        super(arg0);
    }

    public void testRemoveAndPrintModel() {
        SparseModel m = new SparseModel();
        // This used to throw an exception:

        // Put in some rows, each with a diagonal element
        for (int i = 1; i<=3; i++) {
            SparseModelRow row = m.getRowOrCreate(i);
            row.insertCell(i, new CosineRecCell((float)i));
        }
        //m.printModel(System.out);

        int capacity = m.getCapacity();

        // Remove row #2
        Iterator iter = m.rowIterator();
        iter.next();
        iter.next();
        iter.remove();
        
        // Test that we can get row #3 after the remove
        float delta = 0.00001f;
        SparseModelRow row = (SparseModelRow) iter.next();
        assertEquals("r3", 3, row.getKey());
        CosineRecCell c = (CosineRecCell)row.getCell(3);
        assertEquals("r3 c3", 3.0f, c.getSimilarity(), delta);

        // printModel-- this threw an exception once upon a time, due to a bug
        // It would walk over the model, but some rows would be null
        ByteArrayOutputStream bs = new ByteArrayOutputStream(); 
        PrintStream ps = new PrintStream(bs);
        m.printModel(ps);        
        assertTrue("printed something", bs.size() > 0);

        // Test that the correct row was removed, and the others still there
        iter = m.rowIterator();
        row = (SparseModelRow) iter.next();
        assertEquals("r1", 1, row.getKey());
        c = (CosineRecCell)row.getCell(1);
        assertEquals("r1 c1", 1.0f, c.getSimilarity(), delta);
        
        row = (SparseModelRow) iter.next();
        assertEquals("r3", 3, row.getKey());
        c = (CosineRecCell)row.getCell(3);
        assertEquals("r3 c3", 3.0f, c.getSimilarity(), delta);

        assertTrue("no more", !iter.hasNext());
        
        // Now remove another row
        iter = m.rowIterator();
        row = (SparseModelRow) iter.next();
        iter.remove();
        
        // Now check remaining row
        iter = m.rowIterator();
        row = (SparseModelRow) iter.next();
        assertEquals("r3 #2", 3, row.getKey());
        c = (CosineRecCell)row.getCell(3);
        assertEquals("r3 c3 #2", 3.0f, c.getSimilarity(), delta);
        assertTrue("no more #2", !iter.hasNext());
        
        // Now remove the last row
        iter = m.rowIterator();
        row = (SparseModelRow) iter.next();
        iter.remove();
        
        // Now check nothing left
        //m.printModel(System.out);
        iter = m.rowIterator();
        assertTrue("no more #3", !iter.hasNext());

        // getModelRow() needs capacity never to shrink to be thread-safe,
        // see its internal note.
        assertTrue("capacity never shrinks", m.getCapacity() >= capacity);
    }

    public void testRemoveRow() {
        SparseModel m = new SparseModel();
        // This used to throw an exception:

        // Put in some rows, each with a diagonal element
        for (int i = 1; i<=3; i++) {
            SparseModelRow row = m.getRowOrCreate(2*i);
            row.insertCell(2*i, new CosineRecCell((float)2*i));
        }
        //m.printModel(System.out);        

        // Remove row #4
        m.removeRowByKey(4);
        
        // Test that we can get row #6 after the remove
        float delta = 0.00001f;
        SparseModelRow row = (SparseModelRow) m.getModelRow(6);
        assertEquals("r6", 6, row.getKey());
        CosineRecCell c = (CosineRecCell)row.getCell(6);
        assertEquals("r6 c6", 6.0f, c.getSimilarity(), delta);

        // printModel-- this threw an exception once upon a time, due to a bug
        // It would walk over the model, but some rows would be null
        ByteArrayOutputStream bs = new ByteArrayOutputStream(); 
        PrintStream ps = new PrintStream(bs);
        m.printModel(ps);        
        assertTrue("printed something", bs.size() > 0);

        // Test that the correct row was removed, and the others still there
        row = (SparseModelRow) m.getModelRow(2);
        assertEquals("r2", 2, row.getKey());
        c = (CosineRecCell)row.getCell(2);
        assertEquals("r2 c2", 2.0f, c.getSimilarity(), delta);
        
        row = (SparseModelRow) m.getModelRow(6);
        assertEquals("r6", 6, row.getKey());
        c = (CosineRecCell)row.getCell(6);
        assertEquals("r6 c6", 6.0f, c.getSimilarity(), delta);

        assertEquals("no more", 2, m.getNumRows());
        
        // Now remove row # 2
        m.removeRowByKey(2);
        
        // Now check remaining row
        row = (SparseModelRow) m.getModelRow(6);
        assertEquals("r6 #2", 6, row.getKey());
        c = (CosineRecCell)row.getCell(6);
        assertEquals("r6 c6 #2", 6.0f, c.getSimilarity(), delta);
        assertEquals("no more #2", 1, m.getNumRows());

        // Now remove the last row
        m.removeRowByKey(6);
        
        // Now check nothing left
        assertEquals("no more #3", 0, m.getNumRows());
    }


    /**
     * Test replaceRowOrCreate()
     *
     */
    public void testReplaceRowOrCreate() throws CloneNotSupportedException {
        SparseModelRow a = new SparseModelRow(0, 2);
        CosineRecCell myCell = new CosineRecCell(3.0f); 
        a.insertCell(3, myCell);

        SparseModelRow b = new SparseModelRow(0, 2);
        myCell = new CosineRecCell(4.0f);
        b.insertCell(4, myCell);
        
        SparseModel m = new SparseModel();
        m.replaceRowOrCreate(a);

        float delta = 0.000001f;
        assertEquals("a in", ((CosineRecCell)m.getModelRow(2).getCell(3)).getSimilarity(), 3.0f, delta);
        m.replaceRowOrCreate(b);
        
        // a not in
        assertEquals("a not in", null, m.getModelRow(2).getCell(3));

        assertEquals("b in", ((CosineRecCell)m.getModelRow(2).getCell(4)).getSimilarity(), 4.0f, delta);
    }

    public static Test suite() {
        return new TestSuite(TestSparseModel.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
