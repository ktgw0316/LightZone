/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetaTags;

/**
 * A <code>KodakTags</code> defines the constants used for Kodak maker note
 * metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface KodakTags extends ImageMetaTags {

    /**
     * Burst mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>off</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>on</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned byte.
     */
    int KODAK_BURST_MODE                = 0x000A;

    /**
     * Digital zoom.
     * <p>
     * Type: Unsigned short.
     */
    int KODAK_DIGITAL_ZOOM              = 0x0068;

    /**
     * Exposure compensation.
     * <p>
     * Type: Signed short.
     */
    int KODAK_EXPOSURE_COMPENSATION     = 0x0024;

    /**
     * Exposure time.
     * <p>
     * Type: Unsigned long.
     */
    int KODAK_EXPOSURE_TIME             = 0x0020;

    /**
     * Flash fired.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>no</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>yes</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned byte.
     */
    int KODAK_FLASH_FIRED               = 0x005D;

    /**
     * F-Number.
     * <p>
     * Type: Unsigned short.
     */
    int KODAK_FNUMBER                   = 0x001E;

    /**
     * Focus mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>macro</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned byte.
     */
    int KODAK_FOCUS_MODE                = 0x0038;

    /**
     * Image height.
     * <p>
     * Type: Unsigned short.
     */
    int KODAK_IMAGE_HEIGHT              = 0x000E;

    /**
     * Image width.
     * <p>
     * Type: Unsigned short.
     */
    int KODAK_IMAGE_WIDTH               = 0x000C;

    /**
     * ISO.
     * <p>
     * Type: Unsigned short.
     */
    int KODAK_ISO                       = 0x0060;

    /**
     * ISO setting.  The special value 0 means &quot;quto&quot; otherwise it's
     * the ISO value.
     * <p>
     * Type: Unsigned short.
     */
    int KODAK_ISO_SETTING               = 0x005E;

    /**
     * Metering mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>multi-pattern</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>center-weighted</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>spot</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int KODAK_METERING_MODE             = 0x001C;

    /**
     * Camera model.
     *
     * Type: String (exactly 8 bytes, padded with spaces).
     */
    int KODAK_MODEL                     = 0x0000;

    /**
     * Month/day created.
     * <p>
     * Type: Unsigned byte[2].
     */
    int KODAK_MONTH_DAY_CREATED         = 0x0012;

    /**
     * Quality.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>fine</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>normal</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned byte.
     */
    int KODAK_QUALITY                   = 0x0009;

    /**
     * Sequence number.
     * <p>
     * Type: Unsigned byte.
     */
    int KODAK_SEQUENCE_NUMBER           = 0x001D;

    /**
     * Sharpness.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>soft</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>hard</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Signed byte.
     */
    int KODAK_SHARPNESS                 = 0x006B;

    /**
     * Shutter mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>auto</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>aperture priority</td></tr>
     *      <tr><td>32 =&nbsp;</td><td>manual</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned byte.
     */
    int KODAK_SHUTTER_MODE              = 0x001B;

    /**
     * Time created.
     * <p>
     * Type: Unsigned byte[4].
     */
    int KODAK_TIME_CREATED              = 0x0014;

    /**
     * Total zoom.
     * <p>
     * Type: Unsigned short.
     */
    int KODAK_TOTAL_ZOOM                = 0x0062;

    /**
     * White balance.
     * <p>
     * Type: Unsigned byte.
     */
    int KODAK_WHITE_BALANCE             = 0x0040;

    /**
     * Year created.
     * <p>
     * Type: Unsigned short.
     */
    int KODAK_YEAR_CREATED              = 0x0010;

    int KODAK_UNKNOWN_0x18              = 0x0018;
    int KODAK_UNKNOWN_0x26              = 0x0026;
    int KODAK_UNKNOWN_0x28              = 0x0028;
    int KODAK_UNKNOWN_0x2C              = 0x002C;
    int KODAK_UNKNOWN_0x30              = 0x0030;
    int KODAK_UNKNOWN_0x34              = 0x0034;
    int KODAK_UNKNOWN_0x3A              = 0x003A;
    int KODAK_UNKNOWN_0x3C              = 0x003C;
    int KODAK_UNKNOWN_0x3E              = 0x003E;
    int KODAK_UNKNOWN_0x42              = 0x0042;
    int KODAK_UNKNOWN_0x46              = 0x0046;
    int KODAK_UNKNOWN_0x4A              = 0x004A;
    int KODAK_UNKNOWN_0x4E              = 0x004E;
    int KODAK_UNKNOWN_0x52              = 0x0052;
    int KODAK_UNKNOWN_0x56              = 0x0056;
    int KODAK_UNKNOWN_0x5A              = 0x005A;

}
/* vim:set et sw=4 ts=4: */
