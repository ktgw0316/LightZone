/*
 * $RCSfile: MlibMeanOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:59 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.DataBuffer;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.ROI;

import com.lightcrafts.media.jai.opimage.MeanOpImage;
import com.sun.medialib.mlib.*;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An OpImage that performs the Mean operation on an image through mediaLib.
 *
 */
final class MlibMeanOpImage extends MeanOpImage {

    /**
     * Constructs an MlibMeanOpImage. The image dimensions are copied
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout object.
     *
     * @param source    The source image.
     */
    public MlibMeanOpImage(RenderedImage source,
                           ROI roi,
                           int xStart,
                           int yStart,
                           int xPeriod,
                           int yPeriod) {

	super(source, roi, xStart, yStart, xPeriod, yPeriod);
    }

    protected void accumulateStatistics(String name,
                                        Raster source,
                                        Object stats) {
        // Get image and band count.
        PlanarImage sourceImage = getSourceImage(0);
        int numBands = sourceImage.getSampleModel().getNumBands();

        // Determine the format tag and create an accessor.
        int formatTag = MediaLibAccessor.findCompatibleTag(null, source);
        MediaLibAccessor srcAccessor = new MediaLibAccessor(source,
                                                            source.getBounds(),
                                                            formatTag);

        // Get the mediaLib image.
        mediaLibImage[] srcML = srcAccessor.getMediaLibImages();

        // NOTE:  currently srcML.length always equals 1

        double[] dmean = new double[numBands];

        switch (srcAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            for (int i = 0 ; i < srcML.length; i++) {
                Image.Mean(dmean, srcML[i]);
            }
            break;

        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            for (int i = 0 ; i < srcML.length; i++) {
                Image.Mean_Fp(dmean, srcML[i]);
            }
            break;

        default:
            throw new RuntimeException(JaiI18N.getString("Generic2"));
        }

        dmean = srcAccessor.getDoubleParameters(0, dmean);

        // Update the mean.
        double[] mean = (double[])stats;
        double weight =
            (double)(source.getWidth()*source.getHeight())/
            (double)(width*height);
        for ( int i = 0; i < numBands; i++ ) {
            mean[i] += dmean[i]*weight;
        }
    }
}
