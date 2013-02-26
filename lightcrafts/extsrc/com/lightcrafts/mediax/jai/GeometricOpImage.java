/*
 * $RCSfile: GeometricOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/05/10 00:34:12 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.Point;
import java.util.Map;
import java.util.Vector;

import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An abstract base class for image operators that perform a geometric
 * transformation of the source image.
 *
 * <p> The geometric relationship between the source and destination images
 * will be determined by the specific behavior of the methods
 * <code>backwardMapRect()</code> and <code>forwardMapRect()</code> the
 * implementations of which must be provided by a subclass.
 *
 * <p> The location of the source pixel corresponding to a given destination
 * pixel is determined by the aforementioned backward mapping transformation.
 * The value of the destination pixel is then calculated by interpolating the
 * values of a set of source pixels at locations in the vicinity of the
 * backward mapped destination pixel location according to the requirements
 * of a specified interpolation algorithm.  In particular, a given
 * destination pixel value may be interpolated from the neighborhood of source
 * pixels beginning at (sx - leftPadding, sy - topPadding) and extending to
 * (sx + rightPadding, sy + bottomPadding), inclusive, where (sx,&nbsp;sy) is
 * the truncated backward mapped location of the destination pixel.  The
 * actual amount of padding required is determined by a supplied
 * <code>Interpolation</code> object.
 *
 * <p> Since this operator might need a region around each source pixel in
 * order to compute the destination pixel value, the border destination pixels
 * might not be able to be computed without any source extension mechanism.
 * The source extension method can be specified by supplying a
 * <code>BorderExtender</code> object that will define the pixel values of the
 * source outside the actual source area as a function of the actual source
 * pixel values.  If no extension is specified, the destination samples that
 * cannot be computed will be written in the destination as the user-specified
 * background values.
 *
 * @see BorderExtender
 * @see Interpolation
 * @see InterpolationNearest
 * @see OpImage
 *
 * @since JAI 1.1
 */
public abstract class GeometricOpImage extends OpImage {
    /**
     * The <code>Interpolation</code> object describing the subpixel
     * interpolation method.  This variable should not be null.
     */
    protected Interpolation interp;

    /**
     * The <code>BorderExtender</code> describing the method by which
     * source data are extended to provide sufficient context for
     * calculation of the pixel values of backward mapped coordinates
     * according to the interpolation method specified.  If this
     * variable is <code>null</code> no extension will be performed.
     */
    protected BorderExtender extender = null;

    /**
     * The computable bounds of this image within which the pixels of the
     * image may be computed and set.  This is equal to the bounding box of
     * the set of pixels the locations of which backward map to within the
     * source image bounds contracted by the padding values required for
     * interpolation.
     *
     * <p> The <code>GeometricOpImage</code> constructor sets the computable
     * bounds to the image bounds.  Subclasses should set this value to
     * values reasonable for the operation in question.
     */
    protected Rectangle computableBounds;

    /**
     * Indicates whether the background values are provided.
     */
    protected boolean setBackground;

    /**
     * The user-specified background values.
     */
    protected double[] backgroundValues;

    /**
     * The user-specified background values in integer.
     */
    protected int[] intBackgroundValues;

    /**
     * Constructs a <code>GeometricOpImage</code>.  The image layout
     * (image bounds, tile grid layout, <code>SampleModel</code> and
     * <code>ColorModel</code>) of the output are set in the standard
     * way by the <code>OpImage</code> constructor.
     *
     * <p> Additional control over the image bounds, tile grid layout,
     * <code>SampleModel</code>, and <code>ColorModel</code> may be
     * obtained by specifying an <code>ImageLayout</code> parameter.
     * This parameter will be passed to the superclass constructor
     * unchanged.
     *
     * @param layout An <code>ImageLayout</code> containing the source
     *        bounds before padding, and optionally containing the
     *        tile grid layout, <code>SampleModel</code>, and
     *        <code>ColorModel</code>.
     * @param sources  The immediate sources of this image.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param cobbleSources A <code>boolean</code> indicating whether
     *        <code>computeRect()</code> expects contiguous sources.
     * @param extender A <code>BorderExtender</code>, or <code>null</code>.
     * @param interp an <code>Interpolation</code> object to use for
     *        interpolation of the backward mapped pixel values at
     *        fractional positions.  If the supplied parameter is
     *        <code>null</code> the corresponding instance variable
     *        will be initialized to an instance of
     *        <code>InterpolationNearest</code>.
     *
     * @throws IllegalArgumentException if <code>sources</code>
     *         is <code>null</code>.
     * @throws IllegalArgumentException  If <code>sources</code>
     *         is non-<code>null</code> and any object in
     *         <code>sources</code> is <code>null</code>.
     * @throws IllegalArgumentException if combining the intersected
     *         source bounds with the layout parameter results in negative
     *         output width or height.
     */
    public GeometricOpImage(Vector sources,
                            ImageLayout layout,
                            Map configuration,
                            boolean cobbleSources,
                            BorderExtender extender,
                            Interpolation interp) {
        this(sources,
             layout,
             configuration,
             cobbleSources,
             extender,
             interp,
             null);
    }

