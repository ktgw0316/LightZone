/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

/**
 * A <code>BitsPerChannelOption</code> is-an {@link IntegerExportOption} for
 * storing the number of bits-per-channel for an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class BitsPerChannelOption extends IntegerExportOption {

    public static final int DEFAULT_VALUE = 8;

    public static final String NAME = BitsPerChannelOption.class.getName();

    /**
     * Construct a <code>BitsPerChannelOption</code>.
     *
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public BitsPerChannelOption( ImageExportOptions options ) {
        super( NAME, DEFAULT_VALUE, options );
    }

    /**
     * Construct a <code>BitsPerChannelOption</code>.
     *
     * @param defaultValue The default value.
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public BitsPerChannelOption( int defaultValue,
                                 ImageExportOptions options ) {
        super( NAME, defaultValue, options );
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLegalValue( int value ) {
        return value == 8 || value == 16;
    }

}
/* vim:set et sw=4 ts=4: */
