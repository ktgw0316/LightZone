/*
 * $RCSfile: MlibSubsampleBinaryToGrayOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:06 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import java.util.Map;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;

import com.sun.medialib.mlib.*;
import com.lightcrafts.media.jai.opimage.SubsampleBinaryToGrayOpImage;

/**
 * A mediaLib class extending <code>GeometricOpImage</code> to
 * subsample binary images to gray scale images.  Image scaling operations
 * require rectilinear backwards mapping and padding by the resampling
 * filter dimensions.
 *
 * <p> When applying scale factors of scaleX, scaleY to a source image
 * with width of src_width and height of src_height, the resulting image
 * is defined to have the following bounds:
 *
 * <code></pre>
 *       dst minX  = floor(src minX  * scaleX)
 *       dst minY  = floor(src minY  * scaleY)
 *       dst width  =  floor(src width  * scaleX)
 *       dst height =  floor(src height * scaleY)
 * </pre></code>
 *
 * @see ScaleOpImage
 * @see com.lightcrafts.media.jai.opimage.SubsampleBinaryToGrayOpImage
 *
 */
class MlibSubsampleBinaryToGrayOpImage extends SubsampleBinaryToGrayOpImage {

    /**
     * Constructs a <code>MlibSubsampleBinaryToGrayOpImage</code>
     * from a <code>RenderedImage</code> source, x and y scale
     * object.  The image dimensions are determined by forward-mapping
     * the source bounds, and are passed to the superclass constructor
     * by means of the <code>layout</code> parameter.  Other fields of
     * the layout are passed through unchanged.  If
     * <code>layout</code> is <code>null</code>, a new
     * <code>ImageLayout</code> will be constructor to hold the bounds
     * information.
     *
     * The float rounding errors, such as 1.2 being
     * internally represented as 1.200001, are dealt with
     * the floatTol, which is set up so that only 1/10 of pixel
     * error will occur at the end of a line, which yields correct
     * results with Math.round() operation.
     * The repeatability is guaranteed with a one-time computed
     * tables for x-values and y-values.
     *
     * @param source a <code>RenderedImage</code>.
     * @param layout an <code>ImageLayout</code> optionally containing
     *        the tile grid layout, <code>SampleModel</code>, and
     *        <code>ColorModel</code>, or <code>null</code>.

     *        from this <code>OpImage</code>, or <code>null</code>.  If
     *        <code>null</code>, no caching will be performed.
     * @param scaleX scale factor along x axis.
     * @param scaleY scale factor along y axis.
     *
     * @throws IllegalArgumentException if combining the
     *         source bounds with the layout parameter results in negative
     *         output width or height.
     */
    public MlibSubsampleBinaryToGrayOpImage(RenderedImage source,
					ImageLayout layout,
					Map config,
					float scaleX,
					float scaleY){

        super(source, layout, config, scaleX, scaleY);
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
        Rectangle sourceRect = super.backwardMapRect(destRect, sourceIndex);

        // Increment dimensions (fix for 4643583).
        sourceRect.width += (int)invScaleX;
        sourceRect.height += (int)invScaleY;

        // Clamp rectangle to source bounds (fix for 4696977).
        return sourceRect.intersection(getSourceImage(0).getBounds());
    }

    /**
     * Subsample (and condense) the given rectangle by the specified scale.
     * The sources are cobbled.
     *
     * @param sources   an array of sources, guarantee to provide all
     *                  necessary source data for computing the rectangle.
     * @param dest      a tile that contains the rectangle to be computed.
     * @param destRect  the rectangle within this OpImage to be processed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {

	Raster source = sources[0];
	Rectangle srcRect = source.getBounds();

        // Hard-code the source format tag as we know that the image
        // has a binary layout.
        int sourceFormatTag =
            dest.getSampleModel().getDataType() |
            MediaLibAccessor.BINARY |
            MediaLibAccessor.UNCOPIED;

        // Derive format tag for the destination only by providing a
        // null-valued srcs[] parameter.
        int destFormatTag = MediaLibAccessor.findCompatibleTag(null, dest);

        MediaLibAccessor srcAccessor = new MediaLibAccessor(source, srcRect,
							    sourceFormatTag,
                                                            true);
        MediaLibAccessor dstAccessor = new MediaLibAccessor(dest, destRect,
							    destFormatTag);
	mediaLibImage srcML[], dstML[];

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            srcML = srcAccessor.getMediaLibImages();
            dstML = dstAccessor.getMediaLibImages();
            for (int i = 0 ; i < dstML.length; i++) {
                Image.SubsampleBinaryToGray(dstML[i],
							    srcML[i],
							    (double)scaleX,
							    (double)scaleY,
							    lutGray);
            }
            break;
        default:
            String className = this.getClass().getName();
            throw new RuntimeException(JaiI18N.getString("Generic2"));
        }

        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

}