    private static Map configHelper(Map configuration) {

	Map config;
	
	if (configuration == null) {
	    config = new RenderingHints(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, 
					Boolean.TRUE);
	} else {

	    config = configuration;

	    // If the user specified a value for this hint, we don't
	    // want to change that
	    if (!config.containsKey(JAI.KEY_REPLACE_INDEX_COLOR_MODEL)) {

		RenderingHints hints = new RenderingHints(null);
		// This is effectively a clone of configuration
		hints.putAll(configuration);
		config = hints;
		config.put(JAI.KEY_REPLACE_INDEX_COLOR_MODEL, Boolean.TRUE);
	    }
	}

	return config;
    }

    /**
     * Constructs a <code>GeometricOpImage</code>.  The image layout
     * (image bounds, tile grid layout, <code>SampleModel</code> and
     * <code>ColorModel</code>) of the output are set in the standard
     * way by the <code>OpImage</code> constructor.
     *
     * <p> Additional control over the image bounds, tile grid layout,
     * <code>SampleModel</code>, and <code>ColorModel</code> may be
     * obtained by specifying an <code>ImageLayout</code> parameter.
     * This parameter will be passed to the superclass constructor
     * unchanged.
     *
     * <p> A <code>RenderingHints</code> for 
     * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> with the value of
     * <code>Boolean.TRUE</code> is automatically added to the given
     * <code>configuration</code> and passed up to the superclass constructor
     * so that geometric operations are performed on the pixel values instead
     * of being performed on the indices into the color map for those
     * operations whose source(s) have an <code>IndexColorModel</code>.
     * This addition will take place only if a value for the 
     * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> has not already been
     * provided by the user. Note that the <code>configuration</code> Map
     * is cloned before the new hint is added to it. Regarding the value
     * for the <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code>
     * <code>RenderingHints</code>, the operator itself can be smart
     * based on the parameters, i.e. while the default value for
     * the <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> is
     * <code>Boolean.TRUE</code> for operations that extend this class,
     * in some cases the operator could set the default.
     *
     * @param layout An <code>ImageLayout</code> containing the source
     *        bounds before padding, and optionally containing the
     *        tile grid layout, <code>SampleModel</code>, and
     *        <code>ColorModel</code>.
     * @param sources  The immediate sources of this image.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param cobbleSources A <code>boolean</code> indicating whether
     *        <code>computeRect()</code> expects contiguous sources.
     * @param extender A <code>BorderExtender</code>, or <code>null</code>.
     * @param interp an <code>Interpolation</code> object to use for
     *        interpolation of the backward mapped pixel values at
     *        fractional positions.  If the supplied parameter is
     *        <code>null</code> the corresponding instance variable
     *        will be initialized to an instance of
     *        <code>InterpolationNearest</code>.
     * @param backgroundValues The user-specified background values.  If the
     *        provided array length is smaller than the number of bands, all
     *        the bands will be filled with the first element of the array.
     *	      If the provided array is null, set it to <code>new double[]{0.0}
     *	      </code>.
     *
     * @throws IllegalArgumentException if <code>sources</code>
     *         is <code>null</code>.
     * @throws IllegalArgumentException  If <code>sources</code>
     *         is non-<code>null</code> and any object in
     *         <code>sources</code> is <code>null</code>.
     * @throws IllegalArgumentException if combining the intersected
     *         source bounds with the layout parameter results in negative
     *         output width or height.
     * @since JAI 1.1.2
     */
    public GeometricOpImage(Vector sources,
                            ImageLayout layout,
                            Map configuration,
                            boolean cobbleSources,
                            BorderExtender extender,
                            Interpolation interp,
                            double[] backgroundValues) {
        super(sources,
              layout,
              configHelper(configuration),
              cobbleSources);

        this.extender = extender;
        this.interp = interp != null ? interp : new InterpolationNearest();

        if (backgroundValues == null)
	    backgroundValues = new double[]{0.0};

        this.setBackground = false;
        for (int i = 0; i < backgroundValues.length; i++)
            if(backgroundValues[i] != 0.0)
                this.setBackground = true;

	this.backgroundValues = backgroundValues;
        int numBands = getSampleModel().getNumBands();
        if (backgroundValues.length < numBands) {
            this.backgroundValues = new double[numBands];
            for (int i = 0; i < numBands; i++)
                this.backgroundValues[i] = backgroundValues[0];
        }

        if (sampleModel.getDataType() <= DataBuffer.TYPE_INT) {
            int length = this.backgroundValues.length;
            intBackgroundValues = new int[length];
            for (int i = 0; i < length; i++)
                intBackgroundValues[i] = (int)this.backgroundValues[i];
        }

        // Initialize computable bounds to the current image bounds.
        computableBounds = getBounds();
    }

