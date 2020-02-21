/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import java.awt.geom.Point2D;
import java.util.Map;

/**
 * A ColorDropperOperation is a GenericOperation that allows its parameters
 * to be set in two ways: through the GenericOperation API; or by specifying
 * a point in image coordinates.
 * <p>
 * It is allowed that the GenericOperation slider parameters may need to
 * update when an image point is specified.  Therefore the method that
 * accepts a point returns a Map of slider values.
 */
public interface ColorDropperOperation extends GenericOperation {

    /**
     * Set the GenericOperation parameters of this Operation according to the
     * color at the given point in image coordinates.
     * @param p A point in image coordinates that defines the settings.
     * @return A Map of slider key Strings to Double values, indicating the
     * implications this color selection has on other settings.
     */
    Map<String, Float> setColor(Point2D p);

    @Override
    default void accept(GenericOperationVisitor visitor) {
        visitor.visitColorDropperOperation(this);
    }
}
