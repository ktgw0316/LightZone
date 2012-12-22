/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.directory;

import java.io.File;
import java.io.FileFilter;

/**
 * A <code>DirectoryOnlyFilter</code> is-a {@link FileFilter} that accepts only
 * non-hidden directories.
 *
 * @author Paul J. Lucaas [paul@lightcrafts.com]
 */
final class DirectoryOnlyFilter implements FileFilter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Accept a {@link File} only if it's (a) not hidden and (b) a directory.
     *
     * @param file The {@link File} to test.
     * @return Returns <code>true</code> only if the file is accepted.
     */
    public boolean accept( File file ) {
        return !file.isHidden() && file.isDirectory();
    }

    ////////// package ////////////////////////////////////////////////////////

    /** The singleton instance of <code>DirectoryOnlyFilter</code>. */
    static final DirectoryOnlyFilter INSTANCE = new DirectoryOnlyFilter();

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of <code>DirectoryOnlyFilter</code>.
     */
    private DirectoryOnlyFilter() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
