/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.filecache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A <code>FileCacheOutputStream</code> is-a {@link FileOutputStream} that,
 * upon close, notifies the owning cache about it.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
final class FileCacheOutputStream extends FileOutputStream {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Close the underlying {@link FileOutputStream}, then notify the owning
     * {@link FileCache} that an output file was closed.
     */
    public synchronized void close() throws IOException {
        try {
            super.close();
        }
        finally {
            if ( m_file != null ) {
                //
                // We have to set m_file to null so this code will never be
                // called more than once per file.
                //
                // If this were not done, the call to super.close() above
                // somehow calls this close() method again.  Then this code
                // would be called a second time after super.close() returns.
                // This is really strange: this close() method shouldn't be
                // called a second time.  But setting m_file to null and
                // checking for it is a work-around.
                //
                final File temp = m_file;
                m_file = null;
                m_owningCache.notifyAboutCloseOf( temp );
            }
        }
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Construct a <code>FileCacheOutputStream</code>.
     *
     * @param file The {@link File} to write to.
     * @param owningCache The owning {@link FileCache}.
     */
    FileCacheOutputStream( File file, FileCache owningCache )
        throws FileNotFoundException
    {
        super( file );
        m_file = file;
        m_owningCache = owningCache;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The {@link File} that this <code>FileCacheOutputStream</code> was
     * created around.
     */
    private File m_file;

    /**
     * The {@link FileCache} that created this
     * <code>FileCacheOutputStream</code> and that needs to be notified upon
     * the closing of this stream.
     */
    private final FileCache m_owningCache;
}
/* vim:set et sw=4 ts=4: */
