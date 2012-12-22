/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * A <code>CaptionProvider</code> provides the caption of an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface CaptionProvider extends ImageMetadataProvider {

    /**
     * Gets the caption of the image.
     *
     * @return Returns the caption or <code>null</code> if it's unavailable.
     */
    String getCaption();

}
/* vim:set et sw=4 ts=4: */
