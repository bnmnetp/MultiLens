package org.grouplens.multilens;

/*
 * Created by IntelliJ IDEA.
 * org.grouplens.multilens.User: bmiller
 * Date: Aug 9, 2002
 * Time: 8:27:00 AM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */

import java.io.Serializable;

public class Recommendation extends java.lang.Object implements Comparable, Serializable {
    private float similarityScore;
    private float prediction;
    private int itemID;

    /** @see Prediction : NO_PRED, AVERAGE_PRED, COSINE_PRED */
    private int type;

    // Don't construct an empty rec
    private Recommendation() {
    }

    /**
     * 
     * @param itemID
     * @param sim
     */
    public Recommendation(String itemID, float sim) {
        this.itemID = Integer.parseInt(itemID);
        this.similarityScore = sim;
    }

    /**
     * 
     * @param itemID
     * @param sim
     * @param pred
     */
    public Recommendation(String itemID, float sim, float pred) {
        this(Integer.parseInt(itemID), sim, pred);
    }

    /**
     * 
     * @param itemID
     * @param sim
     */
    public Recommendation(int itemID, float sim) {
        this.type = Prediction.UNKNOWN_TYPE;
        this.itemID = itemID;
        this.similarityScore = sim;
    }

    /**
     * 
     * @param itemID
     * @param sim
     * @param pred
     */
    public Recommendation(int itemID, float sim, float pred) {
        this(itemID, sim);
        this.prediction = pred;
    }

    public float getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(float similarityScore) {
        this.similarityScore = similarityScore;
    }

    public float getPrediction() {
        return prediction;
    }

    public void setPrediction(float prediction) {
        this.prediction = prediction;
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public static float getHalfStarSim(float similarityScore) {
        float leftSide = (float)Math.floor(similarityScore);
        float rightSide = similarityScore - leftSide;
        rightSide = (float)Math.floor(rightSide * 100.0);
        if (rightSide > 25.0 && rightSide < 75.0) {
            rightSide = (float)0.5;
        } else if ( rightSide <= 25.0) {
            rightSide = 0;
        } else {
            rightSide = 1;
        }
        return leftSide + rightSide;
    }

    // The following two methods implement the Comparable interface

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o){
        Recommendation r = (Recommendation)o;
        //System.out.println("Recommendation.compareTo: compare " + itemID + " and " + r.getItemID());

        int retval = 0;
        float ss1 = similarityScore;
        float ss2 = r.getSimilarityScore();
        if (ss1 < ss2 ) {
            retval = -1;
        } else if (ss1 > ss2) {
            retval = 1;
        } else {
            retval = Float.compare(prediction,r.getPrediction());
            if (0 == retval) {
                // Tie-break to be consistent-with-equals
                retval = getItemID() - r.getItemID();
            }
        }
        // The natural order for recommendation lists is in reverse so...
        return -retval;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (null == o) return false;
        Recommendation r = (Recommendation) o;
        //System.out.println("Recommendation.equals: compare " + itemID + " and " + r.getItemID());
        return (itemID == r.getItemID()) && (similarityScore == r.getSimilarityScore());
    }

    public int hashCode() {
        return itemID;
    }
    
    public String toString() {
        return "" + "itemID " + itemID
            + " sim " + similarityScore
            + " pred " + prediction;
    }

    /**
     * @return recommendation type
     */
    public int getType() {
        return type;
    }

    /**
     * @param type
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * Return a displayable String for this.getType()
     * 
     * @return String
     */
    public String getTypeString() {
        return Prediction.getTypeString(type);
    }
}
