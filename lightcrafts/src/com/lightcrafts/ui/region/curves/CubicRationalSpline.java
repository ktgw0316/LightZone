/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

/** A CubicBasisSpline whose updateShape() actually generates a closed cubic
  * rational B-spline.  It uses Polynomial.createRationalShape(), which
  * performs flattening.
  */

public class CubicRationalSpline extends RationalSpline {
    @Override
    void ensureBases(int count) {
        while (Bases.size() < count) {
            int i = Bases.size();
            BasisFunction basis = new CubicBasisFunction(i);
            Bases.add(basis);
        }
    }
}
