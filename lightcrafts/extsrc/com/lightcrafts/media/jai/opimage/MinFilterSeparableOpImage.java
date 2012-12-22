/*
 * $RCSfile: MinFilterSeparableOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:35 $
 * $State: Exp $
 */ 
package com.lightcrafts.media.jai.opimage;
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterAccessor;
import java.util.Map;
import com.lightcrafts.mediax.jai.operator.MinFilterDescriptor;
import com.lightcrafts.media.jai.opimage.MinFilterOpImage;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An OpImage class to perform min filtering on a source image.
 *
 */
final class MinFilterSeparableOpImage extends MinFilterOpImage {

    /**
     * Creates a MinFilterSeperableOpImage with the given source and
     * maskSize.  The image dimensions are derived from the source
     * image.  The tile grid layout, SampleModel, and ColorModel may
     * optionally be specified by an ImageLayout object.
     *
     * @param source a RenderedImage.
     * @param extender a BorderExtender, or null.
     * @param layout an ImageLayout optionally containing the tile grid layout,
     *        SampleModel, and ColorModel, or null.
     * @param maskSize the mask size.
     */
    public MinFilterSeparableOpImage(RenderedImage source,
                                     BorderExtender extender,
                                     Map config,
                                     ImageLayout layout,
                                     int maskSize) {
        super(source,
              extender,
              config,
              layout,
              MinFilterDescriptor.MIN_MASK_SQUARE,
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
 
        int minValues[] = new int[filterSize];
        int val, minval;
        int wp = filterSize;
        int tmpBuffer[] = new int[filterSize*dwidth];
        int tmpBufferSize = filterSize*dwidth;
 
        for (int k = 0; k < dnumBands; k++)  {
            byte dstData[] = dstDataArrays[k];
            byte srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];

            int revolver = 0;
            for (int j = 0; j < filterSize-1; j++) {
                int srcPixelOffset = srcScanlineOffset;

                for (int i = 0; i < dwidth; i++) {
                     int imageOffset = srcPixelOffset;
		     minval = Integer.MAX_VALUE;
                     for (int v = 0; v < wp; v++)  {
                          val = srcData[imageOffset] & 0xff;
                          imageOffset += srcPixelStride;
			  minval = (val < minval) ? val : minval;
                     }
                     tmpBuffer[revolver+i] = minval;
                     srcPixelOffset += srcPixelStride;
                }
                revolver += dwidth;
                srcScanlineOffset += srcScanlineStride;
            }

            // srcScanlineStride already bumped by 
            // filterSize-1*scanlineStride
            
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageOffset = srcPixelOffset;
		    minval = Integer.MAX_VALUE;
                    for (int v = 0; v < wp; v++)  {
                         val = srcData[imageOffset] & 0xff;
                         imageOffset += srcPixelStride;
			 minval = (val < minval) ? val : minval;
                    }
                    tmpBuffer[revolver + i] = minval;

		    minval = Integer.MAX_VALUE;
                    for (int b = i; b < tmpBufferSize; b += dwidth) {
                        val = tmpBuffer[b];
			minval = (val < minval) ? val : minval;
                    }

                    dstData[dstPixelOffset] = (byte)minval;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                revolver += dwidth;
                if (revolver == tmpBufferSize) {
                    revolver = 0;
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

        int minValues[] = new int[filterSize];
        int val, minval;
        int wp = filterSize;
        int tmpBuffer[] = new int[filterSize*dwidth];
        int tmpBufferSize = filterSize*dwidth;
 
        for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
 
            int revolver = 0;
            for (int j = 0; j < filterSize-1; j++) {
                int srcPixelOffset = srcScanlineOffset;
 
                for (int i = 0; i < dwidth; i++) {
                     int imageOffset = srcPixelOffset;
		     minval = Integer.MAX_VALUE;
                     for (int v = 0; v < wp; v++)  {
                          val = srcData[imageOffset];
                          imageOffset += srcPixelStride;
			  minval = (val < minval) ? val : minval;
                     }
                     tmpBuffer[revolver+i] = minval;
                     srcPixelOffset += srcPixelStride;
                }
                revolver += dwidth;
                srcScanlineOffset += srcScanlineStride;
            }
 
            // srcScanlineStride already bumped by
            // filterSize-1*scanlineStride
 
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageOffset = srcPixelOffset;
		    minval = Integer.MAX_VALUE;
                    for (int v = 0; v < wp; v++)  {
                         val = srcData[imageOffset];
                         imageOffset += srcPixelStride;
			 minval = (val < minval) ? val : minval;
                    }
                    tmpBuffer[revolver + i] = minval;
 
		    minval = Integer.MAX_VALUE;
                    for (int b = i; b < tmpBufferSize; b += dwidth) {
                        val = tmpBuffer[b];
			minval = (val < minval) ? val : minval;
                    }
 
                    dstData[dstPixelOffset] = (short)minval;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                revolver += dwidth;
                if (revolver == tmpBufferSize) {
                    revolver = 0;
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

        int minValues[] = new int[filterSize];
        int val, minval;
        int wp = filterSize;
        int tmpBuffer[] = new int[filterSize*dwidth];
        int tmpBufferSize = filterSize*dwidth;
 
        for (int k = 0; k < dnumBands; k++)  {
            short dstData[] = dstDataArrays[k];
            short srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
 
            int revolver = 0;
            for (int j = 0; j < filterSize-1; j++) {
                int srcPixelOffset = srcScanlineOffset;
 
                for (int i = 0; i < dwidth; i++) {
                     int imageOffset = srcPixelOffset;
		     minval = Integer.MAX_VALUE;
                     for (int v = 0; v < wp; v++)  {
                          val = srcData[imageOffset] & 0xfff;
                          imageOffset += srcPixelStride;
			  minval = (val < minval) ? val : minval;
                     }
                     tmpBuffer[revolver+i] = minval;
                     srcPixelOffset += srcPixelStride;
                }
                revolver += dwidth;
                srcScanlineOffset += srcScanlineStride;
            }
 
            // srcScanlineStride already bumped by
            // filterSize-1*scanlineStride
 
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageOffset = srcPixelOffset;
		    minval = Integer.MAX_VALUE;
                    for (int v = 0; v < wp; v++)  {
                         val = srcData[imageOffset] & 0xffff;
                         imageOffset += srcPixelStride;
			 minval = (val < minval) ? val : minval;
                    }
                    tmpBuffer[revolver + i] = minval;
 
		    minval = Integer.MAX_VALUE;
                    for (int b = i; b < tmpBufferSize; b += dwidth) {
                        val = tmpBuffer[b];
			minval = (val < minval) ? val : minval;
                    }
 
                    dstData[dstPixelOffset] = (short)minval;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                revolver += dwidth;
                if (revolver == tmpBufferSize) {
                    revolver = 0;
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

        int minValues[] = new int[filterSize];
        int val, minval;
        int wp = filterSize;
        int tmpBuffer[] = new int[filterSize*dwidth];
        int tmpBufferSize = filterSize*dwidth;
 
        for (int k = 0; k < dnumBands; k++)  {
            int dstData[] = dstDataArrays[k];
            int srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
 
            int revolver = 0;
            for (int j = 0; j < filterSize-1; j++) {
                int srcPixelOffset = srcScanlineOffset;
 
                for (int i = 0; i < dwidth; i++) {
                     int imageOffset = srcPixelOffset;
		     minval = Integer.MAX_VALUE;
                     for (int v = 0; v < wp; v++)  {
                          val = srcData[imageOffset];
                          imageOffset += srcPixelStride;
			  minval = (val < minval) ? val : minval;
                     }
                     tmpBuffer[revolver+i] = minval;
                     srcPixelOffset += srcPixelStride;
                }
                revolver += dwidth;
                srcScanlineOffset += srcScanlineStride;
            }
 
            // srcScanlineStride already bumped by
            // filterSize-1*scanlineStride
 
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageOffset = srcPixelOffset;
		    minval = Integer.MAX_VALUE;
                    for (int v = 0; v < wp; v++)  {
                         val = srcData[imageOffset];
                         imageOffset += srcPixelStride;
			 minval = (val < minval) ? val : minval;
                    }
                    tmpBuffer[revolver + i] = minval;
 
		    minval = Integer.MAX_VALUE;
                    for (int b = i; b < tmpBufferSize; b += dwidth) {
                        val = tmpBuffer[b];
			minval = (val < minval) ? val : minval;
                    }
 
                    dstData[dstPixelOffset] = minval;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                revolver += dwidth;
                if (revolver == tmpBufferSize) {
                    revolver = 0;
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

        float minValues[] = new float[filterSize];
        float val, minval;
        int wp = filterSize;
        float tmpBuffer[] = new float[filterSize*dwidth];
        int tmpBufferSize = filterSize*dwidth;
 
        for (int k = 0; k < dnumBands; k++)  {
            float dstData[] = dstDataArrays[k];
            float srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
 
            int revolver = 0;
            for (int j = 0; j < filterSize-1; j++) {
                int srcPixelOffset = srcScanlineOffset;
 
                for (int i = 0; i < dwidth; i++) {
                     int imageOffset = srcPixelOffset;
		     minval = Float.MAX_VALUE;
                     for (int v = 0; v < wp; v++)  {
                          val = srcData[imageOffset];
                          imageOffset += srcPixelStride;
			  minval = (val < minval) ? val : minval;
                     }
                     tmpBuffer[revolver+i] = minval;
                     srcPixelOffset += srcPixelStride;
                }
                revolver += dwidth;
                srcScanlineOffset += srcScanlineStride;
            }
 
            // srcScanlineStride already bumped by
            // filterSize-1*scanlineStride
 
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageOffset = srcPixelOffset;
		    minval = Float.MAX_VALUE;
                    for (int v = 0; v < wp; v++)  {
                         val = srcData[imageOffset];
                         imageOffset += srcPixelStride;
			 minval = (val < minval) ? val : minval;
                    }
                    tmpBuffer[revolver + i] = minval;
 
		    minval = Float.MAX_VALUE;
                    for (int b = i; b < tmpBufferSize; b += dwidth) {
                        val = tmpBuffer[b];
			minval = (val < minval) ? val : minval;
                    }
 
                    dstData[dstPixelOffset] = minval;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                revolver += dwidth;
                if (revolver == tmpBufferSize) {
                    revolver = 0;
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

        double minValues[] = new double[filterSize];
        double val, minval;
        int wp = filterSize;
        double tmpBuffer[] = new double[filterSize*dwidth];
        int tmpBufferSize = filterSize*dwidth;
 
        for (int k = 0; k < dnumBands; k++)  {
            double dstData[] = dstDataArrays[k];
            double srcData[] = srcDataArrays[k];
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
 
            int revolver = 0;
            for (int j = 0; j < filterSize-1; j++) {
                int srcPixelOffset = srcScanlineOffset;
 
                for (int i = 0; i < dwidth; i++) {
                     int imageOffset = srcPixelOffset;
		     minval = Double.MAX_VALUE;
                     for (int v = 0; v < wp; v++)  {
                          val = srcData[imageOffset];
                          imageOffset += srcPixelStride;
			  minval = (val < minval) ? val : minval;
                     }
                     tmpBuffer[revolver+i] = minval;
                     srcPixelOffset += srcPixelStride;
                }
                revolver += dwidth;
                srcScanlineOffset += srcScanlineStride;
            }
 
            // srcScanlineStride already bumped by
            // filterSize-1*scanlineStride
 
            for (int j = 0; j < dheight; j++)  {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
 
                for (int i = 0; i < dwidth; i++)  {
                    int imageOffset = srcPixelOffset;
		    minval = Double.MAX_VALUE;
                    for (int v = 0; v < wp; v++)  {
                         val = srcData[imageOffset];
                         imageOffset += srcPixelStride;
			 minval = (val < minval) ? val : minval;
                    }
                    tmpBuffer[revolver + i] = minval;
 
		    minval = Double.MAX_VALUE;
                    for (int b = i; b < tmpBufferSize; b += dwidth) {
                        val = tmpBuffer[b];
			minval = (val < minval) ? val : minval;
                    }
 
                    dstData[dstPixelOffset] = minval;
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                revolver += dwidth;
                if (revolver == tmpBufferSize) {
                    revolver = 0;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }
        }
    }

//     public static OpImage createTestImage(OpImageTester oit) {
//         return 
//            new MinFilterSeparableOpImage(oit.getSource(), null, null,
//                                             new ImageLayout(oit.getSource()),
//                                             3);
//     }

//     public static void main(String args[]) {
//         String classname = 
//                "com.lightcrafts.media.jai.opimage.MinFilterSeparableOpImage";
//         OpImageTester.performDiagnostics(classname,args);
//     }
}

