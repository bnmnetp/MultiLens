package org.grouplens.multilens;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Mar 6, 2003
 * Time: 2:15:21 PM
 * To change this template use Options | File Templates.
 */
public class SVDRec implements Recommender {
    private ItemModel myModel;
    private HashMap newItems;
    private TreeSet myTopN;


    public SVDRec(ItemModel aModel) {
        myModel = aModel;
    }

    public Prediction ipredict(User u, int i) {
        float sim = myModel.getSim(Integer.parseInt(u.getUserId())-1,i-1);
        float pred = sim + u.getAverage();

        Prediction p = new Prediction(pred);
        p.setItemID(i);
        p.setUserID(Integer.parseInt(u.getUserId()));
        return p;
    }

    public TreeSet getRecs(User u, int numrecs, boolean includeRated, HashSet fset) {
        TreeSet myRecs = new TreeSet();
        TreeSet retRecs = new TreeSet();
        System.out.println("Number of items = " + ((SVDModel)myModel).getNumItems());
        for(int i = 1; i< ((SVDModel)myModel).getNumItems(); i++) {
            Prediction p = ipredict(u,i);
            myRecs.add(new Recommendation(p.getItemID(),p.getPred(),p.getPred()));
        }
        // Now we have a huge tree but we only want to return the first numrecs.
        Iterator it = myRecs.iterator();
        int i = 0;
        while (it.hasNext() && i < numrecs) {
            retRecs.add(it.next());
            i++;
        }
        return retRecs;

    }

}
