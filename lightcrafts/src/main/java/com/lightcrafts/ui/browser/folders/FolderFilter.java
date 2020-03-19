/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.folders;

import java.io.File;
import java.io.FileFilter;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.utils.file.FileUtil;

/**
 * A <code>FolderFilter</code> is-a {@link FileFilter} that accepts only
 * non-hidden directories and aliases to directories.
 */
class FolderFilter implements FileFilter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public boolean accept( File file ) {
        return FileUtil.isFolder( file ) != null;
    }

    ////////// package ////////////////////////////////////////////////////////

    /** The singleton instance. */
    static final FolderFilter INSTANCE =
        Platform.isMac() ?
            new MacOSXFolderFilter() :
            new FolderFilter();

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Construct a <code>FolderFilter</code>.
     */
    protected FolderFilter() {
        // do nothing
    }
}
/* vim:set et sw=4 ts=4: */