    /**
     * Retrieve the <code>Interpolation</code> object associated with
     * this class instance.  The object is returned by reference.
     *
     * @return The associated <code>Interpolation</code> object.
     */
    public Interpolation getInterpolation() {
        return interp;
    }

    /**
     * Retrieve the <code>BorderExtender</code> object associated with
     * this class instance.  The object is returned by reference.
     *
     * @return The associated <code>BorderExtender</code> object
     *         or <code>null</code>.
     */
    public BorderExtender getBorderExtender() {
        return extender;
    }

    /**
     * Computes the position in the specified source that best
     * matches the supplied destination image position. If it
     * is not possible to compute the requested position,
     * <code>null</code> will be returned.
     *
     * <p>The implementation in this class returns the value of
     * <code>pt</code> in the following code snippet:
     *
     * <pre>
     * Rectangle destRect = new Rectangle((int)destPt.getX(),
     *                                    (int)destPt.getY(),
     *                                    1, 1);
     * Rectangle sourceRect = backwardMapRect(destRect, sourceIndex);
     * Point2D pt = (Point2D)destPt.clone();
     * pt.setLocation(sourceRect.x + (sourceRect.width - 1.0)/2.0,
     *                sourceRect.y + (sourceRect.height - 1.0)/2.0);
     * </pre>
     *
     * Subclasses requiring different behavior should override this
     * method.</p>
     *
     * @param destPt the position in destination image coordinates
     * to map to source image coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>destPt</code> or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>destPt</code> is
     * <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>sourceIndex</code> is
     * negative or greater than or equal to the number of sources.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapDestPoint(Point2D destPt, int sourceIndex) {
        if (destPt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IndexOutOfBoundsException(JaiI18N.getString("Generic1"));
        }

        Rectangle destRect = new Rectangle((int)destPt.getX(),
                                           (int)destPt.getY(),
                                           1, 1);

        Rectangle sourceRect = backwardMapRect(destRect, sourceIndex);

        if(sourceRect == null) {
            return null;
        }

        Point2D pt = (Point2D)destPt.clone();
        pt.setLocation(sourceRect.x + (sourceRect.width - 1.0)/2.0,
                       sourceRect.y + (sourceRect.height - 1.0)/2.0);

        return pt;
    }

    /**
     * Computes the position in the destination that best
     * matches the supplied source image position. If it
     * is not possible to compute the requested position,
     * <code>null</code> will be returned.
     *
     * <p>The implementation in this class returns the value of
     * <code>pt</code> in the following code snippet:
     *
     * <pre>
     * Rectangle sourceRect = new Rectangle((int)sourcePt.getX(),
     *                                      (int)sourcePt.getY(),
     *                                      1, 1);
     * Rectangle destRect = forwardMapRect(sourceRect, sourceIndex);
     * Point2D pt = (Point2D)sourcePt.clone();
     * pt.setLocation(destRect.x + (destRect.width - 1.0)/2.0,
     *                destRect.y + (destRect.height - 1.0)/2.0);
     * </pre>
     *
     * Subclasses requiring different behavior should override this
     * method.</p>
     *
     * @param sourcePt the position in source image coordinates
     * to map to destination image coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Point2D</code> of the same class as
     * <code>sourcePt</code> or <code>null</code>.
     *
     * @throws IllegalArgumentException if <code>sourcePt</code> is
     * <code>null</code>.
     * @throws IndexOutOfBoundsException if <code>sourceIndex</code> is
     * negative or greater than or equal to the number of sources.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapSourcePoint(Point2D sourcePt, int sourceIndex) {
        if (sourcePt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IndexOutOfBoundsException(JaiI18N.getString("Generic1"));
        }

        Rectangle sourceRect = new Rectangle((int)sourcePt.getX(),
                                             (int)sourcePt.getY(),
                                             1, 1);
        Rectangle destRect = forwardMapRect(sourceRect, sourceIndex);

        if(destRect == null) {
            return null;
        }

        Point2D pt = (Point2D)sourcePt.clone();
        pt.setLocation(destRect.x + (destRect.width - 1.0)/2.0,
                       destRect.y + (destRect.height - 1.0)/2.0);

        return pt;
    }

    /**
     * Returns the minimum bounding box of the region of the destination
     * to which a particular <code>Rectangle</code> of the specified source
     * will be mapped.
     *
     * <p> The integral source rectangle coordinates should be considered
     * pixel indices.  The "energy" of each pixel is defined to be
     * concentrated in the continuous plane of pixels at an offset of
     * (0.5,&nbsp;0.5) from the index of the pixel.  Forward mappings
     * must take this (0.5,&nbsp;0.5) pixel center into account.  Thus
     * given integral source pixel indices as input, the fractional
     * destination location, as calculated by functions Xf(xSrc,&nbsp;ySrc),
     * Yf(xSrc,&nbsp;ySrc), is given by:
     * <pre>
     *
     *     xDst = Xf(xSrc+0.5, ySrc+0.5) - 0.5
     *     yDst = Yf(xSrc+0.5, ySrc+0.5) - 0.5
     *
     * </pre>
     *
     * @param sourceRect the <code>Rectangle</code> in source coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Rectangle</code> indicating the destination
     *         bounding box, or <code>null</code> if the bounding box
     *         is unknown.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>sourceRect</code> is
     *         <code>null</code>.
     */
    protected abstract Rectangle forwardMapRect(Rectangle sourceRect,
                                                int sourceIndex);

