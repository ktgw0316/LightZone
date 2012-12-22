/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

/**
 * A kind of Operation that does spotting: blotting out small regions by
 * cloning in their surroundings.  This is similar to CloneOperation, except
 * the Operation's Curves are ellipses.
 * <p>
 * It adds no methods to its parent interface.  This is just a marker.
 * <p>
 * @see com.lightcrafts.model.Contour
 * @see com.lightcrafts.model.Operation#setRegion
 */

public interface SpotOperation extends Operation {
}
