package org.grouplens.multilens;

/*
 * Created by IntelliJ IDEA.
 * org.grouplens.multilens.User: bmiller
 * Date: May 22, 2002
 * Time: 6:40:15 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 * A simple recommender that uses the just count model.
 */
import org.grouplens.multilens.DBConnection;
import org.grouplens.multilens.JCCell;
import org.grouplens.multilens.JustCountModel;

import java.util.Date;
import java.util.*;
import java.sql.*;
import java.io.*;

public class JustCountRec implements Recommender {
    JustCountModel myModel;
    /**
     * Create a nume recommender object using the model given.
     * @param theModel  A pre-created Model
     */
    public JustCountRec(JustCountModel theModel) {
        myModel = theModel;
    }

    /**
     * Create a recommender object by reading in an xml version of the model from a flat file.
     * @param modelFile
     */
    JustCountRec(String modelFile){

    }

    /**
     * Default constructor
     */
    JustCountRec() {

    }

    /**
     * Calculate the best n items given a user id and n.
     * Simple calculation of recommendation goes like this:
     * Get the set of things R that the user has rated
     * Create a list to hold the candidate items and their votes
     * Get the rows from the model the items in R
     *    add new items to the candidate list or increment their votes
     * Find the best n items from the candidate list (not rated by the user)
     * @param myUser
     * @param n
     * @return Best n recommended item ids
     */
    public TreeSet getRecs(User myUser, int n, boolean includeRated) {

	 //HashMap userRatings = myUser.getAllRatings();
	 HashMap userRatings = myUser.getGoodItems(4);
        int newItems[] = new int[myModel.getMaxKey()+1]; //TODO this should follow database
        int candidateKey = 0;
        for (Iterator it = userRatings.keySet().iterator(); it.hasNext(); ) {
          Object key = it.next();
          SparseModelRow theRow = myModel.getModelRow(((Integer)key).intValue());
	  Iterator rit = null;
	  if (theRow != null) {
	      rit = theRow.iterator();
	  } else {
	      System.out.println("Error: Row no found");
	  }
          while( theRow != null && rit.hasNext() ) {
              Integer candKey = (Integer)rit.next();
              candidateKey = candKey.intValue();
              int newCount = ((JCCell)theRow.getCell(candidateKey)).getCount();
              newItems[candidateKey] += newCount;
            }
        }

        TreeSet myTopN = new TreeSet();
        int sortme[] = new int[myModel.getMaxKey()+1];
        for (int i = 0; i<=myModel.getMaxKey();i++) {
            sortme[i] = newItems[i];
        }
        // Sort the array, and find the nth best value.  Everything above that goes on the return list.
        if (! includeRated) {
            Iterator it = userRatings.keySet().iterator();
            while (it.hasNext()) {
                Integer key = (Integer)it.next();
                newItems[key.intValue()] = 0;
            }
        }
        Arrays.sort(sortme);
        int limit = sortme[sortme.length - n];
        int j = 0;
        for(int i = 0; i<=myModel.getMaxKey();i++) {
            if (newItems[i] >= limit && j < n) {
                float foo = (float)newItems[i];
                myTopN.add(new Recommendation(Integer.toString(i),foo,foo));
                j++;
            }
        }
        return myTopN;

    }


    /*
    if (newItems.containsKey(candKey)) {
      int count = ((Integer)newItems.get(candKey)).intValue();
      count = count + newCount;
      newItems.remove(candKey);
      newItems.put(candKey,new Integer(count));
      }
    else {
      if (! userRatings.containsKey(candKey) || includeRated)
          newItems.put(candKey,new Integer(newCount));
      }
    */

    /**
     * Calculate the top n items for a marketBasket of items.
     * The recommendations are calculated by computing the total similarity for items that co-occur with the items
     * in the market basket.  Think of a subset of the model matrix.  The marketBasket Vector represents the rows
     * from the model.  Computing the sum of the columns and then selecting the 10 largest columns is how we
     * will calculate the topn.
     * - idea, what if we attached attributes to the columns to indicate things like genre. This would make
     *   filtering by genre etc quite easy and would reduce the computation needed for the topn.
     * @param marketBasket
     * @param n
     * @return Vector of Integers representing the item ids.
     */
    public Object[] getRecs(HashMap marketBasket, int n) {
        return null;
    }

    public int repeatableTopN(String expName) {
        // select distinct users
        // for each of 1000 users
        // randomly select an item from the User's item list.  take take it out of the list.
        // generate a top10 for the user.
        // Check to see if the item is on the list.
        Vector userPopulation;
        userPopulation = new Vector(1000);
        int uCount = 0;

        try {
            DBConnection myConn = new DBConnection();
            Statement stmt = myConn.getConn().createStatement();
            PrintStream predOut = null;
            try {
                predOut = new PrintStream(new FileOutputStream("/Users/bmiller/Projects/PredResults/"+expName));
                } catch (Exception e) {}

            // Select the desired user ratings
            String SQLStmt = "SELECT gl_id FROM test_set where rating > 3";
            //String SQLStmt = "SELECT distinct gl_id, rating.mlid, rating from rating,fewrate where rating.mlid = fewrate.mlid";
            //System.out.println("SQL SELECT Statement: " + SQLStmt);
            ResultSet rs = stmt.executeQuery(SQLStmt);
            Integer glid;
            float totalAE = 0;
            int pcount = 0;
            Vector plist = new Vector(5000);
            Integer coveredUpKey;
            int hits = 0;
            int tries = 0;
            while (rs.next()) {
                System.out.println("Trying user " + uCount);
                uCount++;
                glid = new Integer(rs.getInt(1));
                User currentUser = new User(glid.toString());
                 coveredUpKey = currentUser.deleteOneGood();
                TreeSet myRecs = getRecs(currentUser, 10, false);
                tries++;
                predOut.print(glid + "\t" + coveredUpKey);
                Iterator it = myRecs.iterator();
                boolean hitme = false;
                while (it.hasNext()) {
                    Recommendation rec = (Recommendation)it.next();
                    predOut.print("\t" + rec.getItemID());
                    if (coveredUpKey.intValue() == rec.getItemID()) {
                        hits++;
                        hitme = true;
                    }
                }
                if (hitme) {
                    predOut.println("\tHIT");
                } else {
                    predOut.println("\tMISS");
                }
		//                currentUser.closeDB();
            }
            stmt.close();
            System.out.println("Hits = " + hits + " Tries = " + tries + " % = " + (float)(hits/tries));
        }
        catch (SQLException E) {
            System.err.println("SQLException: " + E.getMessage());
            System.err.println("SQLState:     " + E.getSQLState());
            System.err.println("VendorError:  " + E.getErrorCode());
        }
        return uCount;
    }

    public static void main(String args[]) {
        Date d1 = new Date();
        JustCountModel aModel = new JustCountModel(500,500);

        long stTime = d1.getTime();
	aModel.setModelTable("jc_model");
        aModel.readModel(500,true);
        d1 = new Date();
        long fTime = d1.getTime();
        System.out.println("Time to read = " + (fTime - stTime));
        JustCountRec myRE = new JustCountRec(aModel);
	myRE.repeatableTopN("topn-jc-500.dat");

    }

    public Prediction ipredict(User u, int i) {
        return null;
    }

    public TreeSet getRecs(User u, int numrecs, boolean includeRated, HashSet fset) {
        return getRecs(u,numrecs,includeRated);
    }

}
