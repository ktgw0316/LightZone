/*
 * $RCSfile: MinOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:36 $
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
import java.lang.ref.SoftReference;

/**
 * An <code>OpImage</code> implementing the "Min" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.MinDescriptor</code>.
 *
 * <p>This <code>OpImage</code> chooses the minimum pixel values of the
 * two source images on a per-band basis. In case the two source images
 * have different number of bands, the number of bands for the destination
 * image is the smaller band number of the two source images. That is
 * <code>dstNumBands = Math.min(src1NumBands, src2NumBands)</code>.
 * In case the two source images have different data types, the data type
 * for the destination image is the higher data type of the two source
 * images.
 *
 * <p>The value of the pixel (x, y) in the destination image is defined as:
 * <pre>
 * for (b = 0; b < numBands; b++) {
 *     dst[y][x][b] = Math.min(src1[y][x][b], src2[y][x][b]);
 * }
 * </pre>
 *
 * @see com.lightcrafts.mediax.jai.operator.MinDescriptor
 * @see MinRIF
 *
 */
final class MinOpImage extends PointOpImage {

    private static long negativeZeroFloatBits  = Float.floatToIntBits(-0.0f);
    private static long negativeZeroDoubleBits = Double.doubleToLongBits(-0.0d);
    private static byte[] byteTable = null;
    private static SoftReference softRef = null;

    private synchronized void allocByteTable() {
        if ((softRef == null) || (softRef.get() == null)) {

            //
            // First time or reference has been cleared.
            // Create  the table and make a soft reference to it.
            //
            byteTable = new byte[256*256];
            softRef = new SoftReference(byteTable);

            // Initialize table which implements Min
            int idx = 0;
            for (int i1 = 0; i1 < 256; i1++) {
                int base = i1 << 8;
                for(int i2=0; i2<i1; i2++) {
                    byteTable[base+i2] = (byte)i2;
                }
                for (int i2=i1; i2<256; i2++) {
                    byteTable[base+i2] = (byte)i1;
                }
            }
        }
    }

