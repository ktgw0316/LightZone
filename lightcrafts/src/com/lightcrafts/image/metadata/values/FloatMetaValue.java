/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.lightcrafts.image.metadata.ImageMetaType;
import com.lightcrafts.utils.LCArrays;
import com.lightcrafts.utils.Rational;

import static com.lightcrafts.image.metadata.ImageMetaType.META_FLOAT;

/**
 * A <code>FloatMetaValue</code> is-a {@link NumericMetaValue} for holding a
 * signed <code>float</code> (32-bit) metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class FloatMetaValue extends NumericMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>FloatMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public FloatMetaValue() {
        // do nothing
    }

    /**
     * Construct a <code>FloatMetaValue</code>.
     *
     * @param values The array of values.
     */
    public FloatMetaValue( float... values ) {
        m_value = values;
    }

    /**
     * Construct a <code>FloatMetaValue</code>.
     *
     * @param values The array of values.
     */
    public FloatMetaValue( String... values ) {
        setValuesImpl( values );
    }

    /**
     * {@inheritDoc}
     */
    public FloatMetaValue clone() {
        final FloatMetaValue copy = (FloatMetaValue)super.clone();
        copy.m_value = m_value.clone();
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Object o ) {
        if ( o instanceof NumericMetaValue ) {
            final NumericMetaValue rightVal = (NumericMetaValue)o;
            final float leftFloat = getFloatValue();
            final float rightFloat = rightVal.getFloatValue();
            return Float.compare( leftFloat, rightFloat );
        }
        return super.compareTo( o );
    }

    /**
     * Compares this <code>ImageMetaValue</code> to a {@link String}.  The
     * string is first parsed either as a <code>float</code> or as a
     * {@link Rational} the converted to a <code>float</code>, then a numeric
     * comparison is done.
     *
     * @param s The {@link String} to compare to.
     * @return Returns a negative integer, zero, or a positive integer as this
     * <code>ImageMetaValue</code> is less than, equal to, or greater than the
     * <code>float</code> value of the string.  If the string can not be
     * parsed as either a <code>float</code> or a {@link Rational}, returns 1.
     * @see #compareTo(Object)
     */
    public int compareTo( String s ) {
        try {
            final float rightFloat;
            if ( s.indexOf( '/' ) > 0 )
                rightFloat = Rational.parseRational( s ).floatValue();
            else
                rightFloat = Float.parseFloat( s );
            final float leftFloat = getFloatValue();
            return Float.compare( leftFloat, rightFloat );
        }
        catch ( NumberFormatException e ) {
            return 1;
        }
    }

    /**
     * Get the first value as a <code>double</code>.
     *
     * @return Returns said value.
     */
    public double getDoubleValue() {
        return getFloatValue();
    }

    /**
     * Get the first <code>float</code> value.
     *
     * @return Returns said value.
     */
    public float getFloatValue() {
        return m_value[0];
    }

    /**
     * Get the native <code>float</code> array value.
     *
     * @return Returns said array.
     */
    public float[] getFloatValues() {
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
        return META_FLOAT;
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
            Float.parseFloat( value );
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
        setFloatValue( (float)newValue );
    }

    /**
     * Sets the <code>float</code> value at the given index.
     *
     * @param newValue The new value.
     * @param index The index to set the value of.
     */
    public synchronized void setFloatValueAt( float newValue, int index ) {
        checkIsEditable();
        if ( m_value == null )
            m_value = new float[ index + 1 ];
        else if ( index >= m_value.length )
            m_value = (float[])LCArrays.resize( m_value, index + 1 );
        m_value[ index ] = newValue;
        dirty();
    }

    /**
     * {@inheritDoc}
     */
    public void setLongValue( long newValue ) {
        setFloatValue( newValue );
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        final int length = readHeader( in );
        m_value = new float[ length ];
        for ( int i = 0; i < length; ++i )
            m_value[i] = in.readFloat();
    }

    /**
     * @serialData The header (see {@link #writeHeader(ObjectOutput)}) followed
     * by the <code>float</code> values.
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        writeHeader( out );
        for ( float value : m_value )
            out.writeFloat( value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Parse and append a new value.
     *
     * @param newValue The new value.
     * @throws NumberFormatException if the {@link String} does not contain a
     * parsable <code>float</code>.
     */
    protected void appendValueImpl( String newValue ) {
        final float newFloat = Float.parseFloat( newValue );
        if ( m_value == null )
            m_value = new float[]{ newFloat };
        else {
            m_value = (float[])LCArrays.resize( m_value, m_value.length + 1 );
            m_value[ m_value.length - 1 ] = newFloat;
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
            value[i] = Float.toString( m_value[i] );
        return value;
    }

    /**
     * Parse and set the values.
     *
     * @param newValue The array of new values.
     * @throws NumberFormatException if any one of the {@link String}s do not
     * contain a parsable <code>float</code>.
     */
    protected void setValuesImpl( String[] newValue ) {
        if ( m_value == null || m_value.length != newValue.length )
            m_value = new float[ newValue.length ];
        for ( int i = 0; i < newValue.length; ++i )
            m_value[i] = Float.parseFloat( newValue[i] );
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
        for ( float value : m_value ) {
            if ( !comma )
                comma = true;
            else
                sb.append( ',' );
            sb.append( value );
        }
        return sb.toString();
    }

    ////////// private ////////////////////////////////////////////////////////

    private float[] m_value;
}
/* vim:set et sw=4 ts=4: */
