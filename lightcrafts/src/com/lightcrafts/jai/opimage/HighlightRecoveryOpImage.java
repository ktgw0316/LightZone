/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import com.lightcrafts.mediax.jai.PointOpImage;
import com.lightcrafts.mediax.jai.ImageLayout;

import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.*;
import java.util.Map;

import sun.awt.image.ShortInterleavedRaster;

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
        ushortLoop((ShortInterleavedRaster) sources[0], (ShortInterleavedRaster) dest);
    }

    native private void floatNativeUshortLoop(short dstData[], short srcData[],
                                              int dstBandOffsets[], int srcBandOffsets[],
                                              int dstLineStride, int srcLineStride,
                                              int dstPixelStride, int srcPixelStride,
                                              int width, int height,
                                              float preMul[], float[] csMatrix);

    protected void ushortLoop(ShortInterleavedRaster src, ShortInterleavedRaster dst) {
        int width = src.getWidth();
        int height = src.getHeight();

        short dstData[] = dst.getDataStorage();
        int dstBandOffsets[] = dst.getDataOffsets();
        int dstLineStride = dst.getScanlineStride();
        int dstPixelStride = dst.getPixelStride();

        short srcData[] = src.getDataStorage();
        int srcBandOffsets[] = src.getDataOffsets();
        int srcLineStride = src.getScanlineStride();
        int srcPixelStride = src.getPixelStride();

        float csArray[] = new float[9];

        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                csArray[3*i+j] = csMatrix[i][j];
        
        floatNativeUshortLoop(srcData, dstData,
                              dstBandOffsets, srcBandOffsets,
                              dstLineStride, srcLineStride,
                              dstPixelStride, srcPixelStride,
                              width, height,
                              preMul, csArray);
    }
}
