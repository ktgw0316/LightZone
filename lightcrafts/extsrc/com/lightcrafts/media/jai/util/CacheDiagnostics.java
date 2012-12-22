/*
 * $RCSfile: CacheDiagnostics.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:59 $
 * $State: Exp $
 */
/**
 * Diagnostics Interface for SunTileCache. These routines apply to the
 * tile cache, not the cached tile.  All methods are implicitly public.
 *
 * @since JAI 1.1
 */

package com.lightcrafts.media.jai.util;


public interface CacheDiagnostics {

    /** Enable diagnostic monitoring of the tile cache. */
    void enableDiagnostics();

    /** Disable diagnostic monitoring of the tile cache. */
    void disableDiagnostics();

    /** Returns the total number of tiles in a particular cache. */
    long getCacheTileCount();

    /** Returns the total memory used in a particular cache. */
    long getCacheMemoryUsed();

    /**
     *  Returns the number of times this tile was requested when
     *  it was in the tile cache.
     */
    long getCacheHitCount();

    /**
     *  Returns the number of times this tile was requested when
     *  it was not in the tile cache.
     */
    long getCacheMissCount();

    /** Resets the hit and miss counts to zero. */
    void resetCounts();   // resets hit,miss counts
}
