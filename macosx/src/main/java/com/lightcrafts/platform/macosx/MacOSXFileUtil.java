/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

/**
 * Mac OS X file utilities.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class MacOSXFileUtil {

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
