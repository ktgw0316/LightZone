/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.image.metadata.providers;

/**
 * A <code>GPSProvider</code> provides the latitude and longitude of an
 * image.
 *
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
public interface GPSProvider extends ImageMetadataProvider {
    /**
     * Gets the latitude of the image.
     *
     * @return Returns said latitude or null if it's unavailable.
     */
    Double getGPSLatitude();

    /**
     * Gets the longitude of the image.
     *
     * @return Returns said longitude or null if it's unavailable.
     */
    Double getGPSLongitude();

    /**
     * Gets the latitude and longitude of the image as DMS (degrees,
     * minutes, and seconds) string, e.g. 41°24'12.2"N 2°10'26.5"E
     *
     * @return Returns said string or null if it's unavailable.
     */
    String getGPSPositionDMS();
}
