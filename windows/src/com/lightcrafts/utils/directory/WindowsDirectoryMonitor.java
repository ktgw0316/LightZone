/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.directory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.platform.windows.WindowsFileUtil;
import sun.awt.shell.ShellFolder;

/**
 * A <code>WindowsDirectoryMonitor</code> is-a {@link DirectoryMonitor} for
 * monitoring directories of a Windows system.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class WindowsDirectoryMonitor extends DirectoryMonitor {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>WindowsDirectoryMonitor</code>.
     */
    public WindowsDirectoryMonitor() {
        start();
    }

    /**
     * Add a directory to be monitored.  Adding the same directory more than
     * once is guaranteed to be harmless.
     *
     * @param directory The directory to be monitored.
     */
    public void addDirectory( File directory ) {
        synchronized ( m_dirMap ) {
            if ( !m_dirMap.containsKey( directory ) ) {
                try {
                    final long value;
                    if ( WindowsFileUtil.isGUID( directory ) )
                        value = newHashCode( directory );
                    else
                        value = newHandle( directory.getAbsolutePath() );
                    m_dirMap.put( directory, value );
                    //noinspection ConstantConditions
                    if ( DEBUG )
                        System.out.println(
                            "WindowsDirectoryMonitor: added " + directory
                        );
                }
                catch ( IOException e ) {
                    // ignore
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        super.dispose();
        synchronized ( m_dirMap ) {
            for ( Map.Entry<File,Long> entry : m_dirMap.entrySet() )
                if ( !WindowsFileUtil.isGUID( entry.getKey() ) )
                    try {
                        disposeHandle( entry.getValue() );
                    }
                    catch ( IOException e ) {
                        // ignore
                    }
        }
    }

    /**
     * Remove a directory from being monitored.
     *
     * @param directory The directory to remove.
     * @return Returns <code>true</code> only if the directory was being
     * monitored and thus removed.
     */
    public boolean removeDirectory( File directory ) {
        final Long value;
        synchronized ( m_dirMap ) {
            value = m_dirMap.remove( directory );
        }
        if ( value != null ) {
            if ( !WindowsFileUtil.isGUID( directory ) ) {
                try {
                    disposeHandle( value );
                }
                catch ( IOException e ) {
                    // ignore
                }
            }
            //noinspection ConstantConditions
            if ( DEBUG )
                System.out.println(
                    "WindowsDirectoryMonitor: removed " + directory
                );
            return true;
        }
        return false;
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
                final Long value = m_dirMap.get( dir );
                if ( value != null )
                    if ( WindowsFileUtil.isGUID( dir ) ) {
                        final long newHashCode = newHashCode( dir );
                        if ( newHashCode != value ) {
                            m_dirMap.put( dir, newHashCode );
                            return true;
                        }
                    } else
                        try {
                            if ( hasChanged( value ) ) {
                                //
                                // The contents of the directory have changed.
                                //
                                return true;
                            }
                        }
                        catch ( IOException e ) {
                            // ignore
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
     * Dispose of the given native Windows change notification object referred
     * to by the given handle.
     *
     * @param handle The handle to the native Windows change notification
     * object to dispose of.
     */
    private static native void disposeHandle( long handle ) throws IOException;

    /**
     * Checks whether the directory referred to by the given native Windows
     * change notification <code>HANDLE</code> has changed.
     *
     * @param handle The handle to the native Windows change notification
     * object of a directory being monitored.
     * @return Returns <code>true</code> only if the directory has changed.
     */
    private static native boolean hasChanged( long handle ) throws IOException;

    /**
     * Creates a new native Windows <code>HANDLE</code> that refers to a
     * Windows change notification object that is used to monitor the given
     * directory.
     *
     * @param dir The full path of the directory to monitor.
     * @return Returns a native Windows <code>HANDLE</code> object (casted to
     * an <code>long</code>) that refers to a Windows change notification object
     * that is used to monitor the directory.  The value should only be passed
     * to native methods and should be considered opaque and not touched from
     * Java.
     */
    private static native long newHandle( String dir ) throws IOException;

    /**
     * Computes a hash code for the given directory.
     *
     * @param dir The directory to compute the hash code for.
     * @return Returns said hash code.
     */
    private static long newHashCode( File dir ) {
        if ( dir instanceof ShellFolder ) {
            //
            // This is a hack to work around the problem of scanning the
            // "Network" pseudo-folder that takes a long time.
            //
            final ShellFolder sf = (ShellFolder)dir;
            if ( sf.getDisplayName().equals( "Network" ) )
                return 0;
        }
        final File[] contents =
            FileUtil.listFiles( dir, DirectoryOnlyFilter.INSTANCE, false );
        long hashCode = 0;
        if ( contents != null )
            for ( File file : contents )
                hashCode ^= file.hashCode();
        return hashCode;
    }

    /**
     * The collection of directories to monitor.  The key is the full path to a
     * directory and the value is a native Windows <code>HANDLE</code> object
     * (casted to an <code>long</code>) that refers to a Windows change
     * notification object that is used to monitor the directory.
     * <p>
     * The value should only be gotten from and passed to native methods and
     * should be considered opaque and not touched from Java.
     */
    private final Map<File,Long> m_dirMap = new HashMap<File,Long>();

    static {
        System.loadLibrary( "Windows" );
    }

    ////////// main() /////////////////////////////////////////////////////////

    private static final class TestListener implements DirectoryListener {
        public void directoryChanged( File dir ) {
            System.out.println( dir.getAbsolutePath() );
        }
    }

    public static void main( String[] args ) throws Exception {
        final DirectoryMonitor monitor = new WindowsDirectoryMonitor();
        final DirectoryListener listener = new TestListener();
        monitor.addListener( listener );

        final BufferedReader commandLine =
            new BufferedReader( new InputStreamReader( System.in ) );

        //noinspection InfiniteLoopStatement
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
