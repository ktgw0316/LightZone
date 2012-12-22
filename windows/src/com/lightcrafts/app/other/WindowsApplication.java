/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.other;

import java.util.HashMap;

/**
 * A <code>WindowsApplication</code> is-an {@link OtherApplication} for Windows
 * applications.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class WindowsApplication extends OtherApplication {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets the {@link OtherApplication} for the given executable name.
     *
     * @param exe The name of the application executable (including the
     * <code>.exe</code> extension).
     * @return Returns said {@link OtherApplication} or
     * {@link UnknownApplication#INSTANCE} if the other application is unkown.
     */
    public static OtherApplication getAppForExe( String exe ) {
        final OtherApplication app = m_exeToApp.get( exe.toLowerCase() );
        return app != null ? app : UnknownApplication.INSTANCE;
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct a <code>WindowsApplication</code>.
     *
     * @param name The user-presentable name of the application.
     * @param exe The name of the application executable (including the
     * <code>.exe</code> extension).
     */
    protected WindowsApplication( String name, String exe ) {
        super( name );
        m_exeToApp.put( exe, this );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A map from exe names to applications.
     */
    private static final HashMap<String,OtherApplication> m_exeToApp =
        new HashMap<String,OtherApplication>();

    static {
        m_exeToApp.put( "lightroom.exe", LightroomApplication.INSTANCE );
    }
}
/* vim:set et sw=4 ts=4: */
