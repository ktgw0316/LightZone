/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.file;

import java.io.File;
import java.io.FileFilter;

/**
 * An <code>ICC_ProfileFileFilter</code> is a {@link FileFilter} for filtering
 * <code>ICC_Profile</code> files.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ICC_ProfileFileFilter implements FileFilter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * A singleton static instance.
     */
    public static final ICC_ProfileFileFilter INSTANCE =
        new ICC_ProfileFileFilter();

    /**
     * Tests whether the given {@link File} should be accepted.
     *
     * @param file The {@link File} to test.
     * @return Returns <code>true</code> only if the file is an
     * <code>ICC_Profile</code> based on either the filename extensions of
     * <code>.icc</code> or <code>.icm</code>.
     */
    public boolean accept( File file ) {
        if ( file.isHidden() || !file.isFile() )
            return false;
        final String name = file.getName().toLowerCase();
        return name.endsWith( ".icc" ) || name.endsWith( ".icm" );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Constructs an <code>ICC_ProfileFileFilter</code>.
     */
    private ICC_ProfileFileFilter() {
        // do nothing
    }

}
/* vim:set et sw=4 ts=4: */
