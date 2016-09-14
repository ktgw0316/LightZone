package com.lightcrafts.jai.opimage;

/**
 * Copyright (C) 2010 Light Crafts, Inc.
 * Author: fabio
 * 12/22/10 @ 11:20 AM
 */

import com.lightcrafts.image.color.ColorScience;
import com.lightcrafts.mediax.jai.*;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

public final class BilateralFilterRGBOpImage extends AreaOpImage {

    private final int y_wr, c_wr; /* window radius */
    private final int y_ws, c_ws; /* window size */
    private final float y_kernel[], c_kernel[], y_scale_r, c_scale_r;
    private final float rgb_to_yst[], yst_to_rgb[];

    private static float SQR(float s) { return s * s; }

    private static int border(float y_sigma_d, float c_sigma_d) {
        return (int) Math.max(Math.ceil(y_sigma_d), Math.ceil(c_sigma_d));
    }

    public BilateralFilterRGBOpImage(RenderedImage source,
                                     BorderExtender extender,
                                     Map config,
                                     ImageLayout layout,
                                     float y_sigma_d, float y_sigma_r, float c_sigma_d, float c_sigma_r) {
        super(source,
              layout,
              config,
              true,
              extender,
              border(y_sigma_d, c_sigma_d),
              border(y_sigma_d, c_sigma_d),
              border(y_sigma_d, c_sigma_d),
              border(y_sigma_d, c_sigma_d));

        y_wr = (int) Math.ceil(y_sigma_d);   /* window radius */
        y_ws = 2 * y_wr + 1;		 /* window size */

        y_kernel = new float[y_ws];
        for (int i = -y_wr; i <= y_wr; i++)
            y_kernel[y_wr+i] = (float) (1 / (2 * SQR(y_sigma_d)) * i * i + 0.25);
        y_scale_r = 1 / (2 * SQR(y_sigma_r));

        c_wr = (int) Math.ceil(c_sigma_d);   /* window radius */
        c_ws = 2 * c_wr + 1;		 /* window size */

        c_kernel = new float[c_ws];
        for (int i = -c_wr; i <= c_wr; i++)
            c_kernel[c_wr+i] = (float) (1 / (2 * SQR(c_sigma_d)) * i * i + 0.25);
        c_scale_r = 1 / (2 * SQR(c_sigma_r));

        ColorScience.LinearTransform transform = new ColorScience.YST();

        double[][] rgb2yst = transform.fromRGB(DataBuffer.TYPE_FLOAT);
        rgb_to_yst = new float[9];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                rgb_to_yst[3*i+j] = (float) rgb2yst[i][j];

        double[][] yst2rgb = transform.toRGB(DataBuffer.TYPE_FLOAT);
        yst_to_rgb = new float[9];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                yst_to_rgb[3*i+j] = (float) yst2rgb[i][j];
    }

    /**
     * Performs convolution on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources an array of source Rasters, guaranteed to provide all
     *                necessary source data for computing the output.
     * @param dest a WritableRaster tile containing the area to be computed.
     * @param destRect the rectangle within dest to be processed.
     */
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
        int swidth = src.getWidth();
        int sheight = src.getHeight();

        short dstDataArrays[][] = dst.getShortDataArrays();
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstScanlineStride = dst.getScanlineStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        int srcBandOffsets[] = src.getBandOffsets();
        int srcScanlineStride = src.getScanlineStride();

        short dstData[] = dstDataArrays[0];
        short srcData[] = srcDataArrays[0];

        if (src.getNumBands() == 3)
            bilateralFilterRGB(srcData, dstData,
                    y_wr, c_wr, y_ws, c_ws,
                    4 * y_scale_r, 4 * c_scale_r,
                    y_kernel, c_kernel,
                    rgb_to_yst, yst_to_rgb,
                    swidth, sheight,
                    srcBandOffsets[0], srcBandOffsets[1], srcBandOffsets[2],
                    dstBandOffsets[0], dstBandOffsets[1], dstBandOffsets[2],
                    srcScanlineStride, dstScanlineStride);
    }

    static native void bilateralFilterRGB(short srcData[], short destData[],
                                    int y_wr, int c_wr, int y_ws, int c_ws,
                                    float y_scale_r, float c_scale_r,
                                    float y_kernel[], float c_kernel[],
                                    float rgb_to_yst[], float yst_to_rgb[],
                                    int width, int height,
                                    int srcROffset, int srcGOffset, int srcBOffset,
                                    int destROffset, int destGOffset, int destBOffset,
                                    int srcLineStride, int destLineStride);
}
