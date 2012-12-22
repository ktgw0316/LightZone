/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

/**
 * A <code>NumberUtil</code> is a set of utility functions for numbers.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class NumberUtil {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Round a <code>double</code> off to the tenths decimal place.
     *
     * @param n The <code>double</code> to round.
     * @return Returns a rounded <code>double</code>.
     */
    public static double tenths( double n ) {
        return (int)(n * 10) / 10.0;
    }

    /**
     * Convert a {@link Rational} to <code>double</code> but preserving the
     * fractional part only to the tenths decimal place.
     *
     * @param n The {@link Rational} to convert.
     * @return Returns said <code>double</code>.
     */
    public static double tenths( Rational n ) {
        return tenths( n.doubleValue() );
    }

}
/* vim:set et sw=4 ts=4: */
