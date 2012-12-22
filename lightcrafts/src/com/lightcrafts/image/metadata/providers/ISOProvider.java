/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * An <code>ISOProvider</code> provides the ISO value of an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ISOProvider extends ImageMetadataProvider {

    /**
     * Gets the ISO value of an image.
     *
     * @return Returns the ISO value or 0 if it's unavailable.
     */
    int getISO();

}
/* vim:set et sw=4 ts=4: */
