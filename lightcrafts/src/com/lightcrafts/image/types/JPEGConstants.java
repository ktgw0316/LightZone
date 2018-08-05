/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import static com.lightcrafts.image.metadata.XMPConstants.XMP_XAP_NS;

/**
 * A <code>JPEGConstants</code> defines some constants for JPEG files.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface JPEGConstants {

    /**
     * The size of an ICC profile header (in bytes).
     * The bytes are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td>0-10&nbsp;</td>
     *        <td>String: <code>ICC_PROFILE</code></td>
     *      </tr>
     *      <tr>
     *        <td>11&nbsp;</td>
     *        <td>String terminator: <code>0x00</code></td>
     *      </tr>
     *      <tr>
     *        <td>12&nbsp;</td>
     *        <td>Chunk number</td>
     *      </tr>
     *      <tr>
     *        <td>13&nbsp;</td>
     *        <td>Total number of chunks</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int ICC_PROFILE_HEADER_SIZE = 14;

    /**
     * JFIF APP0 data is stored in this segment.
     */
    byte JPEG_APP0_MARKER  = (byte)0xE0;

    /**
     * EXIF or XMP metadata is stored in this segment.
     */
    byte JPEG_APP1_MARKER  = (byte)0xE1;

    /**
     * ICC profile data is stored in this segment.
     */
    byte JPEG_APP2_MARKER  = (byte)0xE2;

    /**
     * LightZone data is stored in this segment.
     */
    byte JPEG_APP4_MARKER  = (byte)0xE4;

    /**
     * Adobe-specific Embed marker is stored in this segment.
     */
    byte JPEG_APPC_MARKER  = (byte)0xEC;

    /**
     * IPTC metadata is stored in this segment.
     */
    byte JPEG_APPD_MARKER  = (byte)0xED;

    /**
     * Adobe-specific data is stored in this segment.
     */
    byte JPEG_APPE_MARKER  = (byte)0xEE;

    /**
     * Comment marker.
     */
    byte JPEG_COMMENT_MARKER = (byte)0xFE;

    /**
     * End-of-image marker: marks the end of all segments.
     */
    byte JPEG_EOI_MARKER = (byte)0xD9;

    /**
     * All JPEG segments start with this byte.
     */
    byte JPEG_MARKER_BYTE = (byte)0xFF;

    /**
     * The maximum segment size.  (It's 64K - 2 bytes for the length.)
     */
    int JPEG_MAX_SEGMENT_SIZE = 65533;

    /**
     * Start-of-image marker: marks the very beginning of a JPEG file.
     */
    byte JPEG_SOI_MARKER = (byte)0xD8;

    /**
     * Start-of-scan marker: marks the start of the JPEG image data.
     */
    byte JPEG_SOS_MARKER = (byte)0xDA;

    /**
     * The size of the XMP header in the APP1 segment.
     */
    int JPEG_XMP_HEADER_SIZE = XMP_XAP_NS.length() + 1 /* for null */;
}
/* vim:set et sw=4 ts=4: */
