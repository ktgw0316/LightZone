/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.jai.opimage;

import com.sun.media.jai.util.ImageUtil;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;

import javax.media.jai.AreaOpImage;
import javax.media.jai.BorderExtender;
import javax.media.jai.ImageLayout;
import javax.media.jai.RasterAccessor;
import javax.media.jai.RasterFormatTag;

import java.util.Map;

public final class FastBilateralFilterOpImage extends AreaOpImage {
    private final float sigma_d;
    private final float sigma_r;

    private static final BorderExtender copyExtender = BorderExtender.createInstance(BorderExtender.BORDER_COPY);

    private static final float[] transform = new float[0x10000];

    static {
        for (int i = 0; i < 0x10000; i++) {
            float x = i / (float) 0x10000;
            transform[i] = (float) (Math.log1p(x) / Math.log(2) + 1.5 * Math.exp(-10 * x) * Math.pow(x, 0.7));
        }
    }

    private static ImageLayout fblLayout(RenderedImage source) {

        class TwoComponentsColorSpace extends ColorSpace {

            private TwoComponentsColorSpace() {
                super(ColorSpace.TYPE_2CLR, 2);
            }

            @Override
            public float[] toRGB(float[] colorvalue) {
                if (colorvalue.length < 2)
                    throw new ArrayIndexOutOfBoundsException("colorvalue.length < 2");
                return colorvalue;
            }

            @Override
            public float[] fromRGB(float[] rgbvalue) {
                if (rgbvalue.length < 2)
                    throw new ArrayIndexOutOfBoundsException("rgbvalue.length < 3");
                return new float[]{rgbvalue[0], rgbvalue[1]};
            }

            @Override
            public float[] toCIEXYZ(float[] colorvalue) {
                if (colorvalue.length < 2)
                    throw new ArrayIndexOutOfBoundsException("colorvalue.length < 2");
                return colorvalue;
            }

            @Override
            public float[] fromCIEXYZ(float[] xyzvalue) {
                if (xyzvalue.length < 2)
                    throw new ArrayIndexOutOfBoundsException("xyzvalue.length < 3");
                return new float[]{xyzvalue[0], xyzvalue[1]};
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
                (int) (2 * Math.ceil(sigma_d)),
                (int) (2 * Math.ceil(sigma_d)),
                (int) (2 * Math.ceil(sigma_d)),
                (int) (2 * Math.ceil(sigma_d)));

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

    private void ushortLoop(RasterAccessor src, RasterAccessor dst) {
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

    static void fastBilateralFilterMono(short[] srcData, short[] destData,
                                               float sigma_s, float sigma_r,
                                               int width, int height,
                                               int srcPixelStride, int destPixelStride,
                                               int srcOffset, int destOffset,
                                               int srcLineStride, int destLineStride,
                                               float[] transform) {
        final float inv_max = 1.0f / 0xffff;
        final float inv_sq_sigma_s = 1.0f / (sigma_s * sigma_s);

        final var image = new float[height * width];
        final var profile = new float[height * width];

        var srcPtr0 = srcOffset;
        var bufPtr0 = 0;
        for(int y = 0; y < height; y++) {
            var srcPtr = srcPtr0;
            var bufPtr = bufPtr0;
            for(int x = 0; x < width; x++) {
                final int g = srcData[srcPtr] & 0xffff; // convert to unsigned
                image[bufPtr] = g * inv_max;
                profile[bufPtr] = transform[g];

                srcPtr += srcPixelStride;
                bufPtr++;
            }
            srcPtr0 += srcLineStride;
            bufPtr0 += width;
        }

        final int padding = 2 * (int) Math.ceil(sigma_s);
        final int paddedWidth = width - 2 * padding;
        final int paddedHeight = height - 2 * padding;
        var filtered_image = new float[paddedHeight * paddedWidth];
        var weight = new float[paddedHeight * paddedWidth];

        fastLBF(image, profile, width, height, sigma_s, sigma_r, weight, filtered_image);

        var destPtr0 = destOffset;
        bufPtr0 = 0;
        for(int y = 0; y < paddedHeight; y++) {
            var destPtr = destPtr0;
            var bufPtr = bufPtr0;
            for(int x = 0; x < paddedWidth; x++) {
                final float BF = filtered_image[bufPtr];
                final float W = weight[bufPtr] * inv_sq_sigma_s;
                final short bf = clampRoundUShort(BF * 0xffff);
                final short w =  clampRoundUShort(W * 0xffff);
                destData[destPtr] = bf;
                destData[destPtr + 1] = w;

                destPtr += destPixelStride;
                bufPtr++;
            }
            destPtr0 += destLineStride;
            bufPtr0 += paddedWidth;
        }
    }

    static void fastBilateralFilterChroma(short[] srcData, short[] destData,
                                                 float sigma_s, float sigma_r,
                                                 int width, int height,
                                                 int srcPixelStride, int destPixelStride,
                                                 int srcROffset, int srcGOffset, int srcBOffset,
                                                 int destROffset, int destGOffset, int destBOffset,
                                                 int srcLineStride, int destLineStride) {
        final float inv_max = 1.0f / 0xffff;

        final var a_image = new float[height * width];
        final var b_image = new float[height * width];

        var srcGPtr0 = srcGOffset;
        var srcBPtr0 = srcBOffset;
        var bufPtr0 = 0;
        for(int y = 0; y < height; y++) {
            var srcGPtr = srcGPtr0;
            var srcBPtr = srcBPtr0;
            var bufPtr = bufPtr0;
            for(int x = 0; x < width; x++) {
                final int A = srcData[srcGPtr] & 0xffff;
                final int B = srcData[srcBPtr] & 0xffff;
                a_image[bufPtr] = A * inv_max;
                b_image[bufPtr] = B * inv_max;

                srcGPtr += srcPixelStride;
                srcBPtr += srcPixelStride;
                bufPtr++;
            }
            srcGPtr0 += srcLineStride;
            srcBPtr0 += srcLineStride;
            bufPtr0 += width;
        }

        final int padding = 2 * (int) Math.ceil(sigma_s);
        final int paddedWidth = width - 2 * padding;
        final int paddedHeight = height - 2 * padding;
        var filtered_a_image = new float[paddedHeight * paddedWidth];
        var filtered_b_image = new float[paddedHeight * paddedWidth];

        fastLBF(a_image, a_image, width, height, sigma_s, sigma_r, filtered_a_image, filtered_a_image);
        fastLBF(b_image, b_image, width, height, sigma_s, sigma_r, filtered_b_image, filtered_b_image);

        var destRPtr0 = destROffset;
        var destGPtr0 = destGOffset;
        var destBPtr0 = destBOffset;
        var srcRPtr0 = srcLineStride * padding + srcPixelStride * padding + srcROffset;
        bufPtr0 = padding * width + padding;
        for(int y = 0; y < paddedHeight; y++) {
            var destRPtr = destRPtr0;
            var destGPtr = destGPtr0;
            var destBPtr = destBPtr0;
            var srcRPtr = srcRPtr0;
            var bufPtr = bufPtr0;
            for(int x = 0; x < paddedWidth; x++) {
                final float A = filtered_a_image[bufPtr];
                final float B = filtered_b_image[bufPtr];
                destData[destRPtr] = srcData[srcRPtr]; // l
                destData[destGPtr] = clampRoundUShort(A * 0xffff); // a
                destData[destBPtr] = clampRoundUShort(B * 0xffff); // b

                destRPtr += destPixelStride;
                destGPtr += destPixelStride;
                destBPtr += destPixelStride;
                bufPtr++;
            }
            destRPtr0 += destLineStride;
            destGPtr0 += destLineStride;
            destBPtr0 += destLineStride;
            srcRPtr0 += srcLineStride;
            bufPtr0 += paddedWidth;
        }
    }

    private static void fastLBF(float[] input, float[] base,
                                int width, int height,
                                float spaceSigma, float rangeSigma,
                                float[] weight, float[] result) {
        final int paddingXY = 2;
        final int paddingZ = 2;
        final int padding = (int) (2 * Math.ceil(spaceSigma));

        final int resultWidth = width - 2 * padding;
        final int resultHeight = height - 2 * padding;

        final float baseMin = 0;
        final float baseDelta = 1;

        final var smallWidth = (int) ((resultWidth - 1) / spaceSigma) + 2 + 2 * paddingXY;
        final var smallHeight = (int) ((resultHeight - 1) / spaceSigma) + 2 + 2 * paddingXY;
        final var smallDepth = (int) (baseDelta / rangeSigma) + 1 + 2 * paddingZ;

        final float[][][][] data = new float[smallWidth][smallHeight][smallDepth][2];

        for (int x = 0; x < width; x++) {
            final int smallX = (int) (x / spaceSigma + 0.5);

            for (int y = 0; y < height; y++) {
                final var z = base[x + y * width] - baseMin;

                final var smallY = (int) (y / spaceSigma + 0.5);
                final var smallZ = (int) (z / rangeSigma + 0.5) + paddingZ;

                if (smallX < smallWidth && smallY < smallHeight) {
                    data[smallX][smallY][smallZ][0] += input[x + y * width];
                    data[smallX][smallY][smallZ][1] += 1.0f;
                }
            }
        }

        for (int nIter = 0; nIter < 2; nIter++) {
            float[][][][] buffer = data.clone();

            for (int x = 1; x < smallWidth - 1; x++) {
                for (int y = 1; y < smallHeight - 1; y++) {
                    for (int z = 1; z < smallDepth - 1; z++) {
                        for (int i = 0; i < 2; i++) {
                            data[x][y][z][i] = (
                                    buffer[x - 1][y][z][i] + buffer[x + 1][y][z][i] +
                                    buffer[x][y - 1][z][i] + buffer[x][y + 1][z][i] +
                                    buffer[x][y][z - 1][i] + buffer[x][y][z + 1][i] +
                                    3 * 2 * buffer[x][y][z][i]) / 4.0f;
                        }
                    }
                }
            }
        }

        for (int y = 0; y < resultHeight; y++) {
            for (int x = 0; x < resultWidth; x++) {
                final float z = base[x + padding + (y + padding) * width] - baseMin;

                final float[] D = trilinearInterpolation(data,
                        smallWidth, smallHeight, smallDepth,
                        x / spaceSigma + paddingXY,
                        y / spaceSigma + paddingXY,
                        z / rangeSigma + paddingZ);

                weight[x + y * resultWidth] = D[1];
                result[x + y * resultWidth] = D[0] / D[1];
            }
        }
    }

    private static int clamp(int value, int max) {
        return Math.max(0, Math.min(max, value));
    }

    private static short clampRoundUShort(float in) {
        return (short) (in > 65535.0F ? 65535 : (in >= 0.0F ? ((int)(in + 0.5F)) : 0));
    }

    private static float[] trilinearInterpolation(float[][][][] array, int x_size, int y_size, int z_size,
                                                float x, float y, float z) {
        final int x_index  = clamp((int) x, x_size-1);
        final int xx_index = clamp(x_index+1, x_size-1);

        final int y_index  = clamp((int) y, y_size-1);
        final int yy_index = clamp(y_index+1, y_size-1);

        final int z_index  = clamp((int) z, z_size-1);
        final int zz_index = clamp(z_index+1, z_size-1);

        final float x_alpha = x - x_index;
        final float y_alpha = y - y_index;
        final float z_alpha = z - z_index;

        final var result = new float[2];
        for (int i = 0; i < 2; i++) {
            result[i] =
                    (1 - x_alpha) * (1 - y_alpha) * (1 - z_alpha) * array[x_index ][y_index ][z_index ][i] +
                    x_alpha       * (1 - y_alpha) * (1 - z_alpha) * array[xx_index][y_index ][z_index ][i] +
                    (1 - x_alpha) * y_alpha       * (1 - z_alpha) * array[x_index ][yy_index][z_index ][i] +
                    x_alpha       * y_alpha       * (1 - z_alpha) * array[xx_index][yy_index][z_index ][i] +
                    (1 - x_alpha) * (1 - y_alpha) * z_alpha       * array[x_index ][y_index ][zz_index][i] +
                    x_alpha       * (1 - y_alpha) * z_alpha       * array[xx_index][y_index ][zz_index][i] +
                    (1 - x_alpha) * y_alpha       * z_alpha       * array[x_index ][yy_index][zz_index][i] +
                    x_alpha       * y_alpha       * z_alpha       * array[xx_index][yy_index][zz_index][i];
        }
        return result;
    }
}
