/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image;

import java.io.File;
import java.io.FilenameFilter;

/**
 * An <code>ImageFileFilter</code> is a {@link FilenameFilter} for filtering
 * image file names.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see ImageFileFilter
 */
public class ImageFilenameFilter implements FilenameFilter {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance. */
    public static final ImageFilenameFilter INSTANCE =
        new ImageFilenameFilter();

    /**
     * Tests whether the given filename in a given directory should be
     * accepted.
     *
     * @param dir The directory to use.
     * @param name The filename.
     * @return Returns <code>true</code> only if the file is one of the
     * supported image file types (checking by the file's extension).
     */
    public boolean accept( File dir, String name ) {
        return ImageFileFilter.INSTANCE.accept( new File( dir, name ) );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of <code>ImageFilenameFilter</code>.
     */
    private ImageFilenameFilter() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
