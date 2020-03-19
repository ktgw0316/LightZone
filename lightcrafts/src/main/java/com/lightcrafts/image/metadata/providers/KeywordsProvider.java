/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

import java.util.Collection;

/**
 * A {@code Keywords} provides the keywords(s) of an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface KeywordsProvider extends ImageMetadataProvider {

    /**
     * Gets the lens used to capture the image.
     *
     * @return Returns the keywords or {@code null} if there are none.
     */
    Collection<String> getKeywords();

}
/* vim:set et sw=4 ts=4: */
