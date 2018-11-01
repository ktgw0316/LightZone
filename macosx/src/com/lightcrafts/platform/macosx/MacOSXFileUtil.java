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
     * Moves a set of files to the Trash.
     *
     * @param pathNames An array of the file(s) to move.  The file name(s) must
     * all be full paths.
     * @return Returns <code>true</code> only if the move succeeded.
     */
    public static boolean moveToTrash( String[] pathNames ) {
        final HashMap<String,HashSet<String>> dirFilesMap =
            new HashMap<String,HashSet<String>>();

        //
        // Because the native Cocoa API for moving files to the Trash allows
        // only files in the same directory to me moved together, we first have
        // to create a map where each key is a unique directory and each value
        // is a set of file(s) in that directory.
        //
        for ( String pathName : pathNames ) {
            final File file = new File( pathName );
            final String dir = file.getParent();
            HashSet<String> filesInDir = dirFilesMap.get( dir );
            if ( filesInDir == null ) {
                filesInDir = new HashSet<String>();
                dirFilesMap.put( dir, filesInDir );
            }
            filesInDir.add( file.getName() );
        }

        //
        // Given the map, we now call the native moveToTrash() repeatedly, once
        // for each unique directory.
        //
        for ( Map.Entry<String,HashSet<String>> me : dirFilesMap.entrySet() ) {
            final String dir = me.getKey();
            final String[] filesInDir = me.getValue().toArray( new String[0] );
            if ( !moveToTrash( dir, filesInDir ) )
                return false;
        }
        return true;
    }

    /**
     * Resolve a Mac OS X alias file.
     *
     * @param path The full path of a file that may be an alias to resolve.
     * @return Returns the resolved path (or the original path if it didn't
     * refer to an alias) or <code>null</code> if there was an error.
     */
    public static native String resolveAlias( String path );

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Moves a set of files to the Trash.
     *
     * @param directory The full path of the directory containing the file(s)
     * to move to the Trash.
     * @param files An array of the file(s) to move.  These must be file names
     * only and contain no path information.  All the files must be in the
     * given directory.
     * @return Returns <code>true</code> only if the move succeeded.
     */
    private static native boolean moveToTrash( String directory,
                                               String[] files );

    static {
        System.loadLibrary( "MacOSX" );
    }
}
/* vim:set et sw=4 ts=4: */
