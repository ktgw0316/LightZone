/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import java.util.Map;
import java.awt.*;

/**
 * A ColorPickerOperation is a GenericOperation that allows its parameters
 * to be set by choosing a color, for instance by picking a color from a color
 * wheel control.
 * <p>
 * It is allowed that the GenericOperation slider parameters may need to
 * update when an image point is specified.  Therefore the method that
 * accepts a point returns a Map of slider values.
 */
public interface ColorPickerOperation extends GenericOperation {
    /**
     * Set the color to the given color.
     * @param color A color which should be used.
     * @return A Map of slider key Strings to Double values, indicating any
     * implications this color change will have on other settings.
     */
    Map<String, Double> setColor(Color color);

    /**
     * Get the current Color.
     * @return Either the Color at the given Point2D (if the first method)
     * was used); or the given Color (if the second method was used); or some
     * other Color chosen by the Engine (if slider values have moved since
     * those methods were last invoked).
     */
    Color getColor();

    @Override
    default void accept(GenericOperationVisitor visitor) {
        visitor.visitColorPickerOperation(this);
    }
}
