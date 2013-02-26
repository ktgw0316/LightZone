/*
 * $RCSfile: AbsoluteOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:11 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import java.util.Map;
// import com.lightcrafts.media.jai.test.OpImageTester;

/**
 * An <code>OpImage</code> implementing the "Absolute" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.AbsoluteDescriptor</code>.
 *
 * <p>This <code>OpImage</code> takes the absolute value of the pixel 
 * values of an image.  The operation is done on a per-band basis.
 * For an integral number, its absolute value is taken as x = (~x) +1,
 * where ~x is the 1's complement of that number.
 * If the number is the maximum negative of its type, then it will remain
 * the same value.
 *
 * @since EA2
 * @see com.lightcrafts.mediax.jai.operator.AbsoluteDescriptor
 * @see AbsoluteCRIF
 *
 */
final class AbsoluteOpImage extends PointOpImage {

    /**
     * Constructs an <code>AbsoluteOpImage</code>.
     *
     * <p>The <code>layout</code> parameter may optionally contains the
     * tile grid layout, sample model, and/or color model. The image
     * dimension is set to the same values as that of the source image.
     *
     * <p>The image layout of the source image is used as the fall-back
     * for the image layout of the destination image. Any layout parameters
     * not specified in the <code>layout</code> argument are set to the
     * same value as that of the source.
     *
     * @param source     The source image.
     * @param layout     The destination image layout.
     */
    public AbsoluteOpImage(RenderedImage source,
                           Map config,
                           ImageLayout layout) {
        super(source, layout, config, true);

        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Map the pixels inside a specified rectangle whose value is within a 
     * rang to a constant on a per-band basis.
     *
     * @param sources   Cobbled sources, guaranteed to provide all the
     *                  source data necessary for computing the rectangle.
     * @param dest      The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor src = new RasterAccessor(sources[0], destRect, 
                                                formatTags[0],
                                                getSourceImage(0).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect, 
                                                formatTags[1],
                                                getColorModel());

        if(dst.isBinary()) {
            byte[] dstBits = dst.getBinaryDataArray();
            System.arraycopy(src.getBinaryDataArray(), 0,
                             dstBits, 0, dstBits.length);

            dst.copyBinaryDataToRaster();

	    return;
        }

        /* Find out what kind of data type is used to store the image */
        switch ( dst.getDataType() ) {
        case DataBuffer.TYPE_BYTE:
            byteAbsolute(dst.getNumBands(),
                         dst.getWidth(),
                         dst.getHeight(),
                         src.getScanlineStride(),
                         src.getPixelStride(),
                         src.getBandOffsets(),
                         src.getByteDataArrays(),
                         dst.getScanlineStride(),
                         dst.getPixelStride(),
                         dst.getBandOffsets(),
                         dst.getByteDataArrays());
            break;

        case DataBuffer.TYPE_SHORT:
            shortAbsolute(dst.getNumBands(),
                          dst.getWidth(),
                          dst.getHeight(),
                          src.getScanlineStride(),
                          src.getPixelStride(),
                          src.getBandOffsets(),
                          src.getShortDataArrays(),
                          dst.getScanlineStride(),
                          dst.getPixelStride(),
                          dst.getBandOffsets(),
                          dst.getShortDataArrays());
            break;

        case DataBuffer.TYPE_USHORT:
            ushortAbsolute(dst.getNumBands(),
                           dst.getWidth(),
                           dst.getHeight(),
                           src.getScanlineStride(),
                           src.getPixelStride(),
                           src.getBandOffsets(),
                           src.getShortDataArrays(),
                           dst.getScanlineStride(),
                           dst.getPixelStride(),
                           dst.getBandOffsets(),
                           dst.getShortDataArrays());
            break;

        case DataBuffer.TYPE_INT:
            intAbsolute(dst.getNumBands(),
                        dst.getWidth(),
                        dst.getHeight(),
                        src.getScanlineStride(),
                        src.getPixelStride(),
                        src.getBandOffsets(),
                        src.getIntDataArrays(),
                        dst.getScanlineStride(),
                        dst.getPixelStride(),
                        dst.getBandOffsets(),
                        dst.getIntDataArrays());
            break;

        case DataBuffer.TYPE_FLOAT:
            floatAbsolute(dst.getNumBands(),
                          dst.getWidth(),
                          dst.getHeight(),
                          src.getScanlineStride(),
                          src.getPixelStride(),
                          src.getBandOffsets(),
                          src.getFloatDataArrays(),
                          dst.getScanlineStride(),
                          dst.getPixelStride(),
                          dst.getBandOffsets(),
                          dst.getFloatDataArrays());
            break;

        case DataBuffer.TYPE_DOUBLE:
            doubleAbsolute(dst.getNumBands(),
                           dst.getWidth(),
                           dst.getHeight(),
                           src.getScanlineStride(),
                           src.getPixelStride(),
                           src.getBandOffsets(),
                           src.getDoubleDataArrays(),
                           dst.getScanlineStride(),
                           dst.getPixelStride(),
                           dst.getBandOffsets(),
                           dst.getDoubleDataArrays());
            break;
        }
        if (dst.needsClamping()) {
            dst.clampDataArrays();
        }
        dst.copyDataToRaster();
    }

