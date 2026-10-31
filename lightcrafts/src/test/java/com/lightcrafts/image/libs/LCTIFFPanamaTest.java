package com.lightcrafts.image.libs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.Point;
import java.awt.Transparency;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static com.lightcrafts.image.metadata.TIFFTags.TIFF_IMAGE_WIDTH;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_PAGE_NUMBER;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_SOFTWARE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LCTIFFPanamaTest {

    @TempDir
    Path tempDir;

    @Test
    void writesAndReadsStripedTiffWithMetadataAndIccProfile() throws Exception {
        final Path path = tempDir.resolve("striped.tif");
        final BufferedImage source = solidImage(24, 40, 184, 96, 42);
        final ICC_Profile profile = ICC_Profile.getInstance(ColorSpace.CS_sRGB);

        try (var writer = new LCTIFFWriter(path.toString(), source.getWidth(), source.getHeight())) {
            assertTrue(writer.setIntField(TIFF_IMAGE_WIDTH, source.getWidth()));
            writer.setICCProfile(profile);
            assertTrue(
                    TIFFNative.writerSetString(
                            writer.nativeHandle(),
                            TIFF_SOFTWARE,
                            "LC TIFF Panama test\0".getBytes(StandardCharsets.UTF_8)));
            writer.putImageStriped(source, null);
        }

        try (var reader = new LCTIFFReader(path.toString())) {
            assertEquals(source.getWidth(), reader.getIntField(TIFF_IMAGE_WIDTH));
            assertEquals("LC TIFF Panama test", reader.getStringField(TIFF_SOFTWARE));
            assertTrue(java.util.Arrays.equals(profile.getData(), reader.getICCProfile().getData()));
            assertTrue(reader.getNumberOfStrips() > 1);
            assertEquals(-1, reader.getIntField2(TIFF_PAGE_NUMBER, true));
            final var decoded = reader.getImage();
            assertNotNull(decoded);
            final int pixel = decoded.getData().getPixel(12, 20, (int[]) null)[0];
            assertEquals(184, pixel, 4);
        }
    }

    @Test
    void writesAndReadsSixteenBitStripData() throws Exception {
        final Path path = tempDir.resolve("sixteen-bit.tif");
        final BufferedImage source = solidGrayImage(16, 40, 32768);

        try (var writer = new LCTIFFWriter(path.toString(), source.getWidth(), source.getHeight())) {
            writer.putImageStriped(source, null);
        }

        try (var reader = new LCTIFFReader(path.toString())) {
            final var decoded = reader.getImage();
            assertNotNull(decoded);
            assertEquals(32768, decoded.getData().getSample(8, 24, 0));
        }
    }

    @Test
    void missingTiffFileReportsAnImageLibraryException() {
        assertThrows(
                LCImageLibException.class,
                () -> new LCTIFFReader(tempDir.resolve("missing.tif").toString()));
    }

    @Test
    void writesAndReadsTiledTiff() throws Exception {
        final Path path = tempDir.resolve("tiled.tif");
        final BufferedImage source = solidImage(32, 32, 34, 144, 212);

        try (var writer = new LCTIFFWriter(path.toString(), source.getWidth(), source.getHeight())) {
            writer.putImageTiled(source, null);
        }

        try (var reader = new LCTIFFReader(path.toString())) {
            assertTrue(reader.isTiled());
            assertTrue(reader.getNumberOfTiles() > 0);
            final var decoded = reader.getImage();
            assertNotNull(decoded);
            final int[] pixel = decoded.getData().getPixel(16, 16, (int[]) null);
            assertEquals(212, pixel[0], 4);
            assertEquals(144, pixel[1], 4);
            assertEquals(34, pixel[2], 4);
        }
    }

    private static BufferedImage solidImage(
            int width, int height, int red, int green, int blue) {
        final var image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final int rgb = (red << 16) | (green << 8) | blue;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    private static BufferedImage solidGrayImage(int width, int height, int sample) {
        final var colorModel =
                new ComponentColorModel(
                        ColorSpace.getInstance(ColorSpace.CS_GRAY),
                        new int[] {16},
                        false,
                        false,
                        Transparency.OPAQUE,
                        DataBuffer.TYPE_USHORT);
        final var raster =
                Raster.createInterleavedRaster(
                        DataBuffer.TYPE_USHORT,
                        width,
                        height,
                        width,
                        1,
                        new int[] {0},
                        new Point());
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 0, sample);
            }
        }
        return new BufferedImage(colorModel, raster, false, null);
    }
}
