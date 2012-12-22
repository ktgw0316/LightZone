/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;

import java.util.*;

// Given a ImageMetaDirectory object, this class identifies a presentation
// filter for the directory's metadata and filters KeyValuePairs accordingly.

class MetaTagFilter {

    // Take an ArrayList of KeyValuePairs, filter them, and return the result:
    static ArrayList<KeyValuePair> filter(
        ImageMetadataDirectory dir, ArrayList<KeyValuePair> pairs
    ) {
        ArrayList<KeyValuePair> result = new ArrayList<KeyValuePair>(pairs);
        for (Iterator i=result.iterator(); i.hasNext(); ) {
            KeyValuePair pair = (KeyValuePair) i.next();
            Integer id = pair.getTagID();
            if (! dir.shouldDisplayTag(id)) {
                i.remove();
            }
        }
        return result;
    }
}
