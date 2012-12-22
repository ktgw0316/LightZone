/*
 * $RCSfile: WarpOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:24 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * A general implementation of image warping, and a superclass for
 * specific image warping operations.
 *
 * <p> The image warp is specified by a <code>Warp</code> object
 * and an <code>Interpolation</code> object.
 *
 * <p> Subclasses of <code>WarpOpImage</code> may choose whether they
 * wish to implement the cobbled or non-cobbled variant of
 * <code>computeRect</code> by means of the <code>cobbleSources</code>
 * constructor parameter.  The class comments for <code>OpImage</code>
 * provide more information about how to override
 * <code>computeRect</code>.
 *
 * It should be noted that the superclass <code>GeometricOpImage</code>
 * automatically adds a value of <code>Boolean.TRUE</code> for the
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> to the given
 * <code>configuration</code> and passes it up to its superclass constructor
 * so that geometric operations are performed on the pixel values instead
 * of being performed on the indices into the color map for those
 * operations whose source(s) have an <code>IndexColorModel</code>.
 * This addition will take place only if a value for the 
 * <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> has not already been
 * provided by the user. Note that the <code>configuration</code> Map
 * is cloned before the new hint is added to it.  Regarding the value
 * for the <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code>
 * <code>RenderingHints</code>, the operator itself can be smart
 * based on the parameters, i.e. while the default value for
 * the <code>JAI.KEY_REPLACE_INDEX_COLOR_MODEL</code> is
 * <code>Boolean.TRUE</code> for operations that extend this class,
 * in some cases the operator could set the default.
 *
 * @see GeometricOpImage
 * @see OpImage
 * @see Warp
 * @see Interpolation
 */
public abstract class WarpOpImage extends GeometricOpImage {

    /**
     * The <code>Warp</code> object describing the backwards pixel
     * map.  It can not be <code>null</code>.
     */
    protected Warp warp;

