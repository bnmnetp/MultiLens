package org.grouplens.multilens;

/**
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Feb 11, 2003
 * Time: 12:38:27 PM
 * To change this template use Options | File Templates.
 */
public class UTest {


    public static void main(String Args[]) {
        UserModelPearson aModel = new UserModelPearson(3000,3000);
        aModel.buildFromFile("/Users/bmiller/Projects/ml-data/u1.base",null);

        UserRec myRE = new UserRec(aModel);
        Prediction pred = myRE.ipredict(User.getInstance("1"),6);
        System.out.println("pred = " + pred.getPred());
    }
}
