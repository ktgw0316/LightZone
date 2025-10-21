/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.jai.opimage;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;

import org.eclipse.imagen.AreaOpImage;
import org.eclipse.imagen.BorderExtender;
import org.eclipse.imagen.ImageLayout;
import org.eclipse.imagen.RasterAccessor;
import org.eclipse.imagen.RasterFormatTag;

import java.util.Map;

public final class FastBilateralFilterOpImage extends AreaOpImage {
    private final float sigma_d;
    private final float sigma_r;

    private static final BorderExtender copyExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);

    private static final float[] transform = new float[0x10000];

    static {
        for (int i = 0; i < 0x10000; i++) {
            float x = i / (float) 0x10000;
            transform[i] = (float) (Math.log1p(x)/Math.log(2) + 1.5 * Math.exp(-10*x) * Math.pow(x, 0.7));
        }
    }

    private static ImageLayout fblLayout(RenderedImage source) {

        class TwoComponentsColorSpace extends ColorSpace {

            private TwoComponentsColorSpace() {
                super(ColorSpace.TYPE_2CLR, 2);
            }

            @Override
            public float[] toRGB(float[] colorvalue) {
                if(colorvalue.length < 2)
                    throw new ArrayIndexOutOfBoundsException("colorvalue.length < 2");
                return colorvalue;
            }

            @Override
            public float[] fromRGB(float[] rgbvalue) {
                if(rgbvalue.length < 2)
                    throw new ArrayIndexOutOfBoundsException("rgbvalue.length < 3");
                return new float[] {rgbvalue[0], rgbvalue[1]};
            }

            @Override
            public float[] toCIEXYZ(float[] colorvalue) {
                if(colorvalue.length < 2)
                    throw new ArrayIndexOutOfBoundsException("colorvalue.length < 2");
                return colorvalue;
            }

            @Override
            public float[] fromCIEXYZ(float[] xyzvalue) {
                if(xyzvalue.length < 2)
                    throw new ArrayIndexOutOfBoundsException("xyzvalue.length < 3");
                return new float[] {xyzvalue[0], xyzvalue[1]};
            }
        }

        // SampleModel sm = new ComponentSampleModel(DataBuffer.TYPE_USHORT, source.getWidth(), source.getHeight(), 2, 2*source.getWidth(), new int[]{0, 2});

        ColorModel cm = new ComponentColorModel(new TwoComponentsColorSpace(),
                                                false, false, Transparency.OPAQUE, DataBuffer.TYPE_USHORT);
        SampleModel sm = cm.createCompatibleSampleModel(source.getWidth(), source.getHeight());

        return new ImageLayout(source.getMinX(), source.getMinY(),
                        source.getWidth(), source.getHeight(),
                        source.getTileGridXOffset(), source.getTileGridYOffset(),
                        source.getTileWidth(), source.getTileHeight(),
                        sm, cm);
    }

    public FastBilateralFilterOpImage(RenderedImage source, Map config, float sigma_d, float sigma_r) {
        super(source,
              fblLayout(source),
              config,
              true,
              copyExtender,
              (int) (2*Math.ceil(sigma_d)),
              (int) (2*Math.ceil(sigma_d)),
              (int) (2*Math.ceil(sigma_d)),
              (int) (2*Math.ceil(sigma_d)));

        this.sigma_d = sigma_d;
        this.sigma_r = sigma_r;
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

        short[][] dstDataArrays = dst.getShortDataArrays();
        int[] dstBandOffsets = dst.getBandOffsets();
        int dstScanlineStride = dst.getScanlineStride();

        short[][] srcDataArrays = src.getShortDataArrays();
        int[] srcBandOffsets = src.getBandOffsets();
        int srcScanlineStride = src.getScanlineStride();

        short[] dstData = dstDataArrays[0];
        short[] srcData = srcDataArrays[0];

        if (src.getNumBands() == 1)
            fastBilateralFilterMono(srcData, dstData,
                                    sigma_d, sigma_r,
                                    swidth, sheight,
                                    src.getPixelStride(), dst.getPixelStride(),
                                    srcBandOffsets[0], dstBandOffsets[0],
                                    srcScanlineStride, dstScanlineStride,
                                    transform);
        else
            fastBilateralFilterChroma(srcData, dstData,
                                     sigma_d, sigma_r,
                                     swidth, sheight,
                                     src.getPixelStride(), dst.getPixelStride(),
                                     srcBandOffsets[0], srcBandOffsets[1], srcBandOffsets[2],
                                     dstBandOffsets[0], dstBandOffsets[1], dstBandOffsets[2],
                                     srcScanlineStride, dstScanlineStride);
    }

    static native void fastBilateralFilterMono(short[] srcData, short[] destData,
                                               float sigma_s, float sigma_r,
                                               int width, int height,
                                               int srcPixelStride, int destPixelStride,
                                               int srcOffset, int destOffset,
                                               int srcLineStride, int destLineStride,
                                               float[] transform);

    static native void fastBilateralFilterChroma(short[] srcData, short[] destData,
                                                 float sigma_s, float sigma_r,
                                                 int width, int height,
                                                 int srcPixelStride, int destPixelStride,
                                                 int srcROffset, int srcGOffset, int srcBOffset,
                                                 int destROffset, int destGOffset, int destBOffset,
                                                 int srcLineStride, int destLineStride);

}
