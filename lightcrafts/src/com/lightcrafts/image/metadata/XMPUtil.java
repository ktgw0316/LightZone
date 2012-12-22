/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.lightcrafts.app.Application;
import com.lightcrafts.image.metadata.values.ByteMetaValue;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import com.lightcrafts.image.metadata.values.UndefinedMetaValue;
import com.lightcrafts.image.metadata.values.UnsignedByteMetaValue;
import com.lightcrafts.utils.xml.ElementFilter;
import com.lightcrafts.utils.xml.XMLUtil;
import com.lightcrafts.utils.xml.NodeTypeFilter;

import static com.lightcrafts.image.metadata.XMPConstants.*;

/**
 * <code>XMPUtil</code> is a set of utility functions for dealing with XMP
 * metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class XMPUtil {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Creates a new empty XMP document.
     *
     * @param includeXMPPacket If <code>true</code>, XMP packet processing
     * instructions are included in the new document.
     * @return Returns said document.
     */
    public static Document createEmptyXMPDocument( boolean includeXMPPacket ) {
        final StringBuilder s = new StringBuilder();
        if ( includeXMPPacket )
            s.append( XMP_XPACKET_BEGIN );
        s.append( XMP_EMPTY_DOCUMENT_STRING );
        if ( includeXMPPacket )
            s.append( XMP_XPACKET_END );
        try {
            return XMLUtil.readDocumentFrom( s.toString() );
        }
        catch ( IOException e ) {
            throw new Error( e );
        }
    }

    /**
     * Create an empty <code>rdf:Description</code> element.
     *
     * @param xmpDoc The XMP XML {@link Document} to create the RDF element as
     * part of.
     * @param nsURI The XML namespace URI to use.
     * @param prefix The XML prefix to use.
     * @return Returns said <code>rdf:Description</code> element.
     */
    public static Element createRDFDescription( Document xmpDoc,
                                                String nsURI, String prefix ) {
        final Element rdfDescElement =
            xmpDoc.createElementNS( XMP_RDF_NS, XMP_RDF_PREFIX + ":Description" );
        rdfDescElement.setAttribute( XMP_RDF_PREFIX + ":about", "" );
        rdfDescElement.setAttribute( "xmlns:" + prefix, nsURI );
        return rdfDescElement;
    }

    /**
     * Gets the LightZone description element child of an <code>rdf:RDF</code>
     * of an XMP packet document.
     *
     * @param rdfElement The <code>rdf:RDF</code> element of an XMP packet
     * document.
     * @param create If <code>true</code>, create an empty LightZone
     * description element and append it as a child of the <code>rdf:RDF</code>
     * element if no LightZone description element exists.
     * @return Returns the LightZone description element or <code>null</code>
     * if there is no such element and <code>create</code> is false.
     * @see #getRDFElementOf(Document)
     */
    public static Element getLZNDescription( Element rdfElement,
                                             boolean create )
        throws IOException
    {
        final ElementFilter filter =
            new ElementFilter(
                XMP_RDF_PREFIX + ":Description",
                "xmlns:lzn", Application.LznNamespace
            );
        Element lznDescElement =
            (Element)XMLUtil.getFirstChildOf( rdfElement, filter );
        if ( lznDescElement != null || !create )
            return lznDescElement;

        final Document emptyLZNDoc =
            XMLUtil.readDocumentFrom( XMP_EMPTY_LZN_DESCRIPTION_STRING );
        lznDescElement = (Element)rdfElement.getOwnerDocument().importNode(
            emptyLZNDoc.getDocumentElement(), true
        );
        rdfElement.appendChild( lznDescElement );
        return lznDescElement;
    }

    /**
     * Gets the LZN document from the given XMP document.
     *
     * @param xmpDoc The XMP XML {@link Document} to get the LZN document from.
     * @return Returns said {@link Document} or <code>null</code> if the XMP
     * document doesn't contain an LZN document.
     */
    public static Document getLZNDocumentFrom( Document xmpDoc )
        throws IOException
    {
        final Element rdfElement = getRDFElementOf( xmpDoc );
        final Element lznDescription = getLZNDescription( rdfElement, false );
        if ( lznDescription == null )
            return null;
        final Element child = (Element)XMLUtil.getFirstChildOf(
            lznDescription, new NodeTypeFilter( Node.ELEMENT_NODE )
        );
        if ( child == null )
            return null;
        if ( child.getLocalName().equals( "xmpwrapper" ) ) {
            final Node lznNode = child.getFirstChild();
            if ( lznNode == null )
                return null;
            final String lznText = lznNode.getTextContent();
            if ( lznText == null )
                return null;
            return XMLUtil.readDocumentFrom( lznText );
        }
        final Document lznDoc = XMLUtil.createDocument();
        lznDoc.appendChild( lznDoc.importNode( child, true ) );
        return lznDoc;
    }

    /**
     * Gets the <code>rdf:RDF</code> element of an XMP packet document.
     *
     * @param xmpDoc The XMP packet document to get the <code>rdf:RDF</code>
     * element of.
     * @return Returns said element or <code>null</code> if the given document
     * doesn't contain an <code>rdf:RDF</code> element.
     */
    public static Element getRDFElementOf( Document xmpDoc ) {
        return (Element)XMLUtil.getFirstChildOf(
            xmpDoc.getDocumentElement(),
            new ElementFilter( XMP_RDF_PREFIX + ":RDF" )
        );
    }

    /**
     * Gets the XMP data from an {@link ImageMetaValue}.  The reason this
     * function exists is because, despite the XMP specification clearly
     * stating that the type of the XMP data is unsigned byte, it's been seen
     * to be undefined in practice; hence, this functions tests for both cases.
     *
     * @param value The {@link ImageMetaValue} to get the XMP data from.  It is
     * expected to be either an instance of {@link UnsignedByteMetaValue} or
     * {@link UndefinedMetaValue}.
     * @return Returns the raw bytes of the XMP data or <code>null</code> if
     * either the value doesn't appear to contain XMP data or is
     * <code>null</code>.
     */
    public static byte[] getXMPDataFrom( ImageMetaValue value ) {
        if ( value instanceof UnsignedByteMetaValue )
            return ((ByteMetaValue)value).getByteValues();
        if ( value instanceof UndefinedMetaValue )
            return ((UndefinedMetaValue)value).getUndefinedValue();
        return null;
    }

    /**
     * Merge the metadata for the EXIF, IPTC, TIFF, and XAP directories from
     * one XMP document into another.
     *
     * @param newXMPDoc The {@link Document} containing the new XMP metadata
     * for a particular directory.  This document is not modified.
     * @param oldXMPDoc The {@link Document} containing the old XMP metadata
     * for a particular directory.  This document has the new XMP metadata
     * merged into it.
     * @return Returns <code>oldXMPDoc</code>.
     * @see #mergeMetadata(Document,Document,String,String)
     */
    public static Document mergeMetadata( Document newXMPDoc,
                                          Document oldXMPDoc ) {
        mergeMetadata(
            newXMPDoc, oldXMPDoc, XMP_DC_NS, XMP_DC_PREFIX
        );
        mergeMetadata(
            newXMPDoc, oldXMPDoc, XMP_EXIF_NS, XMP_EXIF_PREFIX
        );
        mergeMetadata(
            newXMPDoc, oldXMPDoc, XMP_IPTC_NS, XMP_IPTC_PREFIX
        );
        mergeMetadata(
            newXMPDoc, oldXMPDoc, XMP_TIFF_NS, XMP_TIFF_PREFIX
        );
        mergeMetadata(
            newXMPDoc, oldXMPDoc, XMP_XAP_NS, XMP_XAP_PREFIX
        );
        return oldXMPDoc;
    }

    /**
     * Merge the metadata for a given directory from one XMP document into
     * another.
     *
     * @param newXMPDoc The {@link Document} containing the new XMP metadata
     * for a particular directory.  This document is not modified.
     * @param oldXMPDoc The {@link Document} containing the old XMP metadata
     * for a particular directory.  This document has the new XMP metadata
     * merged into it.
     * @param dirNS The directory's XML namespace.
     * @param dirPrefix The directory's XML prefix.
     * @see #mergeMetadata(Document,Document)
     */
    public static void mergeMetadata( Document newXMPDoc, Document oldXMPDoc,
                                      String dirNS, String dirPrefix ) {
        final Element newRDFElement = getRDFElementOf( newXMPDoc );
        final Element oldRDFElement = getRDFElementOf( oldXMPDoc );
        //
        // Find the rdf element containing the metadata for the given
        // directory.
        //
        final ElementFilter dirFilter = new ElementFilter(
            XMP_RDF_PREFIX + ":Description", "xmlns:" + dirPrefix, dirNS
        );
        Node newRDFDirElement =
            XMLUtil.getFirstChildOf( newRDFElement, dirFilter );
        final Element oldRDFDirElement =
            (Element)XMLUtil.getFirstChildOf( oldRDFElement, dirFilter );
        if ( newRDFDirElement == null ) {
            if ( oldRDFDirElement != null ) {
                //
                // The new document doesn't contain the RDF element of interest
                // so remove it from the old document.
                //
                final Node parent = oldRDFDirElement.getParentNode();
                parent.removeChild( oldRDFDirElement );
            }
            return;
        }

        final Document oldDocument = oldRDFElement.getOwnerDocument();
        newRDFDirElement = oldDocument.importNode( newRDFDirElement, true );

        //
        // See if the existing metadata has metadata for the directory we're
        // doing: if so, replace it; if not, append it.
        //
        if ( oldRDFDirElement != null )
            oldRDFElement.replaceChild( newRDFDirElement, oldRDFDirElement );
        else
            oldRDFElement.appendChild( newRDFDirElement );

/*
        // This old code does a node-by-node merge.

        final ElementPrefixFilter dirPrefixFilter =
            new ElementPrefixFilter( dirPrefix );
        final Node[] newDirElements =
            XMLUtil.getChildrenOf( newRDFDirElement, dirPrefixFilter );
        for ( int i = 0; i < newDirElements.length; ++i ) {
            final Element newDirElement =
                (Element)oldDocument.importNode( newDirElements[i], true );
            final Element oldDirElement = (Element)XMLUtil.getFirstChildOf(
                oldRDFDirElement,
                new ElementFilter( newDirElement.getTagName() )
            );
            if ( oldDirElement != null )
                oldRDFDirElement.replaceChild( newDirElement, oldDirElement );
            else
                oldRDFDirElement.appendChild( newDirElement );
        }
*/
    }

}
/* vim:set et sw=4 ts=4: */
