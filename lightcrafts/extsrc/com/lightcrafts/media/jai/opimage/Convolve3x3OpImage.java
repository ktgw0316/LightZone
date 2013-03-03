/*
 * $RCSfile: Convolve3x3OpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:19 $
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
import com.lightcrafts.mediax.jai.KernelJAI;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import java.util.Map;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An OpImage class to perform a 3x3 convolution on a source image.
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
 * <p> Convolution, or any neighborhood operation, leaves a band of
 * pixels around the edges undefined, i.e., for a 3x3 kernel, only
 * four kernel elements and four source pixels contribute to the
 * destination pixel located at (0,0).  Such pixels are not includined
 * in the destination image.  A BorderOpImage may be used to add an
 * appropriate border to the source image in order to avoid shrinkage
 * of the image boundaries.
 *
 * <p> The Kernel cannot be bigger in any dimension than the image data.
 *
 *
 * @see KernelJAI
 */
final class Convolve3x3OpImage extends AreaOpImage {
    /**
     * The 3x3 kernel with which to do the convolve operation.
     */
    protected KernelJAI kernel;
    
    float tables[][] = new float[9][256];

    /**
     * Creates a Convolve3x3OpImage given a ParameterBlock containing the image
     * source and a pre-rotated convolution kernel.  The image dimensions 
     * are derived
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout
     * object.  
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param kernel the pre-rotated convolution KernelJAI.
     * @param cobbleSources a boolean indicating whether computeRect()
     *        expects contiguous sources.
     */
    public Convolve3x3OpImage(RenderedImage source,
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
	if ((kernel.getWidth() != 3) ||
	    (kernel.getHeight() != 3) ||
	    (kernel.getXOrigin() != 1) ||
	    (kernel.getYOrigin() != 1)) {
            throw new RuntimeException(JaiI18N.getString("Convolve3x3OpImage0"));
        }

        if (sampleModel.getDataType() == DataBuffer.TYPE_BYTE) {
            float kdata[] = kernel.getKernelData();
            float k0 = kdata[0],
                  k1 = kdata[1],
                  k2 = kdata[2],
                  k3 = kdata[3],
                  k4 = kdata[4],
                  k5 = kdata[5],
                  k6 = kdata[6],
                  k7 = kdata[7],
                  k8 = kdata[8];
    
            for (int j = 0; j < 256; j++) {
                byte b = (byte)j;
                float f = (float)j;
                tables[0][b+128] = k0*f+0.5f;
                tables[1][b+128] = k1*f;
                tables[2][b+128] = k2*f;
                tables[3][b+128] = k3*f;
                tables[4][b+128] = k4*f;
                tables[5][b+128] = k5*f;
                tables[6][b+128] = k6*f;
                tables[7][b+128] = k7*f;
                tables[8][b+128] = k8*f;
            }
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
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor srcAccessor = 
            new RasterAccessor(source,srcRect, 
                               formatTags[0], 
                               getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor = 
            new RasterAccessor(dest,destRect, 
                               formatTags[1], getColorModel());

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor,dstAccessor);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor,dstAccessor);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor,dstAccessor);
            break;
        default:
            String className = this.getClass().getName();
            throw new RuntimeException(JaiI18N.getString("Convolve3x3OpImage1"));
        }
 
        // If the RasterAccessor object set up a temporary buffer for the 
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    private void byteLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        // cache these out to avoid an array access per kernel value
        float t0[] = tables[0],
              t1[] = tables[1],
              t2[] = tables[2],
              t3[] = tables[3],
              t4[] = tables[4],
              t5[] = tables[5],
              t6[] = tables[6],
              t7[] = tables[7],
              t8[] = tables[8];

        float kdata[] = kernel.getKernelData();

        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        byte srcDataArrays[][] = src.getByteDataArrays(); 
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int bottomScanlineOffset = srcScanlineStride*2;
        int middlePixelOffset = dnumBands;
        int rightPixelOffset = dnumBands*2;

        for (int k = 0; k < dnumBands; k++)  {
            byte dstData[] = dstDataArrays[k];
            byte srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                for (int i = 0; i < dwidth; i++)  {
                    float f = 
                        t0[128+srcData[srcPixelOffset]] +
                        t1[128+srcData[srcPixelOffset + 
                                       middlePixelOffset]] +
                        t2[128+srcData[srcPixelOffset + 
                                       rightPixelOffset]] +
                        t3[128+srcData[srcPixelOffset + 
                                       centerScanlineOffset]]+ 
                        t4[128+srcData[srcPixelOffset + 
                                       centerScanlineOffset + 
                                       middlePixelOffset]]+
                        t5[128+srcData[srcPixelOffset + 
                                       centerScanlineOffset + 
                                       rightPixelOffset]] +
                        t6[128+srcData[srcPixelOffset + 
                                       bottomScanlineOffset]] +
                        t7[128+srcData[srcPixelOffset + 
                                       bottomScanlineOffset + 
                                       middlePixelOffset]] +
                        t8[128+srcData[srcPixelOffset + 
                                       bottomScanlineOffset + 
                                       rightPixelOffset]];

                    // do the clamp
                    int val = (int) f;
                    if (val < 0)  {
                        val = 0;
                    } else if (val > 255)  {
                        val = 255;
                    }
                    dstData[dstPixelOffset] = (byte)(val);
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void shortLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int bottomScanlineOffset = srcScanlineStride*2;
        int middlePixelOffset = dnumBands;
        int rightPixelOffset = dnumBands*2;

        float kdata[] = kernel.getKernelData();
        float k0 = kdata[0],
              k1 = kdata[1],
              k2 = kdata[2],
              k3 = kdata[3],
              k4 = kdata[4],
              k5 = kdata[5],
              k6 = kdata[6],
              k7 = kdata[7],
              k8 = kdata[8];
 
        for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                for (int i = 0; i < dwidth; i++)  {
                    float f =
                        k0*srcData[srcPixelOffset] +
                        k1*srcData[srcPixelOffset +
                                   middlePixelOffset] +
                        k2*srcData[srcPixelOffset +
                                   rightPixelOffset] +
                        k3*srcData[srcPixelOffset +
                                   centerScanlineOffset] +
                        k4*srcData[srcPixelOffset +
                                   centerScanlineOffset +
                                   middlePixelOffset] +
                        k5*srcData[srcPixelOffset +
                                   centerScanlineOffset +
                                   rightPixelOffset] +
                        k6*srcData[srcPixelOffset +
                                   bottomScanlineOffset] +
                        k7*srcData[srcPixelOffset +
                                   bottomScanlineOffset +
                                   middlePixelOffset] +
                        k8*srcData[srcPixelOffset +
                                   bottomScanlineOffset +
                                   rightPixelOffset];

                    int val = (int)f;
                    if (val < Short.MIN_VALUE) {
                       val = Short.MIN_VALUE;
                    } else if (val > Short.MAX_VALUE) {
                       val = Short.MAX_VALUE;
                    }
                    dstData[dstPixelOffset] = (short)val;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void intLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        int srcDataArrays[][] = src.getIntDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        // precalcaculate offsets
        int centerScanlineOffset = srcScanlineStride;
        int bottomScanlineOffset = srcScanlineStride*2;
        int middlePixelOffset = dnumBands;
        int rightPixelOffset = dnumBands*2;

        float kdata[] = kernel.getKernelData();
        float k0 = kdata[0],
              k1 = kdata[1],
              k2 = kdata[2],
              k3 = kdata[3],
              k4 = kdata[4],
              k5 = kdata[5],
              k6 = kdata[6],
              k7 = kdata[7],
              k8 = kdata[8];
 
        for (int k = 0; k < dnumBands; k++)  {
            int dstData[] = dstDataArrays[k];
            int srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                for (int i = 0; i < dwidth; i++)  {
                    float f =
                        k0*srcData[srcPixelOffset] +
                        k1*srcData[srcPixelOffset +
                                   middlePixelOffset] +
                        k2*srcData[srcPixelOffset +
                                   rightPixelOffset] +
                        k3*srcData[srcPixelOffset +
                                   centerScanlineOffset] +
                        k4*srcData[srcPixelOffset +
                                   centerScanlineOffset +
                                   middlePixelOffset] +
                        k5*srcData[srcPixelOffset +
                                   centerScanlineOffset +
                                   rightPixelOffset] +
                        k6*srcData[srcPixelOffset +
                                   bottomScanlineOffset] +
                        k7*srcData[srcPixelOffset +
                                   bottomScanlineOffset +
                                   middlePixelOffset] +
                        k8*srcData[srcPixelOffset +
                                   bottomScanlineOffset +
                                   rightPixelOffset];
 
                    dstData[dstPixelOffset] = (int)f;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

//     public static OpImage createTestImage(OpImageTester oit) {
//         float data[] = {0.05f,0.10f,0.05f,
//                         0.10f,0.40f,0.10f,
//                         0.05f,0.10f,0.05f};
//         KernelJAI kJAI = new KernelJAI(3,3,1,1,data);
//         return new Convolve3x3OpImage(oit.getSource(), null, null,
//                                       new ImageLayout(oit.getSource()),
//                                       kJAI);
//     }

//     // Calls a method on OpImage that uses introspection, to make this
//     // class, discover it's createTestImage() call, call it and then
//     // benchmark the performance of the created OpImage chain.
//     public static void main(String args[]) {
//         String classname = "com.lightcrafts.media.jai.opimage.Convolve3x3OpImage";
//         OpImageTester.performDiagnostics(classname,args);
//     }
}
