package org.grouplens.multilens.junit;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

import org.grouplens.multilens.CosineModel;
import org.grouplens.multilens.CosineModelCell;
import org.grouplens.multilens.CosineRecModel;
import org.grouplens.multilens.FileRatingsSource;
import org.grouplens.multilens.SparseModel;
import org.grouplens.multilens.SparseModelRow;
import org.grouplens.multilens.ZScore;

import junit.framework.*;

/**
 * @author dfrankow
 *
 */
public class TestCosineModel extends TestCase {
    /** Number of non-done ModelReaderThread-s */
    int threadCount = 0;
    synchronized void incCount() {
        threadCount++;
        //System.out.println("inc count " + threadCount);
    }
    synchronized void decCount() {
        threadCount--;
        //System.out.println("dec count " + threadCount);
        if (0 == threadCount) {
            notify();
        }
    }

    /**
     * A class to read a model in a thread.
     * 
     * @author dfrankow
     */
    private class ModelReaderThread extends Thread {
        CosineRecModel crm;
        String fname;

        // Don't allow empty constructor
        private ModelReaderThread(){}

        ModelReaderThread(CosineRecModel m, String _fname) {
            crm = m;
            fname = _fname;
            incCount();
        }

        /**
         * Read a model
         * @see java.lang.Runnable#run()
         */
        public void run() {
            try {
                crm.readBinaryModel(fname);
            } catch (IOException e) {
                fail("Exception while reading binary model: " + e);
            }
            decCount();
        }
    }

    /**
     * A class to get rows out of a model in a thread.
     * 
     * @author dfrankow
     */
    private class ModelGetterThread extends Thread {
        CosineRecModel crm;

        // Don't allow empty constructor
        private ModelGetterThread(){}

        ModelGetterThread(CosineRecModel m) {
            crm = m;
            incCount();
        }

        /**
         * Read a model
         * @see java.lang.Runnable#run()
         */
        public void run() {
            TreeSet rowKeys = new TreeSet();
            Iterator iter = crm.rowIterator();

            // Store up the keys
            while (iter.hasNext()) {
                SparseModelRow r = (SparseModelRow) iter.next();
                rowKeys.add(new Integer(r.getKey()));
            }

            // Look for keys for awhile
            for (int i=0; i<100; i++) {
                iter = rowKeys.iterator();
                while (iter.hasNext()) {
                    int key = ((Integer)iter.next()).intValue();
                    assertEquals("row is there", key, crm.getModelRow(key).getKey());
                }
            }

            decCount();
        }
    }

    CosineModel fm1;

    public TestCosineModel(String s) {
        super(s);
    }

    static String rats1 = "src/org/grouplens/multilens/junit/testratings1.txt";
    static String rats2 = "src/org/grouplens/multilens/junit/testratings2.txt";

    /**
     * Assert things that are true from a model made from rats1.
     */
    private void assertRats1(SparseModel cm) {
        // I know these are true:
        SparseModel.Stats s = cm.getStats();
//        cm.printModel(System.out);
        assertEquals("# rows", 5, s.getNumRows());
        assertEquals("# cols", 5, s.getNumCols());
        assertEquals("# ratings", 19, s.getNumCells());
    }

    /**
     * Test that we can build a model from a simple ratings file.
     */
    public void testBuildFromFile() throws IOException {
        CosineModel cm = new CosineModel(100, 100);
        cm.buildFromFile(rats1, null);
        assertRats1(cm);
    }

    /**
     * Test equals operator.
     */
    public void testEquals() throws IOException {
        CosineModel cm = new CosineModel(100, 100);
        assertTrue("empty equals, reflexive", cm.equals(cm));
        CosineModel cm2 = new CosineModel(100, 100);
        assertTrue("empty equals", cm.equals(cm2));

        cm.buildFromFile(rats1, null);
        assertTrue("not equals", !cm.equals(cm2));
        cm2.buildFromFile(rats1, null);
        assertTrue("equals, reflexive", cm.equals(cm));
        assertTrue("equals", cm.equals(cm2));
    }

