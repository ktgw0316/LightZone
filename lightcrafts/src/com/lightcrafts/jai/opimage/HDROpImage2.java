/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.image.color.ColorScience;
import com.sun.media.jai.util.ImageUtil;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.*;
import java.awt.image.*;
import java.util.Map;

/**
 * Copyright (C) Light Crafts, Inc.
 * User: fabio
 * Date: Mar 20, 2007
 * Time: 4:32:46 PM
 */
public class HDROpImage2 extends PointOpImage {
    private static final int div = 2;
    private static final int c = 0x10000 / div;

    private final double detail;
    private final double shadows;
    private final double highlights;

    public HDROpImage2(RenderedImage source, RenderedImage mask, double shadows, double highlights, double detail, Map config) {
        super(source, mask, new ImageLayout(source), config, true);

        int numBandsSrc = source.getSampleModel().getNumBands();
        int numBandsMask = mask.getSampleModel().getNumBands();
        int dataType = source.getSampleModel().getDataType();

        if (!(source.getSampleModel() instanceof PixelInterleavedSampleModel))
            throw new UnsupportedOperationException("Unsupported sample model: " + source.getSampleModel().getClass());

        if (dataType != DataBuffer.TYPE_USHORT)
            throw new UnsupportedOperationException("Unsupported data type: " + dataType);

        if (numBandsSrc != 3)
            throw new UnsupportedOperationException("Only three-banded sources are supported: " + numBandsSrc);

        if (numBandsMask != 1 && numBandsMask != 3)
            throw new UnsupportedOperationException("Only single-banded or three-banded masks are supported: " + numBandsMask);

        permitInPlaceOperation();

        this.detail = 1 + (detail - 1) / 5;
        this.shadows = 1 + (shadows - 1) / 5;
        this.highlights = highlights;
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor s1 = new RasterAccessor(sources[0], destRect,
                                               formatTags[0],
                                               getSourceImage(0).getColorModel());
        RasterAccessor s2 = new RasterAccessor(sources[1], destRect,
                                               formatTags[1],
                                               getSourceImage(1).getColorModel());
        RasterAccessor d = new RasterAccessor(dest, destRect,
                                              formatTags[2], getColorModel());

        switch (d.getDataType()) {
            case DataBuffer.TYPE_USHORT:
                computeRectUShort(s1, s2, d);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported data type: " + d.getDataType());
        }

        if (d.needsClamping()) {
            d.clampDataArrays();
        }
        d.copyDataToRaster();
    }

    private static int softLightBlendPixelsIntensity(int front, int back, short[] intensityTable) {
        int m = front * back / c;
        int s = c - (c - front) * (c - back) / c;
        int p = 0xffff & intensityTable[back];
        return (c - p) * m / c + (p * s) / c;
    }

    private static int softLightBlendPixels(int front, int back) {
        int m = front * back / c;
        int s = c - (c - front) * (c - back) / c;
        return (c - back) * m / c + (back * s) / c;
    }

    private static double softLightBlendPixels(double front, double back) {
        double m = front * back;
        // double s = 1 - (1 - front) * (1 - back);
        return (1 - back) * m * m; // + (back * s);
    }

    private static int screenBlendPixels(int front, int back) {
        return c - (c - front) * (c - back) / c;
    }

    private static int screenBlendPixelsIntensity(int front, int back, short[] intensityTable) {
        int s = c - (c - front) * (c - back) / c;
        int p = 0xffff & intensityTable[back];
        return (p * s) / c;
    }

//    int wr = (int) (0x2000 * ColorScience.Wr);
//    int wg = (int) (0x2000 * ColorScience.Wg);
//    int wb = (int) (0x2000 * ColorScience.Wb);

    private static native void cBlendLoop(short[] srcData, short[] maskData, short[] dstData,
                                          int[] srcBandOffsets, int[] maskBandOffsets, int[] dstBandOffsets,
                                          int dstwidth, int dstheight, int srcLineStride, int srcPixelStride,
                                          int maskLineStride, int maskPixelStride, int dstLineStride, int dstPixelStride,
                                          float shadows, float detail, float highlights, float wr, float wg, float wb);

