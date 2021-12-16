/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.nio.ByteBuffer;

import static java.nio.ByteOrder.LITTLE_ENDIAN;

/**
 * Various array utilities.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class LCArrays {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Copies the bytes from the source array to the destination array.
     * Currently, this is just a synonym for
     * {@link System#arraycopy(Object,int,Object,int,int)} for completeness.
     *
     * @param src The source array.
     * @param srcPos The starting position in the source array.
     * @param dest The destination array.
     * @param destPos The starting position in the destination array.
     * @param length The number of bytes to be copied.
     */
    public static void copy( byte[] src, int srcPos, byte[] dest, int destPos,
                             int length ) {
        System.arraycopy( src, srcPos, dest, destPos, length );
    }

    /**
     * Copies the bytes from the source array to the destination array
     * similarly to {@link System#arraycopy(Object,int,Object,int,int)} except
     * that it allows the source and destination arrays to be of different
     * types.
     *
     * In this case, copies the 4 bytes comprising each <code>int</code>
     * element to 4 <code>byte</code> elements.
     * <p>
     * <b>Warning:</b> Because this is implemented in native code using
     * <code>memcpy(3)</code>, it is critical that the <code>length</code>
     * parameter be correctly specified, otherwise bytes can be written beyond
     * the end of the <code>byte</code> array and chaos may ensue.
     *
     * @param src The source array.
     * @param srcPos The starting position in the source array.
     * @param dest The destination array.
     * @param destPos The starting position in the destination array.
     * @param length The number of bytes to be copied.
     */
    public static void copy( int[] src, int srcPos,
                             byte[] dest, int destPos, int length ) {
        ByteBuffer.wrap(dest, destPos, length)
                .order(LITTLE_ENDIAN)
                .asIntBuffer()
                .put(src, srcPos, length / 4);
    }

    /**
     * Copies the bytes from the source array to the destination array
     * similarly to {@link System#arraycopy(Object,int,Object,int,int)} except
     * that it allows the source and destination arrays to be of different
     * types.
     *
     * In this case, copies the 2 bytes comprising each <code>short</code>
     * element to 2 <code>byte</code> elements.
     * <p>
     * <b>Warning:</b> Because this is implemented in native code using
     * <code>memcpy(3)</code>, it is critical that the <code>length</code>
     * parameter be correctly specified, otherwise bytes can be written beyond
     * the end of the <code>byte</code> array and chaos may ensue.
     *
     * @param src The source array.
     * @param srcPos The starting position in the source array.
     * @param dest The destination array.
     * @param destPos The starting position in the destination array.
     * @param length The number of bytes to be copied.
     */
    public static void copy( short[] src, int srcPos,
                             byte[] dest, int destPos, int length ) {
        ByteBuffer.wrap(dest, destPos, length)
                .order(LITTLE_ENDIAN)
                .asShortBuffer()
                .put(src, srcPos, length / 2);
    }

    /**
     * Copies the bytes from the source array to the destination array
     * similarly to {@link System#arraycopy(Object,int,Object,int,int)} except
     * that it allows the source and destination arrays to be of different
     * types.
     *
     * In this case, copies 4 <code>byte</code> elements to each
     * <code>int</code> element.
     * <p>
     * <b>Warning:</b> Because this is implemented in native code using
     * <code>memcpy(3)</code>, it is critical that the <code>length</code>
     * parameter be correctly specified, otherwise bytes can be written beyond
     * the end of the <code>byte</code> array and chaos may ensue.
     *
     * @param src The source array.
     * @param srcPos The starting position in the source array.
     * @param dest The destination array.
     * @param destPos The starting position in the destination array.
     * @param length The number of bytes to be copied.
     */
    public static void copy( byte[] src, int srcPos,
                             int[] dest, int destPos, int length ) {
        ByteBuffer.wrap(src, srcPos, length)
                .order(LITTLE_ENDIAN)
                .asIntBuffer()
                .get(dest, destPos, length / 4);
    }

    /**
     * Copies the bytes from the source array to the destination array
     * similarly to {@link System#arraycopy(Object,int,Object,int,int)} except
     * that it allows the source and destination arrays to be of different
     * types.
     *
     * In this case, copies 2 <code>byte</code> elements to each
     * <code>short</code> element.
     * <p>
     * <b>Warning:</b> Because this is implemented in native code using
     * <code>memcpy(3)</code>, it is critical that the <code>length</code>
     * parameter be correctly specified, otherwise bytes can be written beyond
     * the end of the <code>byte</code> array and chaos may ensue.
     *
     * @param src The source array.
     * @param srcPos The starting position in the source array.
     * @param dest The destination array.
     * @param destPos The starting position in the destination array.
     * @param length The number of bytes to be copied.
     */
    public static void copy( byte[] src, int srcPos,
                             short[] dest, int destPos, int length ) {
        ByteBuffer.wrap(src, srcPos, length)
                .order(LITTLE_ENDIAN)
                .asShortBuffer()
                .get(dest, destPos, length / 2);
    }
}
/* vim:set et sw=4 ts=4: */
