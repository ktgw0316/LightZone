/* Copyright (C) 2026 LightZone Project */

package com.lightcrafts.image.libs;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

final class TIFFNative {

    private static final Linker linker = Linker.nativeLinker();
    private static final int STRING_BUFFER_SIZE = 64 * 1024;
    private static final MethodHandle init;
    private static final MethodHandle open;
    private static final MethodHandle close;
    private static final MethodHandle lastError;
    private static final MethodHandle readerIccProfile;
    private static final MethodHandle readerGetInt;
    private static final MethodHandle readerGetInt2;
    private static final MethodHandle readerGetString;
    private static final MethodHandle readerNumberOfStrips;
    private static final MethodHandle readerNumberOfTiles;
    private static final MethodHandle readerStripSize;
    private static final MethodHandle readerTileSize;
    private static final MethodHandle readerIsTiled;
    private static final MethodHandle readerNextDirectory;
    private static final MethodHandle readerReadStrip;
    private static final MethodHandle readerReadTile;
    private static final MethodHandle writerAppend;
    private static final MethodHandle writerComputeTile;
    private static final MethodHandle writerSetBytes;
    private static final MethodHandle writerSetFloat;
    private static final MethodHandle writerSetInt;
    private static final MethodHandle writerSetInt2;
    private static final MethodHandle writerSetString;
    private static final MethodHandle writerWriteStrip;
    private static final MethodHandle writerWriteTile;

