/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

import javax.swing.*;
import java.util.ArrayList;

/** A basis function is a List Polynomials on a number of segments.
  */
abstract class BasisFunction {

    private int i;  // i is the index of the first nonzero segment
    private int p;  // p+1 is number of nonzero segments

    private ArrayList polys;    // Polynomials for each of p+1 segments

    BasisFunction(int i, int p) {
        this.i = i;
        this.p = p;
        polys = new ArrayList();
        for (int n=i; n<=i+p; n++) {
            Polynomial poly = computeSegment(n);
            polys.add(poly);
        }
    }

    int getFirstIndex() {
        return i;
    }

    int getLastIndex() {
        return i + p;
    }

    boolean isSupportedIndex(int j) {
        return ((j >= getFirstIndex()) && (j <= getLastIndex()));
    }

    Polynomial getSegment(int n) {
        if ((n >= i) && (n <= i+p)) {
            return (Polynomial) polys.get(n - i);
        }
        return new Polynomial(0);
    }

    double evaluate(double t) {
        int j = (int) Math.floor(t);
        Polynomial p = getSegment(j);
        return p.evaluate(t);
    }

    abstract Polynomial computeSegment(int n);
}
