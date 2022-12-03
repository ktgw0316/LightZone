/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * A <code>WidthHeightProvider</code> provides the width and height of an
 * image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface WidthHeightProvider extends ImageMetadataProvider {

    /**
     * Gets the height of the image.
     *
     * @return Returns said height or 0 if it's unavailable.
     */
    int getImageHeight();

    /**
     * Gets the width of the image.
     *
     * @return Returns said width or 0 if it's unavailable.
     */
    int getImageWidth();
}
/* vim:set et sw=4 ts=4: */
