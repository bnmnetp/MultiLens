package org.grouplens.multilens;

/*
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Nov 2, 2002
 * Time: 7:08:39 AM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */


import java.util.Vector;

public class ZScore implements org.grouplens.multilens.ItemVectorModifier {
    public void filter(Vector userItems) {
        int limit = userItems.size();
        // Normalize or zscore adjust userItems here.
        float total = 0;
        for (int i = 0; i < limit; i++) {
            org.grouplens.multilens.Item ci = (org.grouplens.multilens.Item)userItems.elementAt(i);
            total += ci.rating;
        }
        float average = total / (float)limit;
        for (int i = 0; i < limit; i++) {
            org.grouplens.multilens.Item ci = (org.grouplens.multilens.Item)userItems.elementAt(i);
            ci.modRating = ci.rating - average;
        }

    }

}
