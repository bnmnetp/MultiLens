package org.grouplens.multilens;

/**
 * A recommender that has an explanation interface.
 */
public interface ExplainingRecommender extends Recommender {
    /** Get a matrix of items that influenced the last recommendation gotten
     * from getRecs().
     * 
     * NOTE: I'd think this should return "SimilarityModel" that has
     * "SimilarityCell"s in it.  However, "CosineRecModel" and "CosineRecCell"
     * are so close to what is desired (a single float), let's use those instead. 
     */
    public CosineRecModel getExplanation();

    /** Set 'explain' option to true or false.  True will cause future calls
     * to getRecs to populate a data structure that may be retrieved with
     * getExplanation().
     */
    public void setExplain(boolean explain);
}
