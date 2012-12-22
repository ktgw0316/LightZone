/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.metadata.CoreDirectory;
import com.lightcrafts.image.metadata.CoreTags;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.ImageInfo;

class RatingMetadataEntry extends SimpleMetadataEntry {

    // A marker type returned by getValue() that is recognized by the
    // RatingMetadataTableCellRenderer.
    class RatingObject {
        ImageMetaValue value;
        RatingObject(ImageMetaValue value) {
            this.value = value;
        }
        public String toString() {
            // A zero value results from clearRating().
            if ((value != null) && (value.getIntValue() > 0)) {
                String text = value.toString();
                return text;
            }
            return "";
        }
    }

    RatingMetadataEntry() {
        super(CoreDirectory.class, CoreTags.CORE_RATING);
    }

    public RatingObject getValue(ImageMetadata meta) {
        ImageMetadataDirectory dir = meta.getDirectoryFor(clazz);
        if (dir != null) {
            ImageMetaValue value = dir.getValue(tagID);
            return new RatingObject(value);
        }
        return new RatingObject(null);
    }

    public boolean isEditable(ImageInfo info) {
        return info.canWriteMetadata();
    }

    public boolean isValidValue(ImageMetadata meta, String value) {
        // Any number of stars is a valid value
        if (value.replaceAll("\u2605", "").length() == 0) {
            return true;
        }
        try {
            int i = Integer.parseInt(value);
            return ((i >= 0) && (i <= 5));
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    public void setValue(ImageMetadata meta, String value) {
        if (value.replaceAll("\u2605", "").length() == 0) {
            // Interpret as a string of stars:
            int i = value.length();
            if (i > 0) {
                meta.setRating(i);
            }
            else {
                meta.clearRating();
            }
            return;
        }
        // Interpret as a decimal formatted number:
        try {
            Integer i = Integer.parseInt(value);
            if ((i > 0) && (i <= 5)) {
                meta.setRating(i);
            }
            else if (i > 5) {
                meta.setRating(5);
            }
            else {
                meta.clearRating();
            }
        }
        catch (NumberFormatException e) {
            meta.clearRating();
        }
    }
}
