package org.grouplens.multilens;

import java.util.*;

public class AdaptiveRec implements Recommender {
    /** If this is true, write out some debug info */
    private boolean showDebug = false;

    private ItemModel myModel;
    private AverageModel avgModel;
    private CosineRec cosineRec = null;
    private AverageRec avgRec = null;
    
    /** Don't predict (or send back a no-pred) for items rated <= ratingThresh times */
    private int ratingThresh = 10;
    private float simThresh = (float)0.1;

    /* Shouldn't have this constructor, because we need an average model:
    public AdaptiveRec (ItemModel theModel) {
        super(theModel);
    }
    */

    public AdaptiveRec (ItemModel theModel, AverageModel avm) {
        cosineRec = new CosineRec(theModel);
        avgModel = avm;
        avgRec = new AverageRec(avm);
    }

    public int getRatingThresh() {
        return ratingThresh;
    }

    public void setRatingThresh(int ratingThresh) {
        this.ratingThresh = ratingThresh;
    }

    public float getSimThresh() {
        return simThresh;
    }

    public void setSimThresh(float simThresh) {
        this.simThresh = simThresh;
    }
    

    /**
     * Return a recommendation list based on the following set of rules:
     * 1.  For each item:
     *     If #Ratings > ratingThresh and simScore > simThresh
     *        prediction = normal item prediction (Ala CosineRec)
     *     Else if #Ratings > ratingThresh but simScore <= simThresh then
     *        prediction = adjusted average (ala AverageRec)
     *     Else
     *          prediction = no-pred
     * From the recommendations that we get, we remove the items rated by the user with -1
     * @param myUser
     * @param n
     * @param includeRated
     * @param filterSet
     * @return Recommendations in a TreeSet
     */
    public TreeSet getRecs(User myUser, int n, boolean includeRated, HashSet filterSet) {
        int limit = n; // n is really ignored in this implementation
        TreeSet finalSet = new TreeSet();

        //HashMap userRatings = myUser.getGoodItems(4);
        
        HashMap userRatings = myUser.getAllRatings();
        Set minus1Set = myUser.getMinus1Ratings();
        TreeSet intSet = cosineRec.getRecs(userRatings.keySet(),avgModel.getMaxKey(),includeRated,filterSet);
        Iterator it = intSet.iterator();
        int i = 0;
        int normp =0; int avgp = 0; int nop = 0;
        while (it.hasNext()) {
            Recommendation rec = (Recommendation)it.next();
            int itemId = rec.getItemID();

            //remove the -1 items from the recommendation list
            if (minus1Set.contains(new Integer(itemId))) {
                it.remove();
                continue;
            }

            int numRates = avgModel.getNumRatings(itemId);

            if (numRates > ratingThresh && rec.getSimilarityScore() > simThresh) {
                Prediction p = ipredict(myUser,itemId);
                if (p != null) {
                    rec.setSimilarityScore(p.getHalfStarPred()); // cause this to be sorted by pred!!!
                    rec.setType(Prediction.COSINE_PRED);
                    finalSet.add(rec);
                    normp++;
                }
            } else if (numRates > ratingThresh && rec.getSimilarityScore() <= simThresh) {
                Prediction p = avgRec.ipredict(myUser,itemId);
                rec.setSimilarityScore(p.getHalfStarPred());
                rec.setType(Prediction.AVERAGE_PRED);
                finalSet.add(rec);
                avgp++;
            } else {
                rec.setType(Prediction.NO_PRED);
                nop++;
            }
        }
        if (showDebug) {
            System.out.println("User = " + myUser.getUserId() + " NP = " + normp + " AP = " + avgp + " NP = " + nop);
        }
        return finalSet;
    }

    /**
     * @see org.grouplens.multilens.Recommender#ipredict(org.grouplens.multilens.User, int)
     */
    public Prediction ipredict(User myUser, int itemId) {
        // NOTE: This does NOT do thresholding with simThresh or ratingThresh,
        // which seems wrong, but is currently how things work in MovieLens.
        return cosineRec.ipredict(myUser,itemId);
    }

    /**
     * Turn on or off showing of some debug info
     * @param show
     */
    public void setShowDebug(boolean show) {
        showDebug = show;
    }
}
