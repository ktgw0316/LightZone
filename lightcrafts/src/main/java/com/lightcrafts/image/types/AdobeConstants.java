/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

/**
 * An <code>AdoneConstants</code> defines some constants for Adobe.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface AdobeConstants {

    /** The size (in bytes) of an Adobe JPEG APPC segment. */
    int ADOBE_APPC_SEGMENT_SIZE = 6;

    /** The size (in bytes) of an Adobe JPEG APPE segment. */
    int ADOBE_APPE_SEGMENT_SIZE = 12;

    /** Adobe Color Transformation Code: Unknown. */
    byte ADOBE_CTT_UNKNOWN = 0;

    /** Adobe Color Transformation Code: YCbCr. */
    byte ADOBE_CTT_YCBCR   = 1;

    /** Adobe Color Transformation Code: YCCK. */
    byte ADOBE_CTT_YCCK    = 2;

    /**
     * The minimum size for an Adobe resource block.  The bytes are:
     *  <blockquote>
     *    <table cellpadding="0" cellspacing="0">
     *      <tr>
     *        <td align="right">0-3 =&nbsp;</td>
     *        <td><code>8BIM</code></td>
     *      </tr>
     *      <tr>
     *        <td align="right">4-5 =&nbsp;</td>
     *        <td>Resource ID (big endian)</td>
     *      </tr>
     *      <tr>
     *        <td align="right">6-9 =&nbsp;</td>
     *        <td>Resource name (Pascal string padded to be even length)</td>
     *      </tr>
     *      <tr>
     *        <td align="right">10-... =&nbsp;</td>
     *        <td>Resource data</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int ADOBE_RESOURCE_BLOCK_MIN_SIZE = 14;

    /**
     * Photoshop 3.0 identification string; used inside the APPE JPEG segment
     * header for IPTC metadata.
     */
    String PHOTOSHOP_3_IDENT = "Photoshop 3.0";

    /**
     * Thumbnail resource ID for Photoshop 5.0 and later; used inside the
     * <code>TIFF_PHOTOSHOP_IMAGE_RESOURCES</code> metadata tag.
     */
    int PHOTOSHOP_5_THUMBNAIL_RESOURCE_ID = 1036;

    /**
     * Photoshop's creator code; used as a signature for Photoshop data blocks.
     */
    String PHOTOSHOP_CREATOR_CODE = "8BIM";

    /**
     * Photoshop IPTC marker; used inside the APPE JPEG segment header for IPTC
     * metadata.
     */
    short PHOTOSHOP_IPTC_RESOURCE_ID = 0x0404;

}
/* vim:set et sw=4 ts=4: */
