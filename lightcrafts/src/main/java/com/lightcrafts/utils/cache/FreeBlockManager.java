/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

/**
 * A <code>FreeBlockManager</code> is used to manage (find, free)
 * {@link CacheBlock}s.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface FreeBlockManager {

    /**
     * Set the number of blocks being managed to zero.
     */
    void clear();

    /**
     * Try to find a block at least as large as the requested size.  If a block
     * larger than the requested size is found, it is resized to be exactly the
     * right size.
     *
     * @param objSize The minimum size of a block of space to find.
     * @return If a block can be found, returns it; otherwise returns
     * <code>null</code>.
     */
    CacheBlock findBlockOfSize( int objSize );

    /**
     * Release a block back to the manager to do with as it pleases.
     *
     * @param block The block to release.
     */
    void freeBlock( CacheBlock block );

}
/* vim:set et sw=4 ts=4: */
