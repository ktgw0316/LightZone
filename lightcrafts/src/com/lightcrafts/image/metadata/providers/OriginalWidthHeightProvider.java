/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * An <code>OriginalWidthHeightProvider</code> provides the width and height of
 * the original image of an LZN preview image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface OriginalWidthHeightProvider extends ImageMetadataProvider {

    /**
     * Gets the original height of the image.
     *
     * @return Returns said height or 0 if it's unavailable.
     */
    int getOriginalImageHeight();

    /**
     * Gets the original width of the image.
     *
     * @return Returns said width or 0 if it's unavailable.
     */
    int getOriginalImageWidth();
}
/* vim:set et sw=4 ts=4: */
