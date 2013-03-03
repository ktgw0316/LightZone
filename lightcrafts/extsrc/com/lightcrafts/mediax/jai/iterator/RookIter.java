/*
 * $RCSfile: RookIter.java,v $
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
 * An iterator for traversing a read-only image using arbitrary
 * up-down and left-right moves.  This will generally be somewhat
 * slower than a corresponding instance of RectIter, since it must
 * perform bounds checks against the top and left edges of tiles in
 * addition to their bottom and right edges.
 *
 * <p> The iterator is initialized with a particular rectangle as its
 * bounds, which it is illegal to exceed.  This initialization takes
 * place in a factory method and is not a part of the iterator
 * interface itself.  Once initialized, the iterator may be reset to
 * its initial state by means of the startLine(), startPixels(), and
 * startBands() methods.  As with RectIter, its position may be
 * advanced using the nextLine(), jumpLines(), nextPixel(),
 * jumpPixels(), and nextBand() methods.
 *
 * <p> In addition, prevLine(), prevPixel(), and prevBand() methods
 * exist to move in the upwards and leftwards directions and to access
 * smaller band indices.  The iterator may be set to the far edges of
 * the bounding rectangle by means of the endLines(), endPixels(), and
 * endBands() methods.
 *
 * <p> The iterator's position may be tested against the bounding
 * rectangle by means of the finishedLines(), finishedPixels(), and
 * finishedBands() methods, as well as the hybrid methods
 * nextLineDone(), prevLineDone(), nextPixelDone(), prevPixelDone(),
 * nextBandDone(), and prevBandDone().
 *
 * <p> The getSample(), getSampleFloat(), and getSampleDouble()
 * methods are provided to allow read-only access to the source data.
 * The various source bands may also be accessed in random fashion
 * using the variants that accept a band index.  The getPixel() methods
 * allow retrieval of all bands simultaneously.
 *
 * <p> An instance of RookIter may be obtained by means
 * of the RookIterFactory.create() method, which returns
 * an opaque object implementing this interface.
 *
 * @see RectIter
 * @see RookIterFactory
 */
public interface RookIter extends RectIter {

    /**
     * Sets the iterator to the previous line of the image.  The pixel
     * and band offsets are unchanged.  If the iterator passes the
     * top line of the rectangle, calls to get() methods are not
     * valid.
     */
    public void prevLine();

    /**
     * Sets the iterator to the previous line in the image, and
     * returns true if the top row of the bounding rectangle has
     * been passed.
     */
    public boolean prevLineDone();

    /**
     * Sets the iterator to the last line of its bounding rectangle.
     * The pixel and band offsets are unchanged.
     */
    void endLines();

    /**
     * Sets the iterator to the previous pixel in the image (that is,
     * move leftward).  The line and band offsets are unchanged.
     */
    public void prevPixel();

    /**
     * Sets the iterator to the previous pixel in the image (that is,
     * move leftward).  Returns true if the left edge of the bounding
     * rectangle has been passed.  The line and band offsets are
     * unchanged.
     */
    public boolean prevPixelDone();

    /**
     * Sets the iterator to the rightmost pixel of its bounding rectangle.
     * The line and band offsets are unchanged.
     */
    void endPixels();

    /**
     * Sets the iterator to the previous band in the image.
     * The pixel column and line are unchanged.
     */
    void prevBand();

    /**
     * Sets the iterator to the previous band in the image, and
     * returns true if the min band has been exceeded.  The pixel
     * column and line are unchanged.
     */
    boolean prevBandDone();

    /**
     * Sets the iterator to the last band of the image.
     * The pixel column and line are unchanged.
     */
   void endBands();
}
