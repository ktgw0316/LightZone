package com.lightcrafts.jai.opimage;

import org.eclipse.imagen.AreaOpImage;
import org.eclipse.imagen.BorderExtender;
import org.eclipse.imagen.ImageLayout;
import org.eclipse.imagen.KernelImageN;
import org.eclipse.imagen.RasterAccessor;
import org.eclipse.imagen.RasterFormatTag;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

public class FastBoxFilterOpImage extends AreaOpImage {

    protected int kw, kh, kx, ky;

    private final float hValue;
    private final float vValue;

    public FastBoxFilterOpImage(RenderedImage source,
                                BorderExtender extender,
                                Map config,
                                ImageLayout layout,
                                KernelImageN kernel
    ) {
        super(source,
                layout,
                config,
                true,
                extender,
                kernel.getLeftPadding(),
                kernel.getRightPadding(),
                kernel.getTopPadding(),
                kernel.getBottomPadding());

        kw = kernel.getWidth();
        kh = kernel.getHeight();
        kx = kernel.getXOrigin();
        ky = kernel.getYOrigin();

        hValue = kernel.getHorizontalKernelData()[0];
        vValue = kernel.getVerticalKernelData()[0];
    }

    @Override
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
//            case DataBuffer.TYPE_BYTE:
//                byteLoop(srcAccessor, dstAccessor);
//                break;
            case DataBuffer.TYPE_INT:
                intLoop(srcAccessor, dstAccessor);
                break;
//            case DataBuffer.TYPE_SHORT:
//                shortLoop(srcAccessor, dstAccessor);
//                break;
            case DataBuffer.TYPE_USHORT:
                ushortLoop(srcAccessor, dstAccessor);
                break;
//            case DataBuffer.TYPE_FLOAT:
//                floatLoop(srcAccessor, dstAccessor);
//                break;
//            case DataBuffer.TYPE_DOUBLE:
//                doubleLoop(srcAccessor, dstAccessor);
//                break;
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

    protected void intLoop(RasterAccessor src,
                           RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        int[][] dstDataArrays = dst.getIntDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        int[][] srcDataArrays = src.getIntDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        // Temporary cumulative buffer sized to the max of width/height + kernel
        int maxLen = Math.max(dwidth, dheight) + Math.max(kw, kh);
        long[] cum = new long[maxLen + 1];

        for (int k = 0; k < dnumBands; k++) {
            final int[] dstData = dstDataArrays[k];
            final int[] srcData = srcDataArrays[k];

            // intermediate buffer to hold horizontal-pass results; match src layout to include padding
            int[] tmp = new int[srcData.length];

            // horizontal pass using cumulative sums (chunked implicitly by reuse)
            int srcScanlineOffset = srcBandOffsets[k];
            int tmpScanlineOffset = srcBandOffsets[k];
            int rowsToProcess = dheight + kh - 1;
            for (int j = 0; j < rowsToProcess; j++) {
                // build cumulative sum for this source scanline: we need dwidth + kw - 1 samples
                int samples = dwidth + kw - 1;
                cum[0] = 0L;
                int sp = srcScanlineOffset;
                for (int t = 0; t < samples; t++, sp += srcPixelStride) {
                    cum[t + 1] = cum[t] + (long) srcData[sp];
                }

                int tmpPixelOffset = tmpScanlineOffset;
                for (int out = 0; out < dwidth; out++) {
                    long sumRaw = cum[out + kw] - cum[out];
                    tmp[tmpPixelOffset] = (int) (sumRaw * hValue);
                    tmpPixelOffset += srcPixelStride;
                }

                srcScanlineOffset += srcScanlineStride;
                tmpScanlineOffset += srcScanlineStride;
            }

            // vertical pass using cumulative sums down each column reading from tmp and writing to dst
            int tmpPixelOffset = srcBandOffsets[k];
            int dstPixelOffset = dstBandOffsets[k];
            for (int i = 0; i < dwidth; i++) {
                // build cumulative sum for this source column: need dheight + kh - 1 samples
                int samples = dheight + kh - 1;
                cum[0] = 0L;
                int tp = tmpPixelOffset;
                for (int t = 0; t < samples; t++, tp += srcScanlineStride) {
                    cum[t + 1] = cum[t] + (long) tmp[tp];
                }

                int outOffset = dstPixelOffset;
                for (int out = 0; out < dheight; out++) {
                    long sumRaw = cum[out + kh] - cum[out];
                    dstData[outOffset] = (int) (sumRaw * vValue);
                    outOffset += dstScanlineStride;
                }

                tmpPixelOffset += srcPixelStride;
                dstPixelOffset += dstPixelStride;
            }
        }
    }

