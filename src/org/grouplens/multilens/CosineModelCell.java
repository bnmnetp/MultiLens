package org.grouplens.multilens;

public class CosineModelCell extends SparseCell implements RecommenderCell {
    private float partialDot;
    private float lenI;
    private float lenJ;
    private int count;

    public float getSimilarity() {
        return getPartialDot();
    }

    public float getPartialDot() {
        return partialDot;
    }

    public void setPartialDot(float partialDot) {
        this.partialDot = partialDot;
    }

    public float getLenI() {
        return lenI;
    }

    public void setLenI(float lenI) {
        this.lenI = lenI;
    }

    public float getLenJ() {
        return lenJ;
    }

    public void setLenJ(float lenJ) {
        this.lenJ = lenJ;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void incCoCount() {
        this.count++;
    }

    public String toString() {
        String s = new String();
        s += "lenI " + lenI + " lenJ " + lenJ + " count " + count + " partialDot " + partialDot;
        return s;
    }

    /**
     * Compare two cells.
     * @see org.grouplens.multilens.SparseCell#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (null == obj) return false;
        CosineModelCell that = (CosineModelCell) obj;

        if (!floatEquals(partialDot, that.partialDot)) {
            return false;
        }
        if (!floatEquals(lenI, that.lenI)) {
            return false;
        }
        if (!floatEquals(lenJ, that.lenJ)) {
            return false;
        }
        if (count != that.count) {
            return false;
        }
        return true;
    }

    /**
     * Deep copy.
     * @see org.grouplens.multilens.SparseCell#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        CosineModelCell c = new CosineModelCell();
        c.partialDot = partialDot;
        c.lenI = lenI;
        c.lenJ = lenJ;
        c.count = count;
        return c;
    }
}
