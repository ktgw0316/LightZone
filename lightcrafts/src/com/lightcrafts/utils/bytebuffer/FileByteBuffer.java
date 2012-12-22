/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.bytebuffer;

import java.io.Closeable;
import java.io.File;
import java.io.RandomAccessFile;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;

import com.lightcrafts.utils.CloseableManager;

/**
 * A <code>FileByteBuffer</code> is-an {@link LCByteBuffer} that uses a
 * {@link File} for its backing-store.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class FileByteBuffer extends LCByteBuffer implements Closeable {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>FileByteBuffer</code>.
     *
     * @param file The {@link File} to use for the backing-store.
     * @param closeableManager The {@link CloseableManager} to use or
     * <code>null</code> if none.
     */
    public FileByteBuffer( File file, CloseableManager closeableManager ) {
        super( (int)file.length() );
        m_file = file;
        m_order = ByteOrder.BIG_ENDIAN;
        m_closeableManager = closeableManager;
    }

    /**
     * Close a <code>FileByteBuffer</code>.  Note that it can be closed at any
     * time to conserve resources.  It will automatically reopen itself if
     * necessary.
     */
    public synchronized void close() throws IOException {
        if ( m_raf != null ) {
            try {
                m_raf.close();
            }
            finally {
                m_raf = null;
            }
        }
    }

    /**
     * Dispose of a <code>FileByteBuffer</code>.
     */
    public void dispose() throws IOException {
        close();
    }

    /**
     * {@inheritDoc}
     */
    public byte get( int pos ) throws IOException {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 1 > limit() )
            throw new BufferUnderflowException();
        synchronized ( this ) {
            return getRAF( offsetPos ).readByte();
        }
    }

    /**
     * {@inheritDoc}
     */
    public LCByteBuffer get( byte[] dest, int offset, int length )
        throws IOException
    {
        final int pos = position();
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + length > limit() )
            throw new BufferUnderflowException();
        int totalBytesRead = 0;
        synchronized ( this ) {
            final RandomAccessFile raf = getRAF( offsetPos );
            while ( true ) {
                final int bytesRead = raf.read( dest, offset, length );
                if ( bytesRead == -1 )
                    break;
                totalBytesRead += bytesRead;
                if ( bytesRead == length )
                    break;
                offset += bytesRead;
                length -= bytesRead;
            }
        }
        position( pos + totalBytesRead );
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble( int pos ) throws IOException {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 8 > limit() )
            throw new BufferUnderflowException();
        long value;
        synchronized ( this ) {
            value = getRAF( offsetPos ).readLong();
        }
        if ( order() == ByteOrder.LITTLE_ENDIAN )
            value = Long.reverseBytes( value );
        return Double.longBitsToDouble( value );
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat( int pos ) throws IOException {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 4 > limit() )
            throw new BufferUnderflowException();
        int value;
        synchronized ( this ) {
            value = getRAF( offsetPos ).readInt();
        }
        if ( order() == ByteOrder.LITTLE_ENDIAN )
            value = Integer.reverseBytes( value );
        return Float.intBitsToFloat( value );
    }

    /**
     * {@inheritDoc}
     */
    public int getInt( int pos ) throws IOException {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 4 > limit() )
            throw new BufferUnderflowException();
        final int value;
        synchronized ( this ) {
            value = getRAF( offsetPos ).readInt();
        }
        return  order() == ByteOrder.BIG_ENDIAN ?
                value : Integer.reverseBytes( value );
    }

    /**
     * {@inheritDoc}
     */
    public long getLong( int pos ) throws IOException {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 8 > limit() )
            throw new BufferUnderflowException();
        final long value;
        synchronized ( this ) {
            value = getRAF( offsetPos ).readLong();
        }
        return  order() == ByteOrder.BIG_ENDIAN ?
                value : Long.reverseBytes( value );
    }

    /**
     * {@inheritDoc}
     */
    public short getShort( int pos ) throws IOException {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 2 > limit() )
            throw new BufferUnderflowException();
        final short value;
        synchronized ( this ) {
            value = getRAF( offsetPos ).readShort();
        }
        return  order() == ByteOrder.BIG_ENDIAN ?
                value : Short.reverseBytes( value );
    }

    /**
     * {@inheritDoc}
     */
    public ByteOrder order() {
        return m_order;
    }

    /**
     * {@inheritDoc}
     */
    public LCByteBuffer order( ByteOrder order ) {
        m_order = order;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public FileByteBuffer put( int pos, byte value ) throws IOException {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 1 > limit() )
            throw new BufferOverflowException();
        synchronized ( this ) {
            getRAF( offsetPos ).writeByte( value );
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public FileByteBuffer putDouble( int pos, double value )
        throws IOException
    {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 8 > limit() )
            throw new BufferOverflowException();
        long valueAsLong = Double.doubleToLongBits( value );
        if ( order() == ByteOrder.LITTLE_ENDIAN )
            valueAsLong = Long.reverseBytes( valueAsLong );
        synchronized ( this ) {
            getRAF( offsetPos ).writeLong( valueAsLong );
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public FileByteBuffer putFloat( int pos, float value ) throws IOException {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 4 > limit() )
            throw new BufferOverflowException();
        int valueAsInt = Float.floatToIntBits( value );
        if ( order() == ByteOrder.LITTLE_ENDIAN )
            valueAsInt = Integer.reverseBytes( valueAsInt );
        synchronized ( this ) {
            getRAF( offsetPos ).writeInt( valueAsInt );
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public FileByteBuffer putInt( int pos, int value ) throws IOException {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 4 > limit() )
            throw new BufferOverflowException();
        if ( order() == ByteOrder.LITTLE_ENDIAN )
            value = Integer.reverseBytes( value );
        synchronized ( this ) {
            getRAF( offsetPos ).writeInt( value );
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public FileByteBuffer putLong( int pos, long value ) throws IOException {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 8 > limit() )
            throw new BufferOverflowException();
        if ( order() == ByteOrder.LITTLE_ENDIAN )
            value = Long.reverseBytes( value );
        synchronized ( this ) {
            getRAF( offsetPos ).writeLong( value );
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public FileByteBuffer putShort( int pos, short value ) throws IOException {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 2 > limit() )
            throw new BufferOverflowException();
        if ( order() == ByteOrder.LITTLE_ENDIAN )
            value = Short.reverseBytes( value );
        synchronized ( this ) {
            getRAF( offsetPos ).writeShort( value );
        }
        return this;
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Finalize a <code>FileByteBuffer</code>.
     */
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Gets the {@link RandomAccessFile} to use.  This method and methods that
     * use the {@link RandomAccessFile} must all be within the same
     * <code>synchronized</code> block.
     *
     * @param pos The position to seek to.
     * @return Returns said {@link RandomAccessFile}.
     */
    private RandomAccessFile getRAF( long pos ) throws IOException {
        if ( m_raf == null ) {
            if ( m_closeableManager != null )
                m_closeableManager.manage( this );
            m_raf = new RandomAccessFile( m_file, "r" );
        }
        m_raf.seek( pos );
        return m_raf;
    }

    /**
     * The {@link CloseableManager} to use, if any.
     */
    private final CloseableManager m_closeableManager;

    /**
     * The {@link File} to use.
     */
    private final File m_file;

    /**
     * The {@link ByteOrder} to use to interpret bytes in the file.
     */
    private ByteOrder m_order;

    /**
     * The {@link RandomAccessFile} to use.
     */
    private RandomAccessFile m_raf;
}
/* vim:set et sw=4 ts=4: */
