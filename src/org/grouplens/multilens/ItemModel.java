package org.grouplens.multilens;

/**
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Dec 24, 2002
 * Time: 8:07:18 AM
 * To change this template use Options | File Templates.
 */
public interface ItemModel {
    public SparseModelRow getModelRow(int i);

    public float getSim(int i, int j);

    public int getMaxKey();
}
