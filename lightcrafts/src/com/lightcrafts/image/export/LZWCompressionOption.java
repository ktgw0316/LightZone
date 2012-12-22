/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

/**
 * An <code>LZWCompressionOption</code> is-an {@link BooleanExportOption} for
 * storing a boolean value representing whether LZW compression is enabled.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class LZWCompressionOption extends BooleanExportOption {

    public static final String NAME = LZWCompressionOption.class.getName();

    /**
     * Construct an <code>LZWCompressionOption</code>.
     *
     * @param defaultValue The default value.
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public LZWCompressionOption( boolean defaultValue,
                                 ImageExportOptions options ) {
        super( NAME, defaultValue, options );
    }
}
/* vim:set et sw=4 ts=4: */
