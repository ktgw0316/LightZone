/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

/**
 * A <code>ResizeHeightOption</code> is-an {@link IntegerExportOption} for
 * storing an integer value representing an image's resize width.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ResizeHeightOption extends IntegerExportOption {

    public static final String NAME = ResizeHeightOption.class.getName();

    /**
     * Construct a <code>ResizeHeightOption</code>.
     *
     * @param defaultValue The default value.
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public ResizeHeightOption( int defaultValue, ImageExportOptions options ) {
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
