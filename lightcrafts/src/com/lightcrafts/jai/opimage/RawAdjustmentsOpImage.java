/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;
import javax.media.jai.RasterFormatTag;
import javax.media.jai.RasterAccessor;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.*;
import java.util.Map;

public class RawAdjustmentsOpImage extends PointOpImage {
    private float exposureCompensation;
    private float[][] cameraRGB;

    public RawAdjustmentsOpImage(RenderedImage source,
                                Map config,
                                ImageLayout layout,
                                float exposureCompensation,
                                float colorTemperature,
                                float[][] cameraRGB) {
        super(source, layout, config, true);

        this.exposureCompensation = exposureCompensation;
        this.cameraRGB = cameraRGB;

        permitInPlaceOperation();
    }

    protected void computeRect(Raster[] sources,
                               WritableRaster dest,
                               Rectangle destRect) {
        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        Raster source = sources[0];
        Rectangle srcRect = mapDestRect(destRect, 0);

        RasterAccessor srcAccessor =
                new RasterAccessor(source, srcRect, formatTags[0],
                                   getSourceImage(0).getColorModel());
        RasterAccessor dstAccessor =
                new RasterAccessor(dest, destRect, formatTags[1],
                                   this.getColorModel());

        switch (dstAccessor.getDataType()) {
        case DataBuffer.TYPE_USHORT:
            ushortLoop(srcAccessor, dstAccessor);
            break;
        default:
        }

        // If the RasterAccessor object set up a temporary buffer for the
        // op to write to, tell the RasterAccessor to write that data
        // to the raster no that we're done with it.
        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    protected void ushortLoop(RasterAccessor src, RasterAccessor dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        short[][] dstDataArrays = dst.getShortDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstLineStride = dst.getScanlineStride();

        short[][] srcDataArrays = src.getShortDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcLineStride = src.getScanlineStride();

        short[] dstData = dstDataArrays[0];
        short[] srcData = srcDataArrays[0];

        int srcROffset = srcBandOffsets[0];
        int srcGOffset = srcBandOffsets[1];
        int srcBOffset = srcBandOffsets[2];

        int dstROffset = dstBandOffsets[0];
        int dstGOffset = dstBandOffsets[1];
        int dstBOffset = dstBandOffsets[2];

        float t00 = cameraRGB[0][0], t01 = cameraRGB[0][1], t02 = cameraRGB[0][2],
              t10 = cameraRGB[1][0], t11 = cameraRGB[1][1], t12 = cameraRGB[1][2],
              t20 = cameraRGB[2][0], t21 = cameraRGB[2][1], t22 = cameraRGB[2][2];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int r = 0xffff & srcData[3 * col + row * srcLineStride + srcROffset];
                int g = 0xffff & srcData[3 * col + row * srcLineStride + srcGOffset];
                int b = 0xffff & srcData[3 * col + row * srcLineStride + srcBOffset];

                float r1 = t00 * r + t01 * g + t02 * b;
                float g1 = t10 * r + t11 * g + t12 * b;
                float b1 = t20 * r + t21 * g + t22 * b;

                // Highlight recovery code

                float c = exposureCompensation;

                if (c < 1 && (g1 > 0xffff || r1 > 0xffff || b1 > 0xffff)) {
                    float rs = Math.min(r1, 0xffff);
                    float gs = Math.min(g1, 0xffff);
                    float bs = Math.min(b1, 0xffff);

                    float c1s = gs - rs;
                    float c2s = gs - bs;

                    r1 = c * (g1 - c1s);
                    b1 = c * (g1 - c2s);
                } else {
                    r1 *= c;
                    b1 *= c;
                }
                g1 *= c;

                dstData[3 * col + row * dstLineStride + dstROffset] = (short) (r1 < 0 ? 0 : r1 > 0xffff ? 0xffff : (int) r1);
                dstData[3 * col + row * dstLineStride + dstGOffset] = (short) (g1 < 0 ? 0 : g1 > 0xffff ? 0xffff : (int) g1);
                dstData[3 * col + row * dstLineStride + dstBOffset] = (short) (b1 < 0 ? 0 : b1 > 0xffff ? 0xffff : (int) b1);
            }
        }
    }
}
