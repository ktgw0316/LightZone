/* Copyright (C) 2026 LightZone Project */

package com.lightcrafts.utils;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

final class LensfunNative {

    private static final int INITIAL_NAME_BUFFER_SIZE = 1024 * 1024;
    private static final Linker linker = Linker.nativeLinker();
    private static final MethodHandle create;
    private static final MethodHandle destroy;
    private static final MethodHandle getNames;
    private static final MethodHandle initModifier;
    private static final MethodHandle initModifierPoly5;
    private static final MethodHandle distortionColor;
    private static final MethodHandle backwardMapRect;

    static {
        System.loadLibrary("LCLENSFUN");
        final SymbolLookup symbols = SymbolLookup.loaderLookup();
        try {
            create =
                    downcall(
                            symbols,
                            "lensfun_create",
                            FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            destroy =
                    downcall(
                            symbols,
                            "lensfun_destroy",
                            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
            getNames =
                    downcall(
                            symbols,
                            "lensfun_get_names",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT));
            initModifier =
                    downcall(
                            symbols,
                            "lensfun_init_modifier",
                            FunctionDescriptor.ofVoid(
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_FLOAT,
                                    ValueLayout.JAVA_FLOAT));
            initModifierPoly5 =
                    downcall(
                            symbols,
                            "lensfun_init_modifier_poly5",
                            FunctionDescriptor.ofVoid(
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_FLOAT,
                                    ValueLayout.JAVA_FLOAT,
                                    ValueLayout.JAVA_FLOAT,
                                    ValueLayout.JAVA_FLOAT,
                                    ValueLayout.JAVA_FLOAT,
                                    ValueLayout.JAVA_FLOAT));
            distortionColor =
                    downcall(
                            symbols,
                            "lensfun_distortion_color",
                            FunctionDescriptor.ofVoid(
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
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
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT));
            backwardMapRect =
                    downcall(
                            symbols,
                            "lensfun_backward_map_rect",
                            FunctionDescriptor.ofVoid(
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private LensfunNative() {}

    static long create(String path) {
        Objects.requireNonNull(path, "path");
        try (Arena arena = Arena.ofConfined()) {
            final long handle =
                    invokeAddress(create, arena.allocateFrom(path, StandardCharsets.UTF_8)).address();
            if (handle == 0) {
                throw new IllegalStateException("Lensfun database could not be initialized");
            }
            return handle;
        }
    }

    static void destroy(long handle) {
        if (handle != 0) {
            invokeVoid(destroy, address(handle));
        }
    }

    static String[] getCameraNames(long handle) {
        return names(handle, 0, "", "");
    }

    static String[] getLensNames(long handle) {
        return names(handle, 1, "", "");
    }

    static String[] getLensNamesForCamera(long handle, String cameraMaker, String cameraModel) {
        return names(
                handle, 2,
                Objects.requireNonNull(cameraMaker, "cameraMaker"),
                Objects.requireNonNull(cameraModel, "cameraModel"));
    }

    static void initModifier(
            long handle, int fullWidth, int fullHeight,
            String cameraMaker, String cameraModel, String lensMaker, String lensModel,
            float focal, float aperture) {
        try (Arena arena = Arena.ofConfined()) {
            invokeVoid(
                    initModifier,
                    address(handle),
                    fullWidth,
                    fullHeight,
                    utf8(arena, cameraMaker),
                    utf8(arena, cameraModel),
                    utf8(arena, lensMaker),
                    utf8(arena, lensModel),
                    focal,
                    aperture);
        }
    }

    static void initModifierWithPoly5Lens(
            long handle, int fullWidth, int fullHeight,
            float k1, float k2, float kr, float kb, float focal, float aperture) {
        invokeVoid(
                initModifierPoly5, address(handle), fullWidth, fullHeight,
                k1, k2, kr, kb, focal, aperture);
    }

    static void distortionColor(
            long handle,
            short[] srcData, short[] dstData,
            int srcRectX, int srcRectY, int srcRectWidth, int srcRectHeight,
            int dstRectX, int dstRectY, int dstRectWidth, int dstRectHeight,
            int srcPixelStride, int dstPixelStride,
            int srcROffset, int srcGOffset, int srcBOffset,
            int dstROffset, int dstGOffset, int dstBOffset,
            int srcLineStride, int dstLineStride) {
        Objects.requireNonNull(srcData, "srcData");
        Objects.requireNonNull(dstData, "dstData");
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeSource = copyShorts(arena, srcData);
            final MemorySegment nativeDestination =
                    arena.allocate(Math.max((long) dstData.length * Short.BYTES, 1), Short.BYTES);
            invokeVoid(
                    distortionColor,
                    address(handle),
                    nativeSource,
                    nativeDestination,
                    srcRectX, srcRectY, srcRectWidth, srcRectHeight,
                    dstRectX, dstRectY, dstRectWidth, dstRectHeight,
                    srcPixelStride, dstPixelStride,
                    srcROffset, srcGOffset, srcBOffset,
                    dstROffset, dstGOffset, dstBOffset,
                    srcLineStride, dstLineStride);
            if (dstData.length > 0) {
                MemorySegment.ofArray(dstData)
                        .copyFrom(nativeDestination.asSlice(0, (long) dstData.length * Short.BYTES));
            }
        }
    }

    static int[] backwardMapRect(
            long handle, int dstRectX, int dstRectY, int dstRectWidth, int dstRectHeight) {
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment result =
                    arena.allocate((long) Integer.BYTES * 4, Integer.BYTES);
            invokeVoid(
                    backwardMapRect,
                    address(handle),
                    dstRectX, dstRectY, dstRectWidth, dstRectHeight,
                    result);
            return result.toArray(ValueLayout.JAVA_INT);
        }
    }

    private static String[] names(long handle, int kind, String cameraMaker, String cameraModel) {
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeMaker = utf8(arena, cameraMaker);
            final MemorySegment nativeModel = utf8(arena, cameraModel);
            int capacity = INITIAL_NAME_BUFFER_SIZE;
            MemorySegment buffer = arena.allocate(capacity);
            int size = invokeNames(handle, kind, nativeMaker, nativeModel, buffer, capacity);
            if (size < 0) {
                throw new IllegalStateException("Lensfun could not retrieve database names");
            }
            if (size > capacity) {
                capacity = size;
                buffer = arena.allocate(capacity);
                size = invokeNames(handle, kind, nativeMaker, nativeModel, buffer, capacity);
                if (size < 0 || size > capacity) {
                    throw new IllegalStateException("Lensfun returned an invalid name buffer size");
                }
            }
            final ArrayList<String> result = new ArrayList<>();
            for (long offset = 0; offset < size && buffer.get(ValueLayout.JAVA_BYTE, offset) != 0;) {
                final String value = buffer.getString(offset, StandardCharsets.UTF_8);
                result.add(value);
                offset += value.getBytes(StandardCharsets.UTF_8).length + 1L;
            }
            return result.toArray(String[]::new);
        }
    }

    private static int invokeNames(
            long handle, int kind, MemorySegment maker, MemorySegment model,
            MemorySegment buffer, int capacity) {
        return invokeInt(
                getNames, address(handle), kind, maker, model, buffer, capacity);
    }

    private static MemorySegment utf8(Arena arena, String value) {
        return arena.allocateFrom(Objects.requireNonNull(value), StandardCharsets.UTF_8);
    }

    private static MemorySegment copyShorts(Arena arena, short[] source) {
        final MemorySegment destination =
                arena.allocate(Math.max((long) source.length * Short.BYTES, 1), Short.BYTES);
        if (source.length > 0) {
            destination.copyFrom(MemorySegment.ofArray(source));
        }
        return destination;
    }

    private static MethodHandle downcall(
            SymbolLookup symbols, String name, FunctionDescriptor descriptor) {
        return linker.downcallHandle(symbols.find(name).orElseThrow(), descriptor);
    }

    private static MemorySegment address(long value) {
        if (value == 0) {
            throw new IllegalStateException("Lensfun handle is closed");
        }
        return MemorySegment.ofAddress(value);
    }

    private static MemorySegment invokeAddress(MethodHandle handle, Object... arguments) {
        try {
            return (MemorySegment) handle.invokeWithArguments(arguments);
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
        return new IllegalStateException("Lensfun native call failed", error);
    }
}
