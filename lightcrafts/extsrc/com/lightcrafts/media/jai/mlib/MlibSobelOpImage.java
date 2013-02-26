/*
 * $RCSfile: MlibSobelOpImage.java,v $
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
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.AreaOpImage;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.KernelJAI;
import java.util.Map;
import com.sun.medialib.mlib.*;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An OpImage class to perform Sobel (Gradient) on a source image.
 *
 *
 *
 * @see KernelJAI
 */
final class MlibSobelOpImage extends AreaOpImage {
    
    /**
     * Creates a MlibSobelOpImage given the image source.
     * The image dimensions are derived from the source image.
     * The tile grid layout, SampleModel, and ColorModel may
     * optionally be specified by an ImageLayout object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.

     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     */
    public MlibSobelOpImage(RenderedImage source,
                            BorderExtender extender,
                            Map config,
                            ImageLayout layout,
                            KernelJAI kernel) {
        //
        // Both the orthogonal pair of kernels have the same width & height
        // Hence either of them can be used here
        //
	super(source,
              layout,
              config,
              true,
              extender,
              kernel.getLeftPadding(),
              kernel.getRightPadding(),
              kernel.getTopPadding(),
              kernel.getBottomPadding());
    }

    /**
     * Performs Sobel on a specified rectangle. The sources are cobbled.
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

        int formatTag = MediaLibAccessor.findCompatibleTag(sources, dest);

        MediaLibAccessor srcAccessor =
            new MediaLibAccessor(source, srcRect, formatTag);
        MediaLibAccessor dstAccessor =
            new MediaLibAccessor(dest, destRect, formatTag);
        int numBands = getSampleModel().getNumBands();

        mediaLibImage[] srcML = srcAccessor.getMediaLibImages();
        mediaLibImage[] dstML = dstAccessor.getMediaLibImages();
        for (int i = 0; i < dstML.length; i++) {
            switch (dstAccessor.getDataType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                Image.Sobel(dstML[i],
                                            srcML[i],
                                            ((1 << numBands)-1) ,
                                            Constants.MLIB_EDGE_DST_NO_WRITE);
                break;
                
            case DataBuffer.TYPE_FLOAT:
            case DataBuffer.TYPE_DOUBLE:
                Image.Sobel_Fp(dstML[i],
                                               srcML[i],
                                               ((1 << numBands)-1) ,
                                               Constants.MLIB_EDGE_DST_NO_WRITE);
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

//     public static OpImage createTestImage(OpImageTester oit) {
//         float data_h[] = {-1.0f, -2.0f, -1.0f,
//                            0.0f,  0.0f,  0.0f,
//                            1.0f,  2.0f,  1.0f};
//         float data_v[] = {-1.0f, 0.0f, 1.0f,
//                           -2.0f, 0.0f, 2.0f,
//                           -1.0f, 0.0f, 1.0f};

//         KernelJAI kern_h = new KernelJAI(3,3,data_h);
//         KernelJAI kern_v = new KernelJAI(3,3,data_v);
        
//         return new MlibSobelOpImage(oit.getSource(), null, null,
//                                     new ImageLayout(oit.getSource()),
//                                     kern_h);
//     }
 
//     public static void main (String args[]) {
//         String classname = "com.lightcrafts.media.jai.mlib.MlibSobelOpImage";
//         OpImageTester.performDiagnostics(classname,args);
//     }
}
