/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * An <code>RatingProvider</code> provides the rating of an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface RatingProvider extends ImageMetadataProvider {

    /**
     * Gets the rating of an image: 1-5.
     *
     * @return Returns the rating or 0 if it's unavailable.
     * @see #setRating(int)
     */
    int getRating();

    /**
     * Sets the rating of the image.
     *
     * @param rating The rating; must be in the range 0-5.
     * @see #getRating()
     */
    void setRating(int rating);
}
/* vim:set et sw=4 ts=4: */
