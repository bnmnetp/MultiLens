package org.grouplens.multilens;

import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Feb 11, 2003
 * Time: 11:25:01 AM
 * To change this template use Options | File Templates.
 */
public class UserRec implements Recommender {
    ItemModel myModel;

    public UserRec(ItemModel myModel) {
        this.myModel = myModel;
    }

    public Prediction ipredict(User myUser, int itemId) {
        HashMap myItems = myUser.getAllRatings();
        int userId = Integer.parseInt(myUser.getUserId());
        float pred = 0;
        float simTot = 0;
        float scoreTot = 0;
        int swapCt = 0;
        int rct = 0;
        float sim = 0;

        SimilarityHeap sHeap = findBestNeighbors(userId, myUser);
        // Now use the neighbors to calculate a prediction
        Iterator sit = sHeap.iterator();
        while(sit.hasNext()) {
            int i = ((Integer)sit.next()).intValue();
            sim = sHeap.getSimByIndex(i);
            Integer key = new Integer(sHeap.getItembyIndex(i));
            User nbr = User.getInstance(key.toString());
            float nRat = 0;
            nRat = nbr.getRating(new Integer(itemId));
            if (nRat > 0.0)
            scoreTot += ((nRat - nbr.getAverage()) / nbr.getStdev()) * sim;
            simTot += sim;
            rct++;
        }

        //System.out.println("Swapped indexes " + swapCt + " times");
        pred = myUser.getAverage() + (myUser.getStdev()*(scoreTot / simTot));
        Prediction p = new Prediction(pred);
        p.setAverageSim(simTot/rct);
        p.setCcount(rct);
        p.setItemID(itemId);
        p.setUserID( Integer.parseInt(myUser.getUserId()) );
        //p.setRating(((Integer)myItems.get(new Integer(itemId))).intValue());
        return p;

    }

    private SimilarityHeap findBestNeighbors(int userId, User myUser) {
        float sim;
        SparseModelRow myrow = myModel.getModelRow(userId);
        Iterator it = null;
        SimilarityHeap sHeap = new SimilarityHeap(25);

        if (! myUser.hasNbrhood()) {
            try {
                it = myrow.iterator();
            } catch (Exception e) {
                System.out.println("User: " + myUser.getUserId() + " has no row");
                e.printStackTrace();  //To change body of catch statement use Options | File Templates.
                //return null;
            }
            // figure out who our best 25 neighbors are
            while (it.hasNext()) {
                Object key = it.next();
                int iix = ((Integer)key).intValue();
                sim = myModel.getSim(userId,iix);
                if (! Float.isNaN(sim)) {
                    sHeap.insert(sim,iix);
                }
            }
            myUser.setNbrhood(sHeap);
        } else {
            sHeap = myUser.getNbrhood();
        }
        return sHeap;
    }

    public TreeSet getRecs(User u, int numrecs, boolean includeRated, HashSet fset) {
        HashMap myItems = u.getAllRatings();
        int userId = Integer.parseInt(u.getUserId());
        HashMap candidateList = new HashMap();

        SimilarityHeap sHeap = findBestNeighbors(userId,u);

        //Create a candidate list of possible items to recommend.
        Iterator sit = sHeap.iterator();
        while (sit.hasNext()) {
            int i = ((Integer)sit.next()).intValue();
            Integer key = new Integer(sHeap.getItembyIndex(i));
            User nbr = User.getInstance(key.toString());
            candidateList.putAll(nbr.getAllRatings());
        }
        // Now calculate predictions for the candidates keep the best n on a heap
        SimilarityHeap recHeap = new SimilarityHeap(numrecs);
        Iterator it = candidateList.keySet().iterator();
        while (it.hasNext()) {
            Integer key = (Integer)it.next();
            if (! u.getAllRatings().containsKey(key)) {
                Prediction p = ipredict(u,key.intValue());
                recHeap.insert(p.getPred(),key.intValue());
            }
        }
        // Move the best n items off the heap to a TreeSet
        TreeSet recSet = new TreeSet();
        Iterator rit = recHeap.iterator();
        while(rit.hasNext()) {
            int i = ((Integer)rit.next()).intValue();
            float sim = recHeap.getSimByIndex(i);
            int itemId = recHeap.getItembyIndex(i);
            recSet.add(new Recommendation(itemId,sim,sim));
        }
        return recSet;
    }

    public TreeSet getAllPreds(User myUser, int n, boolean includeRated, HashSet filterSet) {
        Iterator rowIt = ((SparseModel)myModel).rowIterator();
        SparseModelRow currentRow;
        TreeSet resultTree = new TreeSet();
        while (rowIt.hasNext()) {
            currentRow = (SparseModelRow)rowIt.next();
            int key = currentRow.getKey();
            Prediction p = ipredict(myUser,key);
            Recommendation r = new Recommendation(key,p.getPred(),p.getPred());
            resultTree.add(r);
        }
        //prune the full result tree
        TreeSet pruneTree = new TreeSet();
        Iterator treeIt = resultTree.iterator();
        int tc = 0;
        while(treeIt.hasNext() && tc < n) {
            pruneTree.add(treeIt.next());
            tc++;
        }
        return pruneTree;
    }
}
