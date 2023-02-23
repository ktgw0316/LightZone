/* Copyright (C) 2023-     Masahiro Kitagawa */

package com.lightcrafts.utils;

import com.lightcrafts.image.metadata.MetadataUtil;
import com.lightcrafts.image.metadata.providers.*;
import com.lightcrafts.jai.JAIContext;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.IOException;
import java.util.Date;

import static java.awt.Transparency.OPAQUE;
import static java.awt.image.DataBuffer.TYPE_USHORT;

/**
 * Get raw image data by interfacing with rawler
 */
@RequiredArgsConstructor
public final class Rawler implements
    ApertureProvider, CaptureDateTimeProvider, FocalLengthProvider,
    ISOProvider, MakeModelProvider, ShutterSpeedProvider, WidthHeightProvider {

    static {
        System.loadLibrary("rawler_jni");
    }

    private static native RawlerRawImage decode(String filename);

    public final String filename;

    private RawlerRawImage _raw = null;

    // DEBUG
    static native int getRawWidth(String filename);
    static native int getRawHeight(String filename);
    static native short[] getRawData(String filename);
    static native short[] getSrgb(String filename);

    @NotNull
    public RenderedImage getImage() throws IOException {
        val raw = getRaw();
        assert raw.bps == 16;  // USHORT
        val dataBuf = new DataBufferUShort(raw.data, raw.width * raw.height);
        val bands = raw.cpp;
        val bandOffsets = bands == 3 ? new int[]{0, 1, 2} : new int[]{0};
        val raster = Raster.createInterleavedRaster(dataBuf, raw.width, raw.height,
                bands * raw.width, bands, bandOffsets, null);
        val cm = getColorModel(bands);
        return new BufferedImage(cm, raster, false, null);
    }

    @NotNull
    private RawlerRawImage getRaw() {
        if (_raw == null) {
            _raw = decode(filename);
        }
        return _raw;
    }

    @NotNull
    static ColorModel getColorModel(int bands) {
        return bands == 1
                ? new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_GRAY),
                    false, false, OPAQUE, TYPE_USHORT)
                : JAIContext.colorModel_linear16;
    }

    @RequiredArgsConstructor
    public final class RawlerRawImage {
        public final String make;
        public final String model;
        public final String clean_make;
        public final int width;
        public final int height;

        /// number of components per pixel (1 for bayer, 3 for RGB images)
        public final int cpp;

        /// Bits per pixel
        public final int bps;

        /// whitebalance coefficients encoded in the file in RGBE order
//        public final float[] wb_coeffs;

        /// image whitelevels in RGBE order
//        public final short[] whitelevels;

        /// image blacklevels in RGBE order
//        public final short[] blacklevels;

        /// matrix to convert XYZ to camera RGBE
//        public final float[][] xyz_to_cam;

        /// color filter array
//        cfa: CFA,

        /// image data itself, has `width`\*`height`\*`cpp` elements
        final short[] data;
    }

    @Override
    public float getAperture() {
        return 0; // TODO
    }

    @Override
    public Date getCaptureDateTime() {
        return null; // TODO
    }

    @Override
    public float getFocalLength() {
        return 0; // TODO
    }

    @Override
    public int getISO() {
        return 0; // TODO
    }

    @Override
    public String getCameraMake(boolean includeModel) {
        val raw = getRaw();
        val make = raw.clean_make.toUpperCase();
        if (!includeModel)
            return make;
        return MetadataUtil.undupMakeModel(make, raw.model.toUpperCase());
    }

    @Override
    public float getShutterSpeed() {
        return 0; // TODO
    }

    @Override
    public int getImageHeight() {
        return getRaw().height;
    }

    @Override
    public int getImageWidth() {
        return getRaw().width;
    }
}
/* vim:set et sw=4 ts=4: */