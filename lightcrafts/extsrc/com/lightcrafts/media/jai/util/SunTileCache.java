/*
 * $RCSfile: SunTileCache.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.2 $
 * $Date: 2005/11/15 01:50:59 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.util;
import java.awt.RenderingHints;
import java.util.Collections;
import java.util.Comparator;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Observable;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.SortedSet;
import java.util.TreeSet;
import java.awt.Point;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import com.lightcrafts.mediax.jai.EnumeratedParameter;
import com.lightcrafts.mediax.jai.TileCache;
import com.lightcrafts.mediax.jai.util.ImagingListener;

/**
 * This is Sun Microsystems' reference implementation of the
 * <code>com.lightcrafts.mediax.jai.TileCache</code> interface.  It provides a
 * central location for images to cache computed tiles, and is used as
 * the default tile cache mechanism when no other tile cache objects
 * are specified.
 *
 * <p> In this implementation, the cache size is limited by the memory
 * capacity, which may be set at construction time or using the
 * <code>setMemoryCapacity(long)</code> method.  The tile capacity
 * is not used.  Different images may have very different tile sizes.
 * Therefore, the memory usage for a specific tile capacity may vary
 * greatly depends on the type of images involved.  In fact, the tile
 * capacity is rather meaningless.
 *
 * @see com.lightcrafts.mediax.jai.TileCache
 *
 */

