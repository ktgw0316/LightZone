/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.types.TIFFConstants;

import static com.lightcrafts.image.types.AdobeConstants.*;

/**
 * An <code>IPTCConstants</code> defines some constants for IPTC metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface IPTCConstants {

    /**
     * The size of an IPTC date string (in bytes): YYYYMMDD.
     */
    int IPTC_DATE_SIZE = 8;

    /**
     * The size of an IPTC header (in bytes).
     * The bytes are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td>0&nbsp;</td><td>{@link #IPTC_TAG_START_BYTE}</td>
     *      </tr>
     *      <tr>
     *        <td>1&nbsp;</td><td>directory type</td>
     *      </tr>
     *      <tr>
     *        <td>2&nbsp;</td><td>tag type</td>
     *      </tr>
     *      <tr>
     *        <td>3-4&nbsp;</td><td>byte-count</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int IPTC_ENTRY_HEADER_SIZE = 5;

    /**
     * @see TIFFConstants#TIFF_INT_SIZE
     */
    int IPTC_INT_SIZE = TIFFConstants.TIFF_INT_SIZE;

    /**
     * The size of an IPTC header in a JPEG file (in bytes).
     * The bytes are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td align="right">0-13&nbsp;</td>
     *        <td><code>Photoshop 3.0\0</code></td>
     *      </tr>
     *      <tr>
     *        <td align="right">14-17&nbsp;</td>
     *        <td><code>8BIM</code></td>
     *      </tr>
     *      <tr>
     *        <td align="right">18-19&nbsp;</td>
     *        <td>0x0404</td>
     *      </tr>
     *      <tr>
     *        <td align="right">20-21&nbsp;</td>
     *        <td>resource name (Pascal string, usually empty)</td>
     *      </tr>
     *      <tr>
     *        <td align="right">22-25&nbsp;</td>
     *        <td>data length</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int IPTC_JPEG_HEADER_SIZE =
        PHOTOSHOP_3_IDENT.length() + 1 + PHOTOSHOP_CREATOR_CODE.length()
        + 2 + 2 + 4;

    /**
     * The ID used for the IPTC Envelope Record data.
     */
    int IPTC_RECORD_ENV = 1;

    /**
     * The ID used for the IPTC Application Record data.
     */
    int IPTC_RECORD_APP = 2;

    /**
     * @see TIFFConstants#TIFF_SHORT_SIZE
     */
    int IPTC_SHORT_SIZE = TIFFConstants.TIFF_SHORT_SIZE;

    /**
     * An IPTC tag must start with this byte.
     */
    byte IPTC_TAG_START_BYTE = (byte)0x1C;

}
/* vim:set et sw=4 ts=4: */
