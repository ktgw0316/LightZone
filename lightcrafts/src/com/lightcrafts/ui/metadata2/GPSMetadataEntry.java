/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.GPSDirectory;
import com.lightcrafts.image.metadata.ImageMetadata;

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
        return dir.getGPSPositionDMS();
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
}