    /**
     * Returns the minimum bounding box of the region of the specified
     * source to which a particular <code>Rectangle</code> of the
     * destination will be mapped.
     *
     * <p> The integral destination rectangle coordinates should be considered
     * pixel indices.  The "energy" of each pixel is defined to be
     * concentrated in the continuous plane of pixels at an offset of
     * (0.5,&nbsp;0.5) from the index of the pixel.  Backward mappings
     * must take this (0.5,&nbsp;0.5) pixel center into account.  Thus
     * given integral destination pixel indices as input, the fractional
     * source location, as calculated by functions Xb(xDst,&nbsp;yDst),
     * Yb(xDst,&nbsp;yDst), is given by:
     * <pre>
     *
     *     xSrc = Xb(xDst+0.5, yDst+0.5) - 0.5
     *     ySrc = Yb(xDst+0.5, yDst+0.5) - 0.5
     *
     * </pre>
     *
     * @param destRect the <code>Rectangle</code> in destination coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Rectangle</code> indicating the source bounding box,
     *         or <code>null</code> if the bounding box is unknown.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>destRect</code> is
     *         <code>null</code>.
     */
    protected abstract Rectangle backwardMapRect(Rectangle destRect,
                                                 int sourceIndex);

    /**
     * Returns a conservative estimate of the destination region that
     * can potentially be affected by the pixels of a rectangle of a
     * given source.  The supplied <code>Rectangle</code> will first be
     * contracted according to the <code>Interpolation</code> object
     * characteristics and then forward mapped into destination coordinate
     * space using <code>forwardMapRect()</code>. The resulting
     * <code>Rectangle</code> is <u>not</u> clipped to the destination
     * image bounds.
     *
     * @param sourceRect the <code>Rectangle</code> in source coordinates.
     * @param sourceIndex the index of the source image.
     * @return a <code>Rectangle</code> indicating the potentially affected
     *         destination region.  This will equal the destination bounds
     *         if <code>forwardMapRect()</code> returns null.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>sourceRect</code> is
     *         <code>null</code>.
     */
    public Rectangle mapSourceRect(Rectangle sourceRect,
                                   int sourceIndex) {

        if (sourceRect == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }

        // Cache left and top padding.
        int lpad = interp.getLeftPadding();
        int tpad = interp.getTopPadding();

        // Shrink the source Rectangle according to the Interpolation.
        Rectangle srcRect = (Rectangle)sourceRect.clone();
        srcRect.x += lpad;
        srcRect.y += tpad;
        srcRect.width -= (lpad + interp.getRightPadding());
        srcRect.height -= (tpad + interp.getBottomPadding());

        // Map the source Rectangle into destination space.
        Rectangle destRect = forwardMapRect(srcRect, sourceIndex);

        // Return Rectangle or destination bounds.
        return destRect == null ? getBounds() : destRect;
    }

