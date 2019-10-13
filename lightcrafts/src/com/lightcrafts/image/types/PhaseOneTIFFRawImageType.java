/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

/**
 * A <code>PhaseOneTIFFRawImageType</code> is-a {@link RawImageType} for raw
 * images from the TODO
 * <p>
 * When such an image is detected, the image's type is retroactively changed to
 * this class so it's treated as a raw image.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class PhaseOneTIFFRawImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>PhaseOneTIFFRawImageType</code>. */
    public static final PhaseOneTIFFRawImageType INSTANCE =
        new PhaseOneTIFFRawImageType();

    /**
     * {@inheritDoc}
     */
    public String[] getExtensions() {
        return EXTENSIONS;
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return "PhaseOneTIFRaw";
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>PhaseOneTIFFRawImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private PhaseOneTIFFRawImageType() {
        // do nothing
    }

    /**
     * Even though this image type has a <code>tif</code> extension, the array
     * can not include it because it's already returned as one of the
     * extensions of {@link TIFFImageType}.  Therefore, just have an empty
     * array.
     */
    private static final String[] EXTENSIONS = {};
}
/* vim:set et sw=4 ts=4: */
