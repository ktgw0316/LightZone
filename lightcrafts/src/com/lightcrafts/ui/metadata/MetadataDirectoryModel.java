/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2019-     Masahiro Kitagawa */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Represent an ImageMetadataDirectory as a String name plus a List of
 * localized key-value pairs suitable for rendering by toString().
 */
class MetadataDirectoryModel {

    @Getter
    private String name;

    private List<KeyValuePair> pairs;

    private static Comparator<KeyValuePair> KeyValuePairComp =
            Comparator.comparing(p -> p.key);

    private static Comparator<KeyValuePair> NoSortComp = (p1, p2) -> 0;

    MetadataDirectoryModel(
        ImageMetadataDirectory directory, boolean filter, boolean sort
    ) {
        name = directory.getName();
        pairs = StreamSupport.stream(directory.spliterator(), false)
                .map(Map.Entry::getKey)
                .map(id -> new KeyValuePair(id, null, directory.getValue(id)))
                .filter(p -> p.value != null && p.value.isDisplayable())
                .map(p -> {
                    String key = directory.getTagLabelFor(p.tagID);
                    return new KeyValuePair(p.tagID, key, p.value);
                })
                .filter(p -> !filter || directory.shouldDisplayTag(p.tagID))
                .sorted(sort ? KeyValuePairComp : NoSortComp)
                .collect(Collectors.toList());
    }

    List<KeyValuePair> getPairs() {
        return new ArrayList<>(pairs);
    }
}
