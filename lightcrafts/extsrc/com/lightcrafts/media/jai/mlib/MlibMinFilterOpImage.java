/*
 * $RCSfile: MlibMinFilterOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:00 $
 * $State: Exp $
 */ 
package com.lightcrafts.media.jai.mlib;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.AreaOpImage;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import java.util.Map;
import com.lightcrafts.mediax.jai.operator.MinFilterDescriptor;
import com.lightcrafts.mediax.jai.operator.MinFilterShape;
import com.sun.medialib.mlib.*;

/**
 * An OpImage class that subclasses will use to perform
 * MinFiltering with specific masks.
 *
 * @see import com.lightcrafts.mediax.jai.operator.MinFilterDescriptor
 */
final class MlibMinFilterOpImage extends AreaOpImage {

    protected int maskType; //XXX using mediaLib's type for MEDIAN operator!!
    protected int maskSize;

    /**
     * Creates a MlibMinFilterOpImage given an image source, an
     * optional BorderExtender, a maskType and maskSize.  The image
     * dimensions are derived the source image.  The tile grid layout,
     * SampleModel, and ColorModel may optionally be specified by an
     * ImageLayout object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param maskType the filter mask type.
     * @param maskSize the filter mask size.
     */
    public MlibMinFilterOpImage(RenderedImage source,
                                   BorderExtender extender,
                                   Map config,
                                   ImageLayout layout,
                                   MinFilterShape maskType,
                                   int maskSize) {
	super(source,
              layout,
              config,
              true,
              extender,
              (maskSize-1)/2,
              (maskSize-1)/2,
              (maskSize/2),
              (maskSize/2));
	//this.maskType = mapToMlibMaskType(maskType);
        this.maskSize = maskSize;
    }

    private static int mapToMlibMaskType(MinFilterShape maskType) {
        if(maskType.equals(MinFilterDescriptor.MIN_MASK_SQUARE)) {
            return Constants.MLIB_MEDIAN_MASK_RECT;
        } else if(maskType.equals(MinFilterDescriptor.MIN_MASK_PLUS)) {
            return Constants.MLIB_MEDIAN_MASK_PLUS;
        } else if(maskType.equals(MinFilterDescriptor.MIN_MASK_X)) {
            return Constants.MLIB_MEDIAN_MASK_X;
        } else if(maskType.equals(
                  MinFilterDescriptor.MIN_MASK_SQUARE_SEPARABLE)) {
            return Constants.MLIB_MEDIAN_MASK_RECT_SEPARABLE;
        }
        throw new RuntimeException(JaiI18N.getString("MinFilterOpImage0")); 
    }

    /**
     * Performs median filtering on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources an array of source Rasters, guaranteed to provide all
     *                necessary source data for computing the output.
     * @param dest a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
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
        int numBands = getSampleModel().getNumBands();
 

        int cmask = (1 << numBands) -1; 
        mediaLibImage[] srcML = srcAccessor.getMediaLibImages();
        mediaLibImage[] dstML = dstAccessor.getMediaLibImages();
        for (int i = 0; i < dstML.length; i++) {
            switch (dstAccessor.getDataType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                if (maskSize == 3) {
                    // Call appropriate Medialib accelerated function
                    Image.MinFilter3x3(dstML[i], srcML[i]);
                } else if (maskSize == 5) {
                    // Call appropriate Medialib accelerated function
                    Image.MinFilter5x5(dstML[i], srcML[i]);
                } else if (maskSize == 7) {
                    // Call appropriate Medialib accelerated function
                    Image.MinFilter7x7(dstML[i], srcML[i]);
                } 

                break;
            case DataBuffer.TYPE_FLOAT:
            case DataBuffer.TYPE_DOUBLE:
                if (maskSize == 3) {
                    // Call appropriate Medialib accelerated function
                    Image.MinFilter3x3_Fp(dstML[i], srcML[i]);
                } else if (maskSize == 5) {
                    // Call appropriate Medialib accelerated function
                    Image.MinFilter5x5_Fp(dstML[i], srcML[i]);
                }  else if (maskSize == 7) {
                    // Call appropriate Medialib accelerated function
                    Image.MinFilter7x7_Fp(dstML[i], srcML[i]);
                } 
                break;
            default:
                String className = this.getClass().getName();
                throw new RuntimeException(JaiI18N.getString("Generic2"));
            }
        }
 
        if (dstAccessor.isDataCopy()) {
            dstAccessor.copyDataToRaster();
        }
    }
}
