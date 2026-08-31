package com.lightcrafts.jai.utils;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.operator.LCSeparableConvolveDescriptor;
import com.lightcrafts.jai.opimage.LCSeparableConvolveRIF;
import org.eclipse.imagen.*;
import org.eclipse.imagen.media.util.SunTileCache;
import org.eclipse.imagen.registry.RIFRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.*;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

class FastGaussianBlurBenchmarkTest {
    private static final int WIDTH = 512;
    private static final int HEIGHT = 512;
    private static final double RADIUS = 16.0;
    private static final int WARMUP = 5;
    private static final int MEASURE = 10;

    private static RenderingHints noCacheHints;

    @BeforeAll
    static void setUpBeforeClass() {
        registerLCSeparableConvolve();

        TileCache noTileCache = new SunTileCache(0);
        noCacheHints = new RenderingHints(ImageN.KEY_TILE_CACHE, noTileCache);
    }

    @Test
//    @Disabled("Manual benchmark: run explicitly when comparing blur performance")
    void compareFastGaussianBlurWithImageNSeparableConvolveOpImage() {
        RenderedImage source = createTestImage(WIDTH, HEIGHT);
        KernelImageN kernel = getGaussKernel(RADIUS);

        RenderingHints hints = new RenderingHints(
                ImageN.KEY_BORDER_EXTENDER,
                BorderExtender.createInstance(BorderExtender.BORDER_COPY)
        );
        hints.add(noCacheHints);

        System.out.printf(
                Locale.ROOT,
                "image=%dx%d, radius=%.2f, kernel=%dx%d, warmup=%d, measure=%d%n",
                source.getWidth(),
                source.getHeight(),
                RADIUS,
                kernel.getWidth(),
                kernel.getHeight(),
                WARMUP,
                MEASURE
        );

        benchmark("Functions.fastGaussianBlur", WARMUP, MEASURE, () ->
                Functions.fastGaussianBlur(source, RADIUS)
        );

        benchmark("LCSeparableConvolve", WARMUP, MEASURE, () ->
                createLCSeparableConvolve(source, kernel, hints)
        );

//        benchmark("ImageN SeparableConvolve", WARMUP, MEASURE, () -> {
////            assert kernel.isSeparable();
//            return ConvolveDescriptor.create(source, kernel,
//                    null, null, 0, false, hints);
//        });

        final var outputDirectory = Path.of("build/gaussian-blur-output");
        try {
            Files.createDirectories(outputDirectory);

            final var sourceFile = outputDirectory.resolve("source.png").toFile();
            ImageIO.write(source, "PNG", sourceFile);

            final var fastGaussianBlurFile = outputDirectory.resolve("fastGaussianBlur.png").toFile();
            ImageIO.write(Functions.fastGaussianBlur(source, RADIUS), "PNG", fastGaussianBlurFile);

            final var lcSeparableConvolveFile = outputDirectory.resolve("lcSeparableConvolve.png").toFile();
            ImageIO.write(createLCSeparableConvolve(source, kernel, hints), "PNG", lcSeparableConvolveFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void registerLCSeparableConvolve() {
        ImageN jaiInstance = ImageN.getDefaultInstance();
        OperationRegistry registry = jaiInstance.getOperationRegistry();

        OperationDescriptor descriptor = new LCSeparableConvolveDescriptor();

        try {
            registry.registerDescriptor(descriptor);
        } catch (IllegalArgumentException ignored) {
            // Already registered, probably by another test or prior initialization.
        }

        RenderedImageFactory rif = new LCSeparableConvolveRIF();
        try {
            RIFRegistry.register(registry, descriptor.getName(), "com.lightcrafts", rif);
        } catch (IllegalArgumentException ignored) {
            // Already registered.
        }
    }

    private static RenderedOp createLCSeparableConvolve(
            RenderedImage source,
            KernelImageN kernel,
            RenderingHints hints
    ) {
        ParameterBlock pb = new ParameterBlock()
                .addSource(source)
                .add(kernel);
        return ImageN.create("LCSeparableConvolve", pb, hints);
    }

    private static KernelImageN getGaussKernel(double sigma) {
        if (sigma < 0.001) {
            sigma = 0.001;
        }

        int size = 2 * (int) Math.ceil(sigma) + 1;

        float[] data = new float[size];
        float scale = 0;

        for (int x = -size / 2, i = 0; x <= size / 2; x++, i++) {
            data[i] = (float) gauss(x, sigma);
            scale += data[i];
        }

        for (int i = 0; i < data.length; i++) {
            data[i] /= scale;
        }

        return new KernelImageN(size, size, size / 2, size / 2, data, data);
    }

    private static double gauss(double x, double s) {
        return Math.exp(-x * x / (2 * s * s));
    }

    private static void benchmark(
            String name,
            int warmupIterations,
            int measureIterations,
            BlurFactory factory
    ) {
        for (int i = 0; i < warmupIterations; i++) {
            forceEvaluation(factory.create());
        }

        long totalNanos = 0;

        for (int i = 0; i < measureIterations; i++) {
            long start = System.nanoTime();
            RenderedOp result = factory.create();
            forceEvaluation(result);
            long elapsed = System.nanoTime() - start;

            totalNanos += elapsed;

            System.out.printf(
                    Locale.ROOT,
                    "%s run %02d: %.3f ms%n",
                    name,
                    i + 1,
                    nanosToMillis(elapsed)
            );
        }

        System.out.printf(
                Locale.ROOT,
                "%s average: %.3f ms%n%n",
                name,
                nanosToMillis(totalNanos / measureIterations)
        );
    }

    private static void forceEvaluation(RenderedOp image) {
        try {
            image.copyData(null);
        } finally {
            image.dispose();
        }
    }

    private static double nanosToMillis(long nanos) {
        return nanos / (double) TimeUnit.MILLISECONDS.toNanos(1);
    }

    private static RenderedImage createTestImage(int width, int height) {
        final WritableRaster raster = Raster.createWritableRaster(
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
        final var image = PlanarImage.wrapRenderedImage(
                new BufferedImage(colorModel, raster, false, null)
        );
        for (int y = 0; y < height; y++) {
            final int g = y & 0xff;
            for (int x = 0; x < width; x++) {
                final int r = x & 0xff;
                final int b = (x * 31 + y * 17) & 0xff;
                raster.setSample(x, y, 0, r << 8);
                raster.setSample(x, y, 1, g << 8);
                raster.setSample(x, y, 2, b << 8);
            }
        }

        return image;
    }

    @FunctionalInterface
    private interface BlurFactory {
        RenderedOp create();
    }
}