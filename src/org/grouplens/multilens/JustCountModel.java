package org.grouplens.multilens;

/*
 * Created by IntelliJ IDEA.
 * org.grouplens.multilens.User: bmiller
 * Date: May 18, 2002
 * Time: 4:47:26 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 * Uses the org.grouplens.multilens.jre.SparseCosineModel Class, but really only the cocount parts.
 */


import org.grouplens.multilens.CellCombiner;
import org.grouplens.multilens.GenericModel;
import org.grouplens.multilens.Item;
import org.grouplens.multilens.JCCell;

import java.util.*;

public class JustCountModel extends GenericModel {
    int cooccurThreshold = 0;

    public JustCountModel() {
        super();
    }


    public JustCountModel(int i, int j) {
        super(i,j);
    }

    public int getCooccurThreshold() {
        return cooccurThreshold;
    }

    public void setCooccurThreshold(int cooccurThreshold) {
        this.cooccurThreshold = cooccurThreshold;
    }

    /**
     * The basic functionality of a JustCountModel is to keep track of
     * the number of times an item has cooccurred with another.
     * Incrementing the cooccurrence counter is a fundamental
     * operation for this model.
     *
     * @param i   Row key
     * @param j   Column key
     */
    public void incCoCount(int i, int j) {
        SparseModelRow r = getRowOrCreate(i);
        JCCell myCell = (JCCell)r.getCell(j);
        if (myCell != null) {
            //System.out.println("JustCountModel(" + i + ", " + j + "), cell j=" + j + " currently " + myCell.getCount() + " INC");
            myCell.incCoCount();
        } else {
            myCell = new JCCell();
            //System.out.println("JustCountModel(" + i + ", " + j + "), cell j=" + j + " currently " + myCell.getCount() + " NEW");
            myCell.setCount(1);
            r.insertCell(j,myCell);
        }
    }

    public void setCoCount(int i, int j, int val) {
        SparseModelRow r = getRowOrCreate(i);
        JCCell myCell = (JCCell)r.getCell(j);
        if (myCell != null) {
            myCell.setCount(val);
        } else {
            myCell = new JCCell();
            myCell.setCount(val);
            r.insertCell(j,myCell);
        }
    }

    /**
     * Get the cooccurrence count from a cell in the matrix.
     *
     * @param i   Row key
     * @param j   Column key
     * @return count, or 0 if no such cell exists
     */
    public float getCoCount(int i, int j) {
        SparseModelRow r = getModelRow(i);
        int retval = 0;
        if (null != r) {
            JCCell myCell = (JCCell)r.getCell(j);
            if (myCell != null) {
                retval = myCell.getCount();
            }
        }
        return retval;
    }

    /**
     * Take a sorted vector of user items and add them to the model.
     * @param userItems This should be a vector of Integers.
     * The integers represent the item ids for each item the current user has rated.
     *
     */
    protected void insertUserItems(Vector userItems) {
        int limit = userItems.size();
        Item itemI, itemJ;
        for(int i = 0; i< limit; i++) {
            itemI = (Item)userItems.elementAt(i);
            for (int j = 0; j< limit; j++) {
                //System.out.println("Inserting: " + userItems.elementAt(i) + "," + userItems.elementAt(j));
                itemJ = (Item)userItems.elementAt(j);
                if (itemI.getRating() > cooccurThreshold && itemJ.getRating() > cooccurThreshold) {
                    incCoCount(itemI.itemID,itemJ.itemID);
                }
            }
        }

    }

    /**
     * Update:  Given a user id and a new item rated by that user, incorporate this new item into the model along with
     * All of the previously rated items by this user.
     * @param user
     * @param item
     */
    public void update(int user, int item) {

    }

    public int getDBRepSimScore(int i, int j) {
        return (int)getCoCount(i,j);
    }

    public void setDBRepSimScore(int i, int j, int val) {
        setCoCount(i, j, val);
    }

    public void addDBRepSimScore(int i, int j, int val, CellCombiner myOp) {
        throw new UnsupportedOperationException("No support for combining models in JustCountModel class");
    }

}
