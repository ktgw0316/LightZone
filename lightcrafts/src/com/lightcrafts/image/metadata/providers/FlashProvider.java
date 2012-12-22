/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * A <code>FlashProvider</code> provides the state of the flash at the time an
 * image was captured.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface FlashProvider extends ImageMetadataProvider {

    /**
     * Gets the state of the flash at the time the image was captured.
     *
     * @return Returns the flash state or <code>null</code> if it's
     * unavailable.
     */
    String getFlash();

}
/* vim:set et sw=4 ts=4: */
