/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;

import org.w3c.dom.Document;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;

/**
 * An <code>LZNDocumentProvider</code> is used to get a LightZone document from
 * a file.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface LZNDocumentProvider {

    /**
     * Gets the LightZone document (if any) from the given image file.
     *
     * @param imageInfo The image to get the LightZone document from.
     * @return Returns said {@link Document} or <code>null</code> if none.
     */
    Document getLZNDocument( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException;

}
/* vim:set et sw=4 ts=4: */
