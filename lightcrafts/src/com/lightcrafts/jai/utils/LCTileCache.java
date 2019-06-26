/* Copyright (C) 2005-2011 Fabio Riccardi */

/*
 * $RCSfile: SunTileCache.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:02 $
 * $State: Exp $
 */
package com.lightcrafts.jai.utils;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.utils.LCArrays;
import com.lightcrafts.utils.MemoryLimits;
import com.lightcrafts.utils.cache.*;
import com.sun.media.jai.util.CacheDiagnostics;
import com.sun.media.jai.util.ImageUtil;
import lombok.Getter;
import lombok.Setter;

import javax.media.jai.EnumeratedParameter;
import javax.media.jai.TileCache;
import javax.media.jai.util.ImagingListener;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.prefs.Preferences;

/**
 * This is Sun Microsystems' reference implementation of the
 * <code>javax.media.jai.TileCache</code> interface.  It provides a
 * central location for images to cachedObject computed tiles, and is used as
 * the default tile cachedObject mechanism when no other tile cachedObject objects
 * are specified.
 *
 * <p> In this implementation, the cachedObject size is limited by the memory
 * capacity, which may be set at construction time or using the
 * <code>setMemoryCapacity(long)</code> method.  The tile capacity
 * is not used.  Different images may have very different tile sizes.
 * Therefore, the memory usage for a specific tile capacity may vary
 * greatly depends on the type of images involved.  In fact, the tile
 * capacity is rather meaningless.
 *
 * @see javax.media.jai.TileCache
 *
 */

