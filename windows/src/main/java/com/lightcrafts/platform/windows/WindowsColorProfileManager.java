/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

/**
 * A <code>WindowsColorProfileManager</code> is a class that is used to get
 * various color profiles.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class WindowsColorProfileManager {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets the path to the system display profile.
     *
     * @return Returns said path.
     */
    public static native String getSystemDisplayProfilePath();

    ////////// private ////////////////////////////////////////////////////////

    static {
        System.loadLibrary( "Windows" );
    }
}
/* vim:set et sw=4 ts=4: */
