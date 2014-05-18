/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.filecache;

import java.io.*;
import java.nio.channels.FileChannel;

import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.file.FileIterator;

/**
 * A <code>FileCache</code> is used to cache files until a maximum capacity is
 * reached.  Once reached, files having the oldest access time are removed.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class FileCache {

    /**
     * The version of the cache data.
     */
    private static final int CACHE_VERSION = 11;

    static final boolean DEBUG = false;

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Create a <code>FileCache</code>.
     *
     * @param maxSize The maximum size of the cache (in bytes).  A size of 0
     * means no maximum size.
     * @param mapper The {@link  FileCacheKeyMapper} to use.
     */
    public FileCache( long maxSize, FileCacheKeyMapper mapper )
        throws IOException
    {
        m_mapper = mapper;
        m_capacity = maxSize;
        if ( m_capacity > 0 )
            m_monitor = new FileCacheMonitor( this );
        final File cacheDir = getCacheDirectory();
        final File versionFile = new File( cacheDir, "version" );
        if ( !checkVersion( versionFile ) ) {
            clear();
            createVersionFile( versionFile );
        }
    }

    /**
     * Clears the cache by removing all cached files in it.
     */
    public synchronized void clear() throws IOException {
        if ( m_monitor != null )
            m_monitor.clear();
        final File cacheDir = getCacheDirectory();
        final File[] contents =
            FileUtil.listFiles( cacheDir, FileCacheFilter.INSTANCE, true );
        if ( !FileUtil.delete( contents, FileCacheFilter.INSTANCE, true ) )
            throw new IOException( "Could not delete old cache" );
        m_size = 0;
    }

    /**
     * Checks whether there is a cache entry for the given key.
     *
     * @param key The key.
     * @return Returns <code>true</code> only if there is a cache entry for the
     * given key.
     */
    public boolean contains( String key ) {
        final File file = m_mapper.mapKeyToFile( key, false );
        synchronized ( this ) {
            //
            // If we have a monitor, just ask it whether it's monitoring the
            // file because it's faster than doing filesystem I/O.
            //
            if ( m_monitor != null )
                return m_monitor.containsFile( file );
        }
        return file.exists();
    }

    /**
     * Dispose of this cache.
     */
    public synchronized void dispose() {
        if ( m_monitor != null ) {
            m_monitor.dispose();
            m_monitor = null;
        }
    }

    /**
     * Gets the cache directory.
     *
     * @return Returns said directory.
     */
    public File getCacheDirectory() {
        return m_mapper.getCacheDirectory();
    }

    /**
     * Gets the capacity of the cache.
     *
     * @return Returns said capacity (in bytes).
     * @see #getSize()
     * @see #setCapacity(long)
     */
    public long getCapacity() {
        return m_capacity;
    }

    /**
     * Gets an existing entry from the cache.
     *
     * @param key The key.
     * @return Returns the {@link File} to read the entry from or
     * <code>null</code> if no such entry exists.
     * @see #contains(String)
     * @see #getStreamFor(String)
     */
    @SuppressWarnings({"ConstantConditions"})
    public File getFileFor( String key ) {
        final File file = m_mapper.mapKeyToFile( key, true );
        if ( DEBUG )
            System.err.println(
                "FileCache.getFileFor(\"" + key + "\"); file = \""
                + file + '"'
            );
        if ( file.exists() ) {
            if ( DEBUG )
                System.err.println( "  --> cache hit" );
            return file;
        }
        if ( DEBUG )
             System.err.println( "  --> cache miss" );
        return null;
    }

    /**
     * Gets the current size of the cache.
     *
     * @return Returns said size (in bytes).
     * @see #getCapacity()
     * @see #setCapacity(long)
     */
    public synchronized long getSize() {
        return m_size;
    }

    /**
     * Gets an existing entry from the cache.
     *
     * @param key The key.
     * @return Returns an {@link InputStream} to read the entry from or
     * <code>null</code> if no such entry exists.
     * @see #contains(String)
     * @see #getFileFor(String)
     */
    @SuppressWarnings({"ConstantConditions"})
    public FileInputStream getStreamFor( String key ) {
        final File file = m_mapper.mapKeyToFile( key, true );
        if ( DEBUG )
            System.err.println(
                "FileCache.getStreamFor(\"" + key + "\"); file = \""
                + file + '"'
            );
        try {
            final FileInputStream fis = new FileInputStream( file );
            if ( DEBUG )
                System.err.println( "  --> cache hit" );
            return fis;
        }
        catch ( FileNotFoundException e ) {
            if ( DEBUG )
                System.err.println( "  --> cache miss" );
            return null;
        }
    }

    /**
     * Notify this <code>FileCache</code> that the given {@link File} used for
     * writing has just been closed (and thus should have its size added to the
     * cache's size).
     * <p>
     * This <b>must</b> only be called if {@link #putToFile(String)} is used.
     *
     * @param file The {@link File} that has just been closed.
     * @see #putToFile(String)
     */
    public void notifyAboutCloseOf( File file ) throws IOException {
        if ( m_monitor != null ) {
            addToCacheSize( file.length() );
            m_monitor.addFile( file );
            wakeUpMonitor();
        }
    }

    /**
     * Gets a new {@link File} to which a new entry is to be written into the
     * cache.
     * <p>
     * After the entry has been written, {@link #notifyAboutCloseOf(File)}
     * <b>must</b> be called passing the {@link File} returned from this
     * method.
     * <p>
     * If at all possible, this method should not be used and
     * {@link #putToStream(String)} should be used instead because the close of
     * the stream automatically notifies the cache.
     *
     * @param key The key.
     * @return Returns a {@link File} to write the new entry to.
     * @see #putToStream(String)
     */
    public File putToFile( String key ) throws IOException {
        final File file = m_mapper.mapKeyToFile( key, true );
        remove( file );
        return file;
    }

    /**
     * Gets a new {@link FileOutputStream} to which a new entry is to be
     * written into the cache.
     *
     * @param key The key.
     * @return Returns a {@link FileOutputStream} to write the new entry to.
     * @see #putToFile(String)
     */
    public FileOutputStream putToStream( String key ) throws IOException {
        final File file = putToFile( key );
        return new FileCacheOutputStream( file, this );
    }

    /**
     * Removes an entry from the cache.
     *
     * @param key The key.
     * @return Returns <code>true</code> only if the entry for the given key
     * was removed.
     */
    public boolean remove( String key ) throws IOException {
        final File file = m_mapper.mapKeyToFile( key, false );
        return remove( file );
    }

    /**
     * Sets a new capacity.  Note that this can be done only on
     * <code>FileCache</code>s that were initially created with a non-zero
     * capacity otherwise this method has no effect.
     *
     * @param capacity The new capacity (in bytes).
     */
    public void setCapacity( long capacity ) {
        if ( m_capacity > 0 ) {
            m_capacity = capacity;
            wakeUpMonitor();
        }
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Add to the cache size.
     *
     * @param size The number of bytes to add.
     */
    @SuppressWarnings({"ConstantConditions"})
    synchronized void addToCacheSize( long size ) {
        m_size += size;
        if ( DEBUG )
            System.err.println(
                "FileCache.addToCacheSize(" + (size / (1024*1024))
                + "); cache size now = " + (m_size / (1024*1024)) + " MB"
            );
    }

    ////////// protected //////////////////////////////////////////////////////

    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Checks the version of the cache on disk.
     *
     * @param file The {@link File} containing the version number.
     * @return Returns <code>true</code> only if the version is acceptable or
     * <code>false</code> if the version file either doesn't exist or is too
     * old.
     */
    private static boolean checkVersion( File file ) throws IOException {
        final FileInputStream fis;
        try {
            fis = new FileInputStream( file );
        }
        catch ( FileNotFoundException e ) {
            return false;
        }

        int fileLength = (int)file.length();
        if ( fileLength > MAX_SANE_VERSION_FILE_SIZE ) {
            //
            // Something is funny: the file shouldn't be bigger than the
            // maximum sane size.
            //
            fileLength = MAX_SANE_VERSION_FILE_SIZE;
        }
        final byte[] buf = new byte[ fileLength ];
        try {
            fis.read( buf );
        }
        finally {
            fis.close();
        }

        final String versionString = new String( buf );
        try {
            final int version = Integer.parseInt( versionString );
            if ( version == CACHE_VERSION )
                return true;
        }
        catch ( NumberFormatException e ) {
            // ignore
        }
        return false;
    }

    /**
     * Create the version file.
     *
     * @param file The {@link File} to create.
     */
    private static void createVersionFile( File file ) throws IOException {
        final FileOutputStream fos = new FileOutputStream( file );
        try {
            fos.write( Integer.toString( CACHE_VERSION ).getBytes( "UTF-8" ) );
        }
        finally {
            fos.close();
        }
    }

    /**
     * Remove a file from the cache.
     *
     * @param file The {@link File} to remove.
     * @return Returns <code>true</code> only if the file for the given key
     * was removed.
     */
    private synchronized boolean remove( File file ) throws IOException {
        if ( file.exists() ) {
            if ( !file.delete() )
                throw new IOException(
                    "Could not delete " + file.getAbsolutePath()
                );
            if ( m_capacity > 0 )
                addToCacheSize( - file.length() );
        }
        return m_monitor == null || m_monitor.removeFile( file );
    }

    /**
     * Notify the {@link FileCacheMonitor} that it may need to do something.
     */
    @SuppressWarnings({"SynchronizeOnNonFinalField"})
    private void wakeUpMonitor() {
        if ( m_monitor != null )
            synchronized ( m_monitor ) {
                m_monitor.notify();
            }
    }

    /**
     * The maximum size a version file should ever be.
     */
    private static final int MAX_SANE_VERSION_FILE_SIZE = 5;

    /**
     * The maximum capacity of the cache (in bytes).  A capacity of 0 means no
     * maximum.
     */
    private long m_capacity;

    /**
     * The {@link FileCacheKeyMapper} in use.
     */
    private final FileCacheKeyMapper m_mapper;

    /**
     * The {@link FileCacheMonitor} in use or <code>null</code> if none.
     */
    private FileCacheMonitor m_monitor;

    /**
     * The current total size of all the files in the cache (in bytes).
     */
    private long m_size;

    ////////// main() for testing /////////////////////////////////////////////

    private static final long TEST_CAPACITY = 256 * 1024 * 1024;

    public static void main( String[] args ) throws Exception {
        final FileCache cache =
            new FileCache( TEST_CAPACITY, PerUserFileCacheKeyMapper.create() );

        for ( FileIterator i = new FileIterator( new File( args[0] ), false );
              i.hasNext(); ) {
            final File file = i.next();
            System.err.println( "main(): putting " + file.getAbsolutePath() + ", size = " + (file.length() / (1024*1024)) + " MB" );
            final FileInputStream fis = new FileInputStream( file );
            final FileChannel fic = fis.getChannel();
            final FileOutputStream fos = cache.putToStream( file.getAbsolutePath() );
            final FileChannel foc = fos.getChannel();
            fic.transferTo( 0, fic.size(), foc );
            fos.close();
            fis.close();
        }
    }
}
/* vim:set et sw=4 ts=4: */
