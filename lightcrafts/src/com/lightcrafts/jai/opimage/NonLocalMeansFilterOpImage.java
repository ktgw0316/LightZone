/* Copyright (C) 2015 Masahiro Kitagawa */
package com.lightcrafts.jai.opimage;

import com.lightcrafts.image.color.ColorScience;
import javax.media.jai.*;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

public final class NonLocalMeansFilterOpImage extends AreaOpImage {

    final float[] y_kernel;
    final float[] c_kernel; // negated exponents of the spatial gaussian function
    final int y_search_radius, c_search_radius;
    final int y_patch_radius, c_patch_radius;
    final float y_h, c_h; // intensities
    final float[] rgb_to_yst;
    final float[] yst_to_rgb;

    static float SQR(float x) { return x * x; }

    static int border(int b1, int b2) {
        return (int) Math.max(Math.ceil(b1), Math.ceil(b2));
    }

    public NonLocalMeansFilterOpImage(RenderedImage source,
                                     BorderExtender extender,
                                     Map config,
                                     ImageLayout layout,
                                     int y_search_radius, int y_patch_radius, float y_h,
                                     int c_search_radius, int c_patch_radius, float c_h) {
        super(source,
              layout,
              config,
              true,
              extender,
              border(2 * y_search_radius, 2 * c_search_radius),
              border(2 * y_search_radius, 2 * c_search_radius),
              border(2 * y_search_radius, 2 * c_search_radius),
              border(2 * y_search_radius, 2 * c_search_radius));

        this.y_search_radius = y_search_radius;
        this.y_patch_radius = y_patch_radius;
        this.y_h = y_h;
        this.c_search_radius = c_search_radius;
        this.c_patch_radius = c_patch_radius;
        this.c_h = c_h;

        y_kernel = new float[2 * y_search_radius + 1];
        for (int i = -y_search_radius; i <= y_search_radius; i++) {
            y_kernel[y_search_radius + i] = (float) (1 / (2 * SQR(y_search_radius)) * i * i + 0.25);
        }

        c_kernel = new float[2 * c_search_radius + 1];
        for (int i = -c_search_radius; i <= c_search_radius; i++) {
            c_kernel[c_search_radius + i] = (float) (1 / (2 * SQR(c_search_radius)) * i * i + 0.25);
        }

        ColorScience.LinearTransform transform = new ColorScience.YST();

        double[][] rgb2yst = transform.fromRGB(DataBuffer.TYPE_FLOAT);
        rgb_to_yst = new float[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                rgb_to_yst[3 * i + j] = (float) rgb2yst[i][j];
            }
        }

        double[][] yst2rgb = transform.toRGB(DataBuffer.TYPE_FLOAT);
        yst_to_rgb = new float[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                yst_to_rgb[3 * i + j] = (float) yst2rgb[i][j];
            }
        }
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

        short[][] dstDataArrays = dst.getShortDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstScanlineStride = dst.getScanlineStride();

        short[][] srcDataArrays = src.getShortDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcScanlineStride = src.getScanlineStride();

        short[] dstData = dstDataArrays[0];
        short[] srcData = srcDataArrays[0];

        if (src.getNumBands() == 3)
            nonLocalMeansFilter(srcData, dstData,
                    y_search_radius, y_patch_radius,
                    c_search_radius, c_patch_radius,
                    y_h, c_h,
                    y_kernel, c_kernel,
                    rgb_to_yst, yst_to_rgb,
                    swidth, sheight,
                    srcBandOffsets[0], srcBandOffsets[1], srcBandOffsets[2],
                    dstBandOffsets[0], dstBandOffsets[1], dstBandOffsets[2],
                    srcScanlineStride, dstScanlineStride);
    }

    static native void nonLocalMeansFilter(short[] srcData, short[] destData,
                                           int y_search_radius, int y_patch_radius,
                                           int c_search_radius, int c_patch_radius,
                                           float y_h, float c_h,
                                           float[] y_kernel, float[] c_kernel,
                                           float[] rgb_to_yst, float[] yst_to_rgb,
                                           int width, int height,
                                           int srcROffset, int srcGOffset, int srcBOffset,
                                           int destROffset, int destGOffset, int destBOffset,
                                           int srcLineStride, int destLineStride);
}
