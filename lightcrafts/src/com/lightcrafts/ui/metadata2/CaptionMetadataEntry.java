/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.metadata.IPTCTags;
import com.lightcrafts.image.metadata.ImageMetadata;

class CaptionMetadataEntry extends IPTCMetadataEntry {

    CaptionMetadataEntry() {
        super(IPTCTags.IPTC_CAPTION_ABSTRACT);
    }

    public String getValue(ImageMetadata meta) {
        String value = meta.getCaption();
        return (value != null) ? value : "";
    }

}
