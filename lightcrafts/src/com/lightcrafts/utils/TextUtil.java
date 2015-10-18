/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

import java.text.Normalizer;
import java.util.Date;
import java.text.DateFormat;

/**
 * A <code>TextUtil</code> is a set of utility functions for text.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class TextUtil {

    ////////// public /////////////////////////////////////////////////////////

    /** The number of bytes in 1 kilobyte. */
    public static final long K = 1024L;

    /** The number of bytes in 1 megabyte. */
    public static final long MB = K * K;

    /** The number of bytes in 1 gigabyte. */
    public static final long GB = MB * K;

    /**
     * Formats a {@link Date} into a date/time string.  The reason this method
     * is necessary is because date formats are not thread-safe.  From the
     * {@link DateFormat} Javadoc:
     * <blockquote>
     * Date formats are not synchronized.  It is recommended to create separate
     * format instances for each thread. If multiple threads access a format
     * concurrently, it must be synchronized externally.
     * </blockquote>
     * @param f The {@link DateFormat} to use.
     * @param d The {@link Date} to be formatted.
     * @return Returns the formatted string.
     */
    public static String dateFormat( DateFormat f, Date d ) {
        synchronized ( f ) {
            return f.format( d );
        }
    }

    /**
     * Convert the <code>byte</code> array to a string of hexadecimal digits.
     *
     * @param buf The <code>byte</code> array to convert.
     * @return Returns said string.
     */
    public static String hexString( byte[] buf ) {
        final StringBuilder hexBuf = new StringBuilder( buf.length * 2 );
        for ( Byte b : buf ) {
            hexBuf.append( HEX_ALPHABET.charAt( (b >>> 4) & 0x0F ) );
            hexBuf.append( HEX_ALPHABET.charAt(  b        & 0x0F ) );
        }
        return hexBuf.toString();
    }

    /**
     * Join an array of strings together seperated by a separator string.
     *
     * @param strings The strings to join.
     * @param sep The seperator or <code>null</code>.
     * @return Returns the joined strings.
     */
    public static String join( String[] strings, String sep ) {
        final StringBuilder sb = new StringBuilder();
        for ( int i = 0; i < strings.length; ++i ) {
            if ( i > 0 && sep != null )
                sb.append( sep );
            sb.append( strings[i] );
        }
        return sb.toString();
    }

    /**
     * This is a convenience method for performing a canonical decomposition
     * (NFD) to the characters of a Unicode string.
     *
     * @param s The {@link String} to normalize.
     * @return Returns the normalized string.
     */
    public static String normalize( String s ) {
        return Normalizer.normalize( s, Normalizer.Form.NFD );
    }

    /**
     * Returns a &quot;quantified&quot; string for a given size, i.e., the size
     * in bytes (B), kilobytes (K), megabytes (MB), or gigabytes (GB).
     *
     * @param size The size to quantify.
     * @return Returns said string.
     */
    public static String quantify( long size ) {
        if ( size < K )
            return Long.toString( size ) + 'B';
        if ( size < MB )
            return Long.toString( size / K ) + 'K';
        if ( size < GB )
            return tenths( (double)size / MB ) + "MB";
        return tenths( (double)size / GB ) + "GB";
    }

    /**
     * A version of {@link NumberUtil#tenths(double)} that returns its result
     * as a {@link String} for convenience.
     *
     * @param n The <code>double</code> to convert.
     * @return Returns said {@link String}.
     */
    public static String tenths( double n ) {
        return Double.toString( NumberUtil.tenths( n ) );
    }

    /**
     * A version of {@link NumberUtil#tenths(Rational)} that returns its result
     * as a {@link String} for convenience.
     *
     * @param n The {@link Rational} to convert.
     * @return Returns said {@link String}.
     */
    public static String tenths( Rational n ) {
        return tenths( n.doubleValue() );
    }

    /**
     * A version of {@link #tenths(double)} that returns an integer when the
     * tenths is ".0".
     *
     * @param n The {@code double} to convert.
     * @return Returns said {@link String}.
     * @see #tenths(double)
     * @see #tenthsNoDotZero(Rational)
     */
    public static String tenthsNoDotZero( double n ) {
        final String s = tenths( n );
        return s.endsWith( ".0" ) ? s.substring( 0, s.length() - 2 ) : s;
    }

    /**
     * A version of {@link #tenths(Rational)} that returns an integer when the
     * tenths is ".0".
     *
     * @param n The {@code double} to convert.
     * @return Returns said {@link String}.
     * @see #tenths(Rational)
     * @see #tenthsNoDotZero(double)
     */
    public static String tenthsNoDotZero( Rational n ) {
        return tenthsNoDotZero( n.doubleValue() );
    }

    /**
     * Trims null characters from both the beginning and ending of the given
     * {@link String}.
     *
     * @param s The {@link String} to trim.
     * @return Returns the {@link String} after having the nulls removed, if
     * any.
     */
    public static String trimNulls( String s ) {
        final char[] c = s.toCharArray();
        int begin = 0, len = c.length;
        while ( begin < len && c[ begin ] == '\0' )
            ++begin;
        while ( begin < len && c[ len - 1 ] == '\0' )
            --len;
        return begin > 0 || len < s.length() ? s.substring( begin, len ) : s;
    }

    /**
     * Zero-pad an integer.
     *
     * @param n The integer.
     * @param base The base, either 10 or 16.  If 16, any values 10-15 are
     * returned as upper-case A-F.
     * @param width The field width, maximum 8.
     * @return Returns a string representation of the integer zero-padded to
     * the given width.
     */
    public static String zeroPad( int n, int base, int width ) {
        final String s;
        switch ( base ) {
            case 10:
                s = Integer.toString( n );
                break;
            case 16:
                s = Integer.toHexString( n ).toUpperCase();
                break;
            default:
                throw new IllegalArgumentException( "base must be 10 or 16" );
        }
        final int len = s.length();
        return len < width ? "0000000".substring( 7 - width + len ) + s : s;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The alphabet of characters used to convert a <code>byte</code> array to
     * a {@link String} of hexadecimal characters.
     */
    private static final String HEX_ALPHABET = "0123456789ABCDEF";

}
/* vim:set et sw=4 ts=4: */
