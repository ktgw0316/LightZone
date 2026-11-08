package com.lightcrafts.utils.raw;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Objects;

public class Bayer {

    private static final MethodHandle interpolateGreenNative;
    private static final MethodHandle interpolateRedBlueNative;

    static {
        System.loadLibrary("Bayer");
        final Linker linker = Linker.nativeLinker();
        final SymbolLookup symbols = SymbolLookup.loaderLookup();
        try {
            interpolateGreenNative = linker.downcallHandle(
                    symbols.find("bayer_interpolate_green").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT));
            interpolateRedBlueNative = linker.downcallHandle(
                    symbols.find("bayer_interpolate_red_blue").orElseThrow(),
                    FunctionDescriptor.of(
                            ValueLayout.JAVA_INT,
                            ValueLayout.ADDRESS,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT,
                            ValueLayout.JAVA_INT));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static void interpolateGreen(
            short[] srcData, short[] destData, int width, int height,
            int srcLineStride, int destLineStride,
            int srcOffset, int rOffset, int gOffset, int bOffset,
            int gx, int gy, int ry) {
        Objects.requireNonNull(srcData, "srcData");
        Objects.requireNonNull(destData, "destData");
        validateSourceBuffer(srcData, width, height, srcLineStride);
        validateInterleavedBuffer(
                destData, width, height, destLineStride, rOffset, gOffset, bOffset);
        validateCfaCoordinate(gx);
        validateCfaCoordinate(gy);
        validateCfaCoordinate(ry);

        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeSource;
            final MemorySegment nativeDestination;
            if (srcData == destData) {
                nativeSource = allocateShorts(arena, srcData);
                nativeDestination = nativeSource;
            } else {
                nativeSource = allocateShorts(arena, srcData);
                nativeDestination = allocateShorts(arena, destData);
            }
            final int status = (int) interpolateGreenNative.invokeExact(
                    nativeSource,
                    srcData.length,
                    nativeDestination,
                    destData.length,
                    width,
                    height,
                    srcLineStride,
                    destLineStride,
                    srcOffset,
                    rOffset,
                    gOffset,
                    bOffset,
                    gx,
                    gy,
                    ry);
            checkInterpolationStatus(status);
            MemorySegment.ofArray(destData).copyFrom(nativeDestination);
        } catch (Throwable e) {
            throwInterpolationFailure(e);
        }
    }

    public static void interpolateRedBlue(
            short[] jdata, int width, int height, int lineStride,
            int rOffset, int gOffset, int bOffset,
            int rx0, int ry0, int bx0, int by0) {
        Objects.requireNonNull(jdata, "jdata");
        validateInterleavedBuffer(
                jdata, width, height, lineStride, rOffset, gOffset, bOffset);
        validateCfaCoordinate(rx0);
        validateCfaCoordinate(ry0);
        validateCfaCoordinate(bx0);
        validateCfaCoordinate(by0);
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeData = allocateShorts(arena, jdata);
            final int status = (int) interpolateRedBlueNative.invokeExact(
                    nativeData,
                    jdata.length,
                    width,
                    height,
                    lineStride,
                    rOffset,
                    gOffset,
                    bOffset,
                    rx0,
                    ry0,
                    bx0,
                    by0);
            checkInterpolationStatus(status);
            MemorySegment.ofArray(jdata).copyFrom(nativeData);
        } catch (Throwable e) {
            throwInterpolationFailure(e);
        }
    }

    private static void validateSourceBuffer(
            short[] data, int width, int height, int lineStride) {
        if (width <= 0 || height <= 0 || lineStride < width) {
            throw new IllegalArgumentException("Invalid DCRaw source dimensions or stride");
        }
        final long required = (long) (height - 1) * lineStride + width;
        if (required > data.length) {
            throw new IndexOutOfBoundsException("DCRaw source buffer is too small");
        }
    }

    private static void validateInterleavedBuffer(
            short[] data, int width, int height, int lineStride,
            int rOffset, int gOffset, int bOffset) {
        if (width <= 0 || height <= 0 || lineStride < width) {
            throw new IllegalArgumentException("Invalid DCRaw image dimensions or stride");
        }
        if (!isRgbOffset(rOffset) || !isRgbOffset(gOffset) || !isRgbOffset(bOffset)) {
            throw new IllegalArgumentException("DCRaw RGB offsets must be between 0 and 2");
        }
        final long required;
        try {
            final long lastPixel = (long) (height - 1) * lineStride + width - 1;
            required = Math.addExact(
                    Math.multiplyExact(lastPixel, 3),
                    Math.max(rOffset, Math.max(gOffset, bOffset)) + 1L);
        } catch (ArithmeticException e) {
            throw new IndexOutOfBoundsException("DCRaw buffer size overflows");
        }
        if (required > data.length) {
            throw new IndexOutOfBoundsException("DCRaw RGB buffer is too small");
        }
    }

    private static void validateCfaCoordinate(int coordinate) {
        if (coordinate < 0 || coordinate > 1) {
            throw new IllegalArgumentException("DCRaw CFA coordinates must be 0 or 1");
        }
    }

    private static void checkInterpolationStatus(int status) {
        if (status != 0) {
            throw new IllegalArgumentException("Invalid DCRaw interpolation arguments");
        }
    }

    private static MemorySegment allocateShorts(Arena arena, short[] data) {
        final MemorySegment nativeData = arena.allocate(
                Math.max((long) data.length * Short.BYTES, Short.BYTES), Short.BYTES);
        nativeData.asSlice(0, (long) data.length * Short.BYTES)
                .copyFrom(MemorySegment.ofArray(data));
        return nativeData;
    }

    private static boolean isRgbOffset(int offset) {
        return offset >= 0 && offset < 3;
    }

    private static void throwInterpolationFailure(Throwable e) {
        if (e instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (e instanceof Error fatalError) {
            throw fatalError;
        }
        throw new IllegalStateException("DCRaw interpolation failed", e);
    }
}
