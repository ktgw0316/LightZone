/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import java.util.*;

/** A TwoWayMultiMap defines bidirectional, one-to-many associations between
  * Objects.  You can ask for the Set of right-values for a left-key, and the
  * Set of left-values for a right-key.
  */

class TwoWayMultiMap {

    // The two assocations (must be kept consistent):

    private Map leftToRight = new HashMap();

    private Map rightToLeft = new HashMap();

    void clear() {
        leftToRight.clear();
        rightToLeft.clear();
    }
    
    Set getLeft(Object right) {
        Set lefts = (Set) rightToLeft.get(right);
        if (lefts != null) {
            return new HashSet(lefts);
        }
        return null;
    }

    Set getRight(Object left) {
        Set rights = (Set) leftToRight.get(left);
        if (rights != null) {
            return new HashSet(rights);
        }
        return null;
    }

    Set getAllLeft() {
        return new HashSet(leftToRight.keySet());
    }

    Set getAllRight() {
        return new HashSet(rightToLeft.keySet());
    }

    void addLeft(Object left) {
        if (! leftToRight.containsKey(left)) {
            leftToRight.put(left, new HashSet());
        }
    }

    void addRight(Object right) {
        if (! rightToLeft.containsKey(right)) {
            rightToLeft.put(right, new HashSet());
        }
    }

    void put(Object left, Object right) {
        Set rights = (Set) leftToRight.get(left);
        if (rights == null) {
            rights = new HashSet();
            leftToRight.put(left, rights);
        }
        rights.add(right);
        Set lefts = (Set) rightToLeft.get(right);
        if (lefts == null) {
            lefts = new HashSet();
            rightToLeft.put(right, lefts);
        }
        lefts.add(left);
    }

    void remove(Object left, Object right) {
        Set rights = (Set) leftToRight.get(left);
        if (rights != null) {
            rights.remove(right);
        }
        Set lefts = (Set) rightToLeft.get(right);
        if (lefts != null) {
            lefts.remove(left);
        }
    }

    void removeLeft(Object left) {
        Set rights = getRight(left);
        if (rights == null) {
            return;
        }
        for (Iterator i=rights.iterator(); i.hasNext(); ) {
            Object right = i.next();
            remove(left, right);
        }
        leftToRight.remove(left);
    }

    void removeRight(Object right) {
        Set lefts = getLeft(right);
        if (lefts == null) {
            return;
        }
        for (Iterator i=lefts.iterator(); i.hasNext(); ) {
            Object left = i.next();
            remove(left, right);
        }
        rightToLeft.remove(right);
    }
}
