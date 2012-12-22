/*
 * $RCSfile: TIFFField.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:34 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;

import java.io.Serializable;

/**
 * A class representing a field in a TIFF 6.0 Image File Directory.
 *
 * <p> The TIFF file format is described in more detail in the
 * comments for the TIFFDescriptor class.
 *
 * <p> A field in a TIFF Image File Directory (IFD).  A field is defined
 * as a sequence of values of identical data type.  TIFF 6.0 defines
 * 12 data types, which are mapped internally onto the Java datatypes
 * byte, int, long, float, and double.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 *
 * @see com.lightcrafts.mediax.jai.operator.TIFFDescriptor
 * @see TIFFDirectory
 */
public class TIFFField extends Object implements Comparable, Serializable {

    /** Flag for 8 bit unsigned integers. */
    public static final int TIFF_BYTE      =  1;

    /** Flag for null-terminated ASCII strings. */
    public static final int TIFF_ASCII     =  2;

    /** Flag for 16 bit unsigned integers. */
    public static final int TIFF_SHORT     =  3;

    /** Flag for 32 bit unsigned integers. */
    public static final int TIFF_LONG      =  4;

    /** Flag for pairs of 32 bit unsigned integers. */
    public static final int TIFF_RATIONAL  =  5;

    /** Flag for 8 bit signed integers. */
    public static final int TIFF_SBYTE     =  6;

    /** Flag for 8 bit uninterpreted bytes. */
    public static final int TIFF_UNDEFINED =  7;

    /** Flag for 16 bit signed integers. */
    public static final int TIFF_SSHORT    =  8;

    /** Flag for 32 bit signed integers. */
    public static final int TIFF_SLONG     =  9;

    /** Flag for pairs of 32 bit signed integers. */
    public static final int TIFF_SRATIONAL = 10;

    /** Flag for 32 bit IEEE floats. */
    public static final int TIFF_FLOAT     = 11;

    /** Flag for 64 bit IEEE doubles. */
    public static final int TIFF_DOUBLE    = 12;

    /** The tag number. */
    int tag;

    /** The tag type. */
    int type;

    /** The number of data items present in the field. */
    int count;

    /** The field data. */
    Object data;
    
    /** The default constructor. */
    TIFFField() {}

    /**
     * Constructs a TIFFField with arbitrary data.  The data
     * parameter must be an array of a Java type appropriate for the
     * type of the TIFF field.  Since there is no available 32-bit
     * unsigned datatype, long is used. The mapping between types is
     * as follows:
     *
     * <table border=1>
     * <tr>
     * <th> TIFF type </th> <th> Java type </th>
     * <tr>
     * <td><tt>TIFF_BYTE</tt></td>      <td><tt>byte</tt></td>
     * <tr>
     * <td><tt>TIFF_ASCII</tt></td>     <td><tt>String</tt></td>
     * <tr>
     * <td><tt>TIFF_SHORT</tt></td>     <td><tt>char</tt></td>
     * <tr>
     * <td><tt>TIFF_LONG</tt></td>      <td><tt>long</tt></td>
     * <tr>
     * <td><tt>TIFF_RATIONAL</tt></td>  <td><tt>long[2]</tt></td>
     * <tr>
     * <td><tt>TIFF_SBYTE</tt></td>     <td><tt>byte</tt></td>
     * <tr>
     * <td><tt>TIFF_UNDEFINED</tt></td> <td><tt>byte</tt></td>
     * <tr>
     * <td><tt>TIFF_SSHORT</tt></td>    <td><tt>short</tt></td>
     * <tr>
     * <td><tt>TIFF_SLONG</tt></td>     <td><tt>int</tt></td>
     * <tr>
     * <td><tt>TIFF_SRATIONAL</tt></td> <td><tt>int[2]</tt></td>
     * <tr>
     * <td><tt>TIFF_FLOAT</tt></td>     <td><tt>float</tt></td>
     * <tr>
     * <td><tt>TIFF_DOUBLE</tt></td>    <td><tt>double</tt></td>
     * </table>
     *
     * <p>Note that the <code>data</code> parameter should always
     * be the actual field value regardless of the number of bytes
     * required for that value.  This is the case despite the fact
     * that the TIFF <i>IFD Entry</i> corresponding to the field may
     * actually contain the offset to the field's value rather than
     * the value itself (the latter occurring if and only if the
     * value fits into 4 bytes).</p>
     */
    public TIFFField(int tag, int type, int count, Object data) {
        this.tag = tag;
        this.type = type;
        this.count = count;
        this.data = data;
    }

    /**
     * Returns the tag number, between 0 and 65535.
     */
    public int getTag() {
        return tag;
    }

    /**
     * Returns the type of the data stored in the field.
     * For a TIFF6.0 file, the value will equal one of the
     * TIFF_ constants defined in this class.  For future
     * revisions of TIFF, higher values are possible.
     *
     */
    public int getType() {
        return type;
    }

