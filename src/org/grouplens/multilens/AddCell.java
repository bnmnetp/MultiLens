package org.grouplens.multilens;

public class AddCell implements CellCombiner {
    public float combine(float x, float y) {
	return(x + y);
    }
}
