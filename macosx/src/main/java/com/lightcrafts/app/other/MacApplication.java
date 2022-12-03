/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.other;

import java.util.HashMap;

/**
 * A <code>MacApplication</code> is-an {@link OtherApplication} for Mac OS X
 * applications.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class MacApplication extends OtherApplication {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets the {@link OtherApplication} for the given Macintosh application
     * bundle signature (creator code).
     *
     * @param bundleSig The bundle signature for the application.
     * @return Returns said {@link OtherApplication} or
     * {@link UnknownApplication#INSTANCE} if the other application is unkown.
     */
    public static OtherApplication getAppForSignature( String bundleSig ) {
        final OtherApplication app = m_bundleSigToApp.get( bundleSig );
        return app != null ? app : UnknownApplication.INSTANCE;
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct a <code>MacApplication</code>.
     *
     * @param name The user-presentable name of the application.
     * @param bundleSig The bundle signature for the application.
     */
    protected MacApplication( String name, String bundleSig ) {
        super( name );
        m_bundleSigToApp.put( bundleSig, this );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A map from bundle signatures to applications.
     */
    private static final HashMap<String,OtherApplication> m_bundleSigToApp =
        new HashMap<String,OtherApplication>();

    static {
        //
        // Load the cross-platform applications.
        //
        m_bundleSigToApp.put( "AgHg", LightroomApplication.INSTANCE );

        //noinspection UNUSED_SYMBOL
        final OtherApplication appsToLoad[] = {
            ApertureApplication.INSTANCE,
            //FinderApplication.INSTANCE,
            iPhotoApplication.INSTANCE,
        };
    }
}
/* vim:set et sw=4 ts=4: */