public final class LCTileCache extends Observable
                                implements TileCache,
        CacheDiagnostics {

    /** The default memory capacity of the cachedObject (16 MB). */
    private static final long DEFAULT_MEMORY_CAPACITY = 16L * 1024L * 1024L;

    /** The default hashtable capacity (heuristic) */
    private static final int DEFAULT_HASHTABLE_CAPACITY = 1009; // prime number

    /** The hashtable load factor */
    private static final float LOAD_FACTOR = 0.5F;

    /** Listener for the flush() method, to detect low memory situations. */
    @Setter
    private static LCTileCacheListener Listener;

    /**
     * The tile cachedObject.
     * A Hashtable is used to cachedObject the tiles.  The "key" is a
     * <code>Object</code> determined based on tile owner's UID if any or
     * hashCode if the UID doesn't exist, and tile index.  The
     * "value" is a LCCachedTile.
     */
    @Getter
    private Hashtable<Object, LCCachedTile> cachedObject;

    /**
     * Sorted (Tree) Set used with tile metrics.
     * Adds another level of metrics used to determine
     * which tiles are removed during memoryControl().
     */
    private SortedSet<LCCachedTile> cacheSortedSet;

    private LinkedList<LCCachedTile> tileList;

    /** The memory capacity of the cachedObject. */
    @Getter
    private long memoryCapacity;

    /** The amount of memory currently being used by the cachedObject. */
    @Getter
    private long cacheMemoryUsed = 0;

    /** The amount of memory to keep after memory control */
    private float memoryThreshold = 0.75F;

    /** A indicator for tile access time. */
    private long timeStamp = 0;

    /** Custom tileComparator used to determine tile cost or
     *  priority ordering in the tile cachedObject.
     */
    @Getter
    private Comparator tileComparator = null;

    /** Tile count used for diagnostics */
    @Getter
    private long cacheTileCount = 0;

    /** Cache hit count */
    @Getter
    private long cacheHitCount = 0;

    /** Cache miss count */
    @Getter
    private long cacheMissCount = 0;

    /** Diagnostics enable/disable */
    private boolean diagnostics;

    private Cache m_objectCache;

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
    private static final int REMOVE_FROM_GCEVENT = 7;

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
            new EnumeratedParameter("remove_by_gcevent",
                                    REMOVE_FROM_GCEVENT),
            new EnumeratedParameter("timestamp_update_by_add", UPDATE_FROM_ADD),
            new EnumeratedParameter("timestamp_update_by_gettile",
                                    UPDATE_FROM_GETTILE),
            new EnumeratedParameter("preremove", ABOUT_TO_REMOVE)
        };
    }

    /**
     * No args constructor. Use the DEFAULT_MEMORY_CAPACITY of 16 Megs.
     */
    public LCTileCache(boolean useDisk) {
        this(DEFAULT_MEMORY_CAPACITY, useDisk);
    }

    /**
     * Constructor.  The memory capacity should be explicitly specified.
     *
     * @param memoryCapacity  The maximum cachedObject memory size in bytes.
     *
     * @throws IllegalArgumentException  If <code>memoryCapacity</code>
     *         is less than 0.
     */
    public LCTileCache(long memoryCapacity, boolean useDisk) {
        if (memoryCapacity < 0) {
            throw new IllegalArgumentException("memory capacity must be >= 0");
        }

        this.memoryCapacity = memoryCapacity;

        // try to get a prime number (more efficient?)
        // lower values of LOAD_FACTOR increase speed, decrease space efficiency
        cachedObject = new Hashtable<>(DEFAULT_HASHTABLE_CAPACITY, LOAD_FACTOR);

        tileList = new LinkedList<>();

        if (useDisk) {
            m_objectCache = createDiskCache();

            m_tileReaper = new TileReaper( this );
            m_tileReaper.start();
        }
    }


    /**
     * Adds a tile to the cachedObject.
     *
     * <p> If the specified tile is already in the cachedObject, it will not be
     * cached again.  If by adding this tile, the cachedObject exceeds the memory
     * capacity, older tiles in the cachedObject are removed to keep the cachedObject
     * memory usage under the specified limit.
     *
     * @param owner            The image the tile blongs to.
     * @param tileX            The tile's X index within the image.
     * @param tileY            The tile's Y index within the image.
     * @param tile             The tile to be cached.
     */
    @Override
    public void add(RenderedImage owner,
                    int tileX,
                    int tileY,
                    Raster tile) {
        add(owner, tileX, tileY, tile, null);
    }

    /**
     * Adds a tile to the cachedObject with an associated tile compute cost.
     *
     * <p> If the specified tile is already in the cachedObject, it will not be
     * cached again.  If by adding this tile, the cachedObject exceeds the memory
     * capacity, older tiles in the cachedObject are removed to keep the cachedObject
     * memory usage under the specified limit.
     *
     * @param owner            The image the tile blongs to.
     * @param tileX            The tile's X index within the image.
     * @param tileY            The tile's Y index within the image.
     * @param tile             The tile to be cached.
     * @param tileCacheMetric  Metric for prioritizing tiles
     */
    @Override
    public synchronized void add(RenderedImage owner,
                                 int tileX,
                                 int tileY,
                                 Raster tile,
                                 Object tileCacheMetric) {

        if ( memoryCapacity == 0 ) {
            return;
        }

        // This tile is not in the cachedObject; create a new LCCachedTile.
        // else just update.
        Object key = LCCachedTile.hashKey(owner, tileX, tileY);
        LCCachedTile ct = cachedObject.get(key);

        if ( ct != null ) {
            updateTileList(ct, UPDATE_FROM_ADD);
        } else {
            // create a new tile
            ct = new LCCachedTile(owner, tileX, tileY, tile, tileCacheMetric);
            updateTileList(ct, ADD);

            // add to tile cachedObject
            if ( cachedObject.put(ct.key, ct) == null ) {
                cacheMemoryUsed += ct.tileSize;
                cacheTileCount++;
                //cacheMissCount++;  Not necessary?

                if ( cacheSortedSet != null ) {
                    cacheSortedSet.add(ct);
                }
            }

            // Bring memory usage down to memoryThreshold % of memory capacity.
            if (cacheMemoryUsed > memoryCapacity) {
                memoryControl();
            }
        }

        if (m_tileReaper != null) {
            // TODO: do we need this?
            synchronized ( this ) {
                Reference<RenderedImage> weakKey = null;

                Set<Reference<RenderedImage>> keySet = m_imageMap.keySet();
                for (Reference<RenderedImage> ref : keySet) {
                    if (ref.get() == owner) {
                        weakKey = ref;
                        break;
                    }
                }

                // weakKey = (WeakReference) m_weakRefMap.get(owner);

                if (weakKey == null) {
                    weakKey = new WeakReference<>( owner, m_tileReaper.getRefQ() );
                    // m_weakRefMap.put(owner, weakKey);
                }

                Set<Object> hashKeys = m_imageMap.computeIfAbsent(weakKey, k -> new HashSet<>());

                hashKeys.add(key);
            }
        }
    }

    private boolean removeFromTileList(Object key, int action) {
        LCCachedTile ct = cachedObject.remove(key);

        if (ct != null) {
            cacheMemoryUsed -= ct.tileSize;
            cacheTileCount--;

            if ( cacheSortedSet != null ) {
                cacheSortedSet.remove(ct);
            }

            tileList.remove(ct);

            // Notify observers that a tile has been removed.
            if ( diagnostics ) {
                ct.action = action;
                setChanged();
                notifyObservers(ct);
            }

            return true;
        }
        return false;
    }

    private void updateTileList(LCCachedTile ct, int action) {
        ct.tileTimeStamp = timeStamp++;

        if (tileList.isEmpty()) {
            tileList.add(ct);
        }
        else if (!tileList.getFirst().equals(ct)) {
            // Bring this tile to the beginning of the list.
            tileList.remove(ct);
            tileList.addFirst(ct);
        }

        cacheHitCount++;

        if ( diagnostics ) {
            ct.action = action;
            setChanged();
            notifyObservers(ct);
        }
    }

    /**
     * Removes a tile from the cachedObject.
     *
     * <p> If the specified tile is not in the cachedObject, this method
     * does nothing.
     */
    @Override
    public synchronized void remove(RenderedImage owner,
                                    int tileX,
                                    int tileY) {

        if ( memoryCapacity == 0 ) {
            return;
        }

        Object key = LCCachedTile.hashKey(owner, tileX, tileY);
        LCCachedTile ct = cachedObject.get(key);

        if ( ct != null ) {
            // Notify observers that a tile is about to be removed.
            // It is possible that the tile will be removed from the
            // cachedObject before the observers get notified.  This should
            // be ok, since a hard reference to the tile will be
            // kept for the observers, so the garbage collector won't
            // remove the tile until the observers release it.
            ct.action = ABOUT_TO_REMOVE;
            setChanged();
            notifyObservers(ct);

            removeFromTileList(key, REMOVE);
        } else {
            // if the tile is not in the memory cachedObject than it might be on disk...
            if (m_objectCache != null && m_objectCache.contains(key)) {
                m_objectCache.remove(key);
                tilesOnDisk--;
            }
        }
    }

    /**
     * Retrieves a tile from the cachedObject.
     *
     * <p> If the specified tile is not in the cachedObject, this method
     * returns <code>null</code>.  If the specified tile is in the
     * cachedObject, its last-access time is updated.
     *
     * @param owner  The image the tile blongs to.
     * @param tileX  The tile's X index within the image.
     * @param tileY  The tile's Y index within the image.
     */
    @Override
    public synchronized Raster getTile(RenderedImage owner,
                                       int tileX,
                                       int tileY) {
        if ( memoryCapacity == 0 )
            return null;

        Object key = LCCachedTile.hashKey(owner, tileX, tileY);
        LCCachedTile ct = cachedObject.get(key);

        if (m_objectCache != null && ct == null) {
            Raster raster = readTileFromDisk(owner, tileX, tileY, key);
            if (raster != null) {
                add(owner, tileX, tileY, raster, null);
                ct = cachedObject.get(key);
                assert ct != null;
            }
        }

        Raster tile = null;

        if ( ct == null ) {
            cacheMissCount++;
        } else {    // found tile in cachedObject
            tile = ct.getTile();

            updateTileList(ct, UPDATE_FROM_GETTILE);
        }

        return tile;
    }

    /**
     * Retrieves a contiguous array of all tiles in the cachedObject which are
     * owned by the specified image.  May be <code>null</code> if there
     * were no tiles in the cachedObject.  The array contains no null entries.
     *
     * @param owner The <code>RenderedImage</code> to which the tiles belong.
     * @return An array of all tiles owned by the specified image or
     *         <code>null</code> if there are none currently in the cachedObject.
     */
    @Override
    public synchronized Raster[] getTiles(RenderedImage owner) {
        Raster[] tiles = null;

        if ( memoryCapacity == 0 ) {
            return null;
        }

        int size = Math.min(owner.getNumXTiles() * owner.getNumYTiles(),
                            (int) cacheTileCount);

        if ( size > 0 ) {
            int minTx = owner.getMinTileX();
            int minTy = owner.getMinTileY();
            int maxTx = minTx + owner.getNumXTiles();
            int maxTy = minTy + owner.getNumYTiles();

            // arbitrarily set a temporary vector size
            Vector<Raster> temp = new Vector<>(10, 20);

            for (int y = minTy; y < maxTy; y++) {
                for (int x = minTx; x < maxTx; x++) {
                    Raster raster = getTile(owner, x, y);

                    if ( raster != null ) {
                        temp.add(raster);
                    }
                }
            }

            int tmpsize = temp.size();
            if ( tmpsize > 0 ) {
                tiles = temp.toArray(new Raster[tmpsize]);
            }
        }

        return tiles;
    }

    /**
     * Removes all the tiles that belong to a <code>RenderedImage</code>
     * from the cachedObject.
     *
     * @param owner  The image whose tiles are to be removed from the cachedObject.
     */
    @Override
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
     * Adds an array of tiles to the tile cachedObject.
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileIndices An array of <code>Point</code>s containing the
     *        <code>tileX</code> and <code>tileY</code> indices for each tile.
     * @param tiles The array of tile <code>Raster</code>s containing tile data.
     * @param tileCacheMetric Object which provides an ordering metric
     *        associated with the <code>RenderedImage</code> owner.
     * @since 1.1
     */
    @Override
    public synchronized void addTiles(RenderedImage owner,
                                      Point[] tileIndices,
                                      Raster[] tiles,
                                      Object tileCacheMetric) {

        if ( memoryCapacity == 0 ) {
            return;
        }

        for ( int i = 0; i < tileIndices.length; i++ ) {
            int tileX = tileIndices[i].x;
            int tileY = tileIndices[i].y;
            Raster tile = tiles[i];

            add(owner, tileX, tileY, tile, tileCacheMetric);
        }
    }

    /**
     * Returns an array of tile <code>Raster</code>s from the cachedObject.
     * Any or all of the elements of the returned array may be <code>null</code>
     * if the corresponding tile is not in the cachedObject.
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileIndices  An array of <code>Point</code>s containing the
     *        <code>tileX</code> and <code>tileY</code> indices for each tile.
     * @since 1.1
     */
    @Override
    public synchronized Raster[] getTiles(RenderedImage owner, Point[] tileIndices) {

        if ( memoryCapacity == 0 ) {
            return null;
        }

        Raster[] tiles = new Raster[tileIndices.length];

        for ( int i = 0; i < tiles.length; i++ ) {
            int tileX = tileIndices[i].x;
            int tileY = tileIndices[i].y;

            tiles[i] = getTile(owner, tileX, tileY);
        }

        return tiles;
    }

    /** Removes -ALL- tiles from the cachedObject. */
    @Override
    public synchronized void flush() {
        // Call the LCTileCacheListener, if one is defined.  This helps detect
        // low memory conditions.
        if (Listener != null) {
            Listener.tileCacheFlushed();
        }
        // NOTE: we don't do flushing for disk caches, it wipes the persistent cachedObject, rather spill half of the cachedObject out
        if (m_objectCache != null) {
            System.err.println("flushing the cachedObject");
            float mt = memoryThreshold;
            memoryThreshold = 0.1f;
            memoryControl();
            memoryThreshold = mt;
            return;
        }

        //
        // It is necessary to clear all the elements
        // from the old cachedObject in order to remove dangling
        // references, due to the linked list.  In other
        // words, it is possible to reache the object
        // through 2 paths so the object does not
        // become weakly reachable until the reference
        // to it in the hash map is null. It is not enough
        // to just set the object to null.
        //
        cachedObject.forEach((key, ct) -> removeFromTileList(key, REMOVE_FROM_FLUSH));

        // reset counters before diagnostics
        cacheHitCount = 0;
        cacheMissCount = 0;

        if ( memoryCapacity > 0 ) {
            cachedObject = new Hashtable<>(DEFAULT_HASHTABLE_CAPACITY, LOAD_FACTOR);
        }

        if ( cacheSortedSet != null ) {
            cacheSortedSet.clear();
            cacheSortedSet = Collections.synchronizedSortedSet( new TreeSet<LCCachedTile>(tileComparator) );
        }

        tileList.clear();

        // force reset after diagnostics
        cacheTileCount = 0;
        timeStamp   = 0;
        cacheMemoryUsed = 0;

        // no System.gc() here, it's too slow and may occur anyway.
    }

    /**
     * Returns the cachedObject's tile capacity.
     *
     * <p> This implementation of <code>TileCache</code> does not use
     * the tile capacity.  This method always returns 0.
     */
    @Override
    public int getTileCapacity() { return 0; }

    /**
     * Sets the cachedObject's tile capacity to the desired number of tiles.
     *
     * <p> This implementation of <code>TileCache</code> does not use
     * the tile capacity.  The cachedObject size is limited by the memory
     * capacity only.  This method does nothing and has no effect on
     * the cachedObject.
     *
     * @param tileCapacity  The desired tile capacity for this cachedObject
     *        in number of tiles.
     */
    @Override
    public void setTileCapacity(int tileCapacity) { }

    /**
     * Sets the cachedObject's memory capacity to the desired number of bytes.
     * If the new memory capacity is smaller than the amount of memory
     * currently being used by this cachedObject, tiles are removed from the
     * cachedObject until the memory usage is less than the specified memory
     * capacity.
     *
     * @param memoryCapacity  The desired memory capacity for this cachedObject
     *        in bytes.
     *
     * @throws IllegalArgumentException  If <code>memoryCapacity</code>
     *         is less than 0.
     */
    @Override
    public void setMemoryCapacity(long memoryCapacity) {
        if (memoryCapacity < 0) {
            throw new IllegalArgumentException("memory capacity must be >= 0");
        } else if ( memoryCapacity == 0 ) {
            flush();
        }

        this.memoryCapacity = memoryCapacity;

        if ( cacheMemoryUsed > memoryCapacity ) {
            memoryControl();
        }
    }

    /** Enable Tile Monitoring and Diagnostics */
    @Override
    public void enableDiagnostics() {
        diagnostics = true;
    }

    /** Turn off diagnostic notification */
    @Override
    public void disableDiagnostics() {
        diagnostics = false;
    }

    /**
     * Reset hit and miss counters.
     *
     * @since 1.1
     */
    @Override
    public void resetCounts() {
        cacheHitCount = 0;
        cacheMissCount = 0;
    }

    /**
     * Set the memory threshold value.
     *
     * @since 1.1
     */
    @Override
    public void setMemoryThreshold(float mt) {
        if ( mt < 0.0F || mt > 1.0F ) {
            throw new IllegalArgumentException("memory threshold must be between 0 and 1");
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
    @Override
    public float getMemoryThreshold() {
        return memoryThreshold;
    }

    /** Returns a string representation of the class object. */
    @Override
    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) +
               ": memoryCapacity = " + Long.toHexString(memoryCapacity) +
               " cacheMemoryUsed = " + Long.toHexString(cacheMemoryUsed) +
               " #tilesInCache = " + cachedObject.size();
    }

    /**
     * Removes tiles from the cachedObject based on their last-access time
     * (old to new) until the memory usage is memoryThreshold % of that of the
     * memory capacity.
     */
    @Override
    public synchronized void memoryControl() {
        if ( cacheSortedSet == null ) {
            standard_memory_control();
        } else {
            custom_memory_control();
        }
    }

    // time stamp based memory control (LRU)
    private void standard_memory_control() {
        long limit = (long)(memoryCapacity * memoryThreshold);

        while( cacheMemoryUsed > limit && ! tileList.isEmpty() ) {
            final Object lastKey = tileList.getLast().key;
            LCCachedTile ct = cachedObject.get(lastKey);

            if ( ct != null ) {
                if (m_objectCache != null) {
                    RenderedImage owner = ct.getOwner();
                    if (owner != null && owner.getProperty(JAIContext.PERSISTENT_CACHE_TAG) == Boolean.TRUE) {
                        writeTileToDisk(ct, lastKey);
                    }
                }
                removeFromTileList(lastKey, REMOVE_FROM_MEMCON);
            }
        }
    }

    private static class TileCacheCacheObjectBroker implements CacheObjectBroker {

        @Override
        public int getEncodedSizeOf( Object obj ) {
            if ( obj instanceof byte[] ) {
                final byte[] ba = (byte[])obj;
                return ba.length;
            } else if ( obj instanceof short[] ) {
                final short[] sa = (short[])obj;
                return sa.length * 2;
            } else if ( obj instanceof int[] ) {
                final int[] ia = (int[])obj;
                return ia.length * 4;
            } else
                throw new IllegalArgumentException(
                    "can't get size of " + obj.getClass()
                );
        }

        // TODO: cachedObject the ByteBuffer with a soft reference

        @Override
        public Object decodeFromByteBuffer( ByteBuffer buf, Object obj ) {
            if ( obj instanceof byte[] )
                if ( buf.hasArray() ) 
                    LCArrays.copy( buf.array(), 0, (byte[])obj, 0, buf.capacity() );
                else
                    buf.get( (byte[])obj );
            else if ( obj instanceof short[] )
                if ( buf.hasArray() ) 
                    LCArrays.copy( buf.array(), 0, (short[])obj, 0, buf.capacity() );
                else
                    buf.asShortBuffer().get( (short[])obj );
            else if ( obj instanceof int[] )
                if ( buf.hasArray() ) 
                    LCArrays.copy( buf.array(), 0, (int[])obj, 0, buf.capacity() );
                else
                    buf.asIntBuffer().get( (int[])obj );
            else
                throw new IllegalArgumentException(
                    "can't decode " + obj.getClass()
                );
            return obj;
        }

        @Override
        public void encodeToByteBuffer( ByteBuffer buf, Object obj ) {
            if ( obj instanceof byte[] )
                if ( buf.hasArray() ) 
                    LCArrays.copy( (byte[])obj, 0, buf.array(), 0, buf.capacity() );
                else
                    buf.put( (byte[])obj );
            else if ( obj instanceof short[] )
                if ( buf.hasArray() ) 
                    LCArrays.copy( (short[])obj, 0, buf.array(), 0, buf.capacity() );
                else
                    buf.asShortBuffer().put( (short[])obj );
            else if ( obj instanceof int[] )
                if ( buf.hasArray() ) 
                    LCArrays.copy( (int[])obj, 0, buf.array(), 0, buf.capacity() );
                else
                    buf.asIntBuffer().put( (int[])obj );
            else
                throw new IllegalArgumentException(
                    "can't encode " + obj.getClass()
                );
        }
    }

    static class CacheFileFilter implements FilenameFilter {
        File goodFile;

        CacheFileFilter(File goodFile) {
            this.goodFile = goodFile;
        }

        @Override
        public boolean accept(File dir, String name) {
            return !name.equals(goodFile.getName()) && name.startsWith("LCCacheFile") && name.endsWith(".cce");
        }
    }

    private final static Preferences Prefs =
        Preferences.userNodeForPackage(LCTileCache.class);

    private final static String CacheDirKey = "ScratchDirectory";

    private File tmpFile = null;

    private Cache createDiskCache() {
        try {
            // Try creating the temp file in the user-specified location
            String path = Prefs.get(CacheDirKey, null);
            tmpFile = null;
            if (path != null) {
                File tmpDir = new File(path);
                if (tmpDir.isDirectory() && tmpDir.canWrite()) {
                    tmpFile = File.createTempFile("LCCacheFile", ".cce", tmpDir);
                }
            }
            // Fall back to the regular java temp directory
            if (tmpFile == null) {
                tmpFile = File.createTempFile("LCCacheFile", ".cce");
            }
            tmpFile.deleteOnExit();

            // Try to delete old cachedObject files, checking if the file is locked by some other instance of ourself
            File[] oldCacheFiles = tmpFile.getParentFile().listFiles(new CacheFileFilter(tmpFile));
            if ( oldCacheFiles != null )
                for (File oldCacheFile : oldCacheFiles) {
                    oldCacheFile.delete();
                }

            int defaultMemorySize = MemoryLimits.getDefault();
            Preferences prefs = Preferences.userRoot().node("/com/lightcrafts/app");
            long maxMemory = (long) prefs.getInt("MaxMemory", defaultMemorySize) * 1024 * 1024;
            long maxHeap = Runtime.getRuntime().maxMemory();
            long extraCacheSize = Math.max( maxMemory - maxHeap, 0 );

            System.out.println("Allocating " + (extraCacheSize / (1024 * 1024)) + "MB for the image cachedObject.");

            return new Cache(
                new TileCacheCacheObjectBroker(),
                extraCacheSize < 128 * 1024 * 1024 ?
                    new WriteThroughCacheObjectMap() :
                    new LRUCacheObjectMap(
                        new NativeByteBufferAllocator( CHUNK_SIZE ), extraCacheSize
                    ),
                new DirectFileCacheStore( tmpFile ),
                new CoalescingFreeBlockManager()
            );
        }
        catch ( IOException e ) {
            e.printStackTrace();
            return null;
        }
    }

    // private static final long CACHE_SIZE = (long) (1024 * 1024 * 1024);
    private static final int CHUNK_SIZE = 16 * 1024 * 1024;

    public synchronized void dispose() throws IOException {
        m_objectCache.dispose();

        // Close and delete the old cachedObject file
        if (m_tileReaper != null)
            m_tileReaper.kill();

        if (tmpFile != null)
            tmpFile.delete();
    }

    /**
     * Finalize an <code>LCTileCache</code>.
     */
    @Override
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    private long tilesWritten = 0;
    private long tilesRead = 0;
    private long tilesOnDisk = 0;

    public long tilesWritten() {
        return tilesWritten;
    }

    public long tilesRead() {
        return tilesRead;
    }

    public long tilesOnDisk() {
        return tilesOnDisk;
    }

    private Raster readTileFromDisk(RenderedImage owner, int tileX, int tileY, Object key) {
        if (m_objectCache.contains(key)) {
            SampleModel sm = owner.getSampleModel();
            DataBuffer db = sm.createDataBuffer();

            try {
                switch (db.getDataType()) {
                    case DataBuffer.TYPE_BYTE:
                        m_objectCache.getOnce(key, ((DataBufferByte) db).getData());
                        break;

                    case DataBuffer.TYPE_USHORT:
                        m_objectCache.getOnce(key, ((DataBufferUShort) db).getData());
                        break;

                    case DataBuffer.TYPE_INT:
                        m_objectCache.getOnce(key, ((DataBufferInt) db).getData());
                        break;

                    default:
                        throw new IllegalArgumentException("unsupported image type " + db.getClass());
                }
                synchronized (this) {
                    tilesOnDisk--;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            WritableRaster raster;

            if (true)
                raster = Raster.createWritableRaster(sm, db, new Point(tileX * owner.getTileWidth(),
                                                                       tileY * owner.getTileHeight()));
            else {
                int bands = sm.getNumBands();
                int[] bandOffsets = ((PixelInterleavedSampleModel) sm).getBandOffsets();

                raster = Raster.createInterleavedRaster(db, owner.getTileWidth(), owner.getTileHeight(),
                                                        bands * owner.getTileWidth(), bands, bandOffsets,
                                                        new Point(tileX * owner.getTileWidth(),
                                                                  tileY * owner.getTileHeight()));
            }
            synchronized (this) {
                tilesRead++;
            }
            return raster;
        } else
            return null;
    }

    private void writeTileToDisk(LCCachedTile ct, Object key) {
        Raster raster = ct.getTile();
        DataBuffer db = raster.getDataBuffer();

        try {
            switch (db.getDataType()) {
                case DataBuffer.TYPE_BYTE:
                    m_objectCache.put(key, ((DataBufferByte) db).getData());
                    break;

                case DataBuffer.TYPE_USHORT:
                    m_objectCache.put(key, ((DataBufferUShort) db).getData());
                    break;

                case DataBuffer.TYPE_INT:
                    m_objectCache.put(key, ((DataBufferInt) db).getData());
                    break;

                default:
                    throw new IllegalArgumentException("unsupported image type " + db.getClass());
            }
            synchronized (this) {
                tilesOnDisk++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        synchronized (this) {
            tilesWritten++;
        }
    }

    // tileComparator based memory control (TreeSet)
    private void custom_memory_control() {
        long limit = (long)(memoryCapacity * memoryThreshold);
        Iterator<LCCachedTile> iter = cacheSortedSet.iterator();

        while( iter.hasNext() && (cacheMemoryUsed > limit) ) {
            final LCCachedTile ct = iter.next();

            cacheMemoryUsed -= ct.tileSize;
            synchronized (this) {
                cacheTileCount--;
            }

            // remove from sorted set
            try {
                iter.remove();
            } catch(ConcurrentModificationException e) {
                ImagingListener listener =
                    ImageUtil.getImagingListener((RenderingHints)null);
                listener.errorOccurred("something wrong with the TileCache",
                                       e, this, false);
//                e.printStackTrace();
            }

            tileList.remove(ct);
            cachedObject.remove(ct.key);

            if ( diagnostics ) {
                ct.action = REMOVE_FROM_MEMCON;
                setChanged();
                notifyObservers(ct);
            }
        }

        // If the custom memory control didn't release sufficient
        // number of tiles to satisfy the memory limit, fallback
        // to the standard memory controller.
        if ( cacheMemoryUsed > limit ) {
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
    @Override
    public synchronized void setTileComparator(Comparator c) {
        if (tileComparator != null)
            throw new IllegalArgumentException("TileComparator not supported by LCTileCache");

        tileComparator = c;

        if ( tileComparator == null ) {
            // turn off tileComparator
            if ( cacheSortedSet != null ) {
                cacheSortedSet.clear();
                cacheSortedSet = null;
            }
        } else {
            // copy tiles from hashtable to sorted tree set
            cacheSortedSet = Collections.synchronizedSortedSet( new TreeSet<LCCachedTile>(tileComparator) );
            cachedObject.forEach((key, ct) -> cacheSortedSet.add(ct));
        }
    }

    // test
    public void dump() {

        System.out.println("first = " + tileList.getFirst());
        System.out.println("last  = " + tileList.getLast());

        int k = 0;
        for (LCCachedTile ct : cacheSortedSet) {
            System.out.println(k++);
            System.out.println(ct);
        }
    }

    void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener =
            ImageUtil.getImagingListener((RenderingHints)null);
        listener.errorOccurred(message, e, this, false);
    }

    /**
     * A <code>TileReaper</code> is-a {@link Thread} that runs continuously and
     * asynchronously in the background waiting for {@link RenderedImage}s that
     * the Java garbage collector has determined are weakly reachable.  Once
     * that's the case, remove all of a {@link RenderedImage}'s associated
     * tiles from the disk cachedObject.
     */
    private static final class TileReaper extends Thread {

        ////////// public /////////////////////////////////////////////////////

        /**
         * Run the thread: wait for a weakly reachable {@link RenderedImage} to
         * become available and remove all of its tiles from the disk cachedObject
         * (if any).
         */
        @Override
        public void run() {
            while ( !m_killed ) {
                try {
                    final Reference weakKey = m_refQ.remove(); // Image to be garbage collected

                    final LCTileCache tileCache =
                            m_tileCacheRef.get();

                    if ( tileCache == null )
                        break;

                    synchronized ( tileCache ) {
                        // System.out.println( "Removing tiles from caches" );

                        Set hashKeys = tileCache.m_imageMap.remove(weakKey);

                        assert hashKeys != null;

                        for (Object o : hashKeys) {
                            if (tileCache.removeFromTileList(o, REMOVE_FROM_GCEVENT)) {
                                // System.out.println("removed entry from memory cachedObject");
                            }

                            if (tileCache.m_objectCache.remove(o)) {
                                synchronized (tileCache) {
                                    tileCache.tilesOnDisk--;
                                }
                                // System.out.println("removed entry from disk cachedObject");
                            }
                        }
                    }
                }
                catch ( InterruptedException e ) {
                    // do nothing
                }
            }
        }

        ////////// package ////////////////////////////////////////////////////

        /**
         * Construct a <code>TileReaper</code> and make it a daemon.
         */
        TileReaper( LCTileCache tileCache ) {
            super("TileReaper");
            setDaemon( true );
            m_refQ = new ReferenceQueue<>();
            m_tileCacheRef = new WeakReference<>( tileCache );
        }

        /**
         * Gets the {@link ReferenceQueue} being used.
         *
         * @return Returns said {@link ReferenceQueue}.
         */
        ReferenceQueue<RenderedImage> getRefQ() {
            return m_refQ;
        }

        /**
         * Kill this thread.
         */
        void kill() {
            m_killed = true;
            interrupt();
        }

        ////////// private ////////////////////////////////////////////////////

        /**
         * A flag to indicate whether this thread has been killed.
         */
        private boolean m_killed;

        /**
         * The {@link ReferenceQueue} wherein the Java garbage collector
         * deposits objects that have become weakly reachable.
         */
        private final ReferenceQueue<RenderedImage> m_refQ;

        /**
         * A {@link WeakReference} to the owning {@link LCTileCache}.
         * TODO: explain why this is needed instead of using an inner class.
         */
        private final WeakReference<LCTileCache> m_tileCacheRef;
    }

    /**
     * This is a set of {@link WeakReference}s to {@link RenderedImage}s.
     */
    private final Map<Reference<RenderedImage>, Set<Object>> m_imageMap = new HashMap<>();

    /**
     * The {@link TileReaper} associated with this  <code>LCTileCache</code>.
     */
    private TileReaper m_tileReaper;
}
/* vim:set et sw=4 ts=4: */
