/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.EXIFTags;
import com.lightcrafts.image.metadata.ImageMetaTags;
import com.lightcrafts.image.metadata.TIFFDirectory;
import com.lightcrafts.image.metadata.TIFFTags;

/**
 * A <code>CanonTags</code> defines the constants used for Canon maker note
 * metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface CanonTags extends ImageMetaTags {

    /**
     * Camera settings.
     * This has sub-fields; see all the <code>CANON_CS_</code> tags.
     */
    int CANON_CAMERA_SETTINGS               = 0x0001;

    /**
     * Preview image start for CR2 image files.
     * <p>
     * Note, however, that this is a weird special case for CR2 files.  This
     * tag value is actually stored in the {@link TIFFTags#TIFF_STRIP_OFFSETS}
     * metadata value in the {@link TIFFDirectory}.
     * <p>
     * Type: Unsigned long.
     */
    int CANON_CR2_PREVIEW_IMAGE_START       = TIFFTags.TIFF_STRIP_OFFSETS;

    /**
     * Preview image length for CR2 image files.
     * <p>
     * Similarly to {@link #CANON_CR2_PREVIEW_IMAGE_START}, this is also a
     * weird special case for CR2 files.  This tag value is actually stored in
     * the {@link TIFFTags#TIFF_STRIP_BYTE_COUNTS} metadata value in the
     * {@link TIFFDirectory}.
     * <p>
     * Type: Unsigned long.
     */
    int CANON_CR2_PREVIEW_IMAGE_LENGTH      = TIFFTags.TIFF_STRIP_BYTE_COUNTS;

    /**
     * Model ID.
     * <p>
     * Type: Unsigned long.
     */
    int CANON_MODEL_ID                      = 0x0010;

    /**
     * Color data.
     * This has sub-fields; see all the <code>CANON_CD_</code> tags.
     */
    int CANON_COLOR_DATA                    = 0x4001;

    /**
     * Color data: color data version
     * <p>
     * Type: Signed short.
     */
    int CANON_CD_VERSION                    = CANON_COLOR_DATA << 8;

    /**
     * Color information.
     * This has sub-fields; see all the <code>CANON_CI_</code> tags.
     */
    int CANON_COLOR_INFO                    = 0x4003;

    /**
     * Color information: color hue.
     * <p>
     * Type: Signed short.
     */
    int CANON_CI_COLOR_HUE                  = CANON_COLOR_INFO << 8 | 0x02;

    /**
     * Color information: color space.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>sRGB</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>Adobe RGB</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Signed short.
     */
    int CANON_CI_COLOR_SPACE                = CANON_COLOR_INFO << 8 | 0x03;

    /**
     * Color information for Canon D30 camera.
     * This has sub-fields; see all the <code>CANON_CI_D30_</code> tags.
     */
    int CANON_COLOR_INFO_D30                = 0x000A;

    /**
     * Color information for Canon D30 camera: color temperature.
     */
    int CANON_CI_D30_COLOR_TEMPERATURE      = CANON_COLOR_INFO_D30 << 8 | 0x09;

    /**
     * Color information for Canon D30 camera: color matrix.
     */
    int CANON_CI_D30_COLOR_MATRIX           = CANON_COLOR_INFO_D30 << 8 | 0x0A;

    /**
     * Color temperature in degrees Kelvin.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_COLOR_TEMPERATURE             = 0x00AE;

    /**
     * Camera setting: AF point selected.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0x2005 =&nbsp;</td><td>manual</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x3000 =&nbsp;</td><td>none</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x3001 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x3002 =&nbsp;</td><td>right</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x3003 =&nbsp;</td><td>center</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x3004 =&nbsp;</td><td>left</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x4001 =&nbsp;</td><td>auto</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_AF_POINT_SELECTED          = CANON_CAMERA_SETTINGS << 8 | 0x13;

    /**
     * Camera setting: continuous drive mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>single shot</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>continuous</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_CONTINUOUS_DRIVE_MODE      = CANON_CAMERA_SETTINGS << 8 | 0x05;

    /**
     * Camera setting: contrast.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td align="right">-1 =&nbsp;</td><td>low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">0 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">1 =&nbsp;</td><td>high</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_CONTRAST                   = CANON_CAMERA_SETTINGS << 8 | 0x0D;

    /**
     * Camera setting: digital zoom.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">-1 =&nbsp;</td><td>na (/)</td></tr>
     *      <tr><td align="right">0 =&nbsp;</td><td>none</td></tr>
     *      <tr><td align="right">1 =&nbsp;</td><td>2x</td></tr>
     *      <tr><td align="right">2 =&nbsp;</td><td>4x</td></tr>
     *      <tr><td align="right">3 =&nbsp;</td><td>other</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_DIGITAL_ZOOM               = CANON_CAMERA_SETTINGS << 8 | 0x0C;

    /**
     * Camera setting: easy-shooting mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>full auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>manual</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>landscape</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>fast shutter</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>slow shutter</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>night</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>black &amp; white</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td><td>sepia</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>8 =&nbsp;</td><td>portrait</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>9 =&nbsp;</td><td>sports</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>10 =&nbsp;</td><td>macro</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>11 =&nbsp;</td><td>pan focus</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_EASY_SHOOTING_MODE         = CANON_CAMERA_SETTINGS << 8 | 0x0B;

    /**
     * Camera setting: exposure mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>easy-shooting</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>program</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>Tv priority</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>Av priority</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>manual</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>A-DEP</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_EXPOSURE_MODE              = CANON_CAMERA_SETTINGS << 8 | 0x14;

    /**
     * Camera setting: flash mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>flash not fired</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>on</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>red-eye reduction</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>slow sync</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>auto + red-eye reduction</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>on + red-eye reduction</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>16 =&nbsp;</td><td>external flash</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_FLASH_MODE                 = CANON_CAMERA_SETTINGS << 8 | 0x04;

    /**
     * Camera setting: flash activity.
     * <p>
     * Type: Short.
     */
    int CANON_CS_FLASH_ACTIVITY             = CANON_CAMERA_SETTINGS << 8 | 0x1C;

    /**
     * Camera setting: flash details.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td align="right">bit 15 =&nbsp;</td><td>external E-TTL</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">bit 13 =&nbsp;</td><td>internal flash</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">bit 11 =&nbsp;</td><td>FP sync used</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">bit 4 =&nbsp;</td><td>FP sync enabled</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_FLASH_DETAILS              = CANON_CAMERA_SETTINGS << 8 | 0x1D;

    /**
     * Camera setting: focal units per mm.
     * <p>
     * Type: Short.
     */
    int CANON_CS_FOCAL_UNITS_PER_MM         = CANON_CAMERA_SETTINGS << 8 | 0x19;

    /**
     * Camera setting: focus mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>one-shot</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>AI servo</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>AI focus</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>MF</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>single</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>continuous</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>MF</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_FOCUS_MODE                 = CANON_CAMERA_SETTINGS << 8 | 0x07;

    /**
     * Camera setting: focus mode (for G1).
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td align="right">-1 =&nbsp;</td><td>n/a (?)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">0 =&nbsp;</td><td>single</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">1 =&nbsp;</td><td>continuous</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_FOCUS_MODE_G1              = CANON_CAMERA_SETTINGS << 8 | 0x20;

    /**
     * Camera setting: focus type.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>manual</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>auto (1)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>auto (2)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>macro mode</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td><td>infinity mode</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>8 =&nbsp;</td><td>pan mode</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_FOCUS_TYPE                 = CANON_CAMERA_SETTINGS << 8 | 0x12;

    /**
     * Camera setting: image size.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>large</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>medium</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>small</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_IMAGE_SIZE                 = CANON_CAMERA_SETTINGS << 8 | 0x0A;

    /**
     * Camera setting: ISO.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td align="right">0 =&nbsp;</td>
     *        <td>use {@link EXIFTags#EXIF_ISO_SPEED_RATINGS}</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">15 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">16 =&nbsp;</td><td>50</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">17 =&nbsp;</td><td>100</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">18 =&nbsp;</td><td>200</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">19 =&nbsp;</td><td>400</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">32767 =&nbsp;</td>
     *        <td>use {@link EXIFTags#EXIF_ISO_SPEED_RATINGS}</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_ISO                        = CANON_CAMERA_SETTINGS << 8 | 0x10;

    /**
     * Camera setting: lens type.
     * <p>
     * Type: Signed short.
     */
    int CANON_CS_LENS_TYPE                  = CANON_CAMERA_SETTINGS << 8 | 0x16;

    /**
     * Camera setting: long focal length in &quot;focal units.&quot;
     * <p>
     * Type: Short.
     */
    int CANON_CS_LONG_FOCAL_LENGTH          = CANON_CAMERA_SETTINGS << 8 | 0x17;

    /**
     * Camera setting: macro mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>macro</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>normal</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_MACRO_MODE                 = CANON_CAMERA_SETTINGS << 8 | 0x01;

    /**
     * Camera setting: metering mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>evaluative</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>partial</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>center-weighted</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_METERING_MODE              = CANON_CAMERA_SETTINGS << 8 | 0x11;

    /**
     * Camera setting: quality.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>fine</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>RAW</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>super-fine</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_QUALITY                    = CANON_CAMERA_SETTINGS << 8 | 0x03;

    /**
     * Camera setting: saturation.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td align="right">-1 =&nbsp;</td><td>low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">0 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">1 =&nbsp;</td><td>high</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_SATURATION                 = CANON_CAMERA_SETTINGS << 8 | 0x0E;

    /**
     * Camera setting: self-timer delay (in 10ths of a second).
     * <p>
     * Type: Short.
     */
    int CANON_CS_SELF_TIMER_DELAY           = CANON_CAMERA_SETTINGS << 8 | 0x02;

    /**
     * Camera setting: sharpness.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td align="right">-1 =&nbsp;</td><td>low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">0 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">1 =&nbsp;</td><td>high</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CS_SHARPNESS                  = CANON_CAMERA_SETTINGS << 8 | 0x0F;

    /**
     * Camera setting: short focal length in &quot;focal units.&quot;
     * <p>
     * Type: Short.
     */
    int CANON_CS_SHORT_FOCAL_LENGTH         = CANON_CAMERA_SETTINGS << 8 | 0x18;

    /**
     * Camera setting: zoomed resolution.
     * <p>
     * Type: Short.
     */
    int CANON_CS_ZOOMED_RESOLUTION          = CANON_CAMERA_SETTINGS << 8 | 0x24;

    /**
     * Camera setting: zoomed resolution.
     * <p>
     * Type: Short.
     */
    int CANON_CS_ZOOMED_RESOLUTION_BASE     = CANON_CAMERA_SETTINGS << 8 | 0x25;

    /**
     * Camera custom functions.
     * This has sub-fields; see all the <code>CANON_CF_</code> tags.
     * <p>
     * First short is the number of bytes in the tag.
     * For each remaining short: high 8 bits are the C.Fn number; low 8 bits
     * are the value.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_CUSTOM_FUNCTIONS              = 0x000F;

    /**
     * Custom functions: AF-assist light
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>on (auto)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>off</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CF_AF_ASSIST_LIGHT
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x05;

    /**
     * Custom functions: auto-exposure bracketing sequence / auto cancellation.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>0,-,+ / enabled</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>0,-,+ / disabled</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>-,0,+ / enabled</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>-,0,+ / disabled</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CF_AUTO_EXPOSURE_BRACKETING
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x07;

    /**
     * Custom functions: Auto exposure-lock buttons
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>AF/AE lock</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>AE lock/AF</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>AF/AF lock</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>AE+release/AE+AF</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CF_AUTO_EXPOSURE_LOCK_BUTTONS
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x02;

    /**
     * Custom functions: fill-flash auto-reduction.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>enabled</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>disabled</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CF_FILL_FLASH_AUTO_REDUCTION
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x0A;

    /**
     * Custom functions: lens auto-focus stop button function switch.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>AF stop</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>operate AF</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>lock AE, start timer</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CF_LENS_AUTO_FOCUS_STOP_BUTTON
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x09;

    /**
     * Custom functions: long exposure noice reduction.
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
     * Type: Short.
     */
    int CANON_CF_LONG_EXPOSURE_NOISE_REDUCTION
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x01;

    /**
     * Custom functions: menu button return position
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>top</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>previous (volatile)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>previous</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CF_MENU_BUTTON_RETURN_POSITION
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x0B;

    /**
     * Custom functions: mirror lock-up.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>disabled</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>enabled</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CF_MIRROR_LOCKUP
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x03;

    /**
     * Custom functions: sensor cleaning.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>disabled</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>enabled</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CF_SENSOR_CLEANING
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x0D;


    /**
     * Custom functions: SET button function when shooting
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>not assigned</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>change quality</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>change ISO</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>select parameters</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CF_SET_BUTTON_FUNCTION
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x0C;

    /**
     * Custom functions: shutter curtain sync.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>1st</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>2nd</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CF_SHUTTER_CURTAIN_SYNC
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x08;

    /**
     * Custom functions: shutter-speed in Av mode
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>1/200</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CF_SHUTTER_SPEED_IN_AV_MODE
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x06;

    /**
     * Custom functions: Tv/Av and exposure level.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>1/2 stop</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>1/3 stop</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_CF_TV_AV_EXPOSURE_LEVEL
        = CANON_CUSTOM_FUNCTIONS << 8 | 0x04;

    /**
     * Color space.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>sRGB</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>Adobe RGB</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int CANON_COLOR_SPACE                   = 0x00B4;

    /**
     * File information.
     * This has sub-fields; see all the <code>CANON_FI_</code> tags.
     */
    int CANON_FILE_INFO                     = 0x0093;

    /**
     * File information: file number.
     * <p>
     * Type: Unsigned long.
     */
    int CANON_FI_FILE_NUMBER                = CANON_FILE_INFO << 8 | 0x01;

    /**
     * File information: shutter count.
     * <p>
     * Type: Unsigned long.
     */
    int CANON_FI_SHUTTER_COUNT              = CANON_FILE_INFO << 8 | 0x02;

    /**
     * File length.
     * <p>
     * Type: Unsigned long.
     */
    int CANON_FILE_LENGTH                   = 0x000E;

    /**
     * Camera firmware version.
     * <p>
     * Type: ASCII.
     */
    int CANON_FIRMWARE_VERSION              = 0x0007;

    /**
     * Flash information.
     */
    int CANON_FLASH_INFO                    = 0x0003;

    /**
     * Focal length.
     * This has sub-fields; see all the <code>CANON_FL_</code> tags.
     */
    int CANON_FOCAL_LENGTH                  = 0x0002;

    /**
     * Focal length: focal length when the image was taken.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_FL_FOCAL_LENGTH               = CANON_FOCAL_LENGTH << 8 | 0x01;

    /**
     * Focal length: focal plane X size.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_FL_FOCAL_PLANE_X_SIZE         = CANON_FOCAL_LENGTH << 8 | 0x02;

    /**
     * Focal length: focal plane Y size.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_FL_FOCAL_PLANE_Y_SIZE         = CANON_FOCAL_LENGTH << 8 | 0x03;

    /**
     * ImageInfo number.
     * <p>
     * Type: Unsigned long.
     */
    int CANON_IMAGE_NUMBER                  = 0x0008;

    /**
     * ImageInfo type.
     * E.g., &quot;IMG:EOS D30 JPEG&quot;
     * <p>
     * Type: ASCII.
     */
    int CANON_IMAGE_TYPE                    = 0x0006;

    /**
     * Lens information for Canon EOS 1-D.
     * This has sub-fields; see all the <code>CANON_LI_</code> tags.
     */
    int CANON_LENS_INFO_1D                  = 0x000D;

    /**
     * Lens information for Canon EOS 1-D: focal length.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_LI_FOCAL_LENGTH               = CANON_LENS_INFO_1D << 8 | 0x0A;

    /**
     * Lens information for Canon EOS 1-D: focal type.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>fixed</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>zoom</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int CANON_LI_FOCAL_TYPE                 = CANON_LENS_INFO_1D << 8 | 0x2D;

    /**
     * Lens information for Canon EOS 1-D: lens type.
     * <p>
     * Type: Unsigned short.
     * @see #CANON_CS_LENS_TYPE
     */
    int CANON_LI_LENS_TYPE                  = CANON_LENS_INFO_1D << 8 | 0x0D;

    /**
     * Lens information for Canon EOS 1-D: long focal length.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_LI_LONG_FOCAL_LENGTH          = CANON_LENS_INFO_1D << 8 | 0x14;

    /**
     * Lens information for Canon EOS 1-D: short focal length.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_LI_SHORT_FOCAL_LENGTH         = CANON_LENS_INFO_1D << 8 | 0x12;

    /**
     * Camera owner's name.
     * <p>
     * Type: ASCII.
     */
    int CANON_OWNER_NAME                    = 0x0009;

    /**
     * Picture information.
     * This has sub-fields; see all the <code>CANON_PI_</code> tags.
     */
    int CANON_PICTURE_INFO                  = 0x0012;

    /**
     * Picture information: AF points used.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_PI_AF_POINTS_USED             = CANON_PICTURE_INFO << 8 | 0x16;

    /**
     * Picture information: image height.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_PI_IMAGE_HEIGHT               = CANON_PICTURE_INFO << 8 | 0x03;

    /**
     * Picture information: image height as shot.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_PI_IMAGE_HEIGHT_AS_SHOT       = CANON_PICTURE_INFO << 8 | 0x05;

    /**
     * Picture information: image width.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_PI_IMAGE_WIDTH                = CANON_PICTURE_INFO << 8 | 0x02;

    /**
     * Picture information: image width as shot.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_PI_IMAGE_WIDTH_AS_SHOT        = CANON_PICTURE_INFO << 8 | 0x04;

    /**
     * Picture information 2.
     * This has sub-fields; see all the <code>CANON_PI2_</code> tags.
     */
    int CANON_PICTURE_INFO2                 = 0x0026;

    /**
     * Picture information 2: AF area mode.
     * <p>
     * Type: Unsigned short.
     */
    int CANON_PI2_AF_AREA_MODE              = CANON_PICTURE_INFO2 << 8 | 0x01;

    /**
     * Preview image info.
     * This has sub-fields; see all the <code>CANON_PII_</code> tags.
     */
    int CANON_PREVIEW_IMAGE_INFO            = 0x00B6;

    /**
     * Preview image info: image height.
     * <p>
     * Type: Unsigned long.
     */
    int CANON_PII_IMAGE_HEIGHT  = CANON_PREVIEW_IMAGE_INFO << 8 | 0x04;

    /**
     * Preview image info: image length.
     * <p>
     * Type: Unsigned long.
     */
    int CANON_PII_IMAGE_LENGTH  = CANON_PREVIEW_IMAGE_INFO << 8 | 0x02;

    /**
     * Preview image info: image start.
     * <p>
     * Type: Unsigned long.
     */
    int CANON_PII_IMAGE_START   = CANON_PREVIEW_IMAGE_INFO << 8 | 0x05;

    /**
     * Preview image info: image width.
     * <p>
     * Type: Unsigned long.
     */
    int CANON_PII_IMAGE_WIDTH   = CANON_PREVIEW_IMAGE_INFO << 8 | 0x03;

    /**
     * Preview image info: focal plane X resolution.
     * <p>
     * Type: Rational.
     */
    int CANON_PII_FOCAL_PLANE_X_RESOLUTION
        = CANON_PREVIEW_IMAGE_INFO << 8 | 0x06;

    /**
     * Preview image info: focal plane Y resolution.
     * <p>
     * Type: Rational.
     */
    int CANON_PII_FOCAL_PLANE_Y_RESOLUTION
        = CANON_PREVIEW_IMAGE_INFO << 8 | 0x08;

    /**
     * Color processing information.
     * This has sub-fields; see all the <code>CANON_PI_</code> tags.
     */
    int CANON_PROCESSING_INFO               = 0x00A0;

    /**
     * Color processing information: color temperature.
     * <p>
     * Type: Signed short.
     */
    int CANON_PI_COLOR_TEMPERATURE          = CANON_PROCESSING_INFO << 8 | 0x09;

    /**
     * Color processing information: digital gain.
     * <p>
     * Type: Signed short.
     */
    int CANON_PI_DIGITAL_GAIN               = CANON_PROCESSING_INFO << 8 | 0x0B;

    /**
     * Color processing information: picture style.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>33 =&nbsp;</td><td>user defined 1</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>34 =&nbsp;</td><td>user defined 2</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>35 =&nbsp;</td><td>user defined 3</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>65 =&nbsp;</td><td>external 1</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>66 =&nbsp;</td><td>external 2</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>67 =&nbsp;</td><td>external 3</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>129 =&nbsp;</td><td>standard</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>130 =&nbsp;</td><td>portrait</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>131 =&nbsp;</td><td>landscape</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>132 =&nbsp;</td><td>neutral</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>133 =&nbsp;</td><td>faithful</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>134 =&nbsp;</td><td>monochrome</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Signed short.
     */
    int CANON_PI_PICTURE_STYLE              = CANON_PROCESSING_INFO << 8 | 0x0A;

    /**
     * Color processing information: sensor red level.
     * <p>
     * Type: Signed short.
     */
    int CANON_PI_SENSOR_RED_LEVEL           = CANON_PROCESSING_INFO << 8 | 0x4;

    /**
     * Color processing information: sensor blue level.
     * <p>
     * Type: Signed short.
     */
    int CANON_PI_SENSOR_BLUE_LEVEL          = CANON_PROCESSING_INFO << 8 | 0x4;

    /**
     * Color processing information: sharpness frequency.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>n/a</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>lowest</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>standard</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>high</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td><td>highest</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Signed short.
     */
    int CANON_PI_SHARPNESS_FREQUENCY        = CANON_PROCESSING_INFO << 8 | 0x03;

    /**
     * Color processing information: tone curve.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>standard</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>manual</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>custom</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Signed short.
     */
    int CANON_PI_TONE_CURVE                 = CANON_PROCESSING_INFO << 8 | 0x01;

    /**
     * Color processing information: white balance shift AB.
     * <p>
     * Type: Signed short.
     */
    int CANON_PI_WB_SHIFT_AB                = CANON_PROCESSING_INFO << 8 | 0x0C;

    /**
     * Color processing information: white balance shift GM.
     * <p>
     * Type: Signed short.
     */
    int CANON_PI_WB_SHIFT_GM                = CANON_PROCESSING_INFO << 8 | 0x0D;

    /**
     * Color processing information: white balance red.
     * <p>
     * Type: Signed short.
     */
    int CANON_PI_WHITE_BALANCE_RED          = CANON_PROCESSING_INFO << 8 | 0x4;

    /**
     * Color processing information: white balance red.
     * <p>
     * Type: Signed short.
     */
    int CANON_PI_WHITE_BALANCE_BLUE         = CANON_PROCESSING_INFO << 8 | 0x4;

    /**
     * Sensor information.
     * This has sub-fields; see all the <code>CANON_SI_SENSOR_</code> tags.
     */
    int CANON_SENSOR_INFO                   = 0x00E0;

    /**
     * Camera's serial number.
     * High 16 bits are printed as a 4-digit hex number;
     * low 16 bits are printed as a 5-digit decimal number.
     * The <code>printf()</code> format string would be &quot;%04X%05d&quot;.
     * <p>
     * Type: Unsigned long.
     */
    int CANON_SERIAL_NUMBER                 = 0x000C;

    /**
     * Shot information.
     * This has sub-fields; see all the <code>CANON_SI_</code> tags.
     */
    int CANON_SHOT_INFO                     = 0x0004;

    /**
     * Shot information: AEB bracket value.
     * <p>
     * Type: Short.
     */
    int CANON_SI_AEB_BRACKET_VALUE          = CANON_SHOT_INFO << 8 | 0x11;

    /**
     * Shot information: AF point used.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>bits 15-12 =&nbsp;</td>
     *        <td>number of available focus points</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>bit 2 =&nbsp;</td><td>left</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>bit 1 =&nbsp;</td><td>center</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>bit 0 =&nbsp;</td><td>right</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_SI_AF_POINT_USED              = CANON_SHOT_INFO << 8 | 0x0E;

    /**
     * Shot information: auto-exposure bracketing.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>-1 =&nbsp;</td><td>on</td>
     *        <td>0 =&nbsp;</td><td>off</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_SI_AUTO_EXPOSURE_BRACKETING   = CANON_SHOT_INFO << 8 | 0x10;

    /**
     * Shot information: auto-rotate.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td align="right">-1 =&nbsp;</td><td>software rotate</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">0 =&nbsp;</td><td>none</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">1 =&nbsp;</td><td>90 CW</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">2 =&nbsp;</td><td>180</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">3 =&nbsp;</td><td>270 CW</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_SI_AUTO_ROTATE                = CANON_SHOT_INFO << 8 | 0x1B;

    /**
     * Shot information: bulb duration.
     * <p>
     * Type: Long.
     */
    int CANON_SI_BULB_DURATION              = CANON_SHOT_INFO << 8 | 0x18;

    /**
     * Shot information: ISO.
     * <p>
     * Type: Short.
     */
    int CANON_SI_ISO                        = CANON_SHOT_INFO << 8 | 0x02;

    /**
     * Shot information: exposure compensation.
     * <p>
     * Type: Short.
     */
    int CANON_SI_EXPOSURE_COMPENSATION      = CANON_SHOT_INFO << 8 | 0x06;

    /**
     * Shot information: exposure time.
     * <p>
     * Type: Short.
     */
    int CANON_SI_EXPOSURE_TIME              = CANON_SHOT_INFO << 8 | 0x16;

    /**
     * Shot information: flash bias.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>-64 =&nbsp;</td><td>-2 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>-52 =&nbsp;</td><td>-1.67 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>-48 =&nbsp;</td><td>-1.50 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>-44 =&nbsp;</td><td>-1.33 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>-32 =&nbsp;</td><td>-1 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>-20 =&nbsp;</td><td>-0.67 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>-16 =&nbsp;</td><td>-0.50 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>-12 =&nbsp;</td><td>-0.33 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>0 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>12 =&nbsp;</td><td>0.33 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>16 =&nbsp;</td><td>0.50 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>20 =&nbsp;</td><td>0.67 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>32 =&nbsp;</td><td>1 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>44 =&nbsp;</td><td>1.33 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>48 =&nbsp;</td><td>1.50 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>52 =&nbsp;</td><td>1.67 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>64 =&nbsp;</td><td>2 EV</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_SI_FLASH_BIAS                 = CANON_SHOT_INFO << 8 | 0x0F;

    /**
     * Shot information: F-stop.
     * <p>
     * Type: Short.
     */
    int CANON_SI_FNUMBER                    = CANON_SHOT_INFO << 8 | 0x15;

    /**
     * Shot information: focus distance (lower).
     * <p>
     * Type: Short.
     */
    int CANON_SI_FOCUS_DISTANCE_LOWER       = CANON_SHOT_INFO << 8 | 0x14;

    /**
     * Shot information: focus distance (upper).
     * <p>
     * Type: Short.
     */
    int CANON_SI_FOCUS_DISTANCE_UPPER       = CANON_SHOT_INFO << 8 | 0x13;

    /**
     * Sensor information: sensor bottom border.
     * <p>
     * Type: Short.
     */
    int CANON_SI_SENSOR_BOTTOM_BORDER       = CANON_SENSOR_INFO << 8 | 0x08;

    /**
     * Sensor information: sensor height.
     * <p>
     * Type: Short.
     */
    int CANON_SI_SENSOR_HEIGHT              = CANON_SENSOR_INFO << 8 | 0x02;

    /**
     * Sensor information: sensor left border.
     * <p>
     * Type: Short.
     */
    int CANON_SI_SENSOR_LEFT_BORDER         = CANON_SENSOR_INFO << 8 | 0x05;

    /**
     * Sensor information: sensor right border.
     * <p>
     * Type: Short.
     */
    int CANON_SI_SENSOR_RIGHT_BORDER        = CANON_SENSOR_INFO << 8 | 0x07;

    /**
     * Sensor information: sensor top border.
     * <p>
     * Type: Short.
     */
    int CANON_SI_SENSOR_TOP_BORDER          = CANON_SENSOR_INFO << 8 | 0x06;

    /**
     * Sensor information: sensor width.
     * <p>
     * Type: Short.
     */
    int CANON_SI_SENSOR_WIDTH               = CANON_SENSOR_INFO << 8 | 0x01;

    /**
     * Shot information: sequence number (if in a continuous burst).
     * <p>
     * Type: Short.
     */
    int CANON_SI_SEQUENCE_NUMBER            = CANON_SHOT_INFO << 8 | 0x09;

    /**
     * Shot information: target exposure.
     * <p>
     * Type: Short.
     */
    int CANON_SI_TARGET_APERTURE            = CANON_SHOT_INFO << 8 | 0x04;

    /**
     * Shot information: exposure time.
     * <p>
     * Type: Short.
     */
    int CANON_SI_TARGET_EXPOSURE_TIME       = CANON_SHOT_INFO << 8 | 0x05;

    /**
     * Shot information: white balance.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>sunny</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>cloudy</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>tungsten</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>flourescent</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>flash</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>custom</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CANON_SI_WHITE_BALANCE              = CANON_SHOT_INFO << 8 | 0x07;

    /**
     * White-balance table.
     * The zeroth number is the number of bytes in the value.  This is followed
     * by a set of quads: red, green1, green2, and blue values for each value
     * in {@link CANON_SI_WHITE_BALANCE}.  The last quad is apparently a
     * baseline quad.
     * <p>
     * Type: Short.
     */
    int CANON_WHITE_BALANCE_TABLE           = 0x00A9;

}
/* vim:set et sw=4 ts=4: */
