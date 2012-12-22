/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.model;

/**
 * A kind of Operation that performs the zone-mapping transformation.
 */

public interface ZoneOperation extends Operation {

    /**
     * In setScale(), specify that zones are defined as ranges of
     * RGB values.  This is the default.
     */
    final static int RgbScale = 0;

    /**
     * In setScale(), specify that zones are defined as ranges of
     * luminosity values.
     */
    final static int LuminosityScale = 1;

    /**
     * In setScale(), specify that zones are defined as ranges of
     * chrominmance values.
     */
    final static int ChromaScale = 2;

    /**
     * Specify whether this ZoneOperation should act on zones defined as
     * intervals of RGB values or as intervals of luminosity.  The default
     * is RGB.
     * @param scale Either RgbScale or LuminosityScale.
     * @throws IllegalArgumentException If the given scale is not equal
     * to either of these constants.
     */
    void setScale(int scale);

    /**
     * The action of this Operation is determined by a transfer function on
     * the unit interval.  This transfer function is defined by an array of
     * doubles, interpreted like this:
     * <ol>
     * <li>The length of the array defines control points along the ordinate
     * at zero, one, and uniformly spaced in between.  (The array must have
     * at least two elements.)</li>
     * <li>Each value in the array is either between 0 and 1 inclusive, or
     * negative.  If it is negative, then the corresponding control point is
     * inactive.  If the value is in the unit interval, then the control
     * point is active.</li>
     * </ol>
     * @param points An array of numbers between 0 and 1 or negative, to be
     * interpreted as values of a transfer function on the interval.
     */
    void setControlPoints(double[] points);

    /**
     * Get the interpolated value of the transfer function at the given
     * index.  If the most recent value in a points array submitted to
     * setControlPoints() at the given index was nonnegative, then the
     * submitted value is returned.  If it was negative, then a computed
     * interpolating value is returned.
     * <p>
     * If setControlPoints() has never been called, the result of this method
     * is undefined.
     * @param index A pointer into the most recent array submitted to
     * setControlPoints().
     */
    double getControlPoint(int index);

    /** Notify this ZoneOperation that a particular control point has gained
      * focus in the user interface.  This is provided specifically to
      * allow interactive repainting of the zone-finder Preview feature.
      * @param index The index of a control point to focus, or if negative,
      * an indication that no point currently has focus.
      */
    void setFocusPoint(int index);
}
