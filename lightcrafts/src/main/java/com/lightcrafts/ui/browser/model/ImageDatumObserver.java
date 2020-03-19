/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

/**
 * An observer for ImageDatum, needed for calls to ImageDatum.getImage()
 * because image data are updated asynchronously.
 */
public interface ImageDatumObserver {

    void imageChanged(ImageDatum datum);
}
