/*
 * $RCSfile: MlibWarpPolynomialOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/12/15 18:35:48 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.Interpolation;
import java.util.Map;
import com.lightcrafts.mediax.jai.WarpOpImage;
import com.lightcrafts.mediax.jai.WarpPolynomial;

import com.sun.medialib.mlib.*;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An <code>OpImage</code> implementing the polynomial "Warp" operation
 * using MediaLib.
 *
 * <p> With warp operations, there is no forward mapping (from source to
 * destination).  JAI images are tiled, while mediaLib does not handle
 * tiles and consider each tile an individual image.  For each tile in
 * destination, in order not to cobble the entire source image, the
 * <code>computeTile</code> method in this class attemps to do a backward
 * mapping on the tile region using the pixels along the perimeter of the
 * rectangular region.  The hope is that the mapped source rectangle
 * should include all source pixels needed for this particular destination
 * tile.  However, with certain unusual warp points, an inner destination
 * pixel may be mapped outside of the mapped perimeter pixels.  In this
 * case, this destination pixel is not filled, and left black.
 *
 * @see com.lightcrafts.mediax.jai.operator.WarpDescriptor
 * @see MlibWarpRIF
 *
 * @since 1.0
 *
 */
final class MlibWarpPolynomialOpImage extends WarpOpImage {

    /** The x and y coefficients. */
    private double[] xCoeffs;
    private double[] yCoeffs;

    /**
     * Indicates what kind of interpolation to use; may be
     * <code>Constants.MLIB_NEAREST</code>,
     * <code>Constants.MLIB_BILINEAR</code>,
     * or <code>Constants.MLIB_BICUBIC</code>,
     * and was determined in <code>MlibWarpRIF.create()</code>.
     */
    private int filter;

    /** The pre and post scale factors. */
    private double preScaleX;
    private double preScaleY;
    private double postScaleX;
    private double postScaleY;


