/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.bytebuffer;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

import com.lightcrafts.utils.CloseableManager;

/**
 * An <code>LCReopenableMappedByteBuffer</code> is-an {@link LCByteBuffer} that
 * maps all or part of a file into memory.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see LCMappedByteBuffer
 */
public final class LCReopenableMappedByteBuffer extends LCByteBuffer
    implements Closeable {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>LCReopenableMappedByteBuffer</code> mapping an entire
     * file read-only.
     *
     * @param file The {@link File} to map read-only.
     */
    public LCReopenableMappedByteBuffer( File file ) {
        this( file, FileChannel.MapMode.READ_ONLY );
    }

    /**
     * Construct an <code>LCReopenableMappedByteBuffer</code> mapping an entire
     * file.
     *
     * @param file The {@link File} to map.
     * @param mode The {@link FileChannel.MapMode} to use.
     */
    public LCReopenableMappedByteBuffer( File file, FileChannel.MapMode mode ) {
        this( file, 0, file.length(), mode, null );
    }

    /**
     * Construct an <code>LCReopenableMappedByteBuffer</code> mapping an entire
     * file read-only.
     *
     * @param file The {@link File} to map read-only.
     * @param closeableManager The {@link CloseableManager} to use or
     * <code>null</code> if none.
     */
    public LCReopenableMappedByteBuffer( File file,
                                         CloseableManager closeableManager ) {
        this(
            file, 0, file.length(), FileChannel.MapMode.READ_ONLY,
            closeableManager
        );
    }

    /**
     * Construct an <code>LCReopenableMappedByteBuffer</code>.
     *
     * @param position The position within the file at which the mapped region
     * is to start.
     * @param size The size of the region to be mapped.
     * @param mode The {@link FileChannel.MapMode} to use.
     */
    public LCReopenableMappedByteBuffer( File file, long position, long size,
                                         FileChannel.MapMode mode ) {
        this( file, position, size, mode, null );
    }

    /**
     * Construct an <code>LCReopenableMappedByteBuffer</code>.
     *
     * @param position The position within the file at which the mapped region
     * is to start.
     * @param size The size of the region to be mapped.
     * @param mode The {@link FileChannel.MapMode} to use.
     * @param closeableManager The {@link CloseableManager} to use or
     * <code>null</code> if none.
     */
    public LCReopenableMappedByteBuffer( File file, long position, long size,
                                         FileChannel.MapMode mode,
                                         CloseableManager closeableManager ) {
        super( (int)size );
        m_closeableManager = closeableManager;
        m_file = file;
        m_mode = mode;
        m_order = ByteOrder.BIG_ENDIAN;
        m_position = position;
        m_size = size;
    }

    /**
     * Closes this <code>LCMapperByteBuffer</code>.  Note that it can be closed
     * at any time to conserve resources.  It will automatically reopen itself
     * if necessary.
     */
    @Override
    public synchronized void close() {
        ByteBufferUtil.clean(m_buf);
        m_buf = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte get( int pos ) throws IOException {
        synchronized ( this ) {
            return getBuf().get( initialOffset() + pos );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LCByteBuffer get( byte[] dest, int offset, int length )
        throws IOException
    {
        final int pos = position();
        synchronized ( this ) {
            final ByteBuffer buf = getBuf();
            buf.position( initialOffset() + pos );
            buf.get( dest, offset, length );
        }
        position( pos + length );
        return this;
    }

    /**
     * Gets the {@link ByteBuffer} in use.
     *
     * @return Returns said {@link ByteBuffer}.
     */
    public ByteBuffer getByteBuffer() {
        return m_buf;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble( int pos ) throws IOException {
        synchronized ( this ) {
            return getBuf().getDouble( initialOffset() + pos );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFloat( int pos ) throws IOException {
        synchronized ( this ) {
            return getBuf().getFloat( initialOffset() + pos );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt( int pos ) throws IOException {
        synchronized ( this ) {
            return getBuf().getInt( initialOffset() + pos );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong( int pos ) throws IOException {
        synchronized ( this ) {
            return getBuf().getLong( initialOffset() + pos );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getShort( int pos ) throws IOException {
        synchronized ( this ) {
            return getBuf().getShort( initialOffset() + pos );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ByteOrder order() {
        return m_order;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized LCByteBuffer order( ByteOrder order ) {
        m_order = order;
        if ( m_buf != null )
            m_buf.order( order );
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LCReopenableMappedByteBuffer put( int pos, byte value )
        throws IOException
    {
        synchronized ( this ) {
            getBuf().put( initialOffset() + pos, value );
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LCReopenableMappedByteBuffer putDouble( int pos, double value )
        throws IOException
    {
        synchronized ( this ) {
            getBuf().putDouble( initialOffset() + pos, value );
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LCReopenableMappedByteBuffer putFloat( int pos, float value )
        throws IOException
    {
        synchronized ( this ) {
            getBuf().putFloat( initialOffset() + pos, value );
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LCReopenableMappedByteBuffer putInt( int pos, int value )
        throws IOException
    {
        synchronized ( this ) {
            getBuf().putInt( initialOffset() + pos, value );
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LCReopenableMappedByteBuffer putLong( int pos, long value )
        throws IOException
    {
        synchronized ( this ) {
            getBuf().putLong( initialOffset() + pos, value );
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LCReopenableMappedByteBuffer putShort( int pos, short value )
        throws IOException
    {
        synchronized ( this ) {
            getBuf().putShort( initialOffset() + pos, value );
        }
        return this;
    }

    ////////// protected //////////////////////////////////////////////////////

    @Override
    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Gets the {@link ByteBuffer} to use.  This method and methods that use
     * the {@link ByteBuffer} must all be within the same
     * <code>synchronized</code> block.
     *
     * @return Returns said {@link ByteBuffer}.
     */
    private ByteBuffer getBuf() throws IOException {
        if ( m_buf == null ) {
            if ( m_closeableManager != null )
                m_closeableManager.manage( this );
            m_buf = ByteBufferUtil.map( m_file, m_position, m_size, m_mode );
            m_buf.order( m_order );
        }
        return m_buf;
    }

    private ByteBuffer m_buf;
    private final CloseableManager m_closeableManager;
    private final File m_file;
    private final FileChannel.MapMode m_mode;
    private ByteOrder m_order;
    private final long m_position;
    private final long m_size;
}
/* vim:set et sw=4 ts=4: */
