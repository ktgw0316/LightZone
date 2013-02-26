/*
 * $RCSfile: RectIter.java,v $
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
 * An iterator for traversing a read-only image in top-to-bottom,
 * left-to-right order.  This will generally be the fastest style of
 * iterator, since it does not need to perform bounds checks against
 * the top or left edges of tiles.
 *
 * <p> The iterator is initialized with a particular rectangle as its
 * bounds, which it is illegal to exceed.  This initialization takes
 * place in a factory method and is not a part of the iterator
 * interface itself.  Once initialized, the iterator may be reset to
 * its initial state by means of the startLine(), startPixels(), and
 * startBands() methods.  Its position may be advanced using the
 * nextLine(), jumpLines(), nextPixel(), jumpPixels(), and nextBand()
 * methods.
 *
 * <p> The iterator's position may be tested against the bounding
 * rectangle by means of the finishedLines(), finishedPixels(),
 * and finishedBands() methods, as well as the hybrid methods
 * nextLineDone(), nextPixelDone(), and nextBandDone().
 * 
 * <p> The getSample(), getSampleFloat(), and getSampleDouble()
 * methods are provided to allow read-only access to the source data.
 * The various source bands may also be accessed in random fashion
 * using the variants that accept a band index.  The getPixel() methods
 * allow retrieval of all bands simultaneously.
 *
 * <p> An instance of RectIter may be obtained by means
 * of the RectIterFactory.create() method, which returns
 * an opaque object implementing this interface.
 *
 * @see WritableRectIter
 * @see RectIterFactory
 */
public interface RectIter {

    /**
     * Sets the iterator to the first line of its bounding rectangle.
     * The pixel and band offsets are unchanged.
     */
    void startLines();
      
    /**
     * Sets the iterator to the next line of the image.  The pixel and
     * band offsets are unchanged.  If the iterator passes the bottom
     * line of the rectangles, calls to get() methods are not valid.
     */
    void nextLine();

    /**
     * Sets the iterator to the next line in the image, and returns
     * true if the bottom row of the bounding rectangle has been
     * passed.
     */
    boolean nextLineDone();
    
    /**
     * Jumps downward num lines from the current position.  Num may be
     * negative.  The pixel and band offsets are unchanged.  If the
     * position after the jump is outside of the iterator's bounding
     * box, an <code>IndexOutOfBoundsException</code> will be thrown
     * and the position will be unchanged.
     *
     * @throws IndexOutOfBoundsException if the position goes outside
     *         of the iterator's bounding box.
     */
    void jumpLines(int num);

    /**
     * Returns true if the bottom row of the bounding rectangle has
     * been passed.
     */
    boolean finishedLines();

    /**
     * Sets the iterator to the leftmost pixel of its bounding rectangle.
     * The line and band offsets are unchanged.
     */
    void startPixels();
      
    /**
     * Sets the iterator to the next pixel in image (that is, move
     * rightward).  The line and band offsets are unchanged. 
     * 
     * <p>This method may be used in conjunction with the method
     * <code>finishedPixels</code>. Or the method <code>nextPixelDone</code>, 
     * which sets the iterator to the next pixel and checks the bound, 
     * may be used instead of this method.
     */
    void nextPixel();
      
    /**
     * Sets the iterator to the next pixel in the image (that is, move
     * rightward).  Returns true if the right edge of the bounding
     * rectangle has been passed.  The line and band offsets are
     * unchanged.
     */
    boolean nextPixelDone();
      
    /**
     * Jumps rightward num pixels from the current position.  Num may
     * be negative.  The line and band offsets are unchanged.  If the
     * position after the jump is outside of the iterator's bounding
     * box, an <code>IndexOutOfBoundsException</code> will be thrown
     * and the position will be unchanged.
     *
     * @throws IndexOutOfBoundsException if the position goes outside
     *         of the iterator's bounding box.
     */
    void jumpPixels(int num);
    
    /**
     * Returns true if the right edge of the bounding rectangle has
     * been passed.
     */
    boolean finishedPixels();

    /**
     * Sets the iterator to the first band of the image.
     * The pixel column and line are unchanged.
     */
    void startBands();
      
    /**
     * Sets the iterator to the next band in the image.
     * The pixel column and line are unchanged.
     */
    void nextBand();

    /**
     * Sets the iterator to the next band in the image, and returns
     * true if the max band has been exceeded.  The pixel column and
     * line are unchanged.
     */
    boolean nextBandDone();
      
    /**
     * Returns true if the max band in the image has been exceeded.
     */
    boolean finishedBands();

    /**
     * Returns the current sample as an integer.
     */
    int getSample();

    /**
     * Returns the specified sample of the current pixel as an integer.
     *
     * @param b the band index of the desired sample.
     */
    int getSample(int b);

    /**
     * Returns the current sample as a float.
     */
    float getSampleFloat();

    /**
     * Returns the specified sample of the current pixel as a float.
     *
     * @param b the band index of the desired sample.
     */
    float getSampleFloat(int b);

    /**
     * Returns the current sample as a double.
     */
    double getSampleDouble();

    /**
     * Returns the specified sample of the current pixel as a double.
     *
     * @param b the band index of the desired sample.
     */
    double getSampleDouble(int b);

    /**
     * Returns the samples of the current pixel from the image
     * in an array of int.
     *
     * @param iArray An optionally preallocated int array.
     * @return the contents of the pixel as an int array.
     */
    int[] getPixel(int[] iArray);

    /**
     * Returns the samples of the current pixel from the image
     * in an array of float.
     *
     * @param fArray An optionally preallocated float array.
     * @return the contents of the pixel as a float array.
     */
    float[] getPixel(float[] fArray);

    /**
     * Returns the samples of the current pixel from the image
     * in an array of double.
     *
     * @param dArray An optionally preallocated double array.
     * @return the contents of the pixel as a double array.
     */
    double[] getPixel(double[] dArray);
}