    /**
     * Test that we can write a model in binary format and read it back in properly.
     */
    public void testReadWriteBinary() throws IOException {
        // Make a model
        CosineModel cm = new CosineModel(100, 100);
        cm.buildFromFile(rats2, null);
        //System.err.println("cm is " + cm.getStats());

        // Write it out
        File t1 = File.createTempFile("model", "bin");
        t1.deleteOnExit();
        cm.writeCosineRecModelBinary(t1.getAbsolutePath(), 0);

        //System.err.println("after write, cm is " + cm.getStats());

        // Read it in
        CosineRecModel crm = new CosineRecModel(100, 100);
        crm.readBinaryModel(t1.getAbsolutePath());
        //System.err.println("crm is " + crm.getStats());
        //crm.printModel(System.err);

        // Test equality with the CosineRecModel(CosineModel) constructor
        CosineRecModel crm2 = new CosineRecModel(cm);

        //System.err.println("crm2 is " + crm2.getStats());
        //crm2.printModel(System.err);

        assertTrue("crm==crm2", crm.equals(crm2));

        {
            // Test that creating crm2 didn't destroy cm
            CosineModel cm2 = new CosineModel(100, 100);
            cm2.buildFromFile(rats2, null);
            assertTrue("didn't destroy", cm.equals(cm2));

            // Test that changing CosineRecModel built from CosineModel 
            // doesn't change CosineModel
            CosineRecModel crm4 = new CosineRecModel(cm2);
            crm4.insertNewCell(10000, 11000, 0.24f);
            assertTrue("didn't destroy #2", cm.equals(cm2));
            assertTrue("!crm==crm4", !crm.equals(crm4));
        }

        // Read 2nd one in and write it out
        CosineModel cm2 = new CosineModel(100, 100);
        cm2.buildFromFile(rats1, null);
        assertRats1(cm2);
        cm2.writeCosineRecModelBinary(t1.getAbsolutePath(), 0);
        
        // Test that crm.readBinaryModel clears out old larger model
        crm2 = new CosineRecModel(cm2);
        crm.readBinaryModel(t1.getAbsolutePath());
        assertTrue("crm2==crm", crm2.equals(crm));

        // Read 2nd one in and test equality.
        CosineRecModel crm3 = new CosineRecModel(100, 100);
        crm3.readBinaryModel(t1.getAbsolutePath());
        assertTrue("crm2==crm3", crm2.equals(crm3));
    }

    public void testBuildAndWrite() throws IOException {
        buildAndWrite(rats1);
        buildAndWrite(rats2);
    }

    public void buildAndWrite(String rats) throws IOException {
        // Build
        CosineModel cm = new CosineModel();
        cm.buildFromFile(rats, new ZScore());
        cm.buildFinal();
        if (rats.equals(rats1)) assertRats1(cm);

        // Write to t1
        File t1 = File.createTempFile("model", "bin");
        t1.deleteOnExit();
        cm.writeCosineRecModelBinary(t1.getAbsolutePath(), 0);

        // Build-and-write to t2
        CosineModel cm2 = new CosineModel();
        FileRatingsSource source = new FileRatingsSource(rats, new ZScore());
        File t2 = File.createTempFile("model", "bin");
        t2.deleteOnExit();
        cm2.buildAndWriteCosineRecModelBinary(source, t2.getAbsolutePath(), 0);

        // Test equality
        CosineRecModel crm = new CosineRecModel();
        crm.readBinaryModel(t1.getAbsolutePath());
        crm.setShowDebug(true);
        CosineRecModel crm2 = new CosineRecModel();
        crm2.readBinaryModel(t2.getAbsolutePath());

        /*
        System.out.println("crm is: ");
        crm.printModel(System.out);
        System.out.println("crm2 is: ");
        crm2.printModel(System.out);
        */

        assertTrue("crm = crm2", crm.equals(crm2));

        // Test that rows are in order, like we want them
        assertTrue("rows in order", CosineRecModel.rowsInOrder(t2.getAbsolutePath(), false));
    }

    /**
     * Take a ratings file, and write out a binary model file.
     * 
     * @param fname
     * @return
     * @throws IOException
     */
    private File writeModelFromFile(String fname) throws IOException {
        CosineModel cm = new CosineModel(100, 100);
        cm.buildFromFile(fname, null);

        // Write it out
        File t1 = File.createTempFile("model", "bin");
        t1.deleteOnExit();
        cm.writeCosineRecModelBinary(t1.getAbsolutePath(), 0);
        return t1;        
    }

