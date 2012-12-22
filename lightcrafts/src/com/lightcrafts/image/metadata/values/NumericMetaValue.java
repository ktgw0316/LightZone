/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.values;

/**
 * A <code>NumericMetaValue</code> is-an {@link ImageMetaValue} for holding a
 * numeric metadata value.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class NumericMetaValue extends ImageMetaValue {

    /**
     * Returns whether this image metadata value is numeric.
     *
     * @return Always returns <code>true</code>.
     */
    public final boolean isNumeric() {
        return true;
    }

}
/* vim:set et sw=4 ts=4: */
