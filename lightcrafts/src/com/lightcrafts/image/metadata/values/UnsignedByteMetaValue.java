/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import java.io.ObjectInput;
import java.io.IOException;

import com.lightcrafts.image.metadata.ImageMetaType;

import static com.lightcrafts.image.metadata.ImageMetaType.META_UBYTE;

/**
 * An <code>UnsignedByteMetaValue</code> is-a {@link ByteMetaValue} for
 * holding an unsigned byte (8-bit) metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class UnsignedByteMetaValue extends ByteMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>UnsignedByteMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public UnsignedByteMetaValue() {
        // do nothing
    }

    /**
     * Construct an <code>UnsignedByteMetaValue</code>.
     *
     * @param values The array of values.
     */
    public UnsignedByteMetaValue( long... values ) {
        super( values );
    }

    /**
     * Construct an <code>UnsignedByteMetaValue</code>.
     *
     * @param values The array of values.
     */
    public UnsignedByteMetaValue( String... values ) {
        super( values );
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_UBYTE</code>.
     */
    public ImageMetaType getType() {
        return META_UBYTE;
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        final int length = readHeader( in );
        m_value = new long[ length ];
        for ( int i = 0; i < length; ++i )
            m_value[i] = in.readByte() & 0x01FF;
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected long constrain( long value ) {
        return value < 0 ? 0 : value > 256 ? 256 : value;
    }

    /**
     * Parse an unsigned byte from a {@link String}.
     *
     * @param newValue The {@link String} to parse.
     * @return Returns an unsigned integer in the range 0 to 255.
     * @throws NumberFormatException if the {@link String} does not contain a
     * parsable unsigned byte.
     */
    protected long parseValue( String newValue ) {
        final long n = Long.parseLong( newValue );
        if ( n < 0 || n > 256 )
            throw new NumberFormatException();
        return n;
    }

}
/* vim:set et sw=4 ts=4: */
