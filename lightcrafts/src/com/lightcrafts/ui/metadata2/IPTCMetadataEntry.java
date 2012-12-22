/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.metadata.*;
import com.lightcrafts.image.ImageInfo;

class IPTCMetadataEntry extends SimpleMetadataEntry {

    IPTCMetadataEntry(int tagID) {
        super(IPTCDirectory.class, tagID);
    }

    public boolean isEditable(ImageInfo info) {
        return info.canWriteMetadata();
    }

    public boolean isValidValue(ImageMetadata meta, String value) {
        ImageMetadataDirectory dir = meta.getDirectoryFor(clazz, true);
        return dir.isLegalValue(tagID, value);
    }

    public void setValue(ImageMetadata meta, String value) {
        ImageMetadataDirectory dir = meta.getDirectoryFor(clazz, true);
        dir.setValue(tagID, value);
    }
}
