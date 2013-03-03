/*
 * $RCSfile: ThresholdOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:45 $
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

/**
 * An <code>OpImage</code> implementing the "Threshold" operation as
 * described in <code>com.lightcrafts.mediax.jai.operator.ThresholdDescriptor</code>.
 *
 * <p>This <code>OpImage</code> maps all the pixels of an image
 * whose value falls within a given range to a constant on a per-band basis.
 * Each of the lower bound, upper bound, and constant arrays may have only
 * one value in it. If that is the case, that value is used for all bands.
 *
 * @see com.lightcrafts.mediax.jai.operator.ThresholdDescriptor
 * @see ThresholdCRIF
 *
 *
 * @since EA2
 */
final class ThresholdOpImage extends ColormapOpImage {

    /** The lower bound, one for each band. */
    private double[] low;

    /** The upper bound, one for each band. */
    private double[] high;

    /** The constants to be mapped, one for each band. */
    private double[] constants;

    /** Lookup table for byte data */
    private byte[][] byteTable = null;

    /**
     * Constructor.
     *
     * @param source     The source image.
     * @param layout     The destination image layout.
     * @param low        The lower bound of the threshold.
     * @param high       The upper bound of the threshold.
     * @param constants  The constants to be mapped within the threshold.
     */
    public ThresholdOpImage(RenderedImage source,
                            Map config,
                            ImageLayout layout,
                            double[] low,
                            double[] high,
                            double[] constants) {
        super(source, layout, config, true);

        int numBands = getSampleModel().getNumBands();
        this.low = new double[numBands];
        this.high = new double[numBands];
        this.constants = new double[numBands];

        for (int i = 0; i < numBands; i++) {
            if (low.length < numBands) {
                this.low[i] = low[0];
            } else {
                this.low[i] = low[i];
            }
            if (high.length < numBands) {
                this.high[i] = high[0];
            } else {
                this.high[i] = high[i];
            }
            if (constants.length < numBands) {
                this.constants[i] = constants[0];
            } else {
                this.constants[i] = constants[i];
            }
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();

        // Initialize the colormap if necessary.
        initializeColormapOperation();
    }

    /**
     * Transform the colormap according to the rescaling parameters.
     */
    protected void transformColormap(byte[][] colormap) {
	initByteTable();	// only create lookup table when necessary

        for(int b = 0; b < 3; b++) {
            byte[] map = colormap[b];
	    byte[] luTable = byteTable[b >= byteTable.length ? 0 : b];
            int mapSize = map.length;

            for(int i = 0; i < mapSize; i++) {
                map[i] = luTable[(map[i] & 0xFF)];
            }
        }
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

        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor src = new RasterAccessor(sources[0], srcRect,
                                                formatTags[0],
                                                getSource(0).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect,
                                                formatTags[1], getColorModel());

        int srcPixelStride = src.getPixelStride();
        int srcLineStride = src.getScanlineStride();
        int[] srcBandOffsets = src.getBandOffsets();

        int dstPixelStride = dst.getPixelStride();
        int dstLineStride = dst.getScanlineStride();
        int[] dstBandOffsets = dst.getBandOffsets();

        int width = dst.getWidth() * dstPixelStride;
        int height = dst.getHeight() * dstLineStride;
        int bands = dst.getNumBands();

        switch (dst.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            byteLoop(width, height, bands,
                     srcPixelStride, srcLineStride, srcBandOffsets,
                     src.getByteDataArrays(),
                     dstPixelStride, dstLineStride, dstBandOffsets,
                     dst.getByteDataArrays());
            break;

        case DataBuffer.TYPE_SHORT:
            shortLoop(width, height, bands,
                      srcPixelStride, srcLineStride, srcBandOffsets,
                      src.getShortDataArrays(),
                      dstPixelStride, dstLineStride, dstBandOffsets,
                      dst.getShortDataArrays());
            break;

        case DataBuffer.TYPE_USHORT:
            ushortLoop(width, height, bands,
                       srcPixelStride, srcLineStride, srcBandOffsets,
                       src.getShortDataArrays(),
                       dstPixelStride, dstLineStride, dstBandOffsets,
                       dst.getShortDataArrays());
            break;

        case DataBuffer.TYPE_INT:
            intLoop(width, height, bands,
                    srcPixelStride, srcLineStride, srcBandOffsets,
                    src.getIntDataArrays(),
                    dstPixelStride, dstLineStride, dstBandOffsets,
                    dst.getIntDataArrays());
            break;

        case DataBuffer.TYPE_FLOAT:
            floatLoop(width, height, bands,
                      srcPixelStride, srcLineStride, srcBandOffsets,
                      src.getFloatDataArrays(),
                      dstPixelStride, dstLineStride, dstBandOffsets,
                      dst.getFloatDataArrays());
            break;

        case DataBuffer.TYPE_DOUBLE:
            doubleLoop(width, height, bands,
                       srcPixelStride, srcLineStride, srcBandOffsets,
                       src.getDoubleDataArrays(),
                       dstPixelStride, dstLineStride, dstBandOffsets,
                       dst.getDoubleDataArrays());
            break;
        }

        if (dst.isDataCopy()) {
            dst.clampDataArrays();
            dst.copyDataToRaster();
        }
    }

    private void byteLoop(int width, int height, int bands,
                          int srcPixelStride, int srcLineStride,
                          int[] srcBandOffsets, byte[][] srcData,
                          int dstPixelStride, int dstLineStride,
                          int[] dstBandOffsets, byte[][] dstData) {

	initByteTable();

        for (int b = 0; b < bands; b++) {
            byte[] s = srcData[b];
            byte[] d = dstData[b];
            byte[] t = byteTable[b];

            int heightEnd = dstBandOffsets[b] + height;

            for (int dstLineOffset = dstBandOffsets[b],
                 srcLineOffset = srcBandOffsets[b];
                 dstLineOffset < heightEnd;
                 dstLineOffset += dstLineStride,
                 srcLineOffset += srcLineStride) {

                int widthEnd = dstLineOffset + width;

                for (int dstPixelOffset = dstLineOffset,
                     srcPixelOffset = srcLineOffset;
                     dstPixelOffset < widthEnd;
                     dstPixelOffset += dstPixelStride,
                     srcPixelOffset += srcPixelStride) {

                    d[dstPixelOffset] = t[s[srcPixelOffset]&0xFF];
                }
            }
        }
    }

    private void shortLoop(int width, int height, int bands,
                           int srcPixelStride, int srcLineStride,
                           int[] srcBandOffsets, short[][] srcData,
                           int dstPixelStride, int dstLineStride,
                           int[] dstBandOffsets, short[][] dstData) {
        for (int b = 0; b < bands; b++) {
            short[] s = srcData[b];
            short[] d = dstData[b];

            double l = low[b];
            double h = high[b];
            short c = (short)constants[b];

            int heightEnd = dstBandOffsets[b] + height;

            for (int dstLineOffset = dstBandOffsets[b],
                 srcLineOffset = srcBandOffsets[b];
                 dstLineOffset < heightEnd;
                 dstLineOffset += dstLineStride,
                 srcLineOffset += srcLineStride) {

                int widthEnd = dstLineOffset + width;

                for (int dstPixelOffset = dstLineOffset,
                     srcPixelOffset = srcLineOffset;
                     dstPixelOffset < widthEnd;
                     dstPixelOffset += dstPixelStride,
                     srcPixelOffset += srcPixelStride) {

                    short p = s[srcPixelOffset];

                    if (p >= l && p <= h) {
                        d[dstPixelOffset] = c;
                    } else {
                        d[dstPixelOffset] = p;
                    }
                }
            }
        }
    }

    private void ushortLoop(int width, int height, int bands,
                            int srcPixelStride, int srcLineStride,
                            int[] srcBandOffsets, short[][] srcData,
                            int dstPixelStride, int dstLineStride,
                            int[] dstBandOffsets, short[][] dstData) {

        for (int b = 0; b < bands; b++) {
            short[] s = srcData[b];
            short[] d = dstData[b];

            double l = low[b];
            double h = high[b];
            short c = (short)constants[b];

            int heightEnd = dstBandOffsets[b] + height;

            for (int dstLineOffset = dstBandOffsets[b],
                 srcLineOffset = srcBandOffsets[b];
                 dstLineOffset < heightEnd;
                 dstLineOffset += dstLineStride,
                 srcLineOffset += srcLineStride) {

                int widthEnd = dstLineOffset + width;

                for (int dstPixelOffset = dstLineOffset,
                     srcPixelOffset = srcLineOffset;
                     dstPixelOffset < widthEnd;
                     dstPixelOffset += dstPixelStride,
                     srcPixelOffset += srcPixelStride) {

                    int p = s[srcPixelOffset] & 0xFFFF;

                    if (p >= l && p <= h) {
                        d[dstPixelOffset] = c;
                    } else {
                        d[dstPixelOffset] = (short)p;
                    }
                }
            }
        }
    }

    private void intLoop(int width, int height, int bands,
                         int srcPixelStride, int srcLineStride,
                         int[] srcBandOffsets, int[][] srcData,
                         int dstPixelStride, int dstLineStride,
                         int[] dstBandOffsets, int[][] dstData) {

        for (int b = 0; b < bands; b++) {
            int[] s = srcData[b];
            int[] d = dstData[b];

            double l = low[b];
            double h = high[b];
            int c = (int)constants[b];

            int heightEnd = dstBandOffsets[b] + height;

            for (int dstLineOffset = dstBandOffsets[b],
                 srcLineOffset = srcBandOffsets[b];
                 dstLineOffset < heightEnd;
                 dstLineOffset += dstLineStride,
                 srcLineOffset += srcLineStride) {

                int widthEnd = dstLineOffset + width;

                for (int dstPixelOffset = dstLineOffset,
                     srcPixelOffset = srcLineOffset;
                     dstPixelOffset < widthEnd;
                     dstPixelOffset += dstPixelStride,
                     srcPixelOffset += srcPixelStride) {

                    int p = s[srcPixelOffset];

                    if (p >= l && p <= h) {
                        d[dstPixelOffset] = c;
                    } else {
                        d[dstPixelOffset] = p;
                    }
                }
            }
        }
    }

    private void floatLoop(int width, int height, int bands,
                           int srcPixelStride, int srcLineStride,
                           int[] srcBandOffsets, float[][] srcData,
                           int dstPixelStride, int dstLineStride,
                           int[] dstBandOffsets, float[][] dstData) {

        for (int b = 0; b < bands; b++) {
            float[] s = srcData[b];
            float[] d = dstData[b];

            double l = low[b];
            double h = high[b];
            float c = (float)constants[b];

            int heightEnd = dstBandOffsets[b] + height;

            for (int dstLineOffset = dstBandOffsets[b],
                 srcLineOffset = srcBandOffsets[b];
                 dstLineOffset < heightEnd;
                 dstLineOffset += dstLineStride,
                 srcLineOffset += srcLineStride) {

                int widthEnd = dstLineOffset + width;

                for (int dstPixelOffset = dstLineOffset,
                     srcPixelOffset = srcLineOffset;
                     dstPixelOffset < widthEnd;
                     dstPixelOffset += dstPixelStride,
                     srcPixelOffset += srcPixelStride) {

                    float p = s[srcPixelOffset];

                    if (p >= l && p <= h) {
                        d[dstPixelOffset] = c;
                    } else {
                        d[dstPixelOffset] = p;
                    }
                }
            }
        }
    }

    private void doubleLoop(int width, int height, int bands,
                            int srcPixelStride, int srcLineStride,
                            int[] srcBandOffsets, double[][] srcData,
                            int dstPixelStride, int dstLineStride,
                            int[] dstBandOffsets, double[][] dstData) {

        for (int b = 0; b < bands; b++) {
            double[] s = srcData[b];
            double[] d = dstData[b];

            double l = low[b];
            double h = high[b];
            double c = constants[b];

            int heightEnd = dstBandOffsets[b] + height;

            for (int dstLineOffset = dstBandOffsets[b],
                 srcLineOffset = srcBandOffsets[b];
                 dstLineOffset < heightEnd;
                 dstLineOffset += dstLineStride,
                 srcLineOffset += srcLineStride) {

                int widthEnd = dstLineOffset + width;

                for (int dstPixelOffset = dstLineOffset,
                     srcPixelOffset = srcLineOffset;
                     dstPixelOffset < widthEnd;
                     dstPixelOffset += dstPixelStride,
                     srcPixelOffset += srcPixelStride) {

                    double p = s[srcPixelOffset];

                    if (p >= l && p <= h) {
                        d[dstPixelOffset] = c;
                    } else {
                        d[dstPixelOffset] = p;
                    }
                }
            }
        }
    }

    private synchronized void initByteTable() {

	if (byteTable != null)
	    return;

	/* Build a ramp lookup table for byte datatype. */
	int numBands = getSampleModel().getNumBands();
	byteTable = new byte[numBands][0x100];

	for (int b = 0; b < numBands; b++) {
	    double l  = low[b];
	    double h = high[b];
	    byte c  = (byte)constants[b];

	    byte[] t = byteTable[b];

	    for (int i = 0; i < 0x100; i++) {
		if (i >= l && i <= h) {
		    t[i] = c;
		} else {
		    t[i] = (byte)i;
		}
	    }
	}
    }
}
