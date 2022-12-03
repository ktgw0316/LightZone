/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.filecache;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

import com.lightcrafts.utils.file.FileUtil;

/**
 * A <code>CacheFile</code> is-a {@link File} that maintains additional
 * information for every file in the cache.  Currently, this is the file's
 * original last access time and its original length.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class CacheFile extends File {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets the length of the file denoted by this abstract pathname.  The
     * return value is unspecified if this pathname denotes a directory.
     * <p>
     * Note that the length returned is the file's length at the time this
     * object was constructed.  Hence, it's not the current length.
     * <p>
     * The reason it's not the current length is because if the file is removed
     * from the cache, the file's length needs to be subtracted from the
     * cache's total size.  In order to guarantee the length subtracted matches
     * the length originally added, the original length is stored.
     *
     * @return Returns said length (in bytes) or <code>0L</code> if the file
     * does not exist.
     */
    public long length() {
        return m_originalLength;
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Construct a <code>CacheFile</code>.
     *
     * @param file An existing {@link File}.
     */
    CacheFile( File file ) throws IOException {
        this( file, FileUtil.getLastAccessTimeOf( file ) );
    }

    /**
     * Construct a <code>CacheFile</code>.
     *
     * @param pathname The full path of the file.
     */
    CacheFile( String pathname ) throws IOException {
        super( pathname );
        m_originalLastAccessTime = FileUtil.getLastAccessTimeOf( this );
        m_originalLength = super.length();
    }

    /**
     * Construct a <code>CacheFile</code>.
     *
     * @param file An existing {@link File}.
     * @param lastAccessTime The last access time of the file (in milliseconds
     * since epoch).
     */
    CacheFile( File file, long lastAccessTime ) {
        this( file.getAbsolutePath(), lastAccessTime );
    }

    /**
     * Construct a <code>CacheFile</code>.
     *
     * @param pathname The full path of the file.
     * @param lastAccessTime The last access time of the file (in milliseconds
     * since epoch).
     */
    CacheFile( String pathname, long lastAccessTime ) {
        super( pathname );
        m_originalLastAccessTime = lastAccessTime;
        m_originalLength = super.length();
    }

    /**
     * Gets this file's last access time at the time this object was
     * constructed.  Hence, it's not the most up-to-date last access time.
     * <p>
     * The reason it can't be the most up-to-date last access time is because
     * it would affect the result of
     * {@link CacheFileComparator#compare(Object,Object)}
     * that would wreak havoc if <code>CacheFile</code>
     * objects are placed into data structures like {@link TreeMap}s.
     *
     * @return Returns said time in milliseconds since epoch.
     */
    long lastAccessTime() {
        return m_originalLastAccessTime;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The last access time of this file since epoch (in milliseconds) at the
     * time this object was constructed.
     */
    private final long m_originalLastAccessTime;

    /**
     * The file's length at the time this object was constructed.
     */
    private final long m_originalLength;
}
/* vim:set et sw=4 ts=4: */
