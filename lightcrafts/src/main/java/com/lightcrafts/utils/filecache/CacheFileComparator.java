/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.filecache;

import java.util.Comparator;

/**
 * A <code>CacheFileComparator</code> is used to compare two {@link CacheFile}
 * objects based on their last access time.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class CacheFileComparator implements Comparator<CacheFile> {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Compares two {@link CacheFile} objects based on their last access times.
     * <P>
     * Note: this comparator imposes orderings that are inconsistent with
     * equals in that the {@link CacheFile}'s files' names are not used during
     * the comparison.
     *
     * @param file1 The first {@link CacheFile}.
     * @param file2 The second {@link CacheFile}.
     * @return Returns negative integer, zero, or a positive integer as the
     * first {@link CacheFile}'s last access time is less than, equal to, or
     * greater than the second.
     */
    public int compare( CacheFile file1, CacheFile file2 ) {
        return (int)(file1.lastAccessTime() - file2.lastAccessTime());
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * The singleton instance of <code>CacheFileComparator</code>.
     */
    static final CacheFileComparator INSTANCE = new CacheFileComparator();

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of <code>CacheFileComparator</code>.
     */
    private CacheFileComparator() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
