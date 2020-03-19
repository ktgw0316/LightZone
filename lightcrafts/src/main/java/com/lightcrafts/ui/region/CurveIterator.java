/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import java.util.*;

// A typesafe iterator for Curves, to describe selection states:

public class CurveIterator {

    private Collection<Curve> collection;
    private Iterator<Curve> iterator;

    CurveIterator(Collection<Curve> curves) {
        collection = new LinkedList<Curve>(curves);
        reset();
    }

    public void reset() {
        iterator = collection.iterator();
    }

    public boolean hasNext() {
        return iterator.hasNext();
    }

    public Curve nextCurve() {
        return iterator.next();
    }
}

