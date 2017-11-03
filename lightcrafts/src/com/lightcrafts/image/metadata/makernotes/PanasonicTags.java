/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetaTags;

/**
 * A {@code PanasonicTags} defines the constants used for Panasonic maker note
 * metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface PanasonicTags extends ImageMetaTags {

    /**
     * Accessory type.
     * <p>
     * Type: ASCII.
     */
    int PANASONIC_ACCESSORY_TYPE            = 0x0053;

    /**
     * Audio.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>yes</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>no</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_AUDIO                     = 0x0020;

    /**
     * Auto-focus assist lamp.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>fired</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>enabled but not used</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>disabled but required</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>disabled but not required</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_AUTO_FOCUS_ASSIST_LAMP    = 0x0031;

    /**
     * Baby (or pet) age.
     * <p>
     * Type: Date.
     */
    int PANASONIC_BABY_AGE                  = 0x0033;

    /**
     * @see #PANASONIC_BABY_AGE
     */
    int PANASONIC_BABY_AGE_2                = 0x8010;

    /**
     * Burst mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>infinite</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>unlimited</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_BURST_MODE                = 0x002A;

    /**
     * City.
     * <p>
     * Type: ASCII.
     */
    int PANASONIC_CITY                      = 0x006D;

    /**
     * Color effect.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>warm</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>cool</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>black &amp; white</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>sepia</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_COLOR_EFFECT              = 0x0028;

    /**
     * Color mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>natural</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>vivid</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_COLOR_MODE                = 0x0032;

    /**
     * Contrast.
     * <p>
     * Type: Signed short.
     */
    int PANASONIC_CONTRAST                  = 0x0039;

    /**
     * Contrast mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">0 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td align="right">1 =&nbsp;</td><td>low</td></tr>
     *      <tr><td align="right">2 =&nbsp;</td><td>high</td></tr>
     *      <tr><td align="right">6 =&nbsp;</td><td>medium low</td></tr>
     *      <tr><td align="right">7 =&nbsp;</td><td>medium high</td></tr>
     *      <tr><td align="right">400 =&nbsp;</td><td>low</td></tr>
     *      <tr><td align="right">420 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td align="right">440 =&nbsp;</td><td>high</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_CONTRAST_MODE             = 0x002C;

    /**
     * Conversion lens.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>wide</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>telephoto</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>macro</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_CONVERSION_LENS           = 0x0035;

    /**
     * Country.
     * <p>
     * Type: ASCII.
     */
    int PANASONIC_COUNTRY                   = 0x0069;

    /**
     * EXIF version.
     * <p>
     * Type: Undefined.
     */
    int PANASONIC_EXIF_VERSION              = 0x0026;

    /**
     * Faces detected.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_FACES_DETECTED            = 0x003F;

    /**
     * Film mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">1 =&nbsp;</td><td>standard (color)</td></tr>
     *      <tr><td align="right">2 =&nbsp;</td><td>dynamic (color)</td></tr>
     *      <tr><td align="right">3 =&nbsp;</td><td>nature (color)</td></tr>
     *      <tr><td align="right">4 =&nbsp;</td><td>smooth (color)</td></tr>
     *      <tr><td align="right">5 =&nbsp;</td><td>standard (B&amp;W)</td></tr>
     *      <tr><td align="right">6 =&nbsp;</td><td>dynamic (B&amp;W)</td></tr>
     *      <tr><td align="right">7 =&nbsp;</td><td>smooth (B&amp;W)</td></tr>
     *      <tr><td align="right">10 =&nbsp;</td><td>nostalgic</td></tr>
     *      <tr><td align="right">11 =&nbsp;</td><td>vibrant</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_FILM_MODE                 = 0x0042;

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
     * Flash fired.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>no</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>yes</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_FLASH_FIRED               = 0x8007;

    /**
     * Flash warning.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>no</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>yes</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_FLASH_WARNING             = 0x0062;

    /**
     * Focus mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>auto</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>manual</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>auto, focus button</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>auto, continuous</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_FOCUS_MODE                = 0x0007;

    /**
     * Image quality.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>2 =&nbsp;</td><td>high</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>6 =&nbsp;</td><td>very high</td></tr>
     *      <tr><td>7 =&nbsp;</td><td>raw</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_IMAGE_QUALITY             = 0x0001;

    /**
     * Image stabilizer.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>2 =&nbsp;</td><td>on, mode 1</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>on, mode 2</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_IMAGE_STABILIZER          = 0x001A;

    /**
     * Intelligent resolution.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_INTELLIGENT_RESOLUTION    = 0x0070;

    /**
     * Internal serial number.
     * <p>
     * Type: TODO
     */
    int PANASONIC_INTERNAL_SERIAL_NUMBER    = 0x0025;

    /**
     * ISO.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">65534 =&nbsp;</td><td>intelligent ISO</td></tr>
     *      <tr><td align="right">65535 =&nbsp;</td><td>n/a</td></tr>
     *      <tr><td align="right">(else) =&nbsp;</td><td>actual ISO value</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_ISO                       = 0x003C;

    /**
     * Landmark.
     * <p>
     * Type: ASCII.
     */
    int PANASONIC_LANDMARK                  = 0x006F;

    /**
     * Lens serial number.
     * <p>
     * Type: ASCII.
     */
    int PANASONIC_LENS_SERIAL_NUMBER        = 0x0052;

    /**
     * Lens type.
     * <p>
     * Type: ASCII.
     */
    int PANASONIC_LENS_TYPE                 = 0x0051;

    /**
     * Macro mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">1 =&nbsp;</td><td>on</td></tr>
     *      <tr><td align="right">2 =&nbsp;</td><td>off</td></tr>
     *      <tr><td align="right">257 =&nbsp;</td><td>tele-macro</td></tr>
     *      <tr><td align="right">1001 =&nbsp;</td><td>macro zoom</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_MACRO_MODE                = 0x001C;

    /**
     * Noise reduction.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>standard</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>low</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>high</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>lowest</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>highest</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_NOISE_REDUCTION           = 0x002D;

    /**
     * Optical zoom mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>standard</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>extended</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_OPTICAL_ZOOM_MODE         = 0x0034;

    /**
     * Rotation.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>landscape</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>180</td></tr>
     *      <tr><td>6 =&nbsp;</td><td>90 CW</td></tr>
     *      <tr><td>8 =&nbsp;</td><td>90 CCW</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_ROTATION                  = 0x0030;

    /**
     * Saturation.
     * <p>
     * Type: Unsigned long.
     */
    int PANASONIC_SATURATION                = 0x0040;

    /**
     * @see #PANASONIC_SHOOTING_MODE
     */
    int PANASONIC_SCENE_MODE                = 0x8001;

    /**
     * Self timer.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>10s</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>2s</td></tr>
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
     * Sharpness.
     * <p>
     * Type: Unsigned long.
     */
    int PANASONIC_SHARPNESS                 = 0x0041;

    /**
     * Shooting mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">1 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td align="right">2 =&nbsp;</td><td>portrait</td></tr>
     *      <tr><td align="right">3 =&nbsp;</td><td>scenery</td></tr>
     *      <tr><td align="right">4 =&nbsp;</td><td>sports</td></tr>
     *      <tr><td align="right">5 =&nbsp;</td><td>night portrait</td></tr>
     *      <tr><td align="right">6 =&nbsp;</td><td>program</td></tr>
     *      <tr><td align="right">7 =&nbsp;</td><td>aperture priority</td></tr>
     *      <tr><td align="right">8 =&nbsp;</td><td>shutter program</td></tr>
     *      <tr><td align="right">9 =&nbsp;</td><td>macro</td></tr>
     *      <tr><td align="right">10 =&nbsp;</td><td>spot</td></tr>
     *      <tr><td align="right">11 =&nbsp;</td><td>manual</td></tr>
     *      <tr><td align="right">12 =&nbsp;</td><td>movie preview</td></tr>
     *      <tr><td align="right">13 =&nbsp;</td><td>panning</td></tr>
     *      <tr><td align="right">14 =&nbsp;</td><td>simple</td></tr>
     *      <tr><td align="right">15 =&nbsp;</td><td>color effects</td></tr>
     *      <tr><td align="right">16 =&nbsp;</td><td>self portrait</td></tr>
     *      <tr><td align="right">17 =&nbsp;</td><td>economy</td></tr>
     *      <tr><td align="right">18 =&nbsp;</td><td>fireworks</td></tr>
     *      <tr><td align="right">19 =&nbsp;</td><td>party</td></tr>
     *      <tr><td align="right">20 =&nbsp;</td><td>snow</td></tr>
     *      <tr><td align="right">21 =&nbsp;</td><td>night scenery</td></tr>
     *      <tr><td align="right">22 =&nbsp;</td><td>food</td></tr>
     *      <tr><td align="right">23 =&nbsp;</td><td>baby</td></tr>
     *      <tr><td align="right">24 =&nbsp;</td><td>soft skin</td></tr>
     *      <tr><td align="right">25 =&nbsp;</td><td>candlelight</td></tr>
     *      <tr><td align="right">26 =&nbsp;</td><td>starry night</td></tr>
     *      <tr><td align="right">27 =&nbsp;</td><td>high sensitivity</td></tr>
     *      <tr><td align="right">28 =&nbsp;</td><td>parorama assist</td></tr>
     *      <tr><td align="right">29 =&nbsp;</td><td>underwater</td></tr>
     *      <tr><td align="right">30 =&nbsp;</td><td>beach</td></tr>
     *      <tr><td align="right">31 =&nbsp;</td><td>aerial</td></tr>
     *      <tr><td align="right">32 =&nbsp;</td><td>sunset</td></tr>
     *      <tr><td align="right">33 =&nbsp;</td><td>pet</td></tr>
     *      <tr><td align="right">34 =&nbsp;</td><td>intelligent ISO</td></tr>
     *      <tr><td align="right">35 =&nbsp;</td><td>clipboard</td></tr>
     *      <tr><td align="right">36 =&nbsp;</td><td>high-speed continuous shooting</td></tr>
     *      <tr><td align="right">37 =&nbsp;</td><td>intelligent auto</td></tr>
     *      <tr><td align="right">39 =&nbsp;</td><td>muti-aspect</td></tr>
     *      <tr><td align="right">41 =&nbsp;</td><td>transform</td></tr>
     *      <tr><td align="right">42 =&nbsp;</td><td>flash burst</td></tr>
     *      <tr><td align="right">43 =&nbsp;</td><td>pin hole</td></tr>
     *      <tr><td align="right">44 =&nbsp;</td><td>film grain</td></tr>
     *      <tr><td align="right">45 =&nbsp;</td><td>my color</td></tr>
     *      <tr><td align="right">46 =&nbsp;</td><td>photo frame</td></tr>
     *      <tr><td align="right">51 =&nbsp;</td><td>HDR</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_SHOOTING_MODE             = 0x001F;

    /**
     * Spot mode.  Note that the value of interest is the second number only.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">1 =&nbsp;</td><td>on</td></tr>
     *      <tr><td align="right">16 =&nbsp;</td><td>off</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short[2].
     */
    int PANASONIC_SPOT_MODE                 = 0x000F;

    /**
     * State.
     * <p>
     * Type: ASCII.
     */
    int PANASONIC_STATE                     = 0x006B;

    /**
     * Text stamp.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_TEXT_STAMP                = 0x003B;

    /**
     * @see #PANASONIC_TEXT_STAMP
     */
    int PANASONIC_TEXT_STAMP_2              = 0x003E;

    /**
     * @see #PANASONIC_TEXT_STAMP
     */
    int PANASONIC_TEXT_STAMP_3              = 0x8008;

    /**
     * @see #PANASONIC_TEXT_STAMP
     */
    int PANASONIC_TEXT_STAMP_4              = 0x8009;

    /**
     * Time since power on.
     * <p>
     * Type: Unsigned long.
     */
    int PANASONIC_TIME_SINCE_POWER_ON       = 0x0029;

    /**
     * Travel day:
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_TRAVEL_DAY                = 0x0036;

    /**
     * White balance.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">1 =&nbsp;</td><td>auto</td></tr>
     *      <tr><td align="right">2 =&nbsp;</td><td>daylight</td></tr>
     *      <tr><td align="right">3 =&nbsp;</td><td>cloudy</td></tr>
     *      <tr><td align="right">4 =&nbsp;</td><td>halogen</td></tr>
     *      <tr><td align="right">5 =&nbsp;</td><td>manual</td></tr>
     *      <tr><td align="right">8 =&nbsp;</td><td>flash</td></tr>
     *      <tr><td align="right">10 =&nbsp;</td><td>black &amp; white</td></tr>
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

    /**
     * World time location.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>home</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>destination</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int PANASONIC_WORLD_TIME_LOCATION       = 0x003A;

}
/* vim:set et sw=4 ts=4: */
