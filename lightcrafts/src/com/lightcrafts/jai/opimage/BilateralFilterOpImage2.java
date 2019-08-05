/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import javax.media.jai.AreaOpImage;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

import java.util.Map;

/**
 * An OpImage class to perform separable convolve on a source image.
 */
final class BilateralFilterOpImage2 extends AreaOpImage {
    private int wr; /* window radius */
    private int ws; /* window size */
    private float[] kernel;
    private float scale_r;
    private float[] elut;

    private static double SQR(double x) {
        return x * x;
    }

    public BilateralFilterOpImage2(RenderedImage source,
                                   BorderExtender extender,
                                   Map config,
                                   ImageLayout layout,
                                   float sigma_d, float sigma_r) {
        super(source,
              layout,
              config,
              true,
              extender,
              (int) Math.ceil(sigma_d * 2),
              (int) Math.ceil(sigma_d * 2),
              (int) Math.ceil(sigma_d * 2),
              (int) Math.ceil(sigma_d * 2));

        wr = (int) Math.ceil(sigma_d * 2);		/* window radius */
        ws = 2 * wr + 1;		                /* window size */

        kernel = new float[ws];

        kernel = new float[ws];
        for (int i = -wr; i <= wr; i++)
            kernel[wr + i] = (float) (256 / (2 * SQR(sigma_d)) * i * i + 0.25);
        scale_r = (float) (256 / (2 * SQR(sigma_r)));
        elut = new float[0x1000];
        for (int i = 0; i < 0x1000; i++)
            elut[i] = (float) Math.exp(-i / 256.0);

    }

    /**
     * Performs convolution on a specified rectangle. The sources are
     * cobbled.
     *
     * @param sources  an array of source Rasters, guaranteed to provide all
     *                 necessary source data for computing the output.
     * @param dest     a WritableRaster tile containing the area to be computed.
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

    protected void ushortLoop(RasterAccessor src,
                              RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();

        short[][] dstDataArrays = dst.getShortDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short[][] srcDataArrays = src.getShortDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();
        float[][] tmpBuffer = new float[3][ws * dwidth];
        int tmpBufferSize = ws * dwidth;

        short[] dstData = dstDataArrays[0];
        short[] srcData = srcDataArrays[0];
        int srcScanlineOffset = srcBandOffsets[0];
        int dstScanlineOffset = dstBandOffsets[0];

        int revolver = 0;
        int kvRevolver = 0;                 // to match kernel vValues
        for (int j = 0; j < ws - 1; j++) {
            int srcPixelOffset = srcScanlineOffset;

            for (int i = 0; i < dwidth; i++) {
                int imageOffset = srcPixelOffset;
                float sa = 0, sb = 0, ss = 0;
                int g0 = srcData[wr + imageOffset + 1] & 0xffff;
                int a0 = g0 - (srcData[wr + imageOffset] & 0xffff);
                int b0 = g0 - (srcData[wr + imageOffset + 2] & 0xffff);
                for (int v = 0; v < ws; v++) {
                    int g = srcData[imageOffset + 1] & 0xffff;
                    int a = g - (srcData[imageOffset] & 0xffff);
                    int b = g - (srcData[imageOffset + 2] & 0xffff);

                    int sep = ((int) ((SQR(a - a0) + SQR(b - b0)) * scale_r + kernel[wr] + kernel[v])) / 0x10000;
                    if (sep < 0x1000) {
                        float exp = elut[sep];
                        sa += exp * a;
                        sb += exp * b;
                        ss += exp;
                    }
                    imageOffset += srcPixelStride;
                }
                tmpBuffer[0][revolver + i] = g0;
                tmpBuffer[1][revolver + i] = sa/ss;
                tmpBuffer[2][revolver + i] = sb/ss;

                srcPixelOffset += srcPixelStride;
            }
            revolver += dwidth;
            srcScanlineOffset += srcScanlineStride;
        }

        // srcScanlineStride already bumped by
        // ws-1*scanlineStride

        for (int j = 0; j < dheight; j++) {
            int srcPixelOffset = srcScanlineOffset;
            int dstPixelOffset = dstScanlineOffset;

            for (int i = 0; i < dwidth; i++) {
                int imageOffset = srcPixelOffset;
                float sa = 0, sb = 0, ss = 0;
                float g0 = srcData[wr + imageOffset + 1] & 0xffff;
                float a0 = g0 - (srcData[wr + imageOffset] & 0xffff);
                float b0 = g0 - (srcData[wr + imageOffset + 2] & 0xffff);
                for (int v = 0; v < ws; v++) {
                    float g = srcData[imageOffset + 1] & 0xffff;
                    float a = g - (srcData[imageOffset] & 0xffff);
                    float b = g - (srcData[imageOffset + 2] & 0xffff);

                    int sep = ((int) ((SQR(a - a0) + SQR(b - b0)) * scale_r + kernel[wr] + kernel[v])) / 0x10000;
                    if (sep < 0x1000) {
                        float exp = elut[sep];
                        sa += exp * a;
                        sb += exp * b;
                        ss += exp;
                    }
                    imageOffset += srcPixelStride;
                }
                tmpBuffer[0][revolver + i] = g0;
                tmpBuffer[1][revolver + i] = sa/ss;
                tmpBuffer[2][revolver + i] = sb/ss;

                int bb = kvRevolver + i;
                sa = sb = ss = 0;

                int idx0 = bb;
                for (int m = 0; m < wr; m++) {
                    idx0 += dwidth;
                    if (idx0 >= tmpBufferSize) idx0 -= tmpBufferSize;
                }
                g0 = tmpBuffer[0][idx0];
                a0 = tmpBuffer[1][idx0];
                b0 = tmpBuffer[2][idx0];

                for (int aa = 0; aa < ws; aa++) {
                    float a = tmpBuffer[1][bb];
                    float b = tmpBuffer[2][bb];
                    int sep = ((int) ((SQR(a - a0) + SQR(b - b0)) * scale_r + kernel[wr] + kernel[aa])) / 0x10000;
                    if (sep < 0x1000) {
                        float exp = elut[sep];
                        sa += exp * a;
                        sb += exp * b;
                        ss += exp;
                    }
                    bb += dwidth;
                    if (bb >= tmpBufferSize) bb -= tmpBufferSize;
                }

                int g = (int) g0;
                int r = g - (int) (sa / ss);
                int b = g - (int) (sb / ss);

                dstData[dstPixelOffset+0] = (short) (0xffff & (r < 0 ? 0 : r > 0xffff ? 0xffff : r));
                dstData[dstPixelOffset+1] = (short) (0xffff & g);
                dstData[dstPixelOffset+2] = (short) (0xffff & (b < 0 ? 0 : b > 0xffff ? 0xffff : b));

                srcPixelOffset += srcPixelStride;
                dstPixelOffset += dstPixelStride;
            }
            revolver += dwidth;
            if (revolver == tmpBufferSize) {
                revolver = 0;
            }
            kvRevolver += dwidth;
            if (kvRevolver == tmpBufferSize) {
                kvRevolver = 0;
            }
            srcScanlineOffset += srcScanlineStride;
            dstScanlineOffset += dstScanlineStride;
        }
    }
}
