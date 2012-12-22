/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

/**
 * Launches the Mac OS X Help Viewer application to show our application's
 * help.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class MacOSXHelp {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Launches the Mac OS X Help Viewer application to show the application's
     * help and goes to a specific topic.
     *
     * @param topic The help topic to show or <code>null</code> to show the
     * cover page.
     */
    public static native void showHelpTopic( String topic );

    ////////// private ////////////////////////////////////////////////////////

    static {
        System.loadLibrary( "MacOSX" );
    }
}
/* vim:set et sw=4 ts=4: */
