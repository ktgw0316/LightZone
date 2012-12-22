/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

/**
 * A kind of Operation that does "cloning": copying translated copies of an
 * image onto itself, masked by Regions.
 * <p>
 * It adds no methods to its parent interface, but it expects that all the
 * Contours in its Region will be CloneContours.
 * <p>
 * @see com.lightcrafts.model.CloneContour
 * @see com.lightcrafts.model.Operation#setRegion
 */

public interface CloneOperation extends Operation {
}