    /**
     * If no bounds are specified, attempt to derive the image bounds by
     * forward mapping the source bounds.
     */
    private static ImageLayout getLayout(ImageLayout layout,
                                         RenderedImage source,
                                         Warp warp) {
        // If a non-null layout with defined bounds is supplied,
        // return it directly.
        if(layout != null &&
           layout.isValid(ImageLayout.MIN_X_MASK |
                          ImageLayout.MIN_Y_MASK |
                          ImageLayout.WIDTH_MASK |
                          ImageLayout.HEIGHT_MASK)) {
            return layout;
        }

        // Get the source bounds.
        Rectangle sourceBounds = 
            new Rectangle(source.getMinX(), source.getMinY(),
                          source.getWidth(), source.getHeight());

        // Attempt to forward map the source bounds.
        Rectangle destBounds = warp.mapSourceRect(sourceBounds);

        // If this failed, attempt to map the vertices.
        if(destBounds == null) {
            Point[] srcPts = new Point[] {
                new Point(sourceBounds.x,
                          sourceBounds.y),
                new Point(sourceBounds.x + sourceBounds.width,
                          sourceBounds.y),
                new Point(sourceBounds.x,
                          sourceBounds.y + sourceBounds.height),
                new Point(sourceBounds.x + sourceBounds.width,
                          sourceBounds.y + sourceBounds.height)};

            boolean verticesMapped = true;

            double xMin = Double.MAX_VALUE;
            double xMax = -Double.MAX_VALUE;
            double yMin = Double.MAX_VALUE;
            double yMax = -Double.MAX_VALUE;

            for(int i = 0; i < 4; i++) {
                Point2D destPt = warp.mapSourcePoint(srcPts[i]);
                if(destPt == null) {
                    verticesMapped = false;
                    break;
                }

                double x = destPt.getX();
                double y = destPt.getY();
                if(x < xMin) {
                    xMin = x;
                }
                if(x > xMax) {
                    xMax = x;
                }
                if(y < yMin) {
                    yMin = y;
                }
                if(y > yMax) {
                    yMax = y;
                }
            }

            // If all vertices mapped, compute the bounds.
            if(verticesMapped) {
                destBounds = new Rectangle();
                destBounds.x = (int)Math.floor(xMin);
                destBounds.y = (int)Math.floor(yMin);
                destBounds.width = (int)Math.ceil(xMax - destBounds.x);
                destBounds.height = (int)Math.ceil(yMax - destBounds.y);
            }
        }

        // If bounds still not computed, approximate the destination bounds
        // by the source bounds, compute an approximate forward mapping,
        // and use it to compute the destination bounds. If the warp is
        // a WarpAffine then skip it as mapSourceRect() already failed.
        if(destBounds == null && !(warp instanceof WarpAffine)) {
            Point[] destPts = new Point[] {
                new Point(sourceBounds.x,
                          sourceBounds.y),
                new Point(sourceBounds.x + sourceBounds.width,
                          sourceBounds.y),
                new Point(sourceBounds.x,
                          sourceBounds.y + sourceBounds.height),
                new Point(sourceBounds.x + sourceBounds.width,
                          sourceBounds.y + sourceBounds.height)};

            float[] sourceCoords = new float[8];
            float[] destCoords = new float[8];
            int offset = 0;

            for(int i = 0; i < 4; i++) {
                Point2D dstPt = destPts[i];
                Point2D srcPt = warp.mapDestPoint(destPts[i]);
                destCoords[offset] = (float)dstPt.getX();
                destCoords[offset+1] = (float)dstPt.getY();
                sourceCoords[offset] = (float)srcPt.getX();
                sourceCoords[offset+1] = (float)srcPt.getY();
                offset += 2;
            }

            // Guaranteed to be a WarpAffine as the degree is 1.
            WarpAffine wa =
                (WarpAffine)WarpPolynomial.createWarp(sourceCoords, 0,
                                                      destCoords, 0,
                                                      8,
                                                      1.0F, 1.0F,
                                                      1.0F, 1.0F,
                                                      1);

            destBounds = wa.mapSourceRect(sourceBounds);
        }

        // If bounds available, clone or create a new ImageLayout
        // to be modified.
        if(destBounds != null) {
            if(layout == null) {
                layout = new ImageLayout(destBounds.x, destBounds.y,
                                         destBounds.width, destBounds.height);
            } else {
                layout = (ImageLayout)layout.clone();
                layout.setMinX(destBounds.x);
                layout.setMinY(destBounds.y);
                layout.setWidth(destBounds.width);
                layout.setHeight(destBounds.height);
            }
        }

        return layout;
    }

    /**
     * Constructor.
     *
     * <p> The image's layout is encapsulated in the <code>layout</code>
     * argument.  The user-supplied layout values supersedes the default
     * settings.  Any layout setting not specified by the user will take
     * the corresponding value of the source image's layout.
     *
     * @param layout  The layout of this image.
     * @param source  The source image; can not be <code>null</code>.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param cobbleSources  A <code>boolean</code> indicating whether
     *        <code>computeRect()</code> expects contiguous sources.
     *        To use the default implementation of warping contained in
     *        this class, set <code>cobbleSources</code> to <code>false</code>.
     * @param extender  A BorderExtender, or null.
     * @param interp  The <code>Interpolation</code> object describing the
     *        interpolation method.
     * @param warp  The <code>Warp</code> object describing the warp.
     *
     * @throws IllegalArgumentException if <code>source</code>
     *         is <code>null</code>.
     * @throws IllegalArgumentException if combining the
     *         source bounds with the layout parameter results in negative
     *         output width or height.
     * @throws IllegalArgumentException  If <code>warp</code> is
     *         <code>null</code>.
     * @since JAI 1.1
     */
    public WarpOpImage(RenderedImage source,
                       ImageLayout layout,
                       Map configuration,
                       boolean cobbleSources,
                       BorderExtender extender,
                       Interpolation interp,
                       Warp warp) {
        this(source,
             layout,
             configuration,
             cobbleSources,
             extender,
             interp,
             warp,
             null);
    }

