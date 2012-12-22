/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.bytebuffer;

import java.nio.ByteOrder;
import java.nio.ByteBuffer;

/**
 * An <code>ArrayByteBuffer</code> is-an {@link LCByteBuffer} that uses either
 * an ordinary <code>byte</code> array or a {@link ByteBuffer} for its backing-
 * store.
 * <p>
 * This class is used to adapt a {@link ByteBuffer} to an {@link LCByteBuffer}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class ArrayByteBuffer extends LCByteBuffer {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>ArrayByteBuffer</code>.
     *
     * @param array The array to use as the backing-store.
     */
    public ArrayByteBuffer( byte[] array ) {
        this( ByteBuffer.wrap( array ) );
    }

    /**
     * Construct an <code>ArrayByteBuffer</code>.
     *
     * @param buf The {@link ByteBuffer} to use as the backing-store.
     */
    public ArrayByteBuffer( ByteBuffer buf ) {
        super( buf.capacity() );
        m_buf = buf;
    }

    /**
     * {@inheritDoc}
     */
    public byte get( int pos ) {
        return m_buf.get( initialOffset() + pos );
    }

    /**
     * {@inheritDoc}
     */
    public LCByteBuffer get( byte[] dest, int offset, int length ) {
        final int pos = position();
        m_buf.position( initialOffset() + pos );
        m_buf.get( dest, offset, length );
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
    public double getDouble( int pos ) {
        return m_buf.getDouble( initialOffset() + pos );
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat( int pos ) {
        return m_buf.getFloat( initialOffset() + pos );
    }

    /**
     * {@inheritDoc}
     */
    public int getInt( int pos ) {
        return m_buf.getInt( initialOffset() + pos );
    }

    /**
     * {@inheritDoc}
     */
    public long getLong( int pos ) {
        return m_buf.getLong( initialOffset() + pos );
    }

    /**
     * {@inheritDoc}
     */
    public short getShort( int pos ) {
        return m_buf.getShort( initialOffset() + pos );
    }

    /**
     * {@inheritDoc}
     */
    public ArrayByteBuffer limit( int newLimit ) {
        super.limit( newLimit );
        m_buf.limit( newLimit );
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ByteOrder order() {
        return m_buf.order();
    }

    /**
     * {@inheritDoc}
     */
    public ArrayByteBuffer order( ByteOrder order ) {
        m_buf.order( order );
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayByteBuffer put( int pos, byte b ) {
        m_buf.put( initialOffset() + pos, b );
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayByteBuffer putDouble( int pos, double value ) {
        m_buf.putDouble( initialOffset() + pos, value );
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayByteBuffer putFloat( int pos, float value ) {
        m_buf.putFloat( initialOffset() + pos, value );
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayByteBuffer putInt( int pos, int value ) {
        m_buf.putInt( initialOffset() + pos, value );
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayByteBuffer putLong( int pos, long value ) {
        m_buf.putLong( initialOffset() + pos, value );
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public ArrayByteBuffer putShort( int pos, short value ) {
        m_buf.putShort( initialOffset() + pos, value );
        return this;
    }

    ////////// private ////////////////////////////////////////////////////////

    private final ByteBuffer m_buf;
}
/* vim:set et sw=4 ts=4: */
