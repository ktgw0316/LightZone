/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

/**
 * Checks whether this computer has an active internet connection.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class WindowsInternetConnection {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Checks whether this computer has an active Internet connection.
     *
     * @return Returns <code>true</code> only if this computer currently has
     * an active internet connection.
     */
    public static native boolean hasConnection();

    ////////// private ////////////////////////////////////////////////////////

    static {
        System.loadLibrary( "Windows" );
    }
}
/* vim:set et sw=4 ts=4: */
