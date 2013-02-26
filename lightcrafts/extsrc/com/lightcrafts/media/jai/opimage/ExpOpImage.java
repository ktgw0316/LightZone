/*
 * $RCSfile: ExpOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:25 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;

import com.lightcrafts.mediax.jai.ColormapOpImage;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import java.util.Map;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An <code>OpImage</code> implementing the "Exp" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.ExpDescriptor</code>.
 *
 * The result is rounded to the closest integer for intergral data types.
 * <p> This <code>OpImage</code> takes the natural exponential of the pixel
 * values of an image.  The operation is done on a per-pixel, per-band
 * basis.
 *
 * @see com.lightcrafts.mediax.jai.operator.ExpDescriptor
 * @see ExpCRIF
 *
 * @since EA2
 *
 */
final class ExpOpImage extends ColormapOpImage {

    /** A lookup table for byte data type. */
    private byte[] byteTable = null;

    /**
     * The largest unsigned short to get a non-overflowed exponential result.
     * i.e. cloeset to 65536.
     * exp(11) = 59874.14171, exp(12) = 162754.7914
     */
    private static int USHORT_UP_BOUND = 11;

    /**
     * The largest short to get a non-overflowed exponential result.
     * i.e. closest to 32767.
     * exp(10) = 22026.46579, exp(11) = 59874.14171
     */
    private static int SHORT_UP_BOUND = 10;

    /**
     * The largest int to get a non-overflown exponential result.
     * i.e. cloeset to 2**31-1 = 2147483647.
     * exp(21) = 1318815734, exp(22) = 3584912846.
     */
    private static int INT_UP_BOUND = 21;

    /**
     * The smallest integer to get a non-zero exponential result is
     * 0. i.e. exp(0) = 1; exp(-1) = 0.367879441, which will be stored as 0. 
     * all other negative values will result in 0.
     */
    private static int LOW_BOUND = 0;

    /**
     * Constructor.
     *
     * <p> The layout of the source is used as the fall-back for
     * the layout of the destination.  Any layout parameters not
     * specified in the <code>layout</code> argument are set to
     * the same value as that of the source.
     *
     * @param source  The source image.

     * @param layout  The destination image layout.
     */
    public ExpOpImage(RenderedImage source,
                      Map config,
                      ImageLayout layout) {
        super(source, layout, config, true);

        /* Set flag to permit in-place operation. */
        permitInPlaceOperation();

        // Initialize the colormap if necessary.
        initializeColormapOperation();
    }

    /**
     * Transform the colormap according to the rescaling parameters.
     */
    protected void transformColormap(byte[][] colormap) {
	initByteTable();

        for(int b = 0; b < 3; b++) {
            byte[] map = colormap[b];
            int mapSize = map.length;

            for(int i = 0; i < mapSize; i++) {
                map[i] = byteTable[(map[i] & 0xFF)];
            }
        }
    }

    /**
     * Map the pixels inside a specified rectangle whose value is within a 
     * range to a constant on a per-band basis.
     *
     * @param sources   Cobbled sources, guaranteed to provide all the
     *                  source data necessary for computing the rectangle.
     * @param dest      The tile containing the rectangle to be computed.
     * @param destRect  The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        /* Retrieve format tags. */
        RasterFormatTag[] formatTags = getFormatTags();

        /* No need to mapSourceRect for PointOps. */
        RasterAccessor s = new RasterAccessor(sources[0],
                                              destRect,
                                              formatTags[0],
                                              getSourceImage(0).getColorModel());
        RasterAccessor d = new RasterAccessor(dest,
                                              destRect,
                                              formatTags[1],
                                              getColorModel());

        switch (d.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(s, d);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(s, d);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(s, d);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(s, d);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(s, d);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(s, d);
            break;
        }

