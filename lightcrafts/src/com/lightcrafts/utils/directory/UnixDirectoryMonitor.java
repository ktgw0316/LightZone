/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.directory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * A <code>UnixDirectoryMonitor</code> is-a {@link DirectoryMonitor} for
 * monitoring directories of a Unix system (Linux, Mac OS X, etc.).
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class UnixDirectoryMonitor extends DirectoryMonitor {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>UnixDirectoryMonitor</code>.
     */
    public UnixDirectoryMonitor() {
        start();
    }

    /**
     * Add a directory to be monitored.  Adding the same directory more than
     * once is guaranteed to be harmless.
     *
     * @param directory The directory to be monitored.
     */
    @SuppressWarnings({"ConstantConditions"})
    public void addDirectory( File directory ) {
        final Long value = directory.lastModified();
        final boolean added;
        synchronized ( m_dirMap ) {
            added = m_dirMap.put( directory, value ) != null;
        }
        if ( DEBUG && added )
            System.out.println( "UnixDirectoryMonitor: added " + directory );
    }

    /**
     * Remove a directory from being monitored.
     *
     * @param directory The directory to remove.
     * @return Returns <code>true</code> only if the directory was being
     * monitored and thus removed.
     */
    @SuppressWarnings({"ConstantConditions"})
    public boolean removeDirectory( File directory ) {
        final boolean removed;
        synchronized ( m_dirMap ) {
            removed = m_dirMap.remove( directory ) != null;
        }
        if ( DEBUG && removed )
            System.out.println( "UnixDirectoryMonitor: removed " + directory );
        return removed;
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    protected File[] getMonitoredDirectories() {
        synchronized ( m_dirMap ) {
            return m_dirMap.keySet().toArray( new File[0] );
        }
    }

    /**
     * {@inheritDoc}
     */
    protected boolean hasChanged( File dir ) {
        if ( dir.exists() ) {
            //
            // The directory still exists: check to see whether its changed
            // since the last time we checked.
            //
            synchronized ( m_dirMap ) {
                final Long prevValue = m_dirMap.get( dir );
                if ( prevValue != null ) {
                    final long newValue = dir.lastModified();
                    if ( newValue != prevValue.longValue() ) {
                        //
                        // The contents of the directory have changed.
                        //
                        m_dirMap.put( dir, newValue );
                        return true;
                    }
                }
            }
        } else {
            //
            // The directory has disappeared from the filesystem: remove it
            // from the collection of directories we're monitoring.
            //
            return removeDirectory( dir );
        }
        return false;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The collection of directories to monitor.  The key is the full path to a
     * directory and the value is the directory's modification time.
     */
    private final Map<File,Long> m_dirMap = new HashMap<File,Long>();

    ////////// main() /////////////////////////////////////////////////////////

    private static final class TestListener implements DirectoryListener {
        public void directoryChanged( File dir ) {
            System.out.println( dir.getAbsolutePath() );
        }
    }

    public static void main( String[] args ) throws Exception {
        final DirectoryMonitor monitor = new UnixDirectoryMonitor();
        final DirectoryListener listener = new TestListener();
        monitor.addListener( listener );

        final BufferedReader commandLine =
            new BufferedReader( new InputStreamReader( System.in ) );

        while ( true ) {
            System.out.print( "> " );
            final String cmd = commandLine.readLine();
            if ( cmd.length() == 0 )
                continue;
            if ( cmd.startsWith( "+ " ) ) {
                final String dir = cmd.substring( 2 );
                monitor.addDirectory( new File( dir ) );
                continue;
            }
            if ( cmd.startsWith( "- " ) ) {
                final String dir = cmd.substring( 2 );
                monitor.removeDirectory( new File( dir ) );
                continue;
            }
            System.err.println( "Unknown command" );
        }
    }
}
/* vim:set et sw=4 ts=4: */
