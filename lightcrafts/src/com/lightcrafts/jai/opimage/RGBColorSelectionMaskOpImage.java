/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.opimage;

import javax.media.jai.PointOpImage;
import javax.media.jai.ImageLayout;
import com.lightcrafts.model.RGBColorSelection;
import com.lightcrafts.utils.ColorScience;
import com.lightcrafts.utils.LCMS;
import com.lightcrafts.jai.JAIContext;

import java.awt.image.*;
import java.awt.color.ColorSpace;
import java.awt.*;
import java.util.Map;

import sun.awt.image.ShortInterleavedRaster;
import sun.awt.image.ByteInterleavedRaster;

public class RGBColorSelectionMaskOpImage extends PointOpImage {
    private static LCMS.Transform ts = new LCMS.Transform(new LCMS.Profile(JAIContext.linearProfile), LCMS.TYPE_RGB_16,
                                                          new LCMS.Profile(JAIContext.labProfile), LCMS.TYPE_Lab_16,
                                                          LCMS.INTENT_RELATIVE_COLORIMETRIC, 0);

    private final RGBColorSelection colorSelection;
    private final float L, a, b;

    private static ImageLayout createLayout(RenderedImage source) {
        ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                                                false, false,
                                                Transparency.OPAQUE, DataBuffer.TYPE_BYTE);

        ImageLayout layout = new ImageLayout(source);
        layout.setColorModel(cm);
        layout.setSampleModel(cm.createCompatibleSampleModel(source.getWidth(), source.getHeight()));
        return layout;
    }

    public RGBColorSelectionMaskOpImage(RenderedImage source, RGBColorSelection colorSelection, Map config) {
        super(source, createLayout(source), config, true);
        this.colorSelection = colorSelection;
        short labColors[] = new short[3];

        ts.doTransform(new short[] {(short) (colorSelection.red * 0xffff),
                                    (short) (colorSelection.green * 0xffff),
                                    (short) (colorSelection.blue * 0xffff)}, labColors);

        L = (0xffff & labColors[0]) / (float) 0xffff;
        a = ((0xffff & labColors[1])) / (float) 0xffff;
        b = ((0xffff & labColors[2])) / (float) 0xffff;

        // System.out.println("RGBColorSelectionMaskOpImage - L:" + L + ", a:" + a + ", b: " + b + ", radius: " + colorSelection.radius);
    }

    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {
        ShortInterleavedRaster src = (ShortInterleavedRaster) sources[0];
        ByteInterleavedRaster dst = (ByteInterleavedRaster) dest;

        dst = (ByteInterleavedRaster) dst.createChild(destRect.x, destRect.y, destRect.width, destRect.height,
                                                      destRect.x, destRect.y, null);

        int width = dst.getWidth();
        int height = dst.getHeight();

        byte dstData[] = dst.getDataStorage();
        int dstBandOffsets[] = dst.getDataOffsets();
        int dstLineStride = dst.getScanlineStride();
        // int dstPixelStride = dst.getPixelStride();

        short srcData[] = src.getDataStorage();
        int srcBandOffsets[] = src.getDataOffsets();
        int srcLineStride = src.getScanlineStride();
        // int srcPixelStride = src.getPixelStride();

        int dstOffset = dstBandOffsets[0];

        float colorSelectionArray[] = {
            L, a, b,
            colorSelection.isColorEnabled ? colorSelection.radius : -1,
            colorSelection.isLuminosityEnabled ? colorSelection.luminosityLower : 0,
            colorSelection.isLuminosityEnabled ? colorSelection.luminosityLowerFeather : 0,
            colorSelection.isLuminosityEnabled ? colorSelection.luminosityUpper : 1,
            colorSelection.isLuminosityEnabled ? colorSelection.luminosityUpperFeather : 0
        };

        nativeUshortLoop(srcData, dstData, width, height,
                         srcBandOffsets, dstOffset,
                         srcLineStride, dstLineStride,
                         colorSelectionArray, colorSelection.isInverted);
    }

    private native void nativeUshortLoop(short srcData[], byte dstData[],
                                         int width, int height,
                                         int srcBandOffsets[], int dstOffset,
                                         int srcLineStride, int dstLineStride,
                                         float colorSelection[], boolean invert);

    private void ushortLoop(short srcData[], byte dstData[],
                            int width, int height,
                            int srcBandOffsets[], int dstOffset,
                            int srcLineStride, int dstLineStride,
                            float colorSelection[]) {
        int srcROffset = srcBandOffsets[0];
        int srcGOffset = srcBandOffsets[1];
        int srcBOffset = srcBandOffsets[2];

        float sL                        = colorSelection[0];
        float sa                        = colorSelection[1];
        float sb                        = colorSelection[2];
        float radius                    = colorSelection[3];
        float luminosityLower           = colorSelection[4];
        float luminosityLowerFeather    = colorSelection[5];
        float luminosityUpper           = colorSelection[6];
        float luminosityUpperFeather    = colorSelection[7];

        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                float L = (0xffff & srcData[3 * col + row * srcLineStride + srcROffset]);
                float a = ((0xffff & srcData[3 * col + row * srcLineStride + srcGOffset])) / (float) 0xffff;
                float b = ((0xffff & srcData[3 * col + row * srcLineStride + srcBOffset])) / (float) 0xffff;

                float brightnessMask, colorMask;

                if (radius >= 0) {
                    final float rmin = 3 * radius / 16;
                    final float rmax = 5 * radius / 16;

                    float da = sa - a;
                    float db = sb - b;
                    float m = (float) Math.sqrt(da * da + db * db);
                    if (m < rmin)
                        colorMask = 1;
                    else if (m < rmax)
                        colorMask = (rmax - m) / (rmax - rmin);
                    else
                        colorMask = 0;
                } else
                    colorMask = 1;

                if (luminosityLower > 0 || luminosityUpper < 1) {
                    float luminosity = (float) ((Math.log1p(L / 0x100))/(8 * Math.log(2)));

                    if (luminosity >= luminosityLower && luminosity <= luminosityUpper)
                        brightnessMask = 1;
                    else if (luminosity >= (luminosityLower - luminosityLowerFeather) && luminosity < luminosityLower)
                        brightnessMask = (luminosity - (luminosityLower - luminosityLowerFeather))/luminosityLowerFeather;
                    else if (luminosity > luminosityUpper && luminosity <= (luminosityUpper + luminosityUpperFeather))
                        brightnessMask = (luminosityUpper + luminosityUpperFeather - luminosity)/luminosityUpperFeather;
                    else
                        brightnessMask = 0;

                    colorMask *= brightnessMask;
                }
                
                dstData[col + row * dstLineStride + dstOffset] = (byte) (0xff * colorMask);
            }
        }
    }
}
