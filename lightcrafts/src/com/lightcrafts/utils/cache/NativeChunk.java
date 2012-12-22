/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

/**
 * A <code>NativeChunk</code> is a chunk of allocated native memory.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class NativeChunk {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>NativeChunk</code>.
     *
     * @param size The size of the chunk to create.  Note that this is subject
     * to operating-system-dependent constraints such as minimum or maximum
     * sizes.
     */
    public NativeChunk( int size ) {
        m_addr = alloc( size );
        if ( m_addr == 0 )
            throw new OutOfMemoryError( "failed to alloc native chunk" );
        m_size = size;
    }

    /**
     * Disposes of the native memory used for this chunk.
     */
    public synchronized void dispose() {
        if ( m_addr != 0 ) {
            free( m_addr, m_size );
            m_addr = 0;
        }
    }

    /**
     * Gets the base address of the native chunk.
     *
     * @return Returns said address cast to a <code>long</code>.
     */
    public long getAddr() {
        return m_addr;
    }

    /**
     * Gets the size of the native chunk.
     *
     * @return Returns said size (in bytes).
     */
    public int getSize() {
        return m_size;
    }

    ////////// protected //////////////////////////////////////////////////////

    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Allocate a new chunk.
     *
     * @param size The size of the chunk to allocate (in bytes).
     * @return Returns the native address of the chunk cast to a
     * <code>long</code>.
     */
    private static native long alloc( int size );

    /**
     * Free a chunk.
     *
     * @param addr The native address of the chunk to free.
     * @param size The size of the chunk to deallocate (in bytes).
     */
    private static native void free( long addr, int size );

    /**
     * The native address of the chunk.
     */
    private long m_addr;

    /**
     * The size of the chunk (in bytes).
     */
    private final int m_size;

    static {
        System.loadLibrary( "LCCache" );
    }
}
/* vim:set et sw=4 ts=4: */