    private void byteAbsolute(int numBands,
                              int dstWidth,
                              int dstHeight,
                              int srcScanlineStride,
                              int srcPixelStride,
                              int[] srcBandOffsets,
                              byte[][] srcData,
                              int dstScanlineStride,
                              int dstPixelStride,
                              int[] dstBandOffsets,
                              byte[][] dstData) {
        for (int band = 0; band < numBands; band++) {
            byte[] src = srcData[band];
            byte[] dst = dstData[band];
            int pixelValue;
            int srcLineOffset = srcBandOffsets[band];
            int dstLineOffset = dstBandOffsets[band];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // For byte data, this is a straight copy.
                    dst[dstPixelOffset] = src[srcPixelOffset];
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcScanlineStride;
                dstLineOffset += dstScanlineStride;
            }
        }
    }

    private void shortAbsolute(int numBands,
                               int dstWidth,
                               int dstHeight,
                               int srcScanlineStride,
                               int srcPixelStride,
                               int[] srcBandOffsets,
                               short[][] srcData,
                               int dstScanlineStride,
                               int dstPixelStride,
                               int[] dstBandOffsets,
                               short[][] dstData) {
        for (int band= 0; band < numBands; band++)  {
            short[] src = srcData[band];
            short[] dst = dstData[band];
            short pixelValue;
            int srcLineOffset = srcBandOffsets[band];
            int dstLineOffset = dstBandOffsets[band];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    pixelValue = src[srcPixelOffset];

                    if ( (pixelValue != Short.MIN_VALUE) &&
                         (pixelValue & Short.MIN_VALUE) != 0 ) {
                        // It is not 0x8000  and its sign bit is set.
                        dst[dstPixelOffset] = (short)-src[srcPixelOffset];
                    } else {
                        // It is either the minimum of short, i.e. 0x8000,
                        // or a positive number;
                        dst[dstPixelOffset] = src[srcPixelOffset];
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcScanlineStride;
                dstLineOffset += dstScanlineStride;
            }
        }
    }

    private void ushortAbsolute(int numBands,
                                int dstWidth,
                                int dstHeight,
                                int srcScanlineStride,
                                int srcPixelStride,
                                int[] srcBandOffsets,
                                short[][] srcData,
                                int dstScanlineStride,
                                int dstPixelStride,
                                int[] dstBandOffsets,
                                short[][] dstData) {
        for (int band= 0; band < numBands; band++)  {
            short[] src = srcData[band];
            short[] dst = dstData[band];
            int srcLineOffset = srcBandOffsets[band];
            int dstLineOffset = dstBandOffsets[band];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    // For unsigned short data, this is a straight copy.
                    dst[dstPixelOffset] = src[srcPixelOffset];
                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcScanlineStride;
                dstLineOffset += dstScanlineStride;
            }
        }
    }

    private void intAbsolute(int numBands,
                             int dstWidth,
                             int dstHeight,
                             int srcScanlineStride,
                             int srcPixelStride,
                             int[] srcBandOffsets,
                             int[][] srcData,
                             int dstScanlineStride,
                             int dstPixelStride,
                             int[] dstBandOffsets,
                             int[][] dstData) {
        for (int band= 0; band < numBands; band++)  {
            int[] src = srcData[band];
            int[] dst = dstData[band];
            int pixelValue;
            int srcLineOffset = srcBandOffsets[band];
            int dstLineOffset = dstBandOffsets[band];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                for (int w = 0; w < dstWidth; w++) {
                    pixelValue = src[srcPixelOffset];

                    if ( (pixelValue != Integer.MIN_VALUE) &&
                         (pixelValue & Integer.MIN_VALUE) != 0 ) {
                        // It is not 0x80000000  and its sign bit is set.
                        dst[dstPixelOffset] = -src[srcPixelOffset];
                    } else {
                        // It is either the minimum of int, i.e. 0x80000000,
                        // or a positive number;
                        dst[dstPixelOffset] = src[srcPixelOffset];
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcScanlineStride;
                dstLineOffset += dstScanlineStride;
            }
        }
    }

    private void floatAbsolute(int numBands,
                               int dstWidth,
                               int dstHeight,
                               int srcScanlineStride,
                               int srcPixelStride,
                               int[] srcBandOffsets,
                               float[][] srcData,
                               int dstScanlineStride,
                               int dstPixelStride,
                               int[] dstBandOffsets,
                               float[][] dstData) {
        for (int band= 0; band < numBands; band++)  {
            float[] src = srcData[band];
            float[] dst = dstData[band];
            int srcLineOffset = srcBandOffsets[band];
            int dstLineOffset = dstBandOffsets[band];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

		// as per Math.abs()
                for (int w = 0; w < dstWidth; w++) {
                    if ( src[srcPixelOffset] <= 0.0F ) {
                        dst[dstPixelOffset] = 0.0F - src[srcPixelOffset];
                    } else {
                        dst[dstPixelOffset] = src[srcPixelOffset];
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcScanlineStride;
                dstLineOffset += dstScanlineStride;
            }
        }
    }

    private void doubleAbsolute(int numBands,
                                int dstWidth,
                                int dstHeight,
                                int srcScanlineStride,
                                int srcPixelStride,
                                int[] srcBandOffsets,
                                double[][] srcData,
                                int dstScanlineStride,
                                int dstPixelStride,
                                int[] dstBandOffsets,
                                double[][] dstData) {
        for (int band= 0; band < numBands; band++)  {
            double[] src = srcData[band];
            double[] dst = dstData[band];
            int srcLineOffset = srcBandOffsets[band];
            int dstLineOffset = dstBandOffsets[band];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                // as per Math.abs()
                for (int w = 0; w < dstWidth; w++) {
                    if ( src[srcPixelOffset] <= 0.0D ) {
                        dst[dstPixelOffset] = 0.0D - src[srcPixelOffset];
                    } else {
                        dst[dstPixelOffset] = src[srcPixelOffset];
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }

                srcLineOffset += srcScanlineStride;
                dstLineOffset += dstScanlineStride;
            }
        }
    }

//     public static void main(String args[]) {
//         System.out.println( "AbsoluteOpImage Test");
//         ImageLayout layout;
//         OpImage src, dst;
//         Rectangle rect = new Rectangle(0, 0, 5, 5);

//         System.out.println("1. PixelInterleaved byte 3-band");
//         layout = OpImageTester.createImageLayout(0, 0, 800, 800, 0, 0, 200, 200,
//                                              DataBuffer.TYPE_BYTE, 3, false);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new AbsoluteOpImage(src, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("2. Banded byte 3-band");
//         layout = OpImageTester.createImageLayout(0, 0, 800, 800, 0, 0, 200, 200,
//                                              DataBuffer.TYPE_BYTE, 3, true);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new AbsoluteOpImage(src, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("3. PixelInterleaved int 3-band");
//         layout = OpImageTester.createImageLayout(0, 0, 512, 512, 0, 0, 200, 200,
//                                              DataBuffer.TYPE_INT, 3, false);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new AbsoluteOpImage(src, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("4. Banded int 3-band");
//         layout = OpImageTester.createImageLayout(0, 0, 512, 512, 0, 0, 200, 200,
//                                              DataBuffer.TYPE_INT, 3, true);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new AbsoluteOpImage(src, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("5. PixelInterleaved float 3-band");
//         layout = OpImageTester.createImageLayout(0, 0, 512, 512, 0, 0, 200, 200,
//                                              DataBuffer.TYPE_FLOAT, 3, false);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new AbsoluteOpImage(src, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("6. Banded float 3-band");
//         layout = OpImageTester.createImageLayout(0, 0, 512, 512, 0, 0, 200, 200,
//                                              DataBuffer.TYPE_FLOAT, 3, true);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new AbsoluteOpImage(src, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("7. PixelInterleaved double 3-band");
//         layout = OpImageTester.createImageLayout(0, 0, 512, 512, 0, 0, 200, 200,
//                                              DataBuffer.TYPE_DOUBLE, 3, false);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new AbsoluteOpImage(src, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);

//         System.out.println("8. Banded double 3-band");
//         layout = OpImageTester.createImageLayout(0, 0, 512, 512, 0, 0, 200, 200,
//                                              DataBuffer.TYPE_DOUBLE, 3, true);
//         src = OpImageTester.createRandomOpImage(layout);
//         dst = new AbsoluteOpImage(src, null, null);
//         OpImageTester.testOpImage(dst, rect);
//         OpImageTester.timeOpImage(dst, 10);
//     }
}
