/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.RasterAccessor;
import com.lightcrafts.mediax.jai.RasterFormatTag;

import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.*;
import java.util.Map;

public class HighlightRecoveryOpImage extends PointOpImage {
    private final float[] preMul;
    private final float[][] csMatrix;

    public HighlightRecoveryOpImage(RenderedImage source, float[] preMul, float[][] csMatrix, Map config) {
        super(source, new ImageLayout(source), config, true);
        permitInPlaceOperation();
        this.preMul = preMul;
        this.csMatrix = csMatrix;
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

        ushortLoop(srcAccessor, dstAccessor);
    }

    native private void floatNativeUshortLoop(short dstData[], short srcData[],
                                              int dstBandOffsets[], int srcBandOffsets[],
                                              int dstLineStride, int srcLineStride,
                                              int dstPixelStride, int srcPixelStride,
                                              int width, int height,
                                              float preMul[], float[] csMatrix);

    protected void ushortLoop(RasterAccessor src, RasterAccessor dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        short dstDataArrays[][] = dst.getShortDataArrays();
        short dstData[] = dstDataArrays[0];
        int dstBandOffsets[] = dst.getBandOffsets();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();

        short srcDataArrays[][] = src.getShortDataArrays();
        short srcData[] = srcDataArrays[0];
        int srcBandOffsets[] = src.getBandOffsets();
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();

        float csArray[] = new float[9];

        for (int i = 0; i < 3; i++)
            System.arraycopy(csMatrix[i], 0, csArray, 3*i, 3);

        floatNativeUshortLoop(srcData, dstData,
                              dstBandOffsets, srcBandOffsets,
                              dstLineStride, srcLineStride,
                              dstPixelStride, srcPixelStride,
                              width, height,
                              preMul, csArray);
    }
}
