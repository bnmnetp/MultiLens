package org.grouplens.multilens;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class CosineRec implements ExplainingRecommender {
    protected ItemModel myModel;
    private CosineRecModel explainModel;
    private HashMap newItems;
    private TreeSet myTopN;
    private boolean explain = false;

    /**
     * Create a nume recommender object using the model given.
     * @param theModel  A pre-created Model
     */
    public CosineRec(ItemModel theModel) {
        myModel = theModel;
    }

    /**
     * Don't have a default constructor.
     * We need the model to recommend.
     */
    private CosineRec() {}

    public TreeSet getRecs(User myUser, int n, boolean includeRated, HashSet filterSet, HashSet like, HashSet notLike) {

        HashMap userRatings = myUser.getAllRatings();
        TreeSet foo = getRecs(like,20,true,filterSet);
        HashMap goodMatches = newItems;
        foo = getRecs(notLike,20,true,filterSet);
        HashMap badMatches = newItems;
        Iterator it = goodMatches.keySet().iterator();
        HashMap newMatches = new HashMap(100);
        while (it.hasNext()) {
            Integer key = (Integer)it.next();
            Float gVal = (Float)goodMatches.get(key);
            if (badMatches.containsKey(key)) {
                Float bVal = (Float)badMatches.get(key);
                gVal = new Float( gVal.floatValue() - bVal.floatValue());
            }
            if (! notLike.contains(key)) {
                newMatches.put(key,gVal);
            }
        }
        TreeSet combinedList = new TreeSet();
        it = newMatches.entrySet().iterator();
        while (it.hasNext()) {
            Object entry = it.next();
            combinedList.add( new Recommendation(((Map.Entry)entry).getKey().toString(),
                    ((Float)((Map.Entry)entry).getValue()).floatValue()));
        }

        return combinedList;
    }


    /**
     * Calculate the best n items given a user id and n.
     * Simple calculation of recommendation goes like this:
     * Get the set of things R that the user has rated
     * Create a list to hold the candidate items and their votes
     * Get the rows from the model the items in R
     *    add new items to the candidate list or increment their votes
     * Find the best n items from the candidate list (not rated by the user)
     * @param myUser
     * @param n
     * @return Best n recommended item ids
     */

    public TreeSet getRecs(User myUser, int n, boolean includeRated, HashSet filterSet) {
        int limit = n;
        TreeSet finalSet = new TreeSet();
        HashMap userRatings = myUser.getGoodItems(4);
        //HashMap userRatings = myUser.getAllRatings();
        TreeSet intSet = getRecs(userRatings.keySet(),n,includeRated,filterSet);
        if (n == 0) {
            return intSet;
        } else {
            Iterator it = intSet.iterator();
            int i = 0;
            while (it.hasNext() && i < n ) {
                finalSet.add(it.next());
                i++;
            }
            return finalSet;
        }
    }

    public TreeSet getRecs(Set marketBasket, int n, boolean includeRated, HashSet filterSet) {
        CosineRecModel tempExplainModel = null;
        if (explain) {
            tempExplainModel = new CosineRecModel();
            explainModel = new CosineRecModel();
        }
        else {
            explainModel = null;
        }

        newItems = new HashMap(100);
        int numItems = myModel.getMaxKey();
        float newItems[] = new float[numItems+1];
        Arrays.fill(newItems,0);
        int candidateKey = 0;
        for (Iterator it = marketBasket.iterator(); it.hasNext(); ) {
          Object key = it.next();
          int rowIx = ((Integer)key).intValue();
          SparseModelRow theRow = myModel.getModelRow(rowIx);
          Iterator rit = null;
          Iterator cit = null;
          if (theRow != null) {
              if(filterSet != null) {
                  rit = theRow.iterator(filterSet);
              } else {
                  rit = theRow.iterator();
                  cit = theRow.cellIterator();
              }
          }
          if (theRow == null) {
              System.out.println("Error: Row not found in model for key = " + key);
          }
          while( theRow != null && rit.hasNext()) {
              //Warning:  We are casting outside the declared Iterator interface
              //but we are doing it for a good cause....
              candidateKey = ((SparseModelRow.SparseRowIterator)rit).inext();
              // If the iterators rit and cit run in parallel, which they should
              // (do) since I wrote them that way, then this should be faster
              // than a getCell lookup!
              float newSim = ((RecommenderCell)cit.next()).getSimilarity();
              newItems[candidateKey] += newSim;
              if (explain) {
                  //System.err.println("EXPLAIN: cell " + candidateKey + ", " + rowIx + " += " + newSim);
                  CosineRecCell cell = tempExplainModel.getCellOrCreate(candidateKey, rowIx);
                  cell.setSimilarity(cell.getSimilarity() + newSim);
              }
          }
        }

        myTopN = new TreeSet();
        float sortme[] = new float[numItems+1];
        for (int i = 0; i<=numItems;i++) {
            sortme[i] = newItems[i];
        }

        if (! includeRated) {
            // Take rated items out of newItems
            Iterator it = marketBasket.iterator();
            while (it.hasNext()) {
                try {
                    Integer key = (Integer)it.next();
                    newItems[key.intValue()] = 0;
                } catch (Exception e) {
                    System.out.println("Error in checking market basket contents. Likely an item/model mismatch");
                }
            }
        }
        Arrays.sort(sortme);
        if (n > sortme.length) {
            n = sortme.length;
        }

        // Copy top n recs from newItems into myTopN
        float limit = sortme[sortme.length - n];
        int j = 0;
        for(int i = 0; i<=numItems;i++) {
            if ((newItems[i] != 0) && (newItems[i] >= limit) && (j < n)) {
                float foo = (float)newItems[i];
                myTopN.add(new Recommendation(i,foo,foo));
                j++;
                
                if (explain) {
                    try {
                        //System.out.println("EXPLAIN: replaceRowOrCreate(get(" + i +")");
                        explainModel.replaceRowOrCreate(tempExplainModel.getModelRow(i));
                    } catch (CloneNotSupportedException e) {
                        // Not sure what to do with this exception yet
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }

        return myTopN;
    }

    public TreeSet getAllPreds(User myUser, int n, boolean includeRated, HashSet filterSet) {
        Iterator rowIt = ((SparseModel)myModel).rowIterator(); //TODO: unify with Rec
        SparseModelRow currentRow;
        TreeSet resultTree = new TreeSet();
        while (rowIt.hasNext()) {
            currentRow = (SparseModelRow)rowIt.next();
            int key = currentRow.getKey();
            Prediction p = ipredict(myUser,key);
            Recommendation r = new Recommendation(key,p.getPred(),p.getPred());
            resultTree.add(r);
        }
        return resultTree;
    }

    /**
     * Return a simple double value for the prediction, given a user and an item.
     * @param myUser
     * @param itemId
     * @return Predicted value for the user based on.
     */
    public float predict(User myUser, int itemId) {
        HashMap myItems = myUser.getAllRatings();
        Iterator it = myItems.keySet().iterator();
        //int MAX_NEIGHBORS = 20;
        SimilarityHeap sHeap = new SimilarityHeap(20);
        float pred = 0;
        float simTot = 0;
        float scoreTot = 0;
        int swapCt = 0;
        while (it.hasNext()) {
            Object key = it.next();
            int iix = ((Integer)key).intValue();
            float sim;
            if (iix < itemId) {
                sim = myModel.getSim(iix, itemId);
            } else {
                sim = myModel.getSim(itemId,iix);
                swapCt++;
            }
            if (! Float.isNaN(sim)) {
                sHeap.insert(sim,iix);
            }
        }
        Iterator sit = sHeap.iterator();
        while(sit.hasNext()) {
            int i = ((Integer)sit.next()).intValue();
            float sim = sHeap.getSimByIndex(i);
            Integer key = new Integer(sHeap.getItembyIndex(i));
            float rating = ((Float)myItems.get(key)).floatValue() - myUser.getAverage();
            scoreTot += sim * rating;
            simTot += Math.abs(sim);
        }
        //System.out.println("Swapped indexes " + swapCt + " times");
        pred = scoreTot / simTot + myUser.getAverage();
        return pred;
    }

    /**
     * Like predict(), but
     * 
     * return a Prediction instead of a float;
     * additionally call p.setAverageSim(rawSimTot/rct), p.setCcount(rct),
     *  p.setItemID(), p.setUserID()
     * 
     * @param myUser
     * @param itemId
     */
    public Prediction ipredict(User myUser, int itemId) {
        // Get the 20 ratings of myUser that are most similar to itemId 
        SimilarityHeap sHeap = new SimilarityHeap(20);
        HashMap myItems = myUser.getAllRatings();
        Iterator it = myItems.keySet().iterator();
        //int swapCt = 0;
        while (it.hasNext()) {
            Object key = it.next();
            int iix = ((Integer)key).intValue();
            float sim;
            if (iix < itemId) {
                sim = myModel.getSim(iix, itemId);
            } else {
                sim = myModel.getSim(itemId,iix);
                //swapCt++;
            }
            if (! Float.isNaN(sim)) {
                sHeap.insert(sim,iix);
            }
        }

        float scoreTot = 0;
        float simTot = 0;
        float rawSimTot = 0;
        int rct = 0;
        Iterator sit = sHeap.iterator();
        while(sit.hasNext()) {
            int i = ((Integer)sit.next()).intValue();
            float sim = sHeap.getSimByIndex(i);
            Integer key = new Integer(sHeap.getItembyIndex(i));
            float rating = 0;
            try {
                rating =  ((Float)myItems.get(key)).floatValue() - myUser.getAverage();
            } catch (Exception e) {
                if (myItems == null) {
                    System.out.println("Error: myItems is null");
                }
                if (myUser == null) {
                    System.out.println("Error: myUser is null");
                }
                if (rating == 0) {
                    System.out.println("error no key in my items called " + key);
                }
            }
            /*
            System.err.println("CosineRec.ipredict(): user " + myUser
                + " rating " + rating + " key " + key + " score " + (sim*rating)
                + " sim " + sim + " rct " + rct
                + " myRat " + ((Float)myItems.get(key)).floatValue());
            */
            scoreTot += sim * rating;
            simTot += Math.abs(sim);
            rawSimTot += sim;
            rct++;
        }

        //System.out.println("Swapped indexes " + swapCt + " times");
        float pred = scoreTot / simTot + myUser.getAverage();
        /*
        System.err.println("CosineRec.ipredict(): pred for user "
            + myUser + " item " + itemId + ": "
            + " scoreTot (" + scoreTot + ") / simTot (" + simTot
            + ") + user-average " + myUser.getAverage() + "= " + pred);
        */
        Prediction p = new Prediction(pred);
        p.setAverageSim(rawSimTot/rct);
        p.setCcount(rct);
        p.setItemID(itemId);
        p.setUserID(-1);
        try {
            p.setUserID( Integer.parseInt(myUser.getUserId()) );
        }
        catch (Exception e) {
            // For now, there's a mismatch: users have string ids,
            // predictions and recommendations have int userids.
            // This exception says that we got a string id that's
            // not an integer.  TODO: Bad, should be fixed.
        }

       //p.setRating(((Integer)myItems.get(new Integer(itemId))).intValue());
        return p;
    }

    /**
     * Get an explanation for getRecs().
     * @see org.grouplens.multilens.ExplainingRecommender#getExplanation()
     */
    public CosineRecModel getExplanation() {
        return explainModel;
    }

    /** Set explain flag.
     * @see org.grouplens.multilens.ExplainingRecommender#setExplain(boolean)
     */
    public void setExplain(boolean explain) {
        this.explain = explain;
    }
}
