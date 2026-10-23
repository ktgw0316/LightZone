package com.lightcrafts.image.libs;

import org.eclipse.imagen.PlanarImage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LCJPEGReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void readsFileAndReportsImageProperties() throws Exception {
        var jpeg = createJpeg(48, 32);
        var file = tempDir.resolve("input.jpg").toFile();
        ImageIO.write(jpeg, "jpeg", file);

        try (var reader = new LCJPEGReader(file.getPath())) {
            assertEquals(48, reader.getWidth());
            assertEquals(32, reader.getHeight());
            assertEquals(3, reader.getColorsPerPixel());
            assertEquals(LCJPEGConstants.CS_RGB, reader.getColorSpace());

            var image = reader.getImage();
            assertEquals(48, image.getWidth());
            assertEquals(32, image.getHeight());
            assertTrue(image.getData().getSample(24, 16, 0) > 0);
        }
    }

    @Test
    void readsProviderAndScalesToLibjpegSupportedSize() throws Exception {
        var jpegData = jpegBytes(createJpeg(48, 32));
        try (var reader = new LCJPEGReader(
                new InputStreamImageDataProvider(new ByteArrayInputStream(jpegData)),
                12, 8)) {
            assertEquals(12, reader.getWidth());
            assertEquals(8, reader.getHeight());
            assertEquals(3, reader.getColorsPerPixel());

            PlanarImage image = reader.getImage();
            assertEquals(12, image.getWidth());
            assertEquals(8, image.getHeight());
            assertTrue(image.getData().getSample(6, 4, 0) > 0);
        }
    }

    @Test
    void readsScanlinesAtTheRequestedArrayOffset() throws Exception {
        var file = tempDir.resolve("scanlines.jpg");
        ImageIO.write(createJpeg(48, 32), "jpeg", file.toFile());

        try (var reader = new LCJPEGReader(file.toString())) {
            var buffer = new byte[48 * 3 + 9];
            Arrays.fill(buffer, (byte) 0x7F);

            assertEquals(1, reader.readScanLines(buffer, 8, 1));
            for (int i = 0; i < 8; i++) {
                assertEquals(0x7F, buffer[i] & 0xFF);
            }
            assertEquals(0x7F, buffer[buffer.length - 1] & 0xFF);
            assertTrue(buffer[8] != 0);
            assertEquals(31, reader.readScanLines(new byte[48 * 3 * 31], 0, 31));
        }
    }

    @Test
    void missingFileThrowsFileNotFoundException() {
        assertThrows(FileNotFoundException.class,
                () -> new LCJPEGReader(tempDir.resolve("missing.jpg").toString()));
    }

    @Test
    void invalidJpegThrowsImageLibraryException() {
        var emptyProvider = new InputStreamImageDataProvider(new ByteArrayInputStream(new byte[0]));
        assertThrows(LCImageLibException.class, () -> new LCJPEGReader(emptyProvider));
    }

    @Test
    void propagatesProviderIoException() {
        LCImageDataProvider failingProvider = buffer -> {
            throw new IOException("read failed");
        };
        assertThrows(IOException.class, () -> new LCJPEGReader(failingProvider));
    }

    @Test
    void closingBeforeReadingTheWholeImageReportsIncompleteDecompression() throws Exception {
        var file = tempDir.resolve("incomplete.jpg");
        ImageIO.write(createJpeg(48, 32), "jpeg", file.toFile());
        var reader = new LCJPEGReader(file.toString());

        assertThrows(LCImageLibException.class, reader::close);
        reader.close();
    }

    private static BufferedImage createJpeg(int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, 0x336699);
            }
        }
        return image;
    }

    private static byte[] jpegBytes(BufferedImage image) throws IOException {
        var output = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", output);
        return output.toByteArray();
    }
}
