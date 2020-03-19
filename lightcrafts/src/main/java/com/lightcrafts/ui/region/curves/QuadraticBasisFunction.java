/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

/** The quadratic basis function for B-splines.
  */

class QuadraticBasisFunction extends BasisFunction {

    QuadraticBasisFunction(int i) {
        super(i, 2);
    }

    // Get the Quadratic defined by the polynomial of this basis function on
    // the segment with the given index.

    Polynomial computeSegment(int j) {

        int i = getFirstIndex();
        
        double t0 = i + 0;
        double t1 = i + 1;
        double t2 = i + 2;
        double t3 = i + 3;

        // Magic factorizations of degree-2 B-spline basis function
        // polynomials on each of the three segments where the function is
        // nonzero:

        if (j == i) {
            return Quadratic.createFactored(t0, t0).divide(2);
        }
        else if (j == i + 1) {
            Quadratic q1 = Quadratic.createFactored(t0, t2);
            Quadratic q2 = Quadratic.createFactored(t1, t3);
            return q1.add(q2).divide(- 2);
        }
        else if (j == i + 2) {
            return Quadratic.createFactored(t3, t3).divide(2);
        }
        else {
            return new Polynomial(0);
        }
    }
}
