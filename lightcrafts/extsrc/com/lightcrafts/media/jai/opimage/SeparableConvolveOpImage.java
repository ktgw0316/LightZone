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
import com.lightcrafts.mediax.jai.KernelJAI;
import java.util.Map;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An OpImage class to perform separable convolve on a source image.
 *
 *
 */
final class SeparableConvolveOpImage extends AreaOpImage {

  static int byteLoopCounter =0;

    protected KernelJAI kernel;
    protected int kw, kh, kx, ky;

    protected float hValues[];
    protected float vValues[];
    protected float hTables[][]; 

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
    public SeparableConvolveOpImage(RenderedImage source,
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

        if (sampleModel.getDataType() == DataBuffer.TYPE_BYTE) {
            hTables = new float[hValues.length][256];
            for (int i = 0; i < hValues.length; i++) {
                float k = hValues[i];
                for (int j = 0; j < 256; j++) {
                    byte b = (byte)j;
                    float f = (float)j;
                    hTables[i][b+128] = k*f;
                }
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
            new RasterAccessor(source, srcRect, formatTags[0], 
                               getSource(0).getColorModel());
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

    protected void byteLoop(RasterAccessor src, 
                            RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        byte dstDataArrays[][] = dst.getByteDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        byte srcDataArrays[][] = src.getByteDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
 
        float tmpBuffer[] = new float[kh*dwidth];
        int   tmpBufferSize = kh*dwidth;
 
        for (int k = 0; k < dnumBands; k++)  {
            byte dstData[] = dstDataArrays[k];
            byte srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];

            int revolver = 0;
            int kvRevolver = 0;                 // to match kernel vValues
            for (int j = 0; j < kh-1; j++) {
                int srcPixelOffset = srcScanlineOffset;

                for (int i = 0; i < dwidth; i++) {
                     int imageOffset = srcPixelOffset;
                     float f = 0.0f;
                     for (int v = 0; v < kw; v++)  {
                          f += hTables[v][srcData[imageOffset]+128];
                          imageOffset += srcPixelStride;
                     }
                     tmpBuffer[revolver+i] = f;
                     srcPixelOffset += srcPixelStride;
                }
                revolver += dwidth;
                srcScanlineOffset += srcScanlineStride;
            }

            // srcScanlineStride already bumped by 
            // kh-1*scanlineStride
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageOffset = srcPixelOffset;
                    float f = 0.0f;
                    for (int v = 0; v < kw; v++)  {
                         f += hTables[v][srcData[imageOffset]+128];
                         imageOffset += srcPixelStride;
                    }
                    tmpBuffer[revolver + i] = f;

                    f = 0.5f;
                    // int a = 0;  
		    // The vertical kernel must revolve as well
		    int b = kvRevolver + i;
                    for (int a=0; a < kh; a++){
                        f += tmpBuffer[b] * vValues[a];
		        b += dwidth;
			if (b >= tmpBufferSize) b -= tmpBufferSize;
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

                revolver += dwidth;
                if (revolver == tmpBufferSize) {
                    revolver = 0;
                } 
                kvRevolver += dwidth;
                if (kvRevolver == tmpBufferSize) {
                    kvRevolver = 0;
                } 
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }



    protected void shortLoop(RasterAccessor src, 
                             RasterAccessor dst) {
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

        float tmpBuffer[] = new float[kh*dwidth];
        int tmpBufferSize = kh*dwidth;
 
        for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
 
            int revolver = 0;
            int kvRevolver = 0;                 // to match kernel vValues
            for (int j = 0; j < kh-1; j++) {
                int srcPixelOffset = srcScanlineOffset;
 
                for (int i = 0; i < dwidth; i++) {
                     int imageOffset = srcPixelOffset;
                     float f = 0.0f;
                     for (int v = 0; v < kw; v++)  {
                          f +=  (srcData[imageOffset]) * hValues[v];
                          imageOffset += srcPixelStride;
                     }
                     tmpBuffer[revolver+i] = f;
                     srcPixelOffset += srcPixelStride;
                }
                revolver += dwidth;
                srcScanlineOffset += srcScanlineStride;
            }
 
            // srcScanlineStride already bumped by
            // kh-1*scanlineStride

            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageOffset = srcPixelOffset;
                    float f = 0.0f;
                    for (int v = 0; v < kw; v++)  {
                         f +=  (srcData[imageOffset]) * hValues[v];
                         imageOffset += srcPixelStride;
                    }
                    tmpBuffer[revolver + i] = f;
 
                    f = 0.5f;
		    int b = kvRevolver + i;
                    for (int a=0; a < kh; a++){
                        f += tmpBuffer[b] * vValues[a];
		        b += dwidth;
			if (b >= tmpBufferSize) b -= tmpBufferSize;
                    }
 
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
                revolver += dwidth;

                if (revolver == tmpBufferSize) {
                    revolver = 0;
                } 
                kvRevolver += dwidth;
                if (kvRevolver == tmpBufferSize) {
                    kvRevolver = 0;
                } 

                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }

 
    }

    protected void ushortLoop(RasterAccessor src, 
                              RasterAccessor dst) {
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
        float tmpBuffer[] = new float[kh*dwidth];
        int tmpBufferSize = kh*dwidth;
 
        for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
 
            int revolver = 0;
            int kvRevolver = 0;                 // to match kernel vValues
            for (int j = 0; j < kh-1; j++) {
                int srcPixelOffset = srcScanlineOffset;
 
                for (int i = 0; i < dwidth; i++) {
                     int imageOffset = srcPixelOffset;
                     float f = 0.0f;
                     for (int v = 0; v < kw; v++)  {
                          f +=  (srcData[imageOffset] & 0xffff) * hValues[v];
                          imageOffset += srcPixelStride;
                     }
                     tmpBuffer[revolver+i] = f;
                     srcPixelOffset += srcPixelStride;
                }
                revolver += dwidth;
                srcScanlineOffset += srcScanlineStride;
            }
 
            // srcScanlineStride already bumped by
            // kh-1*scanlineStride

            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
		for (int i = 0; i < dwidth; i++)  {
                    int imageOffset = srcPixelOffset;
                    float f = 0.0f;
                    for (int v = 0; v < kw; v++)  {
                         f +=  (srcData[imageOffset] & 0xffff) * hValues[v];
                         imageOffset += srcPixelStride;
                    }
                    tmpBuffer[revolver + i] = f;
 
                    f = 0.5f;

		    int b = kvRevolver + i;
                    for (int a=0; a < kh; a++){
                        f += tmpBuffer[b] * vValues[a];
		        b += dwidth;
			if (b >= tmpBufferSize) b -= tmpBufferSize;
                    }

                    int val = (int)f;
                    if (val < 0) {
                       val = 0;
                    } else if (val > 0xffff) {
                       val = 0xffff;
                    }

                    dstData[dstPixelOffset] = (short)val;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                revolver += dwidth;
                if (revolver == tmpBufferSize) {
                    revolver = 0;
                } 
                kvRevolver += dwidth;
                if (kvRevolver == tmpBufferSize) {
                    kvRevolver = 0;
                } 
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    protected void intLoop(RasterAccessor src, 
                           RasterAccessor dst) {
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

        float tmpBuffer[] = new float[kh*dwidth];
        int tmpBufferSize = kh*dwidth;

        for (int k = 0; k < dnumBands; k++)  {
            int dstData[] = dstDataArrays[k];
            int srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
 
            int revolver = 0;
            int kvRevolver = 0;                 // to match kernel vValues
            for (int j = 0; j < kh-1; j++) {
                int srcPixelOffset = srcScanlineOffset;
 
                for (int i = 0; i < dwidth; i++) {
                     int imageOffset = srcPixelOffset;
                     float f = 0.0f;
                     for (int v = 0; v < kw; v++)  {
                          f +=  (srcData[imageOffset]) * hValues[v];
                          imageOffset += srcPixelStride;
                     }
                     tmpBuffer[revolver+i] = f;
                     srcPixelOffset += srcPixelStride;
                }
                revolver += dwidth;
                srcScanlineOffset += srcScanlineStride;
            }
 
            // srcScanlineStride already bumped by
            // kh-1*scanlineStride
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++)  {
                    int imageOffset = srcPixelOffset;
                    float f = 0.0f;
                    for (int v = 0; v < kw; v++)  {
                         f +=  (srcData[imageOffset]) * hValues[v];
                         imageOffset += srcPixelStride;
                    }
                    tmpBuffer[revolver + i] = f;
 
                    f = 0.5f;

		    int b = kvRevolver + i;
                    for (int a=0; a < kh; a++){
                        f += tmpBuffer[b] * vValues[a];
		        b += dwidth;
			if (b >= tmpBufferSize) b -= tmpBufferSize;
                    }

                    int val = (int)f;

                    dstData[dstPixelOffset] = val;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                revolver += dwidth;
                if (revolver == tmpBufferSize) {
                    revolver = 0;
                } 
                kvRevolver += dwidth;
                if (kvRevolver == tmpBufferSize) {
                    kvRevolver = 0;
                } 
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
 
    }

    protected void floatLoop(RasterAccessor src, 
                             RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        float dstDataArrays[][] = dst.getFloatDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        float srcDataArrays[][] = src.getFloatDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        float tmpBuffer[] = new float[kh*dwidth];
        int tmpBufferSize = kh*dwidth;
 
        for (int k = 0; k < dnumBands; k++)  {
            float dstData[] = dstDataArrays[k];
            float srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
 
            int revolver = 0;
            int kvRevolver = 0;                 // to match kernel vValues
            for (int j = 0; j < kh-1; j++) {
                int srcPixelOffset = srcScanlineOffset;
 
                for (int i = 0; i < dwidth; i++) {
                     int imageOffset = srcPixelOffset;
                     float f = 0.0f;
                     for (int v = 0; v < kw; v++)  {
                          f +=  (srcData[imageOffset]) * hValues[v];
                          imageOffset += srcPixelStride;
                     }
                     tmpBuffer[revolver+i] = f;
                     srcPixelOffset += srcPixelStride;
                }
                revolver += dwidth;
                srcScanlineOffset += srcScanlineStride;
            }
 
            // srcScanlineStride already bumped by
            // kh-1*scanlineStride

            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++)  {
                    int imageOffset = srcPixelOffset;
                    float f = 0.0f;
                    for (int v = 0; v < kw; v++)  {
                         f +=  (srcData[imageOffset]) * hValues[v];
                         imageOffset += srcPixelStride;
                    }
                    tmpBuffer[revolver + i] = f;
 
                    f = 0.0f;

		    int b = kvRevolver + i;
                    for (int a=0; a < kh; a++){
                        f += tmpBuffer[b] * vValues[a];
		        b += dwidth;
			if (b >= tmpBufferSize) b -= tmpBufferSize;
                    }
 
                    dstData[dstPixelOffset] = f;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                revolver += dwidth;
                if (revolver == tmpBufferSize) {
                    revolver = 0;
                } 
                kvRevolver += dwidth;
                if (kvRevolver == tmpBufferSize) {
                    kvRevolver = 0;
                } 
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    protected void doubleLoop(RasterAccessor src, 
                              RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();
 
        double dstDataArrays[][] = dst.getDoubleDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();
 
        double srcDataArrays[][] = src.getDoubleDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        double tmpBuffer[] = new double[kh*dwidth];
        int tmpBufferSize = kh*dwidth;
 
        for (int k = 0; k < dnumBands; k++)  {
            double dstData[] = dstDataArrays[k];
            double srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
 
            int revolver = 0;
            int kvRevolver = 0;                 // to match kernel vValues
            for (int j = 0; j < kh-1; j++) {
                int srcPixelOffset = srcScanlineOffset;
 
                for (int i = 0; i < dwidth; i++) {
                     int imageOffset = srcPixelOffset;
                     double f = 0.0;
                     for (int v = 0; v < kw; v++)  {
                          f +=  (srcData[imageOffset]) * hValues[v];
                          imageOffset += srcPixelStride;
                     }
                     tmpBuffer[revolver+i] = f;
                     srcPixelOffset += srcPixelStride;
                }
                revolver += dwidth;
                srcScanlineOffset += srcScanlineStride;
            }
 
            // srcScanlineStride already bumped by
            // kh-1*scanlineStride

            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;

                for (int i = 0; i < dwidth; i++)  {
                    int imageOffset = srcPixelOffset;
                    double f = 0.0;
                    for (int v = 0; v < kw; v++)  {
                         f +=  (srcData[imageOffset]) * hValues[v];
                         imageOffset += srcPixelStride;
                    }
                    tmpBuffer[revolver + i] = f;
 
                    f = 0.0;

		    int b = kvRevolver + i;
                    for (int a=0; a < kh; a++){
                        f += tmpBuffer[b] * vValues[a];
		        b += dwidth;
			if (b >= tmpBufferSize) b -= tmpBufferSize;
                    }

                    dstData[dstPixelOffset] = f;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                revolver += dwidth;
                if (revolver == tmpBufferSize) {
                    revolver = 0;
                } 
                kvRevolver += dwidth;
                if (kvRevolver == tmpBufferSize) {
                    kvRevolver = 0;
                } 
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
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
//         String classname = "com.lightcrafts.media.jai.opimage.SeparableConvolveOpImage";
//         OpImageTester.performDiagnostics(classname,args);
//     }
}
