/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;
import java.awt.image.RenderedImage;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.ImageMetadataReader;
import com.lightcrafts.image.metadata.makernotes.OlympusDirectory;
import com.lightcrafts.image.metadata.MetadataUtil;
import com.lightcrafts.image.metadata.TIFFMetadataReader;

import static com.lightcrafts.image.metadata.makernotes.OlympusTags.*;

/**
 * A <code>ORFImageType</code> is-a {@link RawImageType} for ORF (Olympus Raw
 * Format) images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ORFImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>ORFImageType</code>. */
    public static final ORFImageType INSTANCE = new ORFImageType();

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
        return "ORF";
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        RenderedImage image = null;

        final ImageMetadataDirectory dir =
            imageInfo.getMetadata().getDirectoryFor( OlympusDirectory.class );

        if ( dir != null ) {
            //
            // This should never be null, but just in case ...
            //
            image = JPEGImageType.getImageFromBuffer(
                imageInfo.getByteBuffer(),
                dir.getValue( OLYMPUS_PREVIEW_IMAGE_START ), 0,
                dir.getValue( OLYMPUS_PREVIEW_IMAGE_LENGTH ),
                maxWidth, maxHeight
            );
            if ( image == null )
                image = JPEGImageType.getImageFromBuffer(
                    imageInfo.getByteBuffer(),
                    dir.getValue( OLYMPUS_PREVIEW_IMAGE_START_2 ), 0,
                    dir.getValue( OLYMPUS_PREVIEW_IMAGE_LENGTH_2 ),
                    maxWidth, maxHeight
                );
            if ( image == null )
                image = JPEGImageType.getImageFromBuffer(
                    imageInfo.getByteBuffer(),
                    dir.getValue( OLYMPUS_CS_PREVIEW_IMAGE_START ), 0,
                    dir.getValue( OLYMPUS_CS_PREVIEW_IMAGE_LENGTH ),
                    maxWidth, maxHeight
                );
        }
        return  image != null ?
                image : super.getPreviewImage( imageInfo, maxWidth, maxHeight );
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
     * Construct a <code>ORFImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private ORFImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for ORF files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String EXTENSIONS[] = {
        "orf"
    };
}
/* vim:set et sw=4 ts=4: */
