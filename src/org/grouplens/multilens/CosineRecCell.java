package org.grouplens.multilens;

public class CosineRecCell extends org.grouplens.multilens.SparseCell implements RecommenderCell {
    private float similarity;

    public CosineRecCell() {
        similarity = (float) 0.0;
    }

    public CosineRecCell(float s) {
        similarity = s;
    }

    public CosineRecCell(int s) {
        similarity = (float) ((float)s / 1000.0);
    }

    public float getSimilarity() {
        return similarity;
    }

    public void setSimilarity(float similarity) {
        this.similarity = similarity;
    }

    public String toString() {
        String s = new String();
        s += "sim " + similarity;
        return s;
    }

    /** Compare two cells.
     * @see org.grouplens.multilens.SparseCell#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (null == obj) return false;
        CosineRecCell that = (CosineRecCell) obj;
        return floatEquals(similarity, that.similarity);
    }

    /**
     * Deep copy.
     * @see org.grouplens.multilens.SparseCell#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        CosineRecCell c = new CosineRecCell();
        c.similarity = similarity;
        return c;
    }

}
