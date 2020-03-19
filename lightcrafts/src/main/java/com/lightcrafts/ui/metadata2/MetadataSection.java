/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;

import java.util.ArrayList;

class MetadataSection extends ArrayList<MetadataEntry> {

    void addEntry(MetadataEntry entry) {
        add(entry);
    }

    void addEntry(Class<? extends ImageMetadataDirectory> clazz, int tagID) {
        MetadataEntry entry = new SimpleMetadataEntry(clazz, tagID);
        addEntry(entry);
    }
}
