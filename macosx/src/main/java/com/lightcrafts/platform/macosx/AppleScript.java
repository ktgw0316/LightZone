/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

/**
 * Run AppleScripts on Mac OS X.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class AppleScript {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Bring the given application to the front.
     *
     * @param appName The name of the application.
     */
    public static void bringAppToFront( String appName ) {
        run( "tell application \"" + appName + "\" to activate" );
    }

    /**
     * Runs an AppleScript.
     *
     * @param script The AppleScript to run.  Note: for multi-line scripts,
     * don't forget to seperate lines with newlines!
     * @throws IllegalArgumentException if the script contains errors.
     */
    public static native void run( String script );

    ////////// private ////////////////////////////////////////////////////////

    /**
     * For testing.
     */
    public static void main( String[] args ) throws Exception {
        final String script =
            "set unixFile to do shell script \"echo ~\"\n" +
            "set theFile to POSIX file unixFile as alias\n" +
            "tell application \"Finder\"\n" +
            "    activate\n" +
            "    reveal theFile\n" +
            "end tell";
        run( script );
        Thread.sleep( 5000 );
    }

    static {
        System.loadLibrary( "MacOSX" );
    }
}
/* vim:set et sw=4 ts=4: */
