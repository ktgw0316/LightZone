/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2013-     Masahiro Kitagawa */

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
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.media.jai.EnumeratedParameter;
import javax.media.jai.TileCache;
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
 * @see javax.media.jai.TileCache
 *
 */

public final class LCTileCache extends Observable
                                implements TileCache,
        CacheDiagnostics {

    /** The default memory capacity of the cache (16 MB). */
    private static final long DEFAULT_MEMORY_CAPACITY = 16L * 1024L * 1024L;

    /** The default hashtable capacity (heuristic) */
    private static final int DEFAULT_HASHTABLE_CAPACITY = 1009; // prime number

    /** The hashtable load factor */
    private static final float LOAD_FACTOR = 0.5F;

    /** Listener for the flush() method, to detect low memory situations. */
    @Setter
    private static LCTileCacheListener Listener;

    /**
     * The tile cache.
     * A LinkedHashMap is used to cache the tiles.  The "key" is a
     * <code>Object</code> determined based on tile owner's UID if any or
     * hashCode if the UID doesn't exist, and tile index.  The
     * "value" is a LCCachedTile.
     */
    @Getter
    private final LinkedHashMap<Object, LCCachedTile> cachedObject;

    /** The memory capacity of the cache. */
    @Getter
    private long memoryCapacity;

    /** The amount of memory currently being used by the cache. */
    @Getter
    private long cacheMemoryUsed = 0;

    /** The amount of memory to keep after memory control */
    private float memoryThreshold = 0.75F;

    /** A indicator for tile access time. */
    private long timeStamp = 0;

    @Override
    public long getCacheTileCount() {
        return cachedObject.size();
    }

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
     * @param memoryCapacity  The maximum cache memory size in bytes.
     *
     * @throws IllegalArgumentException  If <code>memoryCapacity</code>
     *         is less than 0.
     */
    public LCTileCache(long memoryCapacity, boolean useDisk) {
        if (memoryCapacity < 0) {
            throw new IllegalArgumentException("memory capacity must be >= 0");
        }

        this.memoryCapacity = memoryCapacity;

        // lower values of LOAD_FACTOR increase speed, decrease space efficiency
        cachedObject = new LinkedHashMap<>(DEFAULT_HASHTABLE_CAPACITY, LOAD_FACTOR, true);

        if (useDisk) {
            m_objectCache = createDiskCache();

            m_tileReaper = new TileReaper( this );
            m_tileReaper.start();
        }
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
    @Override
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
    @Override
    public synchronized void add(RenderedImage owner,
                                 int tileX,
                                 int tileY,
                                 Raster tile,
                                 Object tileCacheMetric) {

        if ( memoryCapacity == 0 ) {
            return;
        }

        // This tile is not in the cache; create a new LCCachedTile.
        // else just update.
        Object key = LCCachedTile.hashKey(owner, tileX, tileY);
        final LCCachedTile ct;
        if (cachedObject.containsKey(key)) {
            ct = cachedObject.get(key);
            updateTileList(ct, UPDATE_FROM_ADD);
        } else {
            // create a new tile
            ct = new LCCachedTile(owner, tileX, tileY, tile, tileCacheMetric);

            // add to tile cache
            cachedObject.put(ct.key, ct);
            updateTileList(ct, ADD);
            cacheMemoryUsed += ct.tileSize;

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

    /// Notify observers that a tile has been changed.
    private void diagnosis(LCCachedTile ct, int action) {
        if (diagnostics) {
            ct.action = action;
            setChanged();
            notifyObservers(ct);
        }
    }

    private boolean removeFromTileList(Object key, int action) {
        LCCachedTile ct = cachedObject.remove(key);
        if (ct != null) {
            removeTile(ct, action);
            return true;
        }
        return false;
    }

    private void removeTile(@NotNull LCCachedTile ct, int action) {
        cacheMemoryUsed -= ct.tileSize;
            diagnosis(ct, action);
    }

    private void updateTileList(LCCachedTile ct, int action) {
        ct.tileTimeStamp = timeStamp++;
        cacheHitCount++;
        diagnosis(ct, action);
    }

    /**
     * Removes a tile from the cache.
     *
     * <p> If the specified tile is not in the cache, this method
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
            // cache before the observers get notified.  This should
            // be ok, since a hard reference to the tile will be
            // kept for the observers, so the garbage collector won't
            // remove the tile until the observers release it.
            diagnosis(ct, ABOUT_TO_REMOVE);
            removeFromTileList(key, REMOVE);
        } else {
            // if the tile is not in the memory cache than it might be on disk...
            if (m_objectCache != null && m_objectCache.contains(key)) {
                m_objectCache.remove(key);
                tilesOnDisk--;
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
     * Retrieves a contiguous array of all tiles in the cache which are
     * owned by the specified image.  May be <code>null</code> if there
     * were no tiles in the cache.  The array contains no null entries.
     *
     * @param owner The <code>RenderedImage</code> to which the tiles belong.
     * @return An array of all tiles owned by the specified image or
     *         <code>null</code> if there are none currently in the cache.
     */
    @Override
    public synchronized Raster[] getTiles(RenderedImage owner) {
        Raster[] tiles = null;

        if ( memoryCapacity == 0 ) {
            return null;
        }

        int size = Math.min(owner.getNumXTiles() * owner.getNumYTiles(),
                            cachedObject.size());

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
     * from the cache.
     *
     * @param owner  The image whose tiles are to be removed from the cache.
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
     * Returns an array of tile <code>Raster</code>s from the cache.
     * Any or all of the elements of the returned array may be <code>null</code>
     * if the corresponding tile is not in the cache.
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

    /** Removes -ALL- tiles from the cache. */
    @Override
    public synchronized void flush() {
        // Call the LCTileCacheListener, if one is defined.  This helps detect
        // low memory conditions.
        if (Listener != null) {
            Listener.tileCacheFlushed();
        }
        // NOTE: we don't do flushing for disk caches, it wipes the persistent cache, rather spill half of the cache out
        if (m_objectCache != null) {
            System.err.println("flushing the cache");
            float mt = memoryThreshold;
            memoryThreshold = 0.1f;
            memoryControl();
            memoryThreshold = mt;
            return;
        }

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
        cachedObject.forEach((key, ct) -> removeFromTileList(key, REMOVE_FROM_FLUSH));

        // reset counters before diagnostics
        cacheHitCount = 0;
        cacheMissCount = 0;

        if ( memoryCapacity > 0 ) {
            cachedObject.clear();
        }

        // force reset after diagnostics
        timeStamp   = 0;
        cacheMemoryUsed = 0;

        // no System.gc() here, it's too slow and may occur anyway.
    }

    /**
     * Returns the cache's tile capacity.
     *
     * <p> This implementation of <code>TileCache</code> does not use
     * the tile capacity.  This method always returns 0.
     */
    @Override
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
    @Override
    public void setTileCapacity(int tileCapacity) { }

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
     * Removes tiles from the cache based on their last-access time
     * (old to new) until the memory usage is memoryThreshold % of that of the
     * memory capacity.
     */
    @Override
    public synchronized void memoryControl() {
        long limit = (long)(memoryCapacity * memoryThreshold);

        final var iter = cachedObject.entrySet().iterator();
        while (iter.hasNext() && cacheMemoryUsed > limit) {
            final var eldestEntry = iter.next();
            final Object eldestKey = eldestEntry.getKey();
            final LCCachedTile ct = eldestEntry.getValue();

            if (ct != null) {
                if (m_objectCache != null) {
                    RenderedImage owner = ct.getOwner();
                    if (owner != null && owner.getProperty(JAIContext.PERSISTENT_CACHE_TAG) == Boolean.TRUE) {
                        writeTileToDisk(ct, eldestKey);
                    }
                }
                removeTile(ct, REMOVE_FROM_MEMCON);
            }
            iter.remove();
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

        // TODO: cache the ByteBuffer with a soft reference

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

            // Try to delete old cache files, checking if the file is locked by some other instance of ourself
            File[] oldCacheFiles = tmpFile.getParentFile().listFiles(new CacheFileFilter(tmpFile));
            if ( oldCacheFiles != null )
                for (File oldCacheFile : oldCacheFiles) {
                    oldCacheFile.delete();
                }

            final long extraCacheSize = getExtraCacheSize();
            System.out.println("Allocating " + extraCacheSize / MB + " MB for the image cache.");

            return new Cache(
                new TileCacheCacheObjectBroker(),
                extraCacheSize < 128 * MB ?
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

    private static long getExtraCacheSize() {
        final int defaultMemorySize = MemoryLimits.getDefault();
        final var prefs = Preferences.userRoot().node("/com/lightcrafts/app");
        final long maxMemory = (long) prefs.getInt("MaxMemory", defaultMemorySize) * MB;
        final long maxHeap = Runtime.getRuntime().maxMemory();
        return Math.max(0, maxMemory - maxHeap);
    }

    private static final int MB = 1024 * 1024;

    private static final int CHUNK_SIZE = 16 * MB;

    public synchronized void dispose() throws IOException {
        m_objectCache.dispose();

        // Close and delete the old cache file
        if (m_tileReaper != null)
            m_tileReaper.kill();

        if (tmpFile != null)
            tmpFile.delete();
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

            raster = Raster.createWritableRaster(sm, db, new Point(
                    tileX * owner.getTileWidth(),
                    tileY * owner.getTileHeight()));
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

    /**
     * Not Supported
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public Comparator<LCCachedTile> getTileComparator() {
        throw new UnsupportedOperationException("Comparator not supported");
    }

    /**
     * Not Supported
     *
     * @throws UnsupportedOperationException
     */
    @Override
    public synchronized void setTileComparator(Comparator c) {
        throw new UnsupportedOperationException("Comparator not supported");
    }

    private synchronized void reap(Reference<? extends RenderedImage> weakKey) {
        Set<Object> hashKeys = m_imageMap.remove(weakKey);
        assert hashKeys != null;

        for (Object o : hashKeys) {
            removeFromTileList(o, REMOVE_FROM_GCEVENT);
            if (m_objectCache.remove(o)) {
                tilesOnDisk--;
                // System.out.println("removed entry from disk cache");
            }
        }
    }

    /**
     * A <code>TileReaper</code> is-a {@link Thread} that runs continuously and
     * asynchronously in the background waiting for {@link RenderedImage}s that
     * the Java garbage collector has determined are weakly reachable.  Once
     * that's the case, remove all of a {@link RenderedImage}'s associated
     * tiles from the disk cache.
     */
    private static final class TileReaper extends Thread {

        ////////// public /////////////////////////////////////////////////////

        /**
         * Run the thread: wait for a weakly reachable {@link RenderedImage} to
         * become available and remove all of its tiles from the disk cache
         * (if any).
         */
        @Override
        public void run() {
            while ( !m_killed ) {
                try {
                    // Image to be garbage collected
                    final Reference<? extends RenderedImage> weakKey = m_refQ.remove();

                    final LCTileCache tileCache = m_tileCacheRef.get();
                    if (tileCache == null) {
                        break;
                    }
                    tileCache.reap(weakKey);
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
