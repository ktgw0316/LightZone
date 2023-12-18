/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2019-     Masahiro Kitagawa */

package com.lightcrafts.ui.metadata;

import com.lightcrafts.image.metadata.values.ImageMetaValue;

/**
 * A simple structure to bind together a metadata tag ID, its presentation
 * name, and a metadata value.
 */
record KeyValuePair(Integer tagID, String key, ImageMetaValue value) {
}
