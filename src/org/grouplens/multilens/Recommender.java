package org.grouplens.multilens;

import java.util.TreeSet;
import java.util.HashSet;

public interface Recommender {
    public Prediction ipredict(User u, int i);

    public TreeSet getRecs(User u, int numrecs, boolean includeRated, HashSet fset );
}
