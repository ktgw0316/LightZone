/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import java.awt.color.ICC_Profile;

import static java.awt.color.ICC_Profile.*;

/**
 * A <code>RenderingIntentOption</code> is-an {@link IntegerExportOption} for
 * storing an integer value representing the rendering intent.  The legal
 * integer values are: {@link ICC_Profile#icAbsoluteColorimetric},
 * {@link ICC_Profile#icPerceptual},
 * {@link ICC_Profile#icRelativeColorimetric}, and
 * {@link ICC_Profile#icSaturation}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class RenderingIntentOption extends IntegerExportOption {

    public static final int DEFAULT_VALUE = icPerceptual;

    public static final String NAME = RenderingIntentOption.class.getName();

    /**
     * Construct a <code>RenderingIntentOption</code> having the default
     * rendering intent of {@link ICC_Profile#icPerceptual}.
     *
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public RenderingIntentOption( ImageExportOptions options ) {
        super( NAME, DEFAULT_VALUE, options );
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLegalValue( int value ) {
        switch ( value ) {
            case icAbsoluteColorimetric:
            case icPerceptual:
            case icRelativeColorimetric:
            case icSaturation:
                return true;
            default:
                return false;
        }
    }

}
/* vim:set et sw=4 ts=4: */
