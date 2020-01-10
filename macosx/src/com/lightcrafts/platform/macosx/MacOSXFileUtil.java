/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import java.io.File;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Mac OS X file utilities.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class MacOSXFileUtil {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Given a NIB file's name, get its full path.
     *
     * @param nibFilename The name of the NIB file.
     * @return Returns the full path to the NIB file.
     */
    public static String getNIBPathOf( String nibFilename ) {
        try {
            //
            // This works only when the application is started from a bundle
            // and not from either the command-line or an IDE.
            //
            Class fileManager = Class.forName( "com.apple.eio.FileManager" );
            Method getResource = fileManager.getDeclaredMethod( "getResource", new Class[] {String.class} );
            return getResource.invoke( null, nibFilename ).toString();
        }
        catch ( Exception e ) {
            //
            // Failing the above, assume that the working directory is the root
            // of the source tree and use a path relative to that.
            //
            return nibFilename;
        }
    }

    /**
     * Checks whether the given path refers to a Mac OS X alias file.
     *
     * @param path The full path of the file to check.
     * @return Returns <code>true</code> only if the file is an alias file.
     */
    public static native boolean isAlias( String path );

    /**
     * Resolve a Mac OS X alias file.
     *
     * @param path The full path of a file that may be an alias to resolve.
     * @return Returns the resolved path (or the original path if it didn't
     * refer to an alias) or <code>null</code> if there was an error.
     */
    public static native String resolveAlias( String path );

    static {
        System.loadLibrary( "MacOSX" );
    }
}
/* vim:set et sw=4 ts=4: */
