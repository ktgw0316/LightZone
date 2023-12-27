package com.lightcrafts.jai.opimage;

import javax.media.jai.*;
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
                                KernelJAI kernel
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
//            case DataBuffer.TYPE_USHORT:
//                ushortLoop(srcAccessor, dstAccessor);
//                break;
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

        for (int k = 0; k < dnumBands; k++) {
            final int[] dstData = dstDataArrays[k];
            final int[] srcData = srcDataArrays[k];

            // horizontal pass
            int srcScanlineOffset = srcBandOffsets[k];
            int dstScanlineOffset = dstBandOffsets[k];
            for (int j = 0; j < dheight; j++) {
                int srcPixelOffset = srcScanlineOffset;
                int dstPixelOffset = dstScanlineOffset;
                float sum = 0.0f;

                for (int i = 0; i < kw; i++) {
                    sum += srcData[srcPixelOffset] * hValue;
                    srcPixelOffset += srcPixelStride;
                }
                dstData[dstPixelOffset] = (int) sum;
                dstPixelOffset += dstPixelStride;

                for (int i = kw; i < dwidth; i++) {
                    final int head = srcData[srcPixelOffset - srcPixelStride * kw];
                    final int tail = srcData[srcPixelOffset];
                    sum += ((float) tail - head) * hValue;
                    dstData[dstPixelOffset] = (int) sum;

                    srcPixelOffset += srcPixelStride;
                    dstPixelOffset += dstPixelStride;
                }
                srcScanlineOffset += srcScanlineStride;
                dstScanlineOffset += dstScanlineStride;
            }

            // vertical pass
            int srcPixelOffset = srcBandOffsets[k];
            int dstPixelOffset = dstBandOffsets[k];
            for (int i = 0; i < dwidth; i++) {
                srcScanlineOffset = srcPixelOffset;
                dstScanlineOffset = dstPixelOffset;
                float sum = 0.0f;

                for (int j = 0; j < kh; j++) {
                    sum += srcData[srcScanlineOffset] * vValue;
                    srcScanlineOffset += srcScanlineStride;
                }
                dstData[dstScanlineOffset] = (int) sum;
                dstScanlineOffset += dstScanlineStride;

                for (int j = kh; j < dheight; j++) {
                    final int head = srcData[srcScanlineOffset - srcScanlineStride * kh];
                    final int tail = srcData[srcScanlineOffset];
                    sum += ((float) tail - head) * vValue;
                    dstData[dstScanlineOffset] = (int) sum;

                    srcScanlineOffset += srcScanlineStride;
                    dstScanlineOffset += dstScanlineStride;
                }
                srcPixelOffset += srcPixelStride;
                dstPixelOffset += dstPixelStride;
            }
        }
    }
}
