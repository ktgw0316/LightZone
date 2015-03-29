/* Copyright (C) 2015 Masahiro Kitagawa */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.mediax.jai.BorderExtender;
import com.lightcrafts.mediax.jai.GeometricOpImage;
import com.lightcrafts.mediax.jai.Interpolation;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.RasterFormatTag;
import com.lightcrafts.mediax.jai.RasterAccessor;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.DataBuffer;
import java.awt.*;
import java.util.Map;

public class DistortionOpImage extends GeometricOpImage {

    private final int fullWidth;
    private final int fullHeight;
    private String lensName = "";

    // Coeffs for 5th order polynomial distortion model
    // c.f. http://www.imatest.com/docs/distortion.html
    private float k1 = 0f;

    public DistortionOpImage(RenderedImage sources, Map configuration, BorderExtender extender,
            float k1) {
        super(OpImage.vectorize(sources), null, configuration, true, extender,
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR));

        fullWidth  = sources.getWidth();
        fullHeight = sources.getHeight();
        this.k1 = k1;
    }

    public DistortionOpImage(RenderedImage sources, Map configuration, BorderExtender extender,
            String lensName) {
        super(OpImage.vectorize(sources), null, configuration, true, extender,
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR));

        fullWidth  = sources.getWidth();
        fullHeight = sources.getHeight();
        this.lensName = lensName;
    }

    @Override
    protected Rectangle forwardMapRect(Rectangle sourceRect, int sourceIndex) {
        if (sourceIndex != 0)
            return null;
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected Rectangle backwardMapRect(Rectangle destRect, int sourceIndex) {
        if (sourceIndex != 0)
            return null;
        // TODO Auto-generated method stub
        return null;
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

        short dstDataArrays[][] = dst.getShortDataArrays();
        final int dstBandOffsets[] = dst.getBandOffsets();
        final int dstScanlineStride = dst.getScanlineStride();

        final short srcDataArrays[][] = src.getShortDataArrays();
        final int srcBandOffsets[] = src.getBandOffsets();
        final int srcScanlineStride = src.getScanlineStride();

        short dstData[] = dstDataArrays[0];
        short srcData[] = srcDataArrays[0];

        if (src.getNumBands() == 3) {
            if (lensName.isEmpty()) {
                // DEBUG
                System.out.println("k1 = " + k1);

                synchronized(this) {
                    distortion(srcData, dstData,
                               fullWidth, fullHeight,
                               dstX, dstY, dstWidth, dstHeight,
                               srcBandOffsets[0], srcBandOffsets[1], srcBandOffsets[2],
                               dstBandOffsets[0], dstBandOffsets[1], dstBandOffsets[2],
                               srcScanlineStride, dstScanlineStride, k1);
                }
            }
            else {
                // DEBUG
                System.out.println("lens = " + lensName);

                synchronized(this) {
                    lensfun(srcData, dstData,
                            fullWidth, fullHeight,
                            dstX, dstY, dstWidth, dstHeight,
                            srcBandOffsets[0], srcBandOffsets[1], srcBandOffsets[2],
                            dstBandOffsets[0], dstBandOffsets[1], dstBandOffsets[2],
                            srcScanlineStride, dstScanlineStride, lensName);
                }
            }
        }
    }

    static native void distortion(short srcData[], short destData[],
                                  int fullWidth, int fullHeight,
                                  int rectX, int rectY, int rectWidth, int rectHeight,
                                  int srcROffset, int srcGOffset, int srcBOffset,
                                  int destROffset, int destGOffset, int destBOffset,
                                  int srcLineStride, int destLineStride,
                                  float k1);

    static native void lensfun(short srcData[], short destData[],
                               int fullWidth, int fullHeight,
                               int rectX, int rectY, int rectWidth, int rectHeight,
                               int srcROffset, int srcGOffset, int srcBOffset,
                               int destROffset, int destGOffset, int destBOffset,
                               int srcLineStride, int destLineStride,
                               String lensName);
}