    /**
     * Construct a <code>MinOpImage</code>.
     *
     * @param source1  The first source image.
     * @param source2  The second source image.
     * @param layout   The destination image layout.
     */
    public MinOpImage(RenderedImage source1,
                      RenderedImage source2,
                      Map config,
                      ImageLayout layout) {
        super(source1, source2, layout, config, true);

        if (sampleModel.getTransferType() == DataBuffer.TYPE_BYTE) {
            allocByteTable();
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Return the minimum pixel value of the source images for a
     * specified rectangle.
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

        /* For PointOpImage, srcRect = destRect. */
        RasterAccessor s1 = new RasterAccessor(sources[0], destRect,  
                                               formatTags[0], 
                                               getSource(0).getColorModel());
        RasterAccessor s2 = new RasterAccessor(sources[1], destRect, 
                                               formatTags[1], 
                                               getSource(1).getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect,  
                                              formatTags[2], getColorModel());

        switch (d.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(s1, s2, d);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(s1, s2, d);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(s1, s2, d);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(s1, s2, d);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(s1, s2, d);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(s1, s2, d);
            break;
        }

        if (d.isDataCopy()) {
            d.clampDataArrays();
            d.copyDataToRaster();
        }
    }

    private void computeRectByte(RasterAccessor src1,
                                 RasterAccessor src2,
                                 RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        byte[][] s1Data = src1.getByteDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        byte[][] s2Data = src2.getByteDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        byte[][] dData = dst.getByteDataArrays();

        for (int b = 0; b < bands; b++) {
            byte[] s1 = s1Data[b];
            byte[] s2 = s2Data[b];
            byte[] d = dData[b];

            int s1LineOffset = s1BandOffsets[b];
            int s2LineOffset = s2BandOffsets[b];
            int dLineOffset = dBandOffsets[b];

            for (int h = 0; h < dheight; h++) {
                int s1PixelOffset = s1LineOffset;
                int s2PixelOffset = s2LineOffset;
                int dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                dLineOffset += dLineStride;

                int dstEnd = dPixelOffset + dwidth*dPixelStride;
                while (dPixelOffset < dstEnd) {
                    int i1 = s1[s1PixelOffset]&0xFF;
                    int i2 = s2[s2PixelOffset]&0xFF;
                    d[dPixelOffset] = byteTable[(i1<<8) + i2];

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }
        }
    }

    private void computeRectUShort(RasterAccessor src1,
                                   RasterAccessor src2,
                                   RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        short[][] s1Data = src1.getShortDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        short[][] s2Data = src2.getShortDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        short[][] dData = dst.getShortDataArrays();

        for (int b = 0; b < bands; b++) {
            short[] s1 = s1Data[b];
            short[] s2 = s2Data[b];
            short[] d = dData[b];

            int s1LineOffset = s1BandOffsets[b];
            int s2LineOffset = s2BandOffsets[b];
            int dLineOffset = dBandOffsets[b];

            for (int h = 0; h < dheight; h++) {
                int s1PixelOffset = s1LineOffset;
                int s2PixelOffset = s2LineOffset;
                int dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    d[dPixelOffset] = minUShort(s1[s1PixelOffset],
                                                s2[s2PixelOffset]);

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }
        }
    }

    private void computeRectShort(RasterAccessor src1,
                                  RasterAccessor src2,
                                  RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        short[][] s1Data = src1.getShortDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        short[][] s2Data = src2.getShortDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        short[][] dData = dst.getShortDataArrays();

        for (int b = 0; b < bands; b++) {
            short[] s1 = s1Data[b];
            short[] s2 = s2Data[b];
            short[] d = dData[b];

            int s1LineOffset = s1BandOffsets[b];
            int s2LineOffset = s2BandOffsets[b];
            int dLineOffset = dBandOffsets[b];

            for (int h = 0; h < dheight; h++) {
                int s1PixelOffset = s1LineOffset;
                int s2PixelOffset = s2LineOffset;
                int dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    d[dPixelOffset] = minShort(s1[s1PixelOffset],
                                               s2[s2PixelOffset]);

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }
        }
    }

    private void computeRectInt(RasterAccessor src1,
                                RasterAccessor src2,
                                RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        int[][] s1Data = src1.getIntDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        int[][] s2Data = src2.getIntDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        int[][] dData = dst.getIntDataArrays();

        for (int b = 0; b < bands; b++) {
            int[] s1 = s1Data[b];
            int[] s2 = s2Data[b];
            int[] d = dData[b];

            int s1LineOffset = s1BandOffsets[b];
            int s2LineOffset = s2BandOffsets[b];
            int dLineOffset = dBandOffsets[b];

            for (int h = 0; h < dheight; h++) {
                int s1PixelOffset = s1LineOffset;
                int s2PixelOffset = s2LineOffset;
                int dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    d[dPixelOffset] = minInt(s1[s1PixelOffset],
                                             s2[s2PixelOffset]);

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }
        }
    }

    private void computeRectFloat(RasterAccessor src1,
                                  RasterAccessor src2,
                                  RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        float[][] s1Data = src1.getFloatDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        float[][] s2Data = src2.getFloatDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        float[][] dData = dst.getFloatDataArrays();

        for (int b = 0; b < bands; b++) {
            float[] s1 = s1Data[b];
            float[] s2 = s2Data[b];
            float[] d = dData[b];

            int s1LineOffset = s1BandOffsets[b];
            int s2LineOffset = s2BandOffsets[b];
            int dLineOffset = dBandOffsets[b];

            for (int h = 0; h < dheight; h++) {
                int s1PixelOffset = s1LineOffset;
                int s2PixelOffset = s2LineOffset;
                int dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    d[dPixelOffset] = minFloat(s1[s1PixelOffset],
                                               s2[s2PixelOffset]);

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }
        }
    }

    private void computeRectDouble(RasterAccessor src1,
                                   RasterAccessor src2,
                                   RasterAccessor dst) {
        int s1LineStride = src1.getScanlineStride();
        int s1PixelStride = src1.getPixelStride();
        int[] s1BandOffsets = src1.getBandOffsets();
        double[][] s1Data = src1.getDoubleDataArrays();

        int s2LineStride = src2.getScanlineStride();
        int s2PixelStride = src2.getPixelStride();
        int[] s2BandOffsets = src2.getBandOffsets();
        double[][] s2Data = src2.getDoubleDataArrays();

        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int bands = dst.getNumBands();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();
        double[][] dData = dst.getDoubleDataArrays();

        for (int b = 0; b < bands; b++) {
            double[] s1 = s1Data[b];
            double[] s2 = s2Data[b];
            double[] d = dData[b];

            int s1LineOffset = s1BandOffsets[b];
            int s2LineOffset = s2BandOffsets[b];
            int dLineOffset = dBandOffsets[b];

            for (int h = 0; h < dheight; h++) {
                int s1PixelOffset = s1LineOffset;
                int s2PixelOffset = s2LineOffset;
                int dPixelOffset = dLineOffset;

                s1LineOffset += s1LineStride;
                s2LineOffset += s2LineStride;
                dLineOffset += dLineStride;

                for (int w = 0; w < dwidth; w++) {
                    d[dPixelOffset] = minDouble(s1[s1PixelOffset],
                                                s2[s2PixelOffset]);

                    s1PixelOffset += s1PixelStride;
                    s2PixelOffset += s2PixelStride;
                    dPixelOffset += dPixelStride;
                }
            }
        }
    }

    private final short minUShort(short a, short b) {
        return (a&0xFFFF) < (b&0xFFFF) ? a : b;
    }

    private final short minShort(short a, short b) {
        return (a <= b) ? a : b;
    }

    private final int minInt(int a, int b) {
        return (a <= b) ? a : b;
    }

    public static float minFloat(float a, float b) {
        if (a != a) return a;   // a is NaN
        if ((a == 0.0f) && (b == 0.0f)
            && (Float.floatToIntBits(b) == negativeZeroFloatBits)) {
            return b;
        }
        return (a <= b) ? a : b;
    }

    public static double minDouble(double a, double b) {
        if (a != a) return a;   // a is NaN
        if ((a == 0.0d) && (b == 0.0d)
            && (Double.doubleToLongBits(b) == negativeZeroDoubleBits)) {
            return b;
        }
        return (a <= b) ? a : b;
    }
}
