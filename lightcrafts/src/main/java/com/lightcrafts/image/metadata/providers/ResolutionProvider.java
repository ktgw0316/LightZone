/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * A <code>ResolutionProvider</code> provides the resolution of an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ResolutionProvider extends ImageMetadataProvider {

    int RESOLUTION_UNIT_NONE = TIFF_RESOLUTION_UNIT_NONE;
    int RESOLUTION_UNIT_CM   = TIFF_RESOLUTION_UNIT_CM;
    int RESOLUTION_UNIT_INCH = TIFF_RESOLUTION_UNIT_INCH;

    /**
     * Gets the resolution of an image.
     *
     * @return Returns the resolution or 0 if it's unavailable.
     */
    double getResolution();

    /**
     * Gets the resolution unit of an image.
     *
     * @return Returns one of {@link #RESOLUTION_UNIT_NONE},
     * {@link #RESOLUTION_UNIT_CM}, or {@link #RESOLUTION_UNIT_INCH}.
     */
    int getResolutionUnit();

}
/* vim:set et sw=4 ts=4: */
