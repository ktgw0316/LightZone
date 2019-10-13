/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2019-     Masahiro Kitagawa */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.image.metadata.values.ImageMetaValue;
import lombok.RequiredArgsConstructor;

/**
 * A simple structure to bind together a metadata tag ID, its presentation
 * name, and a metadata value.
 */
@RequiredArgsConstructor
class KeyValuePair {
    final Integer tagID;
    final String key;
    final ImageMetaValue value;
}
