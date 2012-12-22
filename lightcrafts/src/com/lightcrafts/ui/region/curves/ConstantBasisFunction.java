/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

class ConstantBasisFunction extends BasisFunction {

    ConstantBasisFunction(int i) {
        super(i, 0);
    }

    // Get the Constant defined by the polynomial of this basis function on
    // the segment with the given index.

    Polynomial computeSegment(int j) {
        if (! isSupportedIndex(j)) {
            return new Polynomial(0);
        }
        return new Polynomial(1);
    }
}
