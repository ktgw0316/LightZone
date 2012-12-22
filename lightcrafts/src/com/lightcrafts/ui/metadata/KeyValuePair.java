/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.image.metadata.values.ImageMetaValue;

/**
 * A simple structure to bind together a metadata tag ID, its presentation
 * name, and a metadata value.
 */
class KeyValuePair {

    private Integer tagID;
    private String key;
    private ImageMetaValue value;

    KeyValuePair(Integer tagID, String key, ImageMetaValue value) {
        this.tagID = tagID;
        this.key = key;
        this.value = value;
    }

    Integer getTagID() {
        return tagID;
    }

    String getKey() {
        return key;
    }

    ImageMetaValue getValue() {
        return value;
    }
}
