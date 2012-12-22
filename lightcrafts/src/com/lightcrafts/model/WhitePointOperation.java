/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import java.awt.geom.Point2D;
import java.awt.*;

/**
 * An Operation for specifying color balance by picking a single color,
 * either explicitly or by picking a point in the image.
 * <p>
 * This Operation is no longer directly accessible in LightZone.  It has been
 * retained only for backwards compatibility with saved documents.
 */
public interface WhitePointOperation extends Operation {

    /**
     * Set the color balance such that the given location on the image has
     * neutral color.
     * @param p A point in image coordinates that should acquire neutral
     * color.
     */
    void setWhitePoint(Point2D p);

    /**
     * Set the color balance such that the given color in the image before
     * color balancing transforms into a neutral color.
     * @param color A color which should be transformed by this Operation
     * into a neutral color.
     */
    void setWhitePoint(Color color);

    /**
     * Get the current white point Color.
     * @return Either the Color at the given Point2D (if the first method)
     * was used); or the given Color (if the second method was used); or
     * some other Color chosen by the Engine to represent neutrality, if
     * neither method has yet been invoked on this Operation).
     */
    Color getWhitePoint();
}
