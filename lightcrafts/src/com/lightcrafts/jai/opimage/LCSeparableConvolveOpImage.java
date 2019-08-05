/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: SeparableConvolveOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:43 $
 * $State: Exp $
 */
package com.lightcrafts.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import javax.media.jai.AreaOpImage;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.KernelJAI;
import java.util.Map;
// import com.sun.media.jai.test.OpImageTester;

/**
 * An OpImage class to perform separable convolve on a source image.
 *
 *
 */
final class LCSeparableConvolveOpImage extends AreaOpImage {

    static {
        System.loadLibrary("JAI");
    }

    protected KernelJAI kernel;
    protected int kw, kh, kx, ky;

    private float[] hValues;
    private float[] vValues;

    /**
     * Creates a SeparableConvoveOpImage on the source
     * with the given pre-rotated kernel.  The image dimensions are
     * derived  the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout
     * object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param kernel a pre-rotated convolution kernel
     */
    public LCSeparableConvolveOpImage(RenderedImage source,
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
        kx = kernel.getXOrigin();
        ky = kernel.getYOrigin();

        hValues = kernel.getHorizontalKernelData();

        vValues = kernel.getVerticalKernelData();
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
            new RasterAccessor(source, srcRect, formatTags[0],
                               getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor =
            new RasterAccessor(dest, destRect, formatTags[1],
                               this.getColorModel());

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(srcAccessor, dstAccessor);
            break;
        case DataBuffer.TYPE_INT:
            intLoop(srcAccessor, dstAccessor);
            break;
        case DataBuffer.TYPE_SHORT:
            shortLoop(srcAccessor, dstAccessor);
            break;
        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor, dstAccessor);
            break;
        case DataBuffer.TYPE_FLOAT:
            floatLoop(srcAccessor, dstAccessor);
            break;
        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(srcAccessor, dstAccessor);
            break;

        default:
        }

        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    private final boolean INTERLEAVED3OPT = true;

