/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.file;

import com.lightcrafts.utils.TextUtil;

/**
 * A <code>FileRenamer</code> is a utility class that is used to batch-rename
 * a collection of file (one at a time).
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class FileRenamer {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Gets the new name for the given old name.
     *
     * @param oldName The old name.
     * @return Returns the new name.
     */
    public String getNewNameFor( String oldName ) {
        final StringBuilder newName = new StringBuilder();
        if ( m_name != null )
            newName.append( m_name );
        else
            newName.append( FileUtil.trimExtensionOf( oldName ) );
        if ( m_prepend != null )
            newName.insert( 0, m_prepend );
        if ( m_append != null )
            newName.append( m_append );
        if ( m_index >= 0 ) {
            final String index =
                TextUtil.zeroPad( m_index++, 10, m_indexDigits );
            if ( m_indexAfter ) {
                if ( m_indexSeparator != null )
                    newName.append( m_indexSeparator );
                newName.append( index );
            } else {
                if ( m_indexSeparator != null )
                    newName.insert( 0, m_indexSeparator );
                newName.insert( 0, index );
            }
        }
        newName.append( '.' );
        newName.append( m_extension );
        return newName.toString();
    }

    /**
     * Sets the new filename extension.  This method <b>must</b> be called at
     * least once.
     *
     * @param extension The new filename extension.
     */
    public void setExtension( String extension ) {
        m_extension = extension;
    }

    /**
     * Sets the indexing components for new names.
     *
     * @param start The starting index: if >= 0, new names will contain
     * indicies; if -1, they won't.
     * @param after If <code>true</code>, indicies will be placed after the
     * name (and separator) but before the extension; if <code>false</code>,
     * they will be placed before the name (and separator).
     * @param digits The minimum number of digits the index should be.  If the
     * number of digits comprising a given index is less than the number of
     * digits specified, the index is left-padded with sufficient zeros to
     * make the index have the minimum number of digits.
     * @param separator The separator to use between the base part of the name
     * and the index.  It may be <code>null</code> or the empty string to
     * indicate no separator should be included.
     */
    public void setIndexComponents( int start, boolean after, int digits,
                                    String separator ) {
        if ( start >= 0 && digits < 1 )
            throw new IllegalArgumentException();
        m_index = start;
        m_indexAfter = after;
        m_indexDigits = digits;
        m_indexSeparator = separator;
    }

    /**
     * Sets the name components for new names.
     *
     * @param prepend The string to be prepended to names (but after a leading
     * index, if any); may be <code>null</code> or the empty string to indicate
     * that nothing should be prepended.
     * @param name The main part of the name; may be <code>null</code> to
     * indicate that the original name should be kept.
     * @param append The string to be appended to names (but before a trailing
     * index, if any, and the extension); may be <code>null</code> or the empty
     * string to indicate that nothing should be appended.
     */
    public void setNameComponents( String prepend, String name, String append ) {
        m_append = append;
        m_name = name;
        m_prepend = prepend;
    }

    ////////// private ////////////////////////////////////////////////////////

    private String m_extension;
    private int m_index = -1;
    private boolean m_indexAfter;
    private int m_indexDigits;
    private String m_indexSeparator;
    private String m_append;
    private String m_name;
    private String m_prepend;

    public static void main( String[] args ) {
        final FileRenamer fr = new FileRenamer();
        fr.setExtension( "jpg" );
        fr.setNameComponents( null, "Vacation", null );
        fr.setIndexComponents( 1, false, 2, "-" );
        System.out.println( fr.getNewNameFor( "CRW_1234.CRW" ) );
    }
}
/* vim:set et sw=4 ts=4: */
