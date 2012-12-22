/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A <code>CacheStore</code> is used to store (and later fetch) an object to
 * some auxiliary storage, e.g., a file on disk.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface CacheStore {

    /**
     * Clears this <code>CacheStore</code>.
     */
    void clear() throws IOException;

    /**
     * Dispose of this <code>CacheStore</code>.
     */
    void dispose() throws IOException;

    /**
     * Gets a new {@link ByteBuffer} for use in subsequent calls to
     * {@link #readFromStore(long,ByteBuffer)} or
     * {@link #writeToStore(long,ByteBuffer)}.
     *
     * @param size The size of the {@link ByteBuffer} to create (in bytes).
     * @return Returns said {@link ByteBuffer}.
     */
    ByteBuffer getByteBuffer( int size );

    /**
     * Gets the current size of the cache.  Note that a call to this method
     * that is followed by a call to {@link #writeToStore(long,ByteBuffer)}
     * must both be in a <code>synchronized</code> block on the store.
     *
     * @return Returns said size.
     */
    long getSize() throws IOException;

    /**
     * Read data from the {@link CacheStore}.
     *
     * @param pos The position within the store to read from.
     * @param buf The {@link ByteBuffer} to read into.
     */
    void readFromStore( long pos, ByteBuffer buf ) throws IOException;

    /**
     * Write data to the {@link CacheStore}.
     *
     * @param pos The position within the store to write to.
     * @param buf The {@link ByteBuffer} to write.
     */
    void writeToStore( long pos, ByteBuffer buf ) throws IOException;

}
/* vim:set et sw=4 ts=4: */
