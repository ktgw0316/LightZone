/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * A <code>CopyrightProvider</code> provides the copyright of an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface CopyrightProvider extends ImageMetadataProvider {

    /**
     * Gets the copyright of the image.
     *
     * @return Returns the copyright or <code>null</code> if it's unavailable.
     */
    String getCopyright();

}
/* vim:set et sw=4 ts=4: */
