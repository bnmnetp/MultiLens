package org.grouplens.multilens;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.DoubleFactory2D;
import cern.colt.matrix.linalg.Algebra;
import cern.colt.function.IntIntDoubleFunction;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Mar 6, 2003
 * Time: 9:52:15 AM
 * To change this template use Options | File Templates.
 */
public class SVDModel implements ItemModel {
    DoubleMatrix2D sMat;
    DoubleMatrix2D uMat;
    DoubleMatrix2D vMat;
    DoubleMatrix2D pseudoUsers;
    DoubleMatrix2D pseudoItems;
    DoubleMatrix2D pseudoItemT;

    public SVDModel() {
    }

    public SVDModel(int k, int m, int n) {
        sMat = DoubleFactory2D.dense.make(k,k);
        uMat = DoubleFactory2D.dense.make(m,k);
        vMat = DoubleFactory2D.dense.make(k,n);
    }

    public int getMaxKey() {
        return pseudoItems.columns();
    }

    void readMatrix(String dataFile, DoubleMatrix2D targetMat) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(dataFile));
        } catch(IOException e) {}
        try {
            String line = in.readLine();
            while(line != null) {                                    // Loop forever
                StringTokenizer tokens = new StringTokenizer(line);
                int userID = Integer.parseInt(tokens.nextToken());
                int itemID = Integer.parseInt(tokens.nextToken());
                double rating = Double.parseDouble(tokens.nextToken());
                targetMat.set(userID, itemID, rating);
                line = in.readLine();
            }
            in.close();
        } catch (IOException e) { System.out.println("IO Problems in build"); }

    }

    void readOctaveMatrix(String dataFile, DoubleMatrix2D targetMat, int rows, int cols) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(dataFile));
        } catch(IOException e) {}
        try {
            String line;
            // The first five lines are comments -- eat them
            for(int i = 0; i< 5; i++) {
                 line = in.readLine();
            }
            for(int i = 0; i < rows; i++) {
                line = in.readLine();
                StringTokenizer tokens = new StringTokenizer(line);
                for(int j = 0; j< cols; j++) {
                    double rating = Double.parseDouble(tokens.nextToken());
                    targetMat.set(i,j, rating);
                }
            }
            in.close();
        } catch (IOException e) { System.out.println("IO Problems in build"); }

    }

    public void read(String path, String sFile, String uFile, String vFile) {
        readMatrix(path+"/"+sFile, sMat);
        System.out.println("Dimensions of sMat = "+sMat.rows() +" , " + sMat.columns());
        readMatrix(path+"/"+uFile, uMat);
        System.out.println("Dimensions of uMat = "+uMat.rows() +" , " + uMat.columns());
        readMatrix(path+"/"+vFile, vMat);
        System.out.println("Dimensions of vMat = "+vMat.rows() +" , " + vMat.columns());
    }

    public void readOctave(String path, int m, int n, int k, String uFile, String vFile) {
        readOctaveMatrix(path+"/"+uFile, uMat,m,k);
        System.out.println("Dimensions of uMat = "+uMat.rows() +" , " + uMat.columns());
        readOctaveMatrix(path+"/"+vFile, vMat,k,n);
        System.out.println("Dimensions of vMat = "+vMat.rows() +" , " + vMat.columns());
    }

    public void prepareModel() {
        class MyIntInt implements IntIntDoubleFunction {
            public double apply(int i, int j, double v) {
                return Math.sqrt(v);
            }
        }
        Algebra a = new Algebra();

/*        MyIntInt sqrt = new MyIntInt();
        sMat.forEachNonZero(sqrt);
        Algebra a = new Algebra();
        pseudoUsers = a.mult(uMat,sMat);
        System.out.println("Dimensions of pseudoUsers = "+pseudoUsers.rows() +" , " + pseudoUsers.columns());
        pseudoItems = a.mult(sMat,vMat);
        System.out.println("Dimensions of pseudoItems = "+pseudoItems.rows() +" , " + pseudoItems.columns());
        pseudoItemT = a.transpose(pseudoItems);
        System.out.println("Dimensions of pseudoItemT = "+pseudoItemT.rows() +" , " + pseudoItemT.columns()); */
        pseudoUsers = uMat;
        pseudoItems = vMat;
        pseudoItemT = a.transpose(vMat);
    }

    // Functions required to implement the ItemModel interface
    public SparseModelRow getModelRow(int i) {
        return null;
    }

    public float getSim(int i, int j) {
        Algebra a = new Algebra();
        double dot = (float)0.0;
        dot =  a.mult(pseudoUsers.viewRow(i), pseudoItemT.viewRow(j));

        return (float)dot;
    }
    public int getNumItems () {
        return pseudoItems.columns();
    }
}
