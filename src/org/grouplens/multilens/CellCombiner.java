/*
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Nov 2, 2002
 * Time: 6:58:25 AM
 * To change template for new interface use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.grouplens.multilens;

public interface CellCombiner {
    /**
     * Combine the contents of two cells in a matrix.
     * @param x
     * @param y
     * @return Return combined result
     */
    public float combine(float x, float y);
}
