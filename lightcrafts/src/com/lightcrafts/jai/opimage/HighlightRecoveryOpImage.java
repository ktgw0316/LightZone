/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: May 26, 2007
 * Time: 2:23:56 PM
 */

import javax.media.jai.PointOpImage;
import javax.media.jai.ImageLayout;
import com.sun.media.jai.util.ImageUtil;
import com.lightcrafts.utils.HSB;

import java.awt.image.RenderedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.*;
import java.util.Map;

import sun.awt.image.ShortInterleavedRaster;

public class HighlightRecoveryOpImage extends PointOpImage {
    private final float[] preMul;
    private final float[][] csMatrix;
    private final float[][] wbMatrix;

    public HighlightRecoveryOpImage(RenderedImage source, float[] preMul, float[][] csMatrix, float[][] wbMatrix, Map config) {
        super(source, new ImageLayout(source), config, true);
        permitInPlaceOperation();
        this.preMul = preMul;
        this.csMatrix = csMatrix;
        this.wbMatrix = wbMatrix;
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
                                              float preMul[], float[] csMatrix, float[] wbMatrix);

    native private void intNativeUshortLoop(short dstData[], short srcData[],
                                            int dstBandOffsets[], int srcBandOffsets[],
                                            int dstLineStride, int srcLineStride,
                                            int dstPixelStride, int srcPixelStride,
                                            int width, int height,
                                            float preMul[], float[] csMatrix, float[] wbMatrix);

    native private void sseNativeUshortLoop(short dstData[], short srcData[],
                                            int dstBandOffsets[], int srcBandOffsets[],
                                            int dstLineStride, int srcLineStride,
                                            int dstPixelStride, int srcPixelStride,
                                            int width, int height,
                                            float preMul[], float[] csMatrix, float[] wbMatrix);

    native private void vecNativeUshortLoop(short dstData[], short srcData[],
                                            int dstBandOffsets[], int srcBandOffsets[],
                                            int dstLineStride, int srcLineStride,
                                            int dstPixelStride, int srcPixelStride,
                                            int width, int height,
                                            float preMul[], float[] csMatrix, float[] wbMatrix);

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

        if (true) {
            float csArray[] = new float[9];
            float wbArray[] = new float[9];

            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++) {
                    csArray[3*i+j] = csMatrix[i][j];
                    wbArray[3*i+j] = wbMatrix[i][j];
                }
            floatNativeUshortLoop(srcData, dstData,
                                  dstBandOffsets, srcBandOffsets,
                                  dstLineStride, srcLineStride,
                                  dstPixelStride, srcPixelStride,
                                  width, height,
                                  preMul, csArray, wbArray);
            return;
        }

        int srcROffset = srcBandOffsets[0];
        int srcGOffset = srcBandOffsets[1];
        int srcBOffset = srcBandOffsets[2];

        int dstROffset = dstBandOffsets[0];
        int dstGOffset = dstBandOffsets[1];
        int dstBOffset = dstBandOffsets[2];

        final int scale = 0x1000;

        int t00 = (int) (scale * csMatrix[0][0]), t01 = (int) (scale * csMatrix[0][1]), t02 = (int) (scale * csMatrix[0][2]),
            t10 = (int) (scale * csMatrix[1][0]), t11 = (int) (scale * csMatrix[1][1]), t12 = (int) (scale * csMatrix[1][2]),
            t20 = (int) (scale * csMatrix[2][0]), t21 = (int) (scale * csMatrix[2][1]), t22 = (int) (scale * csMatrix[2][2]);

        int w00 = (int) (scale * wbMatrix[0][0]), w01 = (int) (scale * wbMatrix[0][1]), w02 = (int) (scale * wbMatrix[0][2]),
            w10 = (int) (scale * wbMatrix[1][0]), w11 = (int) (scale * wbMatrix[1][1]), w12 = (int) (scale * wbMatrix[1][2]),
            w20 = (int) (scale * wbMatrix[2][0]), w21 = (int) (scale * wbMatrix[2][1]), w22 = (int) (scale * wbMatrix[2][2]);

        int pm[] = new int[] {(int) (scale * preMul[0]), (int) (scale * preMul[1]), (int) (scale * preMul[2])};

        final int threshold = (int) (0.8 * 0xffff);
        final int maximum = (int) (1 * 0xffff);

        int raw[] = new int[3];
        float hsb[] = new float[3];
        float rgb3[] = new float[3];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int srcPixOffset = srcPixelStride * col + row * srcLineStride;
                int rr = raw[0] = 0xffff & srcData[srcPixOffset + srcROffset];
                int rg = raw[1] = 0xffff & srcData[srcPixOffset + srcGOffset];
                int rb = raw[2] = 0xffff & srcData[srcPixOffset + srcBOffset];

                int r = (t00 * rr + t01 * rg + t02 * rb)/scale;
                int g = (t10 * rr + t11 * rg + t12 * rb)/scale;
                int b = (t20 * rr + t21 * rg + t22 * rb)/scale;

                int max = 0;
                int sum = 0;
                int saturated = 0;
                for (int i = 0; i < 3; i++) {
                    int val = Math.min((pm[i] * raw[i]) / scale, maximum);
                    if (val > threshold) {
                        saturated++;
                        if (val > max)
                            max = val;
                    }
                    sum += val;
                }

                if (saturated > 0) {
                    float m1 = (maximum - max) / (float) (maximum - threshold);
                    float m2 = (maximum - sum/3) / (float) (maximum - threshold);
                    float s = (maximum - sum/3) / (float) maximum;

                    for (int i = 1; i < saturated; i++)
                        s *= s;

                    float m = s * m2 + (1 - s) * m1;

                    if (m < 1) {
                        rgb3[0] = r;
                        rgb3[1] = g;
                        rgb3[2] = b;
                        HSB.fromRGB(rgb3, hsb);
                        hsb[1] *= m; // Math.sqrt(m);
                        HSB.toRGB(hsb, rgb3);
                        r = (int) rgb3[0];
                        g = (int) rgb3[1];
                        b = (int) rgb3[2];
                    }
                }

                if (wbMatrix != null) {
                    int wbr = (w00 * r + w01 * g + w02 * b)/scale;
                    int wbg = (w10 * r + w11 * g + w12 * b)/scale;
                    int wbb = (w20 * r + w21 * g + w22 * b)/scale;
                    r = wbr; g = wbg; b = wbb;
                }

                int dstPixOffset = dstPixelStride * col + row * dstLineStride;
                dstData[dstPixOffset + dstROffset] = ImageUtil.clampUShort(r);
                dstData[dstPixOffset + dstGOffset] = ImageUtil.clampUShort(g);
                dstData[dstPixOffset + dstBOffset] = ImageUtil.clampUShort(b);
            }
        }
    }
}
