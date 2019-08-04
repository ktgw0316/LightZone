package com.lightcrafts.jai.opimage;

import com.lightcrafts.utils.OpenSimplexNoise;
import lombok.val;

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

/**
 * Created by Masahiro Kitagawa on 2018/04/11.
 */
public final class FilmGrainOpImage extends SourcelessOpImage {
    private final double featureSize;
    private final double color;
    private final double intensity;

    private static final OpenSimplexNoise noise = new OpenSimplexNoise();

    public FilmGrainOpImage(RenderedImage im, double featureSize, double color, double intensity) {
        super(new ImageLayout(im), null, im.getSampleModel(),
                im.getMinX(), im.getMinY(), im.getWidth(), im.getHeight());
        this.featureSize = featureSize;
        this.color = color;
        this.intensity = intensity;
    }

    @Override
    protected void computeRect(PlanarImage[] sources, WritableRaster dest, Rectangle destRect) {
        RasterFormatTag[] formatTags = getFormatTags();
        RasterAccessor dstAccessor =
                new RasterAccessor(dest, destRect, formatTags[0], this.getColorModel());

        switch (dstAccessor.getDataType()) {
            case DataBuffer.TYPE_USHORT:
                ushortLoop(dstAccessor);
                break;
            default:
        }

        if (dstAccessor.isDataCopy()) {
            dstAccessor.clampDataArrays();
            dstAccessor.copyDataToRaster();
        }
    }

    private void ushortLoop(RasterAccessor dst) {
        grainImage(dst.getShortDataArray(0),
                dst.getX(), dst.getY(), dst.getWidth(), dst.getHeight(),
                dst.getNumBands(), dst.getBandOffsets(),
                dst.getPixelStride(), dst.getScanlineStride());
    }

    private void grainImage(short[] dstData, int dstX, int dstY, int dstWidth, int dstHeight,
                            int numBands, int[] dstBandOffset,
                            int dstPixelStride, int dstLineStride) {
        if (color != 0) {
            for (int y = 0, pos0 = 0; y < dstHeight; ++y, pos0 += dstLineStride) {
                for (int x = 0, pos = pos0; x < dstWidth; ++x, pos += dstPixelStride) {
                    for (int c = 0; c < numBands; ++c) {
                        val value = noise.eval(
                                (dstX + x) / featureSize,
                                (dstY + y) / featureSize,
                                (c - 1) * color);
                        val rgb = (short) ((value * intensity + 1) * 32767.5);
                        dstData[pos + dstBandOffset[c]] = rgb;
                    }
                }
            }
        } else {
            for (int y = 0, pos0 = 0; y < dstHeight; ++y, pos0 += dstLineStride) {
                for (int x = 0, pos = pos0; x < dstWidth; ++x, pos += dstPixelStride) {
                    val value = noise.eval(
                            (dstX + x) / featureSize,
                            (dstY + y) / featureSize,
                            0);
                    val rgb = (short) ((value * intensity + 1) * 32767.5);
                    for (int c = 0; c < numBands; ++c) {
                        dstData[pos + dstBandOffset[c]] = rgb;
                    }
                }
            }
        }
    }
}
