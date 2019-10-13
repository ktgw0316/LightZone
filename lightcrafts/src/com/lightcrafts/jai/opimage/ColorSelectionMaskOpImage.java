/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.image.color.ColorScience;
import com.lightcrafts.model.ColorSelection;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.util.Map;

public class ColorSelectionMaskOpImage extends PointOpImage {
    private final ColorSelection colorSelection;

    private static ImageLayout createLayout(RenderedImage source) {
        ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                                false, false,
                                                Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

        ImageLayout layout = new ImageLayout(source);
        layout.setColorModel(cm);
        layout.setSampleModel(cm.createCompatibleSampleModel(source.getWidth(), source.getHeight()));
        return layout;
    }

    public ColorSelectionMaskOpImage(RenderedImage source, ColorSelection colorSelection, Map config) {
        super(source, createLayout(source), config, true);
        this.colorSelection = colorSelection;
    }

    @Override
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor src = new RasterAccessor(source, srcRect, formatTags[0],
                getSourceImage(0).getColorModel());
        RasterAccessor dst = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        int width = dst.getWidth();
        int height = dst.getHeight();

        byte[] dstData = dst.getByteDataArray(0);
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstLineStride = dst.getScanlineStride();

        short[] srcData = src.getShortDataArray(0);
        int[] srcBandOffsets = src.getBandOffsets();
        int srcLineStride = src.getScanlineStride();

        int dstOffset = dstBandOffsets[0];

        float[] colorSelectionArray = {
                colorSelection.isHueEnabled ? colorSelection.hueLower : 0,
                colorSelection.isHueEnabled ? colorSelection.hueLowerFeather : 0,
                colorSelection.isHueEnabled ? colorSelection.hueUpper : 1,
                colorSelection.isHueEnabled ? colorSelection.hueUpperFeather : 0,
                colorSelection.isLuminosityEnabled ? colorSelection.luminosityLower : 0,
                colorSelection.isLuminosityEnabled ? colorSelection.luminosityLowerFeather : 0,
                colorSelection.isLuminosityEnabled ? colorSelection.luminosityUpper : 1,
                colorSelection.isLuminosityEnabled ? colorSelection.luminosityUpperFeather : 0
        };

        float wr = ColorScience.Wr;
        float wg = ColorScience.Wg;
        float wb = ColorScience.Wb;

