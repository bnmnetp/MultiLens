package org.grouplens.multilens;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Apr 29, 2003
 * Time: 7:15:15 PM
 * To change this template use Options | File Templates.
 */
public class UserItemHybridRec implements Recommender {
    ItemModel myModel;

    public UserItemHybridRec(ItemModel theModel) {
        this.myModel = theModel;
    }

    public Prediction ipredict(User u, int i) {
        return null;
    }

    /**
     * Use a hybrid item-item and User-Item method to predict.
     * <ol>
     * <li>  myModel is a user-user matrix, we will use this to find the user's neighborhood.
     * <li>  We will build a small item-item model incorporating the ratings from the users
     *       in the neighborhood
     * <li>  We will use the small model to generate the recommendation list.
     * </ol>
     * @param u
     * @param numrecs
     * @param includeRated
     * @param fset
     * @return Recommendations in a TreeSet
     */
    public TreeSet getRecs(User u, int numrecs, boolean includeRated, HashSet fset) {
        SimilarityHeap myNbrs = findBestNeighbors(Integer.parseInt(u.getUserId()),u );
        CosineModel cosModel = new CosineModel(500,500);
        Iterator it = myNbrs.iterator();
        while(it.hasNext()) {
            Integer userId = (Integer)it.next();
            User currentUser = User.getInstance(userId.toString());
            Vector urv = currentUser.getRatingVector();
            ItemComparator myComp = new ItemComparator();
            Collections.sort(urv,myComp);
            ItemVectorModifier myFilt = new Normalizer();
            if (myFilt != null) {
                myFilt.filter(urv);
            }
            cosModel.insertUserItems(urv);
        }
        cosModel.buildFinal();
        CosineRec cosRec = new CosineRec(cosModel);

        return cosRec.getRecs(u,numrecs,includeRated,fset);
    }

    private SimilarityHeap findBestNeighbors(int userId, User myUser) {
        float sim;
        SparseModelRow myrow = myModel.getModelRow(userId);
        Iterator it = null;
        SimilarityHeap sHeap = new SimilarityHeap(35);

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

}
