/*
 * $RCSfile: SubtractFromConstOpImage.java,v $
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
import com.lightcrafts.media.jai.util.ImageUtil;
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
 * An <code>OpImage</code> implementing the "SubtractFromConst" operation.
 *
 * <p>This <code>OpImage</code> subtracts the pixels of a rendered
 * image from a set of constants, one for each band of the source image.
 * The destination pixel values are calculated as:
 * <pre>
 *     for (int h = 0; h < dstHeight; h++) {
 *         for (int w = 0; w < dstWidth; w++) {
 *             for (int b = 0; b < dstNumBands; b++) {
 *                 if (constants.length < dstNumBands) {
 *                     dst[h][w][b] = constants[0] - srcs[h][w][b];
 *                 } else {
 *                     dst[h][w][b] = constants[b] - srcs[h][w][b];
 *                 }
 *             }
 *         }
 *     }
 * </pre>
 *
 * @see com.lightcrafts.mediax.jai.operator.SubtractFromConstDescriptor
 * @see SubtractFromConstCRIF
 *
 *
 * @since EA2
 */
final class SubtractFromConstOpImage extends ColormapOpImage {

    /** The constants to be subtracted from, one for each band. */
    protected double constants[];
    private byte[][] byteTable = null;

    private synchronized void initByteTable() {

	if (byteTable != null)
	    return;

        int nbands = constants.length;

        byteTable = new byte[nbands][256];

        // Initialize table which implements SubtractFromConst and clamp
        for(int band=0; band<nbands; band++) {
            int k = ImageUtil.clampRoundInt(constants[band]);
            byte[] t = byteTable[band];
            for (int i = 0; i < 256; i++) {
                int diff = k - i;
                if (diff < 0) {
                    t[i] = (byte)0;
                } else if (diff > 255) {
                    t[i] = (byte)255;
                } else {
                    t[i] = (byte)diff;
                }
            }
        }
    }

    /**
     * Constructor.
     *
     * @param source     The source image.
     * @param layout     The destination image layout.
     * @param constants  The constants to be subtracted from.
     */
    public SubtractFromConstOpImage(RenderedImage source,
                                    Map config,
                                    ImageLayout layout,
                                    double[] constants) {
        super(source, layout, config, true);

        int numBands = getSampleModel().getNumBands();

        if (constants.length < numBands) {
            this.constants = new double[numBands];
            for (int i = 0; i < numBands; i++) {
                this.constants[i] = constants[0];
            }
        } else {
            this.constants = (double[])constants.clone();
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
	initByteTable();

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
     * Subtracts the pixel values within a specified rectangle from a constant.
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

        RasterAccessor dst = new RasterAccessor(dest, destRect,  
                                                formatTags[1], getColorModel());
        RasterAccessor src = new RasterAccessor(sources[0], srcRect,  
                                                formatTags[0], 
                                                getSource(0).getColorModel());

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

        if (dst.needsClamping()) {
            /* Further clamp down to underlying raster data type. */
            dst.clampDataArrays();
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
            int c = ImageUtil.clampRoundInt(constants[b]);
            byte[] d = dstData[b];
            byte[] s = srcData[b];
            byte[] clamp = byteTable[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                int dstEnd = dstPixelOffset + dstWidth*dstPixelStride;
                while (dstPixelOffset < dstEnd) {
                    d[dstPixelOffset] = clamp[s[srcPixelOffset] & 0xFF];

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
            int c = ImageUtil.clampRoundInt(constants[b]);
            short[] d = dstData[b];
            short[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                int dstEnd = dstPixelOffset + dstWidth*dstPixelStride;
                while (dstPixelOffset < dstEnd) {
                    d[dstPixelOffset] = ImageUtil.clampUShort(
                                        c - (s[srcPixelOffset] & 0xFFFF));

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
            int c = ImageUtil.clampRoundInt(constants[b]);
            short[] d = dstData[b];
            short[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                int dstEnd = dstPixelOffset + dstWidth*dstPixelStride;
                while (dstPixelOffset < dstEnd) {
                    d[dstPixelOffset] = ImageUtil.clampShort(c - s[srcPixelOffset]);

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
            long c = ImageUtil.clampRoundInt(constants[b]);
            int[] d = dstData[b];
            int[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                int dstEnd = dstPixelOffset + dstWidth*dstPixelStride;
                while (dstPixelOffset < dstEnd) {
                    d[dstPixelOffset] = ImageUtil.clampInt(c - s[srcPixelOffset]);

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
            double c = constants[b];
            float[] d = dstData[b];
            float[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                int dstEnd = dstPixelOffset + dstWidth*dstPixelStride;
                while (dstPixelOffset < dstEnd) {
                    d[dstPixelOffset] =
                        ImageUtil.clampFloat(c - s[srcPixelOffset]);

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
            double c = constants[b];
            double[] d = dstData[b];
            double[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                int dstEnd = dstPixelOffset + dstWidth*dstPixelStride;
                while (dstPixelOffset < dstEnd) {
                    d[dstPixelOffset] = c - s[srcPixelOffset];

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }
}
