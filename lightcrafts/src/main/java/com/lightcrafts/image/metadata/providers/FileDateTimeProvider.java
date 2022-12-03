/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

import java.util.Date;

/**
 * A <code>FileDateTimeProvider</code> provides the last modified date/time of
 * an image file.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface FileDateTimeProvider extends ImageMetadataProvider {

    /**
     * Gets the last modified date/time of an image file.
     *
     * @return Returns the date/time or <code>null</code> if no date/time is
     * available.
     */
    Date getFileDateTime();

}
/* vim:set et sw=4 ts=4: */
