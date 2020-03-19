/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Arrays;

import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.image.metadata.ImageMetaType;
import com.lightcrafts.utils.Rational;

import static com.lightcrafts.image.metadata.ImageMetaType.META_SLONG;

/**
 * A <code>LongMetaValue</code> is-a {@link NumericMetaValue} for holding a
 * signed long (32-bit) metadata value.  Note that the number of bits in a
 * metadata long is not the same as that of a Java <code>long</code>.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class LongMetaValue extends NumericMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>LongMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public LongMetaValue() {
        // do nothing
    }

    /**
     * Construct a <code>LongMetaValue</code>.
     *
     * @param values The array of values.
     */
    public LongMetaValue( long... values ) {
        m_value = constrain( values );
    }

    /**
     * Construct a <code>LongMetaValue</code>.
     *
     * @param values The array of values.
     */
    public LongMetaValue( String... values ) {
        setValuesImpl( values );
    }

    /**
     * {@inheritDoc}
     */
    public final LongMetaValue clone() {
        final LongMetaValue copy = (LongMetaValue)super.clone();
        copy.m_value = m_value.clone();
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    public final int compareTo( Object o ) {
        if ( o instanceof NumericMetaValue ) {
            final NumericMetaValue rightValue = (NumericMetaValue)o;
            final long leftLong = getLongValue();
            final long rightLong = rightValue.getLongValue();
            return (int)(leftLong - rightLong);
        }
        return super.compareTo( o );
    }

    /**
     * Compares this <code>ImageMetaValue</code> to a {@link String}.  The
     * string is first parsed either as a <code>long</code> or as a
     * {@link Rational} the converted to a <code>long</code>, then a numeric
     * comparison is done.
     *
     * @param s The {@link String} to compare to.
     * @return Returns a negative integer, zero, or a positive integer as this
     * <code>ImageMetaValue</code> is less than, equal to, or greater than the
     * <code>long</code> value of the string.  If the string can not be parsed
     * as either a <code>long</code> or a {@link Rational}, returns 1.
     * @see #compareTo(Object)
     */
    public final int compareTo( String s ) {
        try {
            final long rightLong;
            if ( s.indexOf( '/' ) > 0 )
                rightLong = Rational.parseRational( s ).longValue();
            else
                rightLong = Long.parseLong( s );
            final long leftLong = getLongValue();
            return (int)(leftLong - rightLong);
        }
        catch ( NumberFormatException e ) {
            return 1;
        }
    }

    /**
     * Gets the <code>long</code> value at the given index.
     *
     * @param index The index to get the value for.
     * @return Returns said value.
     */
    public final long getLongValueAt( int index ) {
        return m_value[ index ];
    }

    /**
     * Gets the native <code>long</code> array value.
     *
     * @return Returns said array.
     */
    public final long[] getLongValues() {
        return m_value;
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_LONG</code>.
     */
    public ImageMetaType getType() {
        return META_SLONG;
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
    public final boolean isLegalValue( String value ) {
        try {
            parseValue( value );
            return true;
        }
        catch ( NumberFormatException e ) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public final void setLongValue( long newValue ) {
        setLongValueAt( newValue, 0 );
    }

    /**
     * Sets the <code>long</code> value at the given index.
     *
     * @param newValue The new value.
     * @param index The index to set the value of.
     */
    public final synchronized void setLongValueAt( long newValue, int index ) {
        checkIsEditable();
        if ( m_value == null )
            m_value = new long[ index + 1 ];
        else if ( index >= m_value.length )
            m_value = Arrays.copyOf( m_value, index + 1 );
        m_value[ index ] = constrain( newValue );
        dirty();
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        final int length = readHeader( in );
        m_value = new long[ length ];
        for ( int i = 0; i < length; ++i )
            m_value[i] = in.readLong();
    }

    /**
     * @serialData The header (see {@link #writeHeader(ObjectOutput)}) followed
     * by the <code>long</code> values.
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        writeHeader( out );
        for ( long value : m_value )
            out.writeLong( value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Parse and append a new value.
     *
     * @param newValue The new value.
     * @throws NumberFormatException if the {@link String} does not contain a
     * parsable long.
     */
    protected final void appendValueImpl( String newValue ) {
        final long newLong = parseValue( newValue );
        if ( m_value == null )
            m_value = new long[]{ newLong };
        else {
            m_value = Arrays.copyOf( m_value, m_value.length + 1 );
            m_value[ m_value.length - 1 ] = newLong;
        }
    }

    /**
     * Constrain the value of a <code>long</code>.
     *
     * @param value The value to constrain.
     * @return Returns the constrained value.
     */
    protected long constrain( long value ) {
        return value;
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
            value[i] = Long.toString( getLongValueAt(i) );
        return value;
    }

    /**
     * Parse a signed long from a {@link String}.
     *
     * @param newValue The {@link String} to parse.
     * @return Returns an signed integer in the range -2^32 to (2^32)-1.
     * @throws NumberFormatException if the {@link String} does not contain a
     * parsable long.
     */
    protected long parseValue( String newValue ) {
        return Long.parseLong( newValue );
    }

    /**
     * Parse and set the values.
     *
     * @param newValue The array of new values.
     * @throws NumberFormatException if any one of the {@link String}s do not
     * contain a parsable long.
     */
    protected final void setValuesImpl( String[] newValue ) {
        if ( m_value == null || m_value.length != newValue.length )
            m_value = new long[ newValue.length ];
        for ( int i = 0; i < newValue.length; ++i )
            m_value[i] = parseValue( newValue[i] );
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
        final ImageMetadataDirectory owningDir = getOwningDirectory();
        final int tagID = getOwningTagID();
        final StringBuilder sb = new StringBuilder();
        boolean comma = false;
        for ( long value : m_value ) {
            if ( !comma )
                comma = true;
            else
                sb.append( ',' );
            final String valueAsString = owningDir != null ?
                owningDir.getTagValueLabelFor( tagID, value ) :
                Long.toString( value );
            sb.append( valueAsString );
        }
        return sb.toString();
    }

    ////////// protected //////////////////////////////////////////////////////

    protected long[] m_value;

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Constrain the values of the elements of a <code>long</code> array.
     *
     * @param values The values to trim.
     * @return Returns a new array with constrained values.
     */
    private long[] constrain( long[] values ) {
        final long[] newValues = new long[ values.length ];
        for ( int i = 0; i < values.length; ++i )
            newValues[i] = constrain( values[i] );
        return newValues;
    }


}
/* vim:set et sw=4 ts=4: */