    /**
     * Do multi-threaded test for two different ratings files.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    public void testMultithreaded() throws InterruptedException, IOException {
        readBinaryMultithreaded(writeModelFromFile(rats1));
        readBinaryMultithreaded(writeModelFromFile(rats2));
    }

    private synchronized void readBinaryMultithreaded(File f) throws InterruptedException, IOException {
        CosineRecModel crm = new CosineRecModel();
        crm.readBinaryModel(f.getAbsolutePath());
        assertTrue("model will be read row-by-row", CosineRecModel.rowsInOrder(f.getAbsolutePath(), false));
        CosineRecModel crm2 = new CosineRecModel();
        crm2.readBinaryModel(f.getAbsolutePath());
        
        // Test multi-threaded row-by-row read
        final int NUM_THREADS = 10;
        ModelReaderThread r[] = new ModelReaderThread[NUM_THREADS];
        ModelGetterThread g[] = new ModelGetterThread[NUM_THREADS];
        //crm2.setShowDebug(true);
        for (int i=0; i< NUM_THREADS; i++) {
            r[i] = new ModelReaderThread(crm, f.getAbsolutePath());
            r[i].start();
            g[i] = new ModelGetterThread(crm);
            g[i].start();
        }

        // Wait for my baby threads to come back home
        wait();
        assertEquals("threads gone", 0, threadCount);

        // Even when multiple threads were wacking away at crm2,
        // they didn't destroy it
        assertTrue("crm2==crm", crm2.equals(crm));
    }

    /**
     * Test that we throw some exceptions if bad things happen while writing.
     */
    public void testWriteBinaryExceptions() {
        boolean ex = false;
        CosineModel cm = new CosineModel(100, 100);
        try {
            cm.writeCosineRecModelBinary("/nosuchdir/no.such.subdir", 0);
        } catch (IOException e) {
            ex = true;
        }
        assertEquals("caught exception", true, ex);
    }

    /**
     * Test the equivalence of building with buildUpper true and false.
     * 
     * A nice side effect is that this is the only test I know of that
     * exercises the 1-level cache of lastidx/lastkey in SparseModel
     * and SparseModelRow, because an upper-triangular-built model has
     * its cells written out in a different order.
     */
    public void testBuildUpper() throws IOException {
        // Build one, buildUpper false
        File t1 = File.createTempFile("model", "bin");
        t1.deleteOnExit();
        {
            CosineModel cm = new CosineModel(100, 100);
            cm.setBuildUpperOnly(false);
            cm.buildFromFile(rats1, null);
    
            // Write it out
            cm.writeCosineRecModelBinary(t1.getAbsolutePath(), 0);
        }

        // Build one, buildUpper true
        File t2 = File.createTempFile("model", "bin");
        t2.deleteOnExit();
        {
            CosineModel cm = new CosineModel(100, 100);
            cm.setBuildUpperOnly(true);
            cm.buildFromFile(rats1, null);
    
            // Write it out-- this is where the other half of the model appears
            cm.writeCosineRecModelBinary(t2.getAbsolutePath(), 0);
        }

        // Read in 1
        CosineRecModel crm = new CosineRecModel(100, 100);
        crm.readBinaryModel(t1.getAbsolutePath());
        // This should be in row order
        assertTrue("Rows in order", CosineRecModel.rowsInOrder(t1.getAbsolutePath(), false));

        // Read in 2
        CosineRecModel crm2 = new CosineRecModel(100, 100);
        crm2.readBinaryModel(t2.getAbsolutePath());
        // This is not in row order
        assertTrue("Rows not in order", !CosineRecModel.rowsInOrder(t2.getAbsolutePath(), false));

        // They'd better be the same
        /*
        System.out.println("crm is: ");
        crm.printModel(System.out);
        System.out.println("crm2 is: ");
        crm2.printModel(System.out);
        */
        
        assertTrue("crm==crm2", crm.equals(crm2));
    }

    //
    // The following tests used to be in "TestFlotModel":
    // They test the basic functioning of SparseModel.
    //

    /** increment a cell four times and check to be sure that 1+1+1+1 = 4 */
    public void testSimpleIncrements() {
        fm1 = new CosineModel(2,2);
        CosineModelCell cmc = (CosineModelCell)fm1.getModelCell(2,2);
        cmc.incCoCount();
        cmc.incCoCount();
        cmc.incCoCount();
        cmc.incCoCount();
        Assert.assertEquals((int)cmc.getCount(),4);
    }

    /** create a very small initial matrix and make sure that expansion works. */
    public void testExtend() {
        fm1 = new CosineModel(2,2);
        ((CosineModelCell)fm1.getModelCell(1,2)).incCoCount();
        ((CosineModelCell)fm1.getModelCell(2,2)).incCoCount();
        ((CosineModelCell)fm1.getModelCell(3,2)).incCoCount();
        ((CosineModelCell)fm1.getModelCell(4,2)).incCoCount();
        ((CosineModelCell)fm1.getModelCell(2,2)).incCoCount();
        Assert.assertEquals((int)((CosineModelCell)fm1.getModelCell(2,2)).getCount(),2);
        Assert.assertEquals((int)((CosineModelCell)fm1.getModelCell(2,2)).getCount(),2);
    }

