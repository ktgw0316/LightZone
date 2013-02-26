/*
 * $RCSfile: DilateOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:23 $
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
 *
 * An OpImage class to perform dilation on a source image.
 *
 * Dilation for grey scale images can be charaterized by "slide, add and max",
 * while for binary images by "slide and set". As always, the kernel
 * is expected to come with a key position.
 * 
 * <p> <b> Grey scale dilation</b> is a spatial operation that computes
 * each output sample by adding elements of a kernel to the samples
 * surrounding a particular source sample and taking the maximum.
 * A mathematical expression is:
 *
 * <p> For a kernel K with a key position (xKey,yKey), the dilation
 * of image I at (x,y) is given by:
 * <pre>
 *     max{ I(x-i, y-j) + K(xKey+i, yKey+j): some (i,j) restriction }
 *  
 *      where the (i,j) restriction means:
 *      all possible (i,j) so that both I(x-i,y-j) and K(xKey+i, yKey+j)
 *      are defined, that is, these indecies are in bounds.
 *
 * </pre> 
 * <p>Intuitively in 2D, the kernel is like
 * an unbrella and the key point is the handle. When the handle moves
 * all over the image surface, the upper outbounds of all the umbrella
 * positions is the dilation. Thus if you want the image to dilate in
 * the upper right direction, the following kernel would do with
 * the bold face key position.
 *
 * <p><center>
 * <table border=1>
 * <tr align=center><td>0</td><td>0</td><td>50</td> </tr>
 * <tr align=center><td>0</td><td>50</td><td>0</td> </tr>
 * <tr align=center><td><b>0</b></td><td>0</td><td>0</td> </tr>
 * </table></center>
 *
 * <p> Note also that zero kernel have effects on the dilation!
 * That is because of the "max" in the add and max process. Thus
 * a 3 x 1 zero kernel with the key persion at the bottom of the kernel
 * dilates the image upwards.
 * 
 * <p> 
 * After the kernel is rotated 180 degrees, Pseudo code for dilation operation
 * is as follows. Of course, you should provide the kernel in its
 * (unrotated) original form. Assuming the kernel K is of size M rows x N cols
 * and the key position is (xKey, yKey).
 * 
 * // dilation
 * for every dst pixel location (x,y){
 *    dst[x][y] = -infinity;
 *    for (i = -xKey; i < M - xKey; i++){
 *       for (j = -yKey; j < N - yKey; j++){
 *          if((x+i, y+j) are in bounds of src &&
 *	      (xKey+i, yKey+j) are in bounds of K){
 *             tmp = src[x + i][y + j]+ K[xKey + i][yKey + j];
 *	       dst[x][y] = max{tmp, dst[x][y]};
 *          }
 *       }
 *    }
 * }
 * </pre>
 *
 * <p> Dilation, unlike convolution and most neighborhood operations,
 * actually can grow the image region. But to conform with other
 * image neighborhood operations, the border pixels are set to 0.
 * For a 3 x 3 kernel with the key point at the center, there will
 * be a pixel wide 0 stripe around the border.
 *
 * <p> The kernel cannot be bigger in any dimension than the image data.
 *
 * <p> <b>Binary Image Dilation</b>
 * requires the kernel K to be binary.
 * Intuitively, starting from dst image being a duplicate of src,
 * binary dilation slides the kernel K to place the key position
 * at every non-zero point (x,y) in src image and set dst positions
 * under ones of K to 1.
 *  
 * <p> After the kernel is rotated 180 degrees, the pseudo code for
 * dilation operation is as follows. (Of course, you should provide
 * the kernel in its original unrotated form.)
 * 
 * <pre>
 * 
 * // dilating
 * for every dst pixel location (x,y){
 *    dst[x][y] = src[x][y];
 *    for (i = -xKey; i < M - xKey; i++){
 *       for (j = -yKey; j < N - yKey; j++){
 *         if(src[x+i,y+i]==1 && Key(xKey+i, yKey+j)==1){
 *            dst[x][y] = 1; break;
 *          }
 *       }
 *    }
 * }
 * </pre>

 * <p> Reference: An Introduction to Nonlinear Image Processing,
 * by Edward R. Bougherty and Jaakko Astola,
 * Spie Optical Engineering Press, 1994.
 *
 *
 * @see KernelJAI
 */
final class DilateOpImage extends AreaOpImage {

    /**
     * The kernel with which to do the dilate operation.
     */
    protected KernelJAI kernel;

    /** Kernel variables. */
    private int kw, kh, kx, ky;
    private float[] kdata;

