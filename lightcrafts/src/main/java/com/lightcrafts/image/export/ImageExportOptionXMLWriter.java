/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import java.io.File;
import java.io.IOException;

import org.w3c.dom.Element;

import com.lightcrafts.utils.xml.XMLUtil;

/**
 * An <code>ImageExportOptionXMLWriter</code> is-a
 * {@link ImageExportOptionWriter} for writing {@link ImageExportOption}s as
 * XML.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ImageExportOptionXMLWriter
    implements ImageExportOptionWriter {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>ImageExportOptionXMLWriter</code>.
     *
     * @param parent The {@link Element} to add child nodes to.
     */
    public ImageExportOptionXMLWriter( ImageExportOptions options,
                                       Element parent ) {
        m_options = options;
        m_parent = parent;
    }

    /**
     * Write a {@link BooleanExportOption}.
     *
     * @param o The {@link BooleanExportOption} to write.
     */
    public void write( BooleanExportOption o ) {
        final Element child =
            XMLUtil.addElementChildTo( m_parent, o.getName() );
        child.setAttribute( "value", Boolean.toString( o.getValue() ) );
    }

    /**
     * Write an {@link IntegerExportOption}.
     *
     * @param o The {@link IntegerExportOption} to write.
     */
    public void write( IntegerExportOption o ) {
        final Element child =
            XMLUtil.addElementChildTo( m_parent, o.getName() );
        child.setAttribute( "value", Integer.toString( o.getValue() ) );
    }

    /**
     * Write a {@link StringExportOption}.
     *
     * @param o The {@link StringExportOption} to write.
     */
    public void write( StringExportOption o ) {
        final Element child =
            XMLUtil.addElementChildTo( m_parent, o.getName() );
        child.setAttribute( "value", o.getValue() );
    }

    /**
     * Write the {@link ImageExportOptions}.
     */
    public void writeAll() throws IOException {
        m_parent = XMLUtil.addElementChildTo( m_parent, ExportOptionsTag );
        m_parent.setAttribute( TypeTag, m_options.getImageType().getName() );
        final File exportFile = m_options.getExportFile();
        if ( exportFile != null )
            m_parent.setAttribute( FileTag, exportFile.getAbsolutePath() );
        m_options.writeTo( this );
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * An XML tag constant used in save() and restore().
     */
    final static String ExportOptionsTag = "ExportOptions";

    /**
     * An XML tag constant used in save() and restore().
     */
    final static String TypeTag = "type";

    /**
     * An XML tag constant used in save() and restore().
     */
    final static String FileTag = "file";

    ////////// private ////////////////////////////////////////////////////////

    private final ImageExportOptions m_options;
    private Element m_parent;
}
/* vim:set et sw=4 ts=4: */
