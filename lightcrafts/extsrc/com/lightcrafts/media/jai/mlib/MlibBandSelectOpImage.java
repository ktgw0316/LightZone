/*
 * $RCSfile: MlibBandSelectOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:50 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.JDKWorkarounds;
import com.sun.medialib.mlib.*;

/**
 * An OpImage class that extracts (a) selected band(s) from an image.
 *
 */
final class MlibBandSelectOpImage extends PointOpImage {
    /* Bitmask for the bands to be extracted. */
    private int cmask = 0x00000000;

    /**
     * Constructs an MlibBandSelectOpImage. The image dimensions are copied
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout object.
     *
     * @param source    a RenderedImage.
     * @param layout    an ImageLayout optionally containing the tile
     *                  grid layout, SampleModel, and ColorModel, or null.
     */
    public MlibBandSelectOpImage(RenderedImage source,
                                 Map config,
                                 ImageLayout layout,
                                 int[] bandIndices) {
        super(source, layout, config, true);

        int numBands = bandIndices.length;
        if (getSampleModel().getNumBands() != numBands) {
            // Create a new SampleModel and ColorModel.
            sampleModel = RasterFactory.createComponentSampleModel(sampleModel,
                                  sampleModel.getDataType(),
                                  tileWidth, tileHeight, numBands);

            if(colorModel != null &&
               !JDKWorkarounds.areCompatibleDataModels(sampleModel,
                                                       colorModel)) {
                colorModel = ImageUtil.getCompatibleColorModel(sampleModel,
                                                               config);
            }
        }

        // Initialize the band selection bitmask.
        int maxShift = source.getSampleModel().getNumBands() - 1;
        for(int i = 0; i < bandIndices.length; i++) {
            cmask |= 0x00000001 << (maxShift - bandIndices[i]);
        }
    }

    /**
     * Extract the selected bands.
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
        Rectangle srcRect = mapDestRect(destRect, 0);

        int formatTag = MediaLibAccessor.findCompatibleTag(sources,dest);

        MediaLibAccessor srcAccessor = 
            new MediaLibAccessor(source,srcRect,formatTag);
        MediaLibAccessor dstAccessor = 
            new MediaLibAccessor(dest,destRect,formatTag);

        mediaLibImage[] srcML = srcAccessor.getMediaLibImages();
        mediaLibImage[] dstML = dstAccessor.getMediaLibImages();

        for (int i = 0; i < dstML.length; i++) {
            Image.ChannelExtract(dstML[i], srcML[i], cmask);
        }

        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }
}
