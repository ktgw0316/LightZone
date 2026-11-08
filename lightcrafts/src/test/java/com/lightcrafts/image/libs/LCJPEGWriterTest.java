package com.lightcrafts.image.libs;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;

import static com.lightcrafts.image.types.TIFFConstants.TIFF_RESOLUTION_UNIT_INCH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LCJPEGWriterTest {

    private static final int WIDTH = 24;
    private static final int HEIGHT = 16;

    @TempDir
    Path tempDir;

    @Test
    void writesRenderedImageToFile() throws Exception {
        var file = tempDir.resolve("rendered.jpg");
        var source = solidImage(WIDTH, HEIGHT, 210, 72, 38);

        try (var writer = new LCJPEGWriter(
                file.toString(), WIDTH, HEIGHT, 3, LCJPEGConstants.CS_RGB,
                95, 72, TIFF_RESOLUTION_UNIT_INCH)) {
            writer.putImage(source);
        }

        var decoded = ImageIO.read(file.toFile());
        assertNotNull(decoded);
        assertImageColor(decoded, WIDTH, HEIGHT, 210, 72, 38);
    }

    @Test
    void writesScanlinesFromAnOffsetUsingTheSpecifiedStride() throws Exception {
        var file = tempDir.resolve("scanlines.jpg");
        final int width = 4;
        final int height = 3;
        final int offset = 5;
        final int lineStride = width * 3 + 4;
        var pixels = new byte[offset + lineStride * height + 3];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                final int pixel = offset + y * lineStride + x * 3;
                pixels[pixel] = (byte) 28;
                pixels[pixel + 1] = (byte) 184;
                pixels[pixel + 2] = (byte) 92;
            }
        }

        try (var writer = new LCJPEGWriter(
                file.toString(), width, height, 3, LCJPEGConstants.CS_RGB,
                95, 72, TIFF_RESOLUTION_UNIT_INCH)) {
            assertEquals(height, writer.writeScanLines(pixels, offset, height, lineStride));
        }

        var decoded = ImageIO.read(file.toFile());
        assertNotNull(decoded);
        assertImageColor(decoded, width, height, 28, 184, 92);
    }

    @Test
    void writesCompressedDataToReceiver() throws Exception {
        var output = new ByteArrayOutputStream();
        LCImageDataReceiver receiver = buffer -> {
            buffer.rewind();
            var bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            output.write(bytes);
            return bytes.length;
        };
        var source = solidImage(WIDTH, HEIGHT, 48, 96, 216);

        try (var writer = new LCJPEGWriter(
                receiver, 1024, WIDTH, HEIGHT, 3, LCJPEGConstants.CS_RGB, 90)) {
            writer.putImage(source);
        }

        var decoded = ImageIO.read(new ByteArrayInputStream(output.toByteArray()));
        assertNotNull(decoded);
        assertImageColor(decoded, WIDTH, HEIGHT, 48, 96, 216);
    }

    private static BufferedImage solidImage(
            int width, int height, int red, int green, int blue) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
        final int rgb = (red << 16) | (green << 8) | blue;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    private static void assertImageColor(
            BufferedImage image, int width, int height, int red, int green, int blue) {
        assertEquals(width, image.getWidth());
        assertEquals(height, image.getHeight());
        final int pixel = image.getRGB(width / 2, height / 2);
        assertTrue(Math.abs(((pixel >>> 16) & 0xFF) - red) <= 12);
        assertTrue(Math.abs(((pixel >>> 8) & 0xFF) - green) <= 12);
        assertTrue(Math.abs((pixel & 0xFF) - blue) <= 12);
    }
}
