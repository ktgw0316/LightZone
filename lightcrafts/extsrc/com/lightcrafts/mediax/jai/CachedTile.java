/*
 * $RCSfile: CachedTile.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:05 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.image.RenderedImage;
import java.awt.image.Raster;

/**
 * Public interface for cached tiles used to
 * retrieve information about the tile.
 *
 * @since JAI 1.1
 */

public interface CachedTile {

    /** Returns the image operation to which this
     *  cached tile belongs.  In Sun Microsystems
     *  implementation, this is a RenderedImage.
     */
    RenderedImage getOwner();

    /** Returns the cached tile.  In Sun Microsystems
     *  implementation, this object is a Raster.
     */
    Raster getTile();

    /** Returns a cost metric associated with the tile.
     *  This value is used to determine which tiles get
     *  removed from the cache.
     */
    Object getTileCacheMetric();

    /** Returns the time stamp of the cached tile. */
    long getTileTimeStamp();

    /** Returns the memory size of the cached tile */
    long getTileSize();

    /** Returns information about which method
     *  triggered a notification event.  In the
     *  Sun Microsystems implementation, events
     *  include add, remove and update tile
     *  information.
     */
    int getAction();
}
