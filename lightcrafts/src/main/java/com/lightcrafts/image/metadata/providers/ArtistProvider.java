/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * An <code>ArtistProvider</code> provides the artist who took the photo.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ArtistProvider extends ImageMetadataProvider {

    /**
     * Gets the artist who took the photo.
     *
     * @return Returns the artist or <code>null</code> if not available.
     */
    String getArtist();

}
/* vim:set et sw=4 ts=4: */
