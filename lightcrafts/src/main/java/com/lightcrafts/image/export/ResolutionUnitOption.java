/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import static com.lightcrafts.image.metadata.providers.ResolutionProvider.*;

/**
 * A <code>ResolutionUnitOption</code> is-an {@link IntegerExportOption} for
 * storing the resolution unit.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ResolutionUnitOption extends IntegerExportOption {

    public static final int DEFAULT_VALUE = RESOLUTION_UNIT_INCH;

    public static final String NAME = ResolutionUnitOption.class.getName();

    /**
     * Construct a <code>ResolutionUnitOption</code>.
     *
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public ResolutionUnitOption( ImageExportOptions options ) {
        this( DEFAULT_VALUE, options );
    }

    /**
     * Construct a <code>ResolutionUnitOption</code>.
     *
     * @param defaultValue The default value.
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public ResolutionUnitOption( int defaultValue,
                                 ImageExportOptions options ) {
        super( NAME, defaultValue, options );
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLegalValue( int value ) {
        switch ( value ) {
            case RESOLUTION_UNIT_NONE:
            case RESOLUTION_UNIT_INCH:
            case RESOLUTION_UNIT_CM:
                return true;
            default:
                return false;
        }
    }

}
/* vim:set et sw=4 ts=4: */
