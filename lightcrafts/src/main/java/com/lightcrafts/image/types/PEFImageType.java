/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;
import java.awt.image.RenderedImage;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.metadata.makernotes.PentaxDirectory;
import com.lightcrafts.image.UnknownImageTypeException;

import static com.lightcrafts.image.metadata.makernotes.PentaxTags.*;

/**
 * A <code>PEFImageType</code> is-a {@link RawImageType} for PEF (PEntax raw
 * Format) images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class PEFImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>PEFImageType</code>. */
    public static final PEFImageType INSTANCE = new PEFImageType();

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
        return "PEF";
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

        final ImageMetadata metadata = imageInfo.getMetadata();
        final ImageMetadataDirectory dir =
            metadata.getDirectoryFor( PentaxDirectory.class );
        if ( dir != null ) {
            // TODO: verify that this is actually an sRGB image, what about Adobe RGB shooting, etc.?
            final RenderedImage image = JPEGImageType.getImageFromBuffer(
                imageInfo.getByteBuffer(),
                dir.getValue( PENTAX_PREVIEW_IMAGE_START ), 0,
                dir.getValue( PENTAX_PREVIEW_IMAGE_LENGTH ),
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
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        final ImageMetadataReader reader = new TIFFMetadataReader( imageInfo );
        MetadataUtil.removePreviewMetadataFrom( reader.readMetadata() );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>PEFImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private PEFImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for PEF files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "pef"
    };
}
/* vim:set et sw=4 ts=4: */
