/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

import java.nio.ByteBuffer;

/**
 * A <code>NativeChunk</code> is a chunk of off-heap memory.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class NativeChunk {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>NativeChunk</code>.
     *
     * @param size The size of the chunk to create, subject to the JVM's
     * direct-memory limit.
     */
    public NativeChunk( int size ) {
        m_buffer = ByteBuffer.allocateDirect( size );
        m_size = size;
    }

    /**
     * Drops this chunk's reference to its memory. Existing views remain valid
     * until they are no longer reachable and the JVM reclaims the direct buffer.
     */
    public synchronized void dispose() {
        m_buffer = null;
    }

    /**
     * Gets a view of a region in this chunk.
     *
     * @param position The start offset in bytes.
     * @param size The size of the region in bytes.
     * @return A direct buffer view over the requested region.
     */
    public synchronized ByteBuffer getByteBuffer( int position, int size ) {
        if ( m_buffer == null )
            throw new IllegalStateException( "chunk is disposed" );
        final ByteBuffer view = m_buffer.duplicate();
        view.position( position );
        view.limit( Math.addExact( position, size ) );
        return view.slice();
    }

    /**
     * Gets the size of the native chunk.
     *
     * @return Returns said size (in bytes).
     */
    public int getSize() {
        return m_size;
    }

    ////////// private ////////////////////////////////////////////////////////

    private ByteBuffer m_buffer;

    /**
     * The size of the chunk (in bytes).
     */
    private final int m_size;

}
/* vim:set et sw=4 ts=4: */
