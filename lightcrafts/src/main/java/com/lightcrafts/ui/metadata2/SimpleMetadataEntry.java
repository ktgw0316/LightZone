/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.ImageInfo;

class SimpleMetadataEntry extends MetadataEntry {

    Class<? extends ImageMetadataDirectory> clazz;
    int tagID;

    SimpleMetadataEntry(
        Class<? extends ImageMetadataDirectory> clazz, int tagID
    ) {
        this.clazz = clazz;
        this.tagID = tagID;
    }

    Class<? extends ImageMetadataDirectory> getMetadataClass() {
        return clazz;
    }

    ImageMetadataDirectory getDirectory(ImageMetadata meta) {
        return meta.getDirectoryFor(clazz);
    }

    int getTagID() {
        return tagID;
    }

    public String getLabel(ImageMetadata meta) {
        ImageMetadataDirectory dir = meta.getDirectoryFor(clazz);
        if (dir != null) {
            String label = dir.getTagLabelFor(tagID);
            return label;
        }
        try {
            return clazz.newInstance().getTagLabelFor(tagID);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
        return "(undefined)";
    }

    public Object getValue(ImageMetadata meta) {
        ImageMetadataDirectory dir = meta.getDirectoryFor(clazz);
        if (dir != null) {
            ImageMetaValue value = dir.getValue(tagID);
            if (value != null) {
                String text = value.toString();
                return text;
            }
        }
        return "";
    }

    public boolean isEditable(ImageInfo info) {
        return false;
    }

    public boolean isValidValue(ImageMetadata meta, String value) {
        // readonly
        return true;
    }

    public void setValue(ImageMetadata meta, String value) {
        // readonly
    }
}