        if (d.needsClamping()) {
            d.clampDataArrays();
        }
        d.copyDataToRaster();
    }

    private void computeRectByte(RasterAccessor src,
                                 RasterAccessor dst) {
	initByteTable();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        byte[][] srcData = src.getByteDataArrays();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        byte[][] dstData = dst.getByteDataArrays();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        for (int b = 0; b < dstBands; b++) {
            byte[] s = srcData[b];
            byte[] d = dstData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = byteTable[s[srcPixelOffset] & ImageUtil.BYTE_MASK];

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private void computeRectUShort(RasterAccessor src,
                                   RasterAccessor dst) {

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        short max = (short)ImageUtil.USHORT_MASK;

        for (int b = 0; b < dstBands; b++) {
            short[] s = srcData[b];
            short[] d = dstData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    double p = s[srcPixelOffset] & ImageUtil.USHORT_MASK;
                    if (p == 0) {
                        d[dstPixelOffset] = 1;
                    } else if (p > USHORT_UP_BOUND) {
                        d[dstPixelOffset] = max;
                    } else {
                        d[dstPixelOffset] = (short)(Math.exp(p) + 0.5);
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private void computeRectShort(RasterAccessor src,
                                  RasterAccessor dst) {

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        for (int b = 0; b < dstBands; b++) {
            short[] s = srcData[b];
            short[] d = dstData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    double p = s[srcPixelOffset];
                    if (p < LOW_BOUND) {
                        d[dstPixelOffset] = 0;
                    } else if (p == 0) {
                        d[dstPixelOffset] = 1;
                    } else if (p > SHORT_UP_BOUND) {
                        d[dstPixelOffset] = Short.MAX_VALUE;
                    } else {
                        d[dstPixelOffset] = (short)(Math.exp(p) + 0.5);
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private void computeRectInt(RasterAccessor src,
                                RasterAccessor dst) {

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        int[][] srcData = src.getIntDataArrays();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        int[][] dstData = dst.getIntDataArrays();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        for (int b = 0; b < dstBands; b++) {
            int[] s = srcData[b];
            int[] d = dstData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    double p = s[srcPixelOffset];
                    if (p < LOW_BOUND) {
                        d[dstPixelOffset] = 0;
                    } else if (p == 0) {
                        d[dstPixelOffset] = 1;
                    } else if (p > INT_UP_BOUND) {
                        d[dstPixelOffset] = Integer.MAX_VALUE;
                    } else {
                        d[dstPixelOffset] = (int)(Math.exp(p) + 0.5);
                    }

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private void computeRectFloat(RasterAccessor src,
                                  RasterAccessor dst) {

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        float[][] srcData = src.getFloatDataArrays();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        float[][] dstData = dst.getFloatDataArrays();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        for (int b = 0; b < dstBands; b++) {
            float[] s = srcData[b];
            float[] d = dstData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = (float)Math.exp(s[srcPixelOffset]);

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private void computeRectDouble(RasterAccessor src,
                                   RasterAccessor dst) {

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        double[][] srcData = src.getDoubleDataArrays();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        double[][] dstData = dst.getDoubleDataArrays();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        for (int b = 0; b < dstBands; b++) {
            double[] s = srcData[b];
            double[] d = dstData[b];

            int srcLineOffset = srcBandOffsets[b];
            int dstLineOffset = dstBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int srcPixelOffset = srcLineOffset;
                int dstPixelOffset = dstLineOffset;

                srcLineOffset += srcLineStride;
                dstLineOffset += dstLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = Math.exp(s[srcPixelOffset]);

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
            }
        }
    }

    private synchronized void initByteTable() {

        if (byteTable != null)
	    return;

        byteTable = new byte[0x100];

        /*
         * exp(5) = 148.4131591...
         * exp(6) = 403.4287935...
         * Calculate up to 5 and set the rest to the maximum value.
         */
        byteTable[0] = 1;

        for (int i = 1; i < 6; i++) {
            byteTable[i] = (byte)(Math.exp(i) + 0.5);
        }

        for (int i = 6; i < 0x100; i++) {
            byteTable[i] = (byte)ImageUtil.BYTE_MASK;
        }
    }
}
