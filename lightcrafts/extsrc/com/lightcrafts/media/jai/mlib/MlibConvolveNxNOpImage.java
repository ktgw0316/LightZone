/*
 * $RCSfile: MlibConvolveNxNOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.4 $
 * $Date: 2005/08/16 00:17:28 $
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
 * An OpImage class to perform convolution on a source image.
 *
 * <p> This class implements a convolution operation. Convolution is a
 * spatial operation that computes each output sample by multiplying
 * elements of a kernel with the samples surrounding a particular
 * source sample.
 *
 * <p> For each destination sample, the kernel is rotated 180 degrees
 * and its "key element" is placed over the source pixel corresponding
 * with the destination pixel.  The kernel elements are multiplied
 * with the source pixels under them, and the resulting products are
 * summed together to produce the destination sample value.
 * 
 * <p> Example code for the convolution operation on a single sample
 * dst[x][y] is as follows, assuming the kernel is of size M rows x N
 * columns and has already been rotated through 180 degrees.  The
 * kernel's key element is located at position (xKey, yKey):
 *
 * <pre>
 * dst[x][y] = 0;
 * for (int i = -xKey; i < M - xKey; i++) {
 *     for (int j = -yKey; j < N - yKey; j++) {
 *         dst[x][y] += src[x + i][y + j] * kernel[xKey + i][yKey + j];
 *     }
 * }
 * </pre>
 *
 * <p> Convolution, or any neighborhood operation, leaves a band of
 * pixels around the edges undefined, e.g., for a 3x3 kernel, only
 * four kernel elements and four source pixels contribute to the
 * destination pixel located at (0,0).  Such pixels are not includined
 * in the destination image, unless a non-null BorderExtender is provided.
 *
 * <p> The Kernel cannot be bigger in any dimension than the image data.
 *
 * @see KernelJAI
 */
final class MlibConvolveNxNOpImage extends AreaOpImage {

    /**
     * The kernel with which to do the convolve operation.
     */
    protected KernelJAI kernel;

    /** Kernel variables. */
    private int kw, kh;
    float kData[];
    double doublekData[];
    int intkData[];
    int shift = -1;
    

    /**
     * Creates a MlibConvolveNxNOpImage given the image source and
     * pre-rotated convolution kernel.  The image dimensions are
     * derived from the source image.  The tile grid layout,
     * SampleModel, and ColorModel may optionally be specified by an
     * ImageLayout object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.

     *        or null.  If null, a default cache will be used.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param kernel the pre-rotated convolution KernelJAI.
     */
    public MlibConvolveNxNOpImage(RenderedImage source,
				  BorderExtender extender,
				  Map config,
				  ImageLayout layout,
				  KernelJAI kernel) {
	super(source,
              layout,
              config,
              true,
              extender,
              kernel.getLeftPadding(),
              kernel.getRightPadding(),
              kernel.getTopPadding(),
              kernel.getBottomPadding());
        
	this.kernel = kernel;
	kw = kernel.getWidth();
	kh = kernel.getHeight();

        // kx and ky are centered in AreaOpImage, not here.

        kData = kernel.getKernelData();

        int count = kw*kh;

        // A little inefficient but figuring out what datatype
        // mediaLibAccessor will want is tricky.
        intkData = new int[count];
        doublekData = new double[count];
        for (int i = 0; i < count; i++) {
            doublekData[i] = (double)kData[i];
        }
    }

    private synchronized void setShift(int formatTag) {
        if (shift == -1) {
            int mediaLibDataType =
                MediaLibAccessor.getMediaLibDataType(formatTag);
            shift = Image.ConvKernelConvert(intkData,
					    doublekData,
					    kw,kh,
					    mediaLibDataType);
        }
    }

