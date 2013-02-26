/*
 * $RCSfile: TileCache.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:22 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Point;
import java.awt.image.Raster;
import java.util.Comparator;
import java.awt.image.RenderedImage;

/**
 * A class implementing a caching mechanism for image tiles.
 *
 * <p> <code>TileCache</code> provides a mechanism by which an
 * <code>OpImage</code> may cache its computed tiles.  There may be
 * multiple <code>TileCache</code>s used in an application up to the
 * point of having a different <code>TileCache</code> for each
 * <code>OpImage</code>.
 *
 * <p> The <code>TileCache</code> used for a particular <code>OpImage</code>
 * is derived from the <code>RenderingHints</code> assigned to the
 * associated imaging chain node.  If the node is constructed using
 * <code>JAI.create()</code> and no <code>TileCache</code> is specified
 * in the <code>RenderingHints</code> parameter, then one is derived
 * from the <code>RenderingHints</code> associated with the instance of the
 * <code>JAI</code> class being used.
 *
 * <p> In the Sun reference implementation, the cache size is limited by
 * the memory capacity, which is set to a default value at construction
 * or subsequently using the <code>setMemoryCapacity()</code> method.
 * The initial value may be obtained using <code>getMemoryCapacity()</code>.
 * The tile capacity is not used as different images may have very different
 * tile sizes so that this metric is not a particularly meaningful control
 * of memory resource consumption in general.
 *
 * @see JAI
 * @see RenderedOp
 * @see java.awt.RenderingHints
 */
public interface TileCache {

    /**
     * Adds a tile to the cache.
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileX The X index of the tile in the owner's tile grid.
     * @param tileY The Y index of the tile in the owner's tile grid.
     * @param data A <code>Raster</code> containing the tile data.
     */
    void add(RenderedImage owner, int tileX, int tileY, Raster data);

    /**
     * Adds a tile to the cache with an associated compute cost
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileX The X index of the tile in the owner's tile grid.
     * @param tileY The Y index of the tile in the owner's tile grid.
     * @param data A <code>Raster</code> containing the tile data.
     * @param tileCacheMetric An <code>Object</code> as a tile metric.
     *
     * @since JAI 1.1
     */
    void add(RenderedImage owner, int tileX, int tileY, Raster data, Object tileCacheMetric);
    
    /**
     * Advises the cache that a tile is no longer needed.  It is legal
     * to implement this method as a no-op.
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileX The X index of the tile in the owner's tile grid.
     * @param tileY The Y index of the tile in the owner's tile grid.
     */
    void remove(RenderedImage owner, int tileX, int tileY);
    
    /**
     * Retrieves a tile.  Returns <code>null</code> if the tile is not
     * present in the cache.
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileX The X index of the tile in the owner's tile grid.
     * @param tileY The Y index of the tile in the owner's tile grid.
     */
    Raster getTile(RenderedImage owner, int tileX, int tileY);
    
    /**
     * Retrieves an array of all tiles in the cache which are owned by the
     * specified image.
     *
     * @param owner The <code>RenderedImage</code> to which the tiles belong.
     * @return An array of all tiles owned by the specified image or
     *         <code>null</code> if there are none currently in the cache.
     *
     * @since JAI 1.1
     */
    Raster[] getTiles(RenderedImage owner);

    /** 
     * Advises the cache that all tiles associated with a given image
     * are no longer needed.  It is legal to implement this method as
     * a no-op.
     *
     * @param owner The <code>RenderedImage</code> owner of the tiles
     *        to be removed.
     */
    void removeTiles(RenderedImage owner);

    /**
     * Adds an array of tiles to the tile cache.
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileIndices An array of <code>Point</code>s containing the
     *        <code>tileX</code> and <code>tileY</code> indices for each tile.
     * @param tiles The array of tile <code>Raster</code>s containing tile data.
     * @param tileCacheMetric Object which provides an ordering metric
     *        associated with the <code>RenderedImage</code> owner.
     *
     * @since JAI 1.1
     */
    void addTiles(RenderedImage owner, Point[] tileIndices, Raster[] tiles, Object tileCacheMetric);

