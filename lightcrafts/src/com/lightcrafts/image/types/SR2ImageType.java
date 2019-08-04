/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.awt.image.RenderedImage;
import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.*;

/**
 * A <code>SR2ImageType</code> is-a {@link RawImageType} for SR2 (Sony Raw
 * version 2) images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class SR2ImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>SR2ImageType</code>. */
    public static final SR2ImageType INSTANCE = new SR2ImageType();

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
        return "SR2";
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

        final RenderedImage image = TIFFImageType.getJPEGInterchangeImage(
            imageInfo, maxWidth, maxHeight
        );
        return  image != null ?
                image : super.getPreviewImage( imageInfo, maxWidth, maxHeight );
    }

    public boolean hasFastPreview() {
        return true;
    }

    /**
     * Reads all the metadata for a given SR2 image file.
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
     * Construct a <code>SR2ImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private SR2ImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for SR2 files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "sr2", "srf"
    };
}
/* vim:set et sw=4 ts=4: */
