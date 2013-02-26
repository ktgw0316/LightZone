/*
 * $RCSfile: MlibCopyOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:53 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import java.util.Map;
import com.sun.medialib.mlib.*;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An OpImage class that copies an image from source to dest.
 *
 */
final class MlibCopyOpImage extends PointOpImage {

    /**
     * Constructs an MlibCopyOpImage. The image dimensions are copied
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout object.
     *
     * @param source    a RenderedImage.

     *        or null.  If null, a default cache will be used.
     * @param layout    an ImageLayout optionally containing the tile
     *                  grid layout, SampleModel, and ColorModel, or null.
     */
    public MlibCopyOpImage(RenderedImage source,
                           Map config,
                           ImageLayout layout) {
        super(source, layout, config, true);

    }

    /**
     * Copy the pixel values of a rectangle with a given constant.
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
        int formatTag = MediaLibAccessor.findCompatibleTag(sources,dest);

        MediaLibAccessor srcMA = 
            new MediaLibAccessor(sources[0], destRect, formatTag);
        MediaLibAccessor dstMA = 
            new MediaLibAccessor(dest, destRect, formatTag);

        mediaLibImage[] srcMLI = srcMA.getMediaLibImages();
        mediaLibImage[] dstMLI = dstMA.getMediaLibImages();

        for (int i = 0 ; i < dstMLI.length; i++) {
            Image.Copy(dstMLI[i], srcMLI[i]);
        }

        if (dstMA.isDataCopy()) {
            dstMA.copyDataToRaster();
        }
    }
}
