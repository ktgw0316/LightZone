/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: ErodeOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:24 $
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
import javax.media.jai.KernelJAI;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.util.Map;
// import com.sun.media.jai.test.OpImageTester;

/**
 *
 * An OpImage class to perform erosion on a source image.
 *
 * <p> This class implements an erosion operation.
 *
 * <p> <b>Grey Scale Erosion</b>
 * is a spatial operation that computes
 * each output sample by subtract elements of a kernel to the samples
 * surrounding a particular source sample with some care.
 * A mathematical expression is:
 *
 * <p> For a kernel K with a key position (xKey, yKey), the erosion
 * of image I at (x,y) is given by:
 * <pre>
 *     max{a:  a + K(xKey+i, yKey+j) <= I(x+i,y+j): all (i,j) }
 *
 *      all possible (i,j) means that both I(x+i,y+j) and K(xKey+i, yKey+j)
 *      are in bounds. Otherwise, the value is set to 0.
 *
 * </pre>
 * <p> Intuitively, the kernel is like an unbrella and the key point
 * is the handle. At every point, you try to push the umbrella up as high
 * as possible but still underneath the image surface. The final height
 * of the handle is the value after erosion. Thus if you want the image
 * to erode from the upper right to bottom left, the following would do.
 *
 * <p><center>
 * <table border=1>
 * <tr align=center><td>0</td><td>0</td><td>X</td> </tr>
 * <tr align=center><td>0</td><td>X</td><td>0</td> </tr>
 * <tr align=center><td><b>X</b></td><td>0</td><td>0</td> </tr>
 * </table></center>
 *
 * <p> Note that zero kernel erosion has effects on the image, the
 * location of the key position and size of kernel all matter.
 *
 * <p> Pseudo code for the erosion operation is as follows.
 * Assuming the kernel K is of size M rows x N cols
 * and the key position is (xKey, yKey).
 *
 * <pre>
 *
 * // erosion
 * for every dst pixel location (x,y){
 *    tmp = infinity;
 *    for (i = -xKey; i < M - xKey; i++){
 *       for (j = -yKey; j < N - yKey; j++){
 *          if((x+i, y+j) are in bounds of src){
 *             tmp = min{tmp, src[x + i][y + j] - K[xKey + i][yKey + j]};
 *          }
 *       }
 *    }
 *    dst[x][y] = tmp;
 *    if (dst[x][y] == infinity)
 *        dst[x][y] = 0;
 * }
 * </pre>
 *
 * <p> The kernel cannot be bigger in any dimension than the image data.
 *
 * <p> <b>Binary Image Erosion</b>
 * requires the kernel to be binary as well.
 * Intuitively, binary erosion slides the kernel
 * key position and place it at every point (x,y) in the src image.
 * The dst value at this position is set to 1 if all the kernel
 * are fully supported by the src image, and the src image value is 1
 * whenever the kernel has value 1.
 * Otherwise, the value after erosion at (x,y) is set to 0.
 * Erosion usually shrinks images, but it can fill holes
 * with kernels like
 * <pre> [1 <b>0</b> 1] </pre>
 * and the key position at the center.
 *
 * <p> Pseudo code for the erosion operation is as follows.
 *
 * <pre>
 * // erosion
 * for every dst pixel location (x,y){
 *    dst[x][y] = 1;
 *    for (i = -xKey; i < M - xKey; i++){
 *       for (j = -yKey; j < N - yKey; j++){
 *         if((x+i,y+j) is out of bounds of src ||
 *             src(x+i, y+j)==0 && Key(xKey+i, yKey+j)==1){
 *            dst[x][y] = 0; break;
 *          }
 *       }
 *    }
 * }
 * </pre>
 *
 * <p> Reference: An Introduction to Nonlinear Image Processing,
 * by Edward R. Bougherty and Jaakko Astola,
 * Spie Optical Engineering Press, 1994.
 *
 *
 * @see KernelJAI
 */
final class LCErodeOpImage extends AreaOpImage {

    /**
     * The kernel with which to do the erode operation.
     */
    protected KernelJAI kernel;

    /** Kernel variables. */
    private int kw, kh, kx, ky;
    private float[] kdata;
    private int[] integerKdata;

    /**
     * Creates a ErodeOpImage given a ParameterBlock containing the image
     * source and pre-rotated erosion kernel.  The image dimensions are
     * derived
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout
     * object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param kernel the pre-rotated erosion KernelJAI.
     */
    public LCErodeOpImage(RenderedImage source,
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

        kdata = kernel.getKernelData();
        integerKdata = new int[kdata.length];
        for (int i = 0; i < kdata.length; i++)
            integerKdata[i] = kdata[i] == 0 ? 0 : 1;
    }

