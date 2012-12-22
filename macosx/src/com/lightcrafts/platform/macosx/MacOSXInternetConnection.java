/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

/**
 * Checks whether this computer has an active internet connection.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class MacOSXInternetConnection {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Checks whether this computer has an active Internet connection.
     *
     * @param hostName The fully qualified name of the desired host to connect
     * to.
     * @return Returns <code>true</code> only if this computer currently has
     * an active internet connection and thus can reach the specified host.
     */
    public static native boolean hasConnectionTo( String hostName );

    ////////// private ////////////////////////////////////////////////////////

    static {
        System.loadLibrary( "MacOSX" );
    }
}
/* vim:set et sw=4 ts=4: */
