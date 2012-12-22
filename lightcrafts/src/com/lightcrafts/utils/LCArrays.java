/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.lang.reflect.Array;

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
    public static native void copy( int[] src, int srcPos,
                                    byte[] dest, int destPos, int length );

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
    public static native void copy( short[] src, int srcPos,
                                    byte[] dest, int destPos, int length );

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
    public static native void copy( byte[] src, int srcPos,
                                    int[] dest, int destPos, int length );

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
    public static native void copy( byte[] src, int srcPos,
                                    short[] dest, int destPos, int length );

    /**
     * Resize an array.
     *
     * @param oldArray The array to resize.  It is not modified.
     * @param newLength The new length for the array.  It must not be negative.
     * @return If the old and new lengths differ, returns a new array of the
     * same type as the original and of the specified size with the elements of
     * the old array copied to the new array.
     * If the new array is smaller, only elements up to the new length are
     * copied; if the new array is larger, elements beyond the old length are
     * zero or <code>null</code> depending on the component type.  If the old
     * and new lengths are the same, returns the old array as-is.
     */
    public static Object resize( Object oldArray, int newLength )  {
        final Class c = oldArray.getClass();
        if ( !c.isArray() )
            throw new IllegalArgumentException( "given non-array" );
        final int oldLength = Array.getLength( oldArray );
        if ( oldLength == newLength )
            return oldArray;
        final Class type = c.getComponentType();
        final Object newArray = Array.newInstance( type, newLength );
        final int copyLength = Math.min( oldLength, newLength );
        if ( copyLength > 0 )
            System.arraycopy( oldArray, 0, newArray, 0, copyLength );
        return newArray;
    }

    static {
        System.loadLibrary( "LCArrays" );
    }
}
/* vim:set et sw=4 ts=4: */
