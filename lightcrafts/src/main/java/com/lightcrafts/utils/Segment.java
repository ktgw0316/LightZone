/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Objects;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Apr 28, 2005
 * Time: 10:49:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class Segment {
    private static final MethodHandle segmentImage;

    static {
        System.loadLibrary("Segment");
        final Linker linker = Linker.nativeLinker();
        final SymbolLookup symbols = SymbolLookup.loaderLookup();
        try {
            segmentImage =
                    linker.downcallHandle(
                            symbols.find("segment_image").orElseThrow(),
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static synchronized byte[] segmentImage(
            byte[] image, int channels, int height, int width) {
        Objects.requireNonNull(image, "image");
        if (channels != 1 && channels != 3) {
            throw new IllegalArgumentException("Segment images must have 1 or 3 channels");
        }
        if (height <= 0 || width <= 0) {
            throw new IllegalArgumentException("Image dimensions must be positive");
        }
        if ((long) height * width > Integer.MAX_VALUE / channels) {
            throw new IllegalArgumentException("Image is too large to segment");
        }
        final long imageSize = (long) channels * height * width;
        if (image.length < imageSize) {
            throw new IllegalArgumentException("Image buffer is smaller than its dimensions");
        }

        final byte[] result = new byte[(int) imageSize];
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeImage = arena.allocate(Math.max(imageSize, 1));
            nativeImage.copyFrom(MemorySegment.ofArray(image).asSlice(0, imageSize));
            final MemorySegment nativeResult = arena.allocate(Math.max(imageSize, 1));
            final int resultSize =
                    (int) segmentImage.invokeExact(
                            nativeImage, channels, height, width, nativeResult, (int) imageSize);
            if (resultSize != imageSize) {
                throw new IllegalStateException("Native image segmentation failed");
            }
            MemorySegment.ofArray(result).copyFrom(nativeResult.asSlice(0, imageSize));
            return result;
        } catch (Throwable e) {
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (e instanceof Error fatalError) {
                throw fatalError;
            }
            throw new IllegalStateException("Native image segmentation failed", e);
        }
    }
}
