package org.grouplens.multilens;

import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Apr 7, 2003
 * Time: 11:09:40 AM
 * To change this template use Options | File Templates.
 */
public class PopularityModel implements ItemModel {
    int[] myCount;
    int numUsers = 0;
    Vector sortedModel;

    public PopularityModel() {
        sortedModel = new Vector(3600);
    }

    public int getMaxKey() {
        for(int i = myCount.length; i > 0; i--) {
            if (myCount[i] > 0) {
                return i;
            }
        }
        return 0;
    }

    public void buildFromFile(String ratingFile, ItemVectorModifier myFilt) {
        BufferedReader in = null;
        Integer currentUser = new Integer(0);
        Vector currentItems;
        LinkedList userList = new LinkedList();
        try {
            in = new BufferedReader(new FileReader(ratingFile));
        } catch(IOException e) {}
        currentItems = new Vector(100);
        try {
            String line = in.readLine();
            while(line != null) {
                StringTokenizer tokens = new StringTokenizer(line);

                Integer userID = new Integer(tokens.nextToken());
                int itemID = Integer.parseInt(tokens.nextToken());
                float rating = Float.parseFloat(tokens.nextToken());
                Item theItem = new Item(itemID,rating);
                if (! userID.equals(currentUser) ) {
                    // Take care of adding the previous user to the model.
                    numUsers++;
                    ItemComparator myComp = new ItemComparator();
                    Collections.sort(currentItems,myComp);
                    if (myFilt != null) {
                        myFilt.filter(currentItems);
                    }
                    User newUser = new User(currentItems);
                    newUser.setUserId(currentUser.toString());
                    User.addUserToCache(newUser);
                    userList.add(newUser);
                    currentItems = new Vector(50);
                    currentUser = userID;
                }
                currentItems.add(theItem);
                line = in.readLine();
            }
            // Take care of the last user
            numUsers++;
            if (myFilt != null) {
                myFilt.filter(currentItems);
            }
            User newUser = new User(currentItems);
            newUser.setUserId(currentUser.toString());
            User.addUserToCache(newUser);
            userList.add(newUser);
            in.close();
        } catch (IOException e) { System.out.println("IO Problems in build"); }
        // Now that we have all of the users in the cache, calculate the Average and count for all items.
        Iterator i = userList.iterator();
        myCount = new int[numUsers+1];

        while(i.hasNext()) {
            User iUser = (User)i.next();
            HashMap myRatings = iUser.getAllRatings();
            Iterator itemIt = myRatings.keySet().iterator();
            while(itemIt.hasNext()) {
                Integer key = (Integer)itemIt.next();
                Float rating = (Float)myRatings.get(key);
                myCount[key.intValue()]++;
            }
        }
        for(int itId = 1; itId<myCount.length; itId++) {
            if (myCount[itId] > 0) {
                sortedModel.add(new Item(itId,(float)myCount[itId]));
            }
        }
        // Now sort the vector according to the 'rating'
        ItemRatingComparator sortByCount = new ItemRatingComparator();
        Collections.sort(sortedModel,sortByCount);
    }


    public SparseModelRow getModelRow(int i) {
        throw new UnsupportedOperationException("No support for inserting new items");
    }

    public float getSim(int i, int j) {
        return ((float)myCount[j]);
    }

    public int getNumItems() {
        return myCount.length;
    }

    public static void main(String Args[]) {
        PopularityModel aModel = new PopularityModel();

        aModel.buildFromFile("/Users/bmiller/tmp/test.dat",null);
        System.out.println(aModel.getSim(1,1));
        System.out.println(aModel.getSim(1,2));
        System.out.println(aModel.getSim(1,3));



    }

}

