/*
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Aug 16, 2002
 * Time: 7:37:55 AM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.grouplens.multilens;
/**
 * SparseCell is an abstract class.  The class serves as a parent for implementations of
 * real sparse cells.
 *
 * @see org.grouplens.multilens.CosineRecCell
 * @see org.grouplens.multilens.CosineModelCell
 * @see org.grouplens.multilens.JCCell
 */
public abstract class SparseCell {
    /** Compare two floats for equality in a cell */
    boolean floatEquals(float f1, float f2) {
        return f1 == f2;
    }
    
    // Force these methods to be overloaded

    /** Print the good stuff inside the cell.
     * @see java.lang.Object#toString()
     */
    public abstract String toString();

    /** Compare two cells.
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public abstract boolean equals(Object obj);

    /**
     * Deep copy.
     * @see java.lang.Object#clone()
     */
    protected abstract Object clone() throws CloneNotSupportedException;
}
