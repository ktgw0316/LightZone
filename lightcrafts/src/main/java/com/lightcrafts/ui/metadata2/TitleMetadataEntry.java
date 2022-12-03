/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.metadata.IPTCTags;
import com.lightcrafts.image.metadata.ImageMetadata;

class TitleMetadataEntry extends IPTCMetadataEntry {

    TitleMetadataEntry() {
        super(IPTCTags.IPTC_TITLE);
    }

    public String getValue(ImageMetadata meta) {
        String value = meta.getTitle();
        return (value != null) ? value : "";
    }

}
