/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.libs;

import java.awt.*;
import java.awt.image.*;
import java.awt.color.ColorSpace;
import java.io.*;
import java.nio.ByteBuffer;
import javax.media.jai.PlanarImage;
import javax.media.jai.ImageLayout;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.CachedImage;
import com.lightcrafts.image.types.AdobeJPEGSegmentFilter;
import com.lightcrafts.image.types.JPEGImageInfo;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.UserCanceledException;

import static com.lightcrafts.image.types.JPEGConstants.*;

/**
 * An <code>LCJPEGReader</code> is a Java wrapper around the LibJPEG library
 * for reading JPEG images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <a href="http://www.ijg.org/">LibJPEG</a>
 */
public final class LCJPEGReader {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * The default buffer size for use with
     * {@link #LCJPEGReader(LCImageDataProvider).
     */
    public static final int DEFAULT_BUF_SIZE = 32 * 1024;

    /**
     * Construct an <code>LCJPEGReader</code>.
     *
     * @param fileName The name of the JPEG file to read.
     */
    public LCJPEGReader( String fileName )
        throws FileNotFoundException, LCImageLibException
    {
        this( fileName, 0, 0, null );
    }

    /**
     * Construct an <code>LCJPEGReader</code>.
     *
     * @param fileName The name of the JPEG file to read.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param jpegInfo The {@link JPEGImageInfo} of the image, or
     * <code>null</code>.
     */
    public LCJPEGReader( String fileName, int maxWidth, int maxHeight,
                         JPEGImageInfo jpegInfo )
        throws FileNotFoundException, LCImageLibException
    {
        openForReading( fileName, maxWidth, maxHeight );
        if ( jpegInfo != null ) {
            final ByteBuffer segBuf = jpegInfo.getFirstSegmentFor(
                JPEG_APPE_MARKER, new AdobeJPEGSegmentFilter()
            );
            m_hasAdobeSegment = segBuf != null;
        }
    }

    /**
     * Construct an <code>LCJPEGReader</code>.
     *
     * @param provider The {@link LCImageDataProvider} to get image data from.
     */
    public LCJPEGReader( LCImageDataProvider provider )
        throws LCImageLibException
    {
        this( provider, 0, 0 );
    }

    /**
     * Construct an <code>LCJPEGReader</code>.
     *
     * @param provider The {@link LCImageDataProvider} to get image data from.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     */
    public LCJPEGReader( LCImageDataProvider provider, int maxWidth,
                         int maxHeight )
        throws LCImageLibException
    {
        beginRead( provider, DEFAULT_BUF_SIZE, maxWidth, maxHeight );
    }

    /**
     * Construct an <code>LCJPEGReader</code>.
     *
     * @param provider The {@link LCImageDataProvider} to get image data from.
     * @param bufSize The size of the buffer (in bytes) to use.
     */
    public LCJPEGReader( LCImageDataProvider provider, int bufSize )
        throws LCImageLibException
    {
        beginRead( provider, bufSize, 0, 0 );
    }

    /**
     * Construct an <code>LCJPEGReader</code>.
     *
     * @param provider The {@link LCImageDataProvider} to get image data from.
     * @param bufSize The size of the buffer to use.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     */
    public LCJPEGReader( LCImageDataProvider provider, int bufSize,
                         int maxWidth, int maxHeight )
        throws LCImageLibException
    {
        beginRead( provider, bufSize, maxWidth, maxHeight );
    }

    /**
     * Dispose of an <code>LCJPEGReader</code>.
     */
    public native void dispose() throws LCImageLibException;

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
     * <code>CS_YCbRr</code>, <code>CS_CMYK</code>, <code>CS_YCCK</code>, or
     * <code>CS_UNKNOWN</code>.
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
     * Gets the JPEG image.
     *
     * @return Returns said image.
     */
    public PlanarImage getImage()
        throws LCImageLibException, UserCanceledException
    {
        return getImage( null, null );
    }

    /**
     * Gets the JPEG image.
     *
     * @param cs The {@link ColorSpace} to use.
     * @return Returns said image.
     */
    public synchronized PlanarImage getImage( ColorSpace cs )
        throws LCImageLibException, UserCanceledException
    {
        return getImage( null, cs );
    }

    /**
     * Gets the JPEG image.
     *
     * @param thread The thread that will do the getting.
     * @param cs The {@link ColorSpace} to use.
     * @return Returns said image.
     */
    public synchronized PlanarImage getImage( ProgressThread thread,
                                              ColorSpace cs )
        throws LCImageLibException, UserCanceledException
    {
        if ( m_image == null ) {
            boolean userCanceled = false;
            try {
                readImage( thread, cs );
            }
            catch ( UserCanceledException e ) {
                userCanceled = true;
                throw e;
            }
            finally {
                try {
                    dispose();
                }
                catch ( LCImageLibException e ) {
                    //
                    // The JPEG library will complain if dispose() is called
                    // before the entire image has been read ("Application
                    // transferred too few scanlines") because the user clicked
                    // the "Cancel" button.  Therefore, ignore any exception if
                    // this is the case, but rethrow it otherwise.
                    //
                    if ( !userCanceled )
                        throw e;
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
     * @param offset The offset into the buffer where the image data will begin
     * being placed.
     * @param numLines The number of scanlines to read.
     * @return Returns the number of scanlines read.
     */
    public native synchronized int readScanLines( byte[] buf, long offset,
                                                  int numLines )
        throws LCImageLibException;

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Finalize this class by calling {@link #dispose()}.
     */
    protected void finalize() throws Throwable {
        super.finalize();
        dispose();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Begin using the {@link LCImageDataProvider} to get JPEG image data.
     *
     * @param provider The {@link LCImageDataProvider} to get image data from.
     * @param bufSize The size of the buffer to use.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     */
    private native void beginRead( LCImageDataProvider provider, int bufSize,
                                   int maxWidth, int maxHeight )
        throws LCImageLibException;

    /**
     * Open a JPEG file for reading.
     *
     * @param fileName The name of the JPEG file to open.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     */
    private native void openForReading( String fileName, int maxWidth,
                                        int maxHeight )
        throws FileNotFoundException, LCImageLibException;

    /**
     * Reads the JPEG image.
     *
     * @param thread The thread that will do the getting.
     * @param cs The {@link ColorSpace} to use.
     */
    private void readImage( ProgressThread thread, ColorSpace cs )
        throws LCImageLibException, UserCanceledException
    {
        ProgressIndicator indicator = null;
        if ( thread != null )
            indicator = thread.getProgressIndicator();
        if ( indicator != null )
            indicator.setMaximum( m_height );

        // todo: deal with color models other than rgb and grayscale

        if ( cs == null )
            cs = (m_colorsPerPixel == 1 ? JAIContext.gray22ColorSpace :
                  m_colorsPerPixel == 3 ? JAIContext.sRGBColorSpace :
                                          JAIContext.CMYKColorSpace);

        // Color model for the image (and everything else).
        final ComponentColorModel ccm = new ComponentColorModel(
            cs, false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE
        );

        // Sample model for the readout buffer large enough to hold a tile or a
        // strip of the image.
        final SampleModel jpegTsm = ccm.createCompatibleSampleModel(
            m_width, JAIContext.TILE_HEIGHT
        );

        // The readout buffer itself.
        final DataBuffer db = new DataBufferByte(
            m_colorsPerPixel * m_width * JAIContext.TILE_HEIGHT
        );

        // Sample model for the output image.
        final SampleModel tsm = ccm.createCompatibleSampleModel(
            JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT
        );

        // Layout of the output image.
        final ImageLayout layout = new ImageLayout(
            0, 0, m_width, m_height, 0, 0, JAIContext.TILE_WIDTH,
            JAIContext.TILE_HEIGHT, tsm, ccm
        );

        // The output image itself, directly allocated in the file cache.
        final CachedImage image =
            new CachedImage( layout, JAIContext.fileCache );

        // Load Image Data
        int tileY = 0;
        int totalLinesRead = 0;
        while ( totalLinesRead < m_height ) {
            if ( thread != null && thread.isCanceled() )
                throw new UserCanceledException();
            final int tileHeight =
                Math.min( JAIContext.TILE_HEIGHT, m_height - totalLinesRead );

            // Wrap the data buffer with a Raster representing the input data.
            final WritableRaster raster = Raster.createWritableRaster(
                jpegTsm, db, new Point( 0, tileY * JAIContext.TILE_HEIGHT )
            );

            final int linesRead = readScanLines(
                ((DataBufferByte)db).getData(), 0, tileHeight
            );
            if ( linesRead <= 0 ) {
                System.out.println("Problem with readScanLines, returned: " + linesRead );
                break;
            }

            if ( m_hasAdobeSegment && m_colorsPerPixel == 4 ) {
                //
                // CMYK JPEG images generated by Photoshop are inverted, so we
                // have to invert the data to make it look right.
                //
                final byte data[] = ((DataBufferByte) db).getData();
                for (int i = 0; i < data.length; i++)
                    data[i] = (byte)~data[i];
            }

            totalLinesRead += linesRead;
            tileY++;

            image.setData( raster );

            if ( indicator != null )
                indicator.incrementBy( linesRead );
        }
        if ( indicator != null )
            indicator.setIndeterminate( true );
        m_image = image;
    }

    /**
     * The number of colors per pixel.  This is set from native code.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private int m_colorsPerPixel;

    /**
     * The colorspace of the image, one of: <code>CS_GRAYSCALE</code>,
     * <code>CS_RGB</code>, <code>CS_YCbRr</code>, <code>CS_CMYK</code>,
     * <code>CS_YCCK</code>, or <code>CS_UNKNOWN</code>.  This is set from
     * native code.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private int m_colorSpace;

    /**
     * This is <code>true</code> only if the JPEG file has an Adobe (APPE)
     * segment.
     */
    private boolean m_hasAdobeSegment;

    /**
     * The image height.  This is set from native code.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private int m_height;

    /**
     * The actual end-result image.
     */
    private PlanarImage m_image;

    /**
     * This is where the native code stores a pointer to the <code>JPEG</code>
     * native data structure.  Do not touch this from Java except to compare it
     * to zero.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private long m_nativePtr;

    /**
     * The image width.  This is set from native code.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private int m_width;

    static {
        System.loadLibrary( "LCJPEG" );
    }
}
/* vim:set et sw=4 ts=4: */