    /**
     * Creates a DilateOpImage given a ParameterBlock containing the image
     * source and pre-rotated dilation kernel.  The image dimensions are 
     * derived
     * from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout
     * object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param kernel the pre-rotated dilation KernelJAI.
     */
    public DilateOpImage(RenderedImage source,
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
    }

    /**
     * Performs dilation on a specified rectangle. The sources are
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
 
        int dstBandOffsets[]  = dst.getBandOffsets();
        int dstPixelStride    = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        int srcBandOffsets[]  = src.getBandOffsets();
        int srcPixelStride    = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        byte dstDataArrays[][] = dst.getByteDataArrays();
        byte srcDataArrays[][] = src.getByteDataArrays();
 
        for (int k = 0; k < dnumBands; k++)  {
            byte dstData[] = dstDataArrays[k];
            byte srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
		    float f = Float.NEGATIVE_INFINITY;
		      for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
			    float tmpIK = ((int)srcData[imageOffset]&0xff) +
			      kdata[kernelVerticalOffset + v];
			    if(tmpIK > f){
			      f = tmpIK;
			    }
			    imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
		      }
		    
                    int val  = (int)f;
                    if (val < 0)  {
                        val = 0;
                    } else if (val > 255)  {
                        val = 255;
                    }
                    dstData[dstPixelOffset] = (byte)val;
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
 
        int dstBandOffsets[]  = dst.getBandOffsets();
        int dstPixelStride    = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        int srcBandOffsets[]  = src.getBandOffsets();
        int srcPixelStride    = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        short dstDataArrays[][] = dst.getShortDataArrays();
        short srcDataArrays[][] = src.getShortDataArrays();
 
        for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
		    float f = Float.NEGATIVE_INFINITY;
		      for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
			    float tmpIK = srcData[imageOffset] +
			      kdata[kernelVerticalOffset + v];
			    if(tmpIK > f){
			      f = tmpIK;
			    }
			    imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
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
 
        int dstBandOffsets[]  = dst.getBandOffsets();
        int dstPixelStride    = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        int srcBandOffsets[]  = src.getBandOffsets();
        int srcPixelStride    = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        short dstDataArrays[][] = dst.getShortDataArrays();
        short srcDataArrays[][] = src.getShortDataArrays();
 
        for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
		    float f = Float.NEGATIVE_INFINITY;
		      for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
			    float tmpIK = (srcData[imageOffset] &  0xffff) +
			      kdata[kernelVerticalOffset + v];
			    if(tmpIK > f){
			      f = tmpIK;
			    }
			    imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
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
 
        int dstBandOffsets[]  = dst.getBandOffsets();
        int dstPixelStride    = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        int srcBandOffsets[]  = src.getBandOffsets();
        int srcPixelStride    = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        int dstDataArrays[][] = dst.getIntDataArrays();
        int srcDataArrays[][] = src.getIntDataArrays();
 
        for (int k = 0; k < dnumBands; k++)  {
            int dstData[] = dstDataArrays[k];
            int srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
		    float f = Float.NEGATIVE_INFINITY;
		      for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
			    float tmpIK = (int)srcData[imageOffset] +
			      kdata[kernelVerticalOffset + v];
			    if(tmpIK > f){
			      f = tmpIK;
			    }
			    imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
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
 
        int dstBandOffsets[]  = dst.getBandOffsets();
        int dstPixelStride    = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        int srcBandOffsets[]  = src.getBandOffsets();
        int srcPixelStride    = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        float dstDataArrays[][] = dst.getFloatDataArrays();
        float srcDataArrays[][] = src.getFloatDataArrays();
 
        for (int k = 0; k < dnumBands; k++)  {
            float dstData[] = dstDataArrays[k];
            float srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
		    float f = Float.NEGATIVE_INFINITY;
		      for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
			    float tmpIK = srcData[imageOffset] +
			      kdata[kernelVerticalOffset + v];
			    if(tmpIK > f){
			      f = tmpIK;
			    }
			    imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
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
 
        int dstBandOffsets[]  = dst.getBandOffsets();
        int dstPixelStride    = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        int srcBandOffsets[]  = src.getBandOffsets();
        int srcPixelStride    = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        double dstDataArrays[][] = dst.getDoubleDataArrays();
        double srcDataArrays[][] = src.getDoubleDataArrays();
 
        for (int k = 0; k < dnumBands; k++)  {
            double dstData[] = dstDataArrays[k];
            double srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
		    double f = Double.NEGATIVE_INFINITY;
		      for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
			    double tmpIK = srcData[imageOffset] +
			      kdata[kernelVerticalOffset + v];
			    if(tmpIK > f){
			      f = tmpIK;
			    }
			    imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
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
