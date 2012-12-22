/*
 * $RCSfile: ClampOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:16 $
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
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An <code>OpImage</code> implementing the "Clamp" operation.
 *
 * <p>This <code>OpImage</code> maps all the pixel values of an image
 * that are less than a lower bound to that lower bound value, and all that
 * are greater than a upper bound to that upper bound value. All pixel values
 * fall within these boundaries remain unchanged. The mapping is done on a 
 * per-band basis.
 *
 * <p>Each of the lower bound and upper bound arrays may have only
 * one value in it. If that is the case, that value is applied to all bands.
 * The number of elements in each array has to be consistent, i.e. either
 * all arrays contain one element in them or all they have the same number
 * of elements that matches the number of bands of the source image.
 *
 * @see com.lightcrafts.mediax.jai.operator.ClampDescriptor
 * @see ClampCRIF
 *
 *
 * @since EA2
 */
final class ClampOpImage extends PointOpImage {

    /** Lookup table for byte data */
    private byte[][] byteTable = null;

    /** The lower bound, one for each band. */
    private final double[] low;

    /** The upper bound, one for each band. */
    private final double[] high;

    private synchronized void initByteTable() {

	if (byteTable == null) {
	    /* Initialize byteTable. */
	    int numBands = getSampleModel().getNumBands();
	    byteTable = new byte[numBands][0x100];
	    for (int b = 0; b < numBands; b++) {
		byte[] t = byteTable[b];
		int l = (int)low[b];
		int h = (int)high[b];

		byte bl = (byte)l;
		byte bh = (byte)h;

		for (int i = 0; i < 0x100; i++) {
		    if (i < l) {
			t[i] = bl;
		    } else if (i > h) {
			t[i] = bh;
		    } else {
			t[i] = (byte)i;
		    }
		}
	    }
	}
    }

    /**
     * Constructor.
     *
     * @param source  The source image.
     * @param layout  The destination image layout.
     * @param low     The lower bound of the clamp.
     * @param high    The upper bound of the clamp.
     */
    public ClampOpImage(RenderedImage source,
                        Map config,
                        ImageLayout layout,
                        double[] low,
                        double[] high) {
        super(source, layout, config, true);

        int numBands = getSampleModel().getNumBands();

        if (low.length < numBands || high.length < numBands) {
            this.low = new double[numBands];
            this.high = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                this.low[i] = low[0];
                this.high[i] = high[0];
            }
        } else {
            this.low = (double[])low.clone();
            this.high = (double[])high.clone();
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();
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
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor src = new RasterAccessor(sources[0], srcRect,  
                                                formatTags[0], 
                                                getSourceImage(0).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect,  
                                                formatTags[1], getColorModel());

        switch (dst.getDataType()) {
        case DataBuffer.TYPE_BYTE:
            computeRectByte(src, dst);
            break;
        case DataBuffer.TYPE_USHORT:
            computeRectUShort(src, dst);
            break;
        case DataBuffer.TYPE_SHORT:
            computeRectShort(src, dst);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(src, dst);
            break;
        case DataBuffer.TYPE_FLOAT:
            computeRectFloat(src, dst);
            break;
        case DataBuffer.TYPE_DOUBLE:
            computeRectDouble(src, dst);
            break;
        }

        dst.copyDataToRaster();
    }

    private void computeRectByte(RasterAccessor src,
                                 RasterAccessor dst) {
	initByteTable();

        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        byte[][] dstData = dst.getByteDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        byte[][] srcData = src.getByteDataArrays();

        for (int b = 0; b < dstBands; b++) {
            byte[] d = dstData[b];
            byte[] s = srcData[b];
            byte[] t = byteTable[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = t[s[srcPixelOffset] & 
                                           ImageUtil.BYTE_MASK];

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectUShort(RasterAccessor src,
                                   RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        for (int b = 0; b < dstBands; b++) {
            short[] d = dstData[b];
            short[] s = srcData[b];
            int lo = (int)low[b];
            int hi = (int)high[b];

            short slo = (short)lo;
            short shi = (short)hi;

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    int p = s[srcPixelOffset] & ImageUtil.USHORT_MASK;
                    if (p < lo) {
                        d[dstPixelOffset] = slo;
                    } else if (p > hi) {
                        d[dstPixelOffset] = shi;
                    } else {
                        d[dstPixelOffset] = (short)p;
                    }

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectShort(RasterAccessor src,
                                  RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[][] dstData = dst.getShortDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[][] srcData = src.getShortDataArrays();

        for (int b = 0; b < dstBands; b++) {
            short[] d = dstData[b];
            short[] s = srcData[b];
            int lo = (int)low[b];
            int hi = (int)high[b];

            short slo = (short)lo;
            short shi = (short)hi;

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    short p = s[srcPixelOffset];
                    if (p < lo) {
                        d[dstPixelOffset] = slo;
                    } else if (p > hi) {
                        d[dstPixelOffset] = shi;
                    } else {
                        d[dstPixelOffset] = p;
                    }

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectInt(RasterAccessor src,
                                RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        int[][] dstData = dst.getIntDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        int[][] srcData = src.getIntDataArrays();

        for (int b = 0; b < dstBands; b++) {
            int[] d = dstData[b];
            int[] s = srcData[b];
            double lo = low[b];
            double hi = high[b];

            int ilo = (int)lo;
            int ihi = (int)hi;

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    int p = s[srcPixelOffset];
                    if (p < lo) {
                        d[dstPixelOffset] = ilo;
                    } else if (p > hi) {
                        d[dstPixelOffset] = ihi;
                    } else {
                        d[dstPixelOffset] = p;
                    }

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectFloat(RasterAccessor src,
                                  RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        float[][] dstData = dst.getFloatDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        float[][] srcData = src.getFloatDataArrays();

        for (int b = 0; b < dstBands; b++) {
            float[] d = dstData[b];
            float[] s = srcData[b];
            double lo = low[b];
            double hi = high[b];

            float flo = (float)lo;
            float fhi = (float)hi;

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    float p = s[srcPixelOffset];
                    if (p < lo) {
                        d[dstPixelOffset] = flo;
                    } else if (p > hi) {
                        d[dstPixelOffset] = fhi;
                    } else {
                        d[dstPixelOffset] = p;
                    }

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }

    private void computeRectDouble(RasterAccessor src,
                                   RasterAccessor dst) {
        int dstWidth = dst.getWidth();
        int dstHeight = dst.getHeight();
        int dstBands = dst.getNumBands();

        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        double[][] dstData = dst.getDoubleDataArrays();

        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        double[][] srcData = src.getDoubleDataArrays();

        for (int b = 0; b < dstBands; b++) {
            double[] d = dstData[b];
            double[] s = srcData[b];
            double lo = low[b];
            double hi = high[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    double p = s[srcPixelOffset];
                    if (p < lo) {
                        d[dstPixelOffset] = lo;
                    } else if (p > hi) {
                        d[dstPixelOffset] = hi;
                    } else {
                        d[dstPixelOffset] = p;
                    }

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }
}
