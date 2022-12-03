/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.filecache;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.PriorityQueue;

import com.lightcrafts.utils.file.FileIterator;
import com.lightcrafts.utils.file.FileUtil;

/**
 * A <code>FileCacheMonitor</code> is used to monitor the size of a
 * {@link FileCache} to ensure that its size stays below its capacity.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class FileCacheMonitor extends Thread {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Monitor the owning {@link FileCache}'s size: if it becomes greater than
     * its capacity, remove files having the oldest access times.
     */
    @SuppressWarnings({"ConstantConditions"})
    public void run() {
        initialize();
        while ( !m_stop ) {
            while ( m_owningCache.getSize() > m_owningCache.getCapacity() ) {
                if ( m_stop )
                    return;
                if ( FileCache.DEBUG )
                    System.err.println(
                        "run(): cache size = "
                         + (m_owningCache.getSize() / (1024*1024)) + " MB"
                    );
                final CacheFile fileToRemove = getNextFileToRemove();
                final long size = fileToRemove.length();
                fileToRemove.delete();
                if ( FileCache.DEBUG )
                    System.err.println(
                        "run(): purging " + fileToRemove.getAbsolutePath()
                        + ", size = " + (size / (1024 * 1024)) + " MB"
                    );
                m_owningCache.addToCacheSize( -size );
                if ( FileCache.DEBUG )
                    System.err.println(
                        "run(): cache size = "
                        + (m_owningCache.getSize() / (1024 * 1024)) + " MB"
                    );
            }
            synchronized ( this ) {
                try {
                    wait();
                }
                catch ( InterruptedException e ) {
                    // ignore
                }
            }
        }
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Construct a <code>FileCacheMonitor</code>.
     *
     * @param owningCache The {@link FileCache} that is using this
     * <code>FileCacheMonitor</code>.
     */
    FileCacheMonitor( FileCache owningCache ) {
        super( "FileCacheMonitor" );
        setDaemon( true );
        setPriority( MIN_PRIORITY );
        m_owningCache = owningCache;
        start();
    }

    /**
     * Add a file.  If the file is already being monitored and its last access
     * time hasn't changed, nothing is added; if the file's last access time is
     * different, the old entry is replaced by the new one having the current
     * last access time.
     *
     * @param file The {@link File} to add.
     * @return Returns <code>true</code> only if the file was added.
     * @throws IOException if the file doesn't exist or its last access time
     * could not be obtained.
     */
    boolean addFile( File file ) throws IOException {
        final long newLastAccessTime = FileUtil.getLastAccessTimeOf( file );
        final String path = file.getAbsolutePath();
        final CacheFile oldCacheFile, newCacheFile;

        synchronized ( m_filesByPath ) {
            oldCacheFile = m_filesByPath.get( path );
            if ( oldCacheFile != null &&
                 oldCacheFile.lastAccessTime() == newLastAccessTime )
                return false;
            newCacheFile = new CacheFile( path, newLastAccessTime );
            m_filesByPath.put( path, newCacheFile );
        }
        synchronized ( m_filesByLastAccessTime ) {
            if ( oldCacheFile != null )
                m_filesByLastAccessTime.remove( oldCacheFile );
            m_filesByLastAccessTime.add( newCacheFile );
        }
        return true;
    }

    /**
     * Remove all the files.
     *
     * @see #removeFile(File)
     */
    void clear() {
        m_abortInitialize = true;
        synchronized ( m_filesByLastAccessTime ) {
            m_filesByLastAccessTime.clear();
        }
        synchronized ( m_filesByPath ) {
            m_filesByPath.clear();
        }
    }

    /**
     * Checks whether the given file is being monitored.
     *
     * @param file The {@link File} to check.
     * @return Returns <code>true</code> only if the file is being monitored.
     */
    boolean containsFile( File file ) {
        final String path = file.getAbsolutePath();
        synchronized ( m_filesByPath ) {
            return m_filesByPath.containsKey( path );
        }
    }

    /**
     * Dispose of this <code>FileCacheMonitor</code>.
     */
    void dispose() {
        m_stop = true;
        synchronized ( this ) {
            notify();
        }
    }

    /**
     * Remove a file.
     *
     * @param file The {@link File} to remove.
     * @return Returns <code>true</code> only if the file was removed.
     * @see #clear()
     */
    boolean removeFile( File file ) {
        final String path = file.getAbsolutePath();
        final CacheFile cacheFile;
        synchronized ( m_filesByPath ) {
            cacheFile = m_filesByPath.remove( path );
        }
        if ( cacheFile != null ) {
            synchronized ( m_filesByLastAccessTime ) {
                m_filesByLastAccessTime.remove( cacheFile );
            }
            return true;
        }
        return false;
    }

    ////////// protected //////////////////////////////////////////////////////

    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Initialize by creating a set of all files in the cache, totaling their
     * sizes, and setting the cache's size to said total.
     */
    private void initialize() {
        long totalSize = 0;
        final FileIterator i = new FileIterator(
            m_owningCache.getCacheDirectory(), FileCacheFilter.INSTANCE, true
        );
        while ( i.hasNext() ) {
            if ( m_abortInitialize || m_stop )
                return;
            final File file = i.next();
            final long size = file.length();
            if ( size > 0 )
                try {
                    addFile( file );
                    totalSize += size;
                }
                catch ( IOException e ) {
                    // ignore?
                }
        }
        if ( totalSize > 0 )
            m_owningCache.addToCacheSize( totalSize );
    }

    /**
     * Gets the next {@link CacheFile} to remove.
     *
     * @return Returns said {@link CacheFile}.
     */
    private CacheFile getNextFileToRemove() {
        while ( true ) {
            CacheFile file;
            synchronized ( m_filesByLastAccessTime ) {
                file = m_filesByLastAccessTime.poll();
            }
            try {
                //
                // We have to get the file's current last access time and
                // compare it to the stored last access time: if they're equal,
                // the file hasn't been accessed since its cache entry was
                // created so it really is the oldest file and thus the one to
                // be removed.
                //
                final long currentLastAccessTime =
                    FileUtil.getLastAccessTimeOf( file );
                if ( file.lastAccessTime() == currentLastAccessTime )
                    return file;
                //
                // Otherwise, the file has been accessed since its cache entry
                // so we need to replace its existing entry with a current one
                // and throw it back into the queue.
                //
                final String path = file.getAbsolutePath();
                file = new CacheFile( path, currentLastAccessTime );
                synchronized( m_filesByPath ) {
                    m_filesByPath.put( path, file );
                }
                synchronized ( m_filesByLastAccessTime ) {
                    m_filesByLastAccessTime.add( file );
                }
            }
            catch ( IOException e ) {
                //
                // There was a problem getting the file's current last access
                // time: delete the file.
                //
                m_owningCache.addToCacheSize( -file.length() );
                synchronized ( m_filesByPath ) {
                    m_filesByPath.remove( file.getAbsolutePath() );
                }
                file.delete();
            }
        }
    }

    /**
     * A flag to indicate that initialization, if in progress, should abort.
     */
    private boolean m_abortInitialize;

    /**
     * The set of all the files in the cache ordered only by their last access
     * times.
     */
    private final PriorityQueue<CacheFile> m_filesByLastAccessTime =
        new PriorityQueue<CacheFile>( 11, CacheFileComparator.INSTANCE );

    /**
     * A map of all the files in the cache where the key is the file's absolute
     * path and the value is the associated {@link CacheFile}.
     * <p>
     * This is needed so as to have only one {@link CacheFile} object for a
     * given file on disk thereby ensuring that there will only be exactly one
     * last access time for a given file.
     */
    private final HashMap<String,CacheFile> m_filesByPath =
        new HashMap<String,CacheFile>();

    /**
     * The {@link FileCache} that owns this <code>FileCacheMonitor</code>.
     */
    private final FileCache m_owningCache;

    /**
     * A flag to indicate when this thread should stop.
     */
    private boolean m_stop;
}
/* vim:set et sw=4 ts=4: */