    /**
     * Returns a conservative estimate of the region of a specified
     * source that is required in order to compute the pixels of a
     * given destination rectangle.  The supplied <code>Rectangle</code>
     * will first be backward mapped into source coordinate space using
     * <code>backwardMapRect()</code> and then the resulting context
     * will be modified according to the <code>Interpolation</code>
     * object characteristics. The resulting <code>Rectangle</code>
     * is <u>not</u> clipped to the source image bounds.
     *
     * @param destRect the <code>Rectangle</code> in destination coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Rectangle</code> indicating the required source region.
     *         This will equal the bounds of the respective source
     *         if <code>backwardMapRect()</code> returns null.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>destRect</code> is
     *         <code>null</code>.
     */
    public Rectangle mapDestRect(Rectangle destRect,
                                 int sourceIndex) {

        if (destRect == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic1"));
        }

        // Map the destination Rectangle into the appropriate source space.
        Rectangle sourceRect = backwardMapRect(destRect, sourceIndex);
        if(sourceRect == null) {
            return getSource(sourceIndex).getBounds();
        }

        // Cache left and top padding.
        int lpad = interp.getLeftPadding();
        int tpad = interp.getTopPadding();

        // Return padded Rectangle.
        return new Rectangle(sourceRect.x - lpad,
                             sourceRect.y - tpad,
                             sourceRect.width + lpad +
                             interp.getRightPadding(),
                             sourceRect.height + tpad +
                             interp.getBottomPadding());
    }

    /**
     * Computes a tile.  A new <code>WritableRaster</code> is created to
     * represent the requested tile.  Its width and height are equal to this
     * image's tile width and tile height respectively.  If the requested
     * tile lies outside of the image's boundary, or if the backward mapped
     * and padded tile region does not intersect all sources, the created
     * raster is returned with all of its pixels set to 0.
     *
     * <p> Whether or not this method performs source cobbling is determined
     * by the <code>cobbleSources</code> variable set at construction time.
     * If <code>cobbleSources</code> is <code>true</code>, cobbling is
     * performed on the source for areas that intersect multiple tiles,
     * and <code>computeRect(Raster[], WritableRaster, Rectangle)</code>
     * is called to perform the actual computation.  Otherwise,
     * <code>computeRect(PlanarImage[], WritableRaster, Rectangle)</code>
     * is called to perform the actual computation.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     *
     * @return The tile as a <code>Raster</code>.
     */
    public Raster computeTile(int tileX, int tileY) {
        // The origin of the tile.
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));

        // Create a new WritableRaster to represent this tile.
        WritableRaster dest = createWritableRaster(sampleModel, org);

        // Find the intersection between this tile and the bounds.
        Rectangle destRect =
            getTileRect(tileX, tileY).intersection(getBounds());

        if (destRect.isEmpty()) {
	    if (setBackground) {
		ImageUtil.fillBackground(dest, destRect, backgroundValues);
	    }

            return dest;	// tile outside of destination bounds
        }

        int numSources = getNumSources();
        if (cobbleSources) {
            Raster[] rasterSources = new Raster[numSources];

            // Cobble areas
            for (int i = 0; i < numSources; i++) {
                PlanarImage source = getSource(i);

                Rectangle srcBounds = source.getBounds();
                Rectangle srcRect = mapDestRect(destRect, i);
                if (srcRect == null) {
                    // Set to source bounds.
                    srcRect = srcBounds;
                } else {
                    if(extender == null && !srcBounds.contains(srcRect)) {
                        // Clip to source bounds.
                        srcRect = srcBounds.intersection(srcRect);
                    }
                    if(!srcRect.intersects(srcBounds)) {
                        // Outside of source bounds.
                        if (setBackground) {
                            ImageUtil.fillBackground(dest, destRect,
                                                     backgroundValues);
                        }
                        return dest;
                    }
                }

                rasterSources[i] = extender != null ?
                    source.getExtendedData(srcRect, extender) :
                    source.getData(srcRect);
            }

            computeRect(rasterSources, dest, destRect);

            for (int i = 0; i < numSources; i++) {
                Raster sourceData = rasterSources[i];
                if(sourceData != null) {
                    PlanarImage source = getSourceImage(i);

                    // Recycle the source tile
                    if(source.overlapsMultipleTiles(sourceData.getBounds())) {
                        recycleTile(sourceData);
                    }
                }
            }
        } else {
            PlanarImage[] imageSources = new PlanarImage[numSources];

            for (int i = 0; i < numSources; i++) {
                imageSources[i] = getSource(i);
            }
            computeRect(imageSources, dest, destRect);
        }

        return dest;
    }
}
