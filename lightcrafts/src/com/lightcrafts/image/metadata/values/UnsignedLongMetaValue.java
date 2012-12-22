/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import com.lightcrafts.image.metadata.ImageMetaType;

import static com.lightcrafts.image.metadata.ImageMetaType.META_ULONG;

/**
 * An <code>UnsignedLongMetaValue</code> is-a {@link LongMetaValue} for
 * holding an unsigned long (32-bit) metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class UnsignedLongMetaValue extends LongMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>UnsignedLongMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public UnsignedLongMetaValue() {
        // do nothing
    }

    /**
     * Construct an <code>UnsignedLongMetaValue</code>.
     *
     * @param values The array of values.
     */
    public UnsignedLongMetaValue( long... values ) {
        super( values );
    }

    /**
     * Construct an <code>UnsignedLongMetaValue</code>.
     *
     * @param values The array of values.
     */
    public UnsignedLongMetaValue( String... values ) {
        super( values );
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_ULONG</code>.
     */
    public ImageMetaType getType() {
        return META_ULONG;
    }

}
/* vim:set et sw=4 ts=4: */
