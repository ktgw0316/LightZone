/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.bytebuffer;

import java.nio.ByteBuffer;

/**
 * A <code>ByteBufferAllocator</code> is used to allocate {@link ByteBuffer}s
 * in some special way.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ByteBufferAllocator {

    /**
     * Allocates a new {@link ByteBuffer}.
     *
     * @param size The size of the {@link ByteBuffer} to allocate.
     * @return Returns said {@link ByteBuffer}.
     *
     * @throws OutOfMemoryError if a {@link ByteBuffer} of the requested size
     * can not be allocated.
     */
    ByteBuffer allocByteBuffer( int size );

    /**
     * Clear all allocated {@link ByteBuffer}s.
     */
    void clear();

    /**
     * Dispose of this <code>ByteBufferAllocator</code>.
     */
    void dispose();

    /**
     * Frees a {@link ByteBuffer} that was allocated using this
     * <code>ByteBufferAllocator</code> via {@link #allocByteBuffer(int)}.
     *
     * @param buf The {@link ByteBuffer} to free.
     * @return Returns <code>true</code> only if the {@link ByteBuffer} was
     * freed.
     */
    boolean freeByteBuffer( ByteBuffer buf );

}
/* vim:set et sw=4 ts=4: */
