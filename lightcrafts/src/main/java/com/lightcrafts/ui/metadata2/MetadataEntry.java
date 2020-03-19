/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.ImageInfo;

import java.net.URI;

abstract class MetadataEntry {

    /**
     * Get the label for this entry, using an ImageMetadataDirectory
     * accessed from the given ImageMetadata instance to look it up.
     */
    abstract String getLabel(ImageMetadata meta);

    /**
     * Get the metadata value to display at this entry for the given
     * ImageMetadata object.
     */
    abstract Object getValue(ImageMetadata meta);

    /**
     * Say whether this entry should get an editable text field for values
     * taken from the given image.
     */
    abstract boolean isEditable(ImageInfo info);

    /**
     * Test whether a given user-entered value for this metadata entry is a
     * valid value.
     */
    abstract boolean isValidValue(ImageMetadata meta, String value);

    /**
     * Assuming isValidValue() returned true, set the value for this field.
     * If the given value is not valid, then do nothing.
     */
    abstract void setValue(ImageMetadata meta, String value);

    /**
     * Some entries have a default value that may be written using a button.
     */
    boolean hasDefaultValue() {
        return false;
    }

    /**
     * Assuming hasDefaultValue() returned true, set the value of this entry
     * to its default value.
     */
    void setDefaultValue(ImageMetadata meta) {
    }

    URI getURI(ImageMetadata meta) {
        return null;
    }
}
