package org.grouplens.multilens;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;

/**
 * @author dfrankow
 * 
 * A source of ratings.
 */
public abstract class RatingsSource {
    /** If you can fill this in your constructor, then you can return
     * MapRatingRowIterator from user_iterator(). */
    protected HashMap userRatingMap;

    /**
     * A "row" of ratings that all have the same id
     * (e.g., a single user or a single item)
     *
     * NOTE: This is a lot like a SparseModelRow, except instead it has
     * a Vector of Item, which has a rating and modRating instead of just
     * one value.  TODO: Unify RatingRow and SparseModelRow.
     * 
     * @author dfrankow
     */
    public static class RatingRow {
        private int id;
        private Vector element;

        public RatingRow(int id, Vector elements) {
            this.id = id;
            element = elements;
        }

        /**
         * The id of the row.
         * 
         * @return int
         */
        public int getId() {
            return id;
        }

        public int size() {
            return element.size();
        }

        protected void addElement(int id, float value) {
            element.add(new Item(id, value));
        }

        /**
         * Get the element Vector.  This should go away in favor of
         * the interfaces below if we wish to be efficient and use
         * parallel native arrays instead of an array of objects.
         */
        public Vector getElements() {
            return element;
        }

        // NOTE: Leave the door open for efficient parallel arrays
        // by making the interface getElementId/getValue instead of
        // an iterator that returns cells.

        /**
         * The id of element at 'index'.
         * 
         * @param index
         * @return the id
         */
        public int getElementId(int index) {
            return ((Item)element.get(index)).getItemID();
        }

        public float getValue(int index) {
            return ((Item)element.get(index)).getRating();
        }

        public float getModValue(int index) {
            return ((Item)element.get(index)).getModRating();
        }

        /**
         * @see java.lang.Object#toString()
         */
        public String toString() {
            String s = "rowid " + id + " ";
            Iterator iter = element.iterator();
            String comma = "";
            while (iter.hasNext()) {
                Item i = (Item) iter.next();
                s += comma + i;
                comma = ", ";
            }
            return s;
        }
    }

    public abstract class RatingRowIterator implements Iterator {
        /**
         * @see java.util.Iterator#hasNext()
         */
        public abstract boolean hasNext();

        /**
         * @see java.util.Iterator#next()
         */
        public abstract Object next();

        /* (non-Javadoc)
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            throw new UnsupportedOperationException("Not implemented");
        }
    }

    /**
     * An implementation of RatingRowIterator that assumes userRatingMap
     * is full.
     */
    class MapRatingRowIterator extends RatingRowIterator {
        private Iterator iter;

        protected MapRatingRowIterator() {
            iter = userRatingMap.keySet().iterator();
        }

        /**
         * @see org.grouplens.multilens.RatingsSource.RatingRowIterator#hasNext()
         */
        public boolean hasNext() {
            return iter.hasNext();
        }

        /**
         * @see org.grouplens.multilens.RatingsSource.RatingRowIterator#next()
         */
        public Object next() {
            Integer key = (Integer) iter.next();
            return new RatingRow(key.intValue(), (Vector) userRatingMap.get(key));
        }
    }

    protected RatingsSource() {
        userRatingMap = new HashMap();
    }

    /**
     * Get an iterator that returns one user's ratings at a time.
     * 
     * @return RatingRowIterator
     */
    /* If you can fill userRatingMap in your constructor, then you can return
     * MapRatingRowIterator from user_iterator(). */
    public abstract RatingRowIterator user_iterator();
}
