/*
 * Created by IntelliJ IDEA.
 * User: bmiller
 * Date: Aug 14, 2002
 * Time: 5:11:13 PM
 * To change template for new class use
 * Code Style | Class Templates options (Tools | IDE Options).
 */
package jre.servlet;

import jre.servlet.ResultParser;

import java.net.*;
import java.util.TreeSet;
import java.util.Date;


public class URLConnectionReader {
    public static void main(String[] args) throws Exception {
        for (int i = 10; i < 100100; i+=10) {
            Date d1 = new java.util.Date();
            long sTime = d1.getTime();
            URL reurl = new URL("http://hugo.cs.umn.edu:8080/jrec/servlet/Jrec?request=getrecs&userid="
                + i + "&numrecs=6000");
            URLConnection re = reurl.openConnection();
            ResultParser foo = new ResultParser();
            TreeSet bar = foo.getRecommendations(re.getInputStream());
            Date d2 = new java.util.Date();
            long fTime = d2.getTime();
            System.out.println(i + "\t" + (fTime-sTime)+ "\t" + bar.size());
        }
    }


}

/*
            Iterator it = bar.iterator();
           while(it.hasNext()) {
            Recommendation r = (Recommendation)it.next();
            System.out.println(r.getItemID() + "\t" + r.getSimilarityScore() + "\t" + r.getPrediction());
        }
*/


