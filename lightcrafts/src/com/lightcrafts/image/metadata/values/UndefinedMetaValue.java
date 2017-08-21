/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.UnsupportedEncodingException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.lightcrafts.image.metadata.ImageMetaType;

import static com.lightcrafts.image.metadata.ImageMetaType.META_UNDEFINED;

/**
 * An <code>UndefinedMetaValue</code> is-an {@link ImageMetaValue} for holding
 * an &quot;undefined&quot; binary metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class UndefinedMetaValue extends ImageMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>UndefinedMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public UndefinedMetaValue() {
        // do nothing
    }

    /**
     * Construct an <code>UndefinedMetaValue</code>.
     *
     * @param value The array of values.
     */
    public UndefinedMetaValue( byte[] value ) {
        m_value = value;
        setNonDisplayable();
    }

    /**
     * {@inheritDoc}
     */
    public UndefinedMetaValue clone() {
        final UndefinedMetaValue copy = (UndefinedMetaValue)super.clone();
        copy.m_value = m_value.clone();
        return copy;
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     *
     * @return Never returns.
     */
    public long getLongValueAt(int index) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_UNDEFINED</code>.
     */
    public ImageMetaType getType() {
        return META_UNDEFINED;
    }

    /**
     * Get the native <code>byte</code> array value.
     *
     * @return Returns said array.
     */
    public byte[] getUndefinedValue() {
        return m_value;
    }

    /**
     * {@inheritDoc}
     */
    public int getValueCount() {
        return m_value != null ? m_value.length : 0;
    }

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    public void setLongValue( long newValue ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Element toXMP( Document xmpDoc, String nsURI, String prefix ) {
        //
        // We currently don't support conversion to XMP of undefined meta
        // values.
        //
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        m_value = new byte[ readHeader( in ) ];
        in.readFully( m_value );
    }

    /**
     * @serialData The header (see {@link #writeHeader(ObjectOutput)}) followed
     * by the <code>byte</code> values.
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        writeHeader( out );
        out.write( m_value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Always throws {@link UnsupportedOperationException}.
     */
    protected void appendValueImpl( String newValue ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the values as an array of 1 {@link String} comprised of all the
     * bytes in the undefined value.
     *
     * @return Returns said array.
     */
    protected String[] getValuesImpl() {
        if ( m_value == null )
            return null;
        try {
            //
            // The use of ISO 8859-1 is a reasonable assumption since (a) it's
            // upwards compatible with ASCII and (b) camera manufacturers
            // probably aren't using UTF-8 (which is the only other reasonable
            // choice).
            //
            return new String[]{ new String( m_value, "UTF-8" ) };
        }
        catch ( UnsupportedEncodingException e ) {
            throw new IllegalStateException( e );
        }
    }

    /**
     * Sets the internal byte array to the bytes of the contatenated strings.
     *
     * @param newValue The array of new values.
     */
    protected void setValuesImpl( String[] newValue ) {
        final String s;
        if ( newValue.length > 1 ) {
            final StringBuilder sb = new StringBuilder();
            for ( String i : newValue )
                sb.append( i );
            s = sb.toString();
        } else
            s = newValue[0];
        try {
            m_value = s.getBytes( "UTF-8" );
        }
        catch ( UnsupportedEncodingException e ) {
            throw new IllegalStateException( e );
        }
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
        for ( byte value : m_value ) {
            if ( !comma )
                comma = true;
            else
                sb.append( ',' );
            sb.append( Integer.toString( value & 0x000000FF ) );
        }
        return sb.toString();
    }

    ////////// private ////////////////////////////////////////////////////////

    private byte[] m_value;
}
/* vim:set et sw=4 ts=4: */
