/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import com.lightcrafts.image.metadata.ImageMetaType;
import com.lightcrafts.utils.Rational;

import static com.lightcrafts.image.metadata.ImageMetaType.META_SRATIONAL;

/**
 * A <code>RationalMetaValue</code> is-a {@link NumericMetaValue} for holding a
 * {@link Rational} metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class RationalMetaValue extends NumericMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>RationalMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public RationalMetaValue() {
        // do nothing
    }

    /**
     * Construct a <code>RationalMetaValue</code>.
     *
     * @param numerator The numerator.
     * @param denominator The denominator.  It must not be zero.
     */
    public RationalMetaValue( int numerator, int denominator ) {
        this( new Rational( numerator, denominator ) );
    }

    /**
     * Construct a <code>RationalMetaValue</code>.
     *
     * @param values The array of values.
     */
    public RationalMetaValue( Rational... values ) {
        m_value = values;
    }

    /**
     * Construct a <code>RationalMetaValue</code>.
     *
     * @param values The array of values.
     */
    public RationalMetaValue( String... values ) {
        setValuesImpl( values );
    }

    /**
     * {@inheritDoc}
     */
    public RationalMetaValue clone() {
        final RationalMetaValue copy = (RationalMetaValue)super.clone();
        copy.m_value = m_value.clone();
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    public final int compareTo( Object o ) {
        if ( o instanceof NumericMetaValue ) {
            final NumericMetaValue rightVal = (NumericMetaValue)o;
            final double leftDouble = getDoubleValue();
            final double rightDouble = rightVal.getDoubleValue();
            return Double.compare( leftDouble, rightDouble );
        }
        return super.compareTo( o );
    }

    /**
     * Compares this <code>ImageMetaValue</code> to a {@link String}.  The
     * string is first parsed as a {@link Rational}, then a numeric comparison
     * is done.
     *
     * @param s The {@link String} to compare to.
     * @return Returns a negative integer, zero, or a positive integer as this
     * <code>ImageMetaValue</code> is less than, equal to, or greater than the
     * {@link Rational} value of the string.  If the string can not be parsed
     * as a {@link Rational}, returns 1.
     * @see #compareTo(Object)
     */
    public final int compareTo( String s ) {
        try {
            final Rational rightRational = Rational.parseRational( s );
            final Rational leftRational = getRationalValue();
            return leftRational.compareTo( rightRational );
        }
        catch ( NumberFormatException e ) {
            return 1;
        }
    }

    /**
     * {@inheritDoc}
     */
    public final double getDoubleValue() {
        return getRationalValue().doubleValue();
    }

    /**
     * {@inheritDoc}
     */
    public final long getLongValueAt(int index) {
        return m_value[index].longValue();
    }

    /**
     * Get the first {@link Rational} value.
     *
     * @return Returns said value.
     */
    public final Rational getRationalValue() {
        return m_value[0];
    }

    /**
     * Get the native {@link Rational} array value.
     *
     * @return Returns said array.
     */
    public final Rational[] getRationalValues() {
        return m_value;
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_RATIONAL</code>.
     */
    public ImageMetaType getType() {
        return META_SRATIONAL;
    }

    /**
     * {@inheritDoc}
     */
    public final int getValueCount() {
        return m_value != null ? m_value.length : 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLegalValue( String value ) {
        try {
            Rational.parseRational( value );
            return true;
        }
        catch ( IllegalArgumentException e ) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setLongValue( long newValue ) {
        setRationalValueAt( new Rational( (int)newValue, 1 ), 0 );
    }

    /**
     * Sets the {@link Rational} value at the given index.
     *
     * @param newValue The new value.
     * @param index The index to set the value of.
     */
    public final synchronized void setRationalValueAt( Rational newValue,
                                                       int index ) {
        checkIsEditable();
        if ( m_value == null )
            m_value = new Rational[ index + 1 ];
        else if ( index >= m_value.length )
            m_value = Arrays.copyOf( m_value, index + 1 );
        m_value[ index ] = newValue;
        dirty();
    }

    /**
     * {@inheritDoc}
     */
    public final void readExternal( ObjectInput in ) throws IOException {
        final int length = readHeader( in );
        m_value = new Rational[ length ];
        for ( int i = 0; i < length; ++i )
            m_value[i] = new Rational( in.readInt(), in.readInt() );
    }

    /**
     * @serialData The header (see {@link #writeHeader(ObjectOutput)}) followed
     * by the {@link Rational} values where each is a pair of <code>int</code>
     * comprising the numerator and denominator.
     */
    public final void writeExternal( ObjectOutput out ) throws IOException {
        writeHeader( out );
        for ( Rational value : m_value ) {
            out.writeInt( value.numerator() );
            out.writeInt( value.denominator() );
        }
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Parse and append a new value.
     *
     * @param newValue The new value.
     */
    protected final void appendValueImpl( String newValue ) {
        final Rational newRational = Rational.parseRational( newValue );
        if ( m_value == null )
            m_value = new Rational[] { newRational };
        else {
            m_value = Arrays.copyOf( m_value, m_value.length + 1 );
            m_value[ m_value.length - 1 ] = newRational;
        }
    }

    /**
     * Gets the values as an array of {@link String}.
     *
     * @return Returns said array.
     */
    protected final String[] getValuesImpl() {
        if ( m_value == null )
            return null;
        final String[] value = new String[ m_value.length ];
        for ( int i = 0; i < m_value.length; ++i )
            value[i] = m_value[i].toString();
        return value;
    }

    /**
     * Parse and set the values.
     *
     * @param newValue The array of new values.
     * @throws IllegalArgumentException if any one of the {@link String}s do
     * not contain a parseable {@link Rational}.
     */
    protected final void setValuesImpl( String[] newValue ) {
        if ( m_value == null || m_value.length != newValue.length )
            m_value = new Rational[ newValue.length ];
        for ( int i = 0; i < newValue.length; ++i )
            m_value[i] = Rational.parseRational( newValue[i] );
    }

    /**
     * Convert this value to its {@link String} representation.  Multiple
     * values are separated by commas.
     *
     * @return Returns said string.
     */
    protected final String toStringImpl() {
        if ( m_value == null )
            return null;
        final StringBuilder sb = new StringBuilder();
        boolean comma = false;
        for ( Rational value : m_value ) {
            if ( !comma )
                comma = true;
            else
                sb.append( ',' );
            sb.append( value.toString() );
        }
        return sb.toString();
    }

    ////////// private ////////////////////////////////////////////////////////

    private Rational[] m_value;
}
/* vim:set et sw=4 ts=4: */
