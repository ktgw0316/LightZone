/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Regions define masks for image processing Operations.  A Contour is a
 * connected component of a Region.  Regions are unions of Contours.
 * <p>
 * Contours are immutable and safe for concurrent access, except for the
 * <code>getTranslation()</code> accessor which may update to indicate the
 * special case change of getting translated.
 * <p>
 * @see com.lightcrafts.model.Region
 */

public interface Contour {

    /**
     * Get the Shape which defines the outer boundary of the transition zone
     * surrounding this Contour.
     * @return A Shape which defines the outer boundary of the transition
     * zone.
     */
    Shape getOuterShape();

    /**
     * The inner Shape and the outer Shape are related by a gap of fixed size.
     * @return The size of the gap between the two Shapes.
     */
    float getWidth();

    /**
     * Contours may be modified by translation.  If this Contour has been
     * translated since it was instantiated, this method will return the
     * offset to apply to the Shapes.  If this Contour has never been
     * translated, then the Shapes are accurate and this method returns null.
     * @return An offset to apply to the Shapes, or null if the Shapes are
     * up to date.
     */
    Point2D getTranslation();
}
