/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import javax.media.jai.PointOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

import java.awt.image.*;
import java.awt.color.ColorSpace;
import java.awt.*;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Mar 13, 2007
 * Time: 2:36:29 PM
 * To change this template use File | Settings | File Templates.
 */
public class RedMaskOpImage extends PointOpImage {
    private final double tolerance;

    private static ImageLayout createLayout(RenderedImage source) {
        ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                                false, false,
                                                Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

        ImageLayout layout = new ImageLayout(source);
        layout.setColorModel(cm);
        layout.setSampleModel(cm.createCompatibleSampleModel(source.getWidth(), source.getHeight()));
        return layout;
    }

    public RedMaskOpImage(RenderedImage source, double tolerance, Map config) {
        super(source, createLayout(source), config, true);
        this.tolerance = tolerance;
    }

    @Override
    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor src = new RasterAccessor(source, srcRect, formatTags[0],
                                                getSourceImage(0).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        switch (dst.getDataType()) {
            case DataBuffer.TYPE_BYTE:
                ushortLoop(src, dst);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported data type: " + dst.getDataType());
        }
    }

    protected void ushortLoop(RasterAccessor src, RasterAccessor dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        byte[] dstData = dst.getByteDataArray(0);
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();

        short[] srcData = src.getShortDataArray(0);
        int[] srcBandOffsets = src.getBandOffsets();
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();

        int srcLOffset = srcBandOffsets[0];
        int srcAOffset = srcBandOffsets[1];
        int srcBOffset = srcBandOffsets[2];

        int dstOffset = dstBandOffsets[0];

//        double ra = 68; double radev = 15.3 * tolerance;
//        double rb = 39; double rbdev = 14.5 * tolerance;
//
//        int raMin = (int) ((ra - radev) * 256);
//        int raMax = (int) ((ra + radev) * 256);
//        int rbMin = (int) ((rb - rbdev) * 256);
//        int rbMax = (int) ((rb + rbdev) * 256);
//
//        double sa = 19; double sadev = 5.1 * 3.5;
//        double sb = 17; double sbdev = 2.66 * 3.5;
//
//        int saMin = (int) ((sa - sadev) * 256);
//        int saMax = (int) ((sa + sadev) * 256);
//        int sbMin = (int) ((sb - sbdev) * 256);
//        int sbMax = (int) ((sb + sbdev) * 256);

        // Sclera Red
        int raMin = (int) ((71.5 - 48.5 * tolerance) * 256);
        int raMax = (int) ((71.5 + 48.5 * tolerance) * 256);
        int rbMin = (int) ((8 - 35 * tolerance) * 256);
        int rbMax = (int) ((8 + 35 * tolerance) * 256);

        // Really Red
        int rraMin = (int) ((62 - 6.8 * 2) * 256);
        int rraMax = (int) ((62 + 6.8 * 2) * 256);
        int rrbMin = (int) ((35 - 6.5 * 2) * 256);
        int rrbMax = (int) ((35 + 6.5 * 2) * 256);

        // Kinda Orange
        int oaMin = (31 - 15 * 2) * 256;
        int oaMax = (31 + 15 * 2) * 256;
        int obMin = (56 -  5 * 2) * 256;
        int obMax = (56 +  5 * 2) * 256;

        // Magenta
        // int maMin = ( 30 - 12 * 2) * 256;
        // int maMax = ( 30 + 12 * 2) * 256;
        // int mbMin = (-12 - 13 * 2) * 256;
        // int mbMax = (-12 + 13 * 2) * 256;

        // Skin
        int sLMin = (int) (43.3593 * 0xffff / 100);
        int sLMax = (int) (99.6093 * 0xffff / 100);
        int saMin = (int) (1.5 * 256);
        int saMax = (int) (43.125 * 256);
        int sbMin = (int) (-4.6875 * 256);
        int sbMax = (int) (34.6875 * 256);

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int L = 0xffff & srcData[3 * col + row * srcLineStride + srcLOffset];
                int a = 0xffff & srcData[srcPixelStride * col + row * srcLineStride + srcAOffset] - 128 * 256;
                int b = 0xffff & srcData[srcPixelStride * col + row * srcLineStride + srcBOffset] - 128 * 256;

                if (((a > raMin && a < raMax && b > rbMin && b < rbMax)
                    || (a > oaMin && a < oaMax && b > obMin && b < obMax)
                    || (a > rraMin && a < rraMax && b > rrbMin && b < rrbMax))
                    // || (a > maMin && a < maMax && b > mbMin && b < mbMax))
                     && !(L > sLMin && L < sLMax && a > saMin && a < saMax && b > sbMin && b < sbMax))
                    dstData[dstPixelStride * col + row * dstLineStride + dstOffset] = (byte) 0xff;
                else
                    dstData[dstPixelStride * col + row * dstLineStride + dstOffset] = (byte) 0;
            }
        }
    }
}
