package org.grouplens.multilens;

/*
 * Created by IntelliJ IDEA.
 * org.grouplens.multilens.User: bmiller
 * Date: Jul 6, 2002
 * Time: 12:10:59 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */

import java.util.Map;
import java.util.Comparator;

public class DoubleItemComparator implements Comparator {
        public int compare(Object o1,Object o2)
        {
                if( ((Double)((Map.Entry)o1).getValue()).doubleValue() < ((Double)((Map.Entry)o2).getValue()).doubleValue() ){
                        return(1);
                }else if( ((Double)((Map.Entry)o1).getValue()).doubleValue() > ((Double)((Map.Entry)o2).getValue()).doubleValue() ){
                        return(-1);
                }else{
                        return(0);
                }
        }
}
