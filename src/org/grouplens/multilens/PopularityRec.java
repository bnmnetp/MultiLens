package org.grouplens.multilens;

import java.util.TreeSet;
import java.util.HashSet;

/**
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Apr 7, 2003
 * Time: 2:17:23 PM
 * To change this template use Options | File Templates.
 */
public class PopularityRec implements Recommender {
    ItemModel myModel;

    public PopularityRec(ItemModel m) {
        myModel = m;
    }

    public Prediction ipredict(User u, int i) {

        return null;
    }

    public TreeSet getRecs(User u, int numrecs, boolean includeRated, HashSet fset) {
        TreeSet myRecs = new TreeSet();
        TreeSet retRecs = new TreeSet();
        for(int i = 1; i< numrecs; i++) {
            Item theItem = (Item)((PopularityModel)myModel).sortedModel.elementAt(i);
            retRecs.add(new Recommendation( theItem.getItemID(),theItem.getRating(),theItem.getRating()));
        }
        return retRecs;
    }
}
