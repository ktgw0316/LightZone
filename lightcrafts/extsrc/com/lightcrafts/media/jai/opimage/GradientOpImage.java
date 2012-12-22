/*
 * $RCSfile: GradientOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:27 $
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
 * An OpImage class to perform Gradient operation on a source image.
 *
 * <p> The Kernels cannot be bigger in any dimension than the image data.
 *
 *
 * @see KernelJAI
 */
final class GradientOpImage extends AreaOpImage {

    /**
     * The kernel with which to do the gradient operation.
     */
    protected KernelJAI kernel_h, kernel_v;

    /** Kernel variables. */
    private int kw, kh;

    /**
     * Creates a GradientOpImage given the image source and
     * the pair of orthogonal gradient kernels. The image dimensions are 
     * derived from the source image.  The tile grid layout, SampleModel, and
     * ColorModel may optionally be specified by an ImageLayout object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.

     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param kernel_h the horizontal kernel.
     * @param kernel_v the vertical kernel
     */
    public GradientOpImage(RenderedImage source,
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

        // Local copy of the kernels
	this.kernel_h = kernel_h;
        this.kernel_v = kernel_v;

        //
        // At this point both kernels should be of same width & height
        // so it's enough to get the information from one of them
        //
	kw = kernel_h.getWidth();
	kh = kernel_h.getHeight();
    }

