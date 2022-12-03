/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageMetadataReader;
import com.lightcrafts.image.metadata.MetadataUtil;
import com.lightcrafts.image.metadata.TIFFMetadataReader;

/**
 * A <code>FFFImageType</code> is-a {@link RawImageType} for FFF (Hasselblad)
 * images.
 *
 * @author Fabio Riccardi [fabio@lightcrafts.com]
 */
public final class FFFImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>FFFImageType</code>. */
    public static final FFFImageType INSTANCE = new FFFImageType();

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
        return "FFF";
    }

    /**
     * {@inheritDoc}
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        final ImageMetadataReader reader = new TIFFMetadataReader( imageInfo );
        final ImageMetadata metadata = reader.readMetadata();
        MetadataUtil.removePreviewMetadataFrom( metadata );
        MetadataUtil.removeWidthHeightFrom( metadata );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>FFFImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private FFFImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for FFF files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "fff"
    };
}
/* vim:set et sw=4 ts=4: */
