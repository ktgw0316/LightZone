/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetaTags;

/**
 * A <code>OlympusTags</code> defines the constants used for Olympus maker note
 * metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface OlympusTags extends ImageMetaTags {

    /**
     * Aperture.
     * <p>
     * Type: Signed rational.
     */
    int OLYMPUS_APERTURE                    = 0x1002;

    /**
     * Black &amp; white mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_BLACK_AND_WHITE_MODE        = 0x0203;

    /**
     * Black level.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_BLACK_LEVEL                 = 0x0102;

    /**
     * Blue balance.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_BLUE_BALANCE                = 0x1018;

    /**
     * Brightness.
     * <p>
     * Type: Signed rational.
     */
    int OLYMPUS_BRIGHTNESS                  = 0x1003;

    /**
     * Camera ID.
     * <p>
     * Type: TODO
     */
    int OLYMPUS_CAMERA_ID                   = 0x0209;

    /**
     * Camera settings.
     * This has sub-fields; see all the <code>OLYMPUS_CS_</code> tags.
     */
    int OLYMPUS_CAMERA_SETTINGS             = 0x0003;

    /**
     * Camera settings.
     * This has sub-fields; see all the <code>OLYMPUS_CS_</code> tags.
     */
    int OLYMPUS_CAMERA_SETTINGS_OLD         = 0x0001;

    /**
     * Camera type.
     * <p>
     * Type: TODO
     */
    int OLYMPUS_CAMERA_TYPE                 = 0x0207;

    /**
     * CCD scan mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>interlaced</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>progressive</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CCD_SCAN_MODE               = 0x1039;

    /**
     * Color mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>natural color</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>black &amp; white</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>vivid color</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>solarization</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>Adobe RGB</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int OLYMPUS_COLOR_MODE                  = 0x0101;

    /**
     * Compressed image size.
     * <p>
     * Type: Unsigned long.
     */
    int OLYMPUS_COMPRESSED_IMAGE_SIZE       = 0x0040;

    /**
     * Compression ratio.
     * <p>
     * Type: Unsigned rational.
     */
    int OLYMPUS_COMPRESSION_RATIO           = 0x1034;

    /**
     * Contrast.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>high</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>low</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CONTRAST                    = 0x1029;

    /**
     * Coring filter.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_CORING_FILTER               = 0x102D;

    /**
     * Camera setting: auto-exposure lock.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_AE_LOCK              = OLYMPUS_CAMERA_SETTINGS << 12 | 0x201;

    /**
     * Camera setting: auto-focus areas.
     * <p>
     * Type: Unsigned long (count = 64).
     */
    int OLYMPUS_CS_AF_AREAS             = OLYMPUS_CAMERA_SETTINGS << 12 | 0x304;

    /**
     * Camera setting: auto-focus search.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>not ready</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>ready</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_AF_SEARCH            = OLYMPUS_CAMERA_SETTINGS << 12 | 0x303;

    /**
     * Camera setting: colorspace.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>sRGB</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>Adobe RGB</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>Pro photo RGB</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_COLORSPACE           = OLYMPUS_CAMERA_SETTINGS << 12 | 0x507;

    /**
     * Camera settings: compression factor.
     * <p>
     * Type: Unsigned rational.
     */
    int OLYMPUS_CS_COMPRESSION_FACTOR   = OLYMPUS_CAMERA_SETTINGS << 12 | 0x50D;

    /**
     * Camera settings: contrast setting.
     * <p>
     * Type: Signed short.
     */
    int OLYMPUS_CS_CONTRAST_SETTING     = OLYMPUS_CAMERA_SETTINGS << 12 | 0x505;

    /**
     * Camera settings: custom saturation.
     * <p>
     * Type: Signed short.
     */
    int OLYMPUS_CS_CUSTOM_SATURATION    = OLYMPUS_CAMERA_SETTINGS << 12 | 0x503;

    /**
     * Camera setting: distortion correction.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_DISTORTION_CORRECTION= OLYMPUS_CAMERA_SETTINGS << 12 | 0x50B;

    /**
     * Camera setting: exposure mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>manual</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>program</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>aperture priority AE</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>shutter priority AE</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>program shift</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_EXPOSURE_MODE        = OLYMPUS_CAMERA_SETTINGS << 12 | 0x200;

    /**
     * Camera settings: flash exposure compensation.
     * <p>
     * Type: Signed rational.
     */
    int OLYMPUS_CS_FLASH_EXPOSURE_COMPENSATION
                                        = OLYMPUS_CAMERA_SETTINGS << 12 | 0x401;

    /**
     * Camera settings: flash mode.
     * Bits:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>on</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>fill-in</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>red-eye</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>slow-sync</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>forced on</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>2nd curtain</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_FLASH_MODE           = OLYMPUS_CAMERA_SETTINGS << 12 | 0x400;

    /**
     * Camera settings: focus mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td align="right">0 =&nbsp;</td><td>single AF</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">1 =&nbsp;</td><td>sequential shooting AF</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">2 =&nbsp;</td><td>continuous AF</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">10 =&nbsp;</td><td>manual</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_FOCUS_MODE           = OLYMPUS_CAMERA_SETTINGS << 12 | 0x301;

    /**
     * Camera setting: focus process.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>AF not used</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>AF used</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_FOCUS_PROCESS        = OLYMPUS_CAMERA_SETTINGS << 12 | 0x302;

    /**
     * Camera setting: gradation.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">-1 =&nbsp;</td><td>low key</td></tr>
     *      <tr><td align="right">0 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td align="right">1 =&nbsp;</td><td>high key<td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_GRADATION            = OLYMPUS_CAMERA_SETTINGS << 12 | 0x50F;

    /**
     * Camera setting: image quality.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>SQ</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>HQ</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>SHQ</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>RAW</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_IMAGE_QUALITY_2      = OLYMPUS_CAMERA_SETTINGS << 12 | 0x603;

    /**
     * Camera setting: macro mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_MACRO_MODE           = OLYMPUS_CAMERA_SETTINGS << 12 | 0x300;

    /**
     * Camera settings: metering mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td align="right">2 =&nbsp;</td>
     *        <td>center weighted</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">3 =&nbsp;</td>
     *        <td>spot</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">5 =&nbsp;</td>
     *        <td>ESP</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">261 =&nbsp;</td>
     *        <td>pattern + AF</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">515 =&nbsp;</td>
     *        <td>spot + highlight control</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">1027 =&nbsp;</td>
     *        <td>spot + shadow control</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_METERING_MODE        = OLYMPUS_CAMERA_SETTINGS << 12 | 0x202;

    /**
     * Camera settings: modified saturation.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>off</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>CM1 (red enhance)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>CM2 (green enhance)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>CM3 (blue enhance)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>CM4 (skin tones)</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_MODIFIED_SATURATION  = OLYMPUS_CAMERA_SETTINGS << 12 | 0x504;

    /**
     * Camera settings: noide reduction.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>off</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>reduction</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>filter</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>reduction + filter</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>filter (ISO boost)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>reduction + filter (ISO boost)</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_NOISE_REDUCTION      = OLYMPUS_CAMERA_SETTINGS << 12 | 0x50A;

    /**
     * Camera setting: panorama mode.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_PANORAMA_MODE        = OLYMPUS_CAMERA_SETTINGS << 12 | 0x601;

    /**
     * Camera setting: picture mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">1 =&nbsp;</td><td>vivid</td></tr>
     *      <tr><td align="right">2 =&nbsp;</td><td>natural</td></tr>
     *      <tr><td align="right">3 =&nbsp;</td><td>muted</td></tr>
     *      <tr><td align="right">256 =&nbsp;</td><td>monotone</td></tr>
     *      <tr><td align="right">512 =&nbsp;</td><td>sepia</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_PICTURE_MODE         = OLYMPUS_CAMERA_SETTINGS << 12 | 0x520;

    /**
     * Camera setting: black &amp; white filter
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>n/a</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>neutral</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>yellow</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>orange</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>red</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>green</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Signed short.
     */
    int OLYMPUS_CS_PM_BW_FILTER         = OLYMPUS_CAMERA_SETTINGS << 12 | 0x525;

    /**
     * Camera setting: picture mode contrast.
     * <p>
     * Type: Signed short.
     */
    int OLYMPUS_CS_PM_CONTRAST          = OLYMPUS_CAMERA_SETTINGS << 12 | 0x523;

    /**
     * Camera setting: picture mode hue.
     * <p>
     * Type: Signed short.
     */
    int OLYMPUS_CS_PM_HUE               = OLYMPUS_CAMERA_SETTINGS << 12 | 0x522;

    /**
     * Camera setting: picture mode saturation.
     * <p>
     * Type: Signed short.
     */
    int OLYMPUS_CS_PM_SATURATION        = OLYMPUS_CAMERA_SETTINGS << 12 | 0x521;

    /**
     * Camera setting: picture mode sharpness.
     * <p>
     * Type: Signed short.
     */
    int OLYMPUS_CS_PM_SHARPNESS         = OLYMPUS_CAMERA_SETTINGS << 12 | 0x524;

    /**
     * Camera setting: picture mode tone.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>n/a</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>neutral</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>sepia</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>blue</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>purple</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>green</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Signed short.
     */
    int OLYMPUS_CS_PM_TONE              = OLYMPUS_CAMERA_SETTINGS << 12 | 0x526;

    /**
     * Camera settings: preview image length.
     * <p>
     * Type: Unsigned long.
     */
    int OLYMPUS_CS_PREVIEW_IMAGE_LENGTH = OLYMPUS_CAMERA_SETTINGS << 12 | 0x102;

    /**
     * Camera settings: preview image start.
     * <p>
     * Type: Unsigned long.
     */
    int OLYMPUS_CS_PREVIEW_IMAGE_START  = OLYMPUS_CAMERA_SETTINGS << 12 | 0x101;

    /**
     * Camera setting: preview image valid?
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>no</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>yes</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int OLYMPUS_CS_PREVIEW_IMAGE_VALID  = OLYMPUS_CAMERA_SETTINGS << 12 | 0x100;

    /**
     * Camera setting: scene mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>standard</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td><td>sport</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>8 =&nbsp;</td><td>portrait</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>9 =&nbsp;</td><td>landscape portrait</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>10 =&nbsp;</td><td>landscape</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>11 =&nbsp;</td><td>night scene</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>13 =&nbsp;</td><td>panorama</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>16 =&nbsp;</td><td>landscape portrait</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>17 =&nbsp;</td><td>night portrait</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>19 =&nbsp;</td><td>fireworks</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>20 =&nbsp;</td><td>sunset</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>22 =&nbsp;</td><td>macro</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>25 =&nbsp;</td><td>documents</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>26 =&nbsp;</td><td>museum</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>28 =&nbsp;</td><td>beach or snow</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>30 =&nbsp;</td><td>candle</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>35 =&nbsp;</td><td>underwater wide #1</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>36 =&nbsp;</td><td>underwater macro</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>39 =&nbsp;</td><td>high key</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>40 =&nbsp;</td><td>digital image stabilization</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>44 =&nbsp;</td><td>underwater wide #2</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>45 =&nbsp;</td><td>low key</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>46 =&nbsp;</td><td>children</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>48 =&nbsp;</td><td>nature macro</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_SCENE_MODE           = OLYMPUS_CAMERA_SETTINGS << 12 | 0x509;

    /**
     * Camera setting: sequence.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_SEQUENCE             = OLYMPUS_CAMERA_SETTINGS << 12 | 0x600;

    /**
     * Camera setting: shading compensation.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_SHADING_COMPENSATION = OLYMPUS_CAMERA_SETTINGS << 12 | 0x50C;

    /**
     * Camera settings: sharpness setting.
     * <p>
     * Type: Signed short.
     */
    int OLYMPUS_CS_SHARPNESS_SETTING    = OLYMPUS_CAMERA_SETTINGS << 12 | 0x506;

    /**
     * Camera setting: white balance.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>16 =&nbsp;</td><td>7500K (fine weather with shade)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>17 =&nbsp;</td><td>6000K (cloudy)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>18 =&nbsp;</td><td>5300K (fine weather)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>20 =&nbsp;</td><td>3000K (tungsten light)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>21 =&nbsp;</td><td>3600K (tungsten light-like)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>33 =&nbsp;</td><td>6600K (daylight fluorescent)</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_WHITE_BALANCE        = OLYMPUS_CAMERA_SETTINGS << 12 | 0x500;

    /**
     * Camera setting: white balance bracket.
     * <p>
     * Type: Signed short.
     */
    int OLYMPUS_CS_WHITE_BALANCE_BRACKET= OLYMPUS_CAMERA_SETTINGS << 12 | 0x502;

    /**
     * Camera setting: white balance temperature.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_CS_WHITE_BALANCE_TEMP   = OLYMPUS_CAMERA_SETTINGS << 12 | 0x501;

    /**
     * Camera setting: version.
     * <p>
     * Type: Undefined.
     */
    int OLYMPUS_CS_VERSION              = OLYMPUS_CAMERA_SETTINGS << 12 | 0x000;

    /**
     * Digital zoom.
     * <p>
     * Type: Unsigned rational.
     */
    int OLYMPUS_DIGITAL_ZOOM                = 0x0204;

    /**
     * Epson image height.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_EPSON_IMAGE_HEIGHT          = 0x020C;

    /**
     * Epson image width.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_EPSON_IMAGE_WIDTH           = 0x020B;

    /**
     * Epson software.
     * <p>
     * Type: String.
     */
    int OLYMPUS_EPSON_SOFTWARE              = 0x020D;

    /**
     * Exposure compensation.
     * <p>
     * Type: Signed rational.
     */
    int OLYMPUS_EXPOSURE_COMPENSATION       = 0x1006;

    /**
     * External flash bounce.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>no</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>yes</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_EXTERNAL_FLASH_BOUNCE       = 0x1026;

    /**
     * External flash mode.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_EXTERNAL_FLASH_MODE         = 0x1028;

    /**
     * External flash zoom.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_EXTERNAL_FLASH_ZOOM         = 0x1027;

    /**
     * Flash charge level.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_FLASH_CHARGE_LEVEL          = 0x1010;

    /**
     * Flash device.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>none</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>internal</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>external</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>internal + external</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_FLASH_DEVICE                = 0x1005;

    /**
     * Flash exposure compensation.
     * <p>
     * Type: Signed rational.
     */
    int OLYMPUS_FLASH_EXPOSURE_COMPENSATION = 0x1023;

    /**
     * Flash mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>2 =&nbsp;</td><td>on</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>off</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_FLASH_MODE                  = 0x1004;

    /**
     * Focal plane diagonal.
     * <p>
     * Type: Unsigned rational.
     */
    int OLYMPUS_FOCAL_PLANE_DIAGONAL        = 0x0205;

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
    int OLYMPUS_FOCUS_MODE                  = 0x100B;

    /**
     * Focus step count.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_FOCUS_STEP_COUNT            = 0x100E;

    /**
     * Image height.
     * <p>
     * Type: Unsigned long.
     */
    int OLYMPUS_IMAGE_HEIGHT                = 0x102F;

    /**
     * Image size.
     * <p>
     * Type: Unsigned long.
     */
    int OLYMPUS_IMAGE_SIZE                  = 0x0103;

    /**
     * Image width.
     * <p>
     * Type: Unsigned long.
     */
    int OLYMPUS_IMAGE_WIDTH                 = 0x102E;

    /**
     * Infinity lens step.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_INFINITY_LENS_STEP          = 0x103B;

    /**
     * ISO.  (This needs special conversion.)
     * <p>
     * Type: Signed rational.
     */
    int OLYMPUS_ISO                         = 0x1001;

    /**
     * Lens distortion parameters.
     * <p>
     * Type: Signed short.
     */
    int OLYMPUS_LENS_DISTORTION_PARAMS      = 0x0206;

    /**
     * Lens temperature.
     * <p>
     * Type: Signed short.
     */
    int OLYMPUS_LENS_TEMPERATURE            = 0x1008;

    /**
     * Macro mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>super</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_MACRO_MODE                  = 0x0202;

    /**
     * Maker notes version.
     * <p>
     * Type: Undefined.
     */
    int OLYMPUS_MAKER_NOTES_VERSION         = 0x0000;

    /**
     * Manual focus distance.
     * <p>
     * Type: Unsigned rational.
     */
    int OLYMPUS_MANUAL_FOCUS_DISTANCE       = 0x100C;

    /**
     * Near lens step.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_NEAR_LENS_STEP              = 0x103C;

    /**
     * Noise reduction.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_NOISE_REDUCTION             = 0x103A;

    /**
     * One touch white balance.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>on (preset)</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_ONE_TOUCH_WHITE_BALANCE     = 0x0302;

    /**
     * Preview image length.
     * <p>
     * Type: Unsigned long.
     */
    int OLYMPUS_PREVIEW_IMAGE_LENGTH        = 0x0089;

    /**
     * Preview image length.
     * <p>
     * Type: Unsigned long.
     */
    int OLYMPUS_PREVIEW_IMAGE_LENGTH_2      = 0x1037;

    /**
     * Preview image start.
     * <p>
     * Type: Unsigned long.
     */
    int OLYMPUS_PREVIEW_IMAGE_START         = 0x0088;

    /**
     * Preview image start.
     * <p>
     * Type: Unsigned long.
     */
    int OLYMPUS_PREVIEW_IMAGE_START_2       = 0x1036;

    /**
     * Preview image valid?
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>no</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>yes</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int OLYMPUS_PREVIEW_IMAGE_VALID         = 0x1035;

    /**
     * Quality.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>raw</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>superfine</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>fine</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>economy</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>extra fine</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_QUALITY                     = 0x0102;

    /**
     * Same as {@link #OLYMPUS_QUALITY}.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_QUALITY_2                   = 0x0103;

    /**
     * Red balance.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_RED_BALANCE                 = 0x1017;

    /**
     * Sensor temperature.
     * <p>
     * Type: Signed short.
     */
    int OLYMPUS_SENSOR_TEMPERATURE          = 0x1007;

    /**
     * Serial number.
     * <p>
     * Type: String.
     */
    int OLYMPUS_SERIAL_NUMBER               = 0x0404;

    /**
     * Serial number.
     * <p>
     * Type: String.
     */
    int OLYMPUS_SERIAL_NUMBER_2             = 0x101A;

    /**
     * Sharpness.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>hard</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>soft</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_SHARPNESS                   = 0x100F;

    /**
     * Sharpness factor.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_SHARPNESS_FACTOR            = 0x102A;

    /**
     * Shutter speed.
     * <p>
     * Type: Signed rational.
     */
    int OLYMPUS_SHUTTER_SPEED               = 0x1000;

    /**
     * Special mode.
     * TODO
     */
    int OLYMPUS_SPECIAL_MODE                = 0x0200;

    /**
     * Text info.
     * This has sub-fields; see all the <code>OLYMPUS_TI_</code> tags.
     */
    int OLYMPUS_TEXT_INFO                   = 0x0208;

    /**
     * White balance mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>hard</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>soft</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int OLYMPUS_WHITE_BALANCE_MODE          = 0x1015;

    /**
     * Zoom step count.
     * <p>
     * Type: Unsigned short.
     */
    int OLYMPUS_ZOOM_STEP_COUNT             = 0x100D;

}
/* vim:set et sw=4 ts=4: */
