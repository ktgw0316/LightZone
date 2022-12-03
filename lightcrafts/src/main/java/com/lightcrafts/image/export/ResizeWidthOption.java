/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

/**
 * A <code>ResizeWidthOption</code> is-an {@link IntegerExportOption} for
 * storing an integer value representing an image's resize width.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ResizeWidthOption extends IntegerExportOption {

    public static final String NAME = ResizeWidthOption.class.getName();

    /**
     * Construct a <code>ResizeWidthOption</code>.
     *
     * @param defaultValue The default value.
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public ResizeWidthOption( int defaultValue, ImageExportOptions options ) {
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
