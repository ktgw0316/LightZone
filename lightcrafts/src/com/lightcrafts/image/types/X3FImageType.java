/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

/**
 * A <code>X3FImageType</code> is-a {@link RawImageType} for X3F (Sigma raw)
 * images.
 *
 * @author Fabio Riccardi [fabio@lightcrafts.com]
 */
public final class X3FImageType extends RawImageType {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance of <code>X3FImageType</code>. */
    public static final X3FImageType INSTANCE = new X3FImageType();

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
        return "X3F";
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>X3FImageType</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private X3FImageType() {
        // do nothing
    }

    /**
     * All the possible filename extensions for X3F files.  All must be lower
     * case and the preferred one must be first.
     */
    private static final String[] EXTENSIONS = {
            "x3f"
    };
}
/* vim:set et sw=4 ts=4: */