    /**
     * Returns the number of data items present in the field.
     */
    public int getCount() {
        return count;
    }

    /**
     * Returns the data as an uninterpreted array of bytes.
     * The type of the field must be one of TIFF_BYTE, TIFF_SBYTE,
     * or TIFF_UNDEFINED;
     *
     * <p> For data in TIFF_BYTE format, the application must take
     * care when promoting the data to longer integral types
     * to avoid sign extension.
     *
     * <p> A ClassCastException will be thrown if the field is not
     * of type TIFF_BYTE, TIFF_SBYTE, or TIFF_UNDEFINED.
     */
    public byte[] getAsBytes() {
        return (byte[])data;
    }

    /**
     * Returns TIFF_SHORT data as an array of chars (unsigned 16-bit
     * integers).
     *
     * <p> A ClassCastException will be thrown if the field is not
     * of type TIFF_SHORT.
     */
    public char[] getAsChars() {
        return (char[])data;
    }

    /**
     * Returns TIFF_SSHORT data as an array of shorts (signed 16-bit
     * integers).
     *
     * <p> A ClassCastException will be thrown if the field is not
     * of type TIFF_SSHORT.
     */
    public short[] getAsShorts() {
        return (short[])data;
    }

    /**
     * Returns TIFF_SLONG data as an array of ints (signed 32-bit
     * integers).
     *
     * <p> A ClassCastException will be thrown if the field is not
     * of type TIFF_SLONG.
     */
    public int[] getAsInts() {
        return (int[])data;
    }

    /**
     * Returns TIFF_LONG data as an array of longs (signed 64-bit
     * integers).
     *
     * <p> A ClassCastException will be thrown if the field is not
     * of type TIFF_LONG.
     */
    public long[] getAsLongs() {
        return (long[])data;
    }

    /**
     * Returns TIFF_FLOAT data as an array of floats. 
     *
     * <p> A ClassCastException will be thrown if the field is not
     * of type TIFF_FLOAT.
     */
    public float[] getAsFloats() {
        return (float[])data;
    }

    /**
     * Returns TIFF_DOUBLE data as an array of doubles. 
     *
     * <p> A ClassCastException will be thrown if the field is not
     * of type TIFF_DOUBLE.
     */
    public double[] getAsDoubles() {
        return (double[])data;
    }

    /**
     * Returns TIFF_SRATIONAL data as an array of 2-element arrays of ints.
     *
     * <p> A ClassCastException will be thrown if the field is not
     * of type TIFF_SRATIONAL.
     */
    public int[][] getAsSRationals() {
        return (int[][])data;
    }

    /**
     * Returns TIFF_RATIONAL data as an array of 2-element arrays of longs.
     *
     * <p> A ClassCastException will be thrown if the field is not
     * of type TIFF_RATTIONAL.
     */
    public long[][] getAsRationals() {
        return (long[][])data;
    }

    /**
     * Returns data in TIFF_BYTE, TIFF_SBYTE, TIFF_UNDEFINED, TIFF_SHORT,
     * TIFF_SSHORT, or TIFF_SLONG format as an int.
     *
     * <p> TIFF_BYTE and TIFF_UNDEFINED data are treated as unsigned;
     * that is, no sign extension will take place and the returned
     * value will be in the range [0, 255].  TIFF_SBYTE data will
     * be returned in the range [-128, 127].
     *
     * <p> A ClassCastException will be thrown if the field is not of
     * type TIFF_BYTE, TIFF_SBYTE, TIFF_UNDEFINED, TIFF_SHORT,
     * TIFF_SSHORT, or TIFF_SLONG.
     */
    public int getAsInt(int index) {
        switch (type) {
        case TIFF_BYTE: case TIFF_UNDEFINED:
            return ((byte[])data)[index] & 0xff;
        case TIFF_SBYTE:
            return ((byte[])data)[index];
        case TIFF_SHORT:
            return ((char[])data)[index] & 0xffff;
        case TIFF_SSHORT:
            return ((short[])data)[index];
        case TIFF_SLONG:
            return ((int[])data)[index];
        default:
            throw new ClassCastException();
        }
    }

    /**
     * Returns data in TIFF_BYTE, TIFF_SBYTE, TIFF_UNDEFINED, TIFF_SHORT,
     * TIFF_SSHORT, TIFF_SLONG, or TIFF_LONG format as a long.
     *
     * <p> TIFF_BYTE and TIFF_UNDEFINED data are treated as unsigned;
     * that is, no sign extension will take place and the returned
     * value will be in the range [0, 255].  TIFF_SBYTE data will
     * be returned in the range [-128, 127].
     *
     * <p> A ClassCastException will be thrown if the field is not of
     * type TIFF_BYTE, TIFF_SBYTE, TIFF_UNDEFINED, TIFF_SHORT,
     * TIFF_SSHORT, TIFF_SLONG, or TIFF_LONG.
     */
    public long getAsLong(int index) {
        switch (type) {
        case TIFF_BYTE: case TIFF_UNDEFINED:
            return ((byte[])data)[index] & 0xff;
        case TIFF_SBYTE:
            return ((byte[])data)[index];
        case TIFF_SHORT:
            return ((char[])data)[index] & 0xffff;
        case TIFF_SSHORT:
            return ((short[])data)[index];
        case TIFF_SLONG:
            return ((int[])data)[index];
        case TIFF_LONG:
            return ((long[])data)[index];
        default:
            throw new ClassCastException();
        }
    }
    
