/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.utils;

/**
 * A listener to discover when JAI has invoked LCTileCache.flush().  This
 * method is only called from within JAI to handle an OutOfMemoryError, and
 * so provides an indirect indication that the image processing pipeline has
 * run out of Java heap space.
 */
public interface LCTileCacheListener {

    void tileCacheFlushed();
}
