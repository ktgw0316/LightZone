/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;


import javax.media.jai.PointOpImage;
import javax.media.jai.ImageLayout;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.HSB;

import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.*;
import java.awt.color.ICC_ProfileRGB;
import java.util.Map;

import sun.awt.image.ShortInterleavedRaster;
import Jama.Matrix;

/**
 * Copyright (C) Light Crafts, Inc.
 * User: fabio
 * Date: Mar 20, 2007
 * Time: 4:32:46 PM
 */
public class HueRotateOpImage extends PointOpImage {
    private float angle;
    private float toSRGB[][];
    private float toLinearRGB[][];

    public HueRotateOpImage(RenderedImage source, float angle, Map config) {
        super(source, new ImageLayout(source), config, true);
        permitInPlaceOperation();
        this.angle = angle;

        ICC_ProfileRGB sRGB = (ICC_ProfileRGB) JAIContext.sRGBColorProfile;
        toSRGB = new Matrix(sRGB.getMatrix()).inverse().times(new Matrix(((ICC_ProfileRGB) JAIContext.linearProfile).getMatrix())).getArrayFloat();
        toLinearRGB = new Matrix(sRGB.getMatrix()).inverse().times(new Matrix(((ICC_ProfileRGB) JAIContext.linearProfile).getMatrix())).inverse().getArrayFloat();
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        ushortLoop((ShortInterleavedRaster) sources[0], (ShortInterleavedRaster) dest);
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

        float rgb[] = new float[3];
        float hsi[] = new float[3];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int srcPixOffset = srcPixelStride * col + row * srcLineStride;
                int r = (0xffff & srcData[srcPixOffset + srcROffset]);
                int g = (0xffff & srcData[srcPixOffset + srcGOffset]);
                int b = (0xffff & srcData[srcPixOffset + srcBOffset]);

                rgb[0] = (toSRGB[0][0] * r + toSRGB[0][1] * g + toSRGB[0][2] * b) / (float) 0xffff;
                rgb[1] = (toSRGB[1][0] * r + toSRGB[1][1] * g + toSRGB[1][2] * b) / (float) 0xffff;
                rgb[2] = (toSRGB[2][0] * r + toSRGB[2][1] * g + toSRGB[2][2] * b) / (float) 0xffff;

                HSB.fromRGB(rgb, hsi);

                hsi[0] += angle;

                if (hsi[0] < 0)
                    hsi[0] += 1;
                else if (hsi[0] >= 1)
                    hsi[0] -= 1;

                HSB.toRGB(hsi, rgb);

                r = (int) (0xffff *(toLinearRGB[0][0] * rgb[0] + toLinearRGB[0][1] * rgb[1] + toLinearRGB[0][2] * rgb[2]));
                g = (int) (0xffff *(toLinearRGB[1][0] * rgb[0] + toLinearRGB[1][1] * rgb[1] + toLinearRGB[1][2] * rgb[2]));
                b = (int) (0xffff *(toLinearRGB[2][0] * rgb[0] + toLinearRGB[2][1] * rgb[1] + toLinearRGB[2][2] * rgb[2]));

                int dstPixOffset = dstPixelStride * col + row * dstLineStride;
                dstData[dstPixOffset + dstROffset] = (short) (r < 0 ? 0 : r > 0xffff ? 0xffff : r);
                dstData[dstPixOffset + dstGOffset] = (short) (g < 0 ? 0 : g > 0xffff ? 0xffff : g);
                dstData[dstPixOffset + dstBOffset] = (short) (b < 0 ? 0 : b > 0xffff ? 0xffff : b);
            }
        }
    }
}
