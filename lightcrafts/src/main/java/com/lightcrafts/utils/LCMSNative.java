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
    private static final MethodHandle openProfileFromMem;
    private static final MethodHandle createRGBProfile;
    private static final MethodHandle createLab2Profile;
    private static final MethodHandle closeProfile;
    private static final MethodHandle createTransform;
    private static final MethodHandle createProofingTransform;
    private static final MethodHandle deleteTransform;
    private static final MethodHandle doTransformBytes;
    private static final MethodHandle doTransformShorts;
    private static final MethodHandle doTransformDoubles;

    static {
        System.loadLibrary("LCLCMS");
        linker = Linker.nativeLinker();
        final SymbolLookup symbols = SymbolLookup.loaderLookup();
        try {
            openProfileFromMem =
                    downcall(
                            symbols,
                            "lcms_open_profile_from_mem",
                            FunctionDescriptor.of(
                                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.JAVA_INT));
            createRGBProfile =
                    downcall(
                            symbols,
                            "lcms_create_rgb_profile",
                            FunctionDescriptor.of(
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_DOUBLE));
            createLab2Profile =
                    downcall(
                            symbols,
                            "lcms_create_lab2_profile",
                            FunctionDescriptor.of(ValueLayout.ADDRESS));
            closeProfile =
                    downcall(
                            symbols,
                            "lcms_close_profile",
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            createTransform =
                    downcall(
                            symbols,
                            "lcms_create_transform",
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
                            "lcms_create_proofing_transform",
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
            deleteTransform =
                    downcall(
                            symbols,
                            "lcms_delete_transform",
                            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
            doTransformBytes =
                    downcall(
                            symbols,
                            "lcms_do_transform_bytes",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT));
            doTransformShorts =
                    downcall(
                            symbols,
                            "lcms_do_transform_shorts",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT));
            doTransformDoubles =
                    downcall(
                            symbols,
                            "lcms_do_transform_doubles",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
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
        return invokeAddress(createLab2Profile);
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
            final MemorySegment nativeWhitePoint = copyDoubles(arena, whitePoint);
            final MemorySegment nativePrimaries = copyDoubles(arena, primaries);
            return invokeAddress(
                    createRGBProfile, nativeWhitePoint, nativePrimaries, gamma);
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
        invokeVoid(deleteTransform, address(transform));
    }

    static void cmsDoTransform(
            long transform, byte[] inputBuffer, byte[] outputBuffer, int size) {
        Objects.requireNonNull(inputBuffer, "inputBuffer");
        Objects.requireNonNull(outputBuffer, "outputBuffer");
        checkSize(size);
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeInput = copyBytes(arena, inputBuffer);
            final MemorySegment nativeOutput = copyBytes(arena, outputBuffer);
            final int status =
                    invokeInt(
                            doTransformBytes,
                            address(transform),
                            nativeInput,
                            inputBuffer.length,
                            nativeOutput,
                            outputBuffer.length,
                            size);
            checkTransformStatus(status);
            copyBytesBack(nativeOutput, outputBuffer);
        }
    }

    static void cmsDoTransform(
            long transform, short[] inputBuffer, short[] outputBuffer, int size) {
        Objects.requireNonNull(inputBuffer, "inputBuffer");
        Objects.requireNonNull(outputBuffer, "outputBuffer");
        checkSize(size);
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeInput = copyShorts(arena, inputBuffer);
            final MemorySegment nativeOutput =
                    arena.allocate(
                            Math.max((long) outputBuffer.length * Short.BYTES, 1),
                            Short.BYTES);
            if (outputBuffer.length > 0) {
                nativeOutput.copyFrom(MemorySegment.ofArray(outputBuffer));
            }
            final int status =
                    invokeInt(
                            doTransformShorts,
                            address(transform),
                            nativeInput,
                            inputBuffer.length,
                            nativeOutput,
                            outputBuffer.length,
                            size);
            checkTransformStatus(status);
            copyShortsBack(nativeOutput, outputBuffer);
        }
    }

    static void cmsDoTransform(
            long transform, double[] inputBuffer, double[] outputBuffer, int size) {
        Objects.requireNonNull(inputBuffer, "inputBuffer");
        Objects.requireNonNull(outputBuffer, "outputBuffer");
        checkSize(size);
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeInput = copyDoubles(arena, inputBuffer);
            final MemorySegment nativeOutput =
                    arena.allocate(
                            Math.max((long) outputBuffer.length * Double.BYTES, 1),
                            Double.BYTES);
            if (outputBuffer.length > 0) {
                nativeOutput.copyFrom(MemorySegment.ofArray(outputBuffer));
            }
            final int status =
                    invokeInt(
                            doTransformDoubles,
                            address(transform),
                            nativeInput,
                            inputBuffer.length,
                            nativeOutput,
                            outputBuffer.length,
                            size);
            checkTransformStatus(status);
            copyDoublesBack(nativeOutput, outputBuffer);
        }
    }

    private static MemorySegment address(long address) {
        return MemorySegment.ofAddress(address);
    }

    private static MemorySegment copyBytes(Arena arena, byte[] source) {
        final MemorySegment destination = arena.allocate(Math.max(source.length, 1));
        if (source.length > 0) {
            destination.copyFrom(MemorySegment.ofArray(source));
        }
        return destination;
    }

    private static MemorySegment copyShorts(Arena arena, short[] source) {
        final MemorySegment destination =
                arena.allocate(
                        Math.max((long) source.length * Short.BYTES, 1), Short.BYTES);
        if (source.length > 0) {
            destination.copyFrom(MemorySegment.ofArray(source));
        }
        return destination;
    }

    private static MemorySegment copyDoubles(Arena arena, double[] source) {
        final MemorySegment destination =
                arena.allocate(
                        Math.max((long) source.length * Double.BYTES, 1), Double.BYTES);
        if (source.length > 0) {
            destination.copyFrom(MemorySegment.ofArray(source));
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

    private static void checkTransformStatus(int status) {
        if (status < 0) {
            throw new IndexOutOfBoundsException("Transform buffers are too small");
        }
    }

    private static long invokeAddress(MethodHandle handle, Object... arguments) {
        try {
            final MemorySegment result = (MemorySegment) handle.invokeWithArguments(arguments);
            return result.address();
        } catch (Throwable e) {
            throw rethrow(e);
        }
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
