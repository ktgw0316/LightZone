/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A <code>CoalescingFreeBlockManager</code> is-a {@link FreeBlockManager} that
 * coalesces adjacent free {@link CacheBlock}s.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class CoalescingFreeBlockManager implements FreeBlockManager {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>CoalescingFreeBlockManager</code>.
     */
    public CoalescingFreeBlockManager() {
        m_freeBlockList = new ArrayList<CacheBlock>();
    }

    /**
     * Set the number of blocks being managed to zero.
     */
    public synchronized void clear() {
        m_freeBlockList.clear();
    }

    /**
     * Try to find a block at least as large as the requested size.  If a block
     * larger than the requested size is found, it is resized to be exactly the
     * right size.
     *
     * @param objSize The minimum size of a block of space to find.
     * @return If a block can be found, returns it; otherwise returns
     * <code>null</code>.
     */
    public synchronized CacheBlock findBlockOfSize( int objSize ) {
        if ( !m_freeBlockList.isEmpty() )
            for ( Iterator<CacheBlock> i = m_freeBlockList.iterator();
                  i.hasNext(); ) {
                final CacheBlock block = i.next();
                final int blockSize = block.getSize();
                if ( blockSize == objSize ) {
                    //
                    // Found an exact fit: simply remove the block as-is from
                    // the free-block list.
                    //
                    i.remove();
                    return block;
                }
                if ( blockSize > objSize ) {
                    //
                    // Found a block that's bigger: adjust its position and
                    // size by the object's size, then return a new CacheBlock
                    // that's the exact size.
                    //
                    final long blockPos = block.getPosition();
                    block.setPosition( blockPos + objSize );
                    block.setSize( blockSize - objSize );
                    return new CacheBlock( blockPos, objSize );
                }
            }
        return null;
    }

    /**
     * Release a block of space back to the manager to put onto a free-space
     * list.
     *
     * @param freeBlock The block to free.
     */
    public synchronized void freeBlock( CacheBlock freeBlock ) {
        if ( m_freeBlockList.isEmpty() ) {
            m_freeBlockList.add( freeBlock );
            return;
        }
        //
        // Go through the free block list and try to coalesce blocks.  An
        // invariant is that all the blocks should be in ascending position
        // order.
        //
        final long freePos = freeBlock.getPosition();
        int freeSize = freeBlock.getSize();
        int index = 0;
        for ( Iterator<CacheBlock> i = m_freeBlockList.iterator();
              i.hasNext(); ++index ) {
            final CacheBlock block = i.next();
            final long blockPos = block.getPosition();
            final int blockSize = block.getSize();
            if ( blockPos + blockSize == freePos ) {
                //
                // The free block comes immediately after the current block:
                // coalesce it by expanding the size of the current block and
                // discarding the free block.
                //
                freeSize += blockSize;
                block.setSize( freeSize );

                if ( i.hasNext() ) {
                    //
                    // Check to see if the free block exactly filled a gap
                    // between the current block and the next block: if it did,
                    // coalesce the next block also.
                    //
                    final CacheBlock nextBlock = i.next();
                    if ( blockPos + freeSize == nextBlock.getPosition() ) {
                        block.setSize( freeSize + nextBlock.getSize() );
                        i.remove();
                    }
                }
                return;
            }
            if ( freePos + freeSize == blockPos ) {
                //
                // The free block comes immediately before the current block:
                // coalesce it by expanding the size of the current block and
                // discarding the free block.
                //
                block.setPosition( freePos );
                block.setSize( blockSize + freeSize );
                return;
            }
            if ( freePos > blockPos )
                break;
        }
        //
        // The free block couldn't be coalesced: just insert it.
        //
        m_freeBlockList.add( index, freeBlock );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The list of freed {@link CacheBlock}s.
     */
    private final ArrayList<CacheBlock> m_freeBlockList;
}
/* vim:set et sw=4 ts=4: */
