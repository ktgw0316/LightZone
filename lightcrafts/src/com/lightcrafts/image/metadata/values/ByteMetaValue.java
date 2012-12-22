/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.lightcrafts.image.metadata.ImageMetaType;

import static com.lightcrafts.image.metadata.ImageMetaType.META_SBYTE;

/**
 * A <code>ByteMetaValue</code> is-a {@link ShortMetaValue} for holding a
 * signed byte (8-bit) metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class ByteMetaValue extends ShortMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>ByteMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public ByteMetaValue() {
        // do nothing
    }

    /**
     * Construct a <code>ByteMetaValue</code>.
     *
     * @param values The array of values.
     */
    public ByteMetaValue( long... values ) {
        super( values );
    }

    /**
     * Construct a <code>ByteMetaValue</code>.
     *
     * @param values The array of values.
     */
    public ByteMetaValue( String... values ) {
        super( values );
    }

    /**
     * Gets the native <code>byte</code> array value.
     *
     * @return Returns said array.
     */
    public byte[] getByteValues() {
        final long[] values = getLongValues();
        final byte[] temp = new byte[ values.length ];
        for ( int i = 0; i < temp.length; ++i )
            temp[i] = (byte)values[i];
        return temp;
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_BYTE</code>.
     */
    public ImageMetaType getType() {
        return META_SBYTE;
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        final int length = readHeader( in );
        m_value = new long[ length ];
        for ( int i = 0; i < length; ++i )
            m_value[i] = in.readByte();
    }

    /**
     * @serialData The header (see {@link #writeHeader(ObjectOutput)}) followed
     * by the <code>byte</code> values.
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        writeHeader( out );
        for ( long value : m_value )
            out.writeByte( (byte)value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected long constrain( long value ) {
        return (byte)value;
    }

    /**
     * Parse a signed byte from a {@link String}.
     *
     * @param newValue The {@link String} to parse.
     * @return Returns a signed integer in the range -128 to 127.
     * @throws NumberFormatException if the {@link String} does not contain a
     * parsable signed byte.
     */
    protected long parseValue( String newValue ) {
        return Byte.parseByte( newValue );
    }

}
/* vim:set et sw=4 ts=4: */
