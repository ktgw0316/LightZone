/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.metadata.IPTCTags;
import com.lightcrafts.image.metadata.ImageMetadata;

class CreatorMetadataEntry extends IPTCMetadataEntry {

    CreatorMetadataEntry() {
        super(IPTCTags.IPTC_CREATOR);
    }

    public String getValue(ImageMetadata meta) {
        String value = meta.getArtist();
        return (value != null) ? value : "";
    }

    boolean hasDefaultValue() {
        String creator = CopyrightDefaults.getDefaultCreator();
        return creator != null;
    }

    void setDefaultValue(ImageMetadata meta) {
        if (hasDefaultValue()) {
            String text = CopyrightDefaults.getDefaultCreator();
            setValue(meta, text);
        }
    }
}
