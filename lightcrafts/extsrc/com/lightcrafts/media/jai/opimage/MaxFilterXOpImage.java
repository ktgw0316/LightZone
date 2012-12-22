/*
 * $RCSfile: MaxFilterXOpImage.java,v $
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
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterAccessor;
import java.util.Map;
import com.lightcrafts.mediax.jai.operator.MaxFilterDescriptor;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An OpImage class to perform max filtering on a source image.
 *
 */
final class MaxFilterXOpImage extends MaxFilterOpImage {

    /**
     * Creates a MaxFilterXOpImage with the given source and
     * maskSize.  The image dimensions are derived from the source
     * image.  The tile grid layout, SampleModel, and ColorModel may
     * optionally be specified by an ImageLayout object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     */
    public MaxFilterXOpImage(RenderedImage source,
                             BorderExtender extender,
                             Map config,
                             ImageLayout layout,
                             int maskSize) {
        super(source,
              extender,
              config,
              layout,
              MaxFilterDescriptor.MAX_MASK_PLUS,
              maskSize);
    }

    protected void byteLoop(RasterAccessor src, 
                            RasterAccessor dst,
                            int filterSize) {
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
	int scanPlusPixelStride = srcScanlineStride+srcPixelStride;
	int scanMinusPixelStride = srcScanlineStride-srcPixelStride;
	int topRightOffset = srcPixelStride*(filterSize-1);
 
        int maxval, val;
        int wp = filterSize;
        int offset = filterSize/2;
 
        for (int k = 0; k < dnumBands; k++)  {
            byte dstData[] = dstDataArrays[k];
            byte srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    maxval = Integer.MIN_VALUE;
 
                    // figure out where the top left of the X starts
                    int imageOffset = srcPixelOffset;
                    for (int u = 0; u < wp; u++)  {
                        val = (int)(srcData[imageOffset]&0xff);
                        imageOffset += scanPlusPixelStride;
			maxval = (val > maxval) ? val : maxval;
                    }
 
                    // figure out where the top right of the X starts
                    imageOffset = srcPixelOffset + topRightOffset;
 
                    for (int v = 0; v < wp; v++)  {
                        val = (int)(srcData[imageOffset]&0xff);
                        imageOffset += scanMinusPixelStride;
			maxval = (val > maxval) ? val : maxval;
                    }

                    dstData[dstPixelOffset] = (byte)maxval;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    protected void shortLoop(RasterAccessor src, 
                             RasterAccessor dst,
                             int filterSize)  {
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
	int scanPlusPixelStride = srcScanlineStride+srcPixelStride;
	int scanMinusPixelStride = srcScanlineStride-srcPixelStride;
	int topRightOffset = srcPixelStride*(filterSize-1);
 
        int maxval, val;
        int wp = filterSize;
        int offset = filterSize/2;
 
        for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    maxval = Integer.MIN_VALUE;
 
                    // figure out where the top left of the X starts
                    int imageOffset =
                        srcPixelOffset;
                    for (int u = 0; u < wp; u++)  {
                        val = (int)(srcData[imageOffset]);
                        imageOffset += scanPlusPixelStride;
			maxval = (val > maxval) ? val : maxval;
                    }
 
                    // figure out where the top right of the X starts
                    imageOffset = srcPixelOffset + topRightOffset;
 
                    for (int v = 0; v < wp; v++)  {
                        val = (int)(srcData[imageOffset]);
                        imageOffset += scanMinusPixelStride;
			maxval = (val > maxval) ? val : maxval;
                    }

                    dstData[dstPixelOffset] = (short)maxval;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    protected void ushortLoop(RasterAccessor src, 
                              RasterAccessor dst,
                              int filterSize)  {
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
	int scanPlusPixelStride = srcScanlineStride+srcPixelStride;
	int scanMinusPixelStride = srcScanlineStride-srcPixelStride;
	int topRightOffset = srcPixelStride*(filterSize-1);
 
        int maxval, val;
        int wp = filterSize;
        int offset = filterSize/2;
 
        for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    maxval = Integer.MIN_VALUE;
 
                    // figure out where the top left of the X starts
                    int imageOffset =
                        srcPixelOffset;
                    for (int u = 0; u < wp; u++)  {
                        val = (int)(srcData[imageOffset]&0xffff);
                        imageOffset += scanPlusPixelStride;
			maxval = (val > maxval) ? val : maxval;
                    }
 
                    // figure out where the top right of the X starts
                    imageOffset = srcPixelOffset + topRightOffset;
 
                    for (int v = 0; v < wp; v++)  {
                        val = (int)(srcData[imageOffset]&0xffff);
                        imageOffset += scanMinusPixelStride;
			maxval = (val > maxval) ? val : maxval;
                    }

                    dstData[dstPixelOffset] = (short)maxval;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    protected void intLoop(RasterAccessor src, 
                           RasterAccessor dst,
                           int filterSize)  {
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
	int scanPlusPixelStride = srcScanlineStride+srcPixelStride;
	int scanMinusPixelStride = srcScanlineStride-srcPixelStride;
	int topRightOffset = srcPixelStride*(filterSize-1);
 
        int maxval, val;
        int wp = filterSize;
        int offset = filterSize/2;
 
        for (int k = 0; k < dnumBands; k++)  {
            int dstData[] = dstDataArrays[k];
            int srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    maxval = Integer.MIN_VALUE;
 
                    // figure out where the top left of the X starts
                    int imageOffset =
                        srcPixelOffset;
                    for (int u = 0; u < wp; u++)  {
                        val = srcData[imageOffset];
                        imageOffset += scanPlusPixelStride;
			maxval = (val > maxval) ? val : maxval;
                    }
 
                    // figure out where the top right of the X starts
                    imageOffset = srcPixelOffset + topRightOffset;
 
                    for (int v = 0; v < wp; v++)  {
                        val = srcData[imageOffset];
                        imageOffset += scanMinusPixelStride;
			maxval = (val > maxval) ? val : maxval;
                    }

                    dstData[dstPixelOffset] = maxval;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    protected void floatLoop(RasterAccessor src, 
                             RasterAccessor dst,
                             int filterSize)  {
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
	int scanPlusPixelStride = srcScanlineStride+srcPixelStride;
	int scanMinusPixelStride = srcScanlineStride-srcPixelStride;
	int topRightOffset = srcPixelStride*(filterSize-1);
 
        float maxval, val;
        int wp = filterSize;
        int offset = filterSize/2;
 
        for (int k = 0; k < dnumBands; k++)  {
            float dstData[] = dstDataArrays[k];
            float srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    maxval = -Float.MAX_VALUE;
 
                    // figure out where the top left of the X starts
                    int imageOffset =
                        srcPixelOffset;
                    for (int u = 0; u < wp; u++)  {
                        val = srcData[imageOffset];
                        imageOffset += scanPlusPixelStride;
			maxval = (val > maxval) ? val : maxval;
                    }
 
                    // figure out where the top right of the X starts
                    imageOffset = srcPixelOffset + topRightOffset;
 
                    for (int v = 0; v < wp; v++)  {
                        val = srcData[imageOffset];
                        imageOffset += scanMinusPixelStride;
			maxval = (val > maxval) ? val : maxval;
                    }
 
                    dstData[dstPixelOffset] = maxval;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

    protected void doubleLoop(RasterAccessor src, 
                              RasterAccessor dst,
                              int filterSize)  {
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
	int scanPlusPixelStride = srcScanlineStride+srcPixelStride;
	int scanMinusPixelStride = srcScanlineStride-srcPixelStride;
	int topRightOffset = srcPixelStride*(filterSize-1);
 
        double maxval, val;
        int wp = filterSize;
        int offset = filterSize/2;
 
        for (int k = 0; k < dnumBands; k++)  {
            double dstData[] = dstDataArrays[k];
            double srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    maxval = -Double.MAX_VALUE;
 
                    // figure out where the top left of the X starts
                    int imageOffset =
                        srcPixelOffset;
                    for (int u = 0; u < wp; u++)  {
                        val = srcData[imageOffset];
                        imageOffset += scanPlusPixelStride;
			maxval = (val > maxval) ? val : maxval;
                    }
 
                    // figure out where the top right of the X starts
                    imageOffset = srcPixelOffset + topRightOffset;
 
                    for (int v = 0; v < wp; v++)  {
                        val = srcData[imageOffset];
                        imageOffset += scanMinusPixelStride;
			maxval = (val > maxval) ? val : maxval;
                    }
 
                    dstData[dstPixelOffset] = maxval;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

//     public static OpImage createTestImage(OpImageTester oit) {
//         return new MaxFilterXOpImage(oit.getSource(), null, null,
//                                         new ImageLayout(oit.getSource()),
//                                         3);
//     }
}
