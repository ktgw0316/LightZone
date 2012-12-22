/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;

import org.w3c.dom.Document;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;

/**
 * A <code>LZNTIFFImageType</code> is-a {@link TIFFImageType} for TIFF images
 * that contain LightZone data.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class LZNTIFFImageType extends TIFFImageType
    implements LZNDocumentProvider {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets the LightZone document (if any) from the given TIFF image.
     *
     * @param imageInfo The image to get the LightZone document from.
     * @return Returns said {@link Document} or <code>null</code> if none.
     */
    public Document getLZNDocument( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        return TIFFImageType.getLZNDocumentImpl( imageInfo );
    }

}
/* vim:set et sw=4 ts=4: */
