/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.lightcrafts.image.metadata.ImageMetaType;

import static com.lightcrafts.image.metadata.ImageMetaType.META_SSHORT;

/**
 * A <code>ShortMetaValue</code> is-a {@link LongMetaValue} for holding a
 * signed short (16-bit) metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class ShortMetaValue extends LongMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>ShortMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public ShortMetaValue() {
        // do nothing
    }

    /**
     * Construct a <code>ShortMetaValue</code>.
     *
     * @param values The array of values.
     */
    public ShortMetaValue( long... values ) {
        super( values );
    }

    /**
     * Construct a <code>ShortMetaValue</code>.
     *
     * @param values The array of values.
     */
    public ShortMetaValue( String... values ) {
        super( values );
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_SHORT</code>.
     */
    public ImageMetaType getType() {
        return META_SSHORT;
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal( ObjectInput in ) throws IOException {
        final int length = readHeader( in );
        m_value = new long[ length ];
        for ( int i = 0; i < length; ++i )
            m_value[i] = in.readShort();
    }

    /**
     * @serialData The header (see {@link #writeHeader(ObjectOutput)}) followed
     * by the <code>short</code> values.
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        writeHeader( out );
        for ( long value : m_value )
            out.writeShort( (short)value );
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected long constrain( long value ) {
        return (short)value;
    }

    /**
     * Parse a signed short from a {@link String}.
     *
     * @param newValue The {@link String} to parse.
     * @return Returns a signed integer in the range -32768 to 32767.
     * @throws NumberFormatException if the {@link String} does not contain a
     * parsable signed short.
     */
    protected long parseValue( String newValue ) {
        return Short.parseShort( newValue );
    }

}
/* vim:set et sw=4 ts=4: */
