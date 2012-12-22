/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * A <code>LensProvider</code> provides the lens used to capture an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface LensProvider extends ImageMetadataProvider {

    /**
     * Gets the lens used to capture the image.
     *
     * @return Returns the lens or <code>null</code> if it's unavailable.
     */
    String getLens();

}
/* vim:set et sw=4 ts=4: */
