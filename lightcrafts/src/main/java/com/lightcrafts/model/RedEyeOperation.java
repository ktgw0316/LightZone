/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

/**
 * A kind of Operation that is like a SpotOperation, taking elliptical
 * Contours, but does not use clone points.
 * <p>
 * It adds no methods to its parent interface.  This is just a marker.
 * <p>
 * @see com.lightcrafts.model.Contour
 * @see com.lightcrafts.model.Operation#setRegion
 */

public interface RedEyeOperation extends Operation {
}
