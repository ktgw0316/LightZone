/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

/**
 * A <code>ResolutionOption</code> is-an {@link IntegerExportOption} for
 * storing the number of pixels per unit measurment for an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ResolutionOption extends IntegerExportOption {

    public static final int DEFAULT_VALUE = 300;

    public static final String NAME = ResolutionOption.class.getName();

    /**
     * Construct a <code>ResolutionOption</code>.
     *
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public ResolutionOption( ImageExportOptions options ) {
        this( DEFAULT_VALUE, options );
    }

    /**
     * Construct a <code>ResolutionOption</code>.
     *
     * @param defaultValue The default value.
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public ResolutionOption( int defaultValue, ImageExportOptions options ) {
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
