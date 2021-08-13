/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.image.types;

import com.lightcrafts.image.*;
import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.image.export.*;
import com.lightcrafts.image.libs.InputStreamImageDataProvider;
import com.lightcrafts.image.libs.LCImageLibException;
import com.lightcrafts.image.libs.LCJPEGReader;
import com.lightcrafts.image.libs.LCJPEGWriter;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.providers.PreviewImageProvider;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.CachedImage;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.bytebuffer.ArrayByteBuffer;
import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;
import com.lightcrafts.utils.bytebuffer.LCByteBuffer;
import com.lightcrafts.utils.bytebuffer.LCMappedByteBuffer;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XMLUtil;
import com.lightcrafts.utils.xml.XmlNode;
import org.w3c.dom.Document;

import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.RenderedImage;
import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import static com.lightcrafts.image.libs.LCJPEGConstants.CS_UNKNOWN;
import static com.lightcrafts.image.metadata.CoreTags.CORE_IMAGE_ORIENTATION;
import static com.lightcrafts.image.metadata.CoreTags.CORE_RATING;
import static com.lightcrafts.image.metadata.EXIFConstants.*;
import static com.lightcrafts.image.metadata.EXIFTags.*;
import static com.lightcrafts.image.metadata.ImageOrientation.ORIENTATION_UNKNOWN;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_MS_RATING;
import static com.lightcrafts.image.types.JPEGConstants.*;

