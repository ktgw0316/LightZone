/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.windows;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import com.lightcrafts.utils.file.SmartFolder;

/**
 * A <code>WindowsSavedSearch</code> is-a {@link SmartFolder} that performs the
 * saved search contained in <code>.search-ms</code> files.  This has the
 * effect of making a <code>WindowsSavedSearch</code> appear to Java as an
 * ordinary directory.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class WindowsSavedSearch extends SmartFolder {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>WindowsSavedSearch</code>.
     *
     * @param pathname The abstract pathname.
     */
    public WindowsSavedSearch( String pathname ) {
        super( pathname );
    }

    /**
     * Construct a <code>WindowsSavedSearch</code>.
     *
     * @param file The original {@link File}.
     */
    public WindowsSavedSearch( File file ) {
        super( file.getAbsolutePath() );
    }

    /**
     * Construct a <code>WindowsSavedSearch</code>.
     *
     * @param parent The parent {@link File}.
     * @param child The child filename.
     */
    public WindowsSavedSearch( File parent, String child ) {
        super( parent, child );
    }

    /**
     * Construct a <code>WindowsSavedSearch</code>.
     *
     * @param parent The parent abstract pathname.
     * @param child The child filename.
     */
    public WindowsSavedSearch( String parent, String child ) {
        super( parent, child );
    }

    /**
     * Tests whether the file denoted by this abstract pathname is either an
     * ordinary directory or a "Saved Search".
     *
     * @return Returns <code>true</code> only if the file denoted by this
     * abstract pathname is either an ordinary directory or a "Saved Search".
     */
    public boolean isDirectory() {
        return isSmartFolder() || super.isDirectory();
    }

    /**
     * Tests whether the file denoted by this absract pathname is not a
     * "Saved Search" and an ordinary file.
     *
     * @return Returns <code>true</code> only if the file denoted by this
     * absract pathname is not a "Saved Search" and an ordinary file.
     */
    public boolean isFile() {
        return !isSmartFolder() && super.isFile();
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSmartFolder() {
        return isSavedSearch( this );
    }

    /**
     * Tests whether the pathname denoted by the given {@link File} actually is
     * a Windows Vista "Saved Search".
     *
     * @return Returns <code>true</code> only if it is.
     */
    public static boolean isSavedSearch( File path ) {
        return isSavedSearch( path.getName() );
    }

    /**
     * Tests whether the given pathname actually is a Windows Vista "Saved
     * Search".
     *
     * @return Returns <code>true</code> only if it is.
     */
    public static boolean isSavedSearch( String path ) {
        return path.endsWith( ".searchconnector-ms" ) || path.endsWith( ".search-ms" );
    }

    /**
     * Gets an array of strings naming the files and directories in the
     * directory denoted by this abstract pathname.  When the pathname refers
     * to a "Saved Search", its query is performed and the results of said
     * query are returned.
     *
     * @return Returns an array of strings naming the files and directories in
     * the directory or the results of the saved search denoted by this
     * abstract pathname.  The array will be empty if the directory is empty.
     * Returns <code>null</code> if this abstract pathname does not denote a
     * directory, or if an I/O error occurs.
     */
    public String[] list() {
        return  isSmartFolder() ?
                savedSearch( getAbsolutePath() ) : super.list();
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
        if ( !isSmartFolder() )
            return super.listFiles();
        final String[] contents = savedSearch( getAbsolutePath() );
        final File[] files = new File[ contents.length ];
        for ( int i = 0; i < contents.length; ++i )
            files[i] = new File( contents[i] );
        return files;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Perform a saved search.
     *
     * @param savedSearchPathname The full path to a <code>.search-ms</code>
     * file.
     * @return Returns an array of strings naming the files and directories in
     * the directory or the results of the saved search denoted by this
     * abstract pathname.  The array will be empty if the directory is empty.
     * Returns <code>null</code> if this abstract pathname does not denote a
     * directory, or if an I/O error occurs.
     */
    private String[] savedSearch( String savedSearchPathname ) {
        final long nativePtr;
        try {
            nativePtr = beginSearch( savedSearchPathname );
        }
        catch (UnsupportedEncodingException e) {
            return null;
        }
        if ( nativePtr == 0 )
            return null;
        try {
            final ArrayList<String> contents = new ArrayList<String>();
            while ( true ) {
                final String next = getNextResult( nativePtr );
                if ( next == null )
                    break;
                contents.add( next );
            }
            return contents.toArray( new String[0] );
        }
        finally {
            endSearch( nativePtr );
        }
    }

    /**
     * Begin a saved search to get the virtual contents of a "Saved Search."
     *
     * @param savedSearchPathname The full path to a <code>.search-ms</code>
     * file.
     * @return Returns a native pointer to a C++ object that is to be treaded
     * as opaque from Java and passed back to {@link #getNextResult(long)} and
     * {@link #endSearch(long)}.
     */
    private static long beginSearch( String savedSearchPathname )
        throws UnsupportedEncodingException
    {
        byte[] savedSearchPathnameUtf8 = ( savedSearchPathname+ '\000' ).getBytes( "UTF-8" );
        return beginSearch( savedSearchPathnameUtf8 );
    }

    private static native long beginSearch( byte[] savedSearchPathnameUtf8 );

    /**
     * End the saved search.  This must be called to dispose of native
     * resources.
     *
     * @param nativePtr The native pointer returned by
     * {@link #beginSearch(String)}.
     */
    private static native void endSearch( long nativePtr );

    /**
     * Gets the next search result.
     *
     * @param nativePtr The native pointer returned by
     * {@link #beginSearch(String)}.
     * @return Returns the full path to th next search result or
     * <code>null</code> if there are no more search results or an error
     * occurred.
     */
    private static native String getNextResult( long nativePtr );

    static {
        System.loadLibrary( "Windows" );
    }

    ////////// main() /////////////////////////////////////////////////////////

    public static void main( String[] args ) {
        final WindowsSavedSearch savedSearch =
            new WindowsSavedSearch( args[0] );
        final String[] contents = savedSearch.list();
        if ( contents != null )
            for ( String file : contents )
                System.out.println( file );
    }
}
/* vim:set et sw=4 ts=4: */
