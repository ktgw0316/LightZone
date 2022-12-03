/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.image.color.ColorScience;
import com.lightcrafts.image.color.HSB;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.LCMatrix;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.*;
import java.awt.color.ICC_ProfileRGB;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

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

    public FilteredGrayscaleOpImage(RenderedImage source, float[] filter, float angle, float strength, Map config) {
        super(source, new ImageLayout(source), config, true);
        permitInPlaceOperation();
        ICC_ProfileRGB sRGB = (ICC_ProfileRGB) JAIContext.sRGBColorProfile;
        ICC_ProfileRGB linRGB = (ICC_ProfileRGB) JAIContext.linearProfile;
        toLinearsRGB = LCMatrix.getArrayFloat(
                new LCMatrix(sRGB.getMatrix())
                        .invert()
                        .mult(new LCMatrix(linRGB.getMatrix()))
        );

        this.filter = filter.clone();
        this.angle = angle;
    }

    @Override
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

        if (dst.needsClamping()) {
            dst.clampDataArrays();
        }
        dst.copyDataToRaster();
    }

    private static float angleDiff(float a, float b) {
        float result = Math.abs(a - b);
        if (result > Math.PI)
            result = (float) (2 * Math.PI - result);
        return result;
    }

    protected void ushortLoop(RasterAccessor src, RasterAccessor dst) {
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

        int srcROffset = srcBandOffsets[0];
        int srcGOffset = srcBandOffsets[1];
        int srcBOffset = srcBandOffsets[2];

        int dstROffset = dstBandOffsets[0];
        int dstGOffset = dstBandOffsets[1];
        int dstBOffset = dstBandOffsets[2];

        float filterHue, filterSat;
        {
            float[] hsb = new float[3];
            HSB.fromRGB(filter, hsb);
            filterHue = (float) (2 * Math.PI * hsb[0] - Math.PI);
            filterSat = hsb[1];
        }

        float[] rgb = new float[3];
        float[] hsb = new float[3];

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
