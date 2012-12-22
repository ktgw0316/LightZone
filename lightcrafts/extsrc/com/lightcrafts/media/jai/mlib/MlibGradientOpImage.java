/*
 * $RCSfile: MlibGradientOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:57 $
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
 * An OpImage class to perform Gradient operation on a source image.
 *
 * <p> The Kernels cannot be bigger in any dimension than the image data.
 *
 *
 * @see KernelJAI
 */
final class MlibGradientOpImage extends AreaOpImage {

    /**
     * The orthogonal kernels with which to do the Gradient operation.
     */
    protected KernelJAI kernel_h, kernel_v;

    /** Kernel variables. */
    private int kh, kw, kx, ky;
    float kernel_h_data[], kernel_v_data[];

    /** Local copy of kernel's data */
    double dbl_kh_data[], dbl_kv_data[];
    
    /**
     * Creates a MlibGradientOpImage given the image source and
     * the pair of orthogonal gradient kernels. The image dimensions are
     * derived from the source image.  The tile grid layout,
     * SampleModel, and ColorModel may optionally be specified by an
     * ImageLayout object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.

     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param kernel_h the horizontal kernel
     * @param kernel_v the vertical kernel
     */
    public MlibGradientOpImage(RenderedImage source,
                               BorderExtender extender,
                               Map config,
                               ImageLayout layout,
                               KernelJAI kernel_h,
                               KernelJAI kernel_v) {
	super(source,
              layout,
              config,
              true,
              extender,
              kernel_h.getLeftPadding(),
              kernel_h.getRightPadding(),
              kernel_h.getTopPadding(),
              kernel_h.getBottomPadding());
        
	this.kernel_h = kernel_h;
        this.kernel_v = kernel_v;

        //
        // At this point both kernels should be of same width & height
        // so it's enough to get the information from one of them
        //
	kw = kernel_h.getWidth();
	kh = kernel_h.getHeight();

        //
        // center of kernels
        //
        kx = kw/2;
        ky = kh/2;

        //
        // Get the kernel data into local double arrays
        //
        kernel_h_data = kernel_h.getKernelData();
        kernel_v_data = kernel_v.getKernelData();
        int count = kw * kh;
        dbl_kh_data = new double[count];
        dbl_kv_data = new double[count];
        for (int i = 0; i < count; i++) {
            dbl_kh_data[i] = (double)kernel_h_data[i];
            dbl_kv_data[i] = (double)kernel_v_data[i];
        }
    }

    /**
     * Performs Gradient on a specified rectangle. The sources are cobbled.
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
                Image.GradientMxN(dstML[i],
                                                  srcML[i],
                                                  dbl_kh_data,
                                                  dbl_kv_data,
                                                  kw,
                                                  kh,
                                                  kx,
                                                  ky,
                                                  ((1 << numBands)-1) , 
                                                  Constants.MLIB_EDGE_DST_NO_WRITE);
                break;
                
            case DataBuffer.TYPE_FLOAT:
            case DataBuffer.TYPE_DOUBLE:
                Image.GradientMxN_Fp(dstML[i],
                                                     srcML[i],
                                                     dbl_kh_data,
                                                     dbl_kv_data,
                                                     kw,
                                                     kh,
                                                     kx,
                                                     ky, 
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

//         return new MlibGradientOpImage(oit.getSource(), null, null,
//                                        new ImageLayout(oit.getSource()),
//                                        kern_h, kern_v);
//     }
 
//     public static void main (String args[]) {
//         String classname = "com.lightcrafts.media.jai.mlib.MlibGradientOpImage";
//         OpImageTester.performDiagnostics(classname,args);
//     }
}
