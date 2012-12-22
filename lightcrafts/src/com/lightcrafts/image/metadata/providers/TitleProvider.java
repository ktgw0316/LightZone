/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * A <code>TitleProvider</code> provides the title of an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface TitleProvider extends ImageMetadataProvider {

    /**
     * Gets the title of the image.
     *
     * @return Returns the title or <code>null</code> if it's unavailable.
     */
    String getTitle();

}
/* vim:set et sw=4 ts=4: */
