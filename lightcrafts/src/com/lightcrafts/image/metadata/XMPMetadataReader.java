/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;

import org.w3c.dom.*;

import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.utils.xml.*;

import static com.lightcrafts.image.metadata.XMPConstants.*;

/**
 * An <code>XMPMetadataReader</code> is used to read XMP metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class XMPMetadataReader {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Reads an XMP metadata from an XMP {@link Document} and converts the
     * metadata therein to an {@link ImageMetadata} object.
     *
     * @param xmpDoc The XML {@link Document} to read the XMP from.
     * @return Returns a new {@link ImageMetadata} object containing the
     * metadata parsed from the XMP XML {@link Document}.
     * @see #readFrom(File)
     * @see #readFrom(InputStream)
     */
    public static ImageMetadata readFrom( Document xmpDoc ) {
        final Element rdfElement = XMPUtil.getRDFElementOf( xmpDoc );
        if ( rdfElement == null )
            return null;
        final ImageMetadata metadata = new ImageMetadata();
        readMetadata(
            rdfElement, XMP_DC_NS, XMP_DC_PREFIX, IPTCDirectory.class,
            metadata
        );
        readMetadata(                   // reads aux:Lens
            rdfElement, XMP_EXIF_AUX_NS, XMP_EXIF_AUX_PREFIX,
            CoreDirectory.class, metadata
        );
        readMetadata(
            rdfElement, XMP_EXIF_NS, XMP_EXIF_PREFIX, EXIFDirectory.class,
            metadata
        );
        readMetadata(
            rdfElement, XMP_IPTC_NS, XMP_IPTC_PREFIX, IPTCDirectory.class,
            metadata
        );
        readMetadata(
            rdfElement, XMP_TIFF_NS, XMP_TIFF_PREFIX, TIFFDirectory.class,
            metadata
        );
        readMetadata(                   // reads xap:Rating
            rdfElement, XMP_XAP_NS, XMP_XAP_PREFIX, CoreDirectory.class,
            metadata
        );
        return metadata;
    }

    /**
     * Reads an XMP XML metadata stream and converts the metadata therein to
     * an {@link ImageMetadata} object.
     *
     * @param file The {@link File} to read the XMP XML from.
     * @return Returns a new {@link ImageMetadata} object containing the
     * metadata parsed from the XMP XML metadata stream.
     * @see #readFrom(Document)
     * @see #readFrom(InputStream)
     */
    public static ImageMetadata readFrom( File file ) throws IOException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return readFrom(fis);
        }
    }

    /**
     * Reads an XMP XML metadata stream and converts the metadata therein to
     * an {@link ImageMetadata} object.
     *
     * @param is The {@link InputStream} to read the XMP XML from.
     * @return Returns a new {@link ImageMetadata} object containing the
     * metadata parsed from the XMP XML metadata stream.
     * @see #readFrom(Document)
     * @see #readFrom(File)
     */
    public static ImageMetadata readFrom( InputStream is ) throws IOException {
        return readFrom( XMLUtil.readDocumentFrom( is ) );
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Parse the metadata from a sequence of elements.
     *
     * @param elements The elements to parse.
     * @param dirPrefixFilter The {@link ElementPrefixFilter} to use.
     * @param dir The {@link ImageMetadataDirectory} to populate.
     */
    static void parseElements( Node[] elements,
                               ElementPrefixFilter dirPrefixFilter,
                               ImageMetadataDirectory dir ) {
        for ( Node node : elements ) {
            final Element dirElement = (Element)node;
            //
            // Use getTagName() since getLocalName() always returns null.
            //
            // final String tagName = dirElement.getLocalName();
            final String tagName = dirElement.getTagName().replaceAll( ".*:", "" );
            final ImageMetaTagInfo tagInfo = dir.getTagInfoFor( tagName );
            if ( tagInfo == null )
                continue;
            if ( dir.parseXMP( tagInfo, dirElement, dirPrefixFilter ) )
                continue;
            switch ( tagInfo.getType() ) {
                case META_UNDEFINED:
                case META_UNKNOWN:
                    continue;
            }
            final ImageMetaValue value = tagInfo.createValue();
            value.setIsChangeable( true );

            try {
                //
                // First, check to see if the element has an rdf:Alt or rdf:Seq
                // element as its child: if so, parse the sequence for multiple
                // values.
                //
                final Node child = XMLUtil.getFirstChildOf(
                    dirElement, m_rdfListElementFilter
                );
                if ( child != null )
                    value.setValues( readSeqList( (Element)child ) );
                else {
                    //
                    // Second, check to see if it has some other element(s) as
                    // its child(ren).  If so, parse them recursively.
                    //
                    final Node[] children =
                        XMLUtil.getChildrenOf( dirElement, dirPrefixFilter );
                    if ( children != null && children.length > 0 ) {
                        parseElements( children, dirPrefixFilter, dir );
                        continue;
                    }

                    //
                    // Lastly, see if its child is a text node.
                    //
                    final String text =
                        XMLUtil.getTextOfFirstTextChildOf( dirElement );
                    if ( text != null )
                        value.setValues( text.trim() );
                    else
                        value.setValues( "" );
                }

                dir.putValue( tagInfo.getID(), value );
            }
            catch ( Exception e ) {
                // ignore
            }
        }
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Construct an <code>XMPMetadataReader</code>.
     */
    private XMPMetadataReader() {
        // nothing
    }

    /**
     * Parse XML element attributes for metadata.  RDF sometimes uses a
     * shorthand notation that puts metadata in attributes rather than child
     * elements.
     *
     * @param atts The attributes to parse.
     * @param prefix The XML namespace prefix to match.
     * @param dir The {@link ImageMetadataDirectory} to populate.
     */
    private static void parseAttributes( NamedNodeMap atts, String prefix,
                                         ImageMetadataDirectory dir ) {
        for ( int i = 0; i < atts.getLength(); ++i ) {
            final Attr att = (Attr)atts.item( i );
            if ( !prefix.equals( att.getPrefix() ) )
                continue;
            final ImageMetaTagInfo tagInfo =
                dir.getTagInfoFor( att.getLocalName() );
            if ( tagInfo == null )
                continue;
            switch ( tagInfo.getType() ) {
                case META_UNDEFINED:
                case META_UNKNOWN:
                    continue;
            }
            try {
                final ImageMetaValue value = tagInfo.createValue();
                value.setIsChangeable( true );
                value.setValues( att.getValue() );
                dir.putValue( tagInfo.getID(), value );
            }
            catch ( Exception e ) {
                // ignore
            }
        }
    }

    /**
     * Reads the metadata for a particular directory from the XMP stream.
     *
     * @param rdfElement The <code>rdf</code> element containing the XMP
     * metadata for a particular directory.
     * @param dirNS The directory's XML namespace.
     * @param dirPrefix The directory's XML prefix.
     * @param dirClass The directory's {@link Class}.
     * @param metadata The {@link ImageMetadata} to read into.
     */
    private static void readMetadata(
        Element rdfElement, String dirNS, String dirPrefix,
        Class<? extends ImageMetadataDirectory> dirClass,
        ImageMetadata metadata )
    {
        //
        // Find the rdf element containing the metadata for the given
        // directory.
        //
        final ElementFilter dirFilter = new ElementFilter(
            XMP_RDF_PREFIX + ":Description", "xmlns:" + dirPrefix, dirNS
        );
        final Element rdfDirElement =
            (Element)XMLUtil.getFirstChildOf( rdfElement, dirFilter );
        if ( rdfDirElement == null )
            return;
        //
        // Get all the child elements of the RDF element that have the right
        // prefix.
        //
        final ElementPrefixFilter dirPrefixFilter =
            new ElementPrefixFilter( dirPrefix );
        final Node[] dirElements =
            XMLUtil.getChildrenOf( rdfDirElement, dirPrefixFilter );
        //
        // Parse the elements into a new directory.
        //
        final ImageMetadataDirectory dir =
            metadata.getDirectoryFor( dirClass, true );
        parseElements( dirElements, dirPrefixFilter, dir );
        //
        // See if this element has attributes that share the same prefix.  If
        // so, the element is using RDF shorthand to encode the metadata.
        //
        parseAttributes( rdfDirElement.getAttributes(), dirPrefix, dir );

        if ( dir.isEmpty() )
            metadata.removeDirectory( dirClass );
    }

    /**
     * Reads the values from the list elements of the given
     * <code>rdf:Seq</code> element.
     *
     * @param seqElement The <code>rdf:Seq</code> element to read.
     * @return Returns an array of {@link String}, one for the value of each
     * list element, or <code>null</code> if there are no list elements.
     */
    private static String[] readSeqList( Element seqElement ) {
        final Node[] listElements =
            XMLUtil.getChildrenOf( seqElement, m_rdfListItemElementFilter );
        if ( listElements.length == 0 )
            return null;
        final String[] values = new String[ listElements.length ];
        for ( int i = 0; i < listElements.length; ++i ) {
            final Element listElement = (Element)listElements[i];
            values[i] = XMLUtil.getTextOfFirstTextChildOf( listElement );
        }
        return values;
    }

    /**
     * An {@link ElementFilter} that returns only elements having the name
     * <code>rdf:Alt</code> or <code>rdf:Seq</code>.
     */
    private static final XMLFilter m_rdfListElementFilter =
        new OrXMLFilter(
            new ElementFilter( XMP_RDF_PREFIX + ":Alt" ),
            new ElementFilter( XMP_RDF_PREFIX + ":Seq" )
        );

    /**
     * An {@link ElementFilter} that returns only elements having the name
     * <code>rdf:li</code>.
     */
    private static final XMLFilter m_rdfListItemElementFilter =
        new ElementFilter( XMP_RDF_PREFIX + ":li" );

    ////////// main() for testing /////////////////////////////////////////////

    public static void main( String[] args ) throws IOException {
        final FileInputStream fis = new FileInputStream( args[0] );
        final ImageMetadata md = readFrom( fis );

        XMPMetadataWriter.mergeInto( md, new File( args[1] ) );

/*
        final Document xmpDoc = XMPUtil.createEmptyXMPDocument( false );
        md.toXMP( xmpDoc );

        XMLUtil.writeDocumentTo( xmpDoc, System.err );
*/
    }
}
/* vim:set et sw=4 ts=4: */