    /**
     * Returns data in any numerical format as a float.  Data in
     * TIFF_SRATIONAL or TIFF_RATIONAL format are evaluated by
     * dividing the numerator into the denominator using
     * double-precision arithmetic and then truncating to single
     * precision.  Data in TIFF_SLONG, TIFF_LONG, or TIFF_DOUBLE
     * format may suffer from truncation.
     *
     * <p> A ClassCastException will be thrown if the field is
     * of type TIFF_UNDEFINED or TIFF_ASCII.
     */
    public float getAsFloat(int index) {
        switch (type) {
        case TIFF_BYTE:
            return ((byte[])data)[index] & 0xff;
        case TIFF_SBYTE:
            return ((byte[])data)[index];
        case TIFF_SHORT:
            return ((char[])data)[index] & 0xffff;
        case TIFF_SSHORT:
            return ((short[])data)[index];
        case TIFF_SLONG:
            return ((int[])data)[index];
        case TIFF_LONG:
            return ((long[])data)[index];
        case TIFF_FLOAT:
            return ((float[])data)[index];
        case TIFF_DOUBLE:
            return (float)((double[])data)[index];
        case TIFF_SRATIONAL:
            int[] ivalue = getAsSRational(index);
            return (float)((double)ivalue[0]/ivalue[1]);
        case TIFF_RATIONAL:
            long[] lvalue = getAsRational(index);
            return (float)((double)lvalue[0]/lvalue[1]);
        default:
            throw new ClassCastException();
        }
    }

    /**
     * Returns data in any numerical format as a float.  Data in
     * TIFF_SRATIONAL or TIFF_RATIONAL format are evaluated by
     * dividing the numerator into the denominator using
     * double-precision arithmetic.
     *
     * <p> A ClassCastException will be thrown if the field is of
     * type TIFF_UNDEFINED or TIFF_ASCII.
     */
    public double getAsDouble(int index) {
        switch (type) {
        case TIFF_BYTE:
            return ((byte[])data)[index] & 0xff;
        case TIFF_SBYTE:
            return ((byte[])data)[index];
        case TIFF_SHORT:
            return ((char[])data)[index] & 0xffff;
        case TIFF_SSHORT:
            return ((short[])data)[index];
        case TIFF_SLONG:
            return ((int[])data)[index];
        case TIFF_LONG:
            return ((long[])data)[index];
        case TIFF_FLOAT:
            return ((float[])data)[index];
        case TIFF_DOUBLE:
            return ((double[])data)[index];
        case TIFF_SRATIONAL:
            int[] ivalue = getAsSRational(index);
            return (double)ivalue[0]/ivalue[1];
        case TIFF_RATIONAL:
            long[] lvalue = getAsRational(index);
            return (double)lvalue[0]/lvalue[1];
        default:
            throw new ClassCastException();
        }
    }

    /**
     * Returns a TIFF_ASCII data item as a String.
     *
     * <p> A ClassCastException will be thrown if the field is not
     * of type TIFF_ASCII.
     */
    public String getAsString(int index) {
        return ((String[])data)[index];
    }

    /**
     * Returns a TIFF_SRATIONAL data item as a two-element array
     * of ints.
     *
     * <p> A ClassCastException will be thrown if the field is not
     * of type TIFF_SRATIONAL.
     */
    public int[] getAsSRational(int index) {
        return ((int[][])data)[index];
    }

    /**
     * Returns a TIFF_RATIONAL data item as a two-element array
     * of ints.
     *
     * <p> A ClassCastException will be thrown if the field is not
     * of type TIFF_RATIONAL.
     */
    public long[] getAsRational(int index) {
        return ((long[][])data)[index];
    }

    /**
     * Compares this <code>TIFFField</code> with another
     * <code>TIFFField</code> by comparing the tags.
     *
     * <p><b>Note: this class has a natural ordering that is inconsistent
     * with <code>equals()</code>.</b>
     *
     * @throws IllegalArgumentException if the parameter is <code>null</code>.
     * @throws ClassCastException if the parameter is not a
     *         <code>TIFFField</code>.
     */
    public int compareTo(Object o) {
        if(o == null) {
            throw new IllegalArgumentException();
        }

        int oTag = ((TIFFField)o).getTag();

        if(tag < oTag) {
            return -1;
        } else if(tag > oTag) {
            return 1;
        } else {
            return 0;
        }
    }
}
