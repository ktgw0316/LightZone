/*
 * $RCSfile: MaxFilterOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:32 $
 * $State: Exp $
 */ 
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.AreaOpImage;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import java.util.Map;
import com.lightcrafts.mediax.jai.operator.MaxFilterShape;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An abstract OpImage class that subclasses will use to perform
 * MaxFiltering with specific masks.
 *
 */
abstract class MaxFilterOpImage extends AreaOpImage {

    protected MaxFilterShape maskType;
    protected int maskSize;

    /**
     * Creates a MaxFilterOpImage given an image source, an
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
    public MaxFilterOpImage(RenderedImage source,
                            BorderExtender extender,
                            Map config,
                            ImageLayout layout,
                            MaxFilterShape maskType,
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

        this.maskType = maskType;
        this.maskSize = maskSize;
    }

    /**
     * Performs max filtering on a specified rectangle. The sources are
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
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);
 
 
        RasterAccessor srcAccessor =
            new RasterAccessor(source, srcRect,
                               formatTags[0], 
                               getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor =
            new RasterAccessor(dest, destRect,  
                               formatTags[1], getColorModel());
 
        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, dstAccessor, maskSize);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor, dstAccessor, maskSize);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor, dstAccessor, maskSize);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor, dstAccessor, maskSize);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(srcAccessor, dstAccessor, maskSize);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(srcAccessor, dstAccessor, maskSize);
            break;
        }
 
        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    /** Performs max filtering using the subclass's mask on byte data */
    protected abstract void byteLoop(RasterAccessor src, 
                                     RasterAccessor dst,
                                     int filterSize);

    /** Performs max filtering using the subclass's mask on short data */
    protected abstract void shortLoop(RasterAccessor src, 
                                      RasterAccessor dst,
                                      int filterSize);

    /** Performs max filtering using the subclass's mask on ushort data */
    protected abstract void ushortLoop(RasterAccessor src, 
                                       RasterAccessor dst,
                                       int filterSize);
          
    /** Performs max filtering using the subclass's mask on int data */
    protected abstract void intLoop(RasterAccessor src, 
                                    RasterAccessor dst,
                                    int filterSize);

    /** Performs max filtering using the subclass's mask on float data */
    protected abstract void floatLoop(RasterAccessor src, 
                                      RasterAccessor dst,
                                      int filterSize);

    /** Performs max filtering using the subclass's mask on double data */
    protected abstract void doubleLoop(RasterAccessor src, 
                                       RasterAccessor dst,
                                       int filterSize);

    /** Returns the max of the input integer array */
    static final int maxFilter(int data[]) {
        if (data.length == 3) {
           int a = data[0];
           int b = data[1];
           int c = data[2];
           if (a < b) {
	     return (b < c ? c : b);
           } else {
	     return (a < c ? c : a);
           }
        }
	
	int max = data[0];
	for(int i = 1; i < data.length; i++)
	    if(max < data[i])
	        max = data[i];
	return max;
    }


    /** Returns the max of the input float array */
    static final float maxFilterFloat(float data[]) {
        if (data.length == 3) {
           float a = data[0];
           float b = data[1];
           float c = data[2];
           if (a < b) {
	     return (b < c ? c : b);
           } else {
	     return (a < c ? c : a);
           }
        }
	
	float max = data[0];
	for(int i = 1; i < data.length; i++)
	    if(max < data[i])
	        max = data[i];
	return max;
    }



    /** Returns the max of the input double array */
    static final double maxFilterDouble(double data[]) {
        if (data.length == 3) {
           double a = data[0];
           double b = data[1];
           double c = data[2];
           if (a < b) {
	     return (b < c ? c : b);
           } else {
	     return (a < c ? c : a);
           }
        }
	
	double max = data[0];
	for(int i = 1; i < data.length; i++)
	    if(max < data[i])
	        max = data[i];
	return max;
    }


//     // Calls a method on OpImage that uses introspection, to make this
//     // class, discover it's createTestImage() call, call it and then
//     // benchmark the performance of the created OpImage chain.
//     public static void main(String args[]) {
//         String classname = 
//                "com.lightcrafts.media.jai.opimage.MaxFilterSquareOpImage";
//         OpImageTester.performDiagnostics(classname,args);
//         classname = 
//                "com.lightcrafts.media.jai.opimage.MaxFilterXOpImage";
//         OpImageTester.performDiagnostics(classname,args);
//         classname = 
//                "com.lightcrafts.media.jai.opimage.MaxFilterPlusOpImage";
//         OpImageTester.performDiagnostics(classname,args);
//         classname = 
//                "com.lightcrafts.media.jai.opimage.MaxFilterSeparableOpImage";
//         OpImageTester.performDiagnostics(classname,args);
//     }
}
