/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetaTags;

/**
 * A <code>MinoltaTags</code> defines the constants used for Minolta maker note
 * metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface MinoltaTags extends ImageMetaTags {

    /**
     * Camera settings.
     * This has sub-fields; see all the <code>MINOLTA_CS_</code> tags.
     */
    int MINOLTA_CAMERA_SETTINGS         = 0x0003;

    /**
     * Camera settings.
     * This has sub-fields; see all the <code>MINOLTA_CS_</code> tags.
     */
    int MINOLTA_CAMERA_SETTINGS_OLD     = 0x0001;

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
    int MINOLTA_COLOR_MODE              = 0x0101;

    /**
     * Compressed image size.
     * <p>
     * Type: Unsigned long.
     */
    int MINOLTA_COMPRESSED_IMAGE_SIZE   = 0x0040;

    /**
     * Image size, but not for A200.
     * <p>
     * Type: Unsigned long.
     */
    int MINOLTA_IMAGE_SIZE              = 0x0103;

    /**
     * Lens ID.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td align="right">1 =&nbsp;</td>
     *        <td>AF80-200mm F2.8G</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">2 =&nbsp;</td>
     *        <td>AF28-70mm F2.8G</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">6 =&nbsp;</td>
     *        <td>AF24-85mm F3.5-4.5</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">7 =&nbsp;</td>
     *        <td>AF100-400mm F4.5-6.7(D)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">11 =&nbsp;</td>
     *        <td>AF300mm F4G</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">12 =&nbsp;</td>
     *        <td>AF100mm F2.8 Soft</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">15 =&nbsp;</td>
     *        <td>AF400mm F4.5G</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">16 =&nbsp;</td>
     *        <td>AF17-35mm F3.5G</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">19 =&nbsp;</td>
     *        <td>AF35mm/1.4</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">20 =&nbsp;</td>
     *        <td>STF135mm F2.8[T4.5]</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">23 =&nbsp;</td>
     *        <td>AF200mm F4G Macro</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">24 =&nbsp;</td>
     *        <td>AF24-105mm F3.5-4.5(D) or SIGMA 18-50mm F2.8</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25 =&nbsp;</td>
     *        <td>AF100-300mm F4.5-5.6(D)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">27 =&nbsp;</td>
     *        <td>AF85mm F1.4G</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">28 =&nbsp;</td>
     *        <td>AF100mm F2.8 Macro(D)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">29 =&nbsp;</td>
     *        <td>AF75-300mm F4.5-5.6(D)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">30 =&nbsp;</td>
     *        <td>AF28-80mm F3.5-5.6(D)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">31 =&nbsp;</td>
     *        <td>AF50mm F2.8 Macro(D) or AF50mm F3.5 Macro</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">32 =&nbsp;</td>
     *        <td>AF100-400mm F4.5-6.7(D) x1.5</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">33 =&nbsp;</td>
     *        <td>AF70-200mm F2.8G SSM</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">35 =&nbsp;</td>
     *        <td>AF85mm F1.4G(D) Limited</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">38 =&nbsp;</td>
     *        <td>AF17-35mm F2.8-4(D)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">39 =&nbsp;</td>
     *        <td>AF28-75mm F2.8(D)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">40 =&nbsp;</td>
     *        <td>AFDT18-70mm F3.5-5.6(D)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">128 =&nbsp;</td>
     *        <td>TAMRON 18-200, 28-300 or 80-300mm F3.5-6.3</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25501 =&nbsp;</td>
     *        <td>AF50mm F1.7</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25521 =&nbsp;</td>
     *        <td>TOKINA 19-35mm F3.5-4.5 or TOKINA 28-70mm F2.8 AT-X</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25541 =&nbsp;</td>
     *        <td>AF35-105mm F3.5-4.5</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25551 =&nbsp;</td>
     *        <td>AF70-210mm F4 Macro or SIGMA 70-210mm F4-5.6 APO</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25581 =&nbsp;</td>
     *        <td>AF24-50mm F4</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25611 =&nbsp;</td>
     *        <td>SIGMA 70-300mm F4-5.6 or SIGMA 300mm F4 APO Macro</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25621 =&nbsp;</td>
     *        <td>AF50mm F1.4 NEW</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25631 =&nbsp;</td>
     *        <td>AF300mm F2.8G</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25641 =&nbsp;</td>
     *        <td>AF50mm F2.8 Macro</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25661 =&nbsp;</td>
     *        <td>AF24mm F2.8</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25721 =&nbsp;</td>
     *        <td>AF500mm F8 Reflex</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25781 =&nbsp;</td>
     *        <td>AF16mm F2.8 Fisheye or SIGMA 8mm F4 Fisheye</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25791 =&nbsp;</td>
     *        <td>AF20mm F2.8</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25811 =&nbsp;</td>
     *        <td>AF100mm F2.8 Macro(D), TAMRON 90mm F2.8 Macro,
     *            or SIGMA 180mm F5.6 Macro</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25858 =&nbsp;</td>
     *        <td>TAMRON 24-135mm F3.5-5.6</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25891 =&nbsp;</td>
     *        <td>TOKINA 80-200mm F2.8</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25921 =&nbsp;</td>
     *        <td>AF85mm F1.4G(D)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25931 =&nbsp;</td>
     *        <td>AF200mm F2.8G</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25961 =&nbsp;</td>
     *        <td>AF28mm F2</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">25981 =&nbsp;</td>
     *        <td>AF100mm F2</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">26061 =&nbsp;</td>
     *        <td>AF100-300mm F4.5-5.6(D)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">26081 =&nbsp;</td>
     *        <td>AF300mm F2.8G</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">26121 =&nbsp;</td>
     *        <td>AF200mm F2.8G(D)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">26131 =&nbsp;</td>
     *        <td>AF50mm F1.7</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">26241 =&nbsp;</td>
     *        <td>AF35-80mm F4-5.6</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">45741 =&nbsp;</td>
     *        <td>AF200mm F2.8G x2 or TOKINA 300mm F2.8 x2</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * <p>
     * Type: Unsigned long.
     */
    int MINOLTA_LENS_ID                 = 0x010C;

    /**
     * Maker notes version.
     */
    int MINOLTA_MAKER_NOTES_VERSION     = 0x0000;

    /**
     * Preview image length.
     * <p>
     * Type: Unsigned long.
     */
    int MINOLTA_PREVIEW_IMAGE_LENGTH    = 0x0089;

    /**
     * Preview image start.
     * <p>
     * Type: Unsigned long.
     */
    int MINOLTA_PREVIEW_IMAGE_START     = 0x0088;

    /**
     * Quality for DiMAGE A2/7Hi.
     * <p>
     * Type: Unsigned long.
     */
    int MINOLTA_QUALITY                 = 0x0102;

    /**
     * Quality for DiMAGE A2/7Hi.
     * <p>
     * Type: Unsigned long.
     */
    int MINOLTA_QUALITY_2               = 0x0103;

    /**
     * Camera setting: aperture.
     */
    int MINOLTA_CS_APERTURE             = MINOLTA_CAMERA_SETTINGS << 8 | 0x0A;

    /**
     * Camera setting: bracket step.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>1/3 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>2/3 EV</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>1 EV</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_BRACKET_STEP         = MINOLTA_CAMERA_SETTINGS << 8 | 0x0E;

    /**
     * Camera setting: brightness.
     */
    int MINOLTA_CS_BRIGHTNESS           = MINOLTA_CAMERA_SETTINGS << 8 | 0x32;

    /**
     * Camera setting: black &amp; white filter.
     */
    int MINOLTA_CS_BW_FILTER            = MINOLTA_CAMERA_SETTINGS << 8 | 0x30;

    /**
     * Camera setting: blue color balance.
     */
    int MINOLTA_CS_COLOR_BALANCE_BLUE   = MINOLTA_CAMERA_SETTINGS << 8 | 0x1E;

    /**
     * Camera setting: green color balance.
     */
    int MINOLTA_CS_COLOR_BALANCE_GREEN  = MINOLTA_CAMERA_SETTINGS << 8 | 0x1D;

    /**
     * Camera setting: red color balance.
     */
    int MINOLTA_CS_COLOR_BALANCE_RED    = MINOLTA_CAMERA_SETTINGS << 8 | 0x1C;

    /**
     * Camera setting: color filter for DiMAGE A2.
     */
    int MINOLTA_CS_COLOR_FILTER         = MINOLTA_CAMERA_SETTINGS << 8 | 0x29;

    /**
     * Camera setting: color mode.
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
     *        <td>4 =&nbsp;</td><td>Abobe RGB</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_COLOR_MODE           = MINOLTA_CAMERA_SETTINGS << 8 | 0x28;

    /**
     * Camera setting: color profile for DiMAGE 7Hi only.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>not embedded</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>embedded</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_COLOR_PROFILE        = MINOLTA_CAMERA_SETTINGS << 8 | 0x39;

    /**
     * Camera setting: contrast.
     */
    int MINOLTA_CS_CONTRAST             = MINOLTA_CAMERA_SETTINGS << 8 | 0x20;

    /**
     * Camera setting: date.
     */
    int MINOLTA_CS_DATE                 = MINOLTA_CAMERA_SETTINGS << 8 | 0x15;

    /**
     * Camera setting: DEC position.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>exposure</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>contrast</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>saturation</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>filter</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_DEC_POSITION         = MINOLTA_CAMERA_SETTINGS << 8 | 0x38;

    /**
     * Camera setting: digital zoom.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>off</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>electronic magnification</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>2x</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_DIGITAL_ZOOM         = MINOLTA_CAMERA_SETTINGS << 8 | 0x0C;

    /**
     * Camera setting: drive mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>single</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>continuous</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>self-timer</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>bracketing</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>interval</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>UHS continuous</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td><td>HS continuous</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_DRIVE_MODE           = MINOLTA_CAMERA_SETTINGS << 8 | 0x06;

    /**
     * Camera setting: exposure compensation.
     */
    int MINOLTA_CS_EXPOSURE_COMPENSATION= MINOLTA_CAMERA_SETTINGS << 8 | 0x0D;

    /**
     * Camera setting: exposure mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>program</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>aperture priority</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>shutter priority</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>manual</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_EXPOSURE_MODE        = MINOLTA_CAMERA_SETTINGS << 8 | 0x01;

    /**
     * Camera setting: number memory.
     */
    int MINOLTA_CS_FILE_NUMBER_MEMORY   = MINOLTA_CAMERA_SETTINGS << 8 | 0x1A;

    /**
     * Camera setting: flash exposure compensation.
     */
    int MINOLTA_CS_FLASH_EXPOSURE_COMP  = MINOLTA_CAMERA_SETTINGS << 8 | 0x23;

    /**
     * Camera setting: flash fired.
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
     */
    int MINOLTA_CS_FLASH_FIRED          = MINOLTA_CAMERA_SETTINGS << 8 | 0x14;

    /**
     * Camera setting: flash mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>fill flash</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>red-eye reduction</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>read flash sync</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>wireless</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_FLASH_MODE           = MINOLTA_CAMERA_SETTINGS << 8 | 0x02;

    /**
     * Camera setting: focal length.
     */
    int MINOLTA_CS_FOCAL_LENGTH         = MINOLTA_CAMERA_SETTINGS << 8 | 0x12;

    /**
     * Camera setting: focus area.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>wide focus (normal)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>spot focus</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_FOCUS_AREA           = MINOLTA_CAMERA_SETTINGS << 8 | 0x37;

    /**
     * Camera setting: focus distance.
     */
    int MINOLTA_CS_FOCUS_DISTANCE       = MINOLTA_CAMERA_SETTINGS << 8 | 0x13;

    /**
     * Camera setting: focus mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>auto focus</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>manual focus</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_FOCUS_MODE           = MINOLTA_CAMERA_SETTINGS << 8 | 0x36;

    /**
     * Camera setting: folder name.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>standard form</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>data form</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_FOLDER_NAME          = MINOLTA_CAMERA_SETTINGS << 8 | 0x27;

    /**
     * Camera setting: image size.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>1600x1200</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>1280x960</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>640x480</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>2560x1920</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>2272x1704</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td><td>2048x1536</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_IMAGE_SIZE           = MINOLTA_CAMERA_SETTINGS << 8 | 0x04;

    /**
     * Camera setting: internal flash.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>no</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>fired</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_INTERNAL_FLASH       = MINOLTA_CAMERA_SETTINGS << 8 | 0x31;

    /**
     * Camera setting: interval length.
     */
    int MINOLTA_CS_INTERVAL_LENGTH      = MINOLTA_CAMERA_SETTINGS << 8 | 0x10;

    /**
     * Camera setting: interval mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>still image</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>time-lapse movie</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_INTERVAL_MODE        = MINOLTA_CAMERA_SETTINGS << 8 | 0x26;

    /**
     * Camera setting: interval number.
     */
    int MINOLTA_CS_INTERVAL_NUMBER      = MINOLTA_CAMERA_SETTINGS << 8 | 0x11;

    /**
     * Camera setting: ISO.
     */
    int MINOLTA_CS_ISO                  = MINOLTA_CAMERA_SETTINGS << 8 | 0x08;

    /**
     * Camera setting: ISO.
     */
    int MINOLTA_CS_ISO_SETTING          = MINOLTA_CAMERA_SETTINGS << 8 | 0x24;

    /**
     * Camera setting: last file number.
     */
    int MINOLTA_CS_LAST_FILE_NUMBER     = MINOLTA_CAMERA_SETTINGS << 8 | 0x1B;

    /**
     * Camera setting: macro mode.
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
     */
    int MINOLTA_CS_MACRO_MODE           = MINOLTA_CAMERA_SETTINGS << 8 | 0x0B;

    /**
     * Camera setting: maximum aperture.
     */
    int MINOLTA_CS_MAX_APERTURE         = MINOLTA_CAMERA_SETTINGS << 8 | 0x17;

    /**
     * Camera setting: metering mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>multi-segment</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>center weighted</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>spot</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_METERING_MODE        = MINOLTA_CAMERA_SETTINGS << 8 | 0x07;

    /**
     * Camera setting: model.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>DiMAGE 7 or X31</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>DiMAGE 5</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>DiMAGE S304</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>DiMAGE S404</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>DiMAGE 7i</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>DiMAGE 7Hi</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td><td>DiMAGE A1</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td><td>DiMAGE A2 or S414</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_MODEL                = MINOLTA_CAMERA_SETTINGS << 8 | 0x25;

    /**
     * Camera setting: quality.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>RAW</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>super fine</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>fine</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>standard</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>economy</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>extra fine</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_QUALITY              = MINOLTA_CAMERA_SETTINGS << 8 | 0x05;

    /**
     * Camera setting: saturation.
     */
    int MINOLTA_CS_SATURATION           = MINOLTA_CAMERA_SETTINGS << 8 | 0x1F;

    /**
     * Camera setting: sharpness.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>hard</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>soft</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_SHARPNESS            = MINOLTA_CAMERA_SETTINGS << 8 | 0x21;

    /**
     * Camera setting: shutter speed.
     */
    int MINOLTA_CS_SHUTTER_SPEED        = MINOLTA_CAMERA_SETTINGS << 8 | 0x09;

    /**
     * Camera setting: spot focus X.
     */
    int MINOLTA_CS_SPOT_FOCUS_X         = MINOLTA_CAMERA_SETTINGS << 8 | 0x33;

    /**
     * Camera setting: spot focus Y.
     */
    int MINOLTA_CS_SPOT_FOCUS_Y         = MINOLTA_CAMERA_SETTINGS << 8 | 0x34;

    /**
     * Camera setting: subject program.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>none</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>portrait</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>text</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>night portrait</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>sunset</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td><td>sports action</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_SUBJECT_PROGRAM      = MINOLTA_CAMERA_SETTINGS << 8 | 0x22;

    /**
     * Camera setting: time.
     */
    int MINOLTA_CS_TIME                 = MINOLTA_CAMERA_SETTINGS << 8 | 0x16;

    /**
     * Camera setting: white balance.
     */
    int MINOLTA_CS_WHITE_BALANCE        = MINOLTA_CAMERA_SETTINGS << 8 | 0x03;

    /**
     * Camera setting: focus zone.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>none</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>center horizontal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>center vertical</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>left</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td><td>right</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int MINOLTA_CS_WIDE_FOCUS_ZONE      = MINOLTA_CAMERA_SETTINGS << 8 | 0x35;

    /**
     * Image stabilization.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int MINOLTA_IMAGE_STABILIZATION     = 0x0107;

    /**
     * Scene mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>standard</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>portrait</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>text</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>night</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>evening</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>sports</td></tr>
     *      <tr><td>6 =&nbsp;</td><td>landscape</td></tr>
     *      <tr><td>9 =&nbsp;</td><td>super macro</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int MINOLTA_SCENE_MODE              = 0x0100;

    /**
     * Zone matching.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>ISO setting used</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>high key</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>low key</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int MINOLTA_ZONE_MATCHING           = 0x010A;

}
/* vim:set et sw=4 ts=4: */