    private static void blendLoop(short[] srcData, short[] maskData, short[] dstData,
                                  int[] srcBandOffsets, int[] maskBandOffsets, int[] dstBandOffsets,
                                  int dstwidth, int dstheight, int srcLineStride, int srcPixelStride,
                                  int maskLineStride, int maskPixelStride, int dstLineStride, int dstPixelStride,
                                  float shadows, float detail, float highlights, float wr, float wg, float wb) {
        int srcROffset = srcBandOffsets[0];
        int srcGOffset = srcBandOffsets[1];
        int srcBOffset = srcBandOffsets[2];

        int maskOffset1, maskOffset2, maskOffset3;

        if (maskBandOffsets.length == 3) {
            maskOffset1 = maskBandOffsets[0];
            maskOffset2 = maskBandOffsets[1];
            maskOffset3 = maskBandOffsets[2];
        } else
            maskOffset1 = maskOffset2 = maskOffset3 = maskBandOffsets[0];

        int dstROffset = dstBandOffsets[0];
        int dstGOffset = dstBandOffsets[1];
        int dstBOffset = dstBandOffsets[2];

        for (int row = 0; row < dstheight; row++) {
            for (int col = 0; col < dstwidth; col++) {
                int r = (0xffff & srcData[srcPixelStride * col + row * srcLineStride + srcROffset]);
                int g = (0xffff & srcData[srcPixelStride * col + row * srcLineStride + srcGOffset]);
                int b = (0xffff & srcData[srcPixelStride * col + row * srcLineStride + srcBOffset]);

                double m = (0xffff & maskData[maskPixelStride * col + row * maskLineStride + maskOffset1])/(double) 0xffff;
                if (maskBandOffsets.length == 3) {
                    double um = (0xffff & maskData[maskPixelStride * col + row * maskLineStride + maskOffset2])/(double) 0xffff;
                    um = Math.min(um*um, 1);

                    double bm = (0xffff & maskData[maskPixelStride * col + row * maskLineStride + maskOffset3])/(double) 0xffff;
                    m = um * m + (1-um) * bm;
                }

                double y = (wr * r + wg * g + wb * b) / 0xffff;

                // y = (1-m) * y * highlights + (1-highlights) * y;

//                double ly = Math.log(y);
//                double lm = Math.log(m);
//                double diff = ly - lm;

                // double diff = Math.log(y/m);

                // double mm = Math.exp(lm / shadows + diff * detail);

                double mm = Math.pow(m, 1/shadows) * Math.pow(y/m, detail);

                // double ratio = (Math.sqrt(1-m) * highlights + (1-highlights)) * mm / y;

                double compressedHilights = softLightBlendPixels(1 - m, y);

                double ratio = (compressedHilights * highlights + (1-highlights)) * mm / y;

                r *= ratio;
                g *= ratio;
                b *= ratio;

                dstData[dstPixelStride * col + row * dstLineStride + dstROffset] = ImageUtil.clampUShort(r);
                dstData[dstPixelStride * col + row * dstLineStride + dstGOffset] = ImageUtil.clampUShort(g);
                dstData[dstPixelStride * col + row * dstLineStride + dstBOffset] = ImageUtil.clampUShort(b);
            }
        }
    }

    private void computeRectUShort(RasterAccessor src,
                                   RasterAccessor mask,
                                   RasterAccessor dst) {
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[] srcData = src.getShortDataArrays()[0];

        int maskLineStride = mask.getScanlineStride();
        int maskPixelStride = mask.getPixelStride();
        int[] maskBandOffsets = mask.getBandOffsets();
        short[] maskData = mask.getShortDataArrays()[0];

        int dstwidth = dst.getWidth();
        int dstheight = dst.getHeight();
        // int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[] dstData = dst.getShortDataArrays()[0];

        cBlendLoop(srcData, maskData, dstData,
                  srcBandOffsets, maskBandOffsets, dstBandOffsets,
                  dstwidth, dstheight, srcLineStride, srcPixelStride,
                  maskLineStride, maskPixelStride, dstLineStride, dstPixelStride,
                  (float) shadows, (float) detail, (float) highlights,
                  ColorScience.Wr, ColorScience.Wg, ColorScience.Wb);
    }
}
