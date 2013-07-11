/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.*;

import java.awt.image.RenderedImage;
import java.io.IOException;

import static com.lightcrafts.image.metadata.makernotes.CanonTags.CANON_CR2_PREVIEW_IMAGE_LENGTH;
import static com.lightcrafts.image.metadata.makernotes.CanonTags.CANON_CR2_PREVIEW_IMAGE_START;

/**
 * A <code>SRWImageType</code> is-a {@link com.lightcrafts.image.types.RawImageType} for SRW (Samsung Raw)
 * images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class SRWImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>SRWImageType</code>. */
    public static final SRWImageType INSTANCE = new SRWImageType();

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
        return "SRW";
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        //
        // Note that this is a weird special case for SRW files.  The tag
        // values below are actually stored in the TIFF directory.
        //
        final ImageMetadataDirectory dir =
            imageInfo.getMetadata().getDirectoryFor( TIFFDirectory.class );
        if ( dir != null ) {
            final RenderedImage image = JPEGImageType.getImageFromBuffer(
                imageInfo.getByteBuffer(),
                dir.getValue( CANON_CR2_PREVIEW_IMAGE_START ), 0,
                dir.getValue( CANON_CR2_PREVIEW_IMAGE_LENGTH ),
                maxWidth, maxHeight
            );
            if ( image != null )
                return image;
        }
        return super.getPreviewImage( imageInfo, maxWidth, maxHeight );
    }

    public boolean hasFastPreview() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return getPreviewImage( imageInfo, 640, 480 );
    }

    /**
     * {@inheritDoc}
     */
    /* public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException
    {
        final ImageMetadataDirectory dir =
            imageInfo.getMetadata().getDirectoryFor( TIFFDirectory.class );
        if ( dir == null ) {
            //
            // This should never be null, but just in case ...
            //
            return null;
        }
        return ((ThumbnailImageProvider)dir).getThumbnailImage( imageInfo );
    } */

    /**
     * Reads all the metadata for a given SRW image file.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        final TIFFMetadataReader reader = new TIFFMetadataReader( imageInfo );
        final ImageMetadata metadata = reader.readMetadata();
        MetadataUtil.removePreviewMetadataFrom( metadata );
        MetadataUtil.removeWidthHeightFrom( metadata );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>SRWImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private SRWImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for SRW files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String EXTENSIONS[] = {
        "srw"
    };
}