    protected void ushortLoop(RasterAccessor src,
                              RasterAccessor dst) {
        int dwidth = dst.getWidth();
        int dheight = dst.getHeight();
        int dnumBands = dst.getNumBands();

        short[][] dstDataArrays = dst.getShortDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstPixelStride = dst.getPixelStride();
        int dstScanlineStride = dst.getScanlineStride();

        short[][] srcDataArrays = src.getShortDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcPixelStride = src.getPixelStride();
        int srcScanlineStride = src.getScanlineStride();

        int maxLen = Math.max(dwidth, dheight) + Math.max(kw, kh);
        long[] cum = new long[maxLen + 1];

        for (int k = 0; k < dnumBands; k++) {
            final short[] dstData = dstDataArrays[k];
            final short[] srcData = srcDataArrays[k];

            // intermediate buffer to hold horizontal-pass results; match src layout to include padding
            int[] tmp = new int[srcData.length];

            // horizontal pass
            int srcScanlineOffset = srcBandOffsets[k];
            int tmpScanlineOffset = srcBandOffsets[k];
            int rowsToProcess = dheight + kh - 1;
            for (int j = 0; j < rowsToProcess; j++) {
                int samples = dwidth + kw - 1;
                cum[0] = 0L;
                int sp = srcScanlineOffset;
                for (int t = 0; t < samples; t++, sp += srcPixelStride) {
                    cum[t + 1] = cum[t] + ((long) srcData[sp] & 0xFFFFL);
                }

                int tmpPixelOffset = tmpScanlineOffset;
                for (int out = 0; out < dwidth; out++) {
                    long sumRaw = cum[out + kw] - cum[out];
                    long val = (long) (sumRaw * hValue);
                    if (val < 0L) val = 0L;
                    if (val > 0xFFFFL) val = 0xFFFFL;
                    tmp[tmpPixelOffset] = (int) (val & 0xFFFF);
                    tmpPixelOffset += srcPixelStride;
                }

                srcScanlineOffset += srcScanlineStride;
                tmpScanlineOffset += srcScanlineStride;
            }

            // vertical pass
            int tmpPixelOffset = srcBandOffsets[k];
            int dstPixelOffset = dstBandOffsets[k];
            for (int i = 0; i < dwidth; i++) {
                int samples = dheight + kh - 1;
                cum[0] = 0L;
                int tp = tmpPixelOffset;
                for (int t = 0; t < samples; t++, tp += srcScanlineStride) {
                    cum[t + 1] = cum[t] + ((long) tmp[tp] & 0xFFFFFFFFL);
                }

                int outOffset = dstPixelOffset;
                for (int out = 0; out < dheight; out++) {
                    long sumRaw = cum[out + kh] - cum[out];
                    long val = (long) (sumRaw * vValue);
                    if (val < 0L) val = 0L;
                    if (val > 0xFFFFL) val = 0xFFFFL;
                    dstData[outOffset] = (short) (val & 0xFFFF);
                    outOffset += dstScanlineStride;
                }

                tmpPixelOffset += srcPixelStride;
                dstPixelOffset += dstPixelStride;
            }
        }
    }
}
