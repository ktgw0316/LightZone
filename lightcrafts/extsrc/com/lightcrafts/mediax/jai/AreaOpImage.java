/*
 * $RCSfile: AreaOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:03 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.Point;
import java.util.Map;

/**
 * An abstract base class for image operators that require only a
 * fixed rectangular source region around a source pixel in order to
 * compute each destination pixel.  
 *
 * <p> The source and the destination images will occupy the same
 * region of the plane.  A given destination pixel (x, y) may be
 * computed from the neighborhood of source pixels beginning at (x -
 * leftPadding, y - topPadding) and extending to (x + rightPadding, y
 * + bottomPadding) inclusive.
 * 
 * <p> Since this operator needs a region around the source pixel in
 * order to compute the destination pixel, the border destination pixels 
 * cannot be computed without any source extension. The source extension 
 * can be specified by supplying a BorderExtender that will define the
 * pixel values of the source outside the actual source area. 
 *
 * <p> If no extension is specified, the destination samples that
 * cannot be computed will be written in the destination as zero.  If
 * the source image begins at pixel (minX, minY) and has width w and
 * height h, the result of performing an area operation will be an
 * image beginning at minX, minY, and having a width of w and a height
 * of h, with the area being computed and written starting at (minX +
 * leftPadding, minY + topPadding) and having width Math.max(w -
 * leftPadding - rightPadding, 0) and height Math.max(h - topPadding
 * - bottomPadding, 0).
 *
 * <p> A <code>RenderingHints</code> for
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> with the value of
 * <code>Boolean.TRUE</code> will automatically be added to the given
 * <code>configuration</code> and passed up to the superclass constructor
 * so that area operations are performed on the pixel values instead
 * of being performed on the indices into the color map for those
 * operations whose source(s) have an <code>IndexColorModel</code>.
 * This addition will only take place if a value for the 
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
 * @see BorderExtender
 */
public abstract class AreaOpImage extends OpImage {
    /**
     * The number of source pixels needed to the left of the central pixel.
     */
    protected int leftPadding;
    
    /** 
     * The number of source pixels needed to the right of the central pixel.
     */
    protected int rightPadding;
    
    /** The number of source pixels needed above the central pixel. */
    protected int topPadding;
    
    /** The number of source pixels needed below the central pixel. */
    protected int bottomPadding;

    /** The BorderExtender, may be null. */
    protected BorderExtender extender = null;

    private Rectangle theDest;

