/* Copyright (C) 2015- Masahiro Kitagawa */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.utils.Lensfun;

import javax.media.jai.BorderExtender;
import javax.media.jai.GeometricOpImage;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.Map;

public class DistortionOpImage extends GeometricOpImage {

    private Lensfun lf;

    public DistortionOpImage(RenderedImage source, Map configuration, BorderExtender extender, Lensfun lf) {
        super(vectorize(source), null, configuration, true, extender, null);
        this.lf = lf;
    }

    @Override
    protected Rectangle forwardMapRect(Rectangle sourceRect, int sourceIndex) {
        return null;
    }

    @Override
    protected Rectangle backwardMapRect(Rectangle destRect, int sourceIndex) {
        if (sourceIndex != 0) {
            return null;
        }
        synchronized(this) {
            return lf.backwardMapRect(destRect);
        }
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

        if (dstAccessor.getDataType() == DataBuffer.TYPE_USHORT) {
            ushortLoop(srcAccessor, dstAccessor);
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
        final int dstX = dst.getX();
        final int dstY = dst.getY();
        final int dstWidth = dst.getWidth();
        final int dstHeight = dst.getHeight();

        short[][] dstDataArrays = dst.getShortDataArrays();
        final int[] dstBandOffsets = dst.getBandOffsets();
        final int dstPixelStride = dst.getPixelStride();
        final int dstScanlineStride = dst.getScanlineStride();

        final short[][] srcDataArrays = src.getShortDataArrays();
        final int[] srcBandOffsets = src.getBandOffsets();
        final int srcPixelStride = src.getPixelStride();
        final int srcScanlineStride = src.getScanlineStride();

        final int srcX = src.getX();
        final int srcY = src.getY();
        final int srcWidth = src.getWidth();
        final int srcHeight = src.getHeight();

        short[] dstData = dstDataArrays[0];
        short[] srcData = srcDataArrays[0];

        if (src.getNumBands() == 1) {
            // TODO:
        }
        else if (src.getNumBands() == 3) {
            synchronized(this) {
                lf.distortionColor(srcData, dstData,
                        srcX, srcY, srcWidth, srcHeight,
                        dstX, dstY, dstWidth, dstHeight,
                        srcPixelStride, dstPixelStride,
                        srcBandOffsets[0], srcBandOffsets[1], srcBandOffsets[2],
                        dstBandOffsets[0], dstBandOffsets[1], dstBandOffsets[2],
                        srcScanlineStride, dstScanlineStride);
            }
        }
    }
}
