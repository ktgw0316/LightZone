/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import java.io.File;

import com.lightcrafts.utils.file.SmartFolder;
import com.lightcrafts.utils.LCArrays;

/**
 * A <code>MacOSXSmartFolder</code> is-a {@link SmartFolder} that performs the
 * smart queries contained in <code>.savedSearch</code> files.  This has the
 * effect of making a <code>MacOSXSmartFolder</code> appear to Java as an
 * ordinary directory.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class MacOSXSmartFolder extends SmartFolder {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>MacOSXSmartFolder</code>.
     *
     * @param pathname The abstract pathname.
     */
    public MacOSXSmartFolder( String pathname ) {
        super( pathname );
    }

    /**
     * Construct a <code>MacOSXSmartFolder</code>.
     *
     * @param file The original {@link File}.
     */
    public MacOSXSmartFolder( File file ) {
        super( file.getAbsolutePath() );
    }

    /**
     * Construct a <code>MacOSXSmartFolder</code>.
     *
     * @param parent The parent {@link File}.
     * @param child The child filename.
     */
    public MacOSXSmartFolder( File parent, String child ) {
        super( parent, child );
    }

    /**
     * Construct a <code>MacOSXSmartFolder</code>.
     *
     * @param parent The parent abstract pathname.
     * @param child The child filename.
     */
    public MacOSXSmartFolder( String parent, String child ) {
        super( parent, child );
    }

    /**
     * Tests whether the file denoted by this abstract pathname is either an
     * ordinary directory or a "Smart Folder".
     *
     * @return Returns <code>true</code> only if the file denoted by this
     * abstract pathname is either an ordinary directory or a "Smart Folder".
     */
    public boolean isDirectory() {
        return isSmartFolder() || super.isDirectory();
    }

    /**
     * Tests whether the file denoted by this absract pathname is not a
     * "Smart Folder" and an ordinary file.
     *
     * @return Returns <code>true</code> only if the file denoted by this
     * absract pathname is not a "Smart Folder" and an ordinary file.
     */
    public boolean isFile() {
        return !isSmartFolder() && super.isFile();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSmartFolder() {
        return isSmartFolder( this );
    }

    /**
     * Tests whether the pathname denoted by the given {@link File} actually is
     * a Mac&nbsp;OS&nbsp;X "Smart Folder".
     *
     * @return Returns <code>true</code> only if it is.
     */
    public static boolean isSmartFolder( File path ) {
        return isSmartFolder( path.getAbsolutePath() );
    }

    /**
     * Tests whether the given pathname actually is a Mac&nbsp;OS&nbsp;X "Smart
     * Folder".
     *
     * @return Returns <code>true</code> only if it is.
     */
    public static boolean isSmartFolder( String path ) {
        return path.endsWith( ".savedSearch" );
    }

    /**
     * Gets an array of strings naming the files and directories in the
     * directory denoted by this abstract pathname.  When the pathname refers
     * to a "Smart Folder", its query is performed and the results of said
     * query are returned.  In that case, the paths are fulll paths because
     * files satisfying the query can be anywhere on the filesystem.
     *
     * @return Returns an array of strings naming the files and directories in
     * the directory or the results of the smart query denoted by this abstract
     * pathname.  The array will be empty if the directory is empty.  Returns
     * <code>null</code> if this abstract pathname does not denote a directory,
     * or if an I/O error occurs.
     */
    public String[] list() {
        final String path = getAbsolutePath();
        if ( !isSmartFolder( path ) )
            return super.list();
        return getQueryResults( path );
    }

    /**
     * Gets an array of abstract pathnames denoting the files in the
     * directory denoted by this abstract pathname.  When the pathname refers
     * to a "Smart Folder", its query is performed and the results of said
     * query are returned.
     *
     * @return Returns an array of abstract pathnames denoting the files and
     * directories in the directory or the results of the smart query denoted
     * by this abstract pathname.  The array will be empty if the directory is
     * empty.  Returns <code>null</code> if this abstract pathname does not
     * denote a directory, or if an I/O error occurs.
     */
    public File[] listFiles() {
        final String path = getAbsolutePath();
        if ( !isSmartFolder( path ) )
            return super.listFiles();
        final String[] contents = getQueryResults( path );
        //
        // At first glance, it might seem that this method doesn't need to be
        // overridden at all since File.listFiles() is pretty much the same.
        // However, it does need to be overridden because File.listFiles()
        // assumes the results from list() are relative paths and so conjoins
        // each to have this File as its parent.  That's wrong since the
        // results from our overridden list() returns absolute paths.
        //
        final int length = contents.length;
        final File[] files = new File[ length ];
        for ( int i = 0; i < length; ++i )
            files[i] = new File( contents[i] );
        return files;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Gets the virtual contents of a "Smart Folder."
     *
     * @param savedSearchPath The full path to a <code>.savedSearch</code>
     * file.
     * @return Returns an array of full pathnames for the files that match the
     * query or <code>null</code> if there was an error.
     */
    private static String[] getQueryResults( String savedSearchPath ) {
        final String[] contents = smartQuery( savedSearchPath );
        //
        // Since the array may contain trailing null elements, we have to see
        // if there are any and remove them.
        //
        int actualLength = contents.length;
        for ( int i = contents.length - 1; i >= 0; --i )
            if ( contents[i] == null )
                --actualLength;
            else
                break;
        return (String[])LCArrays.resize( contents, actualLength );
    }

    /**
     * Performs a smart query to get the virtual contents of a "Smart Folder."
     *
     * @param savedSearchPath The full path to a <code>.savedSearch</code>
     * file.
     * @return Returns an array of full pathnames for the files that match the
     * query or <code>null</code> if there was an error.  Note that the array
     * may contain trailing <code>null</code> elements.
     */
    private static native String[] smartQuery( String savedSearchPath );

    static {
        System.loadLibrary( "MacOSX" );
    }

    ////////// main() for testing /////////////////////////////////////////////

    public static void main( String[] args ) {
        final File smartFolder = new MacOSXSmartFolder( args[0] );
        final String[] contents = smartFolder.list();
        if ( contents != null )
            for ( String file : contents )
                System.out.println( file );
    }
}
/* vim:set et sw=4 ts=4: */
