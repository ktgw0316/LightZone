/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.lightcrafts.image.metadata.ImageMetaType;
import com.lightcrafts.utils.LCArrays;
import com.lightcrafts.utils.Rational;

import static com.lightcrafts.image.metadata.ImageMetaType.META_DOUBLE;

/**
 * A <code>DoubleMetaValue</code> is-a {@link NumericMetaValue} for holding a
 * signed <code>double</code> (64-bit) metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class DoubleMetaValue extends NumericMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>DoubleMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public DoubleMetaValue() {
        // do nothing
    }

    /**
     * Construct a <code>DoubleMetaValue</code>.
     *
     * @param values The array of values.
     */
    public DoubleMetaValue( double... values ) {
        m_value = values;
    }

    /**
     * Construct a <code>DoubleMetaValue</code>.
     *
     * @param values The array of values.
     */
    public DoubleMetaValue( String... values ) {
        setValuesImpl( values );
    }

    /**
     * {@inheritDoc}
     */
    public DoubleMetaValue clone() {
        final DoubleMetaValue copy = (DoubleMetaValue)super.clone();
        copy.m_value = m_value.clone();
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Object o ) {
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
     * string is first parsed either as a <code>double</code> or as a
     * {@link Rational} the converted to a <code>double</code>, then a numeric
     * comparison is done.
     *
     * @param s The {@link String} to compare to.
     * @return Returns a negative integer, zero, or a positive integer as this
     * <code>ImageMetaValue</code> is less than, equal to, or greater than the
     * <code>double</code> value of the string.  If the string can not be
     * parsed as either a <code>double</code> or a {@link Rational}, returns 1.
     * @see #compareTo(Object)
     */
    public int compareTo( String s ) {
        try {
            final double rightDouble;
            if ( s.indexOf( '/' ) > 0 )
                rightDouble = Rational.parseRational( s ).doubleValue();
            else
                rightDouble = Double.parseDouble( s );
            final double leftDouble = getDoubleValue();
            return Double.compare( leftDouble, rightDouble );
        }
        catch ( NumberFormatException e ) {
            return 1;
        }
    }

    /**
     * Get the first <code>double</code> value.
     *
     * @return Returns said value.
     */
    public double getDoubleValue() {
        return m_value[0];
    }

    /**
     * Get the native <code>double</code> array value.
     *
     * @return Returns said array.
     */
    public double[] getDoubleValues() {
        return m_value;
    }

    /**
     * {@inheritDoc}
     */
    public long getLongValueAt(int index) {
        return (long)m_value[index];
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_LONG</code>.
     */
    public ImageMetaType getType() {
        return META_DOUBLE;
    }

    /**
     * {@inheritDoc}
     */
    public int getValueCount() {
        return m_value != null ? m_value.length : 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLegalValue( String value ) {
        try {
            Double.parseDouble( value );
            return true;
        }
        catch ( NumberFormatException e ) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setDoubleValue( double newValue ) {
        setDoubleValueAt( newValue, 0 );
    }

    /**
     * Sets the <code>double</code> value at the given index.
     *
     * @param newValue The new value.
     * @param index The index to set the value of.
     */
    public synchronized void setDoubleValueAt( double newValue, int index ) {
        checkIsEditable();
        if ( m_value == null )
            m_value = new double[ index + 1 ];
        else if ( index >= m_value.length )
            m_value = (double[])LCArrays.resize( m_value, index + 1 );
        m_value[ index ] = newValue;
        dirty();
    }

    /**
     * {@inheritDoc}
     */
    public void setLongValue( long newValue ) {
        setDoubleValueAt( newValue, 0 );
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        final int length = readHeader( in );
        m_value = new double[ length ];
        for ( int i = 0; i < length; ++i )
            m_value[i] = in.readDouble();
    }

    /**
     * @serialData The header (see {@link #writeHeader(ObjectOutput)}) followed
     * by the <code>double</code> values.
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        writeHeader( out );
        for ( double value : m_value )
            out.writeDouble( value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Parse and append a new value.
     *
     * @param newValue The new value.
     * @throws NumberFormatException if the {@link String} does not contain a
     * parsable <code>double</code>.
     */
    protected void appendValueImpl( String newValue ) {
        final double newDouble = Double.parseDouble( newValue );
        if ( m_value == null )
            m_value = new double[]{ newDouble };
        else {
            m_value = (double[])LCArrays.resize( m_value, m_value.length + 1 );
            m_value[ m_value.length - 1 ] = newDouble;
        }
    }

    /**
     * Gets the values as an array of {@link String}.
     *
     * @return Returns said array.
     */
    protected String[] getValuesImpl() {
        if ( m_value == null )
            return null;
        final String[] value = new String[ m_value.length ];
        for ( int i = 0; i < m_value.length; ++i )
            value[i] = Double.toString( m_value[i] );
        return value;
    }

    /**
     * Parse and set the values.
     *
     * @param newValue The array of new values.
     * @throws NumberFormatException if any one of the {@link String}s do not
     * contain a parsable <code>double</code>.
     */
    protected void setValuesImpl( String[] newValue ) {
        if ( m_value == null || m_value.length != newValue.length )
            m_value = new double[ newValue.length ];
        for ( int i = 0; i < newValue.length; ++i )
            m_value[i] = Double.parseDouble( newValue[i] );
    }

    /**
     * Convert this value to its {@link String} representation.  Multiple
     * values are separated by commas.
     *
     * @return Returns said string.
     */
    protected String toStringImpl() {
        if ( m_value == null )
            return null;
        final StringBuilder sb = new StringBuilder();
        boolean comma = false;
        for ( double value : m_value ) {
            if ( !comma )
                comma = true;
            else
                sb.append( ',' );
            sb.append( value );
        }
        return sb.toString();
    }

    ////////// private ////////////////////////////////////////////////////////

    private double[] m_value;
}
/* vim:set et sw=4 ts=4: */
