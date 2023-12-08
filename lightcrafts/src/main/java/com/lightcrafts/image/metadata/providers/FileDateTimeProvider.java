/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2023-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata.providers;

import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;

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
    @Nullable
    ZonedDateTime getFileDateTime();

}
/* vim:set et sw=4 ts=4: */
