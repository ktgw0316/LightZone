/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.lightcrafts.image.metadata.ImageMetaType;

import static com.lightcrafts.image.metadata.ImageMetaType.META_USHORT;

/**
 * An <code>UnsignedShortMetaValue</code> is-a {@link LongMetaValue} for
 * holding an unsigned short (16-bit) metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class UnsignedShortMetaValue extends LongMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>UnsignedShortMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public UnsignedShortMetaValue() {
        // do nothing
    }

    /**
     * Construct an <code>UnsignedShortMetaValue</code>.
     *
     * @param values The array of values.
     */
    public UnsignedShortMetaValue( long... values ) {
        super( values );
    }

    /**
     * Construct an <code>UnsignedShortMetaValue</code>.
     *
     * @param values The array of values.
     */
    public UnsignedShortMetaValue( String... values ) {
        super( values );
    }

   /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_USHORT</code>.
     */
    public ImageMetaType getType() {
        return META_USHORT;
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        final int length = readHeader( in );
        m_value = new long[ length ];
        for ( int i = 0; i < length; ++i )
            m_value[i] = in.readInt() & 0x0000FFFF;
    }

    /**
     * @serialData The header (see {@link #writeHeader(ObjectOutput)}) followed
     * by the <code>int</code> values.
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        writeHeader( out );
        for ( long value : m_value )
            out.writeInt( (int)value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected long constrain( long value ) {
        return value < 0 ? 0 : value > 65536 ? 65536 : value;
    }

    /**
     * Parse an unsigned short from a {@link String}.
     *
     * @param newValue The {@link String} to parse.
     * @return Returns an unsigned integer in the range 0 to 65535.
     * @throws NumberFormatException if the {@link String} does not contain a
     * parsable unsigned short.
     */
    protected long parseValue( String newValue ) {
        final long n = Long.parseLong( newValue );
        if ( n < 0 || n > 65536 )
            throw new NumberFormatException();
        return n;
    }

}
/* vim:set et sw=4 ts=4: */
