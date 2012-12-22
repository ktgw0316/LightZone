/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;

import org.w3c.dom.Document;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.image.metadata.DNGDirectory;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.TIFFMetadataReader;
import com.lightcrafts.image.metadata.XMPMetadataReader;

/**
 * A <code>DNGImageType</code> is-a {@link RawImageType} for DNG (Digital
 * NeGative) images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class DNGImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>DNGImageType</code>. */
    public static final DNGImageType INSTANCE = new DNGImageType();

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
        return "DNG";
    }

    /**
     * {@inheritDoc}
     */
    public Document getXMP( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return TIFFImageType.getXMP( imageInfo, DNGDirectory.class );
    }

    /**
     * Reads all the metadata for a given DNG image.
     *
     * @param imageInfo The image to read the metadata from.
     */
    public void readMetadata( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final TIFFMetadataReader reader =
            new TIFFMetadataReader( imageInfo, DNGDirectory.class );
        final ImageMetadata metadata = reader.readMetadata();
        final Document xmpDoc = getXMP( imageInfo );
        if ( xmpDoc != null ) {
            final ImageMetadata xmpMetadata =
                XMPMetadataReader.readFrom( xmpDoc );
            metadata.mergeFrom( xmpMetadata );
        }
    }

    // through DCRaw, should be fast enough...
    public boolean hasFastPreview() {
        return true;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>DNGImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private DNGImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for DNG files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String EXTENSIONS[] = {
        "dng"
    };
}
/* vim:set et sw=4 ts=4: */
