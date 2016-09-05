/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2016 Masahiro Kitagawa */

package com.lightcrafts.utils.bytebuffer;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.channels.FileChannel;

/**
 * A <code>ByteBufferUtil</code> is a set of utility functions for the
 * {@link ByteBuffer} class.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ByteBufferUtil {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Dump the given {@link ByteBuffer}'s bytes to a file.
     *
     * @param buf The {@link ByteBuffer} to dump.
     * @param fileName The name of the file to dump to.
     */
    public static void dumpToFile( ByteBuffer buf, String fileName ) {
        try {
            final FileOutputStream fos = new FileOutputStream( fileName );
            fos.write( buf.array() );
            fos.close();
        }
        catch ( Exception e ) {
            e.printStackTrace();
            System.exit( -1 );
        }
    }

    /**
     * This is a relative bulk get method for {@link ByteBuffer} that returns
     * the array of bytes gotten.  It is a convenience method so the array
     * doesn't have to be declared beforehand.
     *
     * @param buf The {@link ByteBuffer} to get bytes from.
     * @param length The number of bytes to be copied.
     * @return Returns an array of the bytes gotten.
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes remaining in the buffer.
     */
    public static byte[] getBytes( ByteBuffer buf, int length ) {
        final byte[] dst = new byte[ length ];
        buf.get( dst, 0, length );
        return dst;
    }

    /**
     * This is an absolute bulk get method for {@link ByteBuffer} because it
     * doesn't have one (but should!).  Being absolute, the
     * {@link ByteBuffer}'s position doesn't change.
     *
     * @param buf The {@link ByteBuffer} to get bytes from.
     * @param offset The absolute offset within the {@link ByteBuffer} of the
     * first byte to be read; must be non-negative.
     * @param length The number of bytes to be copied.
     * @return Returns an array of the bytes gotten.
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes in the buffer starting at the given offset.
     * @throws IllegalArgumentException if the offset is either negative or
     * beyond the buffer's limit.
     */
    public static byte[] getBytes( ByteBuffer buf, int offset, int length ) {
        final byte[] dst = new byte[ length ];
        getBytes( buf, offset, dst, 0, length );
        return dst;
    }

    /**
     * This is an absolute bulk get method for {@link ByteBuffer} because it
     * doesn't have one (but should!).  Being absolute, the
     * {@link ByteBuffer}'s position doesn't change.
     *
     * @param buf The {@link ByteBuffer} to get bytes from.
     * @param bufOffset The absolute offset within the {@link ByteBuffer} of
     * the first byte to be read; must be non-negative.
     * @param dst The array into which bytes are to be written.
     * @param dstOffset The offset within the destination array to start
     * writing bytes.
     * @param length The number of bytes to be copied.
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes in the buffer starting at the given offset.
     * @throws IllegalArgumentException if the offset is either negative or
     * beyond the buffer's limit.
     */
    public static void getBytes( ByteBuffer buf, int bufOffset, byte[] dst,
                                 int dstOffset, int length ) {
        final int origPos = buf.position();
        buf.position( bufOffset );
        buf.get( dst, dstOffset, length );
        buf.position( origPos );
    }

    /**
     * Gets a {@link String} that is the same length as the given string
     * starting at the buffer's current position, advances the position by the
     * same length, and compares the two strings for equality.
     *
     * @param buf The {@link ByteBuffer} to get bytes from.
     * @param s The {@link String} to compare to.
     * @param charsetName The name of a supported character set.
     * @return Returns <code>true</code> only if the two strings are equal.
     * @throws BufferUnderflowException if there are fewer than the number of
     * bytes in the given string remaining in the buffer starting at its
     * current position.
     * @see #getEquals(ByteBuffer,int,String,String)
     */
    public static boolean getEquals( ByteBuffer buf, String s,
                                     String charsetName ) {
        return s.equals( getString( buf, s.length(), charsetName ) );
    }

    /**
     * Gets a {@link String} that is the same length as the given string
     * starting at the given position and compares the two strings for equality.
     *
     * @param buf The {@link ByteBuffer} to get bytes from.
     * @param offset The position to start getting bytes from.
     * @param s The {@link String} to compare to.
     * @param charsetName The name of a supported character set.
     * @return Returns <code>true</code> only if the two strings are equal.
     * @throws BufferUnderflowException if there are fewer than the number of
     * bytes in the given string remaining in the buffer starting at the given
     * position.
     * @see #getEquals(ByteBuffer,String,String)
     */
    public static boolean getEquals( ByteBuffer buf, int offset,
                                     String s, String charsetName ) {
        return s.equals( getString( buf, offset, s.length(), charsetName ) );
    }

    /**
     * This is a relative get {@link String} method for {@link ByteBuffer}
     * because it doesn't have one (but should!).
     *
     * @param buf The {@link ByteBuffer} to get bytes from.
     * @param length The number of bytes to be copied.
     * @param charsetName The name of a supported character set.
     * @return Returns a {@link String} comprising the bytes gotten.
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes remaining in the buffer.
     * @throws IllegalArgumentException if the offset is either negative or
     * beyond the buffer's limit, or <code>charsetName</code> specifies an
     * unsupported character set.
     */
    public static String getString( ByteBuffer buf, int length,
                                    String charsetName ) {
        try {
            return new String( getBytes( buf, length ), charsetName );
        }
        catch ( UnsupportedEncodingException e ) {
            throw new IllegalArgumentException( e );
        }
    }

    /**
     * This is an absolute get {@link String} method for {@link ByteBuffer}
     * because it doesn't have one (but should!).  The {@link ByteBuffer}'s
     * position is unchanged.
     *
     * @param buf The {@link ByteBuffer} to get bytes from.
     * @param offset The absolute offset within the {@link ByteBuffer} of the
     * first byte to be read; must be non-negative.
     * @param length The number of bytes to be copied.
     * @param charsetName The name of a supported character set.
     * @return Returns a {@link String} comprising the bytes gotten.
     * @throws BufferUnderflowException if there are fewer than
     * <code>length</code> bytes in the buffer starting at the given offset.
     * @throws IllegalArgumentException if the offset is either negative or
     * beyond the buffer's limit, or <code>charsetName</code> specifies an
     * unsupported character set.
     */
    public static String getString( ByteBuffer buf, int offset, int length,
                                    String charsetName ) {
        try {
            return new String( getBytes( buf, offset, length ), charsetName );
        }
        catch ( UnsupportedEncodingException e ) {
            throw new IllegalArgumentException( e );
        }
    }

    /**
     * This is a relative method for getting a single unsigned byte.  Unlike
     * {@link ByteBuffer#get()}, this method won't do sign-extension.
     *
     * @param buf The {@link ByteBuffer} to get the byte from.
     * @return Returns an unsigned integer in the range 0-256.
     * @throws BufferUnderflowException if the buffer's current position is not
     * smaller than its limit.
     */
    public static int getUnsignedByte( ByteBuffer buf ) {
        return buf.get() & 0x000000FF;
    }

    /**
     * This is an absolute method for getting a single unsigned byte.  Unlike
     * {@link ByteBuffer#get(int)}, this method won't do sign-extension.
     *
     * @param buf The {@link ByteBuffer} to get the byte from.
     * @param offset The absolute offset within the {@link ByteBuffer} of the
     * first byte to read; must be non-negative.
     * @return Returns an unsigned integer in the range 0-256.
     * @throws BufferUnderflowException if the buffer's current position is not
     * smaller than its limit.
     * @throws IllegalArgumentException if the offset is either negative or
     * beyond the buffer's limit.
     */
    public static int getUnsignedByte( ByteBuffer buf, int offset ) {
        try {
            return buf.get( offset ) & 0x000000FF;
        }
        catch ( IndexOutOfBoundsException e ) {
            throw new IllegalArgumentException( e );
        }
    }

    /**
     * This is a relative method for getting a single unsigned integer.  Unlike
     * {@link ByteBuffer#getInt()}, this method won't do sign-extension.
     *
     * @param buf The {@link ByteBuffer} to get the byte from.
     * @return Returns an unsigned integer.
     * @throws BufferUnderflowException if the buffer's current position is not
     * smaller than its limit.
     */
    public static long getUnsignedInt( ByteBuffer buf ) {
        return buf.getInt() & 0x00000000FFFFFFFFL;
    }

    /**
     * This is a relative method for getting a single unsigned integer.  Unlike
     * {@link ByteBuffer#getInt()}, this method won't do sign-extension.
     *
     * @param buf The {@link ByteBuffer} to get the byte from.
     * @param offset The absolute offset within the {@link ByteBuffer} of the
     * first byte to read; must be non-negative.
     * @return Returns an unsigned integer.
     * @throws BufferUnderflowException if the buffer's current position is not
     * smaller than its limit.
     * @throws IllegalArgumentException if the offset is either negative or
     * beyond the buffer's limit minus 1.
     */
    public static long getUnsignedInt( ByteBuffer buf, int offset ) {
        try {
            return buf.getInt( offset ) & 0x00000000FFFFFFFFL;
        }
        catch ( IndexOutOfBoundsException e ) {
            throw new IllegalArgumentException( e );
        }
    }

    /**
     * This is a relative method for getting 2 unsigned bytes.  Unlike
     * {@link ByteBuffer#getShort()}, this method won't do sign-extension.
     *
     * @param buf The {@link ByteBuffer} to get bytes from.
     * @return Returns an unsigned integer in the range 0-65536.
     * @throws BufferUnderflowException if there are fewer than two bytes
     * remaining in the buffer.
     */
    public static int getUnsignedShort( ByteBuffer buf ) {
        return buf.getShort() & 0x0000FFFF;
    }

    /**
     * This is an absolute method for getting 2 unsigned bytes.  Unlike
     * {@link ByteBuffer#getShort(int)}, this method won't do sign-extension.
     *
     * @param buf The {@link ByteBuffer} to get bytes from.
     * @param offset The absolute offset within the {@link ByteBuffer} of the
     * first byte to read; must be non-negative.
     * @return Returns an unsigned integer in the range 0-65536.
     * @throws IllegalArgumentException if the offset is either negative or
     * beyond the buffer's limit minus 1.
     */
    public static int getUnsignedShort( ByteBuffer buf, int offset ) {
        try {
            return buf.getShort( offset ) & 0x0000FFFF;
        }
        catch ( IndexOutOfBoundsException e ) {
            throw new IllegalArgumentException( e );
        }
    }

    /**
     * This is a convenience method for mapping an entire {@link File} into a
     * {@link ByteBuffer}.
     *
     * @param file The {@link File} to map.
     * @return Returns a new {@link ByteBuffer}.
     * @see #map(File,long,long,FileChannel.MapMode)
     * @see #mapReadOnly(File)
     * @see #mapReadOnly(File,long,long)
     * @see #mapReadWrite(File)
     * @see #mapReadWrite(File,long,long)
     */
    public static ByteBuffer map( File file, FileChannel.MapMode mapMode )
        throws IOException
    {
        return map( file, 0, file.length(), mapMode );
    }

    /**
     * This is a convenience method for mapping a {@link File} into a
     * {@link ByteBuffer}.
     *
     * @param file The {@link File} to map.
     * @param position The position within the file at which the mapped region
     * is to start.
     * @param size The size of the region to be mapped.
     * @param mapMode The {@link FileChannel.MapMode} to use.
     * @return Returns a new {@link ByteBuffer}.
     * @see #map(File,FileChannel.MapMode)
     * @see #mapReadOnly(File)
     * @see #mapReadOnly(File,long,long)
     * @see #mapReadWrite(File)
     * @see #mapReadWrite(File,long,long)
     */
    public static ByteBuffer map( File file, long position, long size,
                                  FileChannel.MapMode mapMode )
        throws IOException
    {
        final String rafMode;
        if ( mapMode == FileChannel.MapMode.READ_ONLY )
            rafMode = "r";
        else if ( mapMode == FileChannel.MapMode.READ_WRITE )
            rafMode = "rw";
        else
            throw new IllegalArgumentException( "unsupported MapMode" );

        final RandomAccessFile raf = new RandomAccessFile( file, rafMode );
        try {
            return raf.getChannel().map( mapMode, position, size );
        }
        finally {
            raf.close();
        }
    }

    /**
     * This is a convenience method for mapping an entire {@link File} into a
     * {@link ByteBuffer}.
     *
     * @param file The {@link File} to map.
     * @return Returns a new {@link ByteBuffer}.
     * @see #map(File,FileChannel.MapMode)
     * @see #map(File,long,long,FileChannel.MapMode)
     * @see #mapReadOnly(File,long,long)
     * @see #mapReadWrite(File\)
     * @see #mapReadWrite(File,long,long)
     */
    public static ByteBuffer mapReadOnly( File file ) throws IOException {
        return mapReadOnly( file, 0, file.length() );
    }

    /**
     * This is a convenience method for mapping a {@link File} into a
     * {@link ByteBuffer}.
     *
     * @param file The {@link File} to map.
     * @param position The position within the file at which the mapped region
     * is to start.
     * @param size The size of the region to be mapped.
     * @return Returns a new {@link ByteBuffer}.
     * @see #map(File,FileChannel.MapMode)
     * @see #map(File,long,long,FileChannel.MapMode)
     * @see #mapReadOnly(File)
     * @see #mapReadWrite(File)
     * @see #mapReadWrite(File,long,long)
     */
    public static ByteBuffer mapReadOnly( File file, long position, long size )
        throws IOException
    {
        return map( file, position, size, FileChannel.MapMode.READ_ONLY );
    }

    /**
     * This is a convenience method for mapping an entire {@link File} into a
     * {@link ByteBuffer}.
     *
     * @param file The {@link File} to map.
     * @return Returns a new {@link ByteBuffer}.
     * @see #map(File,FileChannel.MapMode)
     * @see #map(File,long,long,FileChannel.MapMode)
     * @see #mapReadOnly(File)
     * @see #mapReadOnly(File,long,long)
     * @see #mapReadWrite(File,long,long)
     */
    public static ByteBuffer mapReadWrite( File file ) throws IOException {
        return mapReadWrite( file, 0, file.length() );
    }

    /**
     * This is a convenience method for mapping a {@link File} into a
     * {@link ByteBuffer}.
     *
     * @param file The {@link File} to map.
     * @param position The position within the file at which the mapped region
     * is to start.
     * @param size The size of the region to be mapped.
     * @return Returns a new {@link ByteBuffer}.
     * @see #map(File,FileChannel.MapMode)
     * @see #map(File,long,long,FileChannel.MapMode)
     * @see #mapReadOnly(File)
     * @see #mapReadOnly(File,long,long)
     * @see #mapReadWrite(File)
     */
    public static ByteBuffer mapReadWrite( File file, long position, long size )
        throws IOException
    {
        return map( file, position, size, FileChannel.MapMode.READ_WRITE );
    }

    /**
     * Puts a {@link String}'s bytes in the character set starting at the
     * {@link ByteBuffer}'s current position.  The buffer's position is
     * advanced by the number of bytes put.
     * <p>
     * This is just a convenience method for not having to deal with the
     * {@link UnsupportedEncodingException}.
     *
     * @param buf The {@link ByteBuffer} to put the {@link String} into.
     * @param s The {@link String} to put.
     * @param charsetName The name of a supported character set.
     * @throws BufferOverflowException if there is insufficient space remaining
     * in the buffer.
     * @throws IllegalArgumentException if <code>charsetName</code> is an
     * unsupported character set.
     * @see #put(ByteBuffer,int,String,String)
     */
    public static void put( ByteBuffer buf, String s, String charsetName ) {
        try {
            buf.put( s.getBytes( charsetName ) );
        }
        catch ( UnsupportedEncodingException e ) {
            throw new IllegalArgumentException( e );
        }
    }

    /**
     * Puts a {@link String}'s bytes in the given character set at the given
     * absolute position.  Being absolute, the {@link ByteBuffer}'s position
     * doesn't change.
     *
     * @param buf The {@link ByteBuffer} to put the {@link String} into.
     * @param offset The absolute offset within the {@link ByteBuffer} of where
     * to put the {@link String}; must be non-negative.
     * @param s The {@link String} to put.
     * @param charsetName The name of a supported character set.
     * @throws BufferOverflowException if there is insufficient space in the
     * buffer starting at the given position.
     * @throws IllegalArgumentException if the offset is either negative or
     * beyond the buffer's limit, or <code>charsetName</code> specifies an
     * unsupported character set.
     * @see #put(ByteBuffer,String,String)
     */
    public static void put( ByteBuffer buf, int offset, String s,
                            String charsetName ) {
        final int origPos = buf.position();
        buf.position( offset );
        put( buf, s, charsetName );
        buf.position( origPos );
    }

    /**
     * Skip a specified number of bytes, i.e., advance the buffer's position.
     *
     * @param buf The {@link ByteBuffer} to change the position of.
     * @param count The number of bytes to skip.
     * @throws IllegalArgumentException if the buffer's current position plus
     * the count is either negative or greater than the buffer's limit.
     */
    public static void skipBytes( ByteBuffer buf, int count ) {
        buf.position( buf.position() + count );
    }

    public static void clean( ByteBuffer buf ) {
        try {
            final Class<?> directByteBufferClass = Class.forName("java.nio.DirectByteBuffer");
            Method cleanerMethod = directByteBufferClass.getDeclaredMethod("cleaner");
            final Class<?> cleanerClass = Class.forName("sun.misc.Cleaner");
            Method cleanMethod = cleanerClass.getDeclaredMethod("clean");

            if (cleanerMethod != null && cleanMethod != null) {
                final boolean wasCleanerMethodAccessible = cleanerMethod.isAccessible();
                final boolean wasCleanMethodAccessible = cleanMethod.isAccessible();

                try {
                    cleanerMethod.setAccessible(true);
                    Object cleaner = cleanerMethod.invoke(buf);
                    cleanMethod.setAccessible(true);
                    cleanMethod.invoke(cleaner);
                    cleanerMethod.setAccessible(wasCleanerMethodAccessible);
                    cleanMethod.setAccessible(wasCleanMethodAccessible);
                }
                catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            }
        }
        catch (ClassNotFoundException e) {
            // ignore
        }
        catch (NoSuchMethodException e) {
            // ignore
        }
    }
}
/* vim:set et sw=4 ts=4: */
