/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import java.util.Map;
import java.awt.geom.Point2D;

/**
 * This interface combines the methods of ColorPickerOperation and
 * ColorDropperOperation.  It is a GenericOperation that can take its
 * settings from a point in the image and also from a color selection.
 */
public interface ColorPickerDropperOperation extends ColorPickerOperation {

    /**
     * Set the color such to the color in the image at the given location.
     * @param p A point in image coordinates that should define the color.
     * @return A Map of slider key Strings to Double values, indicating any
     * implications this color change will have on other settings.
     */
    Map<String, Double> setColor(Point2D p);

    @Override
    default void accept(GenericOperationVisitor visitor) {
        visitor.visitColorPickerDropperOperation(this);
    }
}
