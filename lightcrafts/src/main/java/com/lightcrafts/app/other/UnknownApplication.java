/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.other;

/**
 * An <code>UnknownApplication</code> is-an {@link OtherApplication} to use
 * when the other application can not be identified.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class UnknownApplication extends OtherApplication {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance. */
    public static final UnknownApplication INSTANCE = new UnknownApplication();

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>UnknownApplication</code>.
     * The constructor is <code>private</code> so only the singleton instance
     * can be constructed.
     */
    private UnknownApplication() {
        super( "Unknown" );
    }

}
/* vim:set et sw=4 ts=4: */
