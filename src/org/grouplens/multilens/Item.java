package org.grouplens.multilens;

/*
 * Created by IntelliJ IDEA.
 * org.grouplens.multilens.User: bmiller
 * Date: Jul 6, 2002
 * Time: 11:24:56 AM
 * To change template for new class use 
 * Code Style | Class Templates options (Tools | IDE Options).
 */

public class Item implements Comparable {
    /**
     * ItemID the integer item identifier for this item
     */
    public int itemID;

    /**
     * The original integer based rating for this item
     */
    public float rating;

    /**
     * The double value for the rating.  If a filter has been applied to a set of ratings.
     * modRating will reflect the filtered value.  rating will continue to reflect the original value.
     */
    public float modRating;

    public Item(int itID, float rat) {
        itemID = itID;
        rating = rat;
        modRating = rat;
    }

    public Item(int itID, float rat, float modRat) {
        this(itID, rat);
        modRating = modRat;
    }

    public int getItemID() {
        return itemID;
    }

    public void setItemID(int itemID) {
        this.itemID = itemID;
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }

    public float getModRating() {
        return modRating;
    }

    public void setModRating(float modRating) {
        this.modRating = modRating;
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return "itemId " + itemID + " rating " + rating + " modRating " + modRating;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Object o) {
        Item i2 = (Item)o;
        return itemID - i2.itemID;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object obj) {
        if (null == obj) return false;
        Item i2 = (Item)obj;
        return itemID == i2.itemID;
    }

}
