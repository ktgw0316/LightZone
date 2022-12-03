/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.cache;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A <code>DirectFileCacheStore</code> is-a {@link CacheStore} that uses a
 * {@link File} directly for its backing store.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class DirectFileCacheStore implements CacheStore {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>DirectFileCacheStore</code>.
     *
     * @param cacheFile The {@link File} to use for the cache.
     */
    public DirectFileCacheStore( File cacheFile ) throws IOException {
        cacheFile.deleteOnExit();
        m_file = new RandomAccessFile( cacheFile, "rw" );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void clear() throws IOException {
        m_file.setLength( 0 );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void dispose() throws IOException {
        if ( m_file != null ) {
            m_file.close();
            m_file = null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public ByteBuffer getByteBuffer( int size ) {
        final ByteBuffer buf = ByteBuffer.allocate( size );
        buf.order( ByteOrder.nativeOrder() );
        return buf;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized long getSize() throws IOException {
        return m_file.length();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void readFromStore( long pos, ByteBuffer buf )
        throws IOException
    {
        m_file.seek( pos );
        if ( buf.hasArray() )
            m_file.read( buf.array(), 0, buf.limit() );
        else
            m_file.getChannel().read( buf );
        buf.position( 0 );
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void writeToStore( long pos, ByteBuffer buf )
        throws IOException
    {
        buf.position( 0 );
        m_file.seek( pos );
        if ( buf.hasArray() )
            m_file.write( buf.array(), 0, buf.limit() );
        else
            m_file.getChannel().write( buf );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The {@link File} to use as the backing store.
     */
    private RandomAccessFile m_file;
}
/* vim:set et sw=4 ts=4: */
