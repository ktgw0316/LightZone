/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import com.lightcrafts.mediax.jai.AreaOpImage;
import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;

import java.util.Map;

public final class BilateralFilterOpImage extends AreaOpImage {

    private int wr; /* window radius */
    private int ws; /* window size */
    private float kernel[], scale_r;

    private static float SQR(float x) { return x * x; }

    public BilateralFilterOpImage(RenderedImage source,
                                  BorderExtender extender,
                                  Map config,
                                  ImageLayout layout,
                                  float sigma_d, float sigma_r) {
        super(source,
              layout,
              config,
              true,
              extender,
              (int) Math.ceil(sigma_d),
              (int) Math.ceil(sigma_d),
              (int) Math.ceil(sigma_d),
              (int) Math.ceil(sigma_d));

        wr = (int) Math.ceil(sigma_d);   /* window radius */
        ws = 2 * wr + 1;		 /* window size */

        kernel = new float[ws];
        for (int i = -wr; i <= wr; i++)
            kernel[wr+i] = (float) (1 / (2 * SQR(sigma_d)) * i * i + 0.25);
        scale_r = 1 / (2 * SQR(sigma_r));
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

        if (src.getNumBands() == 1)
            bilateralFilterMonoRLM(srcData, dstData,
                               wr, ws, 4*scale_r, kernel,
                               swidth, sheight,
                               src.getPixelStride(), dst.getPixelStride(),
                               srcBandOffsets[0], dstBandOffsets[0],
                               srcScanlineStride, dstScanlineStride);
        else
            bilateralFilterChromaRLM(srcData, dstData,
                                  wr, ws, 4*scale_r, kernel,
                                  swidth, sheight,
                                  srcBandOffsets[0], srcBandOffsets[1], srcBandOffsets[2],
                                  dstBandOffsets[0], dstBandOffsets[1], dstBandOffsets[2],
                                  srcScanlineStride, dstScanlineStride);
    }

    static native void bilateralFilterChromaRLM(short srcData[], short destData[],
                                    int wr, int ws, float scale_r, float kernel[],
                                    int width, int height,
                                    int srcROffset, int srcGOffset, int srcBOffset,
                                    int destROffset, int destGOffset, int destBOffset,
                                    int srcLineStride, int destLineStride);

    static native void bilateralFilterMonoRLM(short srcData[], short destData[],
                                        int wr, int ws, float scale_r, float kernel[],
                                        int width, int height,
                                        int srcPixelStride, int destPixelStride,
                                        int srcOffset, int destOffset,
                                        int srcLineStride, int destLineStride);
}
