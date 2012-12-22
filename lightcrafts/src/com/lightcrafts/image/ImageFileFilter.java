/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image;

import java.io.File;
import java.io.FileFilter;

import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.utils.file.FileUtil;

/**
 * An <code>ImageFileFilter</code> is a {@link FileFilter} for filtering image
 * files.
 *
 * @author Tom Bradford [tom@lightcrafts.com]
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see ImageFilenameFilter
 */
public final class ImageFileFilter implements FileFilter {

    ////////// public /////////////////////////////////////////////////////////

    /** The singleton instance. */
    public static final ImageFileFilter INSTANCE = new ImageFileFilter();

    /**
     * Tests whether the given {@link File} should be accepted.
     *
     * @param file The {@link File} to test.
     * @return Returns <code>true</code> only if the file is one of the
     * supported image file types (checking by the file's extension).
     */
    public boolean accept( File file ) {
        if ( file.isHidden() || file.isDirectory() || !file.isFile() )
            return false;
        final File resolvedFile = FileUtil.resolveAliasFile( file );
        if ( resolvedFile != file )
            return accept( resolvedFile );
        return ImageType.determineTypeByExtensionOf( file ) != null;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct the singleton instance of <code>ImageFileFilter</code>.
     */
    private ImageFileFilter() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