/**
 * A <code>JPEGImageType</code> is-an {@link ImageType} for JPEG images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class JPEGImageType extends ImageType implements TrueImageTypeProvider {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>JPEGImageType</code>. */
    public static final JPEGImageType INSTANCE = new JPEGImageType();

    /**
     * <code>ExportOptions</code> are {@link ImageFileExportOptions} for JPEG
     * images.
     */
    public static class ExportOptions extends ImageFileExportOptions {

        ////////// public /////////////////////////////////////////////////////

        public final QualityOption quality;

        /**
         * Construct an <code>ExportOptions</code>.
         */
        public ExportOptions() {
            this( INSTANCE );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void readFrom( ImageExportOptionReader r ) throws IOException {
            super.readFrom( r );
            quality.readFrom( r );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void writeTo( ImageExportOptionWriter w ) throws IOException {
            super.writeTo( w );
            quality.writeTo( w );
        }

        ////////// protected //////////////////////////////////////////////////

        /**
         * Construct an <code>ExportOptions</code>.
         *
         * @param instance The singleton instance of {@link ImageType} that
         * this <code>ExportOptions</code> is for.
         */
        protected ExportOptions( ImageType instance ) {
            super( instance );
            quality = new QualityOption( 85, this );
        }

        @Deprecated
        @Override
        protected void save(XmlNode node) {
            super.save( node );
            quality.save( node );
        }

        @Deprecated
        @Override
        protected void restore( XmlNode node ) throws XMLException {
            super.restore( node );
            try {
                quality.restore(node);
            }
            catch (XMLException e) {
                // Files saved with v4.1.6 cause this, just ignore it.
                System.err.println("Failed to restore JPEG quality");
            }
        }
    }

    /**
     * Checks whether the application can export to JPEG images.
     *
     * @return Always returns <code>true</code>.
     */
    @Override
    public boolean canExport() {
        return true;
    }

    /**
     * Gets all JPEG data segments having the given ID.
     *
     * @param imageInfo The image to get the segments for.
     * @param segID The ID of the segments to get.
     * @return Returns a {@link List} of {@link ByteBuffer}s where each
     * {@link ByteBuffer} is the raw bytes of the segment or returns
     * <code>null</code> if there are no such segments.
     * @see #getAllSegments(ImageInfo,byte,JPEGSegmentFilter)
     * @see #getFirstSegment(ImageInfo,byte)
     * @see #getFirstSegment(ImageInfo,byte,JPEGSegmentFilter)
     * @see JPEGImageInfo#getAllSegmentsFor(Byte,JPEGSegmentFilter)
     */
    public static List<ByteBuffer> getAllSegments( ImageInfo imageInfo,
                                                   byte segID )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return getAllSegments( imageInfo, segID, null );
    }

    /**
     * Gets all JPEG data segments having the given ID that satisfy the given
     * {@link JPEGSegmentFilter}.
     *
     * @param imageInfo The image to get the segments for.
     * @param segID The ID of the segments to get.
     * @param filter The {@link JPEGSegmentFilter} to use.
     * @return Returns a {@link List} of {@link ByteBuffer}s where each
     * {@link ByteBuffer} is the raw bytes of the segment or returns
     * <code>null</code> if there are no such segments.
     * @see #getAllSegments(ImageInfo,byte)
     * @see #getFirstSegment(ImageInfo,byte)
     * @see #getFirstSegment(ImageInfo,byte,JPEGSegmentFilter)
     * @see JPEGImageInfo#getAllSegmentsFor(Byte,JPEGSegmentFilter)
     */
    public static List<ByteBuffer> getAllSegments( ImageInfo imageInfo,
                                                   byte segID,
                                                   JPEGSegmentFilter filter )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final AuxiliaryImageInfo auxInfo = imageInfo.getAuxiliaryInfo();
        if ( !(auxInfo instanceof JPEGImageInfo) )
            return null;
        final JPEGImageInfo jpegInfo = (JPEGImageInfo)auxInfo;
        return jpegInfo.getAllSegmentsFor( segID, filter );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getExtensions() {
        return EXTENSIONS;
    }

    /**
     * Gets the first JPEG data segment having the given ID.
     *
     * @param imageInfo The image to get the segments for.
     * @param segID The ID of the segments to get.
     * @return Returns a {@link ByteBuffer} of the raw bytes of the segment or
     * <code>null</code> if there is no such segment.
     * @see #getAllSegments(ImageInfo,byte)
     * @see #getAllSegments(ImageInfo,byte,JPEGSegmentFilter)
     * @see #getFirstSegment(ImageInfo,byte,JPEGSegmentFilter)
     * @see JPEGImageInfo#getFirstSegmentFor(Byte,JPEGSegmentFilter)
     */
    public static ByteBuffer getFirstSegment( ImageInfo imageInfo, byte segID  )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return getFirstSegment( imageInfo, segID, null );
    }

    /**
     * Gets the first JPEG data segment having the given ID that satisfies the
     * given {@link JPEGSegmentFilter}.
     *
     * @param imageInfo The image to get the segments for.
     * @param segID The ID of the segments to get.
     * @param filter The {@link JPEGSegmentFilter} to use.
     * @return Returns a {@link ByteBuffer} of the raw bytes of the segment or
     * <code>null</code> if there is no such segment.
     * @see #getAllSegments(ImageInfo,byte)
     * @see #getAllSegments(ImageInfo,byte,JPEGSegmentFilter)
     * @see #getFirstSegment(ImageInfo,byte)
     * @see JPEGImageInfo#getFirstSegmentFor(Byte,JPEGSegmentFilter)
     */
    public static ByteBuffer getFirstSegment( ImageInfo imageInfo, byte segID,
                                              JPEGSegmentFilter filter )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final AuxiliaryImageInfo auxInfo = imageInfo.getAuxiliaryInfo();
        if ( !(auxInfo instanceof JPEGImageInfo) )
            return null;
        final JPEGImageInfo jpegInfo = (JPEGImageInfo)auxInfo;
        return jpegInfo.getFirstSegmentFor( segID, filter );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dimension getDimension( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        Dimension d = null;
        try {
            LCJPEGReader reader = null;
            try {
                final String path = imageInfo.getFile().getAbsolutePath();
                reader = new LCJPEGReader( path );
                d = new Dimension( reader.getWidth(), reader.getHeight() );
            }
            finally {
                if ( reader != null )
                    reader.dispose();
            }
        }
        catch ( LCImageLibException e ) {
            // ignore
        }
        return d;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ICC_Profile getICCProfile( ImageInfo imageInfo )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException
    {
        final List<ByteBuffer> iccSegBufs = getAllSegments(
            imageInfo, JPEG_APP2_MARKER, new ICCProfileJPEGSegmentFilter()
        );
        if ( iccSegBufs == null ) {
            final String path = imageInfo.getFile().getAbsolutePath();
            try {
                switch ( new LCJPEGReader(path).getColorsPerPixel() ) {
                    case 1:
                        return JAIContext.gray22Profile;
                    case 3: // sRGB or uncalibrated
                        return getICCProfileFromEXIF( imageInfo );
                    case 4:
                        return JAIContext.CMYKProfile;
                    default:
                        throw new BadColorProfileException( path );
                }
            } catch (LCImageLibException e) {
                // ignore
            }
        }
        final byte[] iccProfileData;
        try {
            iccProfileData = assembleICCProfile( iccSegBufs );
        }
        catch ( BufferUnderflowException e ) {
            throw new BadImageFileException( imageInfo.getFile() );
        }
        catch ( IllegalArgumentException e ) {
            throw new BadImageFileException( imageInfo.getFile() );
        }
        try {
            return ICC_Profile.getInstance( iccProfileData );
        }
        catch ( IllegalArgumentException e ) {
            throw new BadColorProfileException( imageInfo.getFile().getName() );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PlanarImage getImage( ImageInfo imageInfo, ProgressThread thread )
        throws BadImageFileException, IOException, UserCanceledException,
               UnknownImageTypeException
    {
        return getImage( imageInfo, thread, 0, 0 );
    }

    /**
     * Gets a JPEG image from the file given by {@link ImageInfo#getFile()}.
     *
     * @param imageInfo The {@link ImageInfo} to get the actual image from.
     * @param thread The thread that will do othe getting.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @return Returns said image data.
     */
    public PlanarImage getImage( ImageInfo imageInfo, ProgressThread thread,
                                 int maxWidth, int maxHeight )
        throws BadImageFileException, IOException, UserCanceledException,
               UnknownImageTypeException
    {
        try {
            ICC_Profile profile;
            try {
                profile = getICCProfile( imageInfo );
            }
            catch ( ColorProfileException e ) {
                profile = null;
            }

            final LCJPEGReader reader = new LCJPEGReader(
                imageInfo.getFile().getAbsolutePath(), maxWidth, maxHeight,
                (JPEGImageInfo)imageInfo.getAuxiliaryInfo()
            );
            final PlanarImage image = reader.getImage(
                thread, profile != null ? new ICC_ColorSpace( profile ) : null
            );

            assert image instanceof CachedImage
                    && image.getTileWidth() == JAIContext.TILE_WIDTH
                    && image.getTileHeight() == JAIContext.TILE_HEIGHT;

            return image;
        }
        catch ( LCImageLibException e ) {
            throw new BadImageFileException( imageInfo.getFile(), e );
        }
    }

    /**
     * Gets a JPEG image from the given byte array.
     *
     * @param buf The byte array to get the JPEG image from.
     * @param offset The offset into the buffer where the JPEG image data
     * starts.
     * @param length The length in bytes of the JPEG image.
     * @param cs The {@link ColorSpace} to use.  It may be <code>null</code>.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @return Returns said image.
     */
    public static RenderedImage getImageFromBuffer( byte[] buf, int offset,
                                                    int length, ColorSpace cs,
                                                    int maxWidth,
                                                    int maxHeight )
        throws BadImageFileException
    {
        final InputStream is = new ByteArrayInputStream( buf, offset, length );
        return getImageFromInputStream( is, cs, maxWidth, maxHeight );
    }

    /**
     * Gets a JPEG image from the given buffer.
     *
     * @param buf The {@link LCByteBuffer} to get the JPEG image from.
     * @param offsetValue The {@link ImageMetaValue} specifying the offset into
     * the buffer where the JPEG image data starts.
     * @param offsetAdjustment An adjustment to be added to
     * <code>offsetValue</code> since some values need adjustment due to file
     * headers.
     * @param lengthValue The {@link ImageMetaValue} specifying the length in
     * bytes of the JPEG image.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @return Returns said image.
     */
    public static RenderedImage getImageFromBuffer( LCByteBuffer buf,
                                                    ImageMetaValue offsetValue,
                                                    int offsetAdjustment,
                                                    ImageMetaValue lengthValue,
                                                    int maxWidth,
                                                    int maxHeight )
        throws BadImageFileException
    {
        return getImageFromBuffer(
            buf, offsetValue, offsetAdjustment, lengthValue, null,
            maxWidth, maxHeight
        );
    }

    /**
     * Gets a JPEG image from the given buffer.
     *
     * @param buf The {@link LCByteBuffer} to get the JPEG image from.
     * @param offsetValue The {@link ImageMetaValue} specifying the offset into
     * the buffer where the JPEG image data starts.
     * @param offsetAdjustment An adjustment to be added to
     * <code>offsetValue</code> since some values need adjustment due to file
     * headers.
     * @param lengthValue The {@link ImageMetaValue} specifying the length in
     * bytes of the JPEG image.
     * @param cs The {@link ColorSpace} to use.  It may be <code>null</code>.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @return Returns said image.
     */
    public static RenderedImage getImageFromBuffer( LCByteBuffer buf,
                                                    ImageMetaValue offsetValue,
                                                    int offsetAdjustment,
                                                    ImageMetaValue lengthValue,
                                                    ColorSpace cs,
                                                    int maxWidth,
                                                    int maxHeight )
        throws BadImageFileException
    {
        if ( buf == null || offsetValue == null || lengthValue == null )
            return null;
        final int offset = offsetValue.getIntValue();
        final int length = lengthValue.getIntValue();
        if ( offset < 0 || length <= 0 )
            return null;
        return getImageFromBuffer(
            buf, offset + offsetAdjustment, length, cs, maxWidth, maxHeight
        );
    }

    /**
     * Gets a JPEG image from the given buffer.
     *
     * @param buf The {@link LCByteBuffer} to get the JPEG image from.
     * @param offset The offset into the buffer where the JPEG image data
     * starts.
     * @param length The length in bytes of the JPEG image.
     * @param cs The {@link ColorSpace} to use.  It may be <code>null</code>.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @return Returns said image.
     */
    public static RenderedImage getImageFromBuffer( LCByteBuffer buf,
                                                    int offset, int length,
                                                    ColorSpace cs,
                                                    int maxWidth,
                                                    int maxHeight )
        throws BadImageFileException
    {
        final byte[] imageBuf;
        try {
            imageBuf = buf.getBytes( offset, length );
        }
        catch ( Exception e ) {
            //
            // Assume that any exception generated by the above is because the
            // image is corrupt.
            //
            throw new BadImageFileException( e );
        }
        final InputStream is = new ByteArrayInputStream( imageBuf );
        return getImageFromInputStream( is, cs, maxWidth, maxHeight );
    }

    /**
     * Gets a JPEG image from the given {@link InputStream}.
     *
     * @param stream The {@link InputStream} to get the image from.
     * @param cs The {@link ColorSpace} to use.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @return Returns said image.
     */
    public static RenderedImage getImageFromInputStream( InputStream stream,
                                                         ColorSpace cs,
                                                         int maxWidth,
                                                         int maxHeight )
        throws BadImageFileException
    {
        try {
            final LCJPEGReader reader = new LCJPEGReader(
                new InputStreamImageDataProvider( stream ),
                maxWidth, maxHeight
            );
            return reader.getImage( cs );
        }
        catch ( UserCanceledException e ) {
            //
            // This never actually happens.
            //
            return null;
        }
        catch ( Exception e ) {
            //
            // Assume that any other exception generated by the above is
            // because the image is corrupt.
            //
            throw new BadImageFileException( e );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "JPEG";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException
    {
        final ImageMetadata metadata = imageInfo.getMetadata();
        final ImageMetadataDirectory dir =
            metadata.findProviderOf( PreviewImageProvider.class );
        if ( dir != null )
            return ((PreviewImageProvider)dir).getPreviewImage(
                imageInfo, maxWidth, maxHeight
            );
        return super.getPreviewImage( imageInfo, maxWidth, maxHeight );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException
    {
        final ImageMetadataDirectory dir =
            imageInfo.getMetadata().getDirectoryFor( EXIFDirectory.class );
        if ( dir == null ) {
            //
            // This should never be null, but just in case ...
            //
            return null;
        }
        final ByteBuffer exifSegBuf =
            getFirstSegment( imageInfo, JPEG_APP1_MARKER );
        if ( exifSegBuf == null )
            return null;

        final ICC_Profile profile = getICCProfile( imageInfo );

        return getImageFromBuffer(
            new ArrayByteBuffer( exifSegBuf ),
            dir.getValue( EXIF_JPEG_INTERCHANGE_FORMAT ),
            EXIF_HEADER_START_SIZE,
            dir.getValue( EXIF_JPEG_INTERCHANGE_FORMAT_LENGTH ),
            profile != null ? new ICC_ColorSpace( profile ) : null,
            0, 0
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final ImageType getTrueImageTypeOf( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        try {
            ByteBuffer buf = getFirstSegment( imageInfo, JPEG_APP4_MARKER );
            if ( buf != null ) {
                SidecarJPEGImageType sidecar = SidecarJPEGImageType.INSTANCE;

                // sanity checking on the contents
                try {
                    if ( sidecar.getLZNDocument(buf) != null )
                        return sidecar;
                }
                catch ( IOException e ) {
                    // not lzn APP4 contents
                }
            }
        }
        catch ( UnknownImageTypeException e ) {
            // should never happen at this stage
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Document getXMP( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final ByteBuffer xmpSegBuf = getFirstSegment(
            imageInfo, JPEG_APP1_MARKER, new XMPJPEGSegmentFilter()
        );
        if ( xmpSegBuf == null)
            return null;
        byte[] xmpBytes;
        if ( xmpSegBuf.hasArray() ) {
        	xmpBytes = xmpSegBuf.array();
        }
        else {
        	xmpBytes = new byte[xmpSegBuf.remaining()];
        	xmpSegBuf.get(xmpBytes);
        }
        final ByteArrayInputStream bis = new ByteArrayInputStream(
            xmpBytes, JPEG_XMP_HEADER_SIZE,
            xmpBytes.length - JPEG_XMP_HEADER_SIZE
        );
        return XMLUtil.readDocumentFrom( bis );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasFastPreview() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public JPEGImageInfo newAuxiliaryInfo( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        return new JPEGImageInfo(
            imageInfo,
            JPEG_APP1_MARKER,   // EXIF or XMP
            JPEG_APP2_MARKER,   // ICC profile
            JPEG_APP4_MARKER,   // LightZone
            JPEG_APPC_MARKER,   // Adobe EPS or PDF
            JPEG_APPD_MARKER,   // IPTC
            JPEG_APPE_MARKER    // Adobe
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ExportOptions newExportOptions() {
        return new ExportOptions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void putImage( ImageInfo imageInfo, PlanarImage image,
                          ImageExportOptions options, Document lznDoc,
                          ProgressThread thread ) throws IOException {
        final ExportOptions jpegOptions = (ExportOptions)options;

        ImageMetadata metadata;
        try {
            metadata = imageInfo.getMetadata();
        }
        catch ( BadImageFileException e ) {
            metadata = new ImageMetadata( this );
        }
        catch ( UnknownImageTypeException e ) {
            metadata = new ImageMetadata( this );
        }

        try {
            final int numComponents = image.getColorModel().getNumComponents();
            final int colorSpace =
                LCJPEGWriter.getColorSpaceFromNumComponents( numComponents );
            if ( colorSpace == CS_UNKNOWN )
                throw new LCImageLibException(
                    "Unsupported number of components: " + numComponents
                );

            final LCJPEGWriter writer = new LCJPEGWriter(
                options.getExportFile().getPath(),
                image.getWidth(), image.getHeight(),
                numComponents, colorSpace,
                jpegOptions.quality.getValue(),
                jpegOptions.resolution.getValue(),
                jpegOptions.resolutionUnit.getValue()
            );

            ICC_Profile profile = ColorProfileInfo.getExportICCProfileFor(
                jpegOptions.colorProfile.getValue()
            );
            if ( profile == null )
                profile = JAIContext.sRGBExportColorProfile;
            writer.setICCProfile( profile );

            if ( lznDoc != null ) {
                final byte[] buf = XMLUtil.encodeDocument( lznDoc, false );
                writer.writeSegment( JPEG_APP4_MARKER, buf );
            }

            writer.putMetadata( metadata );
            writer.putImage( image, thread );
        }
        catch ( LCImageLibException e ) {
            final IOException ioe = new IOException( "JPEG export failed" );
            ioe.initCause( e );
            throw ioe;
        }
    }

    /**
     * Reads all the metadata for a given JPEG image.
     *
     * @param imageInfo The image to read the metadata from.
     */
    @Override
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        BadImageFileException exceptionOnHold = null;

        ////////// EXIF

        final ByteBuffer exifSegBuf = getFirstSegment(
            imageInfo, JPEG_APP1_MARKER, new EXIFJPEGSegmentFilter()
        );
        if ( exifSegBuf != null ) {
            final ImageMetadataReader reader = new EXIFMetadataReader(
                imageInfo, new ArrayByteBuffer( exifSegBuf ), false
            );
            try {
                reader.readMetadata();
            }
            catch ( BadImageFileException e ) {
                //
                // Catch any BadImageFileException and hold it so we can
                // continue and try to read additional metadata.
                //
                exceptionOnHold = e;
            }
        }

        ////////// IPTC

        final ByteBuffer iptcSegBuf = getFirstSegment(
            imageInfo, JPEG_APPD_MARKER, new IPTCJPEGSegmentFilter()
        );
        if ( iptcSegBuf != null ) {
            final ImageMetadataReader reader = new IPTCMetadataReader(
                imageInfo, new ArrayByteBuffer( iptcSegBuf ), this
            );
            try {
                reader.readMetadata();
            }
            catch ( BadImageFileException e ) {
                if ( exceptionOnHold == null )
                    exceptionOnHold = e;
            }
        }

        ////////// XMP

        final Document xmpDoc = getXMP( imageInfo );
        if ( xmpDoc != null ) {
            final ImageMetadata xmpMetadata =
                XMPMetadataReader.readFrom( xmpDoc );
            imageInfo.getCurrentMetadata().mergeFrom( xmpMetadata );
        }

        //////////

        if ( exceptionOnHold != null )
            throw exceptionOnHold;
    }

    /**
     * Writes the metadata for JPEG files back to the metadata inside the JPEG
     * itself.
     *
     * @param imageInfo The image to write the metadata for.
     */
    @Override
    public void writeMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        //
        // We check to see if a foo_jpg.xmp file exists that was used in
        // LightZone versions 2.2-2.4.  If so, we migrate the orientation and
        // rating metadata and delete the XMP file.
        //
        final File xmpFile = new File( imageInfo.getXMPFilename() );
        final boolean xmpExists = xmpFile.exists();

        final ImageMetadata metadata = imageInfo.getCurrentMetadata();

        ImageMetadata xmpMetadata = null;
        final Document oldXMPDoc = INSTANCE.getXMP( imageInfo );
        if ( oldXMPDoc != null )
            xmpMetadata = XMPMetadataReader.readFrom( oldXMPDoc );

        ////////// Core directory

        final ImageMetadataDirectory coreDir =
            metadata.getDirectoryFor( CoreDirectory.class );

        final ImageMetaValue orientation =
            coreDir.getValue( CORE_IMAGE_ORIENTATION );
        if ( orientation != null && (orientation.isEdited() || xmpExists) ) {
            final ImageOrientation xmpOrientation = xmpMetadata != null ?
                xmpMetadata.getOrientation() : ORIENTATION_UNKNOWN;
            modifyMetadata(
                imageInfo, EXIF_ORIENTATION, orientation.getShortValue(),
                xmpOrientation != ORIENTATION_UNKNOWN ?
                    xmpOrientation.getTIFFConstant() : NO_META_VALUE,
                false
            );
            orientation.clearEdited();
        }

        final ImageMetaValue rating = coreDir.getValue( CORE_RATING );
        if ( rating != null && (rating.isEdited() || xmpExists) ) {
            final short xmpRating = xmpMetadata != null ?
                (short)xmpMetadata.getRating() : NO_META_VALUE;
            final short newRating = rating.getShortValue();
            boolean removeRating = false;
            if ( newRating == 0 ) {
                metadata.removeValues( CoreDirectory.class, CORE_RATING );
                metadata.removeValues( EXIFDirectory.class, EXIF_MS_RATING );
                metadata.removeValues( SubEXIFDirectory.class, EXIF_MS_RATING );
                metadata.removeValues( TIFFDirectory.class, TIFF_MS_RATING );
                removeRating = true;
            }
            modifyMetadata(
                imageInfo, EXIF_MS_RATING, newRating, xmpRating, removeRating
            );
            rating.clearEdited();
        }

        // TODO: must do something about unrating a photo

        ////////// IPTC directory

        final ImageMetadataDirectory iptcDir =
            metadata.getDirectoryFor( IPTCDirectory.class );

        if ( iptcDir != null && iptcDir.isChanged() ) {
            //
            // Write both the binary and XMP forms of IPTC metadata: the binary
            // form to enable non-XMP-aware applications to read it and the XMP
            // form to write all the metadata, i.e., the additional IPTC tags
            // present in XMP.
            //
            final byte[] iptcSegBuf = ((IPTCDirectory)iptcDir).encode( true );

            Document newXMPDoc = metadata.toXMP( true, true );
            if ( oldXMPDoc != null )
                newXMPDoc = XMPUtil.mergeMetadata( newXMPDoc, oldXMPDoc );
            final byte[] xmpSegBuf = XMLUtil.encodeDocument( newXMPDoc, true );

            new JPEGCopier().copyAndInsertSegments(
                imageInfo.getFile(),
                new NotJPEGSegmentFilter(
                    new OrJPEGSegmentFilter(
                        new IPTCJPEGSegmentFilter(),
                        new XMPJPEGSegmentFilter()
                    )
                ),
                new JPEGCopier.SegmentInfo( JPEG_APP1_MARKER, xmpSegBuf ),
                new JPEGCopier.SegmentInfo( JPEG_APPD_MARKER, iptcSegBuf )
            );
            iptcDir.clearEdited();
        }

        //////////

        CoreDirectory.syncEditableMetadata( metadata );

        if ( xmpExists )
            xmpFile.delete();
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct a <code>JPEGImageType</code>.
     */
    protected JPEGImageType() {
        // do nothing
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * An {@link EXIFSegmentFinder} is-a {@link JPEGParserEventHandler} that
     * finds the EXIF segment of a JPEG file.
     */
    private static final class EXIFSegmentFinder
        implements JPEGParserEventHandler {

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean gotSegment( byte segID, int segLength, File jpegFile,
                                   LCByteBuffer buf ) throws IOException {
            if ( segID == JPEG_APP1_MARKER &&
                 buf.getEquals( "Exif", "ASCII" ) ) {
                m_exifSegBuf = new LCMappedByteBuffer(
                    jpegFile, buf.position() - 4, segLength - 4,
                    FileChannel.MapMode.READ_WRITE
                );
                return false;
            }
            return true;
        }

        ////////// package ////////////////////////////////////////////////////

        /**
         * Gets the EXIF segment, if any, of the JPEG file.
         *
         * @param jpegFile The JPEG file to get the EXIF segment of.
         * @return Returns an {@link LCMappedByteBuffer} mapped to the EXIF
         * segment or <code>null</code> if there is no EXIF segment.
         */
        LCMappedByteBuffer getEXIFSegmentOf( File jpegFile )
            throws BadImageFileException, IOException
        {
            try (LCMappedByteBuffer buf = new LCMappedByteBuffer(jpegFile)) {
                JPEGParser.parse(this, jpegFile, buf);
                return m_exifSegBuf;
            }
        }

        ////////// private ////////////////////////////////////////////////////

        /** The EXIF segment data is put here. */
        private LCMappedByteBuffer m_exifSegBuf;
    }

    /**
     * An <code>InPlaceModifier</code> attempts to perform an in-place
     * modification of the value of the given EXIF tag in a JPEG file.  The
     * modification will fail if it doesn't contain an existing value for the
     * given EXIF tag.
     */
    private static final class InPlaceModifier
        extends EXIFMetadataReader {

        /**
         * The {@link EXIFParser} just parsed a tag: see if it's the one whose
         * value we want to modify in-place: if it is, modify it and stop.
         * Field type must be ULONG, SLONG, SSHORT, or USHORT.
         *
         * @param tagID The tag ID.
         * @param fieldType The metadata field type.
         * @param numValues Not used.
         * @param byteCount Not used.
         * @param valueOffset The offset of the first value.
         * @param valueOffsetAdjustment Not used.
         * @param subdirOffset Not used.
         * @param imageInfo Not used.
         * @param buf The {@link LCByteBuffer} the raw metadata is in.
         * @param dir Not used.
         */
        @Override
        public void gotTag( int tagID, int fieldType, int numValues,
                            int byteCount, int valueOffset,
                            int valueOffsetAdjustment, int subdirOffset,
                            ImageInfo imageInfo, LCByteBuffer buf,
                            ImageMetadataDirectory dir ) throws IOException {
            if ( tagID == m_tagID ) {
                buf.position( valueOffset );

                switch ( fieldType ) {
                    case EXIF_FIELD_TYPE_SSHORT:
                    case EXIF_FIELD_TYPE_USHORT:
                        // 16-bit
                        buf.putShort( m_newValue );
                        m_succeeded = true;
                        break;
                    case EXIF_FIELD_TYPE_ULONG:
                    case EXIF_FIELD_TYPE_SLONG:
                        // 32-bit
                        buf.putInt( m_newValue );
                        m_succeeded = true;
                        break;
                    default:
                        m_succeeded = false;
                }

                m_exifParser.stopParsing();
            }
        }

        ////////// package ////////////////////////////////////////////////////

        /**
         * Construct an <code>InPlaceModifier</code>.
         *
         * @param jpegInfo The JPEG image to modify.
         * @param exifSegBuf The {@link LCByteBuffer} containing the raw bytes
         * of the EXIF segment.
         */
        InPlaceModifier( ImageInfo jpegInfo, LCByteBuffer exifSegBuf ) {
            super( jpegInfo, exifSegBuf, false );
        }

        /**
         * Attempt to modify the metadata in-place.
         *
         * @param tagID The tag ID of the value to modify.
         * @param newValue The new value.
         * @return Returns <code>true</code> only if the in-place modification
         * succeeded.
         */
        boolean modify( int tagID, short newValue )
            throws BadImageFileException, IOException
        {
            m_newValue = newValue;
            m_tagID = tagID;
            readMetadata();
            if ( m_succeeded ) {
                //
                // Ensure the modification time of the file is updated so the
                // browser will notice.
                //
                FileUtil.touch( m_imageInfo.getFile() );
            }
            return m_succeeded;
        }

        ////////// private ////////////////////////////////////////////////////

        private short m_newValue;
        private boolean m_succeeded;
        private int m_tagID;
    }

    /**
     * A <code>JPEGCopier</code> is-an {@link JPEGParserEventHandler} to copy
     * a JPEG file and insert/replace segment(s) during the copy.
     */
    private static final class JPEGCopier implements JPEGParserEventHandler {

        ////////// public /////////////////////////////////////////////////////

        /**
         * A <code>SegmentInfo</code> holds information about a segment to
         * insert.
         *
         * @see JPEGCopier#copyAndInsertSegments(File,JPEGSegmentFilter,SegmentInfo...)
         */
        static final class SegmentInfo {
            final byte   m_segID;
            final byte[] m_segData;

            SegmentInfo( byte segID, byte[] segData ) {
                m_segID   = segID;
                m_segData = segData;
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean gotSegment( byte segID, int segLength, File jpegFile,
                                   LCByteBuffer buf ) throws IOException {
            if ( segID == JPEG_SOS_MARKER ) {
                //
                // We've run into the start of the JPEG image data: copy the
                // remainder of the JPEG file as-is; but first insert the new
                // segment(s) if we haven't already done so.
                //
                if ( m_segInfo != null )
                    insertSegments();
                buf.skipBytes( -2 );
                copy( buf, m_raf, buf.remaining() );
                m_copied = true;
                return false;
            }

            //
            // Check whether the current segment should be copied.
            //
            if ( m_segFilter != null ) {
                //
                // Extract a small chunk of the segment data, enough for the
                // filter to work on.
                //
                final int chunkSize = Math.min( segLength, 32 );
                final byte[] chunk = buf.getBytes( buf.position(), chunkSize );
                final ByteBuffer chunkBuf = ByteBuffer.wrap( chunk );
                if ( !m_segFilter.accept( segID, chunkBuf ) )
                    return true;        // don't copy the current segment
            }

            //
            // Insert the segment(s) if we haven't already done so.
            //
            if ( m_segInfo != null )
                insertSegments();

            //
            // Copy the current segment as-is.
            //
            buf.skipBytes( -4 );
            copy( buf, m_raf, segLength + 4 );
            return true;
        }

        ////////// package ////////////////////////////////////////////////////

        /**
         * Copy a JPEG file inserting a new segment(s).
         *
         * @param jpegFile The JPEG file to copy.
         * @param segFilter The {@link JPEGSegmentFilter} to use to filter out
         * segments that should not be copied.
         * @param segments The segments to insert.
         */
        void copyAndInsertSegments( File jpegFile, JPEGSegmentFilter segFilter,
                                    SegmentInfo... segments )
            throws BadImageFileException, IOException
        {
            m_segFilter = segFilter;
            m_segInfo = segments;

            File newFile = null;
            try {
                newFile = File.createTempFile("LZcp", null, jpegFile.getParentFile());
                try (LCMappedByteBuffer buf = new LCMappedByteBuffer(jpegFile);
                    RandomAccessFile raf = new RandomAccessFile( newFile, "rw" )) {
                    m_raf = raf;
                    m_raf.writeByte( JPEG_MARKER_BYTE );
                    m_raf.writeByte( JPEG_SOI_MARKER );
                    JPEGParser.parse( this, jpegFile, buf );
                }
                finally {
                    if ( m_copied ) {
                        //
                        // In order to rename an image file, we must make sure
                        // the files involved are closed first.
                        //
                        ImageInfo.closeAll();
                        //
                        // Ensure that the buf.close() takes effect.
                        //
                        for (int i = 0; !jpegFile.delete() && i < 5; i++) {
                            System.gc();
                        }
                        FileUtil.renameFile( newFile, jpegFile );
                    }
                }
            }
            finally {
                //
                // We need to ensure newFile is deleted regardless of whether
                // (1) the copy failed or (2) the copy succeeded but the rename
                // failed; hence this extra try/finally.
                //
                if ( newFile != null )
                    newFile.delete();
            }
        }

        ////////// private ////////////////////////////////////////////////////

        /**
         * Copies bytes from the given {@link LCByteBuffer} to the given
         * {@link RandomAccessFile}.  Bytes are read starting at the buffer's
         * current position and written to the file's current position.  Upon
         * completion, the buffer's and file's positions are advanced by the
         * number of bytes copied.
         *
         * @param from The {@link LCByteBuffer} to copy from.
         * @param to The {@link RandomAccessFile} to copy to.
         * @param byteCount The number of bytes to copy.
         */
        private static void copy( LCByteBuffer from, RandomAccessFile to,
                                  int byteCount ) throws IOException {
            final byte[] chunk = new byte[ Math.min( byteCount, 64 * 1024 ) ];
            while ( byteCount > 0 ) {
                final int bytesToCopy = Math.min( byteCount, chunk.length );
                from.get( chunk, 0, bytesToCopy );
                to.write( chunk, 0, bytesToCopy );
                byteCount -= bytesToCopy;
            }
        }

        /**
         * Inserts the new segments.
         */
        private void insertSegments() throws IOException {
            for ( SegmentInfo seg : m_segInfo )
                if ( seg.m_segData != null && seg.m_segData.length > 0 ) {
                    m_raf.writeByte( JPEG_MARKER_BYTE );
                    m_raf.writeByte( seg.m_segID );
                    m_raf.writeShort( seg.m_segData.length + 2 );
                    m_raf.write( seg.m_segData );
                }
            m_segInfo = null;
        }

        /**
         * This is set to <code>true</code> only when the copy has been
         * successfully completed.
         */
        private boolean m_copied;

        /**
         * The {@link RandomAccessFile} to copy to.
         */
        private RandomAccessFile m_raf;

        /**
         * The {@link JPEGSegmentFilter} to use, if any.
         */
        private JPEGSegmentFilter m_segFilter;

        /**
         * The segments to insert.
         */
        private SegmentInfo[] m_segInfo;
    }

    /**
     * Modify the EXIF metadata of a JPEG image as non-destructively and as
     * efficiently as possible.
     *
     * @param jpegInfo The JPEG image to modify the EXIF metadata of.
     * @param tagID The EXIF tag whose value is to be modified.
     * @param newValue The new value.
     * @param oldXMPValue The old value from XMP or 0 if none.
     * @param removeValue If <code>true</code>, remove the value for the given
     * tag instead.
     */
    public static void modifyMetadata( ImageInfo jpegInfo, int tagID,
                                       short newValue, short oldXMPValue,
                                       boolean removeValue )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final File jpegFile = jpegInfo.getFile();
        ImageMetadata metadata = jpegInfo.getCurrentMetadata();

        final Document oldXMPDoc = INSTANCE.getXMP( jpegInfo );

        //
        // We can attempt cases 1 and 2 only if there's no XMP metadata for the
        // JPEG.  If there is XMP metadata, we need to modify that instead
        // (because XMP metadata always wins).
        //
        if ( oldXMPValue == NO_META_VALUE ) {
            //
            // Case 1a: see if the JPEG has no EXIF metadata at all: if not,
            // create a new JPEG containing a newly constructed EXIF segment.
            //
            // Case 1b: if we're removing the value, we also need to create a
            // new JPEG containing a newly constructed EXIF segment.
            //
            try (LCMappedByteBuffer exifSegBuf = new EXIFSegmentFinder().getEXIFSegmentOf(jpegFile)) {
                if (exifSegBuf == null || removeValue) {
                    metadata = metadata.prepForExport(INSTANCE, true);
                    final ByteBuffer newEXIFSegBuf =
                            EXIFEncoder.encode(metadata, true);
                    new JPEGCopier().copyAndInsertSegments(
                            jpegInfo.getFile(), null,
                            new JPEGCopier.SegmentInfo(
                                    JPEG_APP1_MARKER, newEXIFSegBuf.array()
                            )
                    );
                    return;
                }

                //
                // Case 2: see if the JPEG's EXIF metadata contains the tag: if so,
                // modify it in-place.
                //
                final InPlaceModifier modifier = new InPlaceModifier(jpegInfo, exifSegBuf);
                if (modifier.modify(tagID, newValue))
                    return;
            }
        }

        //
        // Case 3: the JPEG has EXIF metadata, but no relevant tag: we're
        // forced to modify the XMP metadata instead (and leave the existing
        // EXIF metadata alone), so create a new JPEG containing the modified
        // XMP metadata.
        //
        Document newXMPDoc = metadata.toXMP( true, true );
        if ( oldXMPDoc != null )
            newXMPDoc = XMPUtil.mergeMetadata( newXMPDoc, oldXMPDoc );
        final byte[] newXMPSegBuf = XMLUtil.encodeDocument( newXMPDoc, true );
        new JPEGCopier().copyAndInsertSegments(
            jpegInfo.getFile(),
            new NotJPEGSegmentFilter( new XMPJPEGSegmentFilter() ),
            new JPEGCopier.SegmentInfo( JPEG_APP1_MARKER, newXMPSegBuf )
        );
    }

    /**
     * Assemble a complete ICC profile from one or more chunks of data
     * extracted from one or more APP2 segments in a JPEG file.
     *
     * @param list The {@link List} of {@link ByteBuffer}s containing the
     * chunks of ICC profile data.
     * @return Returns the raw ICC profile data (with the header stripped) or
     * <code>null</code> if none.
     */
    private static byte[] assembleICCProfile( List<ByteBuffer> list ) {
        if ( list.size() == 1 ) {
            //
            // The easy and common case of just 1 segment for the entire
            // profile.
            //
            final ByteBuffer buf = list.get( 0 );
            return ByteBufferUtil.getBytes(
                buf,
                ICC_PROFILE_HEADER_SIZE,
                buf.limit() - ICC_PROFILE_HEADER_SIZE
            );
        }

        //
        // The harder case of a profile being split across multiple segments.
        //
        final ByteBuffer firstBuf = list.get( 0 );
        final int numSegments = firstBuf.get( 13 );
        final ByteBuffer[] sortedList = new ByteBuffer[ numSegments ];
        int totalProfileSize = 0;

        //
        // Although they probably are, we don't assume that the profile chunks
        // are in the file in order.  Each chunk has its correct index, so we
        // sort the chunks.
        //
        // While we're iterating over all the chunks anyway, also compute the
        // total profile size.
        //
        for ( ByteBuffer buf : list ) {
            final int chunkIndex = buf.get( 12 ) - 1;
            sortedList[ chunkIndex ] = buf;
            totalProfileSize += buf.limit() - ICC_PROFILE_HEADER_SIZE;
        }

        //
        // Finally, assemble the chunks into a single, complete ICC profile.
        //
        final byte[] iccProfileData = new byte[ totalProfileSize ];
        int dataOffset = 0;
        for ( ByteBuffer buf : sortedList ) {
            final int chunkSize = buf.limit() - ICC_PROFILE_HEADER_SIZE;
            ByteBufferUtil.getBytes(
                buf, ICC_PROFILE_HEADER_SIZE, iccProfileData, dataOffset,
                chunkSize
            );
            dataOffset += chunkSize;
        }
        return iccProfileData;
    }

    /**
     * Gets the {@link ICC_Profile} specified in the EXIF metadata for the
     * ColorSpace tag.
     *
     * @param imageInfo The image to get the EXIF metadata from.
     * @return If ColorSpace contains "sRGB", returns that profile; if it
     * contains "uncalibrated", returns the Adobe RGB profile.  This may not be
     * correct in all cases, but it's better than using the sRGB profile.  If
     * there is no EXIF metadata or it doesn't contain the ColorSpace tag,
     * returns <code>null</code>.
     */
    private static ICC_Profile getICCProfileFromEXIF( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        try {
            final ImageMetaValue colorSpace =
                imageInfo.getMetadata().getValue(
                    EXIFDirectory.class, EXIF_COLOR_SPACE
                );
            if ( colorSpace != null )
                switch ( colorSpace.getIntValue() ) {
                    case 1:     // sRGB
                        return JAIContext.sRGBColorProfile;
                    default:    // uncalibrated or something else
                        return JAIContext.adobeRGBProfile;
                }
        }
        catch ( UnknownImageTypeException e ) {
            // ignore
        }
        return null;
    }

    /**
     * All the possible filename extensions for JPEG files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "jpg", "jpe", "jpeg"
    };

    /**
     * The value that is used to indicate that no pre-existing metadata value
     * is present.
     */
    private static final short NO_META_VALUE = 0;
}
/* vim:set et sw=4 ts=4: */
