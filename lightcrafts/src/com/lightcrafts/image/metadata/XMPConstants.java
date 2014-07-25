/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.text.SimpleDateFormat;

import com.lightcrafts.app.Application;

/**
 * <code>XMPConstants</code> defines constants for XMP.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface XMPConstants {

    /** This is the date format required by XMP. */
    SimpleDateFormat ISO_8601_DATE_FORMAT =
        new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );

    /** The Dublin Core namespace URI. */
    String XMP_DC_NS = "http://purl.org/dc/elements/1.1/";

    /** The Dublin Core namespace prefix. */
    String XMP_DC_PREFIX = "dc";

    /** The EXIF auxiliary namespace URI. */
    String XMP_EXIF_AUX_NS = "http://ns.adobe.com/exif/1.0/aux/";

    /** The EXIF auxiliary namespace prefix. */
    String XMP_EXIF_AUX_PREFIX = "aux";

    /** The EXIF namespace URI. */
    String XMP_EXIF_NS = "http://ns.adobe.com/exif/1.0/";

    /** The EXIF namespace prefix. */
    String XMP_EXIF_PREFIX = "exif";

    /** The IPTC Core namespace URI. */
    String XMP_IPTC_NS = "http://iptc.org/std/Iptc4xmpCore/1.0/xmlns/";

    /** The IPTC Core namespace prefix. */
    String XMP_IPTC_PREFIX = "Iptc4xmpCore";

    /** The XMP namespace URI. */
    String XMP_NS = "adobe:ns:meta/";

    /** The value for the XMP <code>id</code> attribute. */
    String XMP_PACKET_ID = "W5M0MpCehiHzreSzNTczkc9d";

    /** The RDF namespace URI. */
    String XMP_RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#";

    /** The RDF namespace prefix. */
    String XMP_RDF_PREFIX = "rdf";

    /** The TIFF namespace URI. */
    String XMP_TIFF_NS = "http://ns.adobe.com/tiff/1.0/";

    /** The TIFF namespace prefix. */
    String XMP_TIFF_PREFIX = "tiff";

    /** The XAP namespace URI. */
    String XMP_XAP_NS = "http://ns.adobe.com/xap/1.0/";

    /** The XAP namespace prefix. */
    // String XMP_XAP_PREFIX = "xap";
    String XMP_XAP_PREFIX = "xmp";

    /** The XMP XPacket begin processing instruction. */
    String XMP_XPACKET_BEGIN =
        "<?xpacket begin='' id='" + XMP_PACKET_ID + "'?>";

    /** The XMP XPacket end processing instruction. */
    String XMP_XPACKET_END = "<?xpacket end='w'?>";

    /**
     * The text for an empty XMP document.
     * @see XMPUtil#createEmptyXMPDocument(boolean)
     */
    String XMP_EMPTY_DOCUMENT_STRING =
        "<x:xmpmeta xmlns:x='" + XMP_NS + "'>\n" +
        "  <" + XMP_RDF_PREFIX + ":RDF xmlns:" + XMP_RDF_PREFIX + "='"
            + XMP_RDF_NS + "'/>\n" +
        "</x:xmpmeta>\n";

    /**
     * The text for an empty LZN XMP document.
     */
    String XMP_EMPTY_LZN_DESCRIPTION_STRING =
        '<' + XMP_RDF_PREFIX + ":Description " +
          XMP_RDF_PREFIX + ":about='' xmlns:" + XMP_RDF_PREFIX + "='"
            + XMP_RDF_NS + "' xmlns:lzn='" + Application.LznNamespace + "'/>";

}
/* vim:set et sw=4 ts=4: */
