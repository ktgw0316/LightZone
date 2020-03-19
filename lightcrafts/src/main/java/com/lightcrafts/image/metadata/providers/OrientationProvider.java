/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

import com.lightcrafts.image.metadata.ImageOrientation;

/**
 * An <code>OrientationProvider</code> provides the orientation of an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface OrientationProvider extends ImageMetadataProvider {

    /**
     * Gets the orientation of an image.
     *
     * @return Returns the orientation or
     * {@link ImageOrientation#ORIENTATION_UNKNOWN}.
     */
    ImageOrientation getOrientation();

}
/* vim:set et sw=4 ts=4: */
