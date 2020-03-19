/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

/** A QuadraticBasisSpline whose updateShape() actually generates a closed
  * rational B-spline.  It uses Polynomial.createRationalShape(), which
  * performs flattening.
  */

public class QuadraticRationalSpline extends RationalSpline {

    @Override
    void ensureBases(int count) {
        while (Bases.size() < count) {
            int i = Bases.size();
            BasisFunction basis = new QuadraticBasisFunction(i);
            Bases.add(basis);
        }
    }
}