    /**
     * Constructs a <code>MlibWarpPolynomialOpImage</code>.
     *
     * @param source  The source image.
     * @param layout  The destination image layout.
     * @param warp    An object defining the warp algorithm.
     * @param interp  An object describing the interpolation method.
     */
    public MlibWarpPolynomialOpImage(RenderedImage source,
                                     BorderExtender extender,
                                     Map config,
                                     ImageLayout layout,
                                     WarpPolynomial warp,
                                     Interpolation interp,
                                     int filter,
                                     double[] backgroundValues) {
        super(source,
              layout,
              config,
              true,
              extender,
              interp,
              warp,
              backgroundValues);

        float[] xc = warp.getXCoeffs();
        float[] yc = warp.getYCoeffs();
        int size = xc.length;

        xCoeffs = new double[size];	// X and Y coefficients as doubles
        yCoeffs = new double[size];
        for (int i = 0; i < size; i++) {
            xCoeffs[i] = xc[i];
            yCoeffs[i] = yc[i];
        }

        this.filter = filter;	// interpolation

        preScaleX = warp.getPreScaleX();	// pre/post factors
        preScaleY = warp.getPreScaleY();
        postScaleX = warp.getPostScaleX();
        postScaleY = warp.getPostScaleY();
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
     */
    protected Rectangle backwardMapRect(Rectangle destRect,
                                        int sourceIndex) {
        // Superclass method will throw documented exceptions if needed.
        Rectangle wrect = super.backwardMapRect(destRect, sourceIndex);

        // "Dilate" the backwarp mapped rectangle to account for
        // the lack of being able to know the floating point result of
        // mapDestRect() and to mimic what is done in AffineOpImage.
        // See bug 4518223 for more information.
        wrect.setBounds(wrect.x - 1, wrect.y - 1,
                        wrect.width + 2, wrect.height + 2);

        return wrect;
    }

    /**
     * Computes a tile.  A new <code>WritableRaster</code> is created to
     * represent the requested tile.  Its width and height equals to this
     * image's tile width and tile height respectively.  If the requested
     * tile lies outside of the image's boundary, the created raster is
     * returned with all of its pixels set to 0.
     *
     * <p> This method overrides the method in <code>WarpOpImage</code>
     * and performs source cobbling when necessary.  MediaLib is used to
     * calculate the actual warping.
     *
     * @param tileX The X index of the tile.
     * @param tileY The Y index of the tile.
     *
     * @return The tile as a <code>Raster</code>.
     */
    public Raster computeTile(int tileX, int tileY) {
        /* The origin of the tile. */
        Point org = new Point(tileXToX(tileX), tileYToY(tileY));

        /* Create a new WritableRaster to represent this tile. */
        WritableRaster dest = createWritableRaster(sampleModel, org);

        /* Find the intersection between this tile and the writable bounds. */
        Rectangle rect = new Rectangle(org.x, org.y, tileWidth, tileHeight);
        Rectangle destRect = rect.intersection(computableBounds);
        Rectangle destRect1 = rect.intersection(getBounds());
        if (destRect.isEmpty()) {
	    if (setBackground) {
		ImageUtil.fillBackground(dest, destRect1, backgroundValues);
	    }
            return dest;	// tile completely outside of writable bounds
        }

        /* Map destination rectangle to source space. */
        Rectangle srcRect = backwardMapRect(destRect, 0).intersection(
                            getSourceImage(0).getBounds());

        if (srcRect.isEmpty()) {
	    if (setBackground) {
		ImageUtil.fillBackground(dest, destRect1, backgroundValues);
	    }
            return dest;	// outside of source bounds
        }

        if (!destRect1.equals(destRect)) {
            // beware that destRect1 contains destRect
            ImageUtil.fillBordersWithBackgroundValues(destRect1, destRect, dest, backgroundValues);
        }

        /* Add the interpolation paddings. */
        int l = interp== null ? 0 : interp.getLeftPadding();
        int r = interp== null ? 0 : interp.getRightPadding();
        int t = interp== null ? 0 : interp.getTopPadding();
        int b = interp== null ? 0 : interp.getBottomPadding();

        srcRect = new Rectangle(srcRect.x - l,
                                srcRect.y - t,
                                srcRect.width + l + r,
                                srcRect.height + t + b);

        /* Cobble source into one Raster. */
        Raster[] sources = new Raster[1];
        sources[0] = getBorderExtender() != null ?
                     getSourceImage(0).getExtendedData(srcRect, extender) :
                     getSourceImage(0).getData(srcRect);

        computeRect(sources, dest, destRect);

        // Recycle the source tile
        if(getSourceImage(0).overlapsMultipleTiles(srcRect)) {
            recycleTile(sources[0]);
        }

        return dest;
    }

    /**
     * Performs the "Warp" operation on a rectangular region of
     * the same.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        Raster source = sources[0];

        /* Find the mediaLib data tag. */
        int formatTag = MediaLibAccessor.findCompatibleTag(sources, dest);

        MediaLibAccessor srcMA =
            new MediaLibAccessor(source, source.getBounds(), formatTag);
        MediaLibAccessor dstMA =
            new MediaLibAccessor(dest, destRect, formatTag);

        mediaLibImage[] srcMLI = srcMA.getMediaLibImages();
        mediaLibImage[] dstMLI = dstMA.getMediaLibImages();

        switch (dstMA.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            if (setBackground)
                for (int i = 0 ; i < dstMLI.length; i++) {
                    Image.PolynomialWarp2(dstMLI[i], srcMLI[i],
                                         xCoeffs, yCoeffs,
                                         destRect.x,
                                         destRect.y,
                                         source.getMinX(),
                                         source.getMinY(),
                                         preScaleX, preScaleY,
                                         postScaleX, postScaleY,
                                         filter,
                                         Constants.MLIB_EDGE_DST_NO_WRITE,
                                         intBackgroundValues);
              }
            else
                for (int i = 0 ; i < dstMLI.length; i++) {
                    Image.PolynomialWarp(dstMLI[i], srcMLI[i],
                                         xCoeffs, yCoeffs,
                                         destRect.x,
                                         destRect.y,
                                         source.getMinX(),
                                         source.getMinY(),
                                         preScaleX, preScaleY,
                                         postScaleX, postScaleY,
                                         filter,
                                         Constants.MLIB_EDGE_DST_NO_WRITE);
                    MlibUtils.clampImage(dstMLI[i], getColorModel());
                }
            break;

        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            if (setBackground)
                for (int i = 0 ; i < dstMLI.length; i++) {
                    Image.PolynomialWarp2_Fp(dstMLI[i], srcMLI[i],
                                            xCoeffs, yCoeffs,
                                            destRect.x,
                                            destRect.y,
                                            source.getMinX(),
                                            source.getMinY(),
                                            preScaleX, preScaleY,
                                            postScaleX, postScaleY,
                                            filter,
                                            Constants.MLIB_EDGE_DST_NO_WRITE,
                                            backgroundValues);
                }
            else
                for (int i = 0 ; i < dstMLI.length; i++) {
                    Image.PolynomialWarp_Fp(dstMLI[i], srcMLI[i],
                                            xCoeffs, yCoeffs,
                                            destRect.x,
                                            destRect.y,
                                            source.getMinX(),
                                            source.getMinY(),
                                            preScaleX, preScaleY,
                                            postScaleX, postScaleY,
                                            filter,
                                            Constants.MLIB_EDGE_DST_NO_WRITE);
                }
            break;

        default:
            throw new RuntimeException(JaiI18N.getString("Generic2"));
        }

        if (dstMA.isDataCopy()) {
            dstMA.clampDataArrays();
            dstMA.copyDataToRaster();
        }
    }
}
