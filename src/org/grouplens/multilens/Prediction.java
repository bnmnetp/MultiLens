package org.grouplens.multilens;

/*
 * Created by IntelliJ IDEA.
 * org.grouplens.multilens.User: bmiller
 * Date: Jul 10, 2002
 * Time: 2:49:57 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */

import java.io.PrintStream;

public class Prediction {
    private float pred;
    private float conf;
    private float averageSim;
    private int rating;
    private int ccount;

    public final static int UNKNOWN_TYPE = 0;
    public final static int NO_PRED = 1;
    public final static int AVERAGE_PRED = 2;
    public final static int COSINE_PRED = 3;
    /** NO_PRED, AVERAGE_PRED, COSINE_PRED */
    private int type;

    private int itemID;
    private int userID;

    /**
     * Create a prediction object using pred as the prediction value
     * (returned by getPred()).
     *
     * @param pred
     */
    public Prediction(float pred) {
        this.pred = pred;
        type = UNKNOWN_TYPE;
    }

    public String toString() {
        return "userID " + userID + " itemID " + itemID
            + " pred " + pred + " conf " + conf + " averageSim " + averageSim
            + " rating " + rating + " ccount " + ccount;
    }

    /**
     * Print the contents of the Prediction object.
     *
     * @param out  A PrintStream object to send the output to.
     */
    public void print(PrintStream out) {
        String outstr = "";
        outstr += userID + "\t";
        outstr += itemID + "\t";
        outstr += pred + "\t";
        outstr += rating + "\t";
        outstr += averageSim + "\t";
        outstr += ccount + "\t";
        out.println(outstr);
    }

    public float getHalfStarPred() {
        float leftSide = (float)Math.floor(pred);
        float rightSide = pred - leftSide;
        rightSide = (float)Math.floor(rightSide * 100.0);
        if (rightSide > 25.0 && rightSide < 75.0) {
            rightSide = (float) 0.5;
        } else if ( rightSide <= 25.0) {
            rightSide = (float)0.0;
        } else {
            rightSide = (float)1.0;
        }
        return leftSide + rightSide;
    }

    /**
     * Get the predicted value.
     *
     * @return  calculated prediction.
     */
    public float getPred() {
        return pred;
    }

    /**
     * Set the predicted value.
     *
     * @param pred
     */
    public void setPred(float pred) {
        this.pred = pred;
    }

    /**
     * Get the confidence value for this prediction.  NOTE:  Unimplemented
     * @return the confidence value
     */
    public float getConf() {
        return conf;
    }

    /**
     * Set the confidence factor.  NOTE:  Currently, I have no algorithms that use this.
     *
     * @param conf
     */
    public void setConf(float conf) {
        this.conf = conf;
    }

    /**
     * Get the average similarity score used to calculate this prediciton.
     *
     * @return  Average Similarity
     */
    public float getAverageSim() {
        return averageSim;
    }

    /**
     * Set the average similarity score.  This is currently set by the ipredct method in CosineRec.
     *
     * @param averageSim
     * @see org.grouplens.multilens.CosineRec
     */
    public void setAverageSim(float averageSim) {
        this.averageSim = averageSim;
    }

    /**
     * Get the user's rating for this prediction.  This is currently set by some of the test programs.
     *
     * @return rating value
     */
    public int getRating() {
        return rating;
    }
    /**
     * Set the users rating for this item.
     *
     * @param rating
     */
    public void setRating(int rating) {
        this.rating = rating;
    }

    /**
     * Get the coocurrence count for this item.
     *
     * @return the coocurrence count
     */
    public int getCcount() {
        return ccount;
    }

    /**
     * Set the coocurrence count for this item.
     *
     * @param ccount
     */
    public void setCcount(int ccount) {
        this.ccount = ccount;
    }

    /**
     * Get the itemID for this prediction.
     *
     * @return  itemID.
     */
    public int getItemID() {
        return itemID;
    }

    /**
     * Set the itemID for this prediction.
     *
     * @param itemID
     */
    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    /**
     * Get the user id for this prediction.
     * @return userid
     */
    public int getUserID() {
        return userID;
    }

    /**
     * Set the userid for this prediction.
     *
     * @param userID
     */
    public void setUserID(int userID) {
        this.userID = userID;
    }

    /**
     * @return int NO_PRED, AVERAGE_PRED, COSINE_PRED, UNKNOWN_TYPE
     */
    public int getType() {
        return type;
    }

    /**
     * Return a displayable string for given type.
     * 
     * @param type
     * @return String
     */
    static public String getTypeString(int type) {
        String s = null;
        if (UNKNOWN_TYPE == type) {
            s = "unknown";
        } else if (NO_PRED == type) {
            s = "no_pred";
        } else if (AVERAGE_PRED == type) {
            s = "average";
        } else if (COSINE_PRED == type) {
            s = "cosine";            
        }
        else {
            s = "???";
        }
        return s;
    }

    /**
     * Return a displayable string for this.getType()
     * 
     * @return String
     */
    public String getTypeString() {
        return getTypeString(type);
    }

    /**
     * @param type NO_PRED, AVERAGE_PRED, COSINE_PRED, UNKNOWN_TYPE
     */
    public void setType(int type) {
        this.type = type;
    }
}
