package org.grouplens.multilens;

import java.util.TreeSet;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Apr 7, 2003
 * Time: 2:17:23 PM
 * To change this template use Options | File Templates.
 */
public class AverageRec implements Recommender {
    ItemModel myModel;

    public AverageRec(ItemModel m) {
        myModel = m;
    }

    public Prediction ipredict(User u, int i) {
        float avg = myModel.getSim(0,i);
        float pred = avg + u.getAverage();
        Prediction p = new Prediction(pred);
        p.setItemID(i);
	p.setUserID(-1);
        try {
            p.setUserID( Integer.parseInt(u.getUserId()) );
        }
        catch (Exception e) {
            // For now, there's a mismatch: users have string ids,
            // predictions and recommendations have int userids.
            // This exception says that we got a string id that's
            // not an integer.  TODO: Bad, should be fixed.
        }


        return p;
    }

    public TreeSet getRecs(User u, int numrecs, boolean includeRated, HashSet fset) {
        TreeSet myRecs = new TreeSet();
        TreeSet retRecs = new TreeSet();
        for(int i = 1; i< ((AverageModel)myModel).getNumItems(); i++) {
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
