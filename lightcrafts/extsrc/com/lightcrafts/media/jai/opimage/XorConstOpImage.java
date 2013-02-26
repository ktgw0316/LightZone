/*
 * $RCSfile: XorConstOpImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:48 $
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
 * An <code>OpImage</code> implementing the "XorConst" operation.
 *
 * <p>This <code>OpImage</code> logically "xors" the pixels of a rendered
 * image with a set of constants, one for each band of the source image.
 * The destination pixel values are calculated as:
 * <pre>
 *     for (int h = 0; h < dstHeight; h++) {
 *         for (int w = 0; w < dstWidth; w++) {
 *             for (int b = 0; b < dstNumBands; b++) {
 *                 if (constants.length < dstNumBands) {
 *                     dst[h][w][b] = srcs[h][w][b] ^ constants[0];
 *                 } else {
 *                     dst[h][w][b] = srcs[h][w][b] ^ constants[b];
 *                 }
 *             }
 *         }
 *     }
 * </pre>
 *
 * @see com.lightcrafts.mediax.jai.operator.XorConstDescriptor
 * @see XorConstCRIF
 *
 *
 * @since EA2
 */
final class XorConstOpImage extends ColormapOpImage {

    /** The constants to be xored, one for each band. */
    protected int[] constants;

    /**
     * Constructor.
     *
     * @param source     The source image.
     * @param layout     The destination image layout.
     * @param constants  The constants to be xored, stored as reference.
     */
    public XorConstOpImage(RenderedImage source,
			   Map config,
                           ImageLayout layout,
			   int[] constants) {
        super(source, layout, config, true);

        int numBands = getSampleModel().getNumBands();

        if (constants.length < numBands) {
            this.constants = new int[numBands];
            for (int i = 0; i < numBands; i++) {
                this.constants[i] = constants[0];
            }
        } else {
            this.constants = (int[])constants.clone();
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
        for(int b = 0; b < 3; b++) {
            byte[] map = colormap[b];
            int mapSize = map.length;

            int c = b < constants.length ? constants[b] : constants[0];

            for(int i = 0; i < mapSize; i++) {
                map[i] = ImageUtil.clampRoundByte((map[i] & 0xFF) ^ c);
            }
        }
    }

    /**
     * Logically "xors" a constant with the pixel values within a specified
     * rectangle.
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
        case DataBuffer.TYPE_SHORT:
            computeRectShort(src, dst);
            break;
        case DataBuffer.TYPE_INT:
            computeRectInt(src, dst);
            break;
        }

        /* Do not clamp dst data. */
        dst.copyDataToRaster();
    }

    private void computeRectByte(RasterAccessor src,
                                 RasterAccessor dst) {
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
            int c = constants[b];
            byte[] d = dstData[b];
            byte[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = (byte)(s[srcPixelOffset] ^ c);

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
            int c = constants[b];
            short[] d = dstData[b];
            short[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = (short)(s[srcPixelOffset] ^ c);

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
            int c = constants[b];
            int[] d = dstData[b];
            int[] s = srcData[b];

            int dstLineOffset = dstBandOffsets[b];
            int srcLineOffset = srcBandOffsets[b];

            for (int h = 0; h < dstHeight; h++) {
                int dstPixelOffset = dstLineOffset;
                int srcPixelOffset = srcLineOffset;

                dstLineOffset += dstLineStride;
                srcLineOffset += srcLineStride;

                for (int w = 0; w < dstWidth; w++) {
                    d[dstPixelOffset] = s[srcPixelOffset] ^ c;

                    dstPixelOffset += dstPixelStride;
                    srcPixelOffset += srcPixelStride;
                }
            }
        }
    }
    
    public static void main (String args[]) {
        System.out.println("XorConstOpImage Test");
    }
}
