/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

/**
 * An <code>BlackPointCompensationOption</code> is-an
 * {@link BooleanExportOption} for storing a boolean value representing whether
 * black-point compensation is enabled.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class BlackPointCompensationOption extends BooleanExportOption {

    public static final boolean DEFAULT_VALUE = false;

    public static final String NAME =
        BlackPointCompensationOption.class.getName();

    /**
     * Construct an <code>BlackPointCompensationOption</code>.
     *
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public BlackPointCompensationOption( ImageExportOptions options ) {
        super( NAME, DEFAULT_VALUE, options );
    }
}
/* vim:set et sw=4 ts=4: */