    /**
     * Returns an array of tile <code>Raster</code>s from the cache.
     * Any or all of the elements of the returned array may be
     * <code>null</code> if the corresponding tile is not in the cache.
     * The length of the returned array must be the same as that of the
     * parameter array and the <i>i</i>th <code>Raster</code> in the
     * returned array must correspond to the <i>i</i>th tile index in
     * the parameter array.
     *
     * @param owner The <code>RenderedImage</code> that the tile belongs to.
     * @param tileIndices  An array of <code>Point</code>s containing the
     *        <code>tileX</code> and <code>tileY</code> indices for each tile.
     *
     * @since JAI 1.1
     */
    Raster[] getTiles(RenderedImage owner, Point[] tileIndices);

    /**
     * Advises the cache that all of its tiles may be discarded.  It
     * is legal to implement this method as a no-op.
     */
    void flush();

    /**
     * Advises the cache that some of its tiles may be discarded.  It
     * is legal to implement this method as a no-op.
     *
     * @since JAI 1.1
     */
    void memoryControl();

    /**
     * Sets the tile capacity to a desired number of tiles.
     * If the capacity is smaller than the current capacity,
     * tiles are flushed from the cache.  It is legal to
     * implement this method as a no-op.
     *
     * @param tileCapacity The new capacity, in tiles.
     *
     * @deprecated as of JAI 1.1.
     */
    void setTileCapacity(int tileCapacity);

    /**
     * Returns the tile capacity in tiles.  It is legal to
     * implement this method as a no-op which should be
     * signaled by returning zero.
     *
     * @deprecated as of JAI 1.1.
     */
    int getTileCapacity();

    /**
     * Sets the memory capacity to a desired number of bytes.
     * If the memory capacity is smaller than the amount of 
     * memory currently used by the cache, tiles are flushed 
     * until the <code>TileCache</code>'s memory usage is less than
     * <code>memoryCapacity</code>.
     *
     * @param memoryCapacity The new capacity, in bytes.
     */
    void setMemoryCapacity(long memoryCapacity);

    /**
     * Returns the memory capacity in bytes.
     */
    long getMemoryCapacity();

    /**
     * Sets the <code>memoryThreshold</code> value to a floating
     * point number that ranges from 0.0 to 1.0.
     * When the cache memory is full, the memory
     * usage will be reduced to this fraction of
     * the total cache memory capacity.  For example,
     * a value of .75 will cause 25% of the memory
     * to be cleared, while retaining 75%.
     *
     * @param memoryThreshold.  Retained fraction of memory
     * @throws IllegalArgumentException if the memoryThreshold
     *         is less than 0.0 or greater than 1.0
     *
     * @since JAI 1.1
     */
    void setMemoryThreshold(float memoryThreshold);

    /**
     * Returns the memory threshold, which is the fractional
     * amount of cache memory to retain during tile removal.
     *
     * @since JAI 1.1
     */
    float getMemoryThreshold();

    /**
     * Sets a <code>Comparator</code> which imposes an order on the
     * <code>CachedTile</code>s stored in the <code>TileCache</code>.
     * This ordering is used in <code>memoryControl()</code> to determine
     * the sequence in which tiles will be removed from the
     * <code>TileCache</code> so as to reduce the memory to the level given
     * by the memory threshold.  The <code>Object</code>s passed to the
     * <code>compare()</code> method of the <code>Comparator</code> will
     * be instances of <code>CachedTile</code>.  <code>CachedTile</code>s
     * will be removed from the <code>TileCache</code> in the ascending
     * order imposed by this <code>Comparator</code>.  If no
     * <code>Comparator</code> is currently set, the <code>TileCache</code>
     * should use an implementation-dependent default ordering.  In the
     * Sun Microsystems, Inc., implementation of <code>TileCache</code>,
     * this ordering is the <u>l</u>east <u>r</u>ecently <u>u</u>sed
     * ordering, i.e., the tiles least recently used will be removed first
     * by <code>memoryControl()</code>.
     *
     * @param comparator A <code>Comparator</code> which orders the
     *        <code>CachedTile</code>s stored by the <code>TileCache</code>;
     *        if <code>null</code> an implementation-dependent algorithm
     *        will be used.
     *
     * @since JAI 1.1
     */
    void setTileComparator(Comparator comparator);

    /**
     * Returns the <code>Comparator</code> currently set for use in ordering
     * the <code>CachedTile</code>s stored by the <code>TileCache</code>.
     *
     * @return The tile <code>Comparator</code> or <code>null</code> if the
     *         implementation-dependent ordering algorithm is being used.
     *
     * @since JAI 1.1
     */
    Comparator getTileComparator();
}
