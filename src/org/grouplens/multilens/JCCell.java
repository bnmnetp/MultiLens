package org.grouplens.multilens;

public class JCCell extends SparseCell {
    private int count;

    public void setSimilarity(int s) {
        count = s;
    }

    public int getSimilarity() {
        return count;
    }

    public void incCoCount() {
        count++;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int s) {
        count = s;
    }

    public String toString() {
        String s = new String();
        s += "count " + count;
        return s;
    }

    /** Compare two cells.
     * @see org.grouplens.multilens.SparseCell#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (null == obj) return false;
        JCCell that = (JCCell) obj;
        return count == that.count;
    }

    /**
     * Deep copy.
     * @see org.grouplens.multilens.SparseCell#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        JCCell c = new JCCell();
        c.count = count;
        return c;
    }
}
