/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

/**
 * A <code>ColorTemperatureProvider</code> provides the color temperature of an
 * image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ColorTemperatureProvider extends ImageMetadataProvider {

    /**
     * Gets the color temperature of the image.
     *
     * @return Returns said color temperature or 0 if it's unavailable.
     */
    int getColorTemperature();

}
/* vim:set et sw=4 ts=4: */
