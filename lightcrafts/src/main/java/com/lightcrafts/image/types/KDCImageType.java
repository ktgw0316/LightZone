/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.awt.image.RenderedImage;
import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.UnknownImageTypeException;

/**
 * A <code>KDCImageType</code> is-a {@link RawImageType} for KDC (Kodak raw)
 * images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class KDCImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>KDCImageType</code>. */
    public static final KDCImageType INSTANCE = new KDCImageType();

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
        return "KDC";
    }

    /**
     * {@inheritDoc}
     */
    public RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                          int maxHeight )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
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
     * Construct a <code>KDCImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private KDCImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for KDC files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "kdc"
    };
}
/* vim:set et sw=4 ts=4: */