        nativeUshortLoop(srcData, dstData, width, height,
                         srcBandOffsets, dstOffset,
                         srcLineStride, dstLineStride,
                         colorSelectionArray, wr, wg, wb);
    }

    private static final int sMath_scale = 0x8000;
    private static final int sMath_PI = (int) (sMath_scale * Math.PI);
    private static final int sqrt3d2 = (int) (sMath_scale * Math.sqrt(3) / 2); // 0.866...

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

    public static int hue(int r, int g, int b) {
        int x = r - (g+b) / 2;
        int y = (sqrt3d2 * (g-b)) / sMath_scale;
        int hue = arctan2(y, x);
        if (hue < 0)
            hue += 2 * sMath_PI;
        return hue;
    }

    static float arctan2(float y, float x) {
        final float coeff_1 = (float) Math.PI / 4;
        final float coeff_2 = 3 * coeff_1;
        final float abs_y = Math.abs(y) + 1e-10f;      // kludge to prevent 0/0 condition
        float angle;

        if (x >= 0) {
            float r = (x - abs_y) / (x + abs_y);
            angle = coeff_1 - coeff_1 * r;
        } else {
            float r = (x + abs_y) / (abs_y - x);
            angle = coeff_2 - coeff_1 * r;
        }

        return y < 0 ? -angle : angle;
    }

    public static float hue(float r, float g, float b) {
        float x = r - (g+b) / 2;
        float y = ((g-b) * (float) Math.sqrt(3) / 2);
        float hue = arctan2(y, x);
        if (hue < 0)
            hue += 2 * Math.PI;
        return hue;
    }

    private native void nativeUshortLoop(short[] srcData, byte[] dstData,
                                         int width, int height,
                                         int[] srcBandOffsets, int dstOffset,
                                         int srcLineStride, int dstLineStride,
                                         float[] colorSelection, float wr, float wg, float wb);

    private void ushortLoop(short[] srcData, byte[] dstData,
                            int width, int height,
                            int[] srcBandOffsets, int dstOffset,
                            int srcLineStride, int dstLineStride,
                            float[] colorSelection, float wr, float wg, float wb) {
        int srcROffset = srcBandOffsets[0];
        int srcGOffset = srcBandOffsets[1];
        int srcBOffset = srcBandOffsets[2];

        float hueLower                  = colorSelection[0];
        float hueLowerFeather           = colorSelection[1];
        float hueUpper                  = colorSelection[2];
        float hueUpperFeather           = colorSelection[3];
        float luminosityLower           = colorSelection[4];
        float luminosityLowerFeather    = colorSelection[5];
        float luminosityUpper           = colorSelection[6];
        float luminosityUpperFeather    = colorSelection[7];

        int hueOffset = 0;

        if (hueLower < 0 || hueLower - hueLowerFeather < 0 || hueUpper < 0) {
            hueLower += 1;
            hueUpper += 1;
            hueOffset = 1;
        } else if (hueLower > 1 || hueUpper + hueUpperFeather > 1 || hueUpper > 1) {
            hueOffset = -1;
        }

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                float r = 0xffff & srcData[3 * col + row * srcLineStride + srcROffset];
                float g = 0xffff & srcData[3 * col + row * srcLineStride + srcGOffset];
                float b = 0xffff & srcData[3 * col + row * srcLineStride + srcBOffset];

                // float hue = hue(r / (float) 0xffff, g / (float) 0xffff, b / (float) 0xffff) / (float) (2 * Math.PI);

                float cmax = (r > g) ? r : g;
                if (b > cmax) cmax = b;
                float cmin = (r < g) ? r : g;
                if (b < cmin) cmin = b;

                float saturation;
                if (cmax != 0)
                    saturation = (cmax - cmin) / cmax;
                else
                    saturation = 0;

                float brightnessMask, colorMask;

                final float stmin = 0.01f;
                final float stmax = 0.02f;

                if (saturation > stmin) {
                    float hue = hue(r, g, b) / (float) (2 * Math.PI);

                    if (hueOffset == 1 && hue < hueLower - hueLowerFeather)
                        hue += 1;
                    else if (hueOffset == -1 && hue < 0.5)
                        hue += 1;

                    if (hue >= hueLower && hue <= hueUpper)
                        colorMask = 1;
                    else if (hue >= (hueLower - hueLowerFeather) && hue < hueLower)
                        colorMask = (hue - (hueLower - hueLowerFeather))/hueLowerFeather;
                    else if (hue > hueUpper && hue <= (hueUpper + hueUpperFeather))
                        colorMask = (hueUpper + hueUpperFeather - hue)/hueUpperFeather;
                    else
                        colorMask = 0;

                    if (saturation < stmax)
                        colorMask *= (saturation - stmin) / (stmax - stmin);
                } else
                    colorMask = 0;

                float luminosity = (float) (Math.log1p((wr * r + wg * g + wb * b)/0x100) / (8 * Math.log(2)));

                if (luminosity >= luminosityLower && luminosity <= luminosityUpper)
                    brightnessMask = 1;
                else if (luminosity >= (luminosityLower - luminosityLowerFeather) && luminosity < luminosityLower)
                    brightnessMask = (luminosity - (luminosityLower - luminosityLowerFeather))/luminosityLowerFeather;
                else if (luminosity > luminosityUpper && luminosity <= (luminosityUpper + luminosityUpperFeather))
                    brightnessMask = (luminosityUpper + luminosityUpperFeather - luminosity)/luminosityUpperFeather;
                else
                    brightnessMask = 0;

                colorMask *= brightnessMask;

                dstData[col + row * dstLineStride + dstOffset] = (byte) (0xff * colorMask);
            }
        }
    }
}
