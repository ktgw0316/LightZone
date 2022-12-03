/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * An <code>ApertureProvider</code> provides the aperture of the lens used to
 * to capture an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ApertureProvider extends ImageMetadataProvider {

    /**
     * Gets the aperture of the lens that was used to capture the image.
     *
     * @return Returns said aperture or 0 if it's unavailable.
     */
    float getAperture();

}
/* vim:set et sw=4 ts=4: */
