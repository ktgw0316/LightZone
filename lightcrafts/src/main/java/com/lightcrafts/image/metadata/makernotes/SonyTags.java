/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.ImageMetaTags;

/**
 * A {@code SonyTags} defines the constants used for Sony maker note metadata
 * tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface SonyTags extends ImageMetaTags {

    /**
     * Anti-blur.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on (continuous)</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int SONY_ANTI_BLUR = 0xB04B;

    /**
     * Auto HDR.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td align="right">0x10001 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int SONY_AUTO_HDR = 0x200A;

    /**
     * Camera settings.
     */
    int SONY_CAMERA_SETTINGS = 0x0114;

    /**
     * Color compensation filter.
     * <p>
     * Type: Signed long.
     */
    int SONY_COLOR_COMPENSATION_FILTER = 0xB022;

    /**
     * Color mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td align="right">0 =&nbsp;</td><td>standard</td>
     *      </tr>
     *      <tr>
     *        <td align="right">1 =&nbsp;</td><td>vivid</td>
     *      </tr>
     *      <tr>
     *        <td align="right">2 =&nbsp;</td><td>portrait</td>
     *      </tr>
     *      <tr>
     *        <td align="right">3 =&nbsp;</td><td>landscape</td>
     *      </tr>
     *      <tr>
     *        <td align="right">4 =&nbsp;</td><td>sunset</td>
     *      </tr>
     *      <tr>
     *        <td align="right">5 =&nbsp;</td><td>night view/portrait</td>
     *      </tr>
     *      <tr>
     *        <td align="right">6 =&nbsp;</td><td>B&amp;W</td>
     *      </tr>
     *      <tr>
     *        <td align="right">7 =&nbsp;</td><td>Adobe RGB</td>
     *      </tr>
     *      <tr>
     *        <td align="right">12 =&nbsp;</td><td>neutral</td>
     *      </tr>
     *      <tr>
     *        <td align="right">100 =&nbsp;</td><td>neutral</td>
     *      </tr>
     *      <tr>
     *        <td align="right">101 =&nbsp;</td><td>clear</td>
     *      </tr>
     *      <tr>
     *        <td align="right">102 =&nbsp;</td><td>deep</td>
     *      </tr>
     *      <tr>
     *        <td align="right">103 =&nbsp;</td><td>light</td>
     *      </tr>
     *      <tr>
     *        <td align="right">104 =&nbsp;</td><td>night view</td>
     *      </tr>
     *      <tr>
     *        <td align="right">105 =&nbsp;</td><td>autumn leaves</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int SONY_COLOR_MODE = 0xB029;

    /**
     * Color reproduction.
     * <p>
     * Type: ASCII
     */
    int SONY_COLOR_REPRODUCTION = 0xB020;

    /**
     * Color temperature.
     * <p>
     * Type: Unsigned long.
     */
    int SONY_COLOR_TEMPERATURE = 0xB021;

    /**
     * Camera setting: auto-focus area mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>wide</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>local</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>spot</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_AF_AREA_MODE = SONY_CAMERA_SETTINGS << 8 | 0x11;

    /**
     * Camera setting: auto-focus illuminator.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>auto</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_AF_ILLUMINATOR = SONY_CAMERA_SETTINGS << 8 | 0x29;

    /**
     * Camera setting: auto-focus with shutter.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>on</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>off</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_AF_WITH_SHUTTER = SONY_CAMERA_SETTINGS << 8 | 0x2A;

    /**
     * Camera setting: aspect ratio.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>3:2</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>16:9</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_ASPECT_RATIO = SONY_CAMERA_SETTINGS << 8 | 0x55;

    /**
     * Camera setting: brightness.
     */
    int SONY_CS_BRIGHTNESS = SONY_CAMERA_SETTINGS << 8 | 0x22;

    /**
     * Camera setting: contrast.
     */
    int SONY_CS_CONTRAST = SONY_CAMERA_SETTINGS << 8 | 0x1D;

    /**
     * Camera setting: creative style.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td align="right">1 =&nbsp;</td><td>standard</td>
     *      </tr>
     *      <tr>
     *        <td align="right">2 =&nbsp;</td><td>vivid</td>
     *      </tr>
     *      <tr>
     *        <td align="right">3 =&nbsp;</td><td>portrait</td>
     *      </tr>
     *      <tr>
     *        <td align="right">4 =&nbsp;</td><td>landscape</td>
     *      </tr>
     *      <tr>
     *        <td align="right">5 =&nbsp;</td><td>sunset</td>
     *      </tr>
     *      <tr>
     *        <td align="right">6 =&nbsp;</td><td>night view/portrait</td>
     *      </tr>
     *      <tr>
     *        <td align="right">8 =&nbsp;</td><td>B&W</td>
     *      </tr>
     *      <tr>
     *        <td align="right">9 =&nbsp;</td><td>Adobe RGB</td>
     *      </tr>
     *      <tr>
     *        <td align="right">11 =&nbsp;</td><td>neitral</td>
     *      </tr>
     *      <tr>
     *        <td align="right">12 =&nbsp;</td><td>clear</td>
     *      </tr>
     *      <tr>
     *        <td align="right">13 =&nbsp;</td><td>deep</td>
     *      </tr>
     *      <tr>
     *        <td align="right">14 =&nbsp;</td><td>light</td>
     *      </tr>
     *      <tr>
     *        <td align="right">15 =&nbsp;</td><td>autumn</td>
     *      </tr>
     *      <tr>
     *        <td align="right">16 =&nbsp;</td><td>sepia</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_CREATIVE_STYLE = SONY_CAMERA_SETTINGS << 8 | 0x1A;

    /**
     * Camera setting: drive mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td align="right">1 =&nbsp;</td>
     *        <td>single frame</td>
     *      </tr>
     *      <tr>
     *        <td align="right">2 =&nbsp;</td>
     *        <td>continuous high</td>
     *      </tr>
     *      <tr>
     *        <td align="right">4 =&nbsp;</td>
     *        <td>self-timer 10s</td>
     *      </tr>
     *      <tr>
     *        <td align="right">5 =&nbsp;</td>
     *        <td>self-timer 2s</td>
     *      </tr>
     *      <tr>
     *        <td align="right">7 =&nbsp;</td>
     *        <td>continuous bracketing</td>
     *      </tr>
     *      <tr>
     *        <td align="right">12 =&nbsp;</td>
     *        <td>continuous low</td>
     *      </tr>
     *      <tr>
     *        <td align="right">18 =&nbsp;</td>
     *        <td>white balance bracketing low</td>
     *      </tr>
     *      <tr>
     *        <td align="right">19 =&nbsp;</td>
     *        <td>D-range optimizer bracketing low</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_DRIVE_MODE = SONY_CAMERA_SETTINGS << 8 | 0x04;

    /**
     * Camera setting: dynamic range optimizer level.
     */
    int SONY_CS_DYNAMIC_RANGE_OPTIMIZER_LEVEL =
        SONY_CAMERA_SETTINGS << 8 | 0x19;

    /**
     * Camera setting: exposure level increments.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>33 =&nbsp;</td><td>1/3 EV</td></tr>
     *      <tr><td>55 =&nbsp;</td><td>1/2 EV</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_EXPOSURE_LEVEL_INCREMENTS = SONY_CAMERA_SETTINGS << 8 | 0x58;

    /**
     * Camera setting: exposure program.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td align="right">0 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr>
     *        <td align="right">1 =&nbsp;</td><td>manual</td>
     *      </tr>
     *      <tr>
     *        <td align="right">2 =&nbsp;</td><td>program AE</td>
     *      </tr>
     *      <tr>
     *        <td align="right">3 =&nbsp;</td><td>aperture-priority AE</td>
     *      </tr>
     *      <tr>
     *        <td align="right">4 =&nbsp;</td><td>shutter=speed priority AE</td>
     *      </tr>
     *      <tr>
     *        <td align="right">8 =&nbsp;</td><td>program shift A</td>
     *      </tr>
     *      <tr>
     *        <td align="right">9 =&nbsp;</td><td>program shift S</td>
     *      </tr>
     *      <tr>
     *        <td align="right">19 =&nbsp;</td><td>night portrait</td>
     *      </tr>
     *      <tr>
     *        <td align="right">18 =&nbsp;</td><td>sunset</td>
     *      </tr>
     *      <tr>
     *        <td align="right">17 =&nbsp;</td><td>sports</td>
     *      </tr>
     *      <tr>
     *        <td align="right">21 =&nbsp;</td><td>macro</td>
     *      </tr>
     *      <tr>
     *        <td align="right">20 =&nbsp;</td><td>landscape</td>
     *      </tr>
     *      <tr>
     *        <td align="right">16 =&nbsp;</td><td>portrait</td>
     *      </tr>
     *      <tr>
     *        <td align="right">35 =&nbsp;</td><td>auto no flash</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_EXPOSURE_PROGRAM = SONY_CAMERA_SETTINGS << 8 | 0x3C;

    /**
     * Camera setting: flash mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>ADI</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>TTL</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_FLASH_MODE = SONY_CAMERA_SETTINGS << 8 | 0x23;

    /**
     * Camera setting: focus mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>manual</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>AF-S</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>AF-C</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>AF-A</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_FOCUS_MODE = SONY_CAMERA_SETTINGS << 8 | 0x10;

    /**
     * Camera setting: high ISO noise reduction.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>manual</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>low</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>high</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>off</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_HIGH_ISO_NOISE_REDUCTION = SONY_CAMERA_SETTINGS << 8 | 0x2C;

    /**
     * Camera setting: image size.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>large</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>medium</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>small</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_IMAGE_SIZE = SONY_CAMERA_SETTINGS << 8 | 0x54;

    /**
     * Camera setting: image stabilization.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_IMAGE_STABILIZATION = SONY_CAMERA_SETTINGS << 8 | 0x3D;

    /**
     * Camera setting: image style.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td align="right">1 =&nbsp;</td><td>standard</td></tr>
     *      <tr><td align="right">2 =&nbsp;</td><td>vivid</td></tr>
     *      <tr><td align="right">9 =&nbsp;</td><td>Adobe RGB</td></tr>
     *      <tr><td align="right">11 =&nbsp;</td><td>neutral</td></tr>
     *      <tr><td align="right">129 =&nbsp;</td><td>style box 1</td></tr>
     *      <tr><td align="right">130 =&nbsp;</td><td>style box 2</td></tr>
     *      <tr><td align="right">131 =&nbsp;</td><td>style box 3</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_IMAGE_STYLE = SONY_CAMERA_SETTINGS << 8 | 0x2D;

    /**
     * Camera setting: ISO.
     */
    int SONY_CS_ISO_SETTING = SONY_CAMERA_SETTINGS << 8 | 0x16;

    /**
     * Camera setting: image style.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td alight="right">1 =&nbsp;</td><td>center</td></tr>
     *      <tr><td alight="right">2 =&nbsp;</td><td>top</td></tr>
     *      <tr><td alight="right">3 =&nbsp;</td><td>top-right</td></tr>
     *      <tr><td alight="right">4 =&nbsp;</td><td>right</td></tr>
     *      <tr><td alight="right">5 =&nbsp;</td><td>bottom-right</td></tr>
     *      <tr><td alight="right">6 =&nbsp;</td><td>bottom</td></tr>
     *      <tr><td alight="right">7 =&nbsp;</td><td>bottom-left</td></tr>
     *      <tr><td alight="right">8 =&nbsp;</td><td>left</td></tr>
     *      <tr><td alight="right">9 =&nbsp;</td><td>top-left</td></tr>
     *      <tr><td alight="right">10 =&nbsp;</td><td>far right</td></tr>
     *      <tr><td alight="right">11 =&nbsp;</td><td>far left</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_LOCAL_AF_AREA_POINT = SONY_CAMERA_SETTINGS << 8 | 0x12;

    /**
     * Camera setting: long exposure noise reduction.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_LONG_EXPOSURE_NOISE_REDUCTION =
        SONY_CAMERA_SETTINGS << 8 | 0x2B;

    /**
     * Camera setting: metering mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td align="right">1 =&nbsp;</td><td>multi-segment</td>
     *      </tr>
     *      <tr>
     *        <td align="right">2 =&nbsp;</td><td>center-weighted average</td>
     *      </tr>
     *      <tr>
     *        <td align="right">4 =&nbsp;</td><td>spot</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_METERING_MODE = SONY_CAMERA_SETTINGS << 8 | 0x15;

    /**
     * Camera setting: priority setup shutter release.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>AF</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>release</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_PRIORITY_SETUP_SHUTTER_RELEASE =
        SONY_CAMERA_SETTINGS << 8 | 0x28;

    /**
     * Camera setting: quality.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">0 =&nbsp;</td><td>raw</td></tr>
     *      <tr><td align="right">2 =&nbsp;</td><td>craw</td></tr>
     *      <tr><td align="right">16 =&nbsp;</td><td>extra fine</td></tr>
     *      <tr><td align="right">32 =&nbsp;</td><td>fine</td></tr>
     *      <tr><td align="right">34 =&nbsp;</td><td>raw + JPEG</td></tr>
     *      <tr><td align="right">35 =&nbsp;</td><td>craw + JPEG</td></tr>
     *      <tr><td align="right">48 =&nbsp;</td><td>standard</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_QUALITY = SONY_CAMERA_SETTINGS << 8 | 0x56;

    /**
     * Camera setting: rotation.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>horizontal</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>90 CW</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>270 CW</td></tr>
     *    </table>
     *  </blockquote>
     */
    int SONY_CS_ROTATION = SONY_CAMERA_SETTINGS << 8 | 0x3F;

    /**
     * Camera setting: saturation.
     */
    int SONY_CS_SATURATION = SONY_CAMERA_SETTINGS << 8 | 0x1E;

    /**
     * Camera setting: sharpness.
     */
    int SONY_CS_SHARPNESS = SONY_CAMERA_SETTINGS << 8 | 0x1C;

    /**
     * Camera setting: white balance fine tune (for A700 model only).
     * <p>
     * Type: Signed short.
     */
    int SONY_CS_WHITE_BALANCE_FINE_TUNE = SONY_CAMERA_SETTINGS << 8 | 0x06;

    /**
     * Camera setting: zone matching value.
     */
    int SONY_CS_ZONE_MATCHING_VALUE = SONY_CAMERA_SETTINGS << 8 | 0x1F;

    /**
     * Dynamic optimizer.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td align="right">1 =&nbsp;</td><td>standard</td></tr>
     *      <tr><td align="right">2 =&nbsp;</td><td>advanced auto</td></tr>
     *      <tr><td align="right">3 =&nbsp;</td><td>auto</td></tr>
     *      <tr><td align="right">8 =&nbsp;</td><td>advanced level 1</td></tr>
     *      <tr><td align="right">9 =&nbsp;</td><td>advanced level 2</td></tr>
     *      <tr><td align="right">10 =&nbsp;</td><td>advanced level 3</td></tr>
     *      <tr><td align="right">11 =&nbsp;</td><td>advanced level 4</td></tr>
     *      <tr><td align="right">12 =&nbsp;</td><td>advanced level 5</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int SONY_DYNAMIC_OPTIMIZER = 0xB025;

    /**
     * Dynamic range optimizer.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>fine</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int SONY_DYNAMIC_RANGE_OPTIMIZER = 0xB04F;

    /**
     * Exposure mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td align="right">0 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr>
     *        <td align="right">1 =&nbsp;</td><td>portrait</td>
     *      </tr>
     *      <tr>
     *        <td align="right">2 =&nbsp;</td><td>beach</td>
     *      </tr>
     *      <tr>
     *        <td align="right">4 =&nbsp;</td><td>snow</td>
     *      </tr>
     *      <tr>
     *        <td align="right">5 =&nbsp;</td><td>landscape</td>
     *      </tr>
     *      <tr>
     *        <td align="right">6 =&nbsp;</td><td>program</td>
     *      </tr>
     *      <tr>
     *        <td align="right">7 =&nbsp;</td><td>aperture priority</td>
     *      </tr>
     *      <tr>
     *        <td align="right">8 =&nbsp;</td><td>shutter priority</td>
     *      </tr>
     *      <tr>
     *        <td align="right">9 =&nbsp;</td><td>night scene</td>
     *      </tr>
     *      <tr>
     *        <td align="right">10 =&nbsp;</td><td>high-speed shutter</td>
     *      </tr>
     *      <tr>
     *        <td align="right">11 =&nbsp;</td><td>twilight portrait</td>
     *      </tr>
     *      <tr>
     *        <td align="right">12 =&nbsp;</td><td>soft snap</td>
     *      </tr>
     *      <tr>
     *        <td align="right">13 =&nbsp;</td><td>fireworks</td>
     *      </tr>
     *      <tr>
     *        <td align="right">15 =&nbsp;</td><td>high sensitivity</td>
     *      </tr>
     *      <tr>
     *        <td align="right">18 =&nbsp;</td><td>manual</td>
     *      </tr>
     *      <tr>
     *        <td align="right">29 =&nbsp;</td><td>underwater</td>
     *      </tr>
     *      <tr>
     *        <td align="right">33 =&nbsp;</td><td>gourmet</td>
     *      </tr>
     *      <tr>
     *        <td align="right">34 =&nbsp;</td><td>panorama</td>
     *      </tr>
     *      <tr>
     *        <td align="right">35 =&nbsp;</td><td>handheld twilight</td>
     *      </tr>
     *      <tr>
     *        <td align="right">36 =&nbsp;</td><td>anti motion blur</td>
     *      </tr>
     *      <tr>
     *        <td align="right">37 =&nbsp;</td><td>pet</td>
     *      </tr>
     *      <tr>
     *        <td align="right">38 =&nbsp;</td><td>backlight correction HDR</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int SONY_EXPOSURE_MODE = 0xB041;

    /**
     * Flash exposure compensation.
     * <p>
     * Type: Signed rational.
     */
    int SONY_FLASH_EXPOSURE_COMPENSATION = 0x0104;

    /**
     * Full image size in pixels.
     * <p>
     * Type: Unsigned long.
     */
    int SONY_FULL_IMAGE_SIZE = 0xB02B;

    /**
     * Image stabilization.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int SONY_IMAGE_STABILIZATION = 0xB026;

    /**
     * Intelligent auto.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>advanced</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int SONY_INTELLIGENT_AUTO = 0xB052;

    /**
     * Lens type.
     * <p>
     * Type: Unsigned long.
     */
    int SONY_LENS_TYPE = 0xB027;

    /**
     * Long exposure noise reduction.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int SONY_LONG_EXPOSURE_NOISE_REDUCTION = 0xB04E;

    /**
     * Macro.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int SONY_MACRO = 0xB040;

    /**
     * Minolta maker notes.
     * <p>
     * Type: Unsigned long.
     */
    int SONY_MINOLTA_MAKER_NOTE = 0xB028;

    /**
     * Preview image.
     * <p>
     * Type: Undefined.
     */
    int SONY_PREVIEW_IMAGE = 0x2001;

    /**
     * Preview image size in pixels.
     * <p>
     * Type: Unsigned long.
     */
    int SONY_PREVIEW_IMAGE_SIZE = 0xB02C;

    /**
     * Quality.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>raw</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>super fine</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>fine</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>standard</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>economy</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>extra fine</td></tr>
     *      <tr><td>6 =&nbsp;</td><td>raw + JPEG</td></tr>
     *      <tr><td>7 =&nbsp;</td><td>compressed raw</td></tr>
     *      <tr><td>8 =&nbsp;</td><td>compressed raw + JPEG</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int SONY_QUALITY = 0x0102;

    /**
     * Quality.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>fine</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int SONY_QUALITY_2 = 0xB047;

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
    int SONY_SCENE_MODE = 0xB023;

    /**
     * Shot info.
     */
    int SONY_SHOT_INFO = 0x3000;

    /**
     * Teleconverter.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td align="right">0 =&nbsp;</td><td>none</td>
     *      </tr>
     *      <tr>
     *        <td align="right">72 =&nbsp;</td><td>Minolta AF 2x APO (D)</td>
     *      </tr>
     *      <tr>
     *        <td align="right">80 =&nbsp;</td><td>Minolta AF 2x APO II</td>
     *      </tr>
     *      <tr>
     *        <td align="right">136 =&nbsp;</td><td>Minolta AF 1.4x APO (D)</td>
     *      </tr>
     *      <tr>
     *        <td align="right">144 =&nbsp;</td><td>Minolta AF 1.4x APO II</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int SONY_TELECONVERTER = 0x0105;

    /**
     * White balance.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0x00 =&nbsp;</td><td>auto</td></tr>
     *      <tr><td>0x01 =&nbsp;</td><td>color temperature/filter</td></tr>
     *      <tr><td>0x10 =&nbsp;</td><td>daylight</td></tr>
     *      <tr><td>0x20 =&nbsp;</td><td>cloudy</td></tr>
     *      <tr><td>0x30 =&nbsp;</td><td>shade</td></tr>
     *      <tr><td>0x40 =&nbsp;</td><td>tungsten</td></tr>
     *      <tr><td>0x50 =&nbsp;</td><td>flash</td></tr>
     *      <tr><td>0x60 =&nbsp;</td><td>fluorescent macro</td></tr>
     *      <tr><td>0x70 =&nbsp;</td><td>custom</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int SONY_WHITE_BALANCE = 0x0115;

    /**
     * White balance.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td align="right">0 =&nbsp;</td><td>auto</td></tr>
     *      <tr><td align="right">4 =&nbsp;</td><td>manual</td></tr>
     *      <tr><td align="right">5 =&nbsp;</td><td>daylight</td></tr>
     *      <tr><td align="right">14 =&nbsp;</td><td>incandescent</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int SONY_WHITE_BALANCE_2 = 0xB054;

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
    int SONY_ZONE_MATCHING = 0xB024;

}
/* vim:set et sw=4 ts=4: */
