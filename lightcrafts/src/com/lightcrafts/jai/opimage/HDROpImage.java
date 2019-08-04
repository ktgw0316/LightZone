/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import javax.media.jai.PointOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.RasterAccessor;
import com.sun.media.jai.util.ImageUtil;

import java.awt.image.*;
import java.awt.*;
import java.util.Map;

/**
 * Copyright (C) Light Crafts, Inc.
 * User: fabio
 * Date: Mar 20, 2007
 * Time: 4:32:46 PM
 */
public class HDROpImage extends PointOpImage {
    private static final int div = 2;
    private static final int c = 0x10000 / div;

    private final double detail;
    private short[] intensityTable = new short[c + 1];
    private short[] gammaTable = new short[0x10000];
    private final double intensity;

    private double sigmoid(double x) {
        return x + intensity * x * (1 - 1 / (1 + Math.exp(-6*(x - 0.01))));
    }

    public HDROpImage(RenderedImage source, RenderedImage mask, double intensity, double gamma, double detail, Map config) {
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

        if (numBandsMask != 1)
            throw new UnsupportedOperationException("Only single-banded masks are supported: " + numBandsMask);

        permitInPlaceOperation();

        this.detail = detail;
        this.intensity = intensity;
        
        for (int i = 0; i < c+1; i++)
            intensityTable[i] = (short) (c * sigmoid(i/(double)c));

        for (int i = 0; i < gammaTable.length; i++)
            gammaTable[i] = (short) (0xFFFF * Math.pow(i / (double) 0xFFFF, gamma) + 0.5);
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

    private void computeRectUShort(RasterAccessor src,
                                   RasterAccessor mask,
                                   RasterAccessor dst) {
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();
        int[] srcBandOffsets = src.getBandOffsets();
        short[] srcData = src.getShortDataArrays()[0];

        int srcROffset = srcBandOffsets[0];
        int srcGOffset = srcBandOffsets[1];
        int srcBOffset = srcBandOffsets[2];

        int maskLineStride = mask.getScanlineStride();
        int maskPixelStride = mask.getPixelStride();
        int[] maskBandOffsets = mask.getBandOffsets();
        short[] maskData = mask.getShortDataArrays()[0];

        int maskOffset = maskBandOffsets[0];

        int dstwidth = dst.getWidth();
        int dstheight = dst.getHeight();
        // int dstBands = dst.getNumBands();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();
        int[] dstBandOffsets = dst.getBandOffsets();
        short[] dstData = dst.getShortDataArrays()[0];

        int dstROffset = dstBandOffsets[0];
        int dstGOffset = dstBandOffsets[1];
        int dstBOffset = dstBandOffsets[2];

        int overlays = (int) Math.floor(detail);
        int alpha = (int) (c * (detail - Math.floor(detail)));

        for (int row = 0; row < dstheight; row++) {
            for (int col = 0; col < dstwidth; col++) {
                int r = (0xffff & srcData[srcPixelStride * col + row * srcLineStride + srcROffset]) / div;
                int g = (0xffff & srcData[srcPixelStride * col + row * srcLineStride + srcGOffset]) / div;
                int b = (0xffff & srcData[srcPixelStride * col + row * srcLineStride + srcBOffset]) / div;

                int m = 0xffff & maskData[maskPixelStride * col + row * maskLineStride + maskOffset];

                m = (0xffff & gammaTable[0xffff - m]) / div;

                for (int i = 0; i < overlays; i++)
                    m = softLightBlendPixels(g, m);

                if (alpha > 0)
                    m = softLightBlendPixels(g, m) * alpha / c + m * (c - alpha) / c;

                int rr = div * softLightBlendPixelsIntensity(m, r, intensityTable);
                int gg = div * softLightBlendPixelsIntensity(m, g, intensityTable);
                int bb = div * softLightBlendPixelsIntensity(m, b, intensityTable);

                dstData[dstPixelStride * col + row * dstLineStride + dstROffset] = ImageUtil.clampUShort(rr);
                dstData[dstPixelStride * col + row * dstLineStride + dstGOffset] = ImageUtil.clampUShort(gg);
                dstData[dstPixelStride * col + row * dstLineStride + dstBOffset] = ImageUtil.clampUShort(bb);
            }
        }
    }
}
