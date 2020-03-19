/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import java.io.IOException;

import com.lightcrafts.utils.xml.XmlNode;
import com.lightcrafts.utils.xml.XMLException;

/**
 * An <code>ImageExportOption</code> is an abstract base class for all options
 * that can be specified by a user when exporting an image to a particular
 * image file format.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class ImageExportOption {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Checks for equality of this <code>ImageExportOption</code> to another
     * object.
     *
     * @param o The object to check against.
     * @return Returns <code>true</code> only if the other object is the same
     * kind of <code>ImageExportOption</code> and their names and values are
     * equals.
     */
    public abstract boolean equals( Object o );

    /**
     * Gets the name of this option.
     *
     * @return Returns said name.
     */
    public String getName() {
        return m_name;
    }

    /**
     * Returns the hash code of this <code>ImageExportOption</code>.
     *
     * @return Returns said hash code.
     */
    public final int hashCode() {
        return m_name.hashCode() + 1;
    }

    /**
     * Sets the value of this option.
     *
     * @param newValue The new value.
     */
    public abstract void setValue( String newValue );

    /**
     * Preserve the state of this ImageExportOption.
     *
     * @param node An XmlNode context in which to save state.
     */
    public abstract void save( XmlNode node ) ;

    /**
     * Restore the state of this ImageExportOption.
     *
     * @param node An XmlNode in which state has been saved.
     * @throws XMLException If the contents of the given node are invalid.
     */
    public abstract void restore( XmlNode node ) throws XMLException;

    /**
     * Read the state of this <code>ImageExportOption</code>.
     *
     * @param r The {@link ImageExportOptionReader} to read from.
     */
    public abstract void readFrom( ImageExportOptionReader r )
        throws IOException;

    /**
     * Write the state of this <code>ImageExportOption</code>.
     *
     * @param w The {@link ImageExportOptionWriter} to read from.
     */
    public abstract void writeTo( ImageExportOptionWriter w )
        throws IOException;

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Constructs an <code>ImageExportOption</code>.
     *
     * @param name The name of the option.
     * @param options The {@link ImageExportOptions} of which this option is a
     * member.
     */
    protected ImageExportOption( String name, ImageExportOptions options ) {
        m_name = name;
        options.addOption( this );
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * An XML tag constant used in save() and restore().
     */
    final static String ValueTag = "value";

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The name of this option.
     */
    private final String m_name;
}
/* vim:set et sw=4 ts=4: */
