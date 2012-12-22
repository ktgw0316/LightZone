/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.values.ImageMetaValue;

import java.util.*;

/**
 * Represent an ImageMetadataDirectory as a String name plus a List of
 * localized key-value pairs suitable for rendering by toString().
 */
class MetadataDirectoryModel {

    private String name;
    private ArrayList<KeyValuePair> pairs;

    static Comparator<KeyValuePair> KeyValuePairComp =
        new Comparator<KeyValuePair>() {
            public int compare(KeyValuePair pair1, KeyValuePair pair2) {
                String s1 = pair1.getKey();
                String s2 = pair2.getKey();
                return s1.compareTo(s2);
            }
        };

    MetadataDirectoryModel(
        ImageMetadataDirectory directory, boolean filter, boolean sort
    ) {
        name = directory.getName();
        pairs = new ArrayList<KeyValuePair>();
        Iterator<Map.Entry<Integer,ImageMetaValue>> i=directory.iterator();
        while (i.hasNext()) {
            Map.Entry<Integer,ImageMetaValue> entry = i.next();
            Integer id = entry.getKey();
            ImageMetaValue value = directory.getValue(id);
            if ( value.isDisplayable() ) {
                String key = directory.getTagLabelFor(id);
                KeyValuePair pair = new KeyValuePair(id, key, value);
                pairs.add(pair);
            }
        }
        if (filter) {
            pairs = MetaTagFilter.filter(directory, pairs);
        }
        if (sort) {
            Collections.sort(pairs, KeyValuePairComp);
        }
    }

    List<KeyValuePair> getPairs() {
        return new ArrayList<KeyValuePair>(pairs);
    }

    String getName() {
        return name;
    }
}
