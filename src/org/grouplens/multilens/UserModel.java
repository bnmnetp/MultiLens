package org.grouplens.multilens;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Feb 10, 2003
 * Time: 9:30:07 PM
 * To change this template use Options | File Templates.
 * TODO:  A cool performance enhancement would be to have a buildFinal routine that gets rid all but n nbrs
 */
public abstract class UserModel extends SparseModel {

    public UserModel(int i, int j) {
        super(i,j);
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
            if (myFilt != null) {
                myFilt.filter(currentItems);
            }
            User newUser = new User(currentItems);
            newUser.setUserId(currentUser.toString());
            User.addUserToCache(newUser);
            userList.add(newUser);
            in.close();
        } catch (IOException e) { System.out.println("IO Problems in build"); }
        // Now that we have all of the users in the cache, calculate similarities for all pairs.
        Iterator i = userList.iterator();
        while(i.hasNext()) {
            User iUser = (User)i.next();
            Iterator j = userList.iterator();
            while (j.hasNext()){
                // TODO: Create a short queue of the best users before
                //       creating the final set of cells to add.
                addUserCell(iUser,(User)j.next());
            }
        }
    }


    /**
     * Get the cell for the given user indices
     * 
     * @param i   Row key
     * @param j   Column key
     * @return The UserModelCell
     */
    public UserModelCell getModelCell(int i, int j) {
        SparseModelRow r = getRowOrCreate(i);
        UserModelCell myCell = (UserModelCell)r.getCell(j);
        if (myCell == null) {
            myCell = new UserModelCell();
            r.insertCell(j,myCell);
        }
        return myCell;
    }


    public float getSim(int i, int j) {
        float retval = (float)0.0;
        retval = getModelCell(i,j).getSimilarity();
        return retval;
    }


    /**
     * Add a similarity score for the two users.  This is an abstract function so that we can try out different
     * methods of calculating user similarity. e.g. Pearson, Cosine, etc.
     * @param i
     * @param j
     */
    public abstract void addUserCell(User i, User j);
}
