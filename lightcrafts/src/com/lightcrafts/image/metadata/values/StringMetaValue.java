/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.lightcrafts.image.metadata.ImageMetaType;
import com.lightcrafts.image.metadata.ImageMetadataDirectory;
import com.lightcrafts.utils.LCArrays;

import static com.lightcrafts.image.metadata.ImageMetaType.META_STRING;

/**
 * A <code>StringMetaValue</code> is-an {@link ImageMetaValue} for holding a
 * string metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class StringMetaValue extends ImageMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>StringMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public StringMetaValue() {
        // do nothing
    }

    /**
     * Construct a <code>StringMetaValue</code>.
     *
     * @param values The array of values.
     */
    public StringMetaValue( String... values ) {
        if ( values != null && values.length > 0 ) {
            m_value = new String[ values.length ];
            System.arraycopy( values, 0, m_value, 0, values.length );
        }
    }

    /**
     * {@inheritDoc}
     */
    public StringMetaValue clone() {
        final StringMetaValue copy = (StringMetaValue)super.clone();
        copy.m_value = m_value.clone();
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( Object o ) {
        if ( o instanceof StringMetaValue ) {
            final StringMetaValue rightValue = (StringMetaValue)o;
            final String leftString = getStringValue().toLowerCase();
            final String rightString =
                rightValue.getStringValue().toLowerCase();
            return leftString.compareTo( rightString );
        }
        return super.compareTo( o );
    }

    /**
     * Compares this <code>ImageMetaValue</code> to a {@link String}.  A
     * case-insensitive comparison is done.
     *
     * @param s The {@link String} to compare to.
     * @return Returns a negative integer, zero, or a positive integer as this
     * <code>ImageMetaValue</code> is less than, equal to, or greater than the
     * string.
     * @see #compareTo(Object)
     */
    public int compareTo( String s ) {
        final String leftString = getStringValue();
        if ( leftString == null )
            return s == null ? 0 : -1;
        return leftString.toLowerCase().compareTo( s.toLowerCase() );
    }

    /**
     * {@inheritDoc}
     */
    public double getDoubleValue() {
        try {
            return Double.parseDouble( getStringValue() );
        }
        catch ( NumberFormatException e ) {
            return 0;
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLongValueAt(int index) {
        try {
            return Long.parseLong( getStringValueAt(index) );
        }
        catch ( NumberFormatException e ) {
            return 0;
        }
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_STRING</code>.
     */
    public ImageMetaType getType() {
        return META_STRING;
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
    public void setDoubleValue( double newValue ) {
        setStringValueAt( Double.toString( newValue ), 0 );
    }

    /**
     * {@inheritDoc}
     */
    public void setLongValue( long newValue ) {
        setStringValueAt( Long.toString( newValue ), 0 );
    }

    /**
     * Sets the {@link String} value at the given index.
     *
     * @param newValue The new value.
     * @param index The index to set the value of.
     */
    public synchronized void setStringValueAt( String newValue, int index ) {
        checkIsEditable();
        if ( m_value == null )
            m_value = new String[ index + 1 ];
        else if ( index >= m_value.length )
            m_value = (String[])LCArrays.resize( m_value, index + 1 );
        m_value[ index ] = newValue;
        dirty();
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        final int length = readHeader( in );
        m_value = new String[ length ];
        for ( int i = 0; i < length; ++i )
            m_value[i] = in.readUTF();
    }

    /**
     * @serialData The header (see {@link #writeHeader(ObjectOutput)}) followed
     * by the UTF-8 encoding of the {@link String} values.
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        writeHeader( out );
        for ( String value : m_value )
            out.writeUTF( value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Parse and append a new value.
     *
     * @param newValue The new value.
     */
    protected void appendValueImpl( String newValue ) {
        if ( m_value == null )
            m_value = new String[]{ newValue };
        else {
            m_value = (String[])LCArrays.resize( m_value, m_value.length + 1 );
            m_value[ m_value.length - 1 ] = newValue;
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
        System.arraycopy( m_value, 0, value, 0, m_value.length );
        return value;
    }

    /**
     * Parse and set the values.
     *
     * @param newValue The array of new values.
     */
    protected void setValuesImpl( String[] newValue ) {
        if ( newValue == null ) {
            m_value = null;
            return;
        }
        if ( m_value == null || m_value.length != newValue.length )
            m_value = new String[ newValue.length ];
        System.arraycopy( newValue, 0, m_value, 0, newValue.length );
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
        final ImageMetadataDirectory owningDir = getOwningDirectory();
        final int tagID = getOwningTagID();
        final StringBuilder sb = new StringBuilder();
        boolean comma = false;
        for ( String value : m_value ) {
            if ( !comma )
                comma = true;
            else
                sb.append( ',' );
            if ( owningDir != null )
                value = owningDir.getTagValueLabelFor( tagID, value );
            sb.append( value );
        }
        return sb.toString();
    }

    ////////// private ////////////////////////////////////////////////////////

    private String[] m_value;
}
/* vim:set et sw=4 ts=4: */
