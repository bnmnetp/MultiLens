package org.grouplens.multilens;

import java.util.Iterator;
import java.util.HashMap;

/**
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Feb 11, 2003
 * Time: 8:24:02 AM
 * To change this template use Options | File Templates.
 */
public class UserModelPearson extends UserModel implements ItemModel{

    public UserModelPearson(int i, int j) {
        super(i,j);
    }


    public void addUserCell(User i, User j) {
        float sum = (float)0.0;
        int ccount = 0;
        HashMap iRat = i.getAllRatings();
        HashMap jRat = j.getAllRatings();
        Iterator it = iRat.keySet().iterator();

        while (it.hasNext()){
            Integer key = (Integer)it.next();
            if (jRat.containsKey(key)) {
                sum += (((Float)iRat.get(key)).floatValue() - i.getAverage()  ) *
                        (((Float)jRat.get(key)).floatValue() - j.getAverage() );
                ccount++;
            }

        }
        float pearson = sum / (i.getStdev() * j.getStdev());
        float sfactor = (float)1.0;
        if (ccount < 50) {
            sfactor = (float)ccount / (float)50.0;
        }
        pearson = pearson * sfactor;
        if (ccount > 3 && pearson > 0.0) {
            UserModelCell myCell = getModelCell(Integer.parseInt(i.getUserId()), Integer.parseInt(j.getUserId()));
            myCell.setSimilarity(pearson);
        }
    }
}
