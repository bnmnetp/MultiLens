/*
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Nov 2, 2002
 * Time: 7:00:08 AM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package org.grouplens.multilens;

import org.grouplens.multilens.Item;
import org.grouplens.multilens.ItemVectorModifier;

import java.util.Vector;

public class Normalizer implements ItemVectorModifier {
    public void filter(Vector userItems) {
        int limit = userItems.size();
        // Normalize or zscore adjust userItems here.
        float total = 0;
        for (int i = 0; i < limit; i++) {
            Item ci = (Item)userItems.elementAt(i);
            total += (ci.rating * ci.rating);
        }
        float length = (float)Math.sqrt(total);
        for (int i = 0; i < limit; i++) {
            Item ci = (Item)userItems.elementAt(i);
            ci.modRating = ci.rating / length;
        }
    }

}