    static {
        System.loadLibrary("LCTIFF");
        final SymbolLookup symbols = SymbolLookup.loaderLookup();
        try {
            init = downcall(symbols, "ltiff_init", FunctionDescriptor.ofVoid());
            open =
                    downcall(
                            symbols,
                            "ltiff_open",
                            FunctionDescriptor.of(
                                    ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS));
            close =
                    downcall(
                            symbols,
                            "ltiff_close",
                            FunctionDescriptor.ofVoid(ValueLayout.ADDRESS));
            lastError =
                    downcall(
                            symbols,
                            "ltiff_last_error",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT));
            readerIccProfile =
                    downcall(
                            symbols,
                            "ltiff_reader_icc_profile",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT));
            readerGetInt =
                    downcall(
                            symbols,
                            "ltiff_reader_get_int",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT));
            readerGetInt2 =
                    downcall(
                            symbols,
                            "ltiff_reader_get_int2",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT));
            readerGetString =
                    downcall(
                            symbols,
                            "ltiff_reader_get_string",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT));
            readerNumberOfStrips = unaryIntFunction(symbols, "ltiff_reader_number_of_strips");
            readerNumberOfTiles = unaryIntFunction(symbols, "ltiff_reader_number_of_tiles");
            readerStripSize = unaryIntFunction(symbols, "ltiff_reader_strip_size");
            readerTileSize = unaryIntFunction(symbols, "ltiff_reader_tile_size");
            readerIsTiled = unaryIntFunction(symbols, "ltiff_reader_is_tiled");
            readerNextDirectory = unaryIntFunction(symbols, "ltiff_reader_next_directory");
            readerReadStrip = readFunction(symbols, "ltiff_reader_read_strip");
            readerReadTile = readFunction(symbols, "ltiff_reader_read_tile");
            writerAppend =
                    downcall(
                            symbols,
                            "ltiff_writer_append",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS));
            writerComputeTile =
                    downcall(
                            symbols,
                            "ltiff_writer_compute_tile",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT));
            writerSetBytes =
                    downcall(
                            symbols,
                            "ltiff_writer_set_bytes",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT));
            writerSetFloat =
                    downcall(
                            symbols,
                            "ltiff_writer_set_float",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_FLOAT));
            writerSetInt =
                    downcall(
                            symbols,
                            "ltiff_writer_set_int",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT));
            writerSetInt2 =
                    downcall(
                            symbols,
                            "ltiff_writer_set_int2",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT));
            writerSetString =
                    downcall(
                            symbols,
                            "ltiff_writer_set_string",
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS));
            writerWriteStrip = writeFunction(symbols, "ltiff_writer_write_strip");
            writerWriteTile = writeFunction(symbols, "ltiff_writer_write_tile");
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private TIFFNative() {
    }

    private static MethodHandle downcall(
            SymbolLookup symbols, String name, FunctionDescriptor descriptor) {
        return linker.downcallHandle(symbols.find(name).orElseThrow(), descriptor);
    }

    private static MethodHandle unaryIntFunction(SymbolLookup symbols, String name) {
        return downcall(
                symbols,
                name,
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    }

    private static MethodHandle readFunction(SymbolLookup symbols, String name) {
        return downcall(
                symbols,
                name,
                FunctionDescriptor.of(
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS,
                        ValueLayout.JAVA_INT));
    }

    private static MethodHandle writeFunction(SymbolLookup symbols, String name) {
        return readFunction(symbols, name);
    }

    static synchronized void init() {
        invokeVoid(init);
    }

    static synchronized MemorySegment open(String filename, String mode) {
        Objects.requireNonNull(filename, "filename");
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeFilename = arena.allocateFrom(filename, StandardCharsets.UTF_8);
            final MemorySegment nativeMode = arena.allocateFrom(mode, StandardCharsets.UTF_8);
            final MemorySegment handle = invokeAddress(open, nativeFilename, nativeMode);
            if (isNull(handle)) {
                final String error = getLastError();
                if (!error.isBlank()) {
                    throw imageLibException(error);
                }
            }
            return handle;
        }
    }

    static synchronized void close(MemorySegment handle) {
        if (!isNull(handle)) {
            invokeVoid(close, handle);
            checkError();
        }
    }

    static synchronized byte[] readerIccProfile(MemorySegment handle) {
        final int size = invokeInt(readerIccProfile, handle, MemorySegment.NULL, 0);
        checkError();
        if (size < 0) {
            return null;
        }
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment buffer = arena.allocate(Math.max(size, 1));
            final int read = invokeInt(readerIccProfile, handle, buffer, size);
            checkError();
            if (read != size) {
                throw imageLibException("Could not read TIFF ICC profile");
            }
            final byte[] result = new byte[size];
            if (size > 0) {
                MemorySegment.ofArray(result).copyFrom(buffer.asSlice(0, size));
            }
            return result;
        }
    }

    static synchronized int readerGetInt(MemorySegment handle, int tag) {
        final int result = invokeInt(readerGetInt, handle, tag);
        checkError();
        return result;
    }

    static synchronized int readerGetInt2(MemorySegment handle, int tag, boolean second) {
        final int result = invokeInt(readerGetInt2, handle, tag, second ? 1 : 0);
        checkError();
        return result;
    }

    static synchronized String readerGetString(MemorySegment handle, int tag) {
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment buffer = arena.allocate(STRING_BUFFER_SIZE);
            final int length = invokeInt(readerGetString, handle, tag, buffer, STRING_BUFFER_SIZE);
            checkError();
            return length < 0 ? null : buffer.getString(0, StandardCharsets.UTF_8);
        }
    }

    static synchronized int readerNumberOfStrips(MemorySegment handle) {
        return checkedInt(readerNumberOfStrips, handle);
    }

    static synchronized int readerNumberOfTiles(MemorySegment handle) {
        return checkedInt(readerNumberOfTiles, handle);
    }

    static synchronized int readerStripSize(MemorySegment handle) {
        return checkedInt(readerStripSize, handle);
    }

    static synchronized int readerTileSize(MemorySegment handle) {
        return checkedInt(readerTileSize, handle);
    }

    static synchronized boolean readerIsTiled(MemorySegment handle) {
        return checkedInt(readerIsTiled, handle) != 0;
    }

    static synchronized boolean readerNextDirectory(MemorySegment handle) {
        return checkedInt(readerNextDirectory, handle) != 0;
    }

    static synchronized int readerReadStrip(
            MemorySegment handle, int index, byte[] buffer, long offset, int size) {
        return readerRead(handle, index, buffer, offset, size, readerReadStrip, 1);
    }

    static synchronized int readerReadStrip(
            MemorySegment handle, int index, short[] buffer, long offset, int size) {
        return readerRead(handle, index, buffer, offset, size, readerReadStrip, Short.BYTES);
    }

    static synchronized int readerReadTile(
            MemorySegment handle, int index, byte[] buffer, long offset, int size) {
        return readerRead(handle, index, buffer, offset, size, readerReadTile, 1);
    }

    static synchronized int readerReadTile(
            MemorySegment handle, int index, short[] buffer, long offset, int size) {
        return readerRead(handle, index, buffer, offset, size, readerReadTile, Short.BYTES);
    }

    static synchronized boolean writerAppend(MemorySegment handle, String filename) {
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeFilename = arena.allocateFrom(filename, StandardCharsets.UTF_8);
            final int result = invokeInt(writerAppend, handle, nativeFilename);
            checkError();
            return result != 0;
        }
    }

    static synchronized int writerComputeTile(
            MemorySegment handle, int x, int y, int z, int sample) {
        final int result = invokeInt(writerComputeTile, handle, x, y, z, sample);
        checkError();
        return result;
    }

    static synchronized boolean writerSetBytes(
            MemorySegment handle, int tag, byte[] buffer) {
        Objects.requireNonNull(buffer, "buffer");
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeBuffer = copyBytes(arena, buffer);
            final int result =
                    invokeInt(writerSetBytes, handle, tag, nativeBuffer, buffer.length);
            checkError();
            return result != 0;
        }
    }

    static synchronized boolean writerSetFloat(MemorySegment handle, int tag, float value) {
        final int result = invokeInt(writerSetFloat, handle, tag, value);
        checkError();
        return result != 0;
    }

    static synchronized boolean writerSetInt(MemorySegment handle, int tag, int value) {
        final int result = invokeInt(writerSetInt, handle, tag, value);
        checkError();
        return result != 0;
    }

    static synchronized boolean writerSetInt2(
            MemorySegment handle, int tag, int first, int second) {
        final int result = invokeInt(writerSetInt2, handle, tag, first, second);
        checkError();
        return result != 0;
    }

    static synchronized boolean writerSetString(MemorySegment handle, int tag, byte[] value) {
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeValue = copyBytes(arena, value);
            final int result = invokeInt(writerSetString, handle, tag, nativeValue);
            checkError();
            return result != 0;
        }
    }

    static synchronized int writerWriteStrip(
            MemorySegment handle, int index, byte[] buffer, long offset, int size) {
        return writerWrite(handle, index, buffer, offset, size, writerWriteStrip, 1);
    }

    static synchronized int writerWriteStrip(
            MemorySegment handle, int index, short[] buffer, long offset, int size) {
        return writerWrite(handle, index, buffer, offset, size, writerWriteStrip, Short.BYTES);
    }

    static synchronized int writerWriteTile(
            MemorySegment handle, int index, byte[] buffer, long offset, int size) {
        return writerWrite(handle, index, buffer, offset, size, writerWriteTile, 1);
    }

    static synchronized int writerWriteTile(
            MemorySegment handle, int index, short[] buffer, long offset, int size) {
        return writerWrite(handle, index, buffer, offset, size, writerWriteTile, Short.BYTES);
    }

    private static int checkedInt(MethodHandle function, MemorySegment handle) {
        final int result = invokeInt(function, handle);
        checkError();
        return result;
    }

    private static int readerRead(
            MemorySegment handle,
            int index,
            byte[] buffer,
            long offset,
            int size,
            MethodHandle function,
            int elementBytes) {
        Objects.requireNonNull(buffer, "buffer");
        checkArrayRange(buffer.length, offset, size, elementBytes);
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeBuffer = arena.allocate(Math.max(size, 1));
            if (size > 0) {
                nativeBuffer.copyFrom(
                        MemorySegment.ofArray(buffer).asSlice(offset * elementBytes, size));
            }
            final int result = invokeInt(function, handle, index, nativeBuffer, size);
            checkError();
            if (size > 0) {
                MemorySegment.ofArray(buffer)
                        .asSlice(offset * elementBytes, size)
                        .copyFrom(nativeBuffer.asSlice(0, size));
            }
            return result;
        }
    }

    private static int readerRead(
            MemorySegment handle,
            int index,
            short[] buffer,
            long offset,
            int size,
            MethodHandle function,
            int elementBytes) {
        Objects.requireNonNull(buffer, "buffer");
        checkArrayRange(buffer.length, offset, size, elementBytes);
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeBuffer = arena.allocate(Math.max(size, 1), elementBytes);
            if (size > 0) {
                nativeBuffer.copyFrom(
                        MemorySegment.ofArray(buffer).asSlice(offset * elementBytes, size));
            }
            final int result = invokeInt(function, handle, index, nativeBuffer, size);
            checkError();
            if (size > 0) {
                MemorySegment.ofArray(buffer)
                        .asSlice(offset * elementBytes, size)
                        .copyFrom(nativeBuffer.asSlice(0, size));
            }
            return result;
        }
    }

    private static int writerWrite(
            MemorySegment handle,
            int index,
            byte[] buffer,
            long offset,
            int size,
            MethodHandle function,
            int elementBytes) {
        Objects.requireNonNull(buffer, "buffer");
        checkArrayRange(buffer.length, offset, size, elementBytes);
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeBuffer = arena.allocate(Math.max(size, 1));
            if (size > 0) {
                nativeBuffer.copyFrom(
                        MemorySegment.ofArray(buffer).asSlice(offset * elementBytes, size));
            }
            final int result = invokeInt(function, handle, index, nativeBuffer, size);
            checkError();
            return result;
        }
    }

    private static int writerWrite(
            MemorySegment handle,
            int index,
            short[] buffer,
            long offset,
            int size,
            MethodHandle function,
            int elementBytes) {
        Objects.requireNonNull(buffer, "buffer");
        checkArrayRange(buffer.length, offset, size, elementBytes);
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment nativeBuffer =
                    arena.allocate(Math.max(size, 1), elementBytes);
            if (size > 0) {
                nativeBuffer.copyFrom(
                        MemorySegment.ofArray(buffer).asSlice(offset * elementBytes, size));
            }
            final int result = invokeInt(function, handle, index, nativeBuffer, size);
            checkError();
            return result;
        }
    }

    private static MemorySegment copyBytes(Arena arena, byte[] buffer) {
        final MemorySegment segment = arena.allocate(Math.max(buffer.length, 1));
        if (buffer.length > 0) {
            segment.copyFrom(MemorySegment.ofArray(buffer));
        }
        return segment;
    }

    private static void checkArrayRange(int arrayLength, long offset, int size, int elementBytes) {
        if (offset < 0 || size < 0 || size % elementBytes != 0 ||
                offset > arrayLength || (long) size / elementBytes > arrayLength - offset) {
            throw new IndexOutOfBoundsException("TIFF buffer range is invalid");
        }
    }

    private static boolean isNull(MemorySegment segment) {
        return segment.equals(MemorySegment.NULL);
    }

    private static String getLastError() {
        try (Arena arena = Arena.ofConfined()) {
            final MemorySegment buffer = arena.allocate(1024);
            invokeInt(lastError, buffer, 1024);
            return buffer.getString(0, StandardCharsets.UTF_8);
        }
    }

    private static void checkError() {
        final String error = getLastError();
        if (error.isBlank()) {
            return;
        }
        if (error.equals("unsupported tagID")) {
            throw new IllegalArgumentException(error);
        }
        throw imageLibException(error);
    }

    private static RuntimeException imageLibException(String message) {
        return TIFFNative.<RuntimeException>sneakyThrow(new LCImageLibException(message));
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException sneakyThrow(Throwable error) throws T {
        throw (T) error;
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
        return new IllegalStateException("TIFF native call failed", error);
    }
}
