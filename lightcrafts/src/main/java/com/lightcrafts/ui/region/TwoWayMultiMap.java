/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import java.util.*;

/** A TwoWayMultiMap defines bidirectional, one-to-many associations between
  * Objects.  You can ask for the Set of right-values for a left-key, and the
  * Set of left-values for a right-key.
  */

class TwoWayMultiMap<L, R> {

    // The two associations (must be kept consistent):
    private Map<L, Set<R>> leftToRight = new HashMap<L, Set<R>>();
    private Map<R, Set<L>> rightToLeft = new HashMap<R, Set<L>>();

    void clear() {
        leftToRight.clear();
        rightToLeft.clear();
    }

    Set<L> getLeft(R right) {
        Set<L> lefts = rightToLeft.get(right);
        if (lefts != null) {
            return new HashSet<L>(lefts);
        }
        return null;
    }

    Set<R> getRight(L left) {
        Set<R> rights = leftToRight.get(left);
        if (rights != null) {
            return new HashSet<R>(rights);
        }
        return null;
    }

    Set<L> getAllLeft() {
        return new HashSet<L>(leftToRight.keySet());
    }

    Set<R> getAllRight() {
        return new HashSet<R>(rightToLeft.keySet());
    }

    void addLeft(L left) {
        if (! leftToRight.containsKey(left)) {
            leftToRight.put(left, new HashSet<R>());
        }
    }

    void addRight(R right) {
        if (! rightToLeft.containsKey(right)) {
            rightToLeft.put(right, new HashSet<L>());
        }
    }

    void put(L left, R right) {
        Set<R> rights = leftToRight.get(left);
        if (rights == null) {
            rights = new HashSet<R>();
            leftToRight.put(left, rights);
        }
        rights.add(right);
        Set<L> lefts = rightToLeft.get(right);
        if (lefts == null) {
            lefts = new HashSet<L>();
            rightToLeft.put(right, lefts);
        }
        lefts.add(left);
    }

    void remove(L left, R right) {
        Set<R> rights = leftToRight.get(left);
        if (rights != null) {
            rights.remove(right);
        }
        Set<L> lefts = rightToLeft.get(right);
        if (lefts != null) {
            lefts.remove(left);
        }
    }

    void removeLeft(L left) {
        Set<R> rights = getRight(left);
        if (rights == null) {
            return;
        }
        for (R right : rights) {
            remove(left, right);
        }
        leftToRight.remove(left);
    }

    void removeRight(R right) {
        Set<L> lefts = getLeft(right);
        if (lefts == null) {
            return;
        }
        for (L left : lefts) {
            remove(left, right);
        }
        rightToLeft.remove(right);
    }
}
