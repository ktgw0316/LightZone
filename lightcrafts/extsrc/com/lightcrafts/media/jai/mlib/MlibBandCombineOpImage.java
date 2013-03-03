/*
 * $RCSfile: MlibBandCombineOpImage.java,v $
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
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
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
 * An OpImage class that performs a "BandCombine" operation for 3x3 images.
 *
 */
final class MlibBandCombineOpImage extends PointOpImage {
    /* Color conversion matrix. */
    private double[] cmat = new double[9];

    /* Offset vector. */
    private double[] offset = new double[3];

    /* Flag indicating whether the offset is non-zero. */
    private boolean isOffsetNonZero = false;

    /**
     * Constructs an MlibBandCombineOpImage. The image dimensions are copied
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout object.
     *
     * @param source    a RenderedImage.
     * @param layout    an ImageLayout optionally containing the tile
     *                  grid layout, SampleModel, and ColorModel, or null.
     */
    public MlibBandCombineOpImage(RenderedImage source,
                                  Map config,
                                  ImageLayout layout,
                                  double[][] matrix) {
        super(source, layout, config, true);

        int numBands = matrix.length;  // matrix height is dst numBands
        if (getSampleModel().getNumBands() != numBands) {
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

        // Class of SampleModel should have been verified in the RIF
        // by isMediaLibCompatible().
        ComponentSampleModel csm =
            (ComponentSampleModel)source.getSampleModel();
        int[] bankIndices = csm.getBankIndices();
        int[] bandOffsets = csm.getBandOffsets();

        // The matrix must be 3-by-4 for this to work: the check is done
        // in the RIF. Note that the matrix may be modified to simulate the
        // required interchange of bands 1 and 3.
        if(bankIndices[0] == bankIndices[1] &&
           bankIndices[0] == bankIndices[2] &&
           bandOffsets[0] > bandOffsets[2]) {
            for(int j = 0; j < 3; j++) {
                int k = 8 - 3*j;
                for(int i = 0; i < 3; i++) {
                    cmat[k--] = matrix[j][i];
                }
                offset[2-j] = matrix[j][3];
                if(offset[j] != 0.0) {
                    isOffsetNonZero = true;
                }
            }
        } else {
            for(int j = 0; j < 3; j++) {
                int k = 3*j;
                for(int i = 0; i < 3; i++) {
                    cmat[k++] = matrix[j][i];
                }
                offset[j] = matrix[j][3];
                if(offset[j] != 0.0) {
                    isOffsetNonZero = true;
                }
            }
        }
    }

    /**
     * Combine the selected bands.
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

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
        case DataBuffer.TYPE_USHORT:
        case DataBuffer.TYPE_SHORT:
        case DataBuffer.TYPE_INT:
            for (int i = 0; i < dstML.length; i++) {
                if(isOffsetNonZero) {
                    Image.ColorConvert2(dstML[i],
                                                        srcML[i],
                                                        cmat,
                                                        offset);
                } else {
                    Image.ColorConvert1(dstML[i],
                                                        srcML[i],
                                                        cmat);
                }
            }
            break;

        case DataBuffer.TYPE_FLOAT:
        case DataBuffer.TYPE_DOUBLE:
            for (int i = 0; i < dstML.length; i++) {
                if(isOffsetNonZero) {
                    Image.ColorConvert2_Fp(dstML[i],
                                                           srcML[i],
                                                           cmat,
                                                           offset);
                } else {
                    Image.ColorConvert1_Fp(dstML[i],
                                                           srcML[i],
                                                           cmat);
                }
            }
            break;

        default:
            String className = this.getClass().getName();
            throw new RuntimeException(className +
                                       JaiI18N.getString("Generic2"));
        }

        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }
}
