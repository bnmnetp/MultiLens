package org.grouplens.multilens;

/*
 * Created by IntelliJ IDEA.
 * org.grouplens.multilens.User: bmiller
 * Date: Jul 2, 2002
 * Time: 1:57:03 PM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */

import org.grouplens.multilens.Item;

import java.util.Comparator;

public class ItemComparator implements Comparator {
/*
    public int compare(Object o1,Object o2)
    {
        if( ((Integer)((Map.Entry)o1).getValue()).intValue() < ((Integer)((Map.Entry)o2).getValue()).intValue() ){
            return(1);
        }else if( ((Integer)((Map.Entry)o1).getValue()).intValue() > ((Integer)((Map.Entry)o2).getValue()).intValue() ){
            return(-1);
        }else{
            return(0);
        }
    }
*/
    public int compare(Object o1,Object o2) {
        if( ((Item)o1).getItemID() < ((Item)o2).getItemID()) {
            return(-1);
        }else if( ((Item)o1).getItemID() > ((Item)o2).getItemID()) {
            return(1);
        }else{
            return(0);
        }
    }

}
