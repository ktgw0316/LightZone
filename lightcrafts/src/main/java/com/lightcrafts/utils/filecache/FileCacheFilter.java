/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.filecache;

import java.io.File;
import java.io.FileFilter;

/**
 * A <code>FileCacheFilter</code> is-a {@link FileFilter} for selecting only
 * those files that are cache files.
 *
 * @author Paul J. Lucas [login@domain.com]
 */
public final class FileCacheFilter implements FileFilter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * The filename extension denoting files in the cache.
     */
    public static final String EXTENSION = ".lzc";

    /**
     * The singleton instance of <code>FileCacheFilter</code>.
     */
    public static final FileCacheFilter INSTANCE = new FileCacheFilter();

    /**
     * {@inheritDoc}
     */
    public boolean accept( File file ) {
        final String name = file.getName();
        return  name.endsWith( EXTENSION ) ||
                name.startsWith( "lzncache." ); // Tom's old files
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct a <code>FileCacheFilter</code>.
     */
    private FileCacheFilter() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
