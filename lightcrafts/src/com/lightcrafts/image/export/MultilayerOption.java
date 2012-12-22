/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

/**
 * A <code>MultilayerOption</code> is-a {@link BooleanExportOption} for
 * storing a boolean value representing whether multilayer TIFF is enabled.
 *
 * @author Anton Kast [anton@lightcrafts.com]
 */
public final class MultilayerOption extends BooleanExportOption {

    public static final boolean DEFAULT_VALUE = false;

    public static final String NAME = MultilayerOption.class.getName();

    /**
     * Construct an <code>MultilayerOption</code>.
     *
     * @param defaultValue The default value.
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    public MultilayerOption( boolean defaultValue,
                             ImageExportOptions options ) {
        super( NAME, defaultValue, options );
    }
}
/* vim:set et sw=4 ts=4: */
