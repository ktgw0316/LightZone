/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import java.awt.image.RenderedImage;

/**
 * An observer for preveiw images, needed for calls to
 * ImageDatum.getPreview() because crude previews can be returned quickly but
 * high quality previews take time.
 */
public interface PreviewObserver {

    void previewChanged(RenderedImage preview);
}
