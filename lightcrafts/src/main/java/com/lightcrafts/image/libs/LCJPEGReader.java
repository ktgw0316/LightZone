/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.libs;

import com.lightcrafts.image.types.AdobeEmbedJPEGSegmentFilter;
import com.lightcrafts.image.types.AdobeJPEGSegmentFilter;
import com.lightcrafts.image.types.JPEGImageInfo;
import com.lightcrafts.jai.opimage.CachedImage;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.thread.ProgressThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import org.eclipse.imagen.ImageLayout;
import org.eclipse.imagen.PlanarImage;

import static com.lightcrafts.image.types.JPEGConstants.JPEG_APPC_MARKER;
import static com.lightcrafts.image.types.JPEGConstants.JPEG_APPE_MARKER;
import static com.lightcrafts.jai.JAIContext.CMYKColorSpace;
import static com.lightcrafts.jai.JAIContext.TILE_HEIGHT;
import static com.lightcrafts.jai.JAIContext.TILE_WIDTH;
import static com.lightcrafts.jai.JAIContext.fileCache;
import static com.lightcrafts.jai.JAIContext.gray22ColorSpace;
import static com.lightcrafts.jai.JAIContext.sRGBColorSpace;

/**
 * An <code>LCJPEGReader</code> is a Java wrapper around the LibJPEG library for reading JPEG
 * images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <a href="http://www.ijg.org/">LibJPEG</a>
 */
public final class LCJPEGReader implements AutoCloseable, LCImageReader {

