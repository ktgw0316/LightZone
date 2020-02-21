/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.awt.image.RenderedImage;
import java.awt.color.ColorSpace;
import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.CIFFDirectory;
import com.lightcrafts.image.metadata.CIFFMetadataReader;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.jai.JAIContext;

import static com.lightcrafts.image.metadata.CIFFTags.*;
import static com.lightcrafts.image.types.CIFFConstants.*;

/**
 * A <code>CIFFImageType</code> is-a {@link RawImageType} for CIFF (Canon Image
 * File Format, aka Canon raw) images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class CIFFImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>CIFFImageType</code>. */
    public static final CIFFImageType INSTANCE = new CIFFImageType();

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
        return "CIFF";
    }

    public RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return getPreviewImage( imageInfo, 640, 480 );
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final ImageMetadata metadata = imageInfo.getMetadata();
        final ImageMetadataDirectory dir =
            metadata.getDirectoryFor( CIFFDirectory.class );
        if ( dir != null ) {
            final ImageMetaValue colorSpace = dir.getValue( CIFF_COLOR_SPACE );
            ColorSpace cs = JAIContext.sRGBColorSpace;
            if ( colorSpace != null )
                switch ( colorSpace.getIntValue() ) {
                    case CIFF_COLOR_SPACE_ADOBE_RGB:
                        cs = JAIContext.adobeRGBColorSpace;
                        break;
                }
            final RenderedImage image = JPEGImageType.getImageFromBuffer(
                imageInfo.getByteBuffer(),
                dir.getValue( CIFF_PREVIEW_IMAGE_OFFSET ), 0,
                dir.getValue( CIFF_PREVIEW_IMAGE_LENGTH ),
                cs, maxWidth, maxHeight
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
     * Reads all the metadata for a given CIFF image.
     *
     * @param imageInfo The CIFF image to read the metadata from.
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        new CIFFMetadataReader( imageInfo ).readMetadata();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>CIFFImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private CIFFImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for CIFF files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "crw"
    };
}
/* vim:set et sw=4 ts=4: */
