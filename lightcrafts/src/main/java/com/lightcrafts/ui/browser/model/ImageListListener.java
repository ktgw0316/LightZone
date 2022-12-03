/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

/**
 * A listener to learn about structural changes to an ImageList: when
 * entries appear and disappear, and when the whole list is reordered like
 * after sorting.
 */
public interface ImageListListener {

    void imageAdded(ImageList source, ImageDatum datum, int index);

    void imageRemoved(ImageList source, ImageDatum datum, int index);

    void imagesReordered(ImageList source);
}
