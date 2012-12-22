/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

/**
 * A <code>SidecarTIFFImageType</code> is-a {@link LZNTIFFImageType} for
 * sidecar TIFF images.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class SidecarTIFFImageType extends LZNTIFFImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>SidecarTIFFImageType</code>. */
    @SuppressWarnings({"FieldNameHidesFieldInSuperclass"})
    public static final SidecarTIFFImageType INSTANCE =
        new SidecarTIFFImageType();

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return super.getName() + "-LZN";
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>SidecarTIFFImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private SidecarTIFFImageType() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
