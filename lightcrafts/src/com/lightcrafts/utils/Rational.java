/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

/**
 * <code>Rational</code> is-a {@link Number} for holding a rational number,
 * i.e., a fraction, e.g., 1/3.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class Rational extends Number implements Comparable {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>Rational</code>.
     *
     * @param numerator The numerator.
     * @param denominator The denominator.  It must not be zero.
     * @throws IllegalArgumentException only if the denominator is zero.
     */
    public Rational( int numerator, int denominator ) {
        if ( denominator == 0 )
            throw new IllegalArgumentException();
        final int d = gcd( Math.abs( numerator ), Math.abs( denominator ) );
        m_numerator   = numerator / d;
        m_denominator = denominator / d;
    }

    /**
     * Returns the value of this <code>Rational</code> as a <code>byte</code>.
     * This may involve rounding or truncation.
     *
     * @return Returns the numeric value represented by this object after
     * conversion to type <code>byte</code>.
     */
    public byte byteValue() {
        return (byte)doubleValue();
    }

    /**
     * Compares this <code>Rational</code> with another object for order.
     *
     * @param o The object to compare to.
     * @return Returns a negative, zero, or a positive integer if the numeric
     * value of this <code>Rational</code> is less than, equal to, or greater
     * than the numeric value of the other object.
     * @throws ClassCastException if the other object is not a {@link Number}.
     */
    public int compareTo( Object o ) {
        final double cmp = doubleValue() - ((Number)o).doubleValue();
        return cmp < 0 ? -1 : cmp > 0 ? 1 : 0;
    }

    /**
     * Returns the denominator of this <code>Rational</code>.
     *
     * @return Returns the denominator.
     */
    public int denominator() {
        return m_denominator;
    }

    /**
     * Returns the value of this <code>Rational</code> as a
     * <code>double</code>.  This may involve rounding.
     *
     * @return Returns the numeric value represented by this object after
     * conversion to type <code>double</code>.
     */
    public double doubleValue() {
        return (double)m_numerator / m_denominator;
    }

    /**
     * Compares this <code>Rational</code> to another object for equality.
     *
     * @param o The object to compare to.
     * @return Returns <code>true</code> only if the other object is also a
     * <code>Rational</code> and its value is equal to that of this
     * <code>Rational</code>.
     */
    public boolean equals( Object o ) {
        if ( o == this )
            return true;
        if ( o == null || o.getClass() != getClass() )
            return false;
        final Rational r = (Rational)o;
        return doubleValue() == r.doubleValue();
    }

    /**
     * Returns the value of this <code>Rational</code> as a <code>float</code>.
     * This may involve rounding.
     *
     * @return Returns the numeric value represented by this object after
     * conversion to type <code>float</code>.
     */
    public float floatValue() {
        return (float)m_numerator / m_denominator;
    }

    /**
     * Returns the value of this <code>Rational</code> as an <code>int</code>.
     * This may involve rounding or truncation.
     *
     * @return Returns the numeric value represented by this object after
     * conversion to type <code>int</code>.
     */
    public int intValue() {
        return m_numerator / m_denominator;
    }

    /**
     * Returns whether the value of this <code>Rational</code> can be
     * represented as an integer.
     *
     * @return Returns <code>true</code> only if the remainder of the numerator
     * divided by the denominator is zero.
     */
    public boolean isInteger() {
        return m_numerator % m_denominator == 0;
    }

    /**
     * Returns the value of this <code>Rational</code> as a <code>long</code>.
     * This may involve rounding or truncation.
     *
     * @return Returns the numeric value represented by this object after
     * conversion to type <code>long</code>.
     */
    public long longValue() {
        return (long)doubleValue();
    }

    /**
     * Returns the numerator of this <code>Rational</code>.
     *
     * @return Returns the denominator.
     */
    public int numerator() {
        return m_numerator;
    }

    /**
     * Parses the string argument as a signed rational number.  The characters
     * in the string must either be:
     *  <ul>
     *    <li>
     *      Two sets of decimal digits seperated by a single ASCII slash
     *      <code>'/'</code> (<code>'\u002F'</code>) character
     *      in which case the first set is parsed as the numerator
     *      and the second set is parsed as the denominator.
     *    </li>
     *    <li>
     *       Only a single set of decimal digits in which case it is parsed as
     *       the numerator and 1 is implied for the denominator.
     *    </li>
     *  </ul>
     * The first (or only) set of decimal digits may be preceded by an ASCII
     * minus sign <code>'-'</code> (<code>'\u002D'</code>) to indicate a
     * negative value.
     *
     * @param s The <code>String</code> containing the rational representation
     * to be parsed.
     * @throws IllegalArgumentException if the denominator parses to zero.
     * @throws NumberFormatException if the string does not contain a parsable
     * rational number.
     */
    public static Rational parseRational( String s ) {
        final String[] parts = s.split( "/" );
        if ( parts.length == 2 )
            return new Rational(
                Integer.parseInt( parts[0] ), Integer.parseInt( parts[1] )
            );
        return new Rational( Integer.parseInt( s ), 1 );
    }

    /**
     * Returns a new <code>Rational</code> that is the reciprocal of this
     * <code>Rational</code>.
     *
     * @return Returns said <code>Rational</code>.
     */
    public Rational reciprocal() {
        return new Rational( m_denominator, m_numerator );
    }

    /**
     * Returns the value of this <code>Rational</code> as a <code>short</code>.
     * This may involve rounding or truncation.
     *
     * @return Returns the numeric value represented by this object after
     * conversion to type <code>short</code>.
     */
    public short shortValue() {
        return (short)intValue();
    }

    /**
     * Returns the value of this <code>Rational</code> converted to
     * <code>String</code>.
     *
     * @return Returns said string.
     */
    public String toString() {
        return m_numerator + "/" + m_denominator;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Compute the greatest common divisor (GCD) of two integers.
     * This is an implementation of the Silver/Tersian algorithm that is
     * efficient for integers using a binary representation.
     *
     * @param n The first integer.  It is assumed to be non-negative.
     * @param m The second integer.  It is assumed to be non-negative.
     * @return Returns the GCD.
     * @see "Donald E. Knuth, <i>The Art of Computer Programming</i>, vol. 2,
     * 3rd ed., Addison-Wesley, 1998, section 4.5.2."
     */
    private static int gcd( int n, int m ) {
        int factor = 1;
        // First, repeatedly divide numbers by 2 until at least one is odd.
        while ( (n & 1) == 0 && (m & 1) == 0 ) {
            n >>>= 1;
            m >>>= 1;
            factor <<= 1;
        }
        while ( n > 0 ) {
            if ( (n & 1) == 0 )
                n >>>= 1;
            else if ( (m & 1) == 0 )
                m >>>= 1;
            else
                if ( n < m )
                    m = (m - n) >>> 1;
                else
                    n = (n - m) >>> 1;
        }
        return factor * m;
    }

    private final int m_numerator;
    private final int m_denominator;
}
/* vim:set et sw=4 ts=4: */
