/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetaTags;

/**
 * A <code>FujiTags</code> defines the constants used for Fuji maker note
 * metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface FujiTags extends ImageMetaTags {

    /**
     * Auto-bracketing.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_AUTO_BRACKETING    = 0x1100;

    /**
     * Blur warning.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>none</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>warning</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_BLUR_WARNING       = 0x1300;

    /**
     * Contrast.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0x000 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>0x100 =&nbsp;</td><td>high</td></tr>
     *      <tr><td>0x200 =&nbsp;</td><td>low</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_CONTRAST           = 0x1004;

    /**
     * Exposure warning.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>none</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>warning</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_EXPOSURE_WARNING   = 0x1302;

    /**
     * Flash mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>auto</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>red-eye reduction</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_FLASH_MODE         = 0x1010;

    /**
     * Flash strength.
     * <p>
     * Type: Signed rational.
     */
    int FUJI_FLASH_STRENGTH     = 0x1011;

    /**
     * Focus mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>auto</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>manual</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_FOCUS_MODE         = 0x1021;

    /**
     * Focus warning.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>none</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>warning</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_FOCUS_WARNING      = 0x1301;

    /**
     * Macro mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_MACRO_MODE         = 0x1020;

    int FUJI_MAX_FOCAL_LENGTH   = 0x1405;
    int FUJI_MAX_APER_AT_MIN_FL = 0x1406;
    int FUJI_MAX_APER_AT_MAX_FL = 0x1407;
    int FUJI_MIN_FOCAL_LENGTH   = 0x1404;

    /**
     * Picture mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0x000 =&nbsp;</td><td>auto</td></tr>
     *      <tr><td>0x001 =&nbsp;</td><td>portrait</td></tr>
     *      <tr><td>0x002 =&nbsp;</td><td>landscape</td></tr>
     *      <tr><td>0x004 =&nbsp;</td><td>sports</td></tr>
     *      <tr><td>0x005 =&nbsp;</td><td>night</td></tr>
     *      <tr><td>0x006 =&nbsp;</td><td>program AE</td></tr>
     *      <tr><td>0x100 =&nbsp;</td><td>aperture-priority AE</td></tr>
     *      <tr><td>0x200 =&nbsp;</td><td>shutter-priority AE</td></tr>
     *      <tr><td>0x300 =&nbsp;</td><td>manual</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_PICTURE_MODE       = 0x1031;

    /**
     * Quality.
     * <p>
     * Type: ASCII.
     */
    int FUJI_QUALITY            = 0x1000;

    /**
     * Saturation.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0x000 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>0x100 =&nbsp;</td><td>high</td></tr>
     *      <tr><td>0x200 =&nbsp;</td><td>low</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_SATURATION         = 0x1003;

    /**
     * Sharpness.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>soft</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>soft 2</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>hard</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>hard 2</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_SHARPNESS          = 0x1001;

    /**
     * Slow sync.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_SLOW_SYNC          = 0x1030;

    /**
     * Version.
     * <p>
     * Type: Undefined.
     */
    int FUJI_VERSION            = 0x0000;

    /**
     * White balance.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0x000 =&nbsp;</td><td>auto</td></tr>
     *      <tr><td>0x100 =&nbsp;</td><td>daylight</td></tr>
     *      <tr><td>0x200 =&nbsp;</td><td>cloudy</td></tr>
     *      <tr><td>0x300 =&nbsp;</td><td>daylight color, fluorescent</td></tr>
     *      <tr><td>0x301 =&nbsp;</td><td>daywhite color, fluorescent</td></tr>
     *      <tr><td>0x400 =&nbsp;</td><td>incandescent</td></tr>
     *      <tr><td>0xF00 =&nbsp;</td><td>custom</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int FUJI_WHITE_BALANCE      = 0x1002;

}
/* vim:set et sw=4 ts=4: */
