package com.lightcrafts.utils;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;

class RawlerTest {

    private final String filename = "/Users/masahiro/Downloads/273A6915.CR3";
    @Test
    void getRawWidth() {
        final int ret = Rawler.getRawWidth(filename);
        System.out.println("RawlerTest.getRawWidth: " + ret);
    }

    @Test
    void getRawHeight() {
        final int ret = Rawler.getRawHeight(filename);
        System.out.println("RawlerTest.getRawHeight: " + ret);
    }

    @Test
    void getRawData() throws IOException {
        final int width = Rawler.getRawWidth(filename);
        final int height = Rawler.getRawHeight(filename);
        final short[] data = Rawler.getRawData(filename);
        final var dataBuf = new DataBufferUShort(data, width * height);
        final var bands = 1; // raw.cpp;
        final var bandOffsets = bands == 3 ? new int[]{0, 1, 2} : new int[]{0};
        final var raster = Raster.createInterleavedRaster(dataBuf, width, height,
                bands * width, bands, bandOffsets, null);
        final var cm = Rawler.getColorModel(bands);
        final var bi = new BufferedImage(cm, raster, false, null);
        ImageIO.write(bi, "png", new File("/Users/masahiro/Downloads/YIMG_0647.png"));
    }

    @Test
    void getSrgb() throws IOException {
        final int width = Rawler.getRawWidth(filename);
        final int height = Rawler.getRawHeight(filename);
        final short[] data = Rawler.getSrgb(filename);
        final var dataBuf = new DataBufferUShort(data, data.length);
        final var bands = 3;
        final var bandOffsets = new int[]{0, 1, 2};
        System.out.println(width);
        System.out.println(height);
        final var raster = Raster.createInterleavedRaster(dataBuf, width, height,
                bands * width, bands, bandOffsets, null);
        final var cm = Rawler.getColorModel(bands);
        final var bi = new BufferedImage(cm, raster, false, null);
        ImageIO.write(bi, "png", new File(filename + ".srgb.png"));
    }
}