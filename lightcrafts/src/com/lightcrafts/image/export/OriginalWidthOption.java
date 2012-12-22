/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

/**
 * A <code>OriginalWidthOption</code> is-an {@link IntegerExportOption} for
 * storing an integer value representing an image's original width.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class OriginalWidthOption extends IntegerExportOption {

    public static final String NAME = OriginalWidthOption.class.getName();

    /**
     * Construct a <code>OriginalWidthOption</code>.
     *
     * @param defaultValue The default value.
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public OriginalWidthOption( int defaultValue, ImageExportOptions options ) {
        super( NAME, defaultValue, options );
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLegalValue( int value ) {
        return value > 0;
    }

}
/* vim:set et sw=4 ts=4: */