//
// NOTE: code is inlined for performance reasons
//
public final class SunTileCache extends Observable
                                implements TileCache,
                                           CacheDiagnostics {

    /** The default memory capacity of the cache (16 MB). */
    private static final long DEFAULT_MEMORY_CAPACITY = 16L * 1024L * 1024L;

    /** The default hashtable capacity (heuristic) */
    private static final int DEFAULT_HASHTABLE_CAPACITY = 1009; // prime number

    /** The hashtable load factor */
    private static final float LOAD_FACTOR = 0.5F;

    /**
     * The tile cache.
     * A Hashtable is used to cache the tiles.  The "key" is a 
     * <code>Object</code> determined based on tile owner's UID if any or
     * hashCode if the UID doesn't exist, and tile index.  The
     * "value" is a SunCachedTile.
     */
    private Hashtable cache;

    /**
     * Sorted (Tree) Set used with tile metrics.
     * Adds another level of metrics used to determine
     * which tiles are removed during memoryControl().
     */
    private SortedSet cacheSortedSet;

    /** The memory capacity of the cache. */
    private long memoryCapacity;

    /** The amount of memory currently being used by the cache. */
    private long memoryUsage = 0;

    /** The amount of memory to keep after memory control */
    private float memoryThreshold = 0.75F;

    /** A indicator for tile access time. */
    private long timeStamp = 0;

    /** Custom comparator used to determine tile cost or
     *  priority ordering in the tile cache.
     */
    private Comparator comparator = null;

    /** Pointer to the first (newest) tile of the linked SunCachedTile list. */
    private SunCachedTile first = null;

    /** Pointer to the last (oldest) tile of the linked SunCachedTile list. */
    private SunCachedTile last = null;

    /** Tile count used for diagnostics */
    private long tileCount = 0;

    /** Cache hit count */
    private long hitCount = 0;

    /** Cache miss count */
    private long missCount = 0;

    /** Diagnostics enable/disable */
    private boolean diagnostics = false;

    // diagnostic actions
    // !!! If actions are changed in any way (removal, modification, addition)
    // then the getCachedTileActions() method below should be changed to match.
    private static final int ADD                 = 0;
    private static final int REMOVE              = 1;
    private static final int REMOVE_FROM_FLUSH   = 2;
    private static final int REMOVE_FROM_MEMCON  = 3;
    private static final int UPDATE_FROM_ADD     = 4;
    private static final int UPDATE_FROM_GETTILE = 5;
    private static final int ABOUT_TO_REMOVE     = 6;

    /**
     * Returns an array of <code>EnumeratedParameter</code>s corresponding
     * to the numeric values returned by the <code>getAction()</code>
     * method of the <code>CachedTile</code> implementation used by
     * <code>SunTileCache</code>.  The "name" of each
     * <code>EnumeratedParameter</code> provides a brief string
     * describing the numeric action value.
     */
    public static EnumeratedParameter[] getCachedTileActions() {
        return new EnumeratedParameter[] {
            new EnumeratedParameter("add", ADD),
            new EnumeratedParameter("remove", REMOVE),
            new EnumeratedParameter("remove_by_flush", REMOVE_FROM_FLUSH),
            new EnumeratedParameter("remove_by_memorycontrol",
                                    REMOVE_FROM_MEMCON),
            new EnumeratedParameter("timestamp_update_by_add", UPDATE_FROM_ADD),
            new EnumeratedParameter("timestamp_update_by_gettile",
                                    UPDATE_FROM_GETTILE),
            new EnumeratedParameter("preremove", ABOUT_TO_REMOVE)
        };
    }

    /**
     * No args constructor. Use the DEFAULT_MEMORY_CAPACITY of 16 Megs.
     */
    public SunTileCache() {
        this(DEFAULT_MEMORY_CAPACITY);
    }

    /**
     * Constructor.  The memory capacity should be explicitly specified.
     *
     * @param memoryCapacity  The maximum cache memory size in bytes.
     *
     * @throws IllegalArgumentException  If <code>memoryCapacity</code>
     *         is less than 0.
     */
    public SunTileCache(long memoryCapacity) {
        if (memoryCapacity < 0) {
            throw new IllegalArgumentException(JaiI18N.getString("SunTileCache"));
        }

        this.memoryCapacity = memoryCapacity;

        // try to get a prime number (more efficient?)
        // lower values of LOAD_FACTOR increase speed, decrease space efficiency
        cache = new Hashtable(DEFAULT_HASHTABLE_CAPACITY, LOAD_FACTOR);
    }


    /**
     * Adds a tile to the cache.
     *
     * <p> If the specified tile is already in the cache, it will not be
     * cached again.  If by adding this tile, the cache exceeds the memory
     * capacity, older tiles in the cache are removed to keep the cache
     * memory usage under the specified limit.
     *
     * @param owner            The image the tile blongs to.
     * @param tileX            The tile's X index within the image.
     * @param tileY            The tile's Y index within the image.
     * @param tile             The tile to be cached.
     */
    public void add(RenderedImage owner,
                    int tileX,
                    int tileY,
                    Raster tile) {
        add(owner, tileX, tileY, tile, null);
    }

    /**
     * Adds a tile to the cache with an associated tile compute cost.
     *
     * <p> If the specified tile is already in the cache, it will not be
     * cached again.  If by adding this tile, the cache exceeds the memory
     * capacity, older tiles in the cache are removed to keep the cache
     * memory usage under the specified limit.
     *
     * @param owner            The image the tile blongs to.
     * @param tileX            The tile's X index within the image.
     * @param tileY            The tile's Y index within the image.
     * @param tile             The tile to be cached.
     * @param tileCacheMetric  Metric for prioritizing tiles
     */
    public synchronized void add(RenderedImage owner,
                                 int tileX,
                                 int tileY,
                                 Raster tile,
                                 Object tileCacheMetric) {

        if ( memoryCapacity == 0 ) {
            return;
        }

        // This tile is not in the cache; create a new SunCachedTile.
        // else just update. (code inlined for performance).
        Object key = SunCachedTile.hashKey(owner, tileX, tileY);
        SunCachedTile ct = (SunCachedTile) cache.get(key);

        if ( ct != null ) {
            // tile is cached, inlines update()
            ct.timeStamp = timeStamp++;

            if (ct != first) {
                // Bring this tile to the beginning of the list.
                if (ct == last) {
                    last = ct.previous;
                    last.next = null;
                } else {
                    ct.previous.next = ct.next;
                    ct.next.previous = ct.previous;
                }

                ct.previous = null;
                ct.next = first;

                first.previous = ct;
                first = ct;
            }

            hitCount++;

            if ( diagnostics ) {
                ct.action = UPDATE_FROM_ADD;
                setChanged();
                notifyObservers(ct);
            }
        } else {
            // create a new tile
            ct = new SunCachedTile(owner, tileX, tileY, tile, tileCacheMetric);

            // Don't cache tile if adding it would provoke memoryControl()
            // which would in turn only end up removing the tile.
            if(memoryUsage + ct.memorySize > memoryCapacity &&
               ct.memorySize > (long)(memoryCapacity * memoryThreshold)) {
                return;
            }

            ct.timeStamp = timeStamp++;
            ct.previous = null;
            ct.next = first;

            if (first == null && last == null) {
                first = ct;
                last  = ct;
            } else {
                first.previous = ct;
                first = ct;        // put this tile at the top of the list
            }

            // add to tile cache
            if ( cache.put(ct.key, ct) == null ) {
                memoryUsage += ct.memorySize;
                tileCount++;
                //missCount++;  Not necessary?

                if ( cacheSortedSet != null ) {
                    cacheSortedSet.add(ct);
                }

                if ( diagnostics ) {
                    ct.action = ADD;
                    setChanged();
                    notifyObservers(ct);
                }
            }

            // Bring memory usage down to memoryThreshold % of memory capacity.
            if (memoryUsage > memoryCapacity) {
                memoryControl();
            }
        }
    }

    /**
     * Removes a tile from the cache.
     *
     * <p> If the specified tile is not in the cache, this method
     * does nothing.
     */
    public synchronized void remove(RenderedImage owner,
                                    int tileX,
                                    int tileY) {

        if ( memoryCapacity == 0 ) {
            return;
        }

        Object key = SunCachedTile.hashKey(owner, tileX, tileY);
        SunCachedTile ct = (SunCachedTile) cache.get(key);

        if ( ct != null ) {
            // Notify observers that a tile is about to be removed.
            // It is possible that the tile will be removed from the
            // cache before the observers get notified.  This should
            // be ok, since a hard reference to the tile will be
            // kept for the observers, so the garbage collector won't
            // remove the tile until the observers release it.
            ct.action = ABOUT_TO_REMOVE;
            setChanged();
            notifyObservers(ct);

            ct = (SunCachedTile) cache.remove(key);

            // recalculate memoryUsage only if tile is actually removed
            if ( ct != null ) {
                memoryUsage -= ct.memorySize;
                tileCount--;

                if ( cacheSortedSet != null ) {
                    cacheSortedSet.remove(ct);
                }

                if ( ct == first ) {
                    if ( ct == last ) {
                        first = null;  // only one tile in the list
                        last  = null;
                    } else {
                        first = ct.next;
                        first.previous = null;
                    }
                } else if ( ct == last ) {
                    last = ct.previous;
                    last.next = null;
                } else {
                    ct.previous.next = ct.next;
                    ct.next.previous = ct.previous;
                }

                // Notify observers that a tile has been removed.
                // If the core's hard references go away, the
                // soft references will be garbage collected.
                // Usually, by the time the observers are notified
                // the ct owner and tile are nulled by the GC, so
                // we can't really tell which op was removed
                // This occurs when OpImage's finalize method is
                // invoked.  This code works ok when remove is
                // called directly. (by flush() for example).
                // If the soft references are GC'd, the timeStamp
                // will no longer be contiguous, it will be
                // unique, so this is ok.
                if ( diagnostics ) {
                    ct.action = REMOVE;
                    setChanged();
                    notifyObservers(ct);
                }

                ct.previous = null;
                ct.next = null;
                ct = null;
            }
        }
    }

    /**
     * Retrieves a tile from the cache.
     *
     * <p> If the specified tile is not in the cache, this method
     * returns <code>null</code>.  If the specified tile is in the
     * cache, its last-access time is updated.
     *
     * @param owner  The image the tile blongs to.
     * @param tileX  The tile's X index within the image.
     * @param tileY  The tile's Y index within the image.
     */
    public synchronized Raster getTile(RenderedImage owner,
                                       int tileX,
                                       int tileY) {

        Raster tile = null;

        if ( memoryCapacity == 0 ) {
            return null;
        }

        Object key = SunCachedTile.hashKey(owner, tileX, tileY);
        SunCachedTile ct = (SunCachedTile)cache.get(key);

        if ( ct == null ) {
            missCount++;
        } else {    // found tile in cache
            tile = (Raster) ct.getTile();

            // Update last-access time. (update() inlined for performance)
            ct.timeStamp = timeStamp++;

            if (ct != first) {
                // Bring this tile to the beginning of the list.
                if (ct == last) {
                    last = ct.previous;
                    last.next = null;
                } else {
                    ct.previous.next = ct.next;
                    ct.next.previous = ct.previous;
                }

                ct.previous = null;
                ct.next = first;

                first.previous = ct;
                first = ct;
            }

            hitCount++;

            if ( diagnostics ) {
                ct.action = UPDATE_FROM_GETTILE;
                setChanged();
                notifyObservers(ct);
            }
        }

        return tile;
    }

    /**
     * Retrieves a contiguous array of all tiles in the cache which are
     * owned by the specified image.  May be <code>null</code> if there
     * were no tiles in the cache.  The array contains no null entries.
     *
     * @param owner The <code>RenderedImage</code> to which the tiles belong.
     * @return An array of all tiles owned by the specified image or
     *         <code>null</code> if there are none currently in the cache.
     */
    public synchronized Raster[] getTiles(RenderedImage owner) {
        Raster[] tiles = null;

        if ( memoryCapacity == 0 ) {
            return null;
        }

        int size = Math.min(owner.getNumXTiles() * owner.getNumYTiles(),
                            (int)tileCount);

        if ( size > 0 ) {
            int minTx = owner.getMinTileX();
            int minTy = owner.getMinTileY();
            int maxTx = minTx + owner.getNumXTiles();
            int maxTy = minTy + owner.getNumYTiles();

            // arbitrarily set a temporary vector size
            Vector temp = new Vector(10, 20);

            for (int y = minTy; y < maxTy; y++) {
                for (int x = minTx; x < maxTx; x++) {
                    // inline this method
                    //Raster raster = getTile(owner, x, y);
                    //************************
                    Raster raster = null;
                    Object key = SunCachedTile.hashKey(owner, x, y);
                    SunCachedTile ct = (SunCachedTile)cache.get(key);

                    if ( ct == null ) {
                        raster = null;
                        missCount++;
                    } else {    // found tile in cache
                        raster = (Raster) ct.getTile();

                        // Update last-access time. (update() inlined for performance)
                        ct.timeStamp = timeStamp++;

                        if (ct != first) {
                            // Bring this tile to the beginning of the list.
                            if (ct == last) {
                                last = ct.previous;
                                last.next = null;
                            } else {
                                ct.previous.next = ct.next;
                                ct.next.previous = ct.previous;
                            }

                            ct.previous = null;
                            ct.next = first;

                            first.previous = ct;
                            first = ct;
                        }

                        hitCount++;

                        if ( diagnostics ) {
                            ct.action = UPDATE_FROM_GETTILE;
                            setChanged();
                            notifyObservers(ct);
                        }
                    }
                    //************************

                    if ( raster != null ) {
                        temp.add(raster);
                    }
                }
            }

            int tmpsize = temp.size();
            if ( tmpsize > 0 ) {
                tiles = (Raster[])temp.toArray(new Raster[tmpsize]);
            }
        }

        return tiles;
    }

    /**
     * Removes all the tiles that belong to a <code>RenderedImage</code>
     * from the cache.
     *
     * @param owner  The image whose tiles are to be removed from the cache.
     */
    public void removeTiles(RenderedImage owner) {
        if ( memoryCapacity > 0 ) {
            int minTx = owner.getMinTileX();
            int minTy = owner.getMinTileY();
            int maxTx = minTx + owner.getNumXTiles();
            int maxTy = minTy + owner.getNumYTiles();

            for (int y=minTy; y<maxTy; y++) {
                for (int x=minTx; x<maxTx; x++) {
                    remove(owner, x, y);
                }
            }
        }
    }

    /**
     * Adds an array of tiles to the tile cache.
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileIndices An array of <code>Point</code>s containing the
     *        <code>tileX</code> and <code>tileY</code> indices for each tile.
     * @param tiles The array of tile <code>Raster</code>s containing tile data.
     * @param tileCacheMetric Object which provides an ordering metric
     *        associated with the <code>RenderedImage</code> owner.
     * @since 1.1
     */
    public synchronized void addTiles(RenderedImage owner,
                                      Point[] tileIndices,
                                      Raster[] tiles,
                                      Object tileCacheMetric) {

        if ( memoryCapacity == 0 ) {
            return;
        }

        // this just inlines the add routine (no sync overhead for each call).
        for ( int i = 0; i < tileIndices.length; i++ ) {
            int tileX = tileIndices[i].x;
            int tileY = tileIndices[i].y;
            Raster tile = tiles[i];

            Object key = SunCachedTile.hashKey(owner, tileX, tileY);
            SunCachedTile ct = (SunCachedTile) cache.get(key);

            if ( ct != null ) {
                // tile is cached, inlines update()
                ct.timeStamp = timeStamp++;

                if (ct != first) {
                    // Bring this tile to the beginning of the list.
                    if (ct == last) {
                        last = ct.previous;
                        last.next = null;
                    } else {
                        ct.previous.next = ct.next;
                        ct.next.previous = ct.previous;
                    }

                    ct.previous = null;
                    ct.next = first;

                    first.previous = ct;
                    first = ct;
                }

                hitCount++;

                if ( diagnostics ) {
                    ct.action = UPDATE_FROM_ADD;
                    setChanged();
                    notifyObservers(ct);
                }
            } else {
                // create a new tile
                ct = new SunCachedTile(owner, tileX, tileY, tile, tileCacheMetric);

                // Don't cache tile if adding it would provoke memoryControl()
                // which would in turn only end up removing the tile.
                if(memoryUsage + ct.memorySize > memoryCapacity &&
                   ct.memorySize > (long)(memoryCapacity * memoryThreshold)) {
                    return;
                }

                ct.timeStamp = timeStamp++;
                ct.previous = null;
                ct.next = first;

                if (first == null && last == null) {
                    first = ct;
                    last  = ct;
                } else {
                    first.previous = ct;
                    first = ct;        // put this tile at the top of the list
                }

                // add to tile cache
                if ( cache.put(ct.key, ct) == null ) {
                    memoryUsage += ct.memorySize;
                    tileCount++;
                    //missCount++;  Not necessary?

                    if ( cacheSortedSet != null ) {
                        cacheSortedSet.add(ct);
                    }

                    if ( diagnostics ) {
                        ct.action = ADD;
                        setChanged();
                        notifyObservers(ct);
                    }
                }

                // Bring memory usage down to memoryThreshold % of memory capacity.
                if (memoryUsage > memoryCapacity) {
                    memoryControl();
                }
            }
        }
    }

    /**
     * Returns an array of tile <code>Raster</code>s from the cache.
     * Any or all of the elements of the returned array may be <code>null</code>
     * if the corresponding tile is not in the cache.
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileIndices  An array of <code>Point</code>s containing the
     *        <code>tileX</code> and <code>tileY</code> indices for each tile.
     * @since 1.1
     */
    public synchronized Raster[] getTiles(RenderedImage owner, Point[] tileIndices) {

        if ( memoryCapacity == 0 ) {
            return null;
        }

        Raster[] tiles = new Raster[tileIndices.length];

        for ( int i = 0; i < tiles.length; i++ ) {
            int tileX = tileIndices[i].x;
            int tileY = tileIndices[i].y;

            Object key = SunCachedTile.hashKey(owner, tileX, tileY);
            SunCachedTile ct = (SunCachedTile)cache.get(key);

            if ( ct == null ) {
                tiles[i] = null;
                missCount++;
            } else {    // found tile in cache
                tiles[i] = (Raster) ct.getTile();

                // Update last-access time. (update() inlined for performance)
                ct.timeStamp = timeStamp++;

                if (ct != first) {
                    // Bring this tile to the beginning of the list.
                    if (ct == last) {
                        last = ct.previous;
                        last.next = null;
                    } else {
                        ct.previous.next = ct.next;
                        ct.next.previous = ct.previous;
                    }

                    ct.previous = null;
                    ct.next = first;

                    first.previous = ct;
                    first = ct;
                }

                hitCount++;

                if ( diagnostics ) {
                    ct.action = UPDATE_FROM_GETTILE;
                    setChanged();
                    notifyObservers(ct);
                }
            }
        }

        return tiles;
    }

    /** Removes -ALL- tiles from the cache. */
    public synchronized void flush() {
        //
        // It is necessary to clear all the elements
        // from the old cache in order to remove dangling
        // references, due to the linked list.  In other
        // words, it is possible to reache the object
        // through 2 paths so the object does not
        // become weakly reachable until the reference
        // to it in the hash map is null. It is not enough
        // to just set the object to null.
        //
        Enumeration keys = cache.keys();    // all keys in Hashtable

        // reset counters before diagnostics
        hitCount  = 0;
        missCount = 0;

        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            SunCachedTile ct = (SunCachedTile) cache.remove(key);

            // recalculate memoryUsage only if tile is actually removed
            if ( ct != null ) {
                memoryUsage -= ct.memorySize;
                tileCount--;

                if ( ct == first ) {
                    if ( ct == last ) {
                        first = null;  // only one tile in the list
                        last  = null;
                    } else {
                        first = ct.next;
                        first.previous = null;
                    }
                } else if ( ct == last ) {
                    last = ct.previous;
                    last.next = null;
                } else {
                    ct.previous.next = ct.next;
                    ct.next.previous = ct.previous;
                }

                ct.previous = null;
                ct.next = null;

                // diagnostics
                if ( diagnostics ) {
                    ct.action = REMOVE_FROM_FLUSH;
                    setChanged();
                    notifyObservers(ct);
                }
            }
        }

        if ( memoryCapacity > 0 ) {
            cache = new Hashtable(DEFAULT_HASHTABLE_CAPACITY, LOAD_FACTOR);
        }

        if ( cacheSortedSet != null ) {
            cacheSortedSet.clear();
            cacheSortedSet = Collections.synchronizedSortedSet( new TreeSet(comparator) );
        }

        // force reset after diagnostics
        tileCount   = 0;
        timeStamp   = 0;
        memoryUsage = 0;

        // no System.gc() here, it's too slow and may occur anyway.
    }

    /**
     * Returns the cache's tile capacity.
     *
     * <p> This implementation of <code>TileCache</code> does not use
     * the tile capacity.  This method always returns 0.
     */
    public int getTileCapacity() { return 0; }

    /**
     * Sets the cache's tile capacity to the desired number of tiles.
     *
     * <p> This implementation of <code>TileCache</code> does not use
     * the tile capacity.  The cache size is limited by the memory
     * capacity only.  This method does nothing and has no effect on
     * the cache.
     *
     * @param tileCapacity  The desired tile capacity for this cache
     *        in number of tiles.
     */
    public void setTileCapacity(int tileCapacity) { }

    /** Returns the cache's memory capacity in bytes. */
    public long getMemoryCapacity() {
        return memoryCapacity;
    }

    /**
     * Sets the cache's memory capacity to the desired number of bytes.
     * If the new memory capacity is smaller than the amount of memory
     * currently being used by this cache, tiles are removed from the
     * cache until the memory usage is less than the specified memory
     * capacity.
     *
     * @param memoryCapacity  The desired memory capacity for this cache
     *        in bytes.
     *
     * @throws IllegalArgumentException  If <code>memoryCapacity</code>
     *         is less than 0.
     */
    public void setMemoryCapacity(long memoryCapacity) {
        if (memoryCapacity < 0) {
            throw new IllegalArgumentException(JaiI18N.getString("SunTileCache"));
        } else if ( memoryCapacity == 0 ) {
            flush();
        }

        this.memoryCapacity = memoryCapacity;

        if ( memoryUsage > memoryCapacity ) {
            memoryControl();
        }
    }

    /** Enable Tile Monitoring and Diagnostics */
    public void enableDiagnostics() {
        diagnostics = true;
    }

    /** Turn off diagnostic notification */
    public void disableDiagnostics() {
        diagnostics = false;
    }

    public long getCacheTileCount() {
        return tileCount;
    }

    public long getCacheMemoryUsed() {
        return memoryUsage;
    }

    public long getCacheHitCount() {
        return hitCount;
    }

    public long getCacheMissCount() {
        return missCount;
    }

    /**
     * Reset hit and miss counters.
     *
     * @since 1.1
     */
    public void resetCounts() {
        hitCount  = 0;
        missCount = 0;
    }

    /**
     * Set the memory threshold value.
     *
     * @since 1.1
     */
    public void setMemoryThreshold(float mt) {
        if ( mt < 0.0F || mt > 1.0F ) {
            throw new IllegalArgumentException(JaiI18N.getString("SunTileCache"));
        } else {
            memoryThreshold = mt;
            memoryControl();
        }
    }

    /**
     * Returns the current <code>memoryThreshold</code>.
     *
     * @since 1.1
     */
    public float getMemoryThreshold() {
        return memoryThreshold;
    }

    /** Returns a string representation of the class object. */
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) +
               ": memoryCapacity = " + Long.toHexString(memoryCapacity) +
               " memoryUsage = " + Long.toHexString(memoryUsage) +
               " #tilesInCache = " + Integer.toString(cache.size());
    }

    /** Returns the <code>Object</code> that represents the actual cache. */
    public Object getCachedObject() {
        return cache;
    }

    /**
     * Removes tiles from the cache based on their last-access time
     * (old to new) until the memory usage is memoryThreshold % of that of the
     * memory capacity.
     */
    public synchronized void memoryControl() {
        if ( cacheSortedSet == null ) {
            standard_memory_control();
        } else {
            custom_memory_control();
        }
    }

    // time stamp based memory control (LRU)
    private final void standard_memory_control() {
        long limit = (long)(memoryCapacity * memoryThreshold);

        while( memoryUsage > limit && last != null ) {
            SunCachedTile ct = (SunCachedTile) cache.get(last.key);

            if ( ct != null ) {
                ct = (SunCachedTile) cache.remove(last.key);

                memoryUsage -= last.memorySize;
                tileCount--;

                last = last.previous;

                if (last != null) {
                    last.next.previous = null;
                    last.next = null;
                } else {
                    first = null;
                }

                // diagnostics
                if ( diagnostics ) {
                    ct.action = REMOVE_FROM_MEMCON;
                    setChanged();
                    notifyObservers(ct);
                }
            }
        }
    }

    // comparator based memory control (TreeSet)
    private final void custom_memory_control() {
        long limit = (long)(memoryCapacity * memoryThreshold);
        Iterator iter = cacheSortedSet.iterator();
        SunCachedTile ct;

        while( iter.hasNext() && (memoryUsage > limit) ) {
            ct = (SunCachedTile) iter.next();

            memoryUsage -= ct.memorySize;
            tileCount--;

            // remove from sorted set
            try {
                iter.remove();
            } catch(ConcurrentModificationException e) {
                ImagingListener listener =
                    ImageUtil.getImagingListener((RenderingHints)null);
                listener.errorOccurred(JaiI18N.getString("SunTileCache0"),
                                       e, this, false);
//                e.printStackTrace();
            }

            // remove tile from the linked list
            if ( ct == first ) {
                if ( ct == last ) {
                    first = null;
                    last  = null;
                } else {
                    first = ct.next;

                    if ( first != null ) {
                        first.previous = null;
                        first.next = ct.next.next;
                    }
                }
            } else if ( ct == last ) {
                last = ct.previous;

                if ( last != null ) {
                    last.next = null;
                    last.previous = ct.previous.previous;
                }
            } else {
                SunCachedTile ptr = first.next;

                while( ptr != null ) {

                    if ( ptr == ct ) {
                        if ( ptr.previous != null ) {
                            ptr.previous.next = ptr.next;
                        }

                        if ( ptr.next != null ) {
                            ptr.next.previous = ptr.previous;
                        }

                        break;
                    }

                    ptr = ptr.next;
                }
            }

            // remove reference in the hashtable
            cache.remove(ct.key);

            // diagnostics
            if ( diagnostics ) {
                ct.action = REMOVE_FROM_MEMCON;
                setChanged();
                notifyObservers(ct);
            }
        }

        // If the custom memory control didn't release sufficient
        // number of tiles to satisfy the memory limit, fallback
        // to the standard memory controller.
        if ( memoryUsage > limit ) {
            standard_memory_control();
        }
    }

    /**
     *  The <code>Comparator</code> is used to produce an
     *  ordered list of tiles based on a user defined
     *  compute cost or priority metric.  This determines
     *  which tiles are subject to "ordered" removal
     *  during a memory control operation.
     *
     *  @since 1.1
     */
    public synchronized void setTileComparator(Comparator c) {
        comparator = c;

        if ( comparator == null ) {
            // turn of comparator
            if ( cacheSortedSet != null ) {
                cacheSortedSet.clear();
                cacheSortedSet = null;
            }
        } else {
            // copy tiles from hashtable to sorted tree set
            cacheSortedSet = Collections.synchronizedSortedSet( new TreeSet(comparator) );

            Enumeration keys = cache.keys();

            while( keys.hasMoreElements() ) {
                Object key = keys.nextElement();
                Object ct = cache.get(key);
                cacheSortedSet.add(ct);
            }
        }
    }

    /**
     * Return the current comparator
     *
     * @since 1.1
     */
    public Comparator getTileComparator() {
        return comparator;
    }

    // test
    public void dump() {

        System.out.println("first = " + first);
        System.out.println("last  = " + last);

        Iterator iter = cacheSortedSet.iterator();
        int k = 0;

        while( iter.hasNext() ) {
            SunCachedTile ct = (SunCachedTile) iter.next();
            System.out.println(k++);
            System.out.println(ct);
        }
    }

    void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener =
            ImageUtil.getImagingListener((RenderingHints)null);
        listener.errorOccurred(message, e, this, false);
    }
}
