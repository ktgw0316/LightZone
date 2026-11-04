/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.utils;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Objects;

public class LCMSNative {

    private static final Linker linker;
    private static final MethodHandle buildGamma;
    private static final MethodHandle createRGBProfile;
    private static final MethodHandle createLab2Profile;
    private static final MethodHandle freeToneCurve;
    private static final MethodHandle openProfileFromMem;
    private static final MethodHandle closeProfile;
    private static final MethodHandle createTransform;
    private static final MethodHandle createProofingTransform;
    private static final MethodHandle getTransformInputFormat;
    private static final MethodHandle getTransformOutputFormat;
    private static final MethodHandle deleteTransform;
    private static final MethodHandle doTransform;

    static {
        System.loadLibrary("lcms2");
        linker = Linker.nativeLinker();
        final SymbolLookup symbols = SymbolLookup.loaderLookup();
        try {
            buildGamma =
                    downcall(
                            symbols,
                            "cmsBuildGamma",
                            FunctionDescriptor.of(
                                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_DOUBLE));
            createRGBProfile =
                    downcall(
                            symbols,
                            "cmsCreateRGBProfile",
                            FunctionDescriptor.of(
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS));
            createLab2Profile =
                    downcall(
                            symbols,
                            "cmsCreateLab2Profile",
                            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            freeToneCurve =
                    downcall(
                            symbols,
                            "cmsFreeToneCurve",
                            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
            openProfileFromMem =
                    downcall(
                            symbols,
                            "cmsOpenProfileFromMem",
                            FunctionDescriptor.of(
                                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            closeProfile =
                    downcall(
                            symbols,
                            "cmsCloseProfile",
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            createTransform =
                    downcall(
                            symbols,
                            "cmsCreateTransform",
                            FunctionDescriptor.of(
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT));
            createProofingTransform =
                    downcall(
                            symbols,
                            "cmsCreateProofingTransform",
                            FunctionDescriptor.of(
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT));
            getTransformInputFormat =
                    downcall(
                            symbols,
                            "cmsGetTransformInputFormat",
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            getTransformOutputFormat =
                    downcall(
                            symbols,
                            "cmsGetTransformOutputFormat",
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            deleteTransform =
                    downcall(
                            symbols,
                            "cmsDeleteTransform",
                            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
            doTransform =
                    downcall(
                            symbols,
                            "cmsDoTransform",
                            FunctionDescriptor.ofVoid(
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MethodHandle downcall(
            SymbolLookup symbols, String name, FunctionDescriptor descriptor) {
        return linker.downcallHandle(symbols.find(name).orElseThrow(), descriptor);
    }

    static synchronized long cmsCreateProofingTransform(
            long inputProfile,
            int inputFormat,
            long outputProfile,
            int outputFormat,
            long proofingProfile,
            int intent,
            int proofingIntent,
            int flags) {
        return invokeAddress(
                createProofingTransform,
                address(inputProfile),
                inputFormat,
                address(outputProfile),
                outputFormat,
                address(proofingProfile),
                intent,
                proofingIntent,
                flags);
    }

    protected static synchronized long cmsCreateLab2Profile() {
        return invokeAddress(createLab2Profile, MemorySegment.NULL);
    }

    protected static synchronized long cmsOpenProfileFromMem(byte[] data, int size) {
        Objects.requireNonNull(data, "data");
        if (size < 0 || size > data.length) {
            throw new IndexOutOfBoundsException("Invalid ICC profile size");
        }
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeData = copyBytes(arena, data);
            return invokeAddress(openProfileFromMem, nativeData, size);
        }
    }

    protected static synchronized long cmsCreateRGBProfile(
            double[] whitePoint, double[] primaries, double gamma) {
        Objects.requireNonNull(whitePoint, "whitePoint");
        Objects.requireNonNull(primaries, "primaries");
        if (whitePoint.length < 3 || primaries.length < 9) {
            throw new IndexOutOfBoundsException("Insufficient RGB profile components");
        }
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeWhitePoint = copyDoubles(arena, whitePoint, 3);
            final MemorySegment nativePrimaries = copyDoubles(arena, primaries, 9);
            final MemorySegment gammaContext = gamma == 1
                    ? arena.allocate(Integer.BYTES, Integer.BYTES)
                    : MemorySegment.NULL;
            final MemorySegment toneCurve = invokeSegment(buildGamma, gammaContext, gamma);
            if (toneCurve.address() == 0) {
                return 0;
            }
            try {
                final MemorySegment curves = arena.allocate(3L * ValueLayout.ADDRESS.byteSize());
                for (long offset = 0;
                        offset < 3L * ValueLayout.ADDRESS.byteSize();
                        offset += ValueLayout.ADDRESS.byteSize()) {
                    curves.set(ValueLayout.ADDRESS, offset, toneCurve);
                }
                return invokeAddress(
                        createRGBProfile, nativeWhitePoint, nativePrimaries, curves);
            } finally {
                invokeVoid(freeToneCurve, toneCurve);
            }
        }
    }

    protected static synchronized boolean cmsCloseProfile(long profile) {
        return invokeInt(closeProfile, address(profile)) != 0;
    }

    static synchronized long cmsCreateTransform(
            long inputProfile,
            int inputFormat,
            long outputProfile,
            int outputFormat,
            int intent,
            int flags) {
        return invokeAddress(
                createTransform,
                address(inputProfile),
                inputFormat,
                address(outputProfile),
                outputFormat,
                intent,
                flags);
    }

    static synchronized void cmsDeleteTransform(long transform) {
        if (transform != 0) {
            invokeVoid(deleteTransform, address(transform));
        }
    }

    static void cmsDoTransform(
            long transform, byte[] inputBuffer, byte[] outputBuffer, int size) {
        Objects.requireNonNull(inputBuffer, "inputBuffer");
        Objects.requireNonNull(outputBuffer, "outputBuffer");
        checkSize(size);
        if (transform == 0) {
            return;
        }
        checkTransformBuffers(transform, inputBuffer.length, outputBuffer.length, 1, size);
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeInput = copyBytes(arena, inputBuffer);
            final MemorySegment nativeOutput = copyBytes(arena, outputBuffer);
            invokeVoid(doTransform, address(transform), nativeInput, nativeOutput, size);
            copyBytesBack(nativeOutput, outputBuffer);
        }
    }

    static void cmsDoTransform(
            long transform, short[] inputBuffer, short[] outputBuffer, int size) {
        Objects.requireNonNull(inputBuffer, "inputBuffer");
        Objects.requireNonNull(outputBuffer, "outputBuffer");
        checkSize(size);
        if (transform == 0) {
            return;
        }
        checkTransformBuffers(transform, inputBuffer.length, outputBuffer.length, 2, size);
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeInput = copyShorts(arena, inputBuffer);
            final MemorySegment nativeOutput = copyShorts(arena, outputBuffer);
            invokeVoid(doTransform, address(transform), nativeInput, nativeOutput, size);
            copyShortsBack(nativeOutput, outputBuffer);
        }
    }

    static void cmsDoTransform(
            long transform, double[] inputBuffer, double[] outputBuffer, int size) {
        Objects.requireNonNull(inputBuffer, "inputBuffer");
        Objects.requireNonNull(outputBuffer, "outputBuffer");
        checkSize(size);
        if (transform == 0) {
            return;
        }
        checkTransformBuffers(transform, inputBuffer.length, outputBuffer.length, 0, size);
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeInput = copyDoubles(arena, inputBuffer, inputBuffer.length);
            final MemorySegment nativeOutput = copyDoubles(arena, outputBuffer, outputBuffer.length);
            invokeVoid(doTransform, address(transform), nativeInput, nativeOutput, size);
            copyDoublesBack(nativeOutput, outputBuffer);
        }
    }

    private static void checkTransformBuffers(
            long transform, int inputElements, int outputElements, int expectedBytes, int pixels) {
        final MemorySegment nativeTransform = address(transform);
        final int inputFormat = invokeInt(getTransformInputFormat, nativeTransform);
        final int outputFormat = invokeInt(getTransformOutputFormat, nativeTransform);
        if (bytesPerSample(inputFormat) != expectedBytes
                || bytesPerSample(outputFormat) != expectedBytes
                || (expectedBytes == 0
                        && (!isFloat(inputFormat) || !isFloat(outputFormat)))) {
            throw new IndexOutOfBoundsException("Transform buffer type does not match the transform");
        }

        final long inputRequired =
                (long) pixels * (channels(inputFormat) + extraSamples(inputFormat));
        final long outputRequired =
                (long) pixels * (channels(outputFormat) + extraSamples(outputFormat));
        if (inputElements < inputRequired || outputElements < outputRequired) {
            throw new IndexOutOfBoundsException("Transform buffers are too small");
        }
    }

    private static int bytesPerSample(int format) {
        return format & 7;
    }

    private static int channels(int format) {
        return (format >>> 3) & 15;
    }

    private static int extraSamples(int format) {
        return (format >>> 7) & 7;
    }

    private static boolean isFloat(int format) {
        return ((format >>> 22) & 1) != 0;
    }

    private static MemorySegment address(long value) {
        return MemorySegment.ofAddress(value);
    }

    private static MemorySegment copyBytes(Arena arena, byte[] source) {
        final MemorySegment destination = arena.allocate(Math.max(source.length, 1));
        if (source.length > 0) {
            destination.copyFrom(MemorySegment.ofArray(source));
        }
        return destination;
    }

    private static MemorySegment copyShorts(Arena arena, short[] source) {
        return copyShorts(arena, source, source.length);
    }

    private static MemorySegment copyShorts(Arena arena, short[] source, int length) {
        final MemorySegment destination =
                arena.allocate(Math.max((long) length * Short.BYTES, 1), Short.BYTES);
        if (length > 0) {
            destination.copyFrom(
                    MemorySegment.ofArray(source).asSlice(0, (long) length * Short.BYTES));
        }
        return destination;
    }

    private static MemorySegment copyDoubles(Arena arena, double[] source, int length) {
        final MemorySegment destination =
                arena.allocate(Math.max((long) length * Double.BYTES, 1), Double.BYTES);
        if (length > 0) {
            destination.copyFrom(
                    MemorySegment.ofArray(source).asSlice(0, (long) length * Double.BYTES));
        }
        return destination;
    }

    private static void copyBytesBack(MemorySegment source, byte[] destination) {
        if (destination.length > 0) {
            MemorySegment.ofArray(destination).copyFrom(source.asSlice(0, destination.length));
        }
    }

    private static void copyShortsBack(MemorySegment source, short[] destination) {
        if (destination.length > 0) {
            MemorySegment.ofArray(destination)
                    .copyFrom(source.asSlice(0, (long) destination.length * Short.BYTES));
        }
    }

    private static void copyDoublesBack(MemorySegment source, double[] destination) {
        if (destination.length > 0) {
            MemorySegment.ofArray(destination)
                    .copyFrom(source.asSlice(0, (long) destination.length * Double.BYTES));
        }
    }

    private static void checkSize(int size) {
        if (size < 0) {
            throw new IndexOutOfBoundsException("Transform size must not be negative");
        }
    }

    private static MemorySegment invokeSegment(MethodHandle handle, Object... arguments) {
        try {
            return (MemorySegment) handle.invokeWithArguments(arguments);
        } catch (Throwable e) {
            throw rethrow(e);
        }
    }

    private static long invokeAddress(MethodHandle handle, Object... arguments) {
        return invokeSegment(handle, arguments).address();
    }

    private static int invokeInt(MethodHandle handle, Object... arguments) {
        try {
            return (int) handle.invokeWithArguments(arguments);
        } catch (Throwable e) {
            throw rethrow(e);
        }
    }

    private static void invokeVoid(MethodHandle handle, Object... arguments) {
        try {
            handle.invokeWithArguments(arguments);
        } catch (Throwable e) {
            throw rethrow(e);
        }
    }

    private static RuntimeException rethrow(Throwable error) {
        if (error instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        if (error instanceof Error fatalError) {
            throw fatalError;
        }
        return new IllegalStateException("LCMS native call failed", error);
    }
}
