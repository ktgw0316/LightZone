/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.utils.directory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.util.HashMap;
import java.util.Map;

import static java.nio.file.StandardWatchEventKinds.*;

/**
 * A <code>UnixDirectoryMonitor</code> is-a {@link DirectoryMonitor} for
 * monitoring directories of a Unix system (Linux, Mac OS X, etc.).
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class UnixDirectoryMonitor extends DirectoryMonitor {

    public UnixDirectoryMonitor() {
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch (IOException ignored) {
        }
        start();
    }

    /**
     * Add a directory to be monitored.  Adding the same directory more than
     * once is guaranteed to be harmless.
     *
     * @param directory The directory to be monitored.
     */
    @Override
    public void addDirectory(File directory) {
        try {
            final Path dir = directory.toPath();
            WatchKey watchKey = dir.register(watcher, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            watchKeyMap.put(watchKey, dir);
            if (DEBUG) {
                System.out.println("UnixDirectoryMonitor: added " + dir);
            }
        } catch (InvalidPathException | IOException ignored) {
        }
    }

    /**
     * Remove a directory from being monitored.
     *
     * @param directory The directory to remove.
     * @return Returns <code>true</code> only if the directory was being
     * monitored and thus removed.
     */
    @Override
    public boolean removeDirectory(File directory) {
        boolean removed;
        synchronized (watchKeyMap) {
            removed = watchKeyMap.keySet().stream()
                    .filter(key -> getPathFor(key).equals(directory.toPath()))
                    .anyMatch(key -> watchKeyMap.remove(key) != null);
        }
        if (DEBUG && removed) {
            System.out.println("UnixDirectoryMonitor: removed " + directory);
        }
        return removed;
    }

    @Override
    Path getPathFor(WatchKey key) {
        return watchKeyMap.get(key);
    }

    private final Map<WatchKey, Path> watchKeyMap = new HashMap<>();

    ////////// main() /////////////////////////////////////////////////////////

    /*
    private static final class TestListener implements DirectoryListener {
        @Override
        public void directoryChanged( File dir ) {
            System.out.println( dir.getAbsolutePath() );
        }
    }

    public static void main( String[] args ) throws IOException {
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
    */
}
/* vim:set et sw=4 ts=4: */
