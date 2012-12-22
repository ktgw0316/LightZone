/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

class CubicBasisFunction extends BasisFunction {

    CubicBasisFunction(int i) {
        super(i, 3);
    }

    // Get the Cubic defined by the polynomial of this basis function on the
    // segment with the given index.

    Polynomial computeSegment(int j) {

        int i = getFirstIndex();

        double t0 = i + 0;
        double t1 = i + 1;
        double t2 = i + 2;
        double t3 = i + 3;
        double t4 = i + 4;

        // Magic factorizations of degree-3 B-spline basis function
        // polynomials on each of the four segments where the function is
        // nonzero:

        if (j == i) {
            return Cubic.createFactored(t0, t0, t0).divide(2 * 3);
        }
        else if (j == i + 1) {
            Cubic c1 = Cubic.createFactored(t0, t0, t2);
            Cubic c2 = Cubic.createFactored(t1, t1, t4);
            Cubic c3 = Cubic.createFactored(t0, t1, t3);
            return c1.add(c2.add(c3)).divide(- 2 * 3);
        }
        else if (j == i + 2) {
            Cubic c1 = Cubic.createFactored(t0, t3, t3);
            Cubic c2 = Cubic.createFactored(t1, t3, t4);
            Cubic c3 = Cubic.createFactored(t2, t4, t4);
            return c1.add(c2.add(c3)).divide(2 * 3);
        }
        else if (j == i + 3) {
            return Cubic.createFactored(t4, t4, t4).divide(- 2 * 3);
        }
        else {
            return new Cubic(0, 0, 0, 0);
        }
    }
}
