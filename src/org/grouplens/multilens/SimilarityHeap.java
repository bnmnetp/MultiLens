package org.grouplens.multilens;

import java.util.Arrays;
import java.util.Iterator;

/**
 * The similarity Heap class is used by the recommender algorithms to store topN lists.
 * This implementation of a heap is over simplified, but works with the CosineRec class.
 * It is not intended to be a generic heap implementation.
 *
 */
public class SimilarityHeap {
    float simList[];
    int itemList[];
    int MAX_ITEMS;

    public SimilarityHeap() {

    }

    /**
     * Create a new heap.
     *
     * Given the size parameter the heap maintains the top (size) items on the heap.
     * Items with scores that are lower than the current minimum, are dropped off the heap.
     *
     * @param size Maximum number of items to keep on the heap.
     */
    public SimilarityHeap(int size) {
        MAX_ITEMS = size;
        simList = new float[size];
        itemList = new int[size];
        Arrays.fill(itemList,0);
        Arrays.fill(simList,-Float.MAX_VALUE);

    }

    /**
     * Add a new item to the heap.
     * @param sim
     * @param item
     * @return the position on the heap at which the new item was added.
     */
    public int insert(float sim, int item) {
        int i = 0;
        boolean found = false;
        while (i < MAX_ITEMS && ! found) {
            if (sim > simList[i]) {
                found = true;
                break;
            }
            i++;
        }
        if (found) {
            for (int j = MAX_ITEMS-1; j> i; j--) {
                itemList[j] = itemList[j-1];
                simList[j] =simList[j-1];
            }
            itemList[i] = item;
            simList[i] = sim;
        }
        return i;
    }

    public SimHeapIterator iterator() {
        return new SimHeapIterator();
    }

    public float getSimByIndex(int i) {
        return simList[i];
    }

    public int getItembyIndex(int i) {
        return itemList[i];
    }

    class SimHeapIterator implements Iterator {
        int currentIdx = 0;
    
        SimHeapIterator() {
            currentIdx = 0;
        }
    
        public boolean hasNext() {
            if (currentIdx < MAX_ITEMS) {
                return true;
            } else {
                return false;
            }
        }
    
        public Object next() {
            int retval = currentIdx;
            currentIdx++;
            if (currentIdx < MAX_ITEMS) {
                if (simList[currentIdx] == -Float.MAX_VALUE) {
                    currentIdx = MAX_ITEMS;
                }
            }
            return new Integer(retval);
        }

        public void remove() {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
