/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

import java.util.Date;

/**
 * A <code>CaptureDateTimeProvider</code> provides the capture date/time of an
 * image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface CaptureDateTimeProvider extends ImageMetadataProvider {

    /**
     * Gets the date/time that an image was captured.
     *
     * @return Returns the date/time or <code>null</code> if no date/time is
     * available.
     */
    Date getCaptureDateTime();

}
/* vim:set et sw=4 ts=4: */
