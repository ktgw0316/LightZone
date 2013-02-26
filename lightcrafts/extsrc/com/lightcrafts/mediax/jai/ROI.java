/*
 * $RCSfile: ROI.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:18 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;

import com.lightcrafts.media.jai.util.ImageUtil;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.MultiPixelPackedSampleModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Vector;
import com.lightcrafts.mediax.jai.iterator.RandomIter;
import com.lightcrafts.mediax.jai.iterator.RandomIterFactory;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;

/**
 * The parent class for representations of a region of interest of an
 * image (currently only single band images with integral data types
 * are supported).
 * This class represents region information in image form, and
 * can thus be used as a fallback where a <code>Shape</code>
 * representation is unavailable.  Where possible, subclasses such as
 * ROIShape are used since they provide a more compact means of
 * storage for large regions.
 *
 * <p> The getAsShape() method may be called optimistically on any
 * instance of ROI; however, it may return null to indicate that a
 * <code>Shape</code> representation of the ROI is not available.  In
 * this case, getAsImage() should be called as a fallback.
 *
 * <p> Inclusion and exclusion of pixels is defined by a threshold value.
 * Pixel values greater than or equal to the threshold indicate inclusion.
 *
 */
public class ROI implements Serializable {

    /** A RandomIter used to grab pixels from the ROI. */
    private transient RandomIter iter = null;

    /** The <code>PlanarImage</code> representation of the ROI. */
    transient PlanarImage theImage = null;

    /** The inclusion/exclusion threshold of the ROI. */
    int threshold = 127;

