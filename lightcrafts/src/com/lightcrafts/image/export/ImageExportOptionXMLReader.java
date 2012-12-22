/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.export;

import com.lightcrafts.utils.xml.XmlNode;
import com.lightcrafts.utils.xml.XMLUtil;
import com.lightcrafts.utils.xml.ElementFilter;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.image.types.ImageType;

import java.io.IOException;
import java.io.File;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * An <code>ImageExportOptionXMLReader</code> is-a
 * {@link ImageExportOptionReader} for writing {@link ImageExportOption}s as
 * XML.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class ImageExportOptionXMLReader
    implements ImageExportOptionReader {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Construct an <code>ImageExportOptionXMLReader</code>.
     *
     * @param parent The {@link XmlNode} to add child nodes to.
     */
    public ImageExportOptionXMLReader( Element parent ) throws IOException {
        final Node node = XMLUtil.getFirstChildOf(
            parent,
            new ElementFilter( ImageExportOptionXMLWriter.ExportOptionsTag )
        );
        if ( node == null )
            throw new XMLException( ImageExportOptionXMLWriter.ExportOptionsTag + "\" expected" );
        m_parent = (Element)node;
    }

    /**
     * Read a {@link BooleanExportOption}.
     *
     * @param o The {@link BooleanExportOption} to read.
     */
    public void read( BooleanExportOption o ) throws IOException {
        readImpl( o );
    }

    /**
     * Read an {@link IntegerExportOption}.
     *
     * @param o The {@link IntegerExportOption} to read.
     */
    public void read( IntegerExportOption o ) throws IOException {
        readImpl( o );
    }

    /**
     * Read a {@link StringExportOption}.
     *
     * @param o The {@link StringExportOption} to read.
     */
    public void read( StringExportOption o ) throws IOException {
        readImpl( o );
    }

    /**
     * Reads the XML and reconstitutes an {@link ImageExportOptions}.
     *
     * @return Returns a new {@link ImageExportOptions}.
     */
    public ImageExportOptions readAll() throws IOException {
        final String typeName =
            m_parent.getAttribute( ImageExportOptionXMLWriter.TypeTag );
        final ImageType type = ImageType.getImageTypeByName( typeName );
        if ( type == null )
            throw new XMLException(
                "Unrecognized image export type: \"" + typeName + '"'
            );
        final ImageExportOptions options = type.newExportOptions();
        if ( m_parent.hasAttribute( ImageExportOptionXMLWriter.FileTag ) ) {
            final String exportFileName =
                m_parent.getAttribute( ImageExportOptionXMLWriter.FileTag );
            final File exportFile = new File( exportFileName );
            options.setExportFile( exportFile );
        }
        options.readFrom( this );
        return options;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     *
     * @param o
     * @throws IOException
     */
    private void readImpl( ImageExportOption o ) throws IOException {
        final Node child = XMLUtil.getFirstChildOf(
            m_parent, new ElementFilter( o.getName() )
        );
        if ( child == null )
            return;
        final String value = ((Element)child).getAttribute( "value" );
        if ( value.length() == 0 )
            throw new IOException( "empty value" );
        o.setValue( value );
    }

    private final Element m_parent;
}
/* vim:set et sw=4 ts=4: */
