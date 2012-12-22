/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.bytebuffer;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import com.lightcrafts.utils.CloseableManager;

/**
 * A <code>SoftChunkyFileByteBuffer</code> is-a {@link FileByteBuffer} that
 * reads in chunks of a file at once and caches the chunks to reduce file I/O.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class SoftChunkyFileByteBuffer extends FileByteBuffer {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>SoftChunkyFileByteBuffer</code>.
     *
     * @param file The {@link File} to use for the backing-store.
     * @param closeableManager The {@link CloseableManager} to use or
     * <code>null</code> if none.
     */
    public SoftChunkyFileByteBuffer( File file,
                                     CloseableManager closeableManager ) {
        super( file, closeableManager );
        //noinspection unchecked
        m_chunk = new SoftReference[ (int)(file.length() / CHUNK_SIZE + 1) ];
    }

    /**
     * {@inheritDoc}
     */
    public byte get( int pos ) throws IOException {
        final int offsetPos = initialOffset() + pos;
        if ( offsetPos + 1 > limit() )
            throw new BufferUnderflowException();
        return getChunkForPos( offsetPos ).get( offsetPos % CHUNK_SIZE );
    }

    /**
     * {@inheritDoc}
     */
    public LCByteBuffer get( byte[] dest, int offset, int length )
        throws IOException
    {
        final int pos = position();
        getContiguousBytes( initialOffset() + pos, dest, offset, length );
        position( pos + length );
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public double getDouble( int pos ) throws IOException {
        return Double.longBitsToDouble(
            get248Bytes( initialOffset() + pos, 8 )
        );
    }

    /**
     * {@inheritDoc}
     */
    public float getFloat( int pos ) throws IOException {
        return Float.intBitsToFloat(
            (int)get248Bytes( initialOffset() + pos, 4 )
        );
    }

    /**
     * {@inheritDoc}
     */
    public int getInt( int pos ) throws IOException {
        return (int)get248Bytes( initialOffset() + pos, 4 );
    }

    /**
     * {@inheritDoc}
     */
    public long getLong( int pos ) throws IOException {
        return get248Bytes( initialOffset() + pos, 8 );
    }

    /**
     * {@inheritDoc}
     */
    public short getShort( int pos ) throws IOException {
        return (short)get248Bytes( initialOffset() + pos, 2 );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Gets 2, 4, or 8 bytes.  This is a special case for getting bytes for the
     * numeric types via {@link #getDouble(int)}, {@link #getFloat(int)},
     * {@link #getInt(int)}, {@link #getLong(int)}, or {@link #getShort(int)}.
     *
     * @param pos The position to start getting bytes from.
     * @param length The number of bytes to get.
     * @return Returns the bytes as a <code>long</code>.
     * @see #getContiguousBytes(int,byte[],int,int)
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes remaining in the buffer starting at the given
     * position.
     */
    private long get248Bytes( int pos, int length ) throws IOException {
        final int endPos = pos + length;
        if ( endPos > limit() )
            throw new BufferUnderflowException();
        final int lastPos = endPos - 1;
        final ByteBuffer chunkI = getChunkForPos( pos );
        final ByteBuffer chunkJ = getChunkForPos( lastPos );
        final int modI = pos % CHUNK_SIZE;
        if ( chunkI == chunkJ ) {
            //
            // Easy case: all wanted bytes are in the same chunk.
            //
            return get248Bytes( chunkI, modI, length );
        }
        //
        // Hard case: wanted bytes span 2 chunks.
        // Create a temporary byte array to serve as a mini-chunk so the bytes
        // are contiguous.
        //
        final byte[] temp = new byte[ length ];
        final int firstLength = CHUNK_SIZE - modI;
        final int lastLength = lastPos % CHUNK_SIZE + 1;
        ByteBufferUtil.getBytes( chunkI, modI, temp, 0, firstLength );
        ByteBufferUtil.getBytes(
            chunkJ, 0, temp, length - lastLength, lastLength
        );
        return get248Bytes( ByteBuffer.wrap( temp ), 0, length );
    }

    /**
     * Gets 2, 4, or 8 bytes from the given {@link ByteBuffer}.  This is only
     * used to factor out common code from {@link #get248Bytes(int,int)}.
     *
     * @param buf The {@link ByteBuffer} to get the bytes from.
     * @param pos The position to start getting bytes from.
     * @param length The number of bytes to get; must be 2, 4, or 8.
     * @return Returns the bytes as a <code>long</code>.
     * @throws BufferUnderflowException if there are fewer than 2, 4, or 8
     * bytes remaining the buffer starting at the given position.
     * @throws IllegalArgumentException if <code>length</code> is not 2, 4 or
     * 8.
     */
    private long get248Bytes( ByteBuffer buf, int pos, int length ) {
        buf.order( order() );
        try {
            switch ( length ) {
                case 2:
                    return buf.getShort( pos );
                case 4:
                    return buf.getInt( pos );
                case 8:
                    return buf.getLong( pos );
                default:
                    throw new IllegalArgumentException(
                        "length (" + length + ") must be 2, 4, or 8"
                    );
            }
        }
        catch ( IndexOutOfBoundsException e ) {
            //
            // Morph an IndexOutOfBoundsException to a BufferUnderflowException
            // for consistency.
            //
            final BufferUnderflowException bue = new BufferUnderflowException();
            bue.initCause( e );
            throw bue;
        }
    }

    /**
     * Gets the given chunk reading it into memory from disk if necessary.
     *
     * @param c The chunk index.
     * @return Returns said chunk.
     * @throws IllegalArgumentException if <code>c</code> is negative or
     * greater than the number of chunks comprising the file.
     */
    private synchronized ByteBuffer getChunk( int c ) throws IOException {
        final Reference<ByteBuffer> ref = m_chunk[c];
        if ( ref != null ) {
            final ByteBuffer buf = ref.get();
            if ( buf != null )
                return buf;
        }

        final ByteBuffer buf = ByteBuffer.allocate( CHUNK_SIZE );
        final int origPos = position();
        final int chunkPos = c * CHUNK_SIZE;
        position( chunkPos );
        final int length = Math.min( CHUNK_SIZE, limit() - chunkPos );
        super.get( buf.array(), 0, length );
        position( origPos );

        m_chunk[c] = new SoftReference<ByteBuffer>( buf );
        return buf;
    }

    /**
     * Gets the chunk for the given position, i.e., makes sure it's been read
     * into memory from disk.
     *
     * @param pos The position to get the chunk for.
     * @return Returns the chunk index for the given position.
     * @throws IllegalArgumentException if <code>pos</code> is negative or
     * greater than the buffer's limit.
     */
    private ByteBuffer getChunkForPos( int pos ) throws IOException {
        return getChunk( pos / CHUNK_SIZE );
    }

    /**
     * Gets a contiguous range of bytes.  Note that for getting just 2, 4, or 8
     * bytes (for reading numeric types), {@link #get248Bytes(int,int)} is used
     * instead.
     *
     * @param pos The position to start getting bytes from.
     * @param dest The array to deposit the bytes into.
     * @param offset The element in the array to start depositing.
     * @param length The number of bytes to get.
     * @return Returns a <code>byte</code> array of the contiguous bytes.
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes remaining in the buffer starting at the given
     * position.
     */
    private byte[] getContiguousBytes( int pos, byte[] dest, int offset,
                                       int length ) throws IOException {
        final int endPos = pos + length;
        if ( endPos > limit() )
            throw new BufferUnderflowException();
        final int lastPos = endPos - 1;
        final ByteBuffer chunkI = getChunkForPos( pos );
        final ByteBuffer chunkJ = getChunkForPos( lastPos );
        final int modI = pos % CHUNK_SIZE;
        if ( chunkI == chunkJ ) {
            //
            // Easy case: all wanted bytes are in the same chunk.
            //
            ByteBufferUtil.getBytes( chunkI, modI, dest, offset, length );
        } else {
            //
            // Hard case: wanted bytes span 2 (or more) chunks.
            // First, copy the first and last partial chunks.
            //
            final int firstLength = CHUNK_SIZE - modI;
            final int lastLength = lastPos % CHUNK_SIZE + 1;
            ByteBufferUtil.getBytes( chunkI, modI, dest, offset, firstLength );
            ByteBufferUtil.getBytes(
                chunkJ, 0, dest, length - lastLength, lastLength
            );
            //
            // Then copy whole chunks in between (if any).
            //
            offset += firstLength;
            final int j = lastPos / CHUNK_SIZE;
            for ( int i = pos / CHUNK_SIZE + 1; i < j; ++i ) {
                final ByteBuffer chunk = getChunk( i );
                ByteBufferUtil.getBytes( chunk, 0, dest, offset, CHUNK_SIZE );
                offset += CHUNK_SIZE;
            }
        }
        return dest;
    }

    /**
     * The size of each chunk.  Note that if it's too small, the bad case where
     * the bytes wanted are not within the same chunk happens more often.
     * @see #get248Bytes(int,int)
     * @see #getContiguousBytes(int,byte[],int,int)
     */
    private static final int CHUNK_SIZE = 8 * 1024;

    /**
     * The chunks.  Each chunk is a {@link SoftReference} to a
     * {@link ByteBuffer} with capacity {@link #CHUNK_SIZE}.
     */
    private final Reference<ByteBuffer>[] m_chunk;
}
/* vim:set et sw=4 ts=4: */