    private static final Logger logger = LoggerFactory.getLogger(LCJPEGReader.class);
    private static final Linker linker;
    private static final FunctionDescriptor readCallbackDescriptor =
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT);
    private static final MethodHandle createReader;
    private static final MethodHandle destroyReader;
    private static final MethodHandle getReaderWidth;
    private static final MethodHandle getReaderHeight;
    private static final MethodHandle getReaderComponents;
    private static final MethodHandle getReaderColorSpace;
    private static final MethodHandle readReaderScanLines;
    private static final MethodHandle getReaderError;

    static {
        System.loadLibrary("LCJPEG");
        linker = Linker.nativeLinker();
        var symbols = SymbolLookup.loaderLookup();
        try {
            createReader =
                    linker.downcallHandle(
                            symbols.find("lcjpeg_reader_create").orElseThrow(),
                            FunctionDescriptor.of(
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.JAVA_INT));
            destroyReader =
                    linker.downcallHandle(
                            symbols.find("lcjpeg_reader_destroy").orElseThrow(),
                            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
            getReaderWidth = readerIntFunction(symbols, "lcjpeg_reader_width");
            getReaderHeight = readerIntFunction(symbols, "lcjpeg_reader_height");
            getReaderComponents = readerIntFunction(symbols, "lcjpeg_reader_components");
            getReaderColorSpace = readerIntFunction(symbols, "lcjpeg_reader_color_space");
            readReaderScanLines =
                    linker.downcallHandle(
                            symbols.find("lcjpeg_reader_read_scanlines").orElseThrow(),
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT));
            getReaderError =
                    linker.downcallHandle(
                            symbols.find("lcjpeg_reader_last_error").orElseThrow(),
                            FunctionDescriptor.of(
                                    ValueLayout.JAVA_INT,
                                    ValueLayout.ADDRESS,
                                    ValueLayout.JAVA_INT));
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MethodHandle readerIntFunction(SymbolLookup symbols, String name) {
        return linker.downcallHandle(
                symbols.find(name).orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS));
    }

    /**
     * The default buffer size for use with {@link #LCJPEGReader(LCImageDataProvider).
     */
    private static final int DEFAULT_BUF_SIZE = 32 * 1024;

    /**
     * The number of colors per pixel. This is set from native code.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private int m_colorsPerPixel;
    /**
     * The colorspace of the image, one of: <code>CS_GRAYSCALE</code>, <code>CS_RGB</code>, <code>
     * CS_YCbRr</code>, <code>CS_CMYK</code>, <code>CS_YCCK</code>, or <code>CS_UNKNOWN</code>. This
     * is set from native code.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private int m_colorSpace;
    /**
     * This is <code>true</code> only if the JPEG file has an Adobe (APPE) segment.
     */
    private boolean m_hasAdobeSegment;

    /**
     * This is <code>true</code> only if the JPEG file has an Adobe Embed (APPC) segment.
     */
    private boolean m_hasAdobeEmbedMarker;

    /**
     * The image height. This is set from native code.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private int m_height;
    /**
     * The actual end-result image.
     */
    private PlanarImage m_image;
    /**
     * This is where the native code stores a pointer to the <code>JPEG</code> native data
     * structure. Do not access it outside the FFM calls below.
     */
    private MemorySegment m_nativePtr = MemorySegment.NULL;
    private Arena m_nativeArena;
    private ProviderCallback m_providerCallback;
    private InputStreamImageDataProvider m_ownedProvider;
    /**
     * The image width. This is set from native code.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private int m_width;

    /**
     * Construct an <code>LCJPEGReader</code>.
     *
     * @param fileName The name of the JPEG file to read.
     */
    public LCJPEGReader(String fileName)
            throws FileNotFoundException, LCImageLibException, UnsupportedEncodingException {
        this(fileName, 0, 0, null);
    }

    /**
     * Construct an <code>LCJPEGReader</code>.
     *
     * @param fileName The name of the JPEG file to read.
     * @param maxWidth The maximum width of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     * @param jpegInfo The {@link JPEGImageInfo} of the image, or <code>null</code>.
     */
    public LCJPEGReader(String fileName, int maxWidth, int maxHeight, JPEGImageInfo jpegInfo)
            throws FileNotFoundException, LCImageLibException, UnsupportedEncodingException {
        openForReading(fileName, maxWidth, maxHeight);
        if (jpegInfo != null) {
            if (jpegInfo.getFirstSegmentFor(JPEG_APPE_MARKER, new AdobeJPEGSegmentFilter())
                    != null) {
                m_hasAdobeSegment = true;
            }
            if (jpegInfo.getFirstSegmentFor(JPEG_APPC_MARKER, new AdobeEmbedJPEGSegmentFilter())
                    != null) {
                m_hasAdobeEmbedMarker = true;
            }
        }
    }

    /**
     * Construct an <code>LCJPEGReader</code>.
     *
     * @param provider The {@link LCImageDataProvider} to get image data from.
     */
    public LCJPEGReader(LCImageDataProvider provider) throws LCImageLibException {
        this(provider, 0, 0);
    }

    /**
     * Construct an <code>LCJPEGReader</code>.
     *
     * @param provider The {@link LCImageDataProvider} to get image data from.
     * @param maxWidth The maximum width of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     */
    public LCJPEGReader(LCImageDataProvider provider, int maxWidth, int maxHeight)
            throws LCImageLibException {
        beginRead(provider, DEFAULT_BUF_SIZE, maxWidth, maxHeight);
    }

    /**
     * Construct an <code>LCJPEGReader</code>.
     *
     * @param provider The {@link LCImageDataProvider} to get image data from.
     * @param bufSize The size of the buffer (in bytes) to use.
     */
    public LCJPEGReader(LCImageDataProvider provider, int bufSize) throws LCImageLibException {
        beginRead(provider, bufSize, 0, 0);
    }

    /**
     * Construct an <code>LCJPEGReader</code>.
     *
     * @param provider The {@link LCImageDataProvider} to get image data from.
     * @param bufSize The size of the buffer to use.
     * @param maxWidth The maximum width of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     */
    public LCJPEGReader(LCImageDataProvider provider, int bufSize, int maxWidth, int maxHeight)
            throws LCImageLibException {
        beginRead(provider, bufSize, maxWidth, maxHeight);
    }

    /**
     * Dispose of an <code>LCJPEGReader</code>.
     */
    public synchronized void dispose() throws LCImageLibException {
        final MemorySegment nativePtr = m_nativePtr;
        m_nativePtr = MemorySegment.NULL;
        try {
            if (!nativePtr.equals(MemorySegment.NULL)) {
                final int status = (int) destroyReader.invokeExact(nativePtr);
                if (status < 0) {
                    checkProviderFailure();
                    throw new LCImageLibException(lastNativeError());
                }
            }
            if (m_providerCallback != null) {
                checkProviderFailure();
            }
        } catch (LCImageLibException e) {
            throw e;
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (IOException e) {
            throw LCJPEGReader.<RuntimeException>throwUnchecked(e);
        } catch (Throwable e) {
            throw new LCImageLibException(e);
        } finally {
            try {
                if (m_nativeArena != null) {
                    m_nativeArena.close();
                    m_nativeArena = null;
                    m_providerCallback = null;
                }
            } finally {
                if (m_ownedProvider != null) {
                    m_ownedProvider.close();
                    m_ownedProvider = null;
                }
            }
        }
    }

    /**
     * Gets the number of color components per pixel.
     *
     * @return Returns said number.
     */
    public int getColorsPerPixel() {
        return m_colorsPerPixel;
    }

    /**
     * Gets the colorspace of the image.
     *
     * @return Returns one of: <code>CS_GRAYSCALE</code>, <code>CS_RGB</code>,
     * <code>CS_YCbRr</code>,
     * <code>CS_CMYK</code>, <code>CS_YCCK</code>, or <code>CS_UNKNOWN</code>.
     */
    public int getColorSpace() {
        return m_colorSpace;
    }

    /**
     * Gets the height of the image in pixels.
     *
     * @return Returns said height.
     */
    public int getHeight() {
        return m_height;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlanarImage getImage() throws LCImageLibException, UserCanceledException {
        return getImage( null, null );
    }

    /**
     * Gets the JPEG image.
     *
     * @param cs The {@link ColorSpace} to use.
     * @return Returns said image.
     */
    public synchronized PlanarImage getImage(ColorSpace cs)
            throws LCImageLibException, UserCanceledException {
        return getImage(null, cs);
    }

    /**
     * Gets the JPEG image.
     *
     * @param thread The thread that will do the getting.
     * @param cs The {@link ColorSpace} to use.
     * @return Returns said image.
     */
    public synchronized PlanarImage getImage(ProgressThread thread, ColorSpace cs)
            throws LCImageLibException, UserCanceledException {
        if (m_image == null) {
            boolean userCanceled = false;
            try {
                readImage(thread, cs);
            } catch (UserCanceledException e) {
                userCanceled = true;
                throw e;
            } finally {
                try {
                    dispose();
                } catch (LCImageLibException e) {
                    //
                    // The JPEG library will complain if dispose() is called
                    // before the entire image has been read ("Application
                    // transferred too few scanlines") because the user clicked
                    // the "Cancel" button.  Therefore, ignore any exception if
                    // this is the case, but rethrow it otherwise.
                    //
                    if (!userCanceled) {
                        throw e;
                    }
                }
            }
        }
        return m_image;
    }

    /**
     * Gets the width of the image in pixels.
     *
     * @return Returns said width.
     */
    public int getWidth() {
        return m_width;
    }

    /**
     * Reads and decodes and encoded set of scanlines from the JPEG image.
     *
     * @param buf The buffer into which to read the image data.
     * @param offset The offset into the buffer where the image data will begin being placed.
     * @param numLines The number of scanlines to read.
     * @return Returns the number of scanlines read.
     */
    public synchronized int readScanLines(byte[] buf, long offset, int numLines)
            throws LCImageLibException {
        if (m_nativePtr.equals(MemorySegment.NULL)) {
            throw new LCImageLibException("JPEG reader is closed");
        }
        if (offset < 0 || numLines < 0) {
            throw new IndexOutOfBoundsException("Negative scanline offset or count");
        }
        final long rowSize;
        final long bytesToRead;
        try {
            rowSize = Math.multiplyExact((long) m_width, m_colorsPerPixel);
            bytesToRead = Math.multiplyExact(rowSize, numLines);
        } catch (ArithmeticException e) {
            throw new IndexOutOfBoundsException("Scanline buffer size overflows");
        }
        if (bytesToRead > buf.length || offset > buf.length - bytesToRead) {
            throw new IndexOutOfBoundsException("Scanline buffer is too small");
        }
        if (numLines == 0) {
            return 0;
        }

        try (Arena scratch = Arena.ofConfined()) {
            final MemorySegment nativeBuffer = scratch.allocate(bytesToRead);
            final int linesRead =
                    (int) readReaderScanLines.invokeExact(m_nativePtr, nativeBuffer, numLines);
            checkProviderFailure();
            if (linesRead < 0) {
                throw new LCImageLibException(lastNativeError());
            }
            if (linesRead > 0) {
                final long bytesRead = rowSize * linesRead;
                MemorySegment.ofArray(buf).asSlice(offset, bytesRead)
                        .copyFrom(nativeBuffer.asSlice(0, bytesRead));
            }
            return linesRead;
        } catch (LCImageLibException e) {
            throw e;
        } catch (IOException e) {
            throw LCJPEGReader.<RuntimeException>throwUnchecked(e);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new LCImageLibException(e);
        }
    }

    @Override
    public void close() throws LCImageLibException {
        dispose();
    }

    /**
     * Begin using the {@link LCImageDataProvider} to get JPEG image data.
     *
     * @param provider The {@link LCImageDataProvider} to get image data from.
     * @param bufSize The size of the buffer to use.
     * @param maxWidth The maximum width of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     */
    private void beginRead(
            LCImageDataProvider provider, int bufSize, int maxWidth, int maxHeight)
            throws LCImageLibException {
        if (bufSize < 2) {
            throw new IllegalArgumentException("JPEG input buffer must be at least 2 bytes");
        }
        final Arena arena = Arena.ofShared();
        final ProviderCallback callback = new ProviderCallback(provider);
        m_nativeArena = arena;
        m_providerCallback = callback;
        try {
            final MemorySegment callbackStub =
                    linker.upcallStub(
                            MethodHandles.lookup()
                                    .findVirtual(
                                            ProviderCallback.class,
                                            "read",
                                            MethodType.methodType(
                                                    int.class, MemorySegment.class, int.class))
                                    .bindTo(callback),
                            readCallbackDescriptor,
                            arena);
            m_nativePtr =
                    (MemorySegment)
                            createReader.invokeExact(callbackStub, bufSize, maxWidth, maxHeight);
            if (m_nativePtr.equals(MemorySegment.NULL)) {
                checkProviderFailure();
                throw new LCImageLibException(lastNativeError());
            }
            checkProviderFailure();
            m_width = (int) getReaderWidth.invokeExact(m_nativePtr);
            m_height = (int) getReaderHeight.invokeExact(m_nativePtr);
            m_colorsPerPixel = (int) getReaderComponents.invokeExact(m_nativePtr);
            m_colorSpace = (int) getReaderColorSpace.invokeExact(m_nativePtr);
        } catch (LCImageLibException | RuntimeException | Error e) {
            releaseNativeResources(e);
            throw e;
        } catch (Throwable e) {
            final LCImageLibException exception = new LCImageLibException(e);
            releaseNativeResources(exception);
            if (e instanceof IOException) {
                throw LCJPEGReader.<RuntimeException>throwUnchecked(e);
            }
            throw exception;
        }
    }

    /**
     * Open a JPEG file for reading.
     *
     * @param fileName The name of the JPEG file to open.
     * @param maxWidth The maximum width of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if necessary. A value of 0
     * means don't scale.
     */
    private void openForReading(String fileName, int maxWidth, int maxHeight)
            throws FileNotFoundException, LCImageLibException, UnsupportedEncodingException {
        m_ownedProvider = new InputStreamImageDataProvider(new FileInputStream(fileName));
        try {
            beginRead(m_ownedProvider, DEFAULT_BUF_SIZE, maxWidth, maxHeight);
        } catch (LCImageLibException | RuntimeException | Error e) {
            m_ownedProvider.close();
            m_ownedProvider = null;
            throw e;
        } catch (Throwable e) {
            m_ownedProvider.close();
            m_ownedProvider = null;
            throw LCJPEGReader.<RuntimeException>throwUnchecked(e);
        }
    }

    private void releaseNativeResources(Throwable cause) {
        if (!m_nativeHandleIsNull()) {
            try {
                final int status = (int) destroyReader.invokeExact(m_nativePtr);
                if (status < 0) {
                    cause.addSuppressed(new LCImageLibException(lastNativeError()));
                }
            } catch (Throwable e) {
                cause.addSuppressed(e);
            }
            m_nativePtr = MemorySegment.NULL;
        }
        if (m_nativeArena != null) {
            try {
                m_nativeArena.close();
            } catch (Throwable e) {
                cause.addSuppressed(e);
            }
            m_nativeArena = null;
            m_providerCallback = null;
        }
    }

    private boolean m_nativeHandleIsNull() {
        return m_nativePtr.equals(MemorySegment.NULL);
    }

    private void checkProviderFailure() throws LCImageLibException {
        final Throwable failure = m_providerCallback.failure;
        if (failure == null) {
            return;
        }
        if (failure instanceof LCImageLibException e) {
            throw e;
        }
        if (failure instanceof IOException e) {
            throw LCJPEGReader.<RuntimeException>throwUnchecked(e);
        }
        if (failure instanceof Error e) {
            throw e;
        }
        if (failure instanceof RuntimeException e) {
            throw e;
        }
        throw new LCImageLibException(failure);
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> RuntimeException throwUnchecked(Throwable failure)
            throws T {
        throw (T) failure;
    }

    private static String lastNativeError() throws LCImageLibException {
        try (Arena scratch = Arena.ofConfined()) {
            final MemorySegment error = scratch.allocate(1024);
            final int length = (int) getReaderError.invokeExact(error, 1024);
            if (length < 0) {
                return "Native JPEG decoder failed without an error message";
            }
            final String message = error.getString(0);
            return message.isBlank()
                    ? "Native JPEG decoder failed without an error message"
                    : message;
        } catch (Throwable e) {
            if (e instanceof RuntimeException exception) {
                throw exception;
            }
            if (e instanceof Error error) {
                throw error;
            }
            throw new LCImageLibException("Unable to read JPEG error message: " + e);
        }
    }

    private static final class ProviderCallback {
        private final LCImageDataProvider provider;
        private volatile Throwable failure;
        private ByteBuffer buffer;

        private ProviderCallback(LCImageDataProvider provider) {
            this.provider = provider;
        }

        private int read(MemorySegment destination, int capacity) {
            try {
                if (buffer == null) {
                    buffer = destination.reinterpret(capacity).asByteBuffer();
                } else if (buffer.capacity() != capacity) {
                    throw new LCImageLibException("JPEG input buffer capacity changed");
                }
                buffer.clear();
                final int bytesRead = provider.getImageData(buffer);
                if (bytesRead > capacity) {
                    throw new LCImageLibException(
                            "Image data provider returned more bytes than the supplied buffer");
                }
                return bytesRead;
            } catch (Throwable e) {
                failure = e;
                return -1;
            }
        }
    }

    /**
     * Reads the JPEG image.
     *
     * @param thread The thread that will do the getting.
     * @param cs The {@link ColorSpace} to use.
     */
    private void readImage(ProgressThread thread, ColorSpace cs)
            throws LCImageLibException, UserCanceledException {
        final ProgressIndicator indicator = ProgressIndicatorFactory.create(thread, m_height);

        // todo: deal with color models other than rgb and grayscale

        if (cs == null) {
            cs =
                    (m_colorsPerPixel == 1
                            ? gray22ColorSpace
                            : m_colorsPerPixel == 3 ? sRGBColorSpace : CMYKColorSpace);
        }

        // Color model for the image (and everything else).
        final ComponentColorModel ccm =
                new ComponentColorModel(cs, false, false, Transparency.OPAQUE,
                        DataBuffer.TYPE_BYTE);

        // Sample model for the readout buffer large enough to hold a tile or a
        // strip of the image.
        final SampleModel jpegTsm = ccm.createCompatibleSampleModel(m_width, TILE_HEIGHT);

        // The readout buffer itself.
        final DataBuffer db = new DataBufferByte(m_colorsPerPixel * m_width * TILE_HEIGHT);

        // Sample model for the output image.
        final SampleModel tsm = ccm.createCompatibleSampleModel(TILE_WIDTH, TILE_HEIGHT);

        // Layout of the output image.
        final ImageLayout layout =
                new ImageLayout(0, 0, m_width, m_height, 0, 0, TILE_WIDTH, TILE_HEIGHT, tsm, ccm);

        // The output image itself, directly allocated in the file cache.
        final CachedImage image = new CachedImage(layout, fileCache);

        // Load Image Data
        for (int tileY = 0, totalLinesRead = 0; totalLinesRead < m_height; tileY++) {
            if (thread != null && thread.isCanceled()) {
                throw new UserCanceledException();
            }

            final int tileHeight = Math.min(TILE_HEIGHT, m_height - totalLinesRead);

            // Wrap the data buffer with a Raster representing the input data.
            final WritableRaster raster =
                    Raster.createWritableRaster(jpegTsm, db, new Point(0, tileY * TILE_HEIGHT));

            final int linesRead = readScanLines(((DataBufferByte) db).getData(), 0, tileHeight);
            if (linesRead <= 0) {
                logger.warn("Problem with readScanLines, returned: {}", linesRead);
                break;
            }

            if (m_hasAdobeSegment && m_colorsPerPixel == 4 && !m_hasAdobeEmbedMarker) {
                //
                // CMYK JPEG images generated by Photoshop are inverted, so we
                // have to invert the data to make it look right.
                //
                LCImageLibUtil.invert((DataBufferByte) db);
            }

            totalLinesRead += linesRead;

            image.setData(raster);
            indicator.incrementBy(linesRead);
        }
        indicator.setIndeterminate(true);
        m_image = image;
    }
}
/* vim:set et sw=4 ts=4: */
