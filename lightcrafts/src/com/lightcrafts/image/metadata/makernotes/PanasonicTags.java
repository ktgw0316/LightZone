/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetaTags;

/**
 * A <code>PanasonicTags</code> defines the constants used for Panasonic maker
 * note metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface PanasonicTags extends ImageMetaTags {

    /**
     * Audio
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>yes</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>no</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_AUDIO                     = 0x0020;

    /**
     * Burst mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>off</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>low/high quality</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>infinite</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_BURST_MODE                = 0x002A;

    /**
     * Color effect.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>off</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>warm</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>cool</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>black &amp; white</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>sepia</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_COLOR_EFFECT              = 0x0028;

    /**
     * Color mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>natural</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_COLOR_MODE                = 0x0032;

    /**
     * Contrast.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>high</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_CONTRAST                  = 0x002C;

    /**
     * Firmware version.
     * <p>
     * Type: Undefined.
     */
    int PANASONIC_FIRMWARE_VERSION          = 0x0002;

    /**
     * Flash bias.
     * <p>
     * Type: Signed short.
     */
    int PANASONIC_FLASH_BIAS                = 0x0024;

    /**
     * Focus mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>manual</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>auto, focus button</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>auto, continuous</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_FOCUS_MODE                = 0x0007;

    /**
     * Image quality.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>high</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>very high</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td><td>raw</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_IMAGE_QUALITY             = 0x0001;

    /**
     * Image stabilizer.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>on, mode 1</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>off</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>on, mode 2</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_IMAGE_STABILIZER          = 0x001A;

    /**
     * Internal serial number.
     * <p>
     * Type: TODO
     */
    int PANASONIC_INTERNAL_SERIAL_NUMBER    = 0x0025;

    /**
     * Macro mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>on</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>off</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>257 =&nbsp;</td><td>tele-macro</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_MACRO_MODE                = 0x001C;

    /**
     * Noise reduction.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>standard</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>high</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_NOISE_REDUCTION           = 0x002D;

    /**
     * Rotation.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>landscape</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>90 CW</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>8 =&nbsp;</td><td>90 CCW</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_ROTATION                  = 0x0030;

    /**
     * Self timer.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>off</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>10s</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>2s</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_SELF_TIMER                = 0x002E;

    /**
     * Sequence number.
     * <p>
     * Type: Unsigned long.
     */
    int PANASONIC_SEQUENCE_NUMBER           = 0x002B;

    /**
     * Shooting mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>portrait</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>scenery</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>sports</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>night portrait</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>program</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td><td>aperture priority</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>8 =&nbsp;</td><td>shutter program</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>9 =&nbsp;</td><td>macro</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>11 =&nbsp;</td><td>manual</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>13 =&nbsp;</td><td>panning</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>18 =&nbsp;</td><td>fireworks</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>19 =&nbsp;</td><td>party</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>20 =&nbsp;</td><td>snow</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>21 =&nbsp;</td><td>night scenery</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>22 =&nbsp;</td><td>food</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_SHOOTING_MODE             = 0x001F;

    /**
     * Spot mode.  Note that the value of interest is the second number only.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>on</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>16 =&nbsp;</td><td>off</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short[2].
     */
    int PANASONIC_SPOT_MODE                 = 0x000F;

    /**
     * White balance.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>daylight</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>cloudy</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>halogen</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>manual</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>8 =&nbsp;</td><td>flash</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>10 =&nbsp;</td><td>black &amp; white</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_WHITE_BALANCE             = 0x0003;

    /**
     * White balance bias.
     * <p>
     * Type: Signed short.
     */
    int PANASONIC_WHITE_BALANCE_BIAS        = 0x0023;

}
/* vim:set et sw=4 ts=4: */