    /** Verify that a specified bounds overlaps that of the source image. */
    private static ImageLayout layoutHelper(ImageLayout layout,
                                            RenderedImage source) {
        // If at least one of the bounds variables is set then
        // check the overlap with the source image.
        if(layout != null && source != null &&
           (layout.getValidMask() &
            (ImageLayout.MIN_X_MASK | ImageLayout.MIN_Y_MASK |
             ImageLayout.WIDTH_MASK | ImageLayout.HEIGHT_MASK)) != 0) {
            // Get the source bounds.
            Rectangle sourceRect =
                new Rectangle(source.getMinX(), source.getMinY(),
                              source.getWidth(), source.getHeight());

            // Create destination bounds defaulting un-set variables to
            // their respective source values as is done in the superclass
            // OpImage constructor.
            Rectangle dstRect = new Rectangle(layout.getMinX(source),
                                              layout.getMinY(source),
                                              layout.getWidth(source),
                                              layout.getHeight(source));

            // Check for empty intersection.
            if(dstRect.intersection(sourceRect).isEmpty()) {
                throw new IllegalArgumentException(
                    JaiI18N.getString("AreaOpImage0"));
            }
        }

        return layout;
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
     * Constructs an <code>AreaOpImage</code>.  The layout variables
     * are set in the standard way by the <code>OpImage</code> constructor.
     * 
     * <p> Additional control over the image bounds, tile grid layout,
     * <code>SampleModel</code>, and <code>ColorModel</code> may be
     * obtained by specifying an <code>ImageLayout</code> parameter.
     * If the image bounds are specified but do not overlap the source
     * bounds then an <code>IllegalArgumentException</code> will be thrown.
     * This parameter will be passed to the superclass constructor
     * unchanged.
     *
     * <p> A <code>RenderingHints</code> for
     * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> with the value of
     * <code>Boolean.TRUE</code> will automatically be added to the given
     * <code>configuration</code> and passed up to the superclass constructor
     * so that area operations are performed on the pixel values instead
     * of being performed on the indices into the color map for those
     * operations whose source(s) have an <code>IndexColorModel</code>.
     * This addition will only take place if a value for the 
     * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> has not already been
     * provided by the user. Note that the <code>configuration</code> Map
     * is cloned before the new hint is added to it.
     *
     * @param source A <code>RenderedImage</code>.
     * @param layout An <code>ImageLayout</code> containing the source
     *        dimensions before padding, and optionally containing the
     *        tile grid layout, <code>SampleModel</code>, and
     *        <code>ColorModel</code>.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param cobbleSources A <code>boolean</code> indicating whether
     *        <code>computeRect()</code> expects contiguous sources.
     * @param extender A BorderExtender, or null.
     * @param leftPadding The desired left padding.
     * @param rightPadding The desired right padding.
     * @param topPadding The desired top padding.
     * @param bottomPadding The desired bottom padding.
     *
     * @throws IllegalArgumentException if <code>source</code>
     *         is <code>null</code>.
     * @throws IllegalArgumentException if the user-specified bounds do
     *         intersect the source bounds.
     *
     * @since JAI 1.1
     */
    public AreaOpImage(RenderedImage source,
                       ImageLayout layout,
                       Map configuration,
                       boolean cobbleSources,
                       BorderExtender extender,
                       int leftPadding,
                       int rightPadding,
                       int topPadding,
                       int bottomPadding) {
        super(vectorize(source), // vectorize() checks for null source.
              layoutHelper(layout, source),
              configHelper(configuration),
              cobbleSources);

        this.extender = extender;
        this.leftPadding = leftPadding;
        this.rightPadding = rightPadding;
        this.topPadding = topPadding;
        this.bottomPadding = bottomPadding;
	
	if (extender == null) {

	    int d_x0 = getMinX() + leftPadding;
            int d_y0 = getMinY() + topPadding; 

            int d_w = getWidth() - leftPadding - rightPadding;
            d_w = Math.max(d_w, 0);

            int d_h = getHeight() - topPadding - bottomPadding;
            d_h = Math.max(d_h, 0);

	    theDest = new Rectangle(d_x0, d_y0, d_w, d_h);
	} else {
	    theDest = getBounds();
	}
    }

    /**
     * Returns the number of pixels needed to the left of the central pixel.
     *
     * @return The left padding factor.
     */
    public int getLeftPadding() {
        return leftPadding;
    }
    
    /**
     * Returns the number of pixels needed to the right of the central pixel.
     *
     * @return The right padding factor.
     */
    public int getRightPadding() {
        return rightPadding;
    }
    
    /** Returns the number of pixels needed above the central pixel.
     *
     * @return The top padding factor.
     */
    public int getTopPadding() {
        return topPadding;
    }
    
    /** Returns the number of pixels needed below the central pixel.
     *
     * @return The bottom padding factor.
     */
    public int getBottomPadding() {
        return bottomPadding;
    }
    
    /**
     * Retrieve the <code>BorderExtender</code> object associated with
     * this class instance.  The object is returned by reference.
     *
     * @return The associated <code>BorderExtender</code> object
     *         or <code>null</code>.
     *
     * @since JAI 1.1
     */
    public BorderExtender getBorderExtender() {
        return extender;
    }

    /**
     * Returns a conservative estimate of the destination region that
     * can potentially be affected by the pixels of a rectangle of a
     * given source. The resulting <code>Rectangle</code> is <u>not</u>
     * clipped to the destination image bounds.
     *
     * @param sourceRect the <code>Rectangle</code> in source coordinates.
     * @param sourceIndex the index of the source image.
     * @return a <code>Rectangle</code> indicating the potentially affected
     *         destination region, or <code>null</code> if the region is
     *         unknown.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>sourceRect</code> is
     *         <code>null</code>.
     */
    public Rectangle mapSourceRect(Rectangle sourceRect,
                                   int sourceIndex) {

        if ( sourceRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException(
                JaiI18N.getString("Generic1"));
        }
        
        int lpad = getLeftPadding();
        int rpad = getRightPadding();
        int tpad = getTopPadding();
        int bpad = getBottomPadding();
        
        return new Rectangle(sourceRect.x + lpad,
                             sourceRect.y + tpad,
                             sourceRect.width - lpad - rpad,
                             sourceRect.height - tpad - bpad);
    }
    
    /**
     * Returns a conservative estimate of the region of a specified
     * source that is required in order to compute the pixels of a
     * given destination rectangle. The resulting <code>Rectangle</code>
     * is <u>not</u> clipped to the source image bounds.
     *
     * @param destRect the <code>Rectangle</code> in destination coordinates.
     * @param sourceIndex the index of the source image.
     *
     * @return a <code>Rectangle</code> indicating the required source region.
     *
     * @throws IllegalArgumentException if <code>sourceIndex</code> is
     *         negative or greater than the index of the last source.
     * @throws IllegalArgumentException if <code>destRect</code> is
     *         <code>null</code>.
     */
    public Rectangle mapDestRect(Rectangle destRect,
                                 int sourceIndex) {
        if ( destRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex < 0 || sourceIndex >= getNumSources()) {
            throw new IllegalArgumentException(
                JaiI18N.getString("Generic1"));
        }

        int lpad = getLeftPadding();
        int rpad = getRightPadding();
        int tpad = getTopPadding();
        int bpad = getBottomPadding();
    
        return new Rectangle(destRect.x - lpad,
                             destRect.y - tpad,
                             destRect.width + lpad + rpad,
                             destRect.height + tpad + bpad);
    }

    /**
     * Computes a tile.  If source cobbling was requested at
     * construction time, the source tile boundaries are overlayed
     * onto the destination, cobbling is performed for areas that
     * intersect multiple source tiles, and
     * <code>computeRect(Raster[], WritableRaster, Rectangle)</code>
     * is called for each of the resulting regions.  Otherwise,
     * <code>computeRect(PlanarImage[], WritableRaster,
     * Rectangle)</code> is called once to compute the entire active
     * area of the tile.
     *
     * <p> The image bounds may be larger than the bounds of the
     * source image.  In this case, samples for which there are no
     * no corresponding sources are set to zero.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     *
     * @return The tile as a <code>Raster</code>.
     */
    public Raster computeTile(int tileX, int tileY) {
        if (!cobbleSources) {
            return super.computeTile(tileX, tileY);
        }

        /* Create a new WritableRaster to represent this tile. */
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));
        WritableRaster dest = createWritableRaster(sampleModel, org);

