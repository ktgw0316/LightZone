/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import javax.media.jai.PointOpImage;
import javax.media.jai.ImageLayout;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.ColorScience;
import com.lightcrafts.utils.HSB;

import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.*;
import java.awt.color.ICC_ProfileRGB;
import java.awt.color.ICC_Profile;
import java.awt.color.ColorSpace;
import java.util.Map;

import sun.awt.image.ShortInterleavedRaster;
import Jama.Matrix;

/**
 * Copyright (C) Light Crafts, Inc.
 * User: fabio
 * Date: Mar 20, 2007
 * Time: 4:32:46 PM
 */
public class FilteredGrayscaleOpImage extends PointOpImage {
    private final float[][] toLinearsRGB;
    private final float[] filter;
    private final float angle;

    public FilteredGrayscaleOpImage(RenderedImage source, float filter[], float angle, float strenght, Map config) {
        super(source, new ImageLayout(source), config, true);
        permitInPlaceOperation();
        ICC_ProfileRGB sRGB = (ICC_ProfileRGB) JAIContext.sRGBColorProfile;
        ICC_ProfileRGB linRGB = (ICC_ProfileRGB) JAIContext.linearProfile;
        toLinearsRGB = new Matrix(sRGB.getMatrix()).inverse().times(new Matrix(linRGB.getMatrix())).getArrayFloat();

        this.filter = filter.clone();
        this.angle = angle;
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        ushortLoop((ShortInterleavedRaster) sources[0], (ShortInterleavedRaster) dest);
    }

    /*
     * faster float arctan2 implementation.
     * see: http://www.dspguru.com/comp.dsp/tricks/alg/fxdatan2.htm
     */

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

    static float angleDiff(float a, float b) {
        float result = Math.abs(a - b);
        if (result > Math.PI)
            result = (float) (2 * Math.PI - result);
        return result;
    }

    protected void ushortLoop(ShortInterleavedRaster src, ShortInterleavedRaster dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        short dstData[] = dst.getDataStorage();
        int dstBandOffsets[] = dst.getDataOffsets();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();

        short srcData[] = src.getDataStorage();
        int srcBandOffsets[] = src.getDataOffsets();
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();

        int srcROffset = srcBandOffsets[0];
        int srcGOffset = srcBandOffsets[1];
        int srcBOffset = srcBandOffsets[2];

        int dstROffset = dstBandOffsets[0];
        int dstGOffset = dstBandOffsets[1];
        int dstBOffset = dstBandOffsets[2];

        float filterHue, filterSat;
        {
            float hsb[] = new float[3];
            HSB.fromRGB(filter, hsb);
            filterHue = (float) (2 * Math.PI * hsb[0] - Math.PI);
            filterSat = hsb[1];
        }

        float rgb[] = new float[3];
        float hsb[] = new float[3];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int srcPixOffset = srcPixelStride * col + row * srcLineStride;
                int r = 0xffff & srcData[srcPixOffset + srcROffset];
                int g = 0xffff & srcData[srcPixOffset + srcGOffset];
                int b = 0xffff & srcData[srcPixOffset + srcBOffset];

                rgb[0] = toLinearsRGB[0][0] * r + toLinearsRGB[0][1] * g + toLinearsRGB[0][2] * b;
                rgb[1] = toLinearsRGB[1][0] * r + toLinearsRGB[1][1] * g + toLinearsRGB[1][2] * b;
                rgb[2] = toLinearsRGB[2][0] * r + toLinearsRGB[2][1] * g + toLinearsRGB[2][2] * b;

                HSB.fromRGB(rgb, hsb);
                float hue = (float) (2 * Math.PI * hsb[0] - Math.PI);

                float hueDiff = angleDiff(hue, filterHue);
                float mask = (float) Math.cos(Math.PI * hueDiff / angle);
                // float mask = (float) Math.abs(1 - hueDiff / angle);

                mask = (1-filterSat) + filterSat * mask;

                float gray = ColorScience.Wr * r + ColorScience.Wg * g + ColorScience.Wb * b;

                int rr = (int) (gray * mask);
                int gg = (int) (gray * mask);
                int bb = (int) (gray * mask);

                int dstPixOffset = dstPixelStride * col + row * dstLineStride;
                dstData[dstPixOffset + dstROffset] = (short) (rr < 0 ? 0 : rr > 0xffff ? 0xffff : rr);
                dstData[dstPixOffset + dstGOffset] = (short) (gg < 0 ? 0 : gg > 0xffff ? 0xffff : gg);
                dstData[dstPixOffset + dstBOffset] = (short) (bb < 0 ? 0 : bb > 0xffff ? 0xffff : bb);
            }
        }
    }
}
