package com.lightcrafts.jai.opimage;

import com.lightcrafts.jai.operator.FastBoxFilterDescriptor;
import com.lightcrafts.jai.JAIContext;
import org.eclipse.imagen.PlanarImage;
import org.eclipse.imagen.RenderedOp;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.BandedSampleModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastBoxFilterOpImageUShortTest {
    private final int width = 128;
    private final int height = 96;

    private RenderedOp applyBoxFilter(BufferedImage src, int kw, int kh, int kx, int ky) {
        // Ensure JAI context is initialized so the FastBoxFilter operation is registered
        try {
            Class.forName("com.lightcrafts.jai.JAIContext");
        } catch (ClassNotFoundException ignored) {
        }
        return FastBoxFilterDescriptor.create(src, kw, kh, kx, ky, null);
    }

    @Test
    void boxFilterUShort_uniformMidValue() {
        final int value = 20000;
        BufferedImage src = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster r = src.getRaster();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                r.setSample(x, y, 0, value);
            }
        }

        RenderedOp out = applyBoxFilter(src, 5, 5, 2, 2);
        // force evaluation
        WritableRaster result = out.copyData(null);

        int cx = width/2, cy = height/2;
        System.out.println("Expected center="+value+" got="+(result.getSample(cx, cy, 0) & 0xFFFF));
        int mismatches = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int got = result.getSample(x, y, 0) & 0xFFFF;
                if (got != value) {
                    System.out.println("Mismatch at ("+x+","+y+") got="+got);
                    mismatches++;
                    if (mismatches > 10) break;
                }
            }
            if (mismatches > 10) break;
        }
        assertEquals(0, mismatches, "Found mismatching pixels");
    }

    @Test
    void boxFilterUShort_extremeValues() {
        // Minimum
        BufferedImage srcMin = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster rmin = srcMin.getRaster();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                rmin.setSample(x, y, 0, 0);
            }
        }
        RenderedOp outMin = applyBoxFilter(srcMin, 3, 3, 1, 1);
        WritableRaster resMin = outMin.copyData(null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                assertEquals(0, resMin.getSample(x, y, 0) & 0xFFFF);
            }
        }

        // Maximum
        BufferedImage srcMax = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster rmax = srcMax.getRaster();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                rmax.setSample(x, y, 0, 65535);
            }
        }
        RenderedOp outMax = applyBoxFilter(srcMax, 7, 7, 3, 3);
        WritableRaster resMax = outMax.copyData(null);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                assertEquals(65535, resMax.getSample(x, y, 0) & 0xFFFF);
            }
        }
    }

    @Test
    void boxFilterUShort_saveDummyAndFilteredImages() throws IOException {
        final WritableRaster sourceRaster = Raster.createWritableRaster(
                new BandedSampleModel(DataBuffer.TYPE_USHORT, width, height, 3),
                new DataBufferUShort(width * height, 3),
                new Point(0, 0)
        );
        final var colorModel = new ComponentColorModel(
                JAIContext.sRGBColorSpace,
                new int[]{16, 16, 16},
                false,
                false,
                Transparency.OPAQUE,
                DataBuffer.TYPE_USHORT
        );
        final var source = PlanarImage.wrapRenderedImage(
                new BufferedImage(colorModel, sourceRaster, false, null)
        );
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int base = x < width / 2 ? 8000 : 50000;
                int checkerboard = ((x / 8 + y / 8) % 2) * 3000;
                sourceRaster.setSample(x, y, 0, base + y * 100 + checkerboard);
                sourceRaster.setSample(x, y, 1, base + y * 80 + checkerboard);
                sourceRaster.setSample(x, y, 2, base + y * 60 + checkerboard);
            }
        }

        final RenderedOp filtered = applyBoxFilter(source, 10, 10);

        final Path outputDirectory = Path.of(System.getProperty(
                "fastBoxFilter.outputDir", "build/fast-box-filter-test-output"));
        Files.createDirectories(outputDirectory);

        final Path sourceFile = outputDirectory.resolve("fast-box-filter-original.png");
        final Path filteredFile = outputDirectory.resolve("fast-box-filter-filtered.png");
        assertTrue(ImageIO.write(source, "PNG", sourceFile.toFile()));
        assertTrue(ImageIO.write(filtered, "PNG", filteredFile.toFile()));
    }
}
