/*
 * $RCSfile: RandomIter.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:26 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.iterator;

/**
 * An iterator that allows random read-only access to any sample
 * within its bounding rectangle.  This flexibility will generally
 * exact a corresponding price in speed and setup overhead.
 *
 * <p> The iterator is initialized with a particular rectangle as its
 * bounds, which it is illegal to exceed.  This initialization takes
 * place in a factory method and is not a part of the iterator
 * interface itself.
 *
 * <p> The getSample(), getSampleFloat(), and getSampleDouble()
 * methods are provided to allow read-only access to the source data.
 * The getPixel() methods allow retrieval of all bands simultaneously.
 *
 * <p> An instance of RandomIter may be obtained by means of the
 * RandomIterFactory.create() method, which returns an opaque
 * object implementing this interface.
 *
 * @see WritableRandomIter
 * @see RandomIterFactory
 */
public interface RandomIter {

    /**
     * Returns the specified sample from the image.
     *
     * @param x the X coordinate of the desired pixel.
     * @param y the Y coordinate of the desired pixel.
     * @param b the band to retrieve.
     */
    int getSample(int x, int y, int b);

    /**
     * Returns the specified sample from the image as a float.
     *
     * @param x the X coordinate of the desired pixel.
     * @param y the Y coordinate of the desired pixel.
     * @param b the band to retrieve.
     */
    float getSampleFloat(int x, int y, int b);

    /**
     * Returns the specified sample from the image as a double.
     *
     * @param x the X coordinate of the desired pixel.
     * @param y the Y coordinate of the desired pixel.
     * @param b the band to retrieve.
     */
    double getSampleDouble(int x, int y, int b);

    /**
     * Returns the samples of the specified pixel from the image
     * in an array of int.
     *
     * @param x the X coordinate of the desired pixel.
     * @param y the Y coordinate of the desired pixel.
     * @param iArray An optionally preallocated int array.
     * @return the contents of the pixel as an int array.
     */
    int[] getPixel(int x, int y, int[] iArray);

    /**
     * Returns the samples of the specified pixel from the image
     * in an array of float.
     *
     * @param x the X coordinate of the desired pixel.
     * @param y the Y coordinate of the desired pixel.
     * @param fArray An optionally preallocated float array.
     * @return the contents of the pixel as a float array.
     */
    float[] getPixel(int x, int y, float[] fArray);

    /**
     * Returns the samples of the specified pixel from the image
     * in an array of double.
     *
     * @param x the X coordinate of the desired pixel.
     * @param y the Y coordinate of the desired pixel.
     * @param dArray An optionally preallocated double array.
     * @return the contents of the pixel as a double array.
     */
    double[] getPixel(int x, int y, double[] dArray);

    /**
     * Informs the iterator that it may discard its internal data
     * structures.  This method should be called when the iterator
     * will no longer be used.
     */
    void done();
}