    /**
     * Performs erosion on a specified rectangle. The sources are
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
                               formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor =
            new RasterAccessor(dest, destRect,
                               formatTags[1], getColorModel());

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
    private void byteLoop(RasterAccessor src, RasterAccessor dst) {

        int dwidth    = dst.getWidth();
        int dheight   = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride    = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride    = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        byte[][] dstDataArrays = dst.getByteDataArrays();
        byte[][] srcDataArrays = src.getByteDataArrays();

        for (int k = 0; k < dnumBands; k++) {
            byte[] dstData = dstDataArrays[k];
            byte[] srcData = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++) {
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;

                    int val = (int) srcData[imageVerticalOffset + srcScanlineStride * ky + srcPixelStride * kx] & 0xff;

                    label: if (val > 0) {
                        for (int u = 0; u < kh; u++) {
                            int imageOffset = imageVerticalOffset;
                            for (int v = 0; v < kw; v++) {
                                if (((int) srcData[imageOffset] & 0xff) == 0 && integerKdata[kernelVerticalOffset + v] != 0) {
                                    val = 0;
                                    break label;
                                }
                                imageOffset += srcPixelStride;
                            }
                            kernelVerticalOffset += kw;
                            imageVerticalOffset += srcScanlineStride;
                        }
                    }

                    dstData[dstPixelOffset] = (byte) val;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }


    private void shortLoop(RasterAccessor src, RasterAccessor dst) {

        int dwidth    = dst.getWidth();
        int dheight   = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride    = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride    = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        short[][] dstDataArrays = dst.getShortDataArrays();
        short[][] srcDataArrays = src.getShortDataArrays();

        for (int k = 0; k < dnumBands; k++)  {
            short[] dstData = dstDataArrays[k];
            short[] srcData = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++)  {
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
		    float f = Float.POSITIVE_INFINITY;
		      for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
			    float tmpIK = srcData[imageOffset] -
			      kdata[kernelVerticalOffset + v];
			    if(tmpIK < f){
			      f = tmpIK;
			    }
			    imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
		      }
		      if (Float.isInfinite(f)){
			 f = 0.0F;
		      }
                    int val  = (int)f;
                    if (val < Short.MIN_VALUE)  {
                        val = Short.MIN_VALUE;
                    } else if (val > Short.MAX_VALUE)  {
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


    private void ushortLoop(RasterAccessor src, RasterAccessor dst) {

        int dwidth    = dst.getWidth();
        int dheight   = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride    = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride    = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        short[][] dstDataArrays = dst.getShortDataArrays();
        short[][] srcDataArrays = src.getShortDataArrays();

        for (int k = 0; k < dnumBands; k++)  {
            short[] dstData = dstDataArrays[k];
            short[] srcData = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++)  {
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
		    float f = Float.POSITIVE_INFINITY;
		      for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
			    float tmpIK = (srcData[imageOffset] &  0xffff) -
			      kdata[kernelVerticalOffset + v];
			    if(tmpIK < f){
			      f = tmpIK;
			    }
			    imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
		      }
		      if (Float.isInfinite(f)){
			 f = 0.0F;
		      }
                    int val  = (int)f;
                    if (val < 0)  {
                        val = 0;
                    } else if (val > 0xffff)  {
                        val = 0xffff;
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

        int dwidth    = dst.getWidth();
        int dheight   = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride    = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride    = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int[][] dstDataArrays = dst.getIntDataArrays();
        int[][] srcDataArrays = src.getIntDataArrays();

        for (int k = 0; k < dnumBands; k++)  {
            int[] dstData = dstDataArrays[k];
            int[] srcData = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++)  {
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
		    float f = Float.POSITIVE_INFINITY;
		      for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
			    float tmpIK = (int)srcData[imageOffset] -
			      kdata[kernelVerticalOffset + v];
			    if(tmpIK < f){
			      f = tmpIK;
			    }
			    imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
		      }
                    if (Float.isInfinite(f)){
		      f = 0.0F;
		    }
                    dstData[dstPixelOffset] = (int)f;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void floatLoop(RasterAccessor src, RasterAccessor dst) {

        int dwidth    = dst.getWidth();
        int dheight   = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride    = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride    = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        float[][] dstDataArrays = dst.getFloatDataArrays();
        float[][] srcDataArrays = src.getFloatDataArrays();

        for (int k = 0; k < dnumBands; k++)  {
            float[] dstData = dstDataArrays[k];
            float[] srcData = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++)  {
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
		    float f = Float.POSITIVE_INFINITY;
		      for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
			    float tmpIK = srcData[imageOffset] -
			      kdata[kernelVerticalOffset + v];
			    if(tmpIK < f){
			      f = tmpIK;
			    }
			    imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
		      }
		      if (Float.isInfinite(f)){
     			 f = 0.0F;
		      }
                    dstData[dstPixelOffset] = f;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }


    private void doubleLoop(RasterAccessor src, RasterAccessor dst) {

        int dwidth    = dst.getWidth();
        int dheight   = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride    = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride    = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        double[][] dstDataArrays = dst.getDoubleDataArrays();
        double[][] srcDataArrays = src.getDoubleDataArrays();

        for (int k = 0; k < dnumBands; k++)  {
            double[] dstData = dstDataArrays[k];
            double[] srcData = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++)  {
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
		    double f = Double.POSITIVE_INFINITY;
		      for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
			    double tmpIK = srcData[imageOffset] -
			      kdata[kernelVerticalOffset + v];
			    if(tmpIK < f){
			      f = tmpIK;
			    }
			    imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
		      }

		      if (Double.isInfinite(f)){
			f = 0.0D;
		      }
                    dstData[dstPixelOffset] = f;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }
}
