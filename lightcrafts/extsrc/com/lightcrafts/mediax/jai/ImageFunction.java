/*
 * $RCSfile: ImageFunction.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:09 $
 * $State: Exp $
 */ 
package com.lightcrafts.mediax.jai;

/**
 * ImageFunction is a common interface for vector-valued functions which
 * are to be evaluated at positions in the X-Y coordinate system.  At
 * each position the value of such a function may contain one or more
 * elements each of which may be complex.
 */
public interface ImageFunction {

    /** Returns whether or not each value's elements are complex. */
    boolean isComplex();

    /** Returns the number of elements per value at each position. */
    int getNumElements();

    /**
     * Returns all values of a given element for a specified set of
     * coordinates.  An ArrayIndexOutOfBoundsException may be thrown if
     * the length of the supplied array(s) is insufficient.
     *
     * @param startX The X coordinate of the upper left location to evaluate.
     * @param startY The Y coordinate of the upper left location to evaluate.
     * @param deltaX The horizontal increment.
     * @param deltaY The vertical increment.
     * @param countX The number of points in the horizontal direction.
     * @param countY The number of points in the vertical direction.
     * @param real A pre-allocated float array of length at least countX*countY
     * in which the real parts of all elements will be returned.
     * @param imag A pre-allocated float array of length at least countX*countY
     * in which the imaginary parts of all elements will be returned; may be
     * null for real data, i.e., when <code>isComplex()</code> returns false.
     *
     * @throws ArrayIndexOutOfBoundsException if the length of the supplied
     * array(s) is insufficient.
     */
    void getElements(float startX, float startY,
		     float deltaX, float deltaY,
		     int countX, int countY,
		     int element,
		     float[] real, float[] imag);

    /**
     * Returns all values of a given element for a specified set of
     * coordinates.  An ArrayIndexOutOfBoundsException may be thrown if
     * the length of the supplied array(s) is insufficient.
     *
     * @param startX The X coordinate of the upper left location to evaluate.
     * @param startY The Y coordinate of the upper left location to evaluate.
     * @param deltaX The horizontal increment.
     * @param deltaY The vertical increment.
     * @param countX The number of points in the horizontal direction.
     * @param countY The number of points in the vertical direction.
     * @param real A pre-allocated double array of length at least
     * countX*countY in which the real parts of all elements will be returned.
     * @param imag A pre-allocated double array of length at least
     * countX*countY in which the imaginary parts of all elements will be
     * returned; may be null for real data, i.e., when
     * <code>isComplex()</code> returns false.
     *
     * @throws ArrayIndexOutOfBoundsException if the length of the supplied
     * array(s) is insufficient.
     */
    void getElements(double startX, double startY,
		     double deltaX, double deltaY,
		     int countX, int countY,
		     int element,
		     double[] real, double[] imag);
}

