package org.grouplens.multilens;

public class UserModelCell extends SparseCell {
    private float similarity;

    public UserModelCell() {
        similarity = (float)0.0;
    }

    public UserModelCell(float s) {
        similarity = s;
    }

    public UserModelCell(int s) {
        similarity = (float)s / (float)1000.0;
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
        UserModelCell that = (UserModelCell) obj;
        return floatEquals(similarity, that.similarity);
    }

    /**
     * Deep copy.
     * @see java.lang.Object#clone()
     */
    public Object clone() throws CloneNotSupportedException {
        UserModelCell c = new UserModelCell();
        c.similarity = similarity;
        return c;
    }

}
