/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.bytebuffer;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteOrder;

/**
 * An <code>LCByteBuffer</code> is a Light Crafts version of Java's
 * <code>ByteBuffer</code> that allows different implementations.
 * <p>
 * Ideally, you'd like to derive from <code>ByteBuffer</code>, but you can't
 * since it contains abstract package-protected methods that you can't
 * implement.  This is totally brain-damaged.
 * <p>
 * Despite not being derived from <code>ByteBuffer</code>, the API is designed
 * to mimick <code>ByteBuffer</code>'s API.
 * <p>
 * Most of the methods are declared to throw {@link IOException} because some
 * implementations may choose to use file I/O as the backing-store for the
 * buffer.  This is annoying, but that's life with checked exceptions.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class LCByteBuffer {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets this buffer's capacity.
     *
     * @return Returns said capacity.
     * @see #limit()
     * @see #limit(int)
     */
    public final int capacity() {
        return m_capacity;
    }

    /**
     * Gets a <code>byte</code> at the buffer's current position and advances
     * the position by 1.
     *
     * @return Returns said <code>byte</code>.
     * @throws BufferUnderflowException if the buffer's current position is not
     * less than its limit.
     * @see #get(int)
     * @see #getUnsignedByte()
     * @see #getUnsignedByte(int)
     */
    public final byte get() throws IOException {
        final int origPos = m_position;
        final byte result = get( m_position );
        //
        // It's possible that m_position might have been altered by the call to
        // an implementation of get(int) above.  Therefore, to guarantee that
        // m_position is updated to the correct value, 1 is added to the
        // original position rather than doing m_position++.
        //
        m_position = origPos + 1;
        return result;
    }

    /**
     * Gets a <code>byte</code> at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to get the <code>byte</code> from.
     * @return Returns said <code>byte</code>.
     * @throws BufferUnderflowException if the given position is not less than
     * the buffer's limit.
     * @see #get()
     * @see #getUnsignedByte()
     * @see #getUnsignedByte(int)
     */
    public abstract byte get( int pos ) throws IOException;

    /**
     * Gets a range of bytes starting at the buffer's current position and
     * advances the position by the number of bytes obtained.
     *
     * @param dest The array to deposit the bytes into.
     * @param offset The element in the array to start depositing.
     * @param length The number of bytes to get.
     * @return Always returns <code>this</code>.
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes remaining in the buffer starting at its
     * current position.
     * @see #getBytes(int)
     * @see #getBytes(int,int)
     * @see #getBytes(byte[],int,int)
     */
    public abstract LCByteBuffer get( byte[] dest, int offset, int length )
        throws IOException;

    /**
     * Gets a range of bytes starting at the buffer's current position and
     * advances the position by the number of bytes obtained.
     *
     * @param length The number of bytes to get.
     * @return Returns a new <code>byte</code> array containing the obtained
     * bytes.
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes remaining in the buffer starting at its
     * current position.
     * @see #get(byte[],int,int)
     * @see #getBytes(int,int)
     * @see #getBytes(byte[],int,int)
     */
    public final byte[] getBytes( int length ) throws IOException {
        final byte[] dest = new byte[ length ];
        get( dest, 0, length );
        return dest;
    }

    /**
     * Gets a range of bytes starting at the given position.  The buffer's
     * current position is not changed.
     *
     * @param pos The position to get the <code>byte</code> from.
     * @param length The number of bytes to get.
     * @return Returns a new <code>byte</code> array containing the obtained
     * bytes.
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes remaining in the buffer starting at the given
     * position.
     * @see #get(byte[],int,int)
     * @see #getBytes(int)
     * @see #getBytes(byte[],int,int)
     */
    public final byte[] getBytes( int pos, int length ) throws IOException {
        final byte[] dest = new byte[ length ];
        getBytes( dest, pos, length );
        return dest;
    }

    /**
     * Gets a range of bytes starting at the given position.  The buffer's
     * current position is not changed.
     *
     * @param dest The array to deposit the bytes into.
     * @param pos The position to get the <code>byte</code> from.
     * @param length The number of bytes to get.
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes remaining in the buffer starting at the given
     * position.
     * @see #get(byte[],int,int)
     * @see #getBytes(int)
     * @see #getBytes(int,int)
     */
    public final void getBytes( byte[] dest, int pos, int length )
        throws IOException
    {
        final int origPos = m_position;
        position( pos );
        get( dest, 0, length );
        m_position = origPos;
    }

    /**
     * Gest a Unicode <code>char</code> at the buffer's current position and
     * advances the position by 2.
     *
     * @return Returns said <code>char</code>.
     * @throws BufferUnderflowException if there are fewer than 2 bytes
     * remaining in the buffer starting at its current position.
     * @see #getChar(int)
     */
    public final char getChar() throws IOException {
        return (char)getShort();
    }

    /**
     * Gets a <code>double</code> at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to get the <code>double</code> from.
     * @return Returns said <code>double</code>.
     * @throws BufferUnderflowException if there are fewer than 2 bytes
     * remaining in the buffer starting at the given position.
     * @see #getChar()
     */
    public final char getChar( int pos ) throws IOException {
        return (char)getShort( pos );
    }

    /**
     * Gets a <code>double</code> at the buffer's current position and advances
     * the position by 8.
     *
     * @return Returns said <code>double</code>.
     * @throws BufferUnderflowException if there are fewer than 8 bytes
     * remaining in the buffer starting at its current position.
     * @see #getDouble(int)
     */
    public final double getDouble() throws IOException {
        final int origPos = m_position;
        final double result = getDouble( m_position );
        //
        // It's possible that m_position might have been altered by the call to
        // an implementation of getDouble(int) above.  Therefore, to guarantee
        // that m_position is updated to the correct value, 8 is added to the
        // original position rather than doing m_position += 8.
        //
        m_position = origPos + 8;
        return result;
    }

    /**
     * Gets a <code>double</code> at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to get the <code>double</code> from.
     * @return Returns said <code>double</code>.
     * @throws BufferUnderflowException if there are fewer than 8 bytes
     * remaining in the buffer starting at the given position.
     * @see #getDouble()
     */
    public abstract double getDouble( int pos ) throws IOException;

    /**
     * Gets a {@link String} that is the same length as the given string
     * starting at the buffer's current position, advances the position by the
     * same length, and compares the two strings for equality.
     *
     * @param s The {@link String} to compare to.
     * @param charsetName The name of a supported character set.
     * @return Returns <code>true</code> only if the two strings are equal.
     * @throws BufferUnderflowException if there are fewer than the number of
     * bytes in the given string remaining in the buffer starting at its
     * current position.
     * @see #getEquals(int,String,String)
     * @see #getString(int,String)
     */
    public final boolean getEquals( String s, String charsetName )
        throws IOException
    {
        return s.equals( getString( s.length(), charsetName ) );
    }

    /**
     * Gets a {@link String} that is the same length as the given string
     * starting at the given position and comares the two strings for equality.
     * The buffer's current position is not changed.
     *
     * @param pos The position to get the {@link String} from.
     * @param s The {@link String} to compare to.
     * @param charsetName The name of a supported character set.
     * @return Returns {@code true} only if the two strings are equal.
     * @throws BufferUnderflowException if there are fewer than the number of
     * bytes in the given string remaining in the buffer starting at its
     * current position.
     * @see #getEquals(String,String)
     * @see #getString(int,int,String)
     */
    public final boolean getEquals( int pos, String s, String charsetName )
        throws IOException
    {
        return s.equals( getString( pos, s.length(), charsetName ) );
    }

    /**
     * Gets a {@code float} at the buffer's current position and advances the
     * position by 4.
     *
     * @return Returns said {@code float}.
     * @throws BufferUnderflowException if there are fewer than 4 bytes
     * remaining in the buffer starting at its current position.
     * @see #getFloat(int)
     */
    public final float getFloat() throws IOException {
        final int origPos = m_position;
        final float result = getFloat( m_position );
        //
        // It's possible that m_position might have been altered by the call to
        // an implementation of getFloat(int) above.  Therefore, to guarantee
        // that m_position is updated to the correct value, 4 is added to the
        // original position rather than doing m_position += 4.
        //
        m_position = origPos + 4;
        return result;
    }

    /**
     * Gets a <code>float</code> at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to get the <code>float</code> from.
     * @return Returns said <code>float</code>.
     * @throws BufferUnderflowException if there are fewer than 4 bytes
     * remaining in the buffer starting at the given position.
     * @see #getFloat()
     */
    public abstract float getFloat( int pos ) throws IOException;

    /**
     * Gets an <code>int</code> at the buffer's current position and advances
     * the position by 4.
     *
     * @return Returns said <code>int</code>.
     * @throws BufferUnderflowException if there are fewer than 4 bytes
     * remaining in the buffer starting at its current position.
     * @see #getInt(int)
     */
    public final int getInt() throws IOException {
        final int origPos = m_position;
        final int result = getInt( m_position );
        //
        // It's possible that m_position might have been altered by the call to
        // an implementation of getInt(int) above.  Therefore, to guarantee
        // that m_position is updated to the correct value, 4 is added to the
        // original position rather than doing m_position += 4.
        //
        m_position = origPos + 4;
        return result;
    }

    /**
     * Gets an <code>int</code> at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to get the <code>int</code> from.
     * @return Returns said <code>int</code>.
     * @throws BufferUnderflowException if there are fewer than 4 bytes
     * remaining in the buffer starting at the given position.
     * @see #getInt()
     */
    public abstract int getInt( int pos ) throws IOException;

    /**
     * Gets a <code>long</code> at the buffer's current position and advances
     * the position by 8.
     *
     * @return Returns said <code>long</code>.
     * @throws BufferUnderflowException if there are fewer than 8 bytes
     * remaining in the buffer starting at its current position.
     * @see #getLong(int)
     */
    public final long getLong() throws IOException {
        final int origPos = m_position;
        final long result = getLong( m_position );
        //
        // It's possible that m_position might have been altered by the call to
        // an implementation of getLong(int) above.  Therefore, to guarantee
        // that m_position is updated to the correct value, 8 is added to the
        // original position rather than doing m_position += 8.
        //
        m_position = origPos + 8;
        return result;
    }

    /**
     * Gets a <code>long</code> at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to get the <code>long</code> from.
     * @return Returns said <code>long</code>.
     * @throws BufferUnderflowException if there are fewer than 8 bytes
     * remaining in the buffer starting at the given position.
     * @see #getLong()
     */
    public abstract long getLong( int pos ) throws IOException;

    /**
     * Gets a <code>short</code> at the buffer's current position and advances
     * the position by 2.
     *
     * @return Returns said <code>short</code>.
     * @throws BufferUnderflowException if there are fewer than 2 bytes
     * remaining in the buffer starting at its current position.
     * @see #getShort(int)
     * @see #getUnsignedShort()
     * @see #getUnsignedShort(int)
     */
    public final short getShort() throws IOException {
        final int origPos = m_position;
        final short result = getShort( m_position );
        //
        // It's possible that m_position might have been altered by the call to
        // an implementation of getShort(int) above.  Therefore, to guarantee
        // that m_position is updated to the correct value, 2 is added to the
        // original position rather than doing m_position += 2.
        //
        m_position = origPos + 2;
        return result;
    }

    /**
     * Gets a <code>short</code> at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to get the <code>short</code> from.
     * @return Returns said <code>short</code>.
     * @throws BufferUnderflowException if there are fewer than 2 bytes
     * remaining in the buffer starting at the given position.
     * @see #getShort()
     * @see #getUnsignedShort()
     * @see #getUnsignedShort(int)
     */
    public abstract short getShort( int pos ) throws IOException;

    /**
     * Gets a {@link String} at the buffer's current position and advances the
     * position by the number of bytes obtained.
     *
     * @param length The number of bytes (not characters) to get.
     * @param charsetName The name of a supported character set.
     * @return Returns said {@link String}.
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes remaining in the buffer starting at its
     * current position.
     * @see #getString(int,int,String)
     */
    public final String getString( int length, String charsetName )
        throws IOException
    {
        return new String( getBytes( length ), charsetName );
    }

    /**
     * Gets a {@link String} at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to get the {@link String} from.
     * @param length The number of bytes (not characters) to get.
     * @param charsetName The name of a supported character set.
     * @return Returns said {@link String}.
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes remaining in the buffer starting at the given
     * position.
     * @see #getString(int,String)
     */
    public final String getString( int pos, int length, String charsetName )
        throws IOException
    {
        return new String( getBytes( pos, length ), charsetName );
    }

    /**
     * Gets an unsigned <code>byte</code> at the buffer's current position and
     * advances the position by 1.
     *
     * @return Return said unsigned <code>byte</code>.
     * @throws BufferUnderflowException if the buffer's current position is not
     * less than its limit.
     * @see #get()
     * @see #get(int)
     * @see #getUnsignedByte(int)
     */
    public final int getUnsignedByte() throws IOException {
        return get() & 0x000000FF;
    }

    /**
     * Gets an unsigned <code>byte</code> at given position.  The buffer's
     * current position is not changed.
     *
     * @param pos The position to get the unsigned <code>byte</code> from.
     * @return Return said unsigned <code>byte</code>.
     * @throws BufferUnderflowException if the given position is not less than
     * its limit.
     * @see #get()
     * @see #get(int)
     * @see #getUnsignedByte()
     */
    public final int getUnsignedByte( int pos ) throws IOException {
        return get( pos ) & 0x000000FF;
    }

    /**
     * Gets an unsigned <code>short</code> at the buffer's current position and
     * advances the position by 2.
     *
     * @return Return said unsigned <code>short</code>.
     * @throws BufferUnderflowException if there are fewer than 2 bytes
     * remaining in the buffer starting at its current position.
     * @see #getShort()
     * @see #getShort(int)
     * @see #getUnsignedShort(int)
     */
    public final int getUnsignedShort() throws IOException {
        return getShort() & 0x0000FFFF;
    }

    /**
     * Gets an unsigned <code>short</code> at given position.  The buffer's
     * current position is not changed.
     *
     * @param pos The position to get the unsigned <code>short</code> from.
     * @return Return said unsigned <code>short</code>.
     * @throws BufferUnderflowException if there are fewer than 2 bytes
     * remaining in the buffer starting at the given position.
     * @see #getShort()
     * @see #getShort(int)
     * @see #getUnsignedShort()
     */
    public final int getUnsignedShort( int pos ) throws IOException {
        return getShort( pos ) & 0x0000FFFF;
    }

    /**
     * Gets ths buffer's initial offset.
     *
     * @return Returns said offset.
     * @see #initialOffset(int)
     */
    public final int initialOffset() {
        return m_initialOffset;
    }

    /**
     * Sets the initial offset.  This offset is added to all absolute positions
     * for reading.
     *
     * @param offset The new offset.
     * @return Returns this <code>LCByteBuffer</code>.
     * @see #initialOffset()
     */
    public LCByteBuffer initialOffset( int offset ) {
        m_initialOffset = offset;
        return this;
    }

    /**
     * Get the buffer's limit.
     *
     * @return Returns said limit.
     * @see #capacity()
     * @see #limit(int)
     */
    public final int limit() {
        return m_limit;
    }

    /**
     * Sets this buffer's limit.
     *
     * @param newLimit The new limit.
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IllegalArgumentException if <code>newLimit</code> is negative or
     * greater than the buffer's capacity.
     * @see #capacity()
     * @see #limit()
     */
    public LCByteBuffer limit( int newLimit ) {
        if ( newLimit < 0 || newLimit > m_capacity )
            throw new IllegalArgumentException();
        m_limit = newLimit;
        if ( m_position > m_limit )
            m_position = m_limit;
        return this;
    }

    /**
     * Get the current {@link ByteOrder}.
     *
     * @return Returns said {@link ByteOrder}.
     */
    public abstract ByteOrder order();

    /**
     * Sets the current {@link ByteOrder}.
     *
     * @param order the new {@link ByteOrder}.
     * @return Returns this <code>LCByteBuffer</code>.
     */
    public abstract LCByteBuffer order( ByteOrder order );

    /**
     * Get the current position.
     *
     * @return Returns said position.
     */
    public final int position() {
        return m_position;
    }

    /**
     * Sets the current position.
     *
     * @param newPosition The new position.
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IllegalArgumentException if <code>newPosition</code> is negative
     * or greater than the buffer's limit.
     */
    public final LCByteBuffer position( int newPosition ) {
        if ( newPosition < 0 || newPosition > m_limit )
            throw new IllegalArgumentException();
        m_position = newPosition;
        return this;
    }

    /**
     * Probes the buffer to determine what byte order the data is in and sets
     * it accordingly.
     *
     * @param shortOffset An offset of a supposed unsigned <code>short</code>.
     * @return Returns the original {@link ByteOrder}.
     * @throws BufferUnderflowException if there are fewer than 2 bytes
     * remaining in the buffer starting at its current position.
     */
    public ByteOrder probeOrder( int shortOffset ) throws IOException {
        final ByteOrder origOrder = order();
        //
        // We use a heuristic of extracting a short both using big and little
        // endian byte orders.  The assumption is that reading the bytes in the
        // right order willl yield a smaller number than the wrong order, e.g.
        // 0x0100 is 1 when read as little endian is 1 but is 256 when read as
        // big endian.
        //
        order( ByteOrder.BIG_ENDIAN );
        final int nBig = getUnsignedShort( shortOffset );
        order( ByteOrder.LITTLE_ENDIAN );
        final int nLittle = getUnsignedShort( shortOffset );
        if ( nLittle > nBig )
            order( ByteOrder.BIG_ENDIAN );
        return origOrder;
    }

    /**
     * Puts a <code>byte</code> at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to put the byte at.
     * @param value The byte to put.
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IndexOutOfBoundsException if <tt>pos</tt> is negative or not
     * smaller than the buffer's limit.
     * @see #put(byte)
     */
    public abstract LCByteBuffer put( int pos, byte value ) throws IOException;

    /**
     * Puts a <code>byte</code> at the buffer's current position and advances
     * the position by 1.
     *
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IndexOutOfBoundsException if the current position is not smaller
     * than the buffer's limit.
     * @see #put(int,byte)
     */
    public final LCByteBuffer put( byte value ) throws IOException {
        final int origPos = m_position;
        put( m_position, value );
        //
        // It's possible that m_position might have been altered by the call to
        // an implementation of getInt(int) above.  Therefore, to guarantee
        // that m_position is updated to the correct value, 1 is added to the
        // original position rather than doing m_position += 1.
        //
        m_position = origPos + 1;
        return this;
    }

    /**
     * Puts a <code>double</code> at the buffer's current position and advances
     * the position by 8.
     *
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IndexOutOfBoundsException if the current position is not smaller
     * than the buffer's limit - 7.
     * @see #putDouble(int,double)
     */
    public final LCByteBuffer putDouble( double value ) throws IOException {
        final int origPos = m_position;
        putDouble( m_position, value );
        //
        // It's possible that m_position might have been altered by the call to
        // an implementation of getInt(int) above.  Therefore, to guarantee
        // that m_position is updated to the correct value, 8 is added to the
        // original position rather than doing m_position += 8.
        //
        m_position = origPos + 8;
        return this;
    }

    /**
     * Put a <code>double</code> at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to put the double at.
     * @param value The double to put.
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IndexOutOfBoundsException if <tt>pos</tt> is negative or not
     * smaller than the buffer's limit - 7.
     * @see #putDouble(double)
     */
    public abstract LCByteBuffer putDouble( int pos, double value )
        throws IOException;

    /**
     * Puts a <code>float</code> at the buffer's current position and advances
     * the position by 4.
     *
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IndexOutOfBoundsException if the current position is not smaller
     * than the buffer's limit - 3.
     * @see #putFloat(int,float)
     */
    public final LCByteBuffer putFloat( float value ) throws IOException {
        final int origPos = m_position;
        putFloat( m_position, value );
        //
        // It's possible that m_position might have been altered by the call to
        // an implementation of getInt(int) above.  Therefore, to guarantee
        // that m_position is updated to the correct value, 4 is added to the
        // original position rather than doing m_position += 4.
        //
        m_position = origPos + 4;
        return this;
    }

    /**
     * Puts a <code>float</code> at the given position.  The buffer's current
     * position it not changed.
     *
     * @param pos The position to put the float at.
     * @param value The float to put.
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IndexOutOfBoundsException if <tt>pos</tt> is negative or not
     * smaller than the buffer's limit.
     * @see #putFloat(float)
     */
    public abstract LCByteBuffer putFloat( int pos, float value )
        throws IOException;

    /**
     * Puts an <code>int</code> at the buffer's current position and advances
     * the position by 4.
     *
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IndexOutOfBoundsException if the current position is not smaller
     * than the buffer's limit - 3.
     * @see #putInt(int,int)
     */
    public final LCByteBuffer putInt( int value ) throws IOException {
        final int origPos = m_position;
        putInt( m_position, value );
        //
        // It's possible that m_position might have been altered by the call to
        // an implementation of getInt(int) above.  Therefore, to guarantee
        // that m_position is updated to the correct value, 4 is added to the
        // original position rather than doing m_position += 4.
        //
        m_position = origPos + 4;
        return this;
    }

    /**
     * Puts an <code>int</code> at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to put the integer at.
     * @param value The integer to put.
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IndexOutOfBoundsException if <tt>pos</tt> is negative or not
     * smaller than the buffer's limit - 3.
     * @see #putInt(int)
     */
    public abstract LCByteBuffer putInt( int pos, int value )
        throws IOException;

    /**
     * Puts a <code>long</code> at the buffer's current position and advances
     * the position by 8.
     *
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IndexOutOfBoundsException if the current position is not smaller
     * than the buffer's limit - 7.
     * @see #putLong(int,long)
     */
    public final LCByteBuffer putLong( long value ) throws IOException {
        final int origPos = m_position;
        putLong( m_position, value );
        //
        // It's possible that m_position might have been altered by the call to
        // an implementation of getInt(int) above.  Therefore, to guarantee
        // that m_position is updated to the correct value, 8 is added to the
        // original position rather than doing m_position += 8.
        //
        m_position = origPos + 8;
        return this;
    }

    /**
     * Puts a <code>long</code> at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to put the long at.
     * @param value The long to put.
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IndexOutOfBoundsException if <tt>pos</tt> is negative or not
     * smaller than the buffer's limit - 7.
     * @see #putLong(long)
     */
    public abstract LCByteBuffer putLong( int pos, long value )
        throws IOException;

    /**
     * Puts a <code>short</code> at the buffer's current position and advances
     * the position by 2.
     *
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IndexOutOfBoundsException if the current position is not smaller
     * than the buffer's limit - 1.
     * @see #putShort(int,short)
     */
    public final LCByteBuffer putShort( short value ) throws IOException {
        final int origPos = m_position;
        putShort( m_position, value );
        //
        // It's possible that m_position might have been altered by the call to
        // an implementation of getInt(int) above.  Therefore, to guarantee
        // that m_position is updated to the correct value, 2 is added to the
        // original position rather than doing m_position += 2.
        //
        m_position = origPos + 2;
        return this;
    }

    /**
     * Puts a <code>short</code> at the given position.  The buffer's current
     * position is not changed.
     *
     * @param pos The position to put the short at.
     * @param value The short to put.
     * @return Returns this <code>LCByteBuffer</code>.
     * @throws IndexOutOfBoundsException if <tt>pos</tt> is negative or not
     * smaller than the buffer's limit - 1.
     * @see #putShort(short)
     */
    public abstract LCByteBuffer putShort( int pos, short value )
        throws IOException;

    /**
     * Gets the number of bytes remaining between the current position and the
     * buffer's limit.
     *
     * @return Returns said number of bytes.
     */
    public final int remaining() {
        return m_limit - m_position;
    }

    /**
     * Skip a specified number of bytes, i.e., advance the buffer's position.
     *
     * @param count The number of bytes to skip.
     * @throws IllegalArgumentException if the buffer's current position plus
     * the count is either negative or greater than the buffer's limit.
     * @return Returns this <code>LCByteBuffer</code>.
     */
    public LCByteBuffer skipBytes( int count ) {
        return position( m_position + count );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct an <code>LCByteBuffer</code>.
     *
     * @param capacity The size of the buffer.
     */
    protected LCByteBuffer( int capacity ) {
        m_capacity = m_limit = capacity;
        m_position = 0;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The buffer's capacity.
     */
    private final int m_capacity;

    /**
     * This offset is added to all absolute positions for reading.
     */
    private int m_initialOffset;

    /**
     * The buffer's limit.
     */
    private int m_limit;

    /**
     * The current position.
     */
    private int m_position;
}
/* vim:set et sw=4 ts=4: */