        /* Clip output rectangle to image bounds. */
        Rectangle rect = new Rectangle(org.x, org.y,
                                       sampleModel.getWidth(),
                                       sampleModel.getHeight());

	Rectangle destRect = rect.intersection(theDest);
	if ((destRect.width <= 0) || (destRect.height <= 0)) {
	    return dest;
	}

        /* account for padding in srcRectangle */
        PlanarImage s = getSource(0);
	// Fix 4639755: Area operations throw exception for 
	// destination extending beyond source bounds
	// The default dest image area is the same as the source
	// image area.  However, when an ImageLayout hint is set,
	// this might be not true.  So the destRect should be the 
	// intersection of the provided rectangle, the destination
	// bounds and the source bounds.
	destRect = destRect.intersection(s.getBounds());
        Rectangle srcRect = new Rectangle(destRect);
        srcRect.x -= getLeftPadding();
        srcRect.width += getLeftPadding() + getRightPadding();
        srcRect.y -= getTopPadding();
        srcRect.height += getTopPadding() + getBottomPadding();

        /*
         * The tileWidth and tileHeight of the source image
         * may differ from this tileWidth and tileHeight.
         */
        IntegerSequence srcXSplits = new IntegerSequence();
        IntegerSequence srcYSplits = new IntegerSequence();

        // there is only one source for an AreaOpImage
        s.getSplits(srcXSplits, srcYSplits, srcRect);

        // Initialize new sequences of X splits.
        IntegerSequence xSplits =
            new IntegerSequence(destRect.x, destRect.x + destRect.width);

        xSplits.insert(destRect.x);
        xSplits.insert(destRect.x + destRect.width);
        
        srcXSplits.startEnumeration();
        while (srcXSplits.hasMoreElements()) {
            int xsplit = srcXSplits.nextElement();
            int lsplit = xsplit - getLeftPadding();
            int rsplit = xsplit + getRightPadding();
            xSplits.insert(lsplit);
            xSplits.insert(rsplit);
        }

        // Initialize new sequences of Y splits.
        IntegerSequence ySplits =
            new IntegerSequence(destRect.y, destRect.y + destRect.height);

        ySplits.insert(destRect.y);
        ySplits.insert(destRect.y + destRect.height);
        
        srcYSplits.startEnumeration();
        while (srcYSplits.hasMoreElements()) {
            int ysplit = srcYSplits.nextElement();
            int tsplit = ysplit - getBottomPadding();
            int bsplit = ysplit + getTopPadding();
            ySplits.insert(tsplit);
            ySplits.insert(bsplit);
        }

        /*
         * Divide destRect into sub rectangles based on the source splits,
         * and compute each sub rectangle separately.
         */
        int x1, x2, y1, y2;
        Raster[] sources = new Raster[1];

        ySplits.startEnumeration();
        for (y1 = ySplits.nextElement(); ySplits.hasMoreElements(); y1 = y2) {
            y2 = ySplits.nextElement();

            int h = y2 - y1;
            int py1 = y1 - getTopPadding();
            int py2 = y2 + getBottomPadding();
	    int ph = py2 - py1;

            xSplits.startEnumeration();
            for (x1 = xSplits.nextElement();
                 xSplits.hasMoreElements();
                 x1 = x2) {
                x2 = xSplits.nextElement();

                int w = x2 - x1;
                int px1 = x1 - getLeftPadding();
                int px2 = x2 + getRightPadding();
                int pw = px2 - px1;

                // Fetch the padded src rectangle
                Rectangle srcSubRect = new Rectangle(px1, py1, pw, ph);
                sources[0] = (extender != null) ? 
                             s.getExtendedData(srcSubRect, extender) : 
                             s.getData(srcSubRect);

                // Make a destRectangle
                Rectangle dstSubRect = new Rectangle(x1,y1,w,h);
                computeRect(sources, dest, dstSubRect);

                // Recycle the source tile
                if(s.overlapsMultipleTiles(srcSubRect)) {
                    recycleTile(sources[0]);
                }
            }
        }
        return dest;
    }
}
