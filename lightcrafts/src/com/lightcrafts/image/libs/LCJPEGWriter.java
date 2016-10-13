/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.libs;

import java.awt.*;
import java.awt.color.ICC_Profile;
import java.awt.image.*;
import java.io.*;
import java.nio.ByteBuffer;

import org.w3c.dom.Document;

import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.image.export.ResolutionOption;
import com.lightcrafts.image.export.ResolutionUnitOption;
import com.lightcrafts.image.types.TIFFConstants;
import com.lightcrafts.utils.xml.XMLUtil;

import static com.lightcrafts.image.libs.LCJPEGConstants.*;
import static com.lightcrafts.image.types.AdobeConstants.*;
import static com.lightcrafts.image.types.JPEGConstants.*;

/**
 * An <code>LCJPEGWriter</code> is a Java wrapper around the LibJPEG library
 * for writing JPEG images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <a href="http://www.ijg.org/">LibJPEG</a>
 */
public final class LCJPEGWriter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>LCJPEGWriter</code>.
     *
     * @param fileName The name of the JPEG file to write to.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param colorsPerPixel The number of color components per pixel.
     * @param colorSpace The colorspace of the input image; must be one of
     * {@link LCJPEGConstants#CS_GRAYSCALE}, {@link LCJPEGConstants#CS_RGB},
     * {@link LCJPEGConstants#CS_YCbRr}, {@link LCJPEGConstants#CS_CMYK}, or
     * {@link LCJPEGConstants#CS_YCCK}.
     * @param quality Image quality: 0-100.
     * @param resolution The resolution (in pixels per unit).
     * @param resolutionUnit The resolution unit; must be either
     * {@link TIFFConstants#TIFF_RESOLUTION_UNIT_CM} or
     * {@link TIFFConstants#TIFF_RESOLUTION_UNIT_INCH}.
     */
    public LCJPEGWriter( String fileName, int width, int height,
                         int colorsPerPixel, int colorSpace, int quality,
                         int resolution, int resolutionUnit )
        throws IOException, LCImageLibException
    {
        m_exportWidth = width;
        m_exportHeight = height;
        m_resolution = resolution;
        m_resolutionUnit = resolutionUnit;
        openForWriting(
            fileName, width, height, colorsPerPixel, colorSpace, quality
        );
    }

    /**
     * Construct an <code>LCJPEGWriter</code>.
     *
     * @param receiver The {@link LCImageDataReceiver} to send image data to.
     * @param bufSize The size of the buffer (in bytes) to use.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param colorsPerPixel The number of color components per pixel.
     * @param colorSpace The colorspace of the input image; must be one of
     * {@link LCJPEGConstants#CS_GRAYSCALE}, {@link LCJPEGConstants#CS_RGB},
     * {@link LCJPEGConstants#CS_YCbRr}, {@link LCJPEGConstants#CS_CMYK}, or
     * {@link LCJPEGConstants#CS_YCCK}.
     * @param quality Image quality: 0-100.
     */
    public LCJPEGWriter( LCImageDataReceiver receiver, int bufSize, int width,
                         int height, int colorsPerPixel, int colorSpace,
                         int quality )
        throws LCImageLibException
    {
        m_exportWidth = width;
        m_exportHeight = height;
        m_resolution = ResolutionOption.DEFAULT_VALUE;
        m_resolutionUnit = ResolutionUnitOption.DEFAULT_VALUE;
        beginWrite(
            receiver, bufSize, width, height, colorsPerPixel, colorSpace,
            quality
        );
    }

    /**
     * Dispose of an <code>LCJPEGWriter</code>.
     */
    public native void dispose();

    /**
     * Gets the colorspace constant used by libJPEG from the number of color
     * components of an image.
     *
     * @param numComponents The number of color components.
     * @return Returns said colorspace constant.
     */
    public static int getColorSpaceFromNumComponents( int numComponents ) {
        switch ( numComponents ) {
            case 1:
                return CS_GRAYSCALE;
            case 3:
                return CS_RGB;
            case 4:
                return CS_CMYK;
            default:
                return CS_UNKNOWN;
        }
    }

    /**
     * Puts an image, compressing it into a JPEG.
     *
     * @param image The image to compress into a JPEG.
     */
    public void putImage( RenderedImage image ) throws LCImageLibException {
        putImage( image, null );
    }

    /**
     * Puts an image, compressing it into a JPEG.
     *
     * @param image The image to compress into a JPEG.
     * @param thread The {@link ProgressThread} that is putting the JPEG.
     */
    public void putImage( RenderedImage image, ProgressThread thread )
        throws LCImageLibException
    {
        try {
            final int bands = image.getSampleModel().getNumBands();
            if ( bands == 4 /* CMYK */ ) {
                //
                // Write a mimicked APPE segment so 3rd-party applications that
                // read CYMK JPEG images will think the creator is Photoshop
                // and thus know to invert the image data.
                //
                writeAdobeSegment( ADOBE_CTT_UNKNOWN );
            }
            writeImage( image, thread );
        }
        finally {
            dispose();
        }
    }

    /**
     * Puts the given {@link ImageMetadata} into the JPEG file.  Only EXIF and
     * IPTC metadata are put.  This <i>must</i> be called only once and prior
     * to {@link #putImage(RenderedImage,ProgressThread)}.
     *
     * @param metadata The {@link ImageMetadata} to put.
     */
    public void putMetadata( ImageMetadata metadata )
        throws LCImageLibException
    {
        metadata = metadata.prepForExport(
            JPEGImageType.INSTANCE, m_exportWidth, m_exportHeight,
            m_resolution, m_resolutionUnit, false
        );

        ////////// Write EXIF metadata, if any ////////////////////////////////

        //
        // The binary form of EXIF metadata, if any, has to go before XMP
        // metadata, otherwise Windows Explorer won't see the EXIF metadata.
        //
        final ImageMetadataDirectory exifDir =
            metadata.getDirectoryFor( EXIFDirectory.class );
        if ( exifDir != null ) {
            final byte[] exifSegBuf =
                EXIFEncoder.encode( metadata, true ).array();
            //ByteBufferUtil.dumpToFile( exifBuf, "/tmp/jpg.exif" );
            writeSegment( JPEG_APP1_MARKER, exifSegBuf );
        }

        ////////// Write XMP metadata /////////////////////////////////////////

        final Document xmpDoc = metadata.toXMP( false, true );
        final byte[] xmpSegBuf = XMLUtil.encodeDocument( xmpDoc, true );
        writeSegment( JPEG_APP1_MARKER, xmpSegBuf );

        ////////// Write IPTC metadata, if any ////////////////////////////////

        final ImageMetadataDirectory iptcDir =
            metadata.getDirectoryFor( IPTCDirectory.class );
        if ( iptcDir != null ) {
            final byte[] iptcSegBuf = ((IPTCDirectory)iptcDir).encode( true );
            if ( iptcSegBuf != null )
                writeSegment( JPEG_APPD_MARKER, iptcSegBuf );
        }
    }

    /**
     * Sets the ICC profile of the JPEG image.  This <i>must</i> be called only
     * once and prior to {@link #putImage(RenderedImage,ProgressThread)}.
     *
     * @param iccProfile The {@link ICC_Profile} to set.
     */
    public void setICCProfile( ICC_Profile iccProfile )
        throws LCImageLibException
    {
        final byte[] iccProfileData = iccProfile.getData();
        final int chunkSize = JPEG_MAX_SEGMENT_SIZE - ICC_PROFILE_HEADER_SIZE;
        //
        // We must calculate the total size of all the segments including a
        // header per segment.
        //
        int totalSize = iccProfileData.length
            + ( (iccProfileData.length - 1) / chunkSize + 1 )
              * ICC_PROFILE_HEADER_SIZE;

        //
        // Given the total size, we can calculate the number of segments
        // needed.
        //
        final int numSegments = (totalSize - 1) / JPEG_MAX_SEGMENT_SIZE + 1;

        //
        // Now split the profile data across the number of segments with a
        // header per segment.
        //
        for ( int i = 0; i < numSegments; ++i ) {
            final int segSize = Math.min( JPEG_MAX_SEGMENT_SIZE, totalSize );
            final ByteBuffer buf = ByteBuffer.allocate( segSize );
            ByteBufferUtil.put( buf, "ICC_PROFILE", "ASCII" );
            buf.put( (byte)0 );
            buf.put( (byte)(i + 1) );
            buf.put( (byte)numSegments );
            buf.put(
                iccProfileData, i * chunkSize,
                segSize - ICC_PROFILE_HEADER_SIZE
            );
            writeSegment( JPEG_APP2_MARKER, buf.array() );
            totalSize -= JPEG_MAX_SEGMENT_SIZE;
        }
    }

    /**
     * Compresses and writes a raw set of scanlines to the JPEG image.
     *
     * @param buf The buffer from which to compress the image data.
     * @param offset The offset into the buffer where the image data will begin
     * being read.
     * @param numLines The number of scanlines to compress.
     * @return Returns the number of scanlines written.
     */
    public native synchronized int writeScanLines( byte[] buf, int offset,
                                                   int numLines,
                                                   int lineStride )
        throws LCImageLibException;

    /**
     * Write an APP segment to the JPEG file.
     *
     * @param marker The APP segment marker.
     * @param buf The buffer comprising the raw binary contents for the
     * segment.
     */
    public native void writeSegment( int marker, byte[] buf )
        throws LCImageLibException;

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Finalize this class by calling {@link #dispose()}.
     */
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Begin using the {@link LCImageDataProvider} to get JPEG image data.
     *
     * @param receiver The {@link LCImageDataReceiver} to send image data to.
     * @param bufSize The size of the buffer (in bytes) to use.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param colorsPerPixel The number of color components per pixel.
     * @param colorSpace The colorspace of the input image; must be one of
     * {@link LCJPEGConstants#CS_GRAYSCALE}, {@link LCJPEGConstants#CS_RGB},
     * {@link LCJPEGConstants#CS_YCbRr}, {@link LCJPEGConstants#CS_CMYK}, or
     * {@link LCJPEGConstants#CS_YCCK}.
     * @param quality Image quality: 0-100.
     */
    private native void beginWrite( LCImageDataReceiver receiver, int bufSize,
                                    int width, int height, int colorsPerPixel,
                                    int colorSpace, int quality )
        throws LCImageLibException;

    /**
     * Opens a JPEG file for writing.
     *
     * @param fileName The name of the JPEG file to write to.
     * @param width The width of the image in pixels.
     * @param height The height of the image in pixels.
     * @param colorsPerPixel The number of color components per pixel.
     * @param colorSpace The colorspace of the input image; must be one of
     * {@link LCJPEGConstants#CS_GRAYSCALE}, {@link LCJPEGConstants#CS_RGB},
     * {@link LCJPEGConstants#CS_YCbRr}, {@link LCJPEGConstants#CS_CMYK}, or
     * {@link LCJPEGConstants#CS_YCCK}.
     * @param quality Image quality: 0-100.
     */
    private void openForWriting( String fileName, int width, int height,
                                        int colorsPerPixel, int colorSpace,
                                        int quality )
        throws IOException, LCImageLibException
    {
        byte[] fileNameUtf8 = ( fileName + '\000' ).getBytes( "UTF-8" );
        openForWriting(
            fileNameUtf8, width, height, colorsPerPixel, colorSpace, quality
        );
    }

    private native void openForWriting( byte[] fileNameUtf8, int width, int height,
                                        int colorsPerPixel, int colorSpace,
                                        int quality )
        throws IOException, LCImageLibException;

    /**
     * Writes an Adobe (APPE) segment.  The bytes of an Adobe segment are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0-4&nbsp;</td>
     *        <td>String: <code>Adobe</code></td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5-6&nbsp;</td>
     *        <td>DCTEncode/DCTDecode version number: 0x0065</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7-8&nbsp;</td>
     *        <td>
     *          flags0 0x8000 bit:
     *          <blockquote>
     *            <table border="0" cellpadding="0">
     *              <tr><td>0 =&nbsp;</td><td>downsampling</td></tr>
     *              <tr><td>1 =&nbsp;</td><td>blend</td></tr>
     *            </table>
     *          </blockquote>
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td>9-10&nbsp;</td>
     *        <td>flags1</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>11&nbsp;</td>
     *        <td>
     *          Color transformation code:
     *          <blockquote>
     *            <table border="0" cellpadding="0">
     *              <tr><td>0 =&nbsp;</td><td>unknown</td></tr>
     *              <tr><td>1 =&nbsp;</td><td>YcbCr</td></tr>
     *              <tr><td>2 =&nbsp;</td><td>YCCK</td></tr>
     *            </table>
     *          </blockquote>
     *        </td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Notes:
     *  <ul>
     *    <li>
     *      For a color transform code of 0 (unknown), 3-channel images are
     *      assumed to be RGB and 4-channel images are assumed to be CMYK.
     *    </li>
     *    <li>
     *      Although the Adobe technical note says the version number is 0x65,
     *      all Adobe-generated files in existence have version 0x64.
     *    </li>
     *  </ul>
     *
     * @param colorTransformationCode One of <code>ADOBE_CTT_UNKNOWN</code>,
     * <code>ADOBE_CTT_YCBCR</code>, or <code>ADOBE_CTT_YCCK</code>.
     * @see "Adobe Technical Note #5116: Supporting the DCT Filters in
     * PostScript Level 2, Adobe Systems, Inc., November 24, 1992, p. 23."
     */
    private void writeAdobeSegment( byte colorTransformationCode )
        throws LCImageLibException
    {
        final ByteBuffer buf = ByteBuffer.allocate( ADOBE_APPE_SEGMENT_SIZE );
        ByteBufferUtil.put( buf, "Adobe", "ASCII" );
        buf.putShort( (short)0x0064 );  // version number
        buf.putShort( (short)0 );       // flags0
        buf.putShort( (short)0 );       // flags1
        buf.put( colorTransformationCode );
        writeSegment( JPEG_APPE_MARKER, buf.array() );
    }

    /**
     * Writes an image, compressing it into a JPEG.
     *
     * @param image The image to compress into a JPEG.
     * @param thread The {@link ProgressThread} to use, if any.
     */
    private void writeImage( RenderedImage image, ProgressThread thread )
        throws LCImageLibException
    {
        /* if ( image.getSampleModel().getDataType() != DataBuffer.TYPE_BYTE )
            throw new IllegalArgumentException( "Image data type is not byte" ); */

        final int imageWidth = image.getWidth();
        final int imageHeight = image.getHeight();
        final Rectangle stripRect = new Rectangle();
        int stripHeight = 8;

        ProgressIndicator indicator = null;
        if ( thread != null )
            indicator = thread.getProgressIndicator();
        if ( indicator != null )
            indicator.setMaximum( imageHeight );

        final int bands = image.getSampleModel().getNumBands();

        final WritableRaster rasterBuffer =
                Raster.createInterleavedRaster(
                        DataBuffer.TYPE_BYTE,
                        imageWidth, stripHeight, bands * imageWidth, bands,
                        bands == 1 ? new int[]{ 0 } :
                        bands == 3 ? new int[]{ 0, 1, 2 } :
                                     new int[]{ 0, 1, 2, 3 },
                        new Point(0, 0)
                );

        for ( int y = 0; y < imageHeight; y += stripHeight ) {
            if ( thread != null && thread.isCanceled() )
                return;
            stripHeight = Math.min( stripHeight, imageHeight - y );
            stripRect.setBounds( 0, y, imageWidth, stripHeight );

            final WritableRaster raster = (WritableRaster) rasterBuffer.createTranslatedChild(0, y);

            // Prefetch tiles, uses all CPUs
            if (image instanceof PlanarImage)
                ((PlanarImage) image).getTiles(((PlanarImage) image).getTileIndices(raster.getBounds()));

            image.copyData(raster);

            final DataBufferByte db = (DataBufferByte)raster.getDataBuffer();

            final ComponentSampleModel csm = (ComponentSampleModel)raster.getSampleModel();

            final int[] offsets = csm.getBandOffsets();
            int offset = offsets[0];
            for (int i = 1; i < offsets.length; i++)
                offset = Math.min(offset, offsets[i]);

            if ( bands == 4 /* CMYK */ ) {
                //
                // A long-standing Photoshop bug is that CMYK images are stored
                // inverted.  To be compatible with Photoshop, we have to
                // invert CMYK images too.
                //
                final byte[] data = db.getData();
                for ( int i = 0; i < data.length; ++i )
                    data[i] = (byte)~data[i];
            }

            final int lineStride = csm.getScanlineStride();
            final int written = writeScanLines( db.getData(), offset, stripHeight, lineStride );

            if ( written != stripHeight )
                throw new LCImageLibException(
                    "something is wrong: " + written + " != " + stripHeight
                );

            if ( indicator != null )
                indicator.incrementBy( stripHeight );
        }

        if ( indicator != null )
            indicator.setIndeterminate( true );
    }

    /**
     * The height of the image as exported.
     */
    private final int m_exportHeight;

    /**
     * The width of the image as exported.
     */
    private final int m_exportWidth;

    /**
     * The resolution (in pixels per unit) of the image as exported.
     */
    private final int m_resolution;

    /**
     * The resolution unit of the image as exported.
     */
    private final int m_resolutionUnit;

    /**
     * This is where the native code stores a pointer to the <code>JPEG</code>
     * native data structure.  Do not touch this from Java except to compare it
     * to zero.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private long m_nativePtr;

    static {
        System.loadLibrary( "LCJPEG" );
    }
}
/* vim:set et sw=4 ts=4: */
