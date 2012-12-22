/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.metadata.*;

class CopyrightMetadataEntry extends IPTCMetadataEntry {

    CopyrightMetadataEntry() {
        super(IPTCTags.IPTC_COPYRIGHT_NOTICE);
    }

    public String getValue(ImageMetadata meta) {
        String value = meta.getCopyright();
        return (value != null) ? value : "";
    }
    boolean hasDefaultValue() {
        String copyright = CopyrightDefaults.getDefaultCopyright();
        return copyright != null;
    }

    void setDefaultValue(ImageMetadata meta) {
        if (hasDefaultValue()) {
            String text = CopyrightDefaults.getDefaultCopyright();
            setValue(meta, text);
        }
    }
}
