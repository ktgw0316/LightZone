/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import java.awt.color.ICC_Profile;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.image.color.ColorProfileInfo;

/**
 * A <code>ColorProfileOption</code> is-an {@link IntegerExportOption} for
 * storing the name of the color profile for an image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ColorProfileOption extends StringExportOption {

    /** The default color profile. */
    public static final ICC_Profile DEFAULT_PROFILE =
        JAIContext.sRGBExportColorProfile;

    /** The name of the default color profile. */
    public static final String DEFAULT_PROFILE_NAME =
        ColorProfileInfo.getNameOf( DEFAULT_PROFILE );

    public static final String NAME = ColorProfileOption.class.getName();

    /**
     * Construct a <code>ColorProfileOption</code>.
     *
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public ColorProfileOption( ImageExportOptions options ) {
        super( NAME, DEFAULT_PROFILE_NAME, options );
    }
}
/* vim:set et sw=4 ts=4: */
