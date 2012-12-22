/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * A <code>BitsPerChannelProvider</code> provides the color bits per channel of
 * an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface BitsPerChannelProvider extends ImageMetadataProvider {

    /**
     * Gets the bits per channel of the image.
     *
     * @return Returns said bits per channel or 0 if it's unavailable.
     */
    int getBitsPerChannel();

}
/* vim:set et sw=4 ts=4: */
