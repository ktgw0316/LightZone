/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

/**
 * A <code>MultipageTIFFImageType</code> is-a {@link TIFFImageType} for
 * multipage TIFF images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class MultipageTIFFImageType extends LZNTIFFImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>MultipageTIFFImageType</code>. */
    @SuppressWarnings({"FieldNameHidesFieldInSuperclass"})
    public static final MultipageTIFFImageType INSTANCE =
        new MultipageTIFFImageType();

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return super.getName() + "-MULTI";
    }

    /**
     * {@inheritDoc}
     */
    public ExportOptions newExportOptions() {
        final ExportOptions options = super.newExportOptions();
        options.multilayer.setValue( true );
        return options;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>MultipageTIFFImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private MultipageTIFFImageType() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
