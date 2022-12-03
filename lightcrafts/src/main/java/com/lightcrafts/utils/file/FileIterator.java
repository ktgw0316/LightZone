/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.file;

import java.io.File;
import java.io.FileFilter;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A <code>FileIterator</code> iterates over all the files in a given
 * directory and, optionally, all of its subdirectories.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class FileIterator implements Iterator<File> {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct a <code>FileIterator</code>.
     *
     * @param dir The directory
     * @param recursive If <code>true</code>, recurse into subdirectories.
     */
    public FileIterator( File dir, boolean recursive ) {
        this( dir, null, recursive );
    }

    /**
     * Construct a <code>FileIterator</code>.
     *
     * @param dir The directory
     * @param filter The {@link FileFilter} to use, if any.
     * @param recursive If <code>true</code>, recurse into subdirectories.
     */
    public FileIterator( File dir, FileFilter filter, boolean recursive ) {
        m_files = new LinkedList<File>();
        m_filter = filter;
        m_recursive = recursive;
        addIfOK( dir );
    }

    /**
     * Checks whether there is a next file.
     *
     * @return Returns <code>true</code> only if there is.
     */
    public synchronized boolean hasNext() {
        if ( m_next == null )
            m_next = next();
        return m_next != null;
    }

    /**
     * Gets the next file.
     *
     * @return Returns the next file or <code>null</code> if there is no next
     * file.
     */
    public synchronized File next() {
        if ( m_next != null ) {
            final File temp = m_next;
            m_next = null;
            return temp;
        }
        while ( true ) {
            if ( m_files.isEmpty() )
                return null;
            final File file = m_files.removeFirst();
            if ( !file.isDirectory() )
                return file;
            if ( !m_recursive ) {
                if ( m_isInitialDirectory )
                    m_isInitialDirectory = false;
                else
                    continue;
            }
            final File[] files = FileUtil.listFiles( file, m_filter, true );
            if ( files == null )
                return null;
            for ( File f : files )
                m_files.addFirst( f );
        }
    }

    /**
     * Removing files is currently not supported.
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    ////////// private ////////////////////////////////////////////////////////

    private void addIfOK( File file ) {
        if ( file.isDirectory() || m_filter == null || m_filter.accept( file ) )
            m_files.addFirst( file );
    }

    /**
     * TODO
     */
    private boolean m_isInitialDirectory = true;

    /**
     * The list of files being iterated over.
     */
    private final LinkedList<File> m_files;

    /**
     * The {@link FileFilter} to use, if any.
     */
    private final FileFilter m_filter;

    /**
     * The cahced next {@link File} to return.
     */
    private File m_next;

    /**
     * If <code>true</code>, recurse into subdirectories.
     */
    private final boolean m_recursive;
}
/* vim:set et sw=4 ts=4: */
