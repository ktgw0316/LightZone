/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

import com.lightcrafts.image.metadata.ImageMetaType;
import com.lightcrafts.utils.Rational;

import static com.lightcrafts.image.metadata.ImageMetaType.META_URATIONAL;

/**
 * An <code>UnsignedRationalMetaValue</code> is-a {@link RationalMetaValue} for
 * holding an unsigned {@link Rational} metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class UnsignedRationalMetaValue extends RationalMetaValue {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>UnsignedRationalMetaValue</code>.
     * This constructor exists only for externalization.
     */
    public UnsignedRationalMetaValue() {
        // do nothing
    }

    /**
     * Construct an <code>UnsignedRationalMetaValue</code>.
     *
     * @param numerator The numerator.
     * @param denominator The denominator.  It must not be zero.
     */
    public UnsignedRationalMetaValue( int numerator, int denominator ) {
        super( numerator, denominator );
    }

    /**
     * Construct an <code>UnsignedRationalMetaValue</code>.
     *
     * @param values The array of values.
     */
    public UnsignedRationalMetaValue( Rational... values ) {
        super( values );
    }

    /**
     * Construct an <code>UnsignedRationalMetaValue</code>.
     *
     * @param values The array of values.
     */
    public UnsignedRationalMetaValue( String... values ) {
        super( values );
    }

    /**
     * Get the native {@link Rational} array values.
     *
     * @return Returns said array.
     */
    public Rational[] getUnsignedRationalValues() {
        return getRationalValues();
    }

    /**
     * Gets the type of this metadata value.
     *
     * @return Always returns <code>META_URATIONAL</code>.
     */
    public ImageMetaType getType() {
        return META_URATIONAL;
    }

}
/* vim:set et sw=4 ts=4: */
