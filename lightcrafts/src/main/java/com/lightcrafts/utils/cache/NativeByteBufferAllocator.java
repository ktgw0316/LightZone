/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import com.lightcrafts.utils.bytebuffer.ByteBufferAllocator;

/**
 * A <code>NativeByteBufferAllocator</code> is-a {@link ByteBufferAllocator}
 * that uses direct off-heap memory for allocated {@link ByteBuffer}s.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class NativeByteBufferAllocator implements ByteBufferAllocator {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>NativeByteBufferAllocator</code>.
     *
     * @param chunkSize The size of each chunk to allocate.
     */
    public NativeByteBufferAllocator( int chunkSize ) {
        m_chunkList = new LinkedList<>();
        m_chunkSize = chunkSize;
        m_freeBlockManagerList = new LinkedList<>();
        m_allocdBlocks = new IdentityHashMap<>();
        m_allocdFBMs = new IdentityHashMap<>();
    }

    /**
     * Allocates a new {@link ByteBuffer}.
     *
     * @param size The size of the {@link ByteBuffer} to allocate.  It must not
     * exceed the chunk size given to the constructor.
     * @return Returns said {@link ByteBuffer}.
     *
     * @throws IllegalArgumentException if the requested size is greater than
     * the chunk size given to the constructor.
     * @throws OutOfMemoryError if a {@link ByteBuffer} of the requested size
     * can not be allocated.
     */
    public synchronized ByteBuffer allocByteBuffer( int size ) {
        if ( size > m_chunkSize )
            throw new IllegalArgumentException( "size > chunkSize" );
        final Iterator<NativeChunk> c = m_chunkList.iterator();
        final Iterator<FreeBlockManager> f = m_freeBlockManagerList.iterator();
        while ( c.hasNext() ) {
            final NativeChunk chunk = c.next();
            final FreeBlockManager fbm = f.next();
            final ByteBuffer buf = getByteBuffer( size, chunk, fbm );
            if ( buf != null )
                return buf;
        }
        addChunk();
        final NativeChunk chunk = m_chunkList.getFirst();
        final FreeBlockManager fbm = m_freeBlockManagerList.getFirst();
        return getByteBuffer( size, chunk, fbm );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void clear() {
        dispose();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void dispose() {
        m_chunkList.forEach(NativeChunk::dispose);
        m_chunkList.clear();
        m_allocdBlocks.clear();
        m_allocdFBMs.clear();
        m_freeBlockManagerList.clear();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean freeByteBuffer( ByteBuffer buf ) {
        final CacheBlock block = m_allocdBlocks.remove( buf );
        final FreeBlockManager fbm = m_allocdFBMs.remove( buf );
        if ( block != null && fbm != null ) {
            fbm.freeBlock( block );
            return true;
        }
        assert block == null && fbm == null;
        return false;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Adds a new {@link NativeChunk} and corresponding {@link FreeBlockManager}
     * to the beginning of their respective lists.
     *
     * @throws OutOfMemoryError if a chunk can not be added.
     */
    private void addChunk() {
        final NativeChunk chunk = new NativeChunk( m_chunkSize );
        m_chunkList.addFirst( chunk );
        final FreeBlockManager fbm = new CoalescingFreeBlockManager();
        m_freeBlockManagerList.addFirst( fbm );
        //
        // Prime the FreeBlockManager by creating a free block that's the
        // entire size of the chunk.
        //
        fbm.freeBlock( new CacheBlock( 0, m_chunkSize ) );
    }

    /**
     * Gets a {@link ByteBuffer} of the requested size.
     *
     * @param size The size of the {@link ByteBuffer} to get.
     * @param chunk The {@link NativeChunk} to use.
     * @param fbm The {@link FreeBlockManager} to use.
     * @return Returns a {@link ByteBuffer} of the requested size or
     * <code>null</code> if the chunk has insufficient remaining size.
     */
    private ByteBuffer getByteBuffer( int size, NativeChunk chunk,
                                      FreeBlockManager fbm ) {
        final CacheBlock block = fbm.findBlockOfSize( size );
        if ( block != null ) {
            final ByteBuffer buf = chunk.getByteBuffer(
                Math.toIntExact( block.getPosition() ), size
            );
            buf.order( ByteOrder.nativeOrder() );
            m_allocdBlocks.put( buf, block );
            m_allocdFBMs.put( buf, fbm );
            return buf;
        }
        return null;
    }

    /**
     * A map of the allocated blocks keyed by the exact {@link ByteBuffer}
     * instance returned to the caller.
     * <p>
     * For every entry, there is a corresponding entry in
     * {@link #m_allocdFBMs} having the same key.
     */
    private final Map<ByteBuffer,CacheBlock> m_allocdBlocks;

    /**
     * A map of the allocated {@link FreeBlockManager}s keyed by the exact
     * {@link ByteBuffer} instance.
     * <p>
     * For every entry, there is a corresponding entry in
     * {@link #m_allocdBlocks} having the same key.
     */
    private final Map<ByteBuffer,FreeBlockManager> m_allocdFBMs;

    /**
     * A list of all the allocated chunks.  For every entry, there is a
     * corresponding entry in {@link #m_freeBlockManagerList}.
     */
    private final LinkedList<NativeChunk> m_chunkList;

    /**
     * The size of each chunk to allocate.
     */
    private final int m_chunkSize;

    /**
     * A list of all the {@link FreeBlockManager}s in use.  For every entry,
     * there is a corresponding entry in {@link #m_chunkList}.
     */
    private final LinkedList<FreeBlockManager> m_freeBlockManagerList;

}
/* vim:set et sw=4 ts=4: */
