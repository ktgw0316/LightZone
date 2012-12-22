/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view;

import java.awt.image.RenderedImage;
import java.util.LinkedHashSet;
import java.util.Iterator;

/**
 * This class holds a fixed number of recently used references to
 * RenderedImages painted in an AbstractImageBrowser, for the sole purpose of
 * preventing the images from getting GC'd.
 * <p>
 * These RenderedImages are stored in ImageDatums via soft references, and
 * the assumption is that anything that was painted recently is likely to be
 * painted again soon.
 */
class ImagePreservationQueue extends LinkedHashSet<RenderedImage> {

    // The number of recently used images to withold from GC:
    final static int Limit = 100;

    public boolean add(RenderedImage image) {
        boolean added = super.add(image);
        if (added) {
            Iterator<RenderedImage> i = iterator();
            while (size() > Limit) {
                i.next();
                i.remove();
            }
        }
        return added;
    }
}
