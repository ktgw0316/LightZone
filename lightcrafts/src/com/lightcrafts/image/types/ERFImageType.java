package com.lightcrafts.image.types;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.*;

import java.awt.image.RenderedImage;
import java.io.IOException;

import static com.lightcrafts.image.metadata.TIFFTags.TIFF_JPEG_INTERCHANGE_FORMAT;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH;

/**
 * A <code>ERFImageType</code> is-a {@link com.lightcrafts.image.types.RawImageType} for ERF (Epson Raw)
 * images.
 */
public final class ERFImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>ERFImageType</code>. */
    public static final ERFImageType INSTANCE = new ERFImageType();

    /**
     * {@inheritDoc}
     */
    public String[] getExtensions() {
        return EXTENSIONS;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "ERF";
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        if (!USE_EMBEDDED_PREVIEW)
            return super.getPreviewImage(imageInfo, maxWidth, maxHeight);

        final ImageMetadataDirectory dir =
            imageInfo.getMetadata().getDirectoryFor( TIFFDirectory.class );
        if ( dir == null ) {
            //
            // This should never be null, but just in case ...
            //
            return null;
        }
        final RenderedImage image = JPEGImageType.getImageFromBuffer(
            imageInfo.getByteBuffer(),
            dir.getValue( TIFF_JPEG_INTERCHANGE_FORMAT ), 0,
            dir.getValue( TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH ),
            maxWidth, maxHeight
        );
        return  image != null ?
                image : super.getPreviewImage( imageInfo, maxWidth, maxHeight );
    }

    public boolean hasFastPreview() {
        return true;
    }

    /**
     * Reads all the metadata for a given ERF image file.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        final ImageMetadataReader reader = new TIFFMetadataReader( imageInfo );
        MetadataUtil.removePreviewMetadataFrom( reader.readMetadata() );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>ERFImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private ERFImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for ERF files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String EXTENSIONS[] = {
        "erf"
    };
}