    /** make sure testExtend works with larger keys */
    public void testBigExtend() {
        fm1 = new CosineModel(2,2);
        ((CosineModelCell)fm1.getModelCell(100,20)).incCoCount();
        ((CosineModelCell)fm1.getModelCell(200,20)).incCoCount();
        ((CosineModelCell)fm1.getModelCell(300,20)).incCoCount();
        ((CosineModelCell)fm1.getModelCell(300,20)).incCoCount();
        ((CosineModelCell)fm1.getModelCell(200,20)).incCoCount();
        Assert.assertEquals((int)((CosineModelCell)fm1.getModelCell(200,20)).getCount(),2);
        Assert.assertEquals((int)((CosineModelCell)fm1.getModelCell(300,20)).getCount(),2);
    }

    /** make a very small initial matrix and add a lot of cells, then increment
     * them all 5 times and make sure that the increment works for all cases.
     */
    public void testReverseExtend() {
        fm1 = new CosineModel(2,2);
        for(int times=0; times < 5; times++) {
            //System.out.println("times = " + times);
            for(int i = 1000; i > 0; i--) {
                CosineModelCell cmc = fm1.getModelCell(i,i);
                cmc.incCoCount();
            }
        }
        for (int i = 1000; i > 0; i--) {
            //System.out.println("i = " + i + " count = " + fm1.getCoCount(i,i));
            CosineModelCell cmc = fm1.getModelCell(i,i);
            Assert.assertEquals((int)cmc.getCount(),5);
        }
    }

    public void testIterator() {
        fm1 = new CosineModel(2,2);
        for(int i = 1; i<10; i++) {
            for(int j = 10; j<=100; j+= 10) {
                CosineModelCell cmc = fm1.getModelCell(i,j);
                cmc.incCoCount();
            }
        }
        int i = 3;
        SparseModelRow r1 = fm1.getModelRow(i);
        Iterator it = r1.iterator();
        for(int j = 10; j<=100;j+=10) {
            int cellIdx = ((Integer)it.next()).intValue();
            // System.out.println("j = " + j + " cellIdx" + cellIdx);
            assertEquals(j, cellIdx);
        }
    }

    public void testWalkMatrix() {
        fm1 = new CosineModel(10,10);
        for(int i = 1; i<=10; i++) {
            for(int j = 1; j<=10; j++) {
                for (int k = 1; k<=j*i; k++) {
                    CosineModelCell cmc = fm1.getModelCell(i,j);
                    cmc.incCoCount();
                }
            }
        }
        int i,j;
        SparseModelRow r1;
        Iterator rmit = fm1.rowIterator();
        while( rmit.hasNext() ) {
            r1 = (SparseModelRow)rmit.next();
            Iterator it = r1.iterator();
            while ( it.hasNext() ) {
                j = ((Integer)it.next()).intValue();
                Assert.assertEquals(r1.getKey()*j, ((CosineModelCell)r1.getCell(j)).getCount());
            }
        }
    }

    public void testSets() {
        fm1 = new CosineModel(2,2);
        for(int i = 1; i<=10; i++) {
            for(int j = 1; j<=10; j++) {
                CosineModelCell cmc = fm1.getModelCell(i,j);
                cmc.setCount(i*j);
            }
        }
        int i,j;
        SparseModelRow r1;
        Iterator rmit = fm1.rowIterator();
        while( rmit.hasNext() ) {
            r1 = (SparseModelRow)rmit.next();
            Iterator it = r1.iterator();
            while ( it.hasNext()) {
                j = ((Integer)it.next()).intValue();
                Assert.assertEquals(r1.getKey()*j, ((CosineModelCell)r1.getCell(j)).getCount());
            }
        }
    }

    public void testDeleteCell() {
        fm1 = new CosineModel(10,10);
        for(int i = 1; i<=10; i++) {
            for(int j = 1; j<=10; j++) {
                for (int k = 1; k<=j*i; k++) {
                    CosineModelCell cmc = fm1.getModelCell(i,j);
                    cmc.incCoCount();
                }
            }
        }
        int i,j;
        i = 1;
        SparseModelRow r1;
        Iterator rmit = fm1.rowIterator();
        while( rmit.hasNext() ) {
            r1 = (SparseModelRow)rmit.next();
            r1.deleteCell(i);
            i++;
        }

        rmit = fm1.rowIterator();
        while( rmit.hasNext() ) {
            r1 = (SparseModelRow)rmit.next();
            Iterator it = r1.iterator();
            while ( it.hasNext() ) {
                j = ((Integer)it.next()).intValue();
                Assert.assertEquals(r1.getKey()*j, ((CosineModelCell)r1.getCell(j)).getCount());
            }
        }

    }

    public static Test suite() {
/*
        TestSuite suite= new TestSuite();
        suite.addTest(new TestCosineModel("testBuildUpper"));
        return suite;
*/
        return new TestSuite(TestCosineModel.class);
    }
    public static void main(String args[]) { 
        junit.textui.TestRunner.run(suite());
    }
}