    /**
     * Performs convolution on a specified rectangle. The sources are
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
            new MediaLibAccessor(source,srcRect,formatTag,true);
        MediaLibAccessor dstAccessor =
            new MediaLibAccessor(dest,destRect,formatTag,true);
        int numBands = getSampleModel().getNumBands();


        mediaLibImage[] srcML = srcAccessor.getMediaLibImages();
        mediaLibImage[] dstML = dstAccessor.getMediaLibImages();
        for (int i = 0; i < dstML.length; i++) {
            switch (dstAccessor.getDataType()) {
            case DataBuffer.TYPE_BYTE:
            case DataBuffer.TYPE_USHORT:
            case DataBuffer.TYPE_SHORT:
            case DataBuffer.TYPE_INT:
                if (shift == -1) {
                    setShift(formatTag);
                }

		if (kw == 2) {
                    Image.Conv2x2(dstML[i],
                                  srcML[i], intkData, shift,
                                  ((1 << numBands)-1) ,
                                  Constants.MLIB_EDGE_DST_NO_WRITE);
                } else if (kw == 3) {
                    Image.Conv3x3(dstML[i],
                                  srcML[i], intkData, shift,
                                  ((1 << numBands)-1) ,
                                  Constants.MLIB_EDGE_DST_NO_WRITE);
                } else if (kw == 4) {
                    Image.Conv4x4(dstML[i],
                                  srcML[i], intkData, shift,
                                  ((1 << numBands)-1) ,
                                  Constants.MLIB_EDGE_DST_NO_WRITE);
                } else if (kw == 5) {
                    Image.Conv5x5(dstML[i],
                                  srcML[i], intkData, shift,
                                  ((1 << numBands)-1) ,
                                  Constants.MLIB_EDGE_DST_NO_WRITE);
                } else if (kw == 7) {
                    Image.Conv7x7(dstML[i],
                                  srcML[i], intkData, shift,
                                  ((1 << numBands)-1) ,
                                  Constants.MLIB_EDGE_DST_NO_WRITE);
                }
                break;
            case DataBuffer.TYPE_FLOAT:
            case DataBuffer.TYPE_DOUBLE:
                if (kw == 2) {
                    Image.Conv2x2_Fp(dstML[i],
                                     srcML[i], doublekData,
                                     ((1 << numBands)-1) ,
                                     Constants.MLIB_EDGE_DST_NO_WRITE);
		} else if (kw == 3) {
                    Image.Conv3x3_Fp(dstML[i],
                                     srcML[i], doublekData,
                                     ((1 << numBands)-1) ,
                                     Constants.MLIB_EDGE_DST_NO_WRITE);
		} else if (kw == 4) {
                    Image.Conv4x4_Fp(dstML[i],
                                     srcML[i], doublekData,
                                     ((1 << numBands)-1) ,
                                     Constants.MLIB_EDGE_DST_NO_WRITE);
		} else if (kw == 5) {
                    Image.Conv5x5_Fp(dstML[i],
                                     srcML[i], doublekData,
                                     ((1 << numBands)-1) ,
                                     Constants.MLIB_EDGE_DST_NO_WRITE);
                } else if (kw == 7) {
                    Image.Conv7x7_Fp(dstML[i],
                                     srcML[i], doublekData,
                                     ((1 << numBands)-1) ,
                                     Constants.MLIB_EDGE_DST_NO_WRITE);
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

//     public static OpImage createTestImage(OpImageTester oit) {
//         float data[] = {0.05f,0.10f,0.05f,
//                         0.10f,0.40f,0.10f,
//                         0.05f,0.10f,0.05f};
//         KernelJAI kJAI = new KernelJAI(3,3,1,1,data);
//         return new MlibConvolve3x3OpImage(oit.getSource(), null, null,
//                                           new ImageLayout(oit.getSource()),
//                                           kJAI);
//     }
 
//     public static void main (String args[]) {
//         String classname = "com.lightcrafts.media.jai.mlib.MlibConvolve3x3OpImage";
//         OpImageTester.performDiagnostics(classname,args);
//     }
}