    protected void byteLoop(RasterAccessor src,
                            RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        byte[][] dstDataArrays = dst.getByteDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        byte[][] srcDataArrays = src.getByteDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        if (INTERLEAVED3OPT && dstPixelStride == 3 && dstPixelStride == srcPixelStride) {
            int band0 = 0;
            for (int k = 1; k < dnumBands; k++)
                if (dstBandOffsets[k] < dstBandOffsets[band0])
                    band0 = k;

            byte[] dstData = dstDataArrays[band0];
            byte[] srcData = srcDataArrays[band0];
            int srcScanlineOffset = srcBandOffsets[band0];
            int dstScanlineOffset = dstBandOffsets[band0];

            Convolutions.cInterleaved3ByteLoop(srcData, dstData,
                                   srcScanlineOffset, dstScanlineOffset,
                                   srcScanlineStride, dstScanlineStride,
                                   dheight, dwidth, kw, kh, hValues, vValues);
        } else
            for (int k = 0; k < dnumBands; k++) {
                byte[] dstData = dstDataArrays[k];
                byte[] srcData = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];

                Convolutions.cByteLoop(srcData, dstData,
                                       srcScanlineOffset, dstScanlineOffset,
                                       srcScanlineStride, dstScanlineStride,
                                       srcPixelStride, dstPixelStride,
                                       dheight, dwidth, kw, kh, hValues, vValues);
            }
    }



    protected void shortLoop(RasterAccessor src,
                             RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        short[][] dstDataArrays = dst.getShortDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short[][] srcDataArrays = src.getShortDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        if (INTERLEAVED3OPT && dstPixelStride == 3 && dstPixelStride == srcPixelStride) {
            int band0 = 0;
            for (int k = 1; k < dnumBands; k++)
                if (dstBandOffsets[k] < dstBandOffsets[band0])
                    band0 = k;

            short[] dstData = dstDataArrays[band0];
            short[] srcData = srcDataArrays[band0];
            int srcScanlineOffset = srcBandOffsets[band0];
            int dstScanlineOffset = dstBandOffsets[band0];

            Convolutions.cInterleaved3ShortLoop(srcData, dstData,
                                   srcScanlineOffset, dstScanlineOffset,
                                   srcScanlineStride, dstScanlineStride,
                                   dheight, dwidth, kw, kh, hValues, vValues);
        } else
            for (int k = 0; k < dnumBands; k++) {
                short[] dstData = dstDataArrays[k];
                short[] srcData = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];

                Convolutions.cShortLoop(srcData, dstData,
                                        srcScanlineOffset, dstScanlineOffset,
                                        srcScanlineStride, dstScanlineStride,
                                        srcPixelStride, dstPixelStride,
                                        dheight, dwidth, kw, kh, hValues, vValues);
            }


    }

    protected void ushortLoop(RasterAccessor src,
                              RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        short[][] dstDataArrays = dst.getShortDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short[][] srcDataArrays = src.getShortDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        if (INTERLEAVED3OPT && dstPixelStride == 3 && dstPixelStride == srcPixelStride) {
            int band0 = 0;
            for (int k = 1; k < dnumBands; k++)
                if (dstBandOffsets[k] < dstBandOffsets[band0])
                    band0 = k;

            short[] dstData = dstDataArrays[band0];
            short[] srcData = srcDataArrays[band0];
            int srcScanlineOffset = srcBandOffsets[band0];
            int dstScanlineOffset = dstBandOffsets[band0];

            Convolutions.cInterleaved3UShortLoop(srcData, dstData,
                                                srcScanlineOffset, dstScanlineOffset,
                                                srcScanlineStride, dstScanlineStride,
                                                dheight, dwidth, kw, kh, hValues, vValues);
        } else
            for (int k = 0; k < dnumBands; k++) {
                short[] dstData = dstDataArrays[k];
                short[] srcData = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];

                Convolutions.cUShortLoop(srcData, dstData,
                                         srcScanlineOffset, dstScanlineOffset,
                                         srcScanlineStride, dstScanlineStride,
                                         srcPixelStride, dstPixelStride,
                                         dheight, dwidth, kw, kh, hValues, vValues);
            }
    }

    protected void intLoop(RasterAccessor src,
                           RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int[][] dstDataArrays = dst.getIntDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int[][] srcDataArrays = src.getIntDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        if (INTERLEAVED3OPT && dstPixelStride == 3 && dstPixelStride == srcPixelStride) {
            int band0 = 0;
            for (int k = 1; k < dnumBands; k++)
                if (dstBandOffsets[k] < dstBandOffsets[band0])
                    band0 = k;

            int[] dstData = dstDataArrays[band0];
            int[] srcData = srcDataArrays[band0];
            int srcScanlineOffset = srcBandOffsets[band0];
            int dstScanlineOffset = dstBandOffsets[band0];

            Convolutions.cInterleaved3IntLoop(srcData, dstData,
                                              srcScanlineOffset, dstScanlineOffset,
                                              srcScanlineStride, dstScanlineStride,
                                              dheight, dwidth, kw, kh, hValues, vValues);
        } else
            for (int k = 0; k < dnumBands; k++) {
                int[] dstData = dstDataArrays[k];
                int[] srcData = srcDataArrays[k];
                int srcScanlineOffset = srcBandOffsets[k];
                int dstScanlineOffset = dstBandOffsets[k];

                Convolutions.cIntLoop(srcData, dstData,
                                      srcScanlineOffset, dstScanlineOffset,
                                      srcScanlineStride, dstScanlineStride,
                                      srcPixelStride, dstPixelStride,
                                      dheight, dwidth, kw, kh, hValues, vValues);
            }

    }

    protected void floatLoop(RasterAccessor src,
                             RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        float[][] dstDataArrays = dst.getFloatDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        float[][] srcDataArrays = src.getFloatDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        for (int k = 0; k < dnumBands; k++)  {
            float[] dstData = dstDataArrays[k];
            float[] srcData = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];

            Convolutions.cFloatLoop(srcData, dstData,
                                   srcScanlineOffset, dstScanlineOffset,
                                   srcScanlineStride, dstScanlineStride,
                                   srcPixelStride, dstPixelStride,
                                   dheight, dwidth, kw, kh, hValues, vValues);
        }
    }

    protected void doubleLoop(RasterAccessor src,
                              RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        double[][] dstDataArrays = dst.getDoubleDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        double[][] srcDataArrays = src.getDoubleDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        for (int k = 0; k < dnumBands; k++)  {
            double[] dstData = dstDataArrays[k];
            double[] srcData = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];

            Convolutions.cDoubleLoop(srcData, dstData,
                                   srcScanlineOffset, dstScanlineOffset,
                                   srcScanlineStride, dstScanlineStride,
                                   srcPixelStride, dstPixelStride,
                                   dheight, dwidth, kw, kh, hValues, vValues);
        }
    }

//     public static OpImage createTestImage(OpImageTester oit) {
//         float data[] = {0.05f,0.10f,0.05f,
//                         0.10f,0.20f,0.10f,
//                         0.05f,0.10f,0.05f};
//         KernelJAI kJAI = new KernelJAI(3,3,1,1,data);
//         return new SeparableConvolveOpImage(oit.getSource(), null, null,
//                                    new ImageLayout(oit.getSource()),
//                                    kJAI);
//     }

//     public static void main(String args[]) {
//         String classname = "com.sun.media.jai.opimage.SeparableConvolveOpImage";
//         OpImageTester.performDiagnostics(classname,args);
//     }
}
