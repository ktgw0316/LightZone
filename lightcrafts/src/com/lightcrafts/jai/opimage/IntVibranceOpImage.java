/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.jai.JAIContext;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.*;
import java.awt.color.ICC_ProfileRGB;
import java.awt.color.ICC_Profile;
import java.awt.color.ColorSpace;
import java.util.Map;

import Jama.Matrix;

/**
 * Copyright (C) Light Crafts, Inc.
 * User: fabio
 * Date: Mar 20, 2007
 * Time: 4:32:46 PM
 */
public class IntVibranceOpImage extends PointOpImage {
    private final int transform[][] = new int[3][3];
    private final int toLinearsRGB[][] = new int[3][3];
    private final boolean saturationIncrease;

    private static final int sMath_scale = 0x8000;
    private static final int sMath_PI = (int) (sMath_scale * Math.PI);

    /*
     * fast integer arctan2 implementation.
     * see: http://www.dspguru.com/comp.dsp/tricks/alg/fxdatan2.htm
     */

    static int arctan2(int y, int x) {
        final int coeff_1 = sMath_PI / 4;
        final int coeff_2 = 3 * coeff_1;
        final int abs_y = Math.abs(y) + 1;      // kludge to prevent 0/0 condition
        final int angle;

        if (x >= 0) {
            int r = (sMath_scale * (x - abs_y)) / (x + abs_y);
            angle = coeff_1 - coeff_1 * r / sMath_scale;
        } else {
            int r = (sMath_scale * (x + abs_y)) / (abs_y - x);
            angle = coeff_2 - coeff_1 * r / sMath_scale;
        }

        return y < 0 ? -angle : angle;
    }

    public IntVibranceOpImage(RenderedImage source, float transform[][], Map config) {
        super(source, new ImageLayout(source), config, true);
        permitInPlaceOperation();

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                this.transform[i][j] = (int) (sMath_scale * transform[i][j]);

        saturationIncrease = transform[0][0] > 1;

        ICC_ProfileRGB linRGB = (ICC_ProfileRGB) ICC_Profile.getInstance(ColorSpace.CS_LINEAR_RGB);
        Matrix XYZtoLinsRGB = new Matrix(linRGB.getMatrix()).inverse();
        Matrix CIERGBtoXYZ = new Matrix(((ICC_ProfileRGB) JAIContext.linearProfile).getMatrix());
        double CIERGBtoLinsRGB[][] = XYZtoLinsRGB.times(CIERGBtoXYZ).getArray();

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                toLinearsRGB[i][j] = (int) (sMath_scale * CIERGBtoLinsRGB[i][j]);

    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor src = new RasterAccessor(sources[0], destRect, formatTags[0],
                getSourceImage(0).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        switch (dst.getDataType()) {
            case DataBuffer.TYPE_USHORT:
                ushortLoop(src, dst);
                break;
            default:
                throw new UnsupportedOperationException("Unsupported data type: " + dst.getDataType());
        }
    }

    protected void ushortLoop(RasterAccessor src, RasterAccessor dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        short dstData[] = dst.getShortDataArray(0);
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();

        short srcData[] = src.getShortDataArray(0);
        int srcBandOffsets[] = src.getBandOffsets();
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();

        int srcROffset = srcBandOffsets[0];
        int srcGOffset = srcBandOffsets[1];
        int srcBOffset = srcBandOffsets[2];

        int dstROffset = dstBandOffsets[0];
        int dstGOffset = dstBandOffsets[1];
        int dstBOffset = dstBandOffsets[2];

        final int sqrt3d2 = (int) (sMath_scale * Math.sqrt(3) / 2); // 0.866...

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int srcPixOffset = srcPixelStride * col + row * srcLineStride;
                int r = (0xffff & srcData[srcPixOffset + srcROffset]) / 2;
                int g = (0xffff & srcData[srcPixOffset + srcGOffset]) / 2;
                int b = (0xffff & srcData[srcPixOffset + srcBOffset]) / 2;

                int lr = (toLinearsRGB[0][0] * r + toLinearsRGB[0][1] * g + toLinearsRGB[0][2] * b) / sMath_scale;
                int lg = (toLinearsRGB[1][0] * r + toLinearsRGB[1][1] * g + toLinearsRGB[1][2] * b) / sMath_scale;
                int lb = (toLinearsRGB[2][0] * r + toLinearsRGB[2][1] * g + toLinearsRGB[2][2] * b) / sMath_scale;

                int x = lr - (lg+lb) / 2;
                int y = (sqrt3d2 * (lg-lb)) / sMath_scale;

                int hue = arctan2(x, y) + sMath_PI;

                if (hue < 0)
                    hue += 2 * sMath_PI;

                if (hue > 4 * sMath_PI / 3)
                    hue -= 4 * sMath_PI / 3;
                else if (hue > 2 * sMath_PI / 3)
                    hue -= 2 * sMath_PI / 3;

                int mask = sMath_scale / 2 + (sMath_scale - (sMath_scale * Math.abs(sMath_PI / 6 - hue)) / (sMath_PI / 3)) / 2;

                if (saturationIncrease) {
                    int min = Math.min(r, Math.min(g, b));
                    int max = Math.max(r, Math.max(g, b));

                    int saturation = max != 0 ? sMath_scale - sMath_scale * min / max : 0;
                    mask = mask * (sMath_scale - saturation * saturation / sMath_scale) / sMath_scale;
                }

                int rr = (transform[0][0] * r + transform[0][1] * g + transform[0][2] * b) / sMath_scale;
                int gg = (transform[1][0] * r + transform[1][1] * g + transform[1][2] * b) / sMath_scale;
                int bb = (transform[2][0] * r + transform[2][1] * g + transform[2][2] * b) / sMath_scale;

                rr = 2 * ((sMath_scale - mask) * r / sMath_scale + rr * mask / sMath_scale);
                gg = 2 * ((sMath_scale - mask) * g / sMath_scale + gg * mask / sMath_scale);
                bb = 2 * ((sMath_scale - mask) * b / sMath_scale + bb * mask / sMath_scale);

                int dstPixOffset = dstPixelStride * col + row * dstLineStride;
                dstData[dstPixOffset + dstROffset] = (short) (rr < 0 ? 0 : rr > 0xffff ? 0xffff : rr);
                dstData[dstPixOffset + dstGOffset] = (short) (gg < 0 ? 0 : gg > 0xffff ? 0xffff : gg);
                dstData[dstPixOffset + dstBOffset] = (short) (bb < 0 ? 0 : bb > 0xffff ? 0xffff : bb);
            }
        }
    }
}
