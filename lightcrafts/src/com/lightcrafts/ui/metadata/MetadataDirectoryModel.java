/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2019-     Masahiro Kitagawa */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represent an ImageMetadataDirectory as a String name plus a List of
 * localized key-value pairs suitable for rendering by toString().
 */
class MetadataDirectoryModel {

    @Getter
    private String name;

    private ArrayList<KeyValuePair> pairs;

    private static Comparator<KeyValuePair> KeyValuePairComp =
            (pair1, pair2) -> {
                String s1 = pair1.getKey();
                String s2 = pair2.getKey();
                return s1.compareTo(s2);
            };

    MetadataDirectoryModel(
        ImageMetadataDirectory directory, boolean filter, boolean sort
    ) {
        name = directory.getName();
        pairs = new ArrayList<>();
        directory.forEach(entry -> {
            Integer id = entry.getKey();
            ImageMetaValue value = directory.getValue(id);
            if (value != null && value.isDisplayable()) {
                String key = directory.getTagLabelFor(id);
                KeyValuePair pair = new KeyValuePair(id, key, value);
                pairs.add(pair);
            }
        });
        if (filter) {
            pairs = MetaTagFilter.filter(directory, pairs);
        }
        if (sort) {
            pairs.sort(KeyValuePairComp);
        }
    }

    List<KeyValuePair> getPairs() {
        return new ArrayList<>(pairs);
    }
}
