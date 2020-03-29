/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import com.lightcrafts.image.*;
import com.lightcrafts.image.color.ColorProfileInfo;
import com.lightcrafts.image.export.*;
import com.lightcrafts.image.libs.LCImageLibException;
import com.lightcrafts.image.libs.LCTIFFReader;
import com.lightcrafts.image.libs.LCTIFFWriter;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.values.ByteMetaValue;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.metadata.values.UndefinedMetaValue;
import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.opimage.CachedImage;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.bytebuffer.ByteBufferUtil;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XMLUtil;
import com.lightcrafts.utils.xml.XmlDocument;
import com.lightcrafts.utils.xml.XmlNode;
import org.w3c.dom.Document;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import java.awt.*;
import java.awt.color.ICC_Profile;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.lightcrafts.image.metadata.TIFFTags.*;
import static com.lightcrafts.image.types.AdobeConstants.PHOTOSHOP_5_THUMBNAIL_RESOURCE_ID;
import static com.lightcrafts.image.types.AdobeConstants.PHOTOSHOP_CREATOR_CODE;
import static com.lightcrafts.image.types.TIFFConstants.TIFF_COMPRESSION_LZW;
import static com.lightcrafts.image.types.TIFFConstants.TIFF_COMPRESSION_NONE;

/**
 * A <code>TIFFImageType</code> is-an {@link ImageType} for TIFF images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class TIFFImageType extends ImageType implements TrueImageTypeProvider {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>TIFFImageType</code>. */
    public static final TIFFImageType INSTANCE = new TIFFImageType();

    /**
     * <code>ExportOptions</code> are {@link ImageFileExportOptions} for TIFF
     * images.
     */
    public static class ExportOptions extends ImageFileExportOptions {

        ////////// public /////////////////////////////////////////////////////

        public final BitsPerChannelOption   bitsPerChannel;
        public final LZWCompressionOption   lzwCompression;
        public final MultilayerOption       multilayer;

        /**
         * Construct an <code>ExportOptions</code>.
         */
        public ExportOptions() {
            this( INSTANCE );
        }

        /**
         * {@inheritDoc}
         */
        public void readFrom( ImageExportOptionReader r ) throws IOException {
            super.readFrom( r );
            bitsPerChannel.readFrom( r );
            lzwCompression.readFrom( r );
            multilayer.readFrom( r );
        }

        /**
         * {@inheritDoc}
         */
        public void writeTo( ImageExportOptionWriter w ) throws IOException {
            super.writeTo( w );
            bitsPerChannel.writeTo( w );
            lzwCompression.writeTo( w );
            multilayer.writeTo( w );
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
            bitsPerChannel = new BitsPerChannelOption( 8, this );
            lzwCompression = new LZWCompressionOption( false, this );
            multilayer     = new MultilayerOption( false, this );
        }

        @Deprecated
        protected void save( XmlNode node ) {
            super.save( node );
            bitsPerChannel.save( node );
            lzwCompression.save( node );
            multilayer.save( node );
        }

        @Deprecated
        protected void restore( XmlNode node ) throws XMLException {
            super.restore( node );
            bitsPerChannel.restore( node );
            lzwCompression.restore( node );
            if (node.hasChild( multilayer.getName() )) {
                multilayer.restore( node );
            }
        }
    }

    /**
     * Checks whether the application can export to TIFF images.
     *
     * @return Always returns <code>true</code>.
     */
    public boolean canExport() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Dimension getDimension( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final ImageMetadata metadata = imageInfo.getMetadata();
        final ImageMetadataDirectory tiffDir =
            metadata.getDirectoryFor( TIFFDirectory.class );
        if ( tiffDir == null )
            return null;
        final ImageMetaValue width = tiffDir.getValue( TIFF_IMAGE_WIDTH );
        final ImageMetaValue height = tiffDir.getValue( TIFF_IMAGE_LENGTH );
        return width != null && height != null ?
            new Dimension( width.getIntValue(), height.getIntValue() ) : null;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getExtensions() {
        return EXTENSIONS;
    }

    /**
     * {@inheritDoc}
     */
    public ICC_Profile getICCProfile( ImageInfo imageInfo )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException
    {
        final ImageMetaValue v = imageInfo.getMetadata().getValue(
            TIFFDirectory.class, TIFF_ICC_PROFILE
        );
        if ( v != null ) {
            final byte[] iccData = ((UndefinedMetaValue)v).getUndefinedValue();
            try {
                return ICC_Profile.getInstance( iccData );
            }
            catch ( IllegalArgumentException e ) {
                throw new BadColorProfileException(
                    imageInfo.getFile().getName()
                );
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public PlanarImage getImage( ImageInfo imageInfo, ProgressThread thread )
        throws BadImageFileException, UserCanceledException, UnsupportedEncodingException
    {
        return getImage( imageInfo, thread, false );
    }

    /**
     * Gets the actual image data of an image.
     *
     * @param imageInfo The {@link ImageInfo} to get the actual image from.
     * @param thread The thread doing the getting.
     * @param read2nd If <code>true</code>, read the second TIFF image (if
     * present).
     * @return Returns said image data.
     */
    public static PlanarImage getImage( ImageInfo imageInfo,
                                        ProgressThread thread,
                                        boolean read2nd )
        throws BadImageFileException, UserCanceledException, UnsupportedEncodingException
    {
        try {
            final String fileName = imageInfo.getFile().getAbsolutePath();
            final PlanarImage image;

            if (true) {
                final LCTIFFReader reader =
                    new LCTIFFReader( fileName, read2nd );
                image = reader.getImage( thread );

                assert image instanceof CachedImage
                        && image.getTileWidth() == JAIContext.TILE_WIDTH
                        && image.getTileHeight() == JAIContext.TILE_HEIGHT;
            } else {
                final PlanarImage tiffImage =
                    new LCTIFFReader.TIFFImage( fileName );
                if (tiffImage.getTileWidth() != JAIContext.TILE_WIDTH ||
                    tiffImage.getTileHeight() != JAIContext.TILE_HEIGHT) {
                    final RenderingHints formatHints = new RenderingHints(
                        JAI.KEY_IMAGE_LAYOUT,
                        new ImageLayout(
                            0, 0, JAIContext.TILE_WIDTH, JAIContext.TILE_HEIGHT,
                            tiffImage.getSampleModel(),
                            tiffImage.getColorModel()
                        )
                    );
                    final ParameterBlock pb = new ParameterBlock();
                    pb.addSource(tiffImage);
                    pb.add(tiffImage.getSampleModel().getDataType());
                    image = JAI.create("Format", pb, formatHints);
                    image.setProperty(JAIContext.PERSISTENT_CACHE_TAG, Boolean.TRUE);
                } else
                    image = tiffImage;
            }

            return image;
        }
        catch ( LCImageLibException e ) {
            throw new BadImageFileException( imageInfo.getFile(), e );
        }
    }

    /**
     * Gets the image specified by the JPEGInterchangeFormat tag, if any.
     *
     * @param imageInfo The {@link ImageInfo} to get the actual preview image
     * from.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @return Returns said image data or <code>null</code> if none.
     */
    public static RenderedImage getJPEGInterchangeImage( ImageInfo imageInfo,
                                                         int maxWidth,
                                                         int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final ImageMetadata metadata = imageInfo.getMetadata();
        final ImageMetadataDirectory dir =
            metadata.getDirectoryFor( TIFFDirectory.class );
        if ( dir == null )
            return null;
        return JPEGImageType.getImageFromBuffer(
            imageInfo.getByteBuffer(),
            dir.getValue( TIFF_JPEG_INTERCHANGE_FORMAT ), 0,
            dir.getValue( TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH ),
            maxWidth, maxHeight
        );
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "TIFF";
    }

    /**
     * Gets the actual thumbnail image.  We normally don't get a thumbnail
     * image for TIFF files, but we have to handle a special case for files
     * generated by Photoshop that can embed a thumbnail image as part of the
     * value of the {@link TIFFTags#TIFF_PHOTOSHOP_IMAGE_RESOURCES} tag.
     *
     * @param imageInfo The {@link ImageInfo} to get the actual preview image
     * from.
     * @return Returns said image data.
     */
    public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return getPhotoshopThumbnail( imageInfo );
    }

    /**
     * {@inheritDoc}
     */
    public ImageType getTrueImageTypeOf( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        //
        // Some camera vendors, in their infinite wisdom, decided, for some of
        // their cameras, to create raw files with a .TIF extension.  Since the
        // file isn't really a TIFF file, the ImageType returned by
        // ImageType.determineTypeByExtensionOf() is wrong.  If we were to try
        // to read one of these raw files as a TIFF, reading the full-sized
        // image doesn't work (you get the thumbnail instead).
        //
        // We therefore have to probe the image file to determine whether it's
        // really one of these raw image files.
        //
        for ( TrueImageTypeProvider p : m_rawImageProbes ) {
            final ImageType t = p.getTrueImageTypeOf( imageInfo );
            if ( t != null )
                return t;
        }

        try {
            final Document lznDoc = getLZNDocumentImpl( imageInfo );
            if ( lznDoc != null ) {
                final XmlDocument xmlDoc =
                    new XmlDocument( lznDoc.getDocumentElement() );
                // The original image may be in the same file,
                // or referenced through a path pointer:
                final XmlNode root = xmlDoc.getRoot();
                // (tag copied from ui.editor.Document)
                final XmlNode imageNode = root.getChild( "Image" );
                // (tag written in export())
                if ( imageNode.hasAttribute( "self" ) )
                    return MultipageTIFFImageType.INSTANCE;
                else
                    return SidecarTIFFImageType.INSTANCE;
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
    public Document getXMP( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return getXMP( imageInfo, TIFFDirectory.class );
    }

    /**
     * {@inheritDoc}
     */
    public ExportOptions newExportOptions() {
        return new ExportOptions();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void putImage( ImageInfo imageInfo, PlanarImage image,
                          ImageExportOptions options, Document lznDoc,
                          ProgressThread thread ) throws IOException {
        final ExportOptions tiffOptions = (ExportOptions)options;
        final File exportFile = options.getExportFile();
        File tempFile = null;

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

        final LCTIFFWriter writer;
        try {
            if ( tiffOptions.multilayer.getValue() ) {
                File originalFile = imageInfo.getFile();
                if ( exportFile.equals( originalFile ) ) {
                    tempFile = File.createTempFile( "LightZone", "tif" );
                    FileUtil.copyFile( originalFile, tempFile );
                    originalFile = tempFile;
                }
                writer = new LCTIFFWriter(
                    exportFile.getAbsolutePath(),
                    originalFile.getAbsolutePath(),
                    tiffOptions.resizeWidth.getValue(),
                    tiffOptions.resizeHeight.getValue(),
                    tiffOptions.resolution.getValue(),
                    tiffOptions.resolutionUnit.getValue()
                );
            } else {
                writer = new LCTIFFWriter(
                    options.getExportFile().getAbsolutePath(),
                    tiffOptions.resizeWidth.getValue(),
                    tiffOptions.resizeHeight.getValue(),
                    tiffOptions.resolution.getValue(),
                    tiffOptions.resolutionUnit.getValue()
                );
            }

            writer.setIntField(
                TIFF_COMPRESSION,
                tiffOptions.lzwCompression.getValue() ?
                    TIFF_COMPRESSION_LZW : TIFF_COMPRESSION_NONE
            );

            ICC_Profile profile = ColorProfileInfo.getExportICCProfileFor(
                tiffOptions.colorProfile.getValue()
            );
            if ( profile == null )
                profile = JAIContext.sRGBExportColorProfile;
            writer.setICCProfile( profile );

            if ( lznDoc != null ) {
                final byte[] buf = XMLUtil.encodeDocument( lznDoc, false );
                writer.setByteField( TIFF_LIGHTZONE, buf );
            }

            writer.putMetadata( metadata );
            writer.putImageStriped( image, thread );
            // TODO: allow users to write tiled TIFFs if they want
            // writer.putImageTiled( image, thread );
            writer.dispose();
        }
        catch ( LCImageLibException e ) {
            final IOException ioe = new IOException( "TIFF export failed" );
            ioe.initCause( e );
            throw ioe;
        }
        finally {
            if ( tempFile != null )
                tempFile.delete();
        }
    }

    /**
     * Reads all the metadata for a given TIFF image.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final TIFFMetadataReader reader = new TIFFMetadataReader( imageInfo );
        final ImageMetadata metadata = reader.readMetadata();
        final Document xmpDoc = getXMP( imageInfo );
        if ( xmpDoc != null ) {
            final ImageMetadata xmpMetadata =
                XMPMetadataReader.readFrom( xmpDoc );
            metadata.mergeFrom( xmpMetadata );
        }
    }

    /**
     * Writes the metadata for TIFF files to an XMP sidecar file.
     *
     * @param imageInfo The image to write the metadata for.
     */
    public void writeMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final File xmpFile = new File( imageInfo.getXMPFilename() );
        final ImageMetadata metadata = imageInfo.getCurrentMetadata();
        XMPMetadataWriter.mergeInto( metadata, xmpFile );
        metadata.clearEdited();
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Gets the XMP document from the XMP packet tag.
     *
     * @param imageInfo The TIFF or DNG image to get the XMP document from.
     * @param dirClass The {@link Class} of the {@link ImageMetadataDirectory}
     * to get the XMP packet from.
     * @return Returns said {@link Document} or <code>null</code> if none.
     */
    static Document getXMP(
        ImageInfo imageInfo, Class<? extends ImageMetadataDirectory> dirClass
    )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final ImageMetadata metadata = imageInfo.getMetadata();
        final ImageMetaValue xmpValue =
            metadata.getValue( dirClass, TIFF_XMP_PACKET );
        if ( xmpValue == null )
            return null;
        final byte[] xmpBytes = XMPUtil.getXMPDataFrom( xmpValue );
        if ( xmpBytes == null )
            return null;
        return XMLUtil.readDocumentFrom( new ByteArrayInputStream( xmpBytes ) );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct a <code>TIFFImageType</code>.
     */
    protected TIFFImageType() {
        // do nothing
    }

    /**
     * Gets the LightZone document (if any) from the given TIFF image.
     *
     * @param imageInfo The image to get the LightZone document from.
     * @return Returns said {@link Document} or <code>null</code> if none.
     */
    protected static Document getLZNDocumentImpl( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final ImageMetadata metadata = imageInfo.getMetadata();
        final ImageMetaValue lznValue =
            metadata.getValue( TIFFDirectory.class, TIFF_LIGHTZONE );
        if ( lznValue != null ) {
            final byte[] buf = ((ByteMetaValue)lznValue).getByteValues();
            final InputStream in = new ByteArrayInputStream( buf );
            return XMLUtil.readDocumentFrom( in );
        }
        //
        // For backwards compatibility, check for LightZone data inside XMP
        // metadata.
        //
        final ImageMetaValue xmpValue =
            metadata.getValue( TIFFDirectory.class, TIFF_XMP_PACKET );
        final byte[] xmp = XMPUtil.getXMPDataFrom( xmpValue );
        if ( xmp != null ) {
            final InputStream in = new ByteArrayInputStream( xmp );
            final Document xmpDoc = XMLUtil.readDocumentFrom( in );
            final Document lznDoc = XMPUtil.getLZNDocumentFrom( xmpDoc );
            if ( lznDoc != null )
                return lznDoc;
        }
        return null;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Gets the thumbnail image from the thumbnail resource block from the
     * value of the {@link TIFFTags#TIFF_PHOTOSHOP_IMAGE_RESOURCES} tag.
     *
     * @param imageInfo The {@link ImageInfo} to get the actual thumbnail image
     * from.
     * @return Returns said image data.
     * @see <i>Photoshop CS File Formats Specification</i>, Adobe Systems,
     * Incorporated, October 2003.
     */
    private static RenderedImage getPhotoshopThumbnail( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final ImageMetadata metadata = imageInfo.getMetadata();
        final ImageMetaValue photoshop = metadata.getValue(
            TIFFDirectory.class, TIFF_PHOTOSHOP_IMAGE_RESOURCES
        );
        if ( photoshop == null )
            return null;
        final byte[] bytes = ((ByteMetaValue)photoshop).getByteValues();
        ByteBuffer buf = ByteBuffer.wrap( bytes );

        //
        // Go through all the Image Resource Blocks looking for the ones for
        // the thumbnail.
        //
        int blockSize;
        try {
            while ( true ) {
                final String blockSig =
                    ByteBufferUtil.getString( buf, 4, "ASCII" );
                if ( !blockSig.equals( PHOTOSHOP_CREATOR_CODE ) )
                    return null;
                final int blockID = ByteBufferUtil.getUnsignedShort( buf );
                final int blockNameLen = ByteBufferUtil.getUnsignedByte( buf );
                ByteBufferUtil.skipBytes( buf, blockNameLen );
                if ( (blockNameLen + 1) % 2 == 1 )
                    ByteBufferUtil.skipBytes( buf, 1 );
                blockSize = buf.getInt();
                if ( blockID == PHOTOSHOP_5_THUMBNAIL_RESOURCE_ID )
                    break;
                if ( blockSize % 2 == 1 ) // must be made even
                    ++blockSize;
                ByteBufferUtil.skipBytes( buf, blockSize );
            }
        }
        catch ( BufferUnderflowException e ) {
            return null;
        }
        catch ( IllegalArgumentException e ) {
            return null;
        }

        // Wrap just the thumbnail resource block.
        buf = ByteBuffer.wrap( bytes, buf.position(), blockSize );

        if ( buf.getInt() != 1 )        // format: should always = 1
            return null;
        buf.getInt();                   // skip width
        buf.getInt();                   // skip height
        buf.getInt();                   // skip scanline size
        buf.getInt();                   // skip decompressed memory size
        final int jpegSize = buf.getInt();
        buf.getShort();                 // skip bits per pixel
        buf.getShort();                 // skip number of planes

        // Finally, the buffer is positioned at the JPEG image data.
        return JPEGImageType.getImageFromBuffer(
            bytes, buf.position(), jpegSize, null, 0, 0
        );
    }

    /**
     * All the possible filename extensions for TIFF files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "tif", "tiff", "iiq"
    };

    /**
     * The global static list of all the {@link TrueImageTypeProvider}s that
     * are used to probe raw image types.
     */
    private static final ArrayList<TrueImageTypeProvider> m_rawImageProbes =
        new ArrayList<TrueImageTypeProvider>();

    static {
        // TODO: is there a better way to do this?
        m_rawImageProbes.add( CanonTIFFRawImageProbe.INSTANCE );
        m_rawImageProbes.add( PhaseOneTIFFRawImageProbe.INSTANCE );
    }
}
/* vim:set et sw=4 ts=4: */
