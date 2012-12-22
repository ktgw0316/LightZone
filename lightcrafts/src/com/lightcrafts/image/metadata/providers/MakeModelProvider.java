/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * A <code>MakeModelProvider</code> provides the make and model used to capture
 * an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface MakeModelProvider extends ImageMetadataProvider {

    /**
     * Gets the camera make, and possibly model, of the camera used.
     *
     * @param includeModel If <code>true</code>, the model is included.
     * @return Returns the make (and possibly model) converted to uppercase and
     * seperated by a space or <code>null</code> if not available.
     */
    String getCameraMake( boolean includeModel );

}
/* vim:set et sw=4 ts=4: */