    /**
     * Merge a <code>LinkedList</code> of <code>Rectangle</code>s
     * representing run lengths of pixels in the ROI into a minimal
     * list wherein vertically abutting <code>Rectangle</code>s are
     * merged. The operation is effected in place.
     *
     * @param rectList The list of run length <code>Rectangle</code>s.
     * @throws IllegalArgumentException if rectList is null.
     * @return The merged list.
     */
    protected static LinkedList mergeRunLengthList(LinkedList rectList) {

        if ( rectList == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // Merge the run length rectangles if more than one was detected.
        if (rectList.size() > 1) {
            // Traverse the list sequentially merging all subsequent
            // vertically abutting Rectangles with the same abscissa
            // origin and width with the current starting Rectangle.
            for (int mergeIndex = 0;
                mergeIndex < rectList.size() - 1;
                mergeIndex++) {

                ListIterator rectIter = rectList.listIterator(mergeIndex);
                Rectangle mergeRect = (Rectangle)rectIter.next();

                while (rectIter.hasNext()) {
                    Rectangle runRect = (Rectangle)rectIter.next();

                    // Calculate ordinate value of abutting rectangle.
                    int abuttingY = mergeRect.y + mergeRect.height;

                    if (runRect.y == abuttingY &&
                       runRect.x == mergeRect.x &&
                       runRect.width == mergeRect.width) {
                        mergeRect =
                            new Rectangle(mergeRect.x, mergeRect.y,
                                          mergeRect.width,
                                          mergeRect.height + runRect.height);

                        // Remove "runRect" from the list.
                        rectIter.remove();

                        // Replace "mergeRect" with updated version.
                        rectList.set(mergeIndex, (Object)mergeRect);
                    } else if (runRect.y > abuttingY) {
                        // All Rectangles in the list with index greater than
                        // mergeIndex are runlength Rectangles and are sorted
                        // in non-decreasing ordinate order. Therefore there
                        // are no more Rectangles which could possibly be
                        // merged with mergeRect.
                        break;
                    }
                }
            }
        }

        return rectList;
    }

    /**
      * The default constructor.
      *
      * Using this constructor means that the subclass must override
      * all methods that reference theImage.
      */
    protected ROI() {}

    /**
     * Constructs an ROI from a RenderedImage.  The inclusion
     * threshold is taken to be halfway between the minimum and maximum
     * sample values specified by the image's SampleModel.
     *
     * @param im A single-banded RenderedImage.
     *
     * @throws IllegalArgumentException if im is null.
     * @throws IllegalArgumentException if im does not have exactly one band
     */
    public ROI(RenderedImage im) {
	this(im, 127);
    }

    /**
     * Constructs an ROI from a RenderedImage.  The inclusion
     * threshold is specified explicitly.
     *
     * @param im A single-banded RenderedImage.
     * @param threshold The desired inclusion threshold.
     *
     * @throws IllegalArgumentException if im is null.
     * @throws IllegalArgumentException if im does not have exactly one band
     */
    public ROI(RenderedImage im, int threshold) {

	if (im == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
	}

	SampleModel sm = im.getSampleModel();

        if (sm.getNumBands() != 1) {
	    throw new IllegalArgumentException(JaiI18N.getString("ROI0"));
        }

        this.threshold = threshold;

	// If the image is already binary and the threshold is >1
	// then there is no work to do.
	if ((threshold >= 1) && ImageUtil.isBinary(sm)) {
	    theImage = PlanarImage.wrapRenderedImage(im);

	// Otherwise binarize the image for efficiency.
	} else {

	    ParameterBlockJAI pbj = new ParameterBlockJAI("binarize");

	    pbj.setSource("source0", im);
	    pbj.setParameter("threshold", (double)threshold);

	    theImage = JAI.create("binarize", pbj, null);
	}
    }

    /** Get the iterator, construct it if need be. */
    private RandomIter getIter() {
        if (iter == null) {
            iter = RandomIterFactory.create(theImage, null);
        }
        return iter;
    }

    /** Returns the inclusion/exclusion threshold value. */
    public int getThreshold() {
        return threshold;
    }

    /** Sets the inclusion/exclusion threshold value. */
    public void setThreshold(int threshold) {
        this.threshold = threshold;
	((RenderedOp)theImage).setParameter((double)threshold, 0);
	iter = null;
	getIter();
    }

    /** Returns the bounds of the ROI as a <code>Rectangle</code>. */
    public Rectangle getBounds() {
        return new Rectangle(theImage.getMinX(),
                             theImage.getMinY(),
                             theImage.getWidth(),
                             theImage.getHeight());
    }

    /** Returns the bounds of the ROI as a <code>Rectangle2D</code>. */
    public Rectangle2D getBounds2D() {
        return new Rectangle2D.Float((float) theImage.getMinX(),
                                     (float) theImage.getMinY(),
                                     (float) theImage.getWidth(),
                                     (float) theImage.getHeight());
    }

    /**
     * Returns <code>true</code> if the ROI contains a given Point.
     *
     * @param p A Point identifying the pixel to be queried.
     * @throws IllegalArgumentException if p is null.
     * @return <code>true</code> if the pixel lies within the ROI.
     */
    public boolean contains(Point p) {
        if ( p == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return contains(p.x, p.y);
    }

    /**
     * Returns <code>true</code> if the ROI contains a given Point2D.
     *
     * @param p A Point2D identifying the pixel to be queried.
     * @throws IllegalArgumentException if p is null.
     * @return <code>true</code> if the pixel lies within the ROI.
     */
    public boolean contains(Point2D p) {
        if ( p == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return contains((int) p.getX(), (int) p.getY());
    }

    /**
     * Returns <code>true</code> if the ROI contains the point (x, y).
     *
     * @param x An int specifying the X coordinate of the pixel to be queried.
     * @param y An int specifying the Y coordinate of the pixel to be queried.
     * @return <code>true</code> if the pixel lies within the ROI.
     */
    public boolean contains(int x, int y) {
        int minX = theImage.getMinX();
        int minY = theImage.getMinY();

        return (x >= minX && x < minX + theImage.getWidth()) &&
	       (y >= minY && y < minY + theImage.getHeight()) &&
               (getIter().getSample(x, y, 0) >= 1);
    }

    /**
     * Returns <code>true</code> if the ROI contain the point (x, y).
     *
     * @param x A double specifying the X coordinate of the pixel
     *        to be queried.
     * @param y A double specifying the Y coordinate of the pixel
     *        to be queried.
     * @return <code>true</code> if the pixel lies within the ROI.
     */
    public boolean contains(double x, double y) {
        return contains((int) x, (int) y);
    }

    /**
     * Returns <code>true</code> if a given <code>Rectangle</code> is
     * entirely included within the ROI.
     *
     * @param rect A <code>Rectangle</code> specifying the region to be tested
     *        for inclusion.
     * @throws IllegalArgumentException if rect is null.
     * @return <code>true</code> if the rectangle is entirely
     *         contained within the ROI.
     */
    public boolean contains(Rectangle rect) {
        if ( rect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (!rect.equals(rect.intersection(getBounds()))) {
            return false;
        }

	byte[] packedData =
	    ImageUtil.getPackedBinaryData(theImage.getData(), rect);

	// ImageUtil.getPackedBinaryData does not zero out the extra
	// bits used to pad to the nearest byte - therefore ignore
	// these bits.
	int leftover = rect.width % 8;

	if (leftover == 0) {

	    for (int i = 0; i < packedData.length; i++)
		if ((packedData[i] & 0xff) != 0xff)
		    return false;

	} else {

	    int mask = ((1 << leftover) - 1) << (8 - leftover);

	    for (int y = 0, k = 0; y < rect.height; y++) {
		for (int x = 0 ; x < rect.width-leftover; x += 8, k++) {
		    if ((packedData[k] & 0xff) != 0xff)
			return false;
		}

		if ((packedData[k] & mask) != mask)
		    return false;

		k++;
	    }
	}

        return true;
    }

    /**
     * Returns <code>true</code> if a given <code>Rectangle2D</code> is
     * entirely included within the ROI.
     *
     * @param rect A <code>Rectangle2D</code> specifying the region to be
     *        tested for inclusion.
     * @throws IllegalArgumentException if rect is null.
     * @return <code>true</code> if the rectangle is entirely contained
     *         within the ROI.
     */
    public boolean contains(Rectangle2D rect) {
        if ( rect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }
        Rectangle r = new Rectangle((int) rect.getX(),
                                    (int) rect.getY(),
                                    (int) rect.getWidth(),
                                    (int) rect.getHeight());
        return contains(r);
    }

    /**
     * Returns <code>true</code> if a given rectangle (x, y, w, h) is entirely
     * included within the ROI.
     *
     * @param x The int X coordinate of the upper left corner of the region.
     * @param y The int Y coordinate of the upper left corner of the region.
     * @param w The int width of the region.
     * @param h The int height of the region.
     * @return <code>true</code> if the rectangle is entirely contained
     *         within the ROI.
     */
    public boolean contains(int x, int y, int w, int h) {
        Rectangle r = new Rectangle(x, y, w, h);
        return contains(r);
    }

    /**
     * Returns <code>true</code> if a given rectangle (x, y, w, h) is entirely
     * included within the ROI.
     *
     * @param x The double X coordinate of the upper left corner of the region.
     * @param y The double Y coordinate of the upper left corner of the region.
     * @param w The double width of the region.
     * @param h The double height of the region.
     *
     * @return <code>true</code> if the rectangle is entirely
     * contained within the ROI.
     */
    public boolean contains(double x, double y, double w, double h) {
        Rectangle rect = new Rectangle((int) x, (int) y,
                                       (int) w, (int) h);
        return contains(rect);
    }

    /**
     * Returns <code>true</code> if a given <code>Rectangle</code>
     * intersects the ROI.
     *
     * @param rect A <code>Rectangle</code> specifying the region to be tested
     *        for inclusion.
     * @throws IllegalArgumentException if rect is null.
     * @return <code>true</code> if the rectangle intersects the ROI.
     */
    public boolean intersects(Rectangle rect) {
        if ( rect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Rectangle r = rect.intersection(getBounds());

        if (r.isEmpty()) {
            return false;
        }

	byte[] packedData =
	    ImageUtil.getPackedBinaryData(theImage.getData(), r);

	// ImageUtil.getPackedBinaryData does not zero out the extra
	// bits used to pad to the nearest byte - therefore ignore
	// these bits.
	int leftover = r.width % 8;

	if (leftover == 0) {

	    for (int i = 0; i < packedData.length; i++)
		if ((packedData[i] & 0xff) != 0)
		    return true;

	} else {

	    int mask = ((1 << leftover) - 1) << (8 - leftover);

	    for (int y = 0, k = 0; y < r.height; y++) {
		for (int x = 0 ; x < r.width-leftover; x += 8, k++) {
		    if ((packedData[k] & 0xff) != 0)
			return true;
		}
		if ((packedData[k] & mask) != 0)
		    return true;
		k++;
	    }
	}

        return false;
    }

    /**
     * Returns <code>true</code> if a given <code>Rectangle2D</code>
     * intersects the ROI.
     *
     * @param r A <code>Rectangle2D</code> specifying the region to be tested
     *        for inclusion.
     * @throws IllegalArgumentException if r is null.
     * @return <code>true</code> if the rectangle intersects the ROI.
     */
    public boolean intersects(Rectangle2D r) {
        if ( r == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        Rectangle rect = new Rectangle((int) r.getX(),
                                       (int) r.getY(),
                                       (int) r.getWidth(),
                                       (int) r.getHeight());
        return intersects(rect);
    }

    /**
     * Returns <code>true</code> if a given rectangular region
     * intersects the ROI.
     *
     * @param x The int X coordinate of the upper left corner of the region.
     * @param y The int Y coordinate of the upper left corner of the region.
     * @param w The int width of the region.
     * @param h The int height of the region.
     * @return <code>true</code> if the rectangle intersects the ROI.
     */
    public boolean intersects(int x, int y, int w, int h) {
        Rectangle rect = new Rectangle(x, y, w, h);
        return intersects(rect);
    }

    /**
     * Returns <code>true</code> if a given rectangular region
     * intersects the ROI.
     *
     * @param x The double X coordinate of the upper left corner of the region.
     * @param y The double Y coordinate of the upper left corner of the region.
     * @param w The double width of the region.
     * @param h The double height of the region.
     * @return <code>true</code> if the rectangle intersects the ROI.
     */
    public boolean intersects(double x, double y, double w, double h) {
        Rectangle rect = new Rectangle((int) x, (int) y,
                                       (int) w, (int) h);
        return intersects(rect);
    }

    /**
     * Create a binary PlanarImage of the size/bounds specified by
     * the rectangle.
     */
    private static PlanarImage createBinaryImage(Rectangle r) {

	if ((r.x == 0) && (r.y == 0)) {

	    BufferedImage bi =
		    new BufferedImage(r.width, r.height,
			    BufferedImage.TYPE_BYTE_BINARY);

	    return PlanarImage.wrapRenderedImage(bi);

	} else {

	    SampleModel sm =
		new MultiPixelPackedSampleModel(
			DataBuffer.TYPE_BYTE, r.width, r.height, 1);

	    // Create a TiledImage into which to write.
	    return new TiledImage(r.x, r.y, r.width, r.height, r.x, r.y,
				sm, PlanarImage.createColorModel(sm));
	}
    }

    /**
     * Creates a merged ROI by performing the specified image operation
     * on <code>this</code> image and the image of the specified ROI.
     *
     * @param ROI the ROI to merge with <code>this</code>
     * @param op  the JAI operator to use for merge.
     *
     * @return the merged ROI
     */
    private ROI createOpROI(ROI roi, String op) {

        if (roi == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

	PlanarImage imThis = this.getAsImage();
	PlanarImage imROI  =  roi.getAsImage();
	PlanarImage imDest;

        Rectangle boundsThis = imThis.getBounds();
        Rectangle boundsROI  =  imROI.getBounds();

	// If the bounds of the two images do not match, then
	// expand as necessary to the union of the two bounds
	// using the "overlay" operator and then perform the JAI
	// operation.
	if (op.equals("and") || boundsThis.equals(boundsROI)) {
	    imDest = JAI.create(op, imThis, imROI);

	} else if (op.equals("subtract") || boundsThis.contains(boundsROI)) {

	    PlanarImage imBounds = createBinaryImage(boundsThis);

	    imBounds = JAI.create("overlay", imBounds, imROI);
	    imDest   = JAI.create(op       , imThis, imBounds);

	} else if (boundsROI.contains(boundsThis)) {

	    PlanarImage imBounds = createBinaryImage(boundsROI);

	    imBounds = JAI.create("overlay", imBounds, imThis);
	    imDest   = JAI.create(op       , imBounds, imROI);

	} else {

	    Rectangle merged = boundsThis.union(boundsROI);

	    PlanarImage imBoundsThis = createBinaryImage(merged);
	    PlanarImage imBoundsROI  = createBinaryImage(merged);

	    imBoundsThis = JAI.create("overlay", imBoundsThis, imThis);
	    imBoundsROI  = JAI.create("overlay", imBoundsROI , imROI );
	    imDest	 = JAI.create(op       , imBoundsThis, imBoundsROI);
	}

	return new ROI(imDest, threshold);
    }

    /**
     * Adds another <code>ROI</code> to this one and returns the result
     * as a new <code>ROI</code>. The supplied <code>ROI</code> will
     * be converted to a rendered form if necessary. The bounds of the
     * resultant <code>ROI</code> will be the union of the bounds of the
     * two <code>ROI</code>s being merged.
     *
     * @param roi An ROI.
     * @throws IllegalArgumentException if roi is null.
     * @return A new ROI containing the new ROI data.
     */
    public ROI add(ROI roi) {
        return createOpROI(roi, "add");
    }

    /**
     * Subtracts another <code>ROI</code> from this one and returns the
     * result as a new <code>ROI</code>. The supplied <code>ROI</code>
     * will be converted to a rendered form if necessary. The
     * bounds of the resultant <code>ROI</code> will be the same as
     * <code>this</code> <code>ROI</code>.
     *
     * @param roi An ROI.
     * @throws IllegalArgumentException if roi is null.
     * @return A new ROI containing the new ROI data.
     */
    public ROI subtract(ROI roi) {
        return createOpROI(roi, "subtract");
    }

    /**
     * Intersects the <code>ROI</code> with another <code>ROI</code> and returns the result as
     * a new <code>ROI</code>. The supplied <code>ROI</code> will be converted to a rendered
     * form if necessary. The bounds of the resultant <code>ROI</code> will be the
     * intersection of the bounds of the two <code>ROI</code>s being merged.
     *
     * @param roi An ROI.
     * @throws IllegalArgumentException if roi is null.
     * @return A new ROI containing the new ROI data.
     */
    public ROI intersect(ROI roi) {
        return createOpROI(roi, "and");
    }

    /**
     * Exclusive-ors the <code>ROI</code> with another <code>ROI</code>
     * and returns the result as a new <code>ROI</code>. The supplied
     * <code>ROI</code> will be converted to a rendered form if
     * necessary. The bounds of the resultant <code>ROI</code> will
     * be the union of the bounds of the two <code>ROI</code>s being
     * merged.
     *
     * @param roi An ROI.
     * @throws IllegalArgumentException if roi is null.
     * @return A new ROI containing the new ROI data.
     */
    public ROI exclusiveOr(ROI roi) {
        return createOpROI(roi, "xor");
    }

    /**
     * Performs an affine transformation and returns the result as a new
     * ROI.  The transformation is performed by an "Affine" RIF using the
     * indicated interpolation method.
     *
     * @param at an AffineTransform specifying the transformation.
     * @param interp the Interpolation to be used.
     * @throws IllegalArgumentException if at is null.
     * @throws IllegalArgumentException if interp is null.
     * @return a new ROI containing the transformed ROI data.
     */
    public ROI transform(AffineTransform at, Interpolation interp) {

	if (at == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("ROI5"));
	}

	if (interp == null) {
	    throw new IllegalArgumentException(JaiI18N.getString("ROI6"));
	}

        ParameterBlock paramBlock = new ParameterBlock();
        paramBlock.add(at);
        paramBlock.add(interp);
        return performImageOp("Affine", paramBlock, 0, null);
    }

    /**
     * Performs an affine transformation and returns the result as a new
     * ROI.  The transformation is performed by an "Affine" RIF using
     * nearest neighbor interpolation.
     *
     * @param at an AffineTransform specifying the transformation.
     * @throws IllegalArgumentException if at is null.
     * @return a new ROI containing the transformed ROI data.
     */
    public ROI transform(AffineTransform at) {
        if ( at == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        return transform(at,
                      Interpolation.getInstance(Interpolation.INTERP_NEAREST));
    }

    /**
     * Transforms an ROI using an imaging operation.  The operation is
     * specified by a <code>RenderedImageFactory</code>.  The
     * operation's <code>ParameterBlock</code>, minus the image source
     * itself is supplied, along with an index indicating where to
     * insert the ROI image.  The <code>renderHints</code> argument
     * allows rendering hints to be passed in.
     *
     * @param RIF A <code>RenderedImageFactory</code> that will be used
     *        to create the op.
     * @param paramBlock A <code>ParameterBlock</code> containing all
     *        sources and parameters for the op except for the ROI itself.
     * @param sourceIndex The index of the <code>ParameterBlock</code>'s
     *        sources where the ROI is to be inserted.
     * @param renderHints A <code>RenderingHints</code> object containing
     *        rendering hints, or null.
     * @throws IllegalArgumentException if RIF is null.
     * @throws IllegalArgumentException if paramBlock is null.
     */
    public ROI performImageOp(RenderedImageFactory RIF,
                              ParameterBlock paramBlock,
                              int sourceIndex,
                              RenderingHints renderHints) {

        if (  RIF == null || paramBlock == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // Clone the ParameterBlock and insert a source
        ParameterBlock pb = (ParameterBlock) paramBlock.clone();
        Vector sources = pb.getSources();
        sources.insertElementAt(this.getAsImage(), sourceIndex);

        // Create a new RenderedImage based on the RIF
        // and ParameterBlock.
        RenderedImage im = RIF.create(pb, renderHints);
        return new ROI(im, threshold);
    }

    /**
     * Transforms an ROI using an imaging operation.  The
     * operation is specified by name; the default JAI registry is
     * used to resolve this into a RIF.  The operation's
     * <code>ParameterBlock</code>, minus the image source itself is supplied,
     * along with an index indicating where to insert the ROI image.
     * The <code>renderHints</code> argument allows rendering hints to
     * be passed in.
     *
     * @param name The name of the operation to perform.
     * @param paramBlock A <code>ParameterBlock</code> containing all
     *        sources and parameters for the op except for the ROI itself.
     * @param sourceIndex The index of the <code>ParameterBlock</code>'s
     *        sources where the ROI is to be inserted.
     * @param renderHints A <code>RenderingHints</code> object containing
     *        rendering hints, or null.
     * @throws IllegalArgumentException if name is null.
     * @throws IllegalArgumentException if paramBlock is null.
     */
    public ROI performImageOp(String name,
                              ParameterBlock paramBlock,
                              int sourceIndex,
                              RenderingHints renderHints) {

        if ( name == null || paramBlock == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        // Clone the ParameterBlock and insert a source
        ParameterBlock pb = (ParameterBlock) paramBlock.clone();
        Vector sources = pb.getSources();
        sources.insertElementAt(this.getAsImage(), sourceIndex);

        // Create a new RenderedImage based on the operation name
        // and ParameterBlock using the default registry.
        RenderedImage im = JAI.create(name, pb, renderHints);
        return new ROI(im, threshold);
    }

    /**
     * Returns a <code>Shape</code> representation of the
     * <code>ROI</code>, if possible. If none is available, null is
     * returned. A proper instance of <code>ROI</code> (one that is not
     * an instance of any subclass of <code>ROI</code>) will always
     * return null.
     *
     * @return The <code>ROI</code> as a <code>Shape</code>.
     */
    public Shape getAsShape() {
        return null;
    }

    /**
     * Returns a <code>PlanarImage</code> representation of the
     * <code>ROI</code>. This method will always succeed. This method
     * returns a (bilevel) image whose <code>SampleModel</code> is an
     * instance of <code>MultiPixelPackedSampleModel</code>.
     *
     * @return The <code>ROI</code> as a <code>PlanarImage</code>.
     */
    public PlanarImage getAsImage() {
        return theImage;
    }

    /**
     * Returns a bitmask for a given rectangular region of the ROI
     * indicating whether the pixel is included in the region of
     * interest.  The results are packed into 32-bit integers, with
     * the MSB considered to lie on the left.  The last entry in each
     * row of the result may have bits that lie outside of the
     * requested rectangle.  These bits are guaranteed to be zeroed.
     *
     * <p> The <code>mask</code> array, if supplied, must be of length
     * equal to or greater than <code>height</code> and each of its
     * subarrays must have length equal to or greater than (width +
     * 31)/32.  If <code>null</code> is passed in, a suitable array
     * will be constructed.  If the mask is non-null but has
     * insufficient size, an exception will be thrown.
     *
     * @param x The X coordinate of the upper left corner of the rectangle.
     * @param y The Y coordinate of the upper left corner of the rectangle.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     * @param mask A two-dimensional array of ints at least
     *        (width + 31)/32 entries wide and (height) entries tall,
     *        or null.
     * @return A reference to the <code>mask</code> parameter, or
     *         to a newly constructed array if <code>mask</code> is
     *         <code>null</code>. If the specified rectangle does
     *	       intersect with the image bounds then a <code>null</code>
     *	       is returned.
     */
    public int[][] getAsBitmask(int x, int y,
                                int width, int height,
                                int[][] mask) {

        Rectangle rect =
            getBounds().intersection(new Rectangle(x, y, width, height));

        // Verify that the requested area actually intersects the ROI image.
        if (rect.isEmpty()) {
            return null;
        }

        // Determine the minimum required width of the bitmask in integers.
        int bitmaskIntWidth = (width + 31)/32;

        // Construct bitmask array if argument is null.
        if (mask == null) {
	    mask = new int[height][bitmaskIntWidth];
	} else if (mask.length < height || mask[0].length < bitmaskIntWidth) {
            throw new RuntimeException(JaiI18N.getString("ROI3"));
        }

	byte[] data = ImageUtil.getPackedBinaryData(
				    theImage.getData(), rect);

	// ImageUtil.getPackedBinaryData does not zero out the extra
	// bits used to pad to the nearest byte - so zero these
	// bits out.
	int leftover = rect.width % 8;

	if (leftover != 0) {
	    int datamask = ((1 << leftover) - 1) << (8 - leftover);
	    int linestride = (width + 7)/8;

	    for (int i = linestride-1; i < data.length; i += linestride) {
		data[i] = (byte)(data[i] & datamask);
	    }
	}

	int lineStride = (rect.width + 7)/8;
	int leftOver   = lineStride % 4;

	int row, col, k;
	int ncols = (lineStride - leftOver) / 4;

	for (row = 0, k = 0; row < rect.height; row++) {
            int[] maskRow = mask[row];

	    for (col = 0; col < ncols; col++) {
		maskRow[col] = ((data[k  ] & 0xff) << 24) |
			       ((data[k+1] & 0xff) << 16) |
			       ((data[k+2] & 0xff) <<  8) |
			       ((data[k+3] & 0xff) <<  0);
		k += 4;
	    }

	    switch(leftOver) {
	    case 0: break;
	    case 1: maskRow[col++] = ((data[k  ] & 0xff) << 24);
		    break;
	    case 2: maskRow[col++] = ((data[k  ] & 0xff) << 24) |
				     ((data[k+1] & 0xff) << 16);
		    break;
	    case 3: maskRow[col++] = ((data[k  ] & 0xff) << 24) |
				     ((data[k+1] & 0xff) << 16) |
				     ((data[k+2] & 0xff) <<  8);
		    break;
	    }

	    k += leftOver;

	    Arrays.fill(maskRow, col, bitmaskIntWidth, 0);
	}

        // Clear any trailing rows.
        for (row = rect.height; row < height; row++) {
            Arrays.fill(mask[row], 0);
        }

	return mask;
    }

    /**
     * Returns a <code>LinkedList</code> of <code>Rectangle</code>s
     * for a given rectangular region of the ROI. The
     * <code>Rectangle</code>s in the list are merged into a minimal
     * set.
     *
     * @param x The X coordinate of the upper left corner of the rectangle.
     * @param y The Y coordinate of the upper left corner of the rectangle.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     * @return A <code>LinkedList</code> of <code>Rectangle</code>s.
     *	       If the specified rectangle does intersect with the image
     *	       bounds then a <code>null</code> is returned.
     */
    public LinkedList getAsRectangleList(int x, int y,
                                         int width, int height) {
        return getAsRectangleList(x, y, width, height, true);
    }

    /**
     * Returns a <code>LinkedList</code> of <code>Rectangle</code>s for
     * a given rectangular region of the ROI.
     *
     * @param x The X coordinate of the upper left corner of the rectangle.
     * @param y The Y coordinate of the upper left corner of the rectangle.
     * @param width The width of the rectangle.
     * @param height The height of the rectangle.
     * @param mergeRectangles <code>true</code> if the <code>Rectangle</code>s
     *        are to be merged into a minimal set.
     * @return A <code>LinkedList</code> of <code>Rectangle</code>s.
     *	       If the specified rectangle does intersect with the image
     *	       bounds then a <code>null</code> is returned.
     */
    protected LinkedList getAsRectangleList(int x, int y,
                                            int width, int height,
                                            boolean mergeRectangles) {

        // Verify that the requested area actually intersects the ROI image.
        Rectangle bounds = getBounds();
        Rectangle rect = new Rectangle(x, y, width, height);
        if (!bounds.intersects(rect)) {
            return null;
        }

        // Clip the requested area to the ROI image if necessary.
        if (!bounds.contains(rect)) {
            rect = bounds.intersection(rect);
            x = rect.x;
            y = rect.y;
            width = rect.width;
            height = rect.height;
        }

	byte[] data = ImageUtil.getPackedBinaryData(
				    theImage.getData(), rect);

	// ImageUtil.getPackedBinaryData does not zero out the extra
	// bits used to pad to the nearest byte - therefore ignore
	// these bits.
	int lineStride = (width + 7)/8;
	int leftover = width % 8;
	int mask = (leftover == 0) ? 0xff :
			((1 << leftover) - 1) << (8 - leftover);

        LinkedList rectList = new LinkedList();

        // Calculate the initial list of rectangles as a list of run
        // lengths which are in fact rectangles of unit height.

	int row, col, k, start, val, cnt;

        for (row = 0, k = 0; row < height; row++) {

	    start = -1;

            for (col = 0, cnt = 0; col < lineStride; col++, k++) {

		val = data[k] & ((col == lineStride-1) ? mask : 0xff);

		if (val == 0) {
		    if (start >= 0) {
			rectList.addLast(
			    new Rectangle(x+start, y+row, col*8 - start, 1));
			start = -1;
		    }

		} else if (val == 0xff) {
		    if (start < 0) {
			start = col*8;
		    }

		} else {
		    for (int bit = 7; bit >= 0; bit--) {
			if ((val & (1 << bit)) == 0x00) {
			    if (start >= 0) {
				rectList.addLast(new Rectangle(
				    x+start, y+row, col*8 + (7 - bit) - start, 1));
				start = -1;
			    }
			} else {
			    if (start < 0) {
				start = col*8 + (7 - bit);
			    }
			}
		    }
		}
            }

	    if (start >= 0) {
		rectList.addLast(
		    new Rectangle(x+start, y+row, col*8 - start, 1));
	    }
        }

        // Return the list of Rectangles possibly merged into a minimal set.
        return mergeRectangles ? mergeRunLengthList(rectList) : rectList;
    }

    /**
      * Serialize the <code>ROI</code>.
      *
      * @param out The <code>ObjectOutputStream</code>.
      */
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        if (theImage != null) {
            out.writeBoolean(true);
            RenderingHints hints = new RenderingHints(null);
            hints.put(JAI.KEY_SERIALIZE_DEEP_COPY, new Boolean(true));
            out.writeObject(SerializerFactory.getState(theImage, hints));
        } else {
            out.writeBoolean(false);
        }
    }

    /**
      * Deserialize the <code>ROI</code>.
      *
      * @param in The <code>ObjectInputStream</code>.
      */
    private void readObject(ObjectInputStream in)
        throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        if ((boolean)in.readBoolean()) {
            SerializableState ss = (SerializableState)in.readObject();
            RenderedImage ri =(RenderedImage)(ss.getObject());
            theImage = PlanarImage.wrapRenderedImage(ri);
        } else {
            theImage = null;
        }
        iter = null;
    }
}