    /**
     * Performs gradient operation on a specified rectangle. The sources are
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
            new RasterAccessor(source,
                               srcRect, 
                               formatTags[0], getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor =
            new RasterAccessor(dest,
                               destRect, 
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
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        float[] kdata_h = kernel_h.getKernelData();
        float[] kdata_v = kernel_v.getKernelData();
 
        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        byte srcDataArrays[][] = src.getByteDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
        
        for (int k = 0; k < dnumBands; k++)  {
            byte dstData[] = dstDataArrays[k];
            byte srcData[] = srcDataArrays[k];
            
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    float f_h = 0.0f;
                    float f_v = 0.0f;
                    
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
                    
                    for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        
                        for (int v = 0; v < kw; v++)  {
                            
                            f_h += ((int)srcData[imageOffset] & 0xff)
                                * kdata_h[kernelVerticalOffset + v];
                            f_v += ((int)srcData[imageOffset] & 0xff)
                                * kdata_v[kernelVerticalOffset + v];
                            
                            imageOffset += srcPixelStride;
                        }
                        
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
                    }
                    
                    // Do the Gradient 
                    float sqr_f_h = f_h * f_h;
                    float sqr_f_v = f_v * f_v;
                    float result = (float)Math.sqrt(sqr_f_h + sqr_f_v);
                    
                    int val  = (int)(result + 0.5f); // Round
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
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        float[] kdata_h = kernel_h.getKernelData();
        float[] kdata_v = kernel_v.getKernelData();
 
        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                for (int i = 0; i < dwidth; i++)  {
                    float f_h = 0.0f;
                    float f_v = 0.0f;
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
                    for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
                            f_h += (srcData[imageOffset])
                                * kdata_h[kernelVerticalOffset + v];
                            f_v += (srcData[imageOffset])
                                * kdata_v[kernelVerticalOffset + v];
                            imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
                    }

                    // Do the Gradient
                    float sqr_f_h = f_h * f_h;
                    float sqr_f_v = f_v * f_v;
                    float result = (float)Math.sqrt(sqr_f_h + sqr_f_v);
                    
                    int val = (int)(result + 0.5f); // Round
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

    private void ushortLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        float[] kdata_h = kernel_h.getKernelData();
        float[] kdata_v = kernel_v.getKernelData();
 
        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                for (int i = 0; i < dwidth; i++)  {
                    float f_h = 0.0f;
                    float f_v = 0.0f;
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
                    for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
                            f_h += (srcData[imageOffset] & 0xffff)
                                * kdata_h[kernelVerticalOffset + v];
                            f_v += (srcData[imageOffset] & 0xffff)
                                * kdata_v[kernelVerticalOffset + v];
                            imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
                    }

                    // Do the Gradient
                    float sqr_f_h = f_h * f_h;
                    float sqr_f_v = f_v * f_v;
                    float result = (float)Math.sqrt(sqr_f_h + sqr_f_v);
                    
                    int val = (int)(result + 0.5f); // Round
                    if (val < 0) {
                       val = 0;
                    } else if (val > 0xffff) {
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
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        float[] kdata_h = kernel_h.getKernelData();
        float[] kdata_v = kernel_v.getKernelData();
 
        int dstDataArrays[][] = dst.getIntDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        int srcDataArrays[][] = src.getIntDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        for (int k = 0; k < dnumBands; k++)  {
            int dstData[] = dstDataArrays[k];
            int srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                for (int i = 0; i < dwidth; i++)  {
                    float f_h = 0.0f;
                    float f_v = 0.0f;
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
                    for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
                            f_h += ((int)srcData[imageOffset])
                                * kdata_h[kernelVerticalOffset + v];
                            f_v += ((int)srcData[imageOffset])
                                * kdata_v[kernelVerticalOffset + v];
                            imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
                    }

                    // Do the Gradient
                    float sqr_f_h = f_h * f_h;
                    float sqr_f_v = f_v * f_v;
                    float result = (float)Math.sqrt(sqr_f_h + sqr_f_v);
                    
                    dstData[dstPixelOffset] = (int)(result + 0.5f); // Round
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void floatLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        float[] kdata_h = kernel_h.getKernelData();
        float[] kdata_v = kernel_v.getKernelData();
 
        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        float srcDataArrays[][] = src.getFloatDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        for (int k = 0; k < dnumBands; k++)  {
            float dstData[] = dstDataArrays[k];
            float srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset; 
                for (int i = 0; i < dwidth; i++)  {
                    float f_h = 0.0f;
                    float f_v = 0.0f;
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
                    for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
                            f_h += (srcData[imageOffset])
                                * kdata_h[kernelVerticalOffset + v];
                            f_v += (srcData[imageOffset])
                                * kdata_v[kernelVerticalOffset + v];
                            imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
                    }

                    // Do the Gradient
                    float sqr_f_h = f_h * f_h;
                    float sqr_f_v = f_v * f_v;
                    float result = (float)Math.sqrt(sqr_f_h + sqr_f_v);
                    
                    dstData[dstPixelOffset] = result;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    private void doubleLoop(RasterAccessor src, RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        float[] kdata_h = kernel_h.getKernelData();
        float[] kdata_v = kernel_v.getKernelData();
 
        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        double srcDataArrays[][] = src.getDoubleDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        for (int k = 0; k < dnumBands; k++)  {
            double dstData[] = dstDataArrays[k];
            double srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    double f_h = 0.0;
                    double f_v = 0.0;
                    int kernelVerticalOffset = 0;
                    int imageVerticalOffset = srcPixelOffset;
                    for (int u = 0; u < kh; u++)  {
                        int imageOffset = imageVerticalOffset;
                        for (int v = 0; v < kw; v++)  {
                            f_h += (srcData[imageOffset])
                                * kdata_h[kernelVerticalOffset + v];
                            f_v += (srcData[imageOffset])
                                * kdata_v[kernelVerticalOffset + v];
                            imageOffset += srcPixelStride;
                        }
                        kernelVerticalOffset += kw;
                        imageVerticalOffset += srcScanlineStride;
                    }

                    // Do the Gradient
                    double sqr_f_h = f_h * f_h;
                    double sqr_f_v = f_v * f_v;
                    double result = Math.sqrt(sqr_f_h + sqr_f_v);
                    
                    dstData[dstPixelOffset] = result;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
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

//         return new GradientOpImage(oit.getSource(), null, null,
//                                    new ImageLayout(oit.getSource()),
//                                    kern_h, kern_v);
//     }
 
//     public static void main(String args[]) {
//         String classname = "com.lightcrafts.media.jai.opimage.GradientOpImage";
//         OpImageTester.performDiagnostics(classname,args);
//     }
}
