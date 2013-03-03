/*
 * $RCSfile: MlibMosaicOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:00 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.mlib;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.ROI;
import com.lightcrafts.mediax.jai.operator.MosaicDescriptor;
import com.lightcrafts.mediax.jai.operator.MosaicType;
import com.lightcrafts.media.jai.opimage.MosaicOpImage;
import com.sun.medialib.mlib.Image;
import com.sun.medialib.mlib.mediaLibImage;

final class MlibMosaicOpImage extends MosaicOpImage {
    /** The Integral lower bound for Thresh1. */
    private int[] glow;

    /** The Integral upper bound for Thresh1. */
    private int[] ghigh;

    /** The shift value for MulShift and DivShift. */
    private int shift;

    public MlibMosaicOpImage(Vector sources,
                             ImageLayout layout,
                             Map config,
                             MosaicType mosaicType,
                             PlanarImage[] sourceAlpha,
                             ROI[] sourceROI,
                             double[][] sourceThreshold,
                             double[] backgroundValues) {
        super(sources, layout, config,
              mosaicType, sourceAlpha, sourceROI,
              sourceThreshold, backgroundValues);

        int numSources = sources.size();

        int dataType = sampleModel.getDataType();
        if(dataType == DataBuffer.TYPE_FLOAT ||
           dataType == DataBuffer.TYPE_DOUBLE) {
            throw new UnsupportedOperationException(JaiI18N.getString("MlibMosaicOpImage0"));
        } else {
            // Decrement integral source thresholds to account for Thresh1
            // which uses ">" instead of ">=" as in the "mosaic" spec.
            for(int i = 0; i < numSources; i++) {
                for(int j = 0; j < numBands; j++) {
                    this.threshold[i][j]--;
                }
            }

            // Set extrema and shift based on data type.
            int minValue = -Integer.MAX_VALUE;
            int maxValue = Integer.MAX_VALUE;
            switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                minValue = 0;
                maxValue = 0xFF;
                shift = 8;
                break;
            case DataBuffer.TYPE_USHORT:
                minValue = 0;
                maxValue = 0xFFFF;
                shift = 16;
                break;
            case DataBuffer.TYPE_SHORT:
                minValue = Short.MIN_VALUE;
                maxValue = Short.MAX_VALUE;
                shift = 16;
                break;
            case DataBuffer.TYPE_INT:
                minValue = Integer.MIN_VALUE;
                maxValue = Integer.MAX_VALUE;
                shift = 32;
                break;
            default:
            }

            // Initialize upper and lower integral bounds for Thresh1.
            this.glow = new int[numBands];
            Arrays.fill(this.glow, minValue);
            this.ghigh = new int[numBands];
            Arrays.fill(this.ghigh, maxValue);
        }
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect,
                               Raster[] alphaRaster,
                               Raster[] roiRaster) {

        // Save the total number of sources.
        int numSources = sources.length;

        // Put all non-null sources in a list.
        ArrayList sourceList = new ArrayList(numSources);
        for(int i = 0; i < numSources; i++) {
            if(sources[i] != null) {
                sourceList.add(sources[i]);
            }
        }

        // Convert the non-null sources to an array.
        int numNonNullSources = sourceList.size();
        Raster[] nonNullSources = null;
        if(numNonNullSources != 0) {
            nonNullSources = new Raster[numNonNullSources];
            sourceList.toArray((Raster[])nonNullSources);
        }

        // Get the format tag.
        int formatTag =
            MediaLibAccessor.findCompatibleTag(nonNullSources, dest);

        // Get dest accessor and image.
        MediaLibAccessor dstAccessor = 
            new MediaLibAccessor(dest, destRect, formatTag);
        mediaLibImage[] dst  = dstAccessor.getMediaLibImages();

        // Re-order the background values as needed.
        int[] mlibBackground = dstAccessor.getIntParameters(0, background);

        if(numNonNullSources == 0) {
            // Fill the destination with the background value.
            Image.Clear(dst[0], mlibBackground);
            return;
        }

        // Get source accessor(s).
        MediaLibAccessor[] srcAccessor = new MediaLibAccessor[numSources];
        for(int i = 0; i < numSources; i++) {
            if(sources[i] != null) {
                srcAccessor[i] =
                    new MediaLibAccessor(sources[i], destRect, formatTag);
            }
        }

        // Get source image(s).
        int[][] mlibThreshold = new int[numSources][];
        mediaLibImage[][] src = new mediaLibImage[numSources][];
        for(int i = 0; i < numSources; i++) {
            if(srcAccessor[i] != null) {
                src[i] = srcAccessor[i].getMediaLibImages();
                mlibThreshold[i] =
                    srcAccessor[i].getIntParameters(0, threshold[i]);
            }
        }

        // Temporary images.
        mediaLibImage tmpIm1 = null;
        mediaLibImage tmpImN = null;
        mediaLibImage[] tmpIm1Array = new mediaLibImage[] {tmpIm1};
        mediaLibImage[] tmpImNArray = new mediaLibImage[] {tmpImN};

        if(mosaicType == MosaicDescriptor.MOSAIC_TYPE_OVERLAY) {
            // Fill the destination with the background value.
            Image.Clear(dst[0], mlibBackground);

            for(int i = numSources - 1; i >= 0; i--) {
                if(src[i] == null) {
                    continue;
                }

                mediaLibImage weight =
                    getWeightImage(destRect, formatTag,
                                   dst[0], src[i][0],
                                   sourceAlpha != null &&
                                   sourceAlpha[i] != null ?
                                   alphaRaster[i] : null,
                                   sourceROI != null &&
                                   sourceROI[i] != null ?
                                   roiRaster[i] : null,
                                   mlibThreshold[i],
                                   tmpIm1Array,
                                   tmpImNArray);

                Image.Blend2(dst[0], src[i][0], weight);
            }
        } else if(mosaicType == MosaicDescriptor.MOSAIC_TYPE_BLEND) {
            tmpIm1 = new mediaLibImage(dst[0].getType(),
                                       1,
                                       dst[0].getWidth(),
                                       dst[0].getHeight());
            tmpImN = new mediaLibImage(dst[0].getType(),
                                       dst[0].getChannels(),
                                       dst[0].getWidth(),
                                       dst[0].getHeight());

            mediaLibImage[] alphas = new mediaLibImage[numNonNullSources];
            mediaLibImage[] srcs = new mediaLibImage[numNonNullSources];

            int sourceCount = 0;

            for(int i = 0; i < numSources; i++) {
                if(src[i] == null) {
                    continue;
                }

                srcs[sourceCount] = src[i][0];
                alphas[sourceCount] =
                    getWeightImage(destRect, formatTag,
                                   dst[0], src[i][0],
                                   sourceAlpha != null &&
                                   sourceAlpha[i] != null ?
                                   alphaRaster[i] : null,
                                   sourceROI != null &&
                                   sourceROI[i] != null ?
                                   roiRaster[i] : null,
                                   mlibThreshold[i],
                                   null,
                                   null);
                sourceCount++;
            }

            if(sourceCount != numNonNullSources) {
                mediaLibImage[] srcsNew = new mediaLibImage[sourceCount];
                System.arraycopy(srcs, 0, srcsNew, 0, sourceCount);
                srcs = srcsNew;
                mediaLibImage[] alphasNew = new mediaLibImage[sourceCount];
                System.arraycopy(alphas, 0, alphasNew, 0, sourceCount);
                alphas = alphasNew;
            }

            Image.BlendMulti(dst[0], srcs, alphas, mlibBackground);
        }

        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    /**
     * Compute the weight image.
     */
    private mediaLibImage getWeightImage(Rectangle destRect,
                                         int formatTag,
                                         mediaLibImage dst,
                                         mediaLibImage src,
                                         Raster alphaRaster,
                                         Raster roiRaster,
                                         int[] thresh,
                                         mediaLibImage[] tmpIm1,
                                         mediaLibImage[] tmpImN) {
        mediaLibImage weight = null;

        if(alphaRaster != null) {
            MediaLibAccessor alphaAccessor =
                new MediaLibAccessor(alphaRaster, destRect,
                                     formatTag);
            mediaLibImage[] alphaML = alphaAccessor.getMediaLibImages();

            if(isAlphaBitmask) {
                if(tmpIm1 == null) {
                    tmpIm1 = new mediaLibImage[] {null};
                }
                if(tmpIm1[0] == null) {
                    tmpIm1[0] = new mediaLibImage(src.getType(),
                                                  1,
                                                  src.getWidth(),
                                                  src.getHeight());
                }

                Image.Thresh1(tmpIm1[0], alphaML[0], new int[] {0},
                              new int[] {ghigh[0]}, new int[] {glow[0]});
                weight = tmpIm1[0];
            } else {
                weight = alphaML[0];
            }
        } else if(roiRaster != null) {
            int roiFmtTag =
                MediaLibAccessor.findCompatibleTag(null, roiRaster);

            MediaLibAccessor roiAccessor =
                new MediaLibAccessor(roiRaster, destRect,
                                     roiFmtTag, true);
            mediaLibImage[] roi = roiAccessor.getMediaLibImages();

            if(tmpIm1 == null) {
                tmpIm1 = new mediaLibImage[] {null};
            }
            if(tmpIm1[0] == null) {
                tmpIm1[0] = new mediaLibImage(src.getType(),
                                              1,
                                              src.getWidth(),
                                              src.getHeight());
            }

            if(tmpIm1[0].getType() != roi[0].getType()) {
                if(tmpIm1[0] == null) {
                    tmpIm1[0] = new mediaLibImage(src.getType(),
                                                  1,
                                                  src.getWidth(),
                                                  src.getHeight());
                }
                Image.DataTypeConvert(tmpIm1[0], roi[0]);
            } else {
                // This is safe because the ROI image is bilevel
                // so the accessor must have copied the data.
                tmpIm1[0] = roi[0];
            }

            Image.Thresh1(tmpIm1[0], new int[] {0},
                          new int[] {ghigh[0]}, new int[] {glow[0]});

            weight = tmpIm1[0];
        } else {
            if(tmpImN == null) {
                tmpImN = new mediaLibImage[] {null};
            }
            if(tmpImN[0] == null) {
                tmpImN[0] = dst.createCompatibleImage();
            }
            weight = tmpImN[0];
            Image.Thresh1(weight, src, thresh, ghigh, glow);
        }

        return weight;
    }
}
