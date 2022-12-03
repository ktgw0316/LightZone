/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import java.util.Collection;

/** A Region defines a localized mask that can be associated to Operations.
  * <p>
  * A Region is like two Shapes: an inner Shape within which the Operation
  * should be fully effective; and an outer Shape outside of which the
  * Operation should be off.  The area bounded by these two Shapes defines a
  * transition zone where Operations should somehow interpolate between on
  * and off behavior.
  * <p>
  * Regions are unions of Contours.
  * <p>
  * @see com.lightcrafts.model.Contour
  */

public interface Region extends Contour {

    /** A Region may be a union of other non-intersecting Contours.  This
     * method returns a Collection of Shapes whose union is the same as
     * the Shape returned by <code>getOuterShape()</code> on this instance.
     * @return A Collection of Contours whose union is this Region.
     */
    Collection<Contour> getContours();
}
