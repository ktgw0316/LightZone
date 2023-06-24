/* Copyright (C) 2023- Masahiro Kitagawa */

package com.lightcrafts.image.types;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.*;

import java.io.IOException;

/**
 * A <code>CR3ImageType</code> is-a {@link RawImageType} for CR3 (Canon Raw
 * version 3) images.
 */
public final class CR3ImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>CR3ImageType</code>. */
    public static final CR3ImageType INSTANCE = new CR3ImageType();

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
        return "CR3";
    }

    /**
     * Reads all the metadata for a given CR3 image file.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException
    {
        final TIFFMetadataReader reader = new TIFFMetadataReader( imageInfo );
//        final ImageMetadata metadata = reader.readMetadata();
//        MetadataUtil.removePreviewMetadataFrom( metadata );
//        MetadataUtil.removeWidthHeightFrom( metadata );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>CR3ImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private CR3ImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for CR3 files.  All must be lowercase
     * and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "cr3"
    };
}
/* vim:set et sw=4 ts=4: */
