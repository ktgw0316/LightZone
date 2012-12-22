/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * A <code>ShutterSpeedProvider</code> provides the shutter speed of the lens
 * that was used to capture an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ShutterSpeedProvider extends ImageMetadataProvider {

    /**
     * Gets the shutter speed of the lens that was used to capture the image.
     *
     * @return Returns the shutter speed in seconds or 0 if it's unavailable.
     */
    float getShutterSpeed();

}
/* vim:set et sw=4 ts=4: */
