package org.grouplens.multilens;

import org.grouplens.multilens.CellCombiner;

public class MultCell implements CellCombiner {
    public float combine(float x, float y) {
	return(x * y);
    }
}
