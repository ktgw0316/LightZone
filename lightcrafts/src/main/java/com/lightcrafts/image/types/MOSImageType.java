/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.ImageMetadataReader;
import com.lightcrafts.image.metadata.MetadataUtil;
import com.lightcrafts.image.metadata.TIFFMetadataReader;

/**
 * A <code>MOSImageType</code> is-a {@link RawImageType} for MOS (Leaf Valeo
 * raw) images.
 *
 * @author Fabio Riccardi [fabio@lightcrafts.com]
 */
public final class MOSImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>MOSImageType</code>. */
    public static final MOSImageType INSTANCE = new MOSImageType();

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
        return "MOS";
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
     * Construct a <code>MOSImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private MOSImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for MOS files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "mos"
    };
}
/* vim:set et sw=4 ts=4: */