    /**
     * Constructor.
     *
     * <p> The image's layout is encapsulated in the <code>layout</code>
     * argument.  The user-supplied layout values supersedes the default
     * settings.  Any layout setting not specified by the user will take
     * the corresponding value of the source image's layout.
     *
     * @param layout  The layout of this image.
     * @param source  The source image; can not be <code>null</code>.
     * @param configuration Configurable attributes of the image including
     *        configuration variables indexed by
     *        <code>RenderingHints.Key</code>s and image properties indexed
     *        by <code>String</code>s or <code>CaselessStringKey</code>s.
     *        This is simply forwarded to the superclass constructor.
     * @param cobbleSources  A <code>boolean</code> indicating whether
     *        <code>computeRect()</code> expects contiguous sources.
     *        To use the default implementation of warping contained in
     *        this class, set <code>cobbleSources</code> to <code>false</code>.
     * @param extender  A BorderExtender, or null.
     * @param interp  The <code>Interpolation</code> object describing the
     *        interpolation method.
     * @param warp  The <code>Warp</code> object describing the warp.
     * @param backgroundValues The user-specified background values.  If the
     *        provided array length is smaller than the number of bands, all
     *        the bands will be filled with the first element of the array.
     *        If the provided array is null, it will be set to
     *        <code>new double[]{0.0}</code> in the superclass.
     *
     * @throws IllegalArgumentException if <code>source</code>
     *         is <code>null</code>.
     * @throws IllegalArgumentException if combining the
     *         source bounds with the layout parameter results in negative
     *         output width or height.
     * @throws IllegalArgumentException  If <code>warp</code> is
     *         <code>null</code>.
     *
     * @since JAI 1.1.2
     */
    public WarpOpImage(RenderedImage source,
                       ImageLayout layout,
                       Map configuration,
                       boolean cobbleSources,
                       BorderExtender extender,
                       Interpolation interp,
                       Warp warp,
		       double[] backgroundValues) {
        super(vectorize(source), // vectorize() checks for null source.
              getLayout(layout, source, warp),
              configuration,
              cobbleSources,
              extender,
              interp,
	      backgroundValues);

        if (warp == null) {
            throw new IllegalArgumentException(
                JaiI18N.getString("Generic0"));
        }
        this.warp = warp;

        if (cobbleSources && extender == null) {
            // Do a basic forward mapping, taking into account the
            // pixel energy is at (0.5, 0.5).
            int l = interp == null ? 0 : interp.getLeftPadding();
            int r = interp == null ? 0 : interp.getRightPadding();
            int t = interp == null ? 0 : interp.getTopPadding();
            int b = interp == null ? 0 : interp.getBottomPadding();

            int x = getMinX() + l;
            int y = getMinY() + t;
            int w = Math.max(getWidth() - l - r, 0);
            int h = Math.max(getHeight() - t - b, 0);

            computableBounds = new Rectangle(x, y, w, h);

        } else {
            // Extender is availabe, write the entire destination.
            computableBounds = getBounds();
        }
    }

    /**
     * Returns the number of samples required to the left of the center.
     *
     * @return The left padding factor.
     *
     * @deprecated as of JAI 1.1.
     */
    public int getLeftPadding() {
        return interp == null ? 0 : interp.getLeftPadding();
    }

    /**
     * Returns the number of samples required to the right of the center.
     *
     * @return The right padding factor.
     *
     * @deprecated as of JAI 1.1.
     */
    public int getRightPadding() {
        return interp == null ? 0 : interp.getRightPadding();
    }

    /**
     * Returns the number of samples required above the center.
     *
     * @return The top padding factor.
     *
     * @deprecated as of JAI 1.1.
     */
    public int getTopPadding() {
        return interp == null ? 0 : interp.getTopPadding();
    }

    /**
     * Returns the number of samples required below the center.
     *
     * @return The bottom padding factor.
     *
     * @deprecated as of JAI 1.1.
     */
    public int getBottomPadding() {
        return interp == null ? 0 : interp.getBottomPadding();
    }

