package org.grouplens.multilens;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * @author dfrankow
 */
public class FileRatingsSource extends RatingsSource {
    public FileRatingsSource(String source, ItemVectorModifier myFilt) throws IOException {
        BufferedReader in = new BufferedReader(new FileReader(source));

        Integer currentUser = new Integer(-1);
        Vector currentItems = new Vector(100);

        //
        // Read in all users. This is simplest, and works well as long as
        // a) All the ratings fit in memory
        // b) A client usually wants to read all the ratings
        //
        String line = in.readLine();
        while(line != null) {
            StringTokenizer tokens = new StringTokenizer(line);

            Integer userID = new Integer(tokens.nextToken());
            int itemID = Integer.parseInt(tokens.nextToken());
            float rating = Float.parseFloat(tokens.nextToken());
            Item theItem = new Item(itemID,rating);
            if (! userID.equals(currentUser)) {
                if (currentUser.intValue() > -1 ) {

                    // Enforce that ratings must be sorted in non-decreasing
                    // user order.  Without this, you could lose ratings when
                    // the same user appears in more than one contiguous block.
                    if (userID.compareTo(currentUser) < 0) {
                        throw new IOException("Ratings file " + source + " must be sorted so that users appear in non-decreasing order.");
                    }

                    // Take care of adding the previous user to the model.
                    ItemComparator myComp = new ItemComparator();
                    Collections.sort(currentItems,myComp);
                    if (myFilt != null) {
                        myFilt.filter(currentItems);
                    }
                    userRatingMap.put(currentUser, currentItems);
                    currentItems = new Vector(50);
                }
                currentUser = userID;
            }
            currentItems.add(theItem);
            line = in.readLine();
        }
        if (myFilt != null) {
            myFilt.filter(currentItems);
        }
        //System.out.println("userRatingMap.put(" + currentUser + ", ...)");
        userRatingMap.put(currentUser, currentItems);
    }

    /**
     * @see org.grouplens.multilens.RatingsSource#user_iterator()
     */
    public RatingRowIterator user_iterator() {
        return new RatingsSource.MapRatingRowIterator();
    }
}
