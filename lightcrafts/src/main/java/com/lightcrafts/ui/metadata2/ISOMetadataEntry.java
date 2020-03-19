/* Copyright (C) 2023-     Masahiro Kitagawa */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.metadata.CoreDirectory;
import com.lightcrafts.image.metadata.CoreTags;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.values.ImageMetaValue;

import java.util.Optional;

public class ISOMetadataEntry extends MetadataEntry {
    @Override
    public String getLabel(ImageMetadata meta) {
        return "ISO";
    }

    @Override
    public String getValue(ImageMetadata meta) {
        final var coreDir = (CoreDirectory) meta.getDirectoryFor(CoreDirectory.class);
        if (coreDir == null) {
            return "";
        }
        final int iso = coreDir.getISO();
        return (iso > 0) ? String.valueOf(iso) : "";
    }

    @Override
    public boolean isEditable(ImageInfo info) {
        return false;
    }

    @Override
    public boolean isValidValue(ImageMetadata meta, String value) {
        return true;
    }

    @Override
    public void setValue(ImageMetadata meta, String value) {
        // readonly
    }
}