    /**
     * Computes the position in the specified source that best
     * matches the supplied destination image position.
     *
     * <p>The implementation in this class returns the value returned by
     * <code>warp.mapDestPoint(destPt)</code>. Subclasses requiring
     * different behavior should override this method.</p>
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
     * non-zero.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapDestPoint(Point2D destPt, int sourceIndex) {
        if (destPt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if (sourceIndex != 0) {
            throw new IndexOutOfBoundsException(JaiI18N.getString("Generic1"));
        }

        return warp.mapDestPoint(destPt);
    }

    /**
     * Computes the position in the destination that best
     * matches the supplied source image position.

     * <p>The implementation in this class returns the value returned by
     * <code>warp.mapSourcePoint(sourcePt)</code>. Subclasses requiring
     * different behavior should override this method.</p>
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
     * non-zero.
     *
     * @since JAI 1.1.2
     */
    public Point2D mapSourcePoint(Point2D sourcePt, int sourceIndex) {
        if (sourcePt == null) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        } else if (sourceIndex != 0) {
            throw new IndexOutOfBoundsException(JaiI18N.getString("Generic1"));
        }

        return warp.mapSourcePoint(sourcePt);
    }

    /**
     * Returns the minimum bounding box of the region of the destination
     * to which a particular <code>Rectangle</code> of the specified source
     * will be mapped.
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
     *
     * @since JAI 1.1
     */
    protected Rectangle forwardMapRect(Rectangle sourceRect,
                                       int sourceIndex) {

        if ( sourceRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex != 0) {	// this image only has one source
            throw new IllegalArgumentException(
                JaiI18N.getString("Generic1"));
        }

        return warp.mapSourceRect(sourceRect);
    }

    /**
     * Returns the minimum bounding box of the region of the specified
     * source to which a particular <code>Rectangle</code> of the
     * destination will be mapped.
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
     *
     * @since JAI 1.1
     */
    protected Rectangle backwardMapRect(Rectangle destRect,
                                        int sourceIndex) {
        if ( destRect == null ) {
            throw new IllegalArgumentException(JaiI18N.getString("Generic0"));
        }

        if (sourceIndex != 0) {	// this image only has one source
            throw new IllegalArgumentException(
                JaiI18N.getString("Generic1"));
        }

        Rectangle wrect = warp.mapDestRect(destRect);

        return wrect == null ? getSource(0).getBounds() : wrect;
    }

    /**
     * Computes a tile.  A new <code>WritableRaster</code> is created to
     * represent the requested tile.  Its width and height equals to this
     * image's tile width and tile height respectively.  This method
     * assumes that the requested tile either intersects or is within
     * the bounds of this image.
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

        // Find the intersection between this tile and the writable bounds.
        Rectangle destRect = new Rectangle(org.x, org.y,
                  tileWidth, tileHeight).intersection(computableBounds);

        if (destRect.isEmpty()) {
            if (setBackground) {
                ImageUtil.fillBackground(dest, destRect, backgroundValues);
            }
            return dest;	// tile completely outside of computable bounds
        }

        PlanarImage source = getSource(0);

        Rectangle srcRect = mapDestRect(destRect, 0);
        if (!srcRect.intersects(source.getBounds())) {
            if (setBackground) {
                ImageUtil.fillBackground(dest, destRect, backgroundValues);
            }
            return dest;	// outside of source bounds
        }

        // This image only has one source.
        if (cobbleSources) {
            Raster[] srcs = new Raster[1];
            srcs[0] = extender != null ?
                      source.getExtendedData(srcRect, extender) :
                      source.getData(srcRect);

            // Compute the destination tile.
            computeRect(srcs, dest, destRect);

            // Recycle the source tile
            if(source.overlapsMultipleTiles(srcRect)) {
                recycleTile(srcs[0]);
            }
        } else {
            PlanarImage[] srcs = { source };
            computeRect(srcs, dest, destRect);
        }

        return dest;
    }
}
