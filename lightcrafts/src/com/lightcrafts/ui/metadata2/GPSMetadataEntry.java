/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.GPSDirectory;
import com.lightcrafts.image.metadata.ImageMetadata;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * @author Masahiro Kitagawa [arctica0316@gmail.com]
 */
public class GPSMetadataEntry extends MetadataEntry {

    @Override
    public String getLabel(ImageMetadata meta) {
        return "GPS"; // TODO: LOCALE.get("GPSLabel");
    }

    @Override
    public String getValue(ImageMetadata meta) {
        final GPSDirectory dir =
                (GPSDirectory) meta.getDirectoryFor(GPSDirectory.class);
        if (dir == null) {
            return "";
        }
        final String lat = dir.getGPSLatitudeDMS();
        if (lat.isEmpty()) {
            return "";
        }
        final String lon = dir.getGPSLongitudeDMS();
        if (lon.isEmpty()) {
            return "";
        }
        return lat + ", " + lon;
    }

    @Override
    boolean isEditable(ImageInfo info) {
        return false;
    }

    @Override
    boolean isValidValue(ImageMetadata meta, String value) {
        return true;
    }

    @Override
    void setValue(ImageMetadata meta, String value) {
        // readonly
    }

    @Override
    URI getURI(ImageMetadata meta) {
        final GPSDirectory dir =
                (GPSDirectory) meta.getDirectoryFor(GPSDirectory.class);
        if (dir == null) {
            return null;
        }
        Double latitude  = dir.getGPSLatitude();
        Double longitude = dir.getGPSLongitude();
        if (latitude == null || longitude == null) {
            return null;
        }

        // c.f. https://developers.google.com/maps/documentation/urls/guide
        try {
            return new URI("https://www.google.com/maps/search/?api=1&query="
                    + latitude + "," + longitude);
        } catch (URISyntaxException ignored) {
            return null;
        }
    }
}
