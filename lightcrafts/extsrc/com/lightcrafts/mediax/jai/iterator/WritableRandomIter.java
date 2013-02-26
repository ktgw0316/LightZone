/*
 * $RCSfile: WritableRandomIter.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:27 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.iterator;

/**
 * An iterator that allows random read/write access to any sample
 * within its bounding rectangle.  This flexibility will generally
 * exact a corresponding price in speed and setup overhead.
 *
 * <p> The iterator is initialized with a particular rectangle as its
 * bounds, which it is illegal to exceed.  This initialization takes
 * place in a factory method and is not a part of the iterator
 * interface itself.
 *
 * <p> The setSample() and setPixel() methods allow individual source
 * samples and whole pixels to be written.
 *
 * <p> An instance of RandomIter may be obtained by means of the
 * RandomIterFactory.createWritable() method, which returns an
 * opaque object implementing this interface.
 *
 * @see RandomIter
 * @see RandomIterFactory
 */
public interface WritableRandomIter extends RandomIter {
    
    /**
     * Sets the specified sample of the image to an integral value.
     *
     * @param x the X coordinate of the pixel.
     * @param y the Y coordinate of the pixel.
     * @param b the band to be set.
     * @param s the sample's new integral value.
     */
    void setSample(int x, int y, int b, int s);

    /**
     * Sets the specified sample of the image to a float value.
     *
     * @param x the X coordinate of the pixel.
     * @param y the Y coordinate of the pixel.
     * @param b the band to be set.
     * @param s the sample's new float value.
     */
    void setSample(int x, int y, int b, float s);

    /**
     * Sets the specified sample of the image to a double value.
     *
     * @param x the X coordinate of the pixel.
     * @param y the Y coordinate of the pixel.
     * @param b the band to be set.
     * @param s the sample's new double value.
     */
    void setSample(int x, int y, int b, double s);

    /**
     * Sets a pixel in the image using an int array of samples for input.
     *
     * @param x the X coordinate of the pixel.
     * @param y the Y coordinate of the pixel.
     * @param iArray the input samples in an int array.
     */
    void setPixel(int x, int y, int[] iArray);

    /**
     * Sets a pixel in the image using a float array of samples for input.
     *
     * @param x the X coordinate of the pixel.
     * @param y the Y coordinate of the pixel.
     * @param iArray the input samples in a float array.
     */
    void setPixel(int x, int y, float[] fArray);

    /**
     * Sets a pixel in the image using a float array of samples for input.
     *
     * @param x the X coordinate of the pixel.
     * @param y the Y coordinate of the pixel.
     * @param dArray the input samples in a double array.
     */
    void setPixel(int x, int y, double[] dArray);
}
