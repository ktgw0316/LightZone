/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2021-     Masahiro Kitagawa */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.jai.utils.OpImageUtil;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: Mar 14, 2007
 * Time: 5:31:50 PM
 */
public class RedMaskBlackener extends PointOpImage {
    private static short[] powTable = new short[0x10000];

    static {
        for (int i = 0; i < 0x10000; i++)
            powTable[i] = (short) (0xffff * Math.pow(i / (double) 0xffff, 1.4D));
    }

    public RedMaskBlackener(RenderedImage source, RenderedImage mask, Map config) {
        super(source, mask, new ImageLayout(source), config, true);
        permitInPlaceOperation();
    }

    @Override
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        var formatTags = new RasterFormatTag[] {
                OpImageUtil.getFormatTag(sources[0]),
                OpImageUtil.getFormatTag(sources[1]),
                OpImageUtil.getFormatTag(dest)
        };

        RasterAccessor src = new RasterAccessor(sources[0], destRect,
                formatTags[0],
                getSourceImage(0).getColorModel());
        RasterAccessor mask = new RasterAccessor(sources[1], destRect,
                formatTags[1],
                getSourceImage(1).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect,
                formatTags[2], getColorModel());

        switch (dst.getDataType()) {
            case DataBuffer.TYPE_USHORT:
                ushortLoop(src, mask, dst);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported data type: " + dst.getDataType());
        }
    }

    protected void ushortLoop(RasterAccessor src, RasterAccessor mask, RasterAccessor dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        short[] dstData = dst.getShortDataArray(0);
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();

        short[] srcData = src.getShortDataArray(0);
        int[] srcBandOffsets = src.getBandOffsets();
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();

        byte[] maskData = mask.getByteDataArray(0);
        int[] maskBandOffsets = mask.getBandOffsets();
        int maskLineStride = mask.getScanlineStride();

        int srcROffset = srcBandOffsets[0];
        int srcGOffset = srcBandOffsets[1];
        int srcBOffset = srcBandOffsets[2];

        int dstROffset = dstBandOffsets[0];
        int dstGOffset = dstBandOffsets[1];
        int dstBOffset = dstBandOffsets[2];

        int maskOffset = maskBandOffsets[0];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                short r = srcData[srcPixelStride * col + row * srcLineStride + srcROffset];
                short g = srcData[srcPixelStride * col + row * srcLineStride + srcGOffset];
                short b = srcData[srcPixelStride * col + row * srcLineStride + srcBOffset];

                int m = 0xff & maskData[col + row * maskLineStride + maskOffset];

                if (m == 0) {
                    dstData[dstPixelStride * col + row * dstLineStride + dstROffset] = r;
                    dstData[dstPixelStride * col + row * dstLineStride + dstGOffset] = g;
                    dstData[dstPixelStride * col + row * dstLineStride + dstBOffset] = b;
                } else {
                    int gg = powTable[0xffff & g];

                    int r1 = ((255 - m) * (0xffff & r) + m * gg) / 255;
                    int g1 = ((255 - m) * (0xffff & g) + m * gg) / 255;
                    int b1 = ((255 - m) * (0xffff & b) + m * gg) / 255;

                    dstData[dstPixelStride * col + row * dstLineStride + dstROffset] = (short) r1;
                    dstData[dstPixelStride * col + row * dstLineStride + dstGOffset] = (short) g1;
                    dstData[dstPixelStride * col + row * dstLineStride + dstBOffset] = (short) b1;
                }
            }
        }
    }
}
