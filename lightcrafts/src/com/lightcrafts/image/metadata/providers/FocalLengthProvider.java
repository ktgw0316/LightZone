/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * A <code>FocalLengthProvider</code> provides the focal length of the lens
 * used to capture an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface FocalLengthProvider extends ImageMetadataProvider {

    /**
     * Gets the focal length of the lens that was used to capture the image.
     *
     * @return Returns said focal length or 0 if it's unavailable.
     */
    float getFocalLength();

}
/* vim:set et sw=4 ts=4: */
