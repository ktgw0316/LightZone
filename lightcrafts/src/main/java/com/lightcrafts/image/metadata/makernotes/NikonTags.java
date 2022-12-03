/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.makernotes;

import com.lightcrafts.image.metadata.EXIFTags;
import com.lightcrafts.image.metadata.ImageMetaTags;
import com.lightcrafts.image.metadata.TIFFTags;

/**
 * A <code>NikonTags</code> defines the constants used for Nikon maker note
 * metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings( { "UnusedDeclaration" } )
public interface NikonTags extends ImageMetaTags {

    /**
     * AF point.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0x0000 =&nbsp;</td><td>center</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x0100 =&nbsp;</td><td>top</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x0200 =&nbsp;</td><td>bottom</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x0300 =&nbsp;</td><td>left</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x0400 =&nbsp;</td><td>Right</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x0000001 =&nbsp;</td><td>single area, center</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x0010002 =&nbsp;</td><td>single area, top</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x0020004 =&nbsp;</td><td>single area, bottom</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x0030008 =&nbsp;</td><td>single area, left</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x0040010 =&nbsp;</td><td>single area, right</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x1000001 =&nbsp;</td><td>dynamic area, center</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x1010002 =&nbsp;</td><td>dynamic area, top</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x1020004 =&nbsp;</td><td>dynamic area, bottom</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x1030008 =&nbsp;</td><td>dynamic area, left</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x1040010 =&nbsp;</td><td>dynamic area, right</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x2000001 =&nbsp;</td><td>closest subject, center</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x2010002 =&nbsp;</td><td>closest subject, top</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x2020004 =&nbsp;</td><td>closest subject, bottom</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x2030008 =&nbsp;</td><td>closest subject, left</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0x2040010 =&nbsp;</td><td>closest subject, right</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int NIKON_AF_POINT                      = 0x0088;

    /**
     * Auto-focus response.
     * <p>
     * Type: ASCII.
     */
    int NIKON_AF_RESPONSE                   = 0x00AD;

    /**
     * Auto bracket release.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>none</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>auto</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>manual</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int NIKON_AUTO_BRACKET_RELEASE          = 0x008A;

    /**
     * Auxiliary lens.
     * <p>
     * Type: ASCII.
     */
    int NIKON_AUXILIARY_LENS                = 0x0082;

    /**
     * Color hue.
     * <p>
     * Type: ASCII.
     */
    int NIKON_COLOR_HUE                     = 0x008D;

    /**
     * Color mode.
     * <p>
     * Type: ASCII.
     */
    int NIKON_COLOR_MODE                    = 0x0003;

    /**
     * Color space.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>sRGB</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>Adobe RGB</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int NIKON_COLOR_SPACE                   = 0x001E;

    /**
     * Crop high-speed.
     * <p>
     * Type: Unsigned short.
     */
    int NIKON_CROP_HIGH_SPEED               = 0x001B;

    /**
     * Digital zoom.
     * <p>
     * Type: Unsigned rational.
     */
    int NIKON_DIGITAL_ZOOM                  = 0x0086;

    /**
     * Exposure bracket value.
     * <p>
     * Type: Unsigned rational.
     */
    int NIKON_EXPOSURE_BRACKET_VALUE        = 0x0019;

    /**
     * Exposure difference.
     * <p>
     * Type: Undefined.
     */
    int NIKON_EXPOSURE_DIFFERENCE           = 0x000E;

    /**
     * Firmware version.
     * <p>
     * Type: ASCII.
     */
    int NIKON_FIRMWARE_VERSION              = 0x0001;

    /**
     * Flash exposure bracket value.
     * The value is in just the high-order byte.
     * <p>
     * Type: Unsigned long.
     */
    int NIKON_FLASH_EXPOSURE_BRACKET_VALUE  = 0x0018;

    /**
     * Flash exposure compensation.
     * The value is in just the high-order byte.
     * <p>
     * Type: Unsigned long.
     */
    int NIKON_FLASH_EXPOSURE_COMPENSATION   = 0x0012;

    /**
     * Flash mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>none</td></tr>
     *      <tr><td>8 =&nbsp;</td><td>commander</td></tr>
     *      <tr><td>9 =&nbsp;</td><td>TTL</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned byte.
     */
    int NIKON_FLASH_MODE                    = 0x0087;

    /**
     * Flash setting.
     * <p>
     * Type: ASCII.
     */
    int NIKON_FLASH_SETTING                 = 0x0008;

    /**
     * Flash type.
     * <p>
     * Type: ASCII.
     */
    int NIKON_FLASH_TYPE                    = 0x0009;

    /**
     * The focus mode for the photo.
     * <p>
     * Type: ASCII.
     */
    int NIKON_FOCUS_MODE                    = 0x0007;

    /**
     * High-ISO noise reduction.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>off</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>on for ISO 1600,3200</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>weak</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>6 =&nbsp;</td><td>strong</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int NIKON_HIGH_ISO_NOISE_REDUCTION      = 0x00B1;

    /**
     * Hue adjustment.
     * <p>
     * Type: Short.
     */
    int NIKON_HUE_ADJUSTMENT                = 0x0092;

    /**
     * Image adjustment.
     * <p>
     * Type: ASCII.
     */
    int NIKON_IMAGE_ADJUSTMENT              = 0x0080;

    /**
     * Image data size.
     * <p>
     * Type: Unsigned long.
     */
    int NIKON_IMAGE_DATA_SIZE               = 0x00A2;

    /**
     * TODO
     */
    int NIKON_IMAGE_OPTIMIZATION            = 0x00A9;

    /**
     * Image processing.
     * <p>
     * Type: ASCII.
     */
    int NIKON_IMAGE_PROCESSING              = 0x001A;

    /**
     * Image stabilization.
     * <p>
     * Type: ASCII.
     */
    int NIKON_IMAGE_STABILIZATION           = 0x00AC;

    /**
     * The ISO setting for the photo.
     * There are two numbers:
     *  <ol>
     *    <li>unknown
     *    <li>ISO setting
     *  </ol>
     * Type: Unsigned short.
     */
    int NIKON_ISO                           = 0x0002;

    /**
     * The ISO setting for the photo for a D70 camera.
     * There are two numbers:
     *  <ol>
     *    <li>unknown
     *    <li>ISO setting
     *  </ol>
     * Type: Unsigned short.
     */
    int NIKON_ISO_D70                       = 0x000F;

    /**
     * The ISO setting for the photo for a D70 camera.
     * There are two numbers:
     *  <ol>
     *    <li>unknown
     *    <li>ISO setting
     *  </ol>
     * Type: Unsigned short.
     */
    int NIKON_ISO_D70_2                     = 0x0013;

    /**
     * ISO data.
     * This has sub-fields; see all the {@code NIKON_II_} tags.
     */
    int NIKON_ISO_INFO                      = 0x0025;

    /**
     * ISO info: ISO.
     */
    @SuppressWarnings( { "PointlessBitwiseExpression" } )
    int NIKON_II_ISO                        = NIKON_ISO_INFO << 8 | 0x00;

    /**
     * ISO info: ISO.
     */
    int NIKON_II_ISO2                       = NIKON_ISO_INFO << 8 | 0x06;

    /**
     * Lens.  There are 4 values:
     *  <ol>
     *    <li>short focal length
     *    <li>long focal length
     *    <li>aperture at short focal length
     *    <li>aperture at long focal length
     *  </ol>
     * Type: Unsigned rational.
     */
    int NIKON_LENS                          = 0x0084;

    /**
     * Lens data.
     * This has sub-fields; see all the <code>NIKON_LD_</code> tags.
     */
    int NIKON_LENS_DATA                     = 0x0098;

    /**
     * Lens data version 0201: version.
     */
    @SuppressWarnings( { "PointlessBitwiseExpression" } )
    int NIKON_LD_VERSION                    = NIKON_LENS_DATA << 16 | 0x00;

    /**
     * Lens data version 010x: aperture at maximum focal length.
     */
    int NIKON_LD1X_APERTURE_AT_MAX_FOCAL    = NIKON_LENS_DATA << 16 | 0x100B;

    /**
     * Lens data version 010x: aperture at minimum focal length.
     */
    int NIKON_LD1X_APERTURE_AT_MIN_FOCAL    = NIKON_LENS_DATA << 16 | 0x100A;

    /**
     * Lens data version 010x: lens ID (name).
     */
    int NIKON_LD1X_LENS_ID                  = NIKON_LENS_DATA << 16 | 0x1006;

    /**
     * Lens data version 010x: F-stops.
     */
    int NIKON_LD1X_LENS_FSTOPS              = NIKON_LENS_DATA << 16 | 0x1007;

    /**
     * Lens data version 010x: maximum focal length.
     */
    int NIKON_LD1X_MAX_FOCAL_LENGTH         = NIKON_LENS_DATA << 16 | 0x1009;

    /**
     * Lens data version 010x: minimum focal length.
     */
    int NIKON_LD1X_MIN_FOCAL_LENGTH         = NIKON_LENS_DATA << 16 | 0x1008;

    /**
     * Lens data version 010x: MCU version.
     */
    int NIKON_LD1X_MCU_VERSION              = NIKON_LENS_DATA << 16 | 0x100C;

    /**
     * Lens data version 0201: auto-focus aperture.
     */
    int NIKON_LD21_AF_APERTURE              = NIKON_LENS_DATA << 16 | 0x2105;

    /**
     * Lens data version 0201: aperture at maximum focal length.
     */
    int NIKON_LD21_APERTURE_AT_MAX_FOCAL    = NIKON_LENS_DATA << 16 | 0x2110;

    /**
     * Lens data version 0201: aperture at minimum focal length.
     */
    int NIKON_LD21_APERTURE_AT_MIN_FOCAL    = NIKON_LENS_DATA << 16 | 0x210F;

    /**
     * Lens data version 0201: effective maximum aperture.
     */
    int NIKON_LD21_EFFECTIVE_MAX_APERTURE   = NIKON_LENS_DATA << 16 | 0x2112;

    /**
     * Lens data version 0201: focal length.
     */
    int NIKON_LD21_FOCAL_LENGTH             = NIKON_LENS_DATA << 16 | 0x210A;

    /**
     * Lens data version 0201: focus distance.
     */
    int NIKON_LD21_FOCUS_DISTANCE           = NIKON_LENS_DATA << 16 | 0x2109;

    /**
     * Lens data version 0201: focus position.
     */
    int NIKON_LD21_FOCUS_POSITION           = NIKON_LENS_DATA << 16 | 0x2108;

    /**
     * Lens data version 0201: F-stops.
     */
    int NIKON_LD21_LENS_FSTOPS              = NIKON_LENS_DATA << 16 | 0x210C;

    /**
     * Lens data version 0201: lens ID (name).
     */
    int NIKON_LD21_LENS_ID                  = NIKON_LENS_DATA << 16 | 0x210B;

    /**
     * Lens data version 0201: maximum focal length.
     */
    int NIKON_LD21_MAX_FOCAL_LENGTH         = NIKON_LENS_DATA << 16 | 0x210E;

    /**
     * Lens data version 0201: minimum focal length.
     */
    int NIKON_LD21_MIN_FOCAL_LENGTH         = NIKON_LENS_DATA << 16 | 0x210D;

    /**
     * Lens data version 0201: MCU version.
     */
    int NIKON_LD21_MCU_VERSION              = NIKON_LENS_DATA << 16 | 0x2111;

    /**
     * Lens data version 0204: auto-focus aperture.
     */
    int NIKON_LD24_AF_APERTURE              = NIKON_LENS_DATA << 16 | 0x2405;

    /**
     * Lens data version 0204: aperture at maximum focal length.
     */
    int NIKON_LD24_APERTURE_AT_MAX_FOCAL    = NIKON_LENS_DATA << 16 | 0x2411;

    /**
     * Lens data version 0204: aperture at minimum focal length.
     */
    int NIKON_LD24_APERTURE_AT_MIN_FOCAL    = NIKON_LENS_DATA << 16 | 0x2410;

    /**
     * Lens data version 0204: effective maximum aperture.
     */
    int NIKON_LD24_EFFECTIVE_MAX_APERTURE   = NIKON_LENS_DATA << 16 | 0x2413;

    /**
     * Lens data version 0204: focal length.
     */
    int NIKON_LD24_FOCAL_LENGTH             = NIKON_LENS_DATA << 16 | 0x240B;

    /**
     * Lens data version 0204: focus distance.
     */
    int NIKON_LD24_FOCUS_DISTANCE           = NIKON_LENS_DATA << 16 | 0x240A;

    /**
     * Lens data version 0204: focus position.
     */
    int NIKON_LD24_FOCUS_POSITION           = NIKON_LENS_DATA << 16 | 0x2408;

    /**
     * Lens data version 0204: F-stops.
     */
    int NIKON_LD24_LENS_FSTOPS              = NIKON_LENS_DATA << 16 | 0x240D;

    /**
     * Lens data version 0204: lens ID (name).
     */
    int NIKON_LD24_LENS_ID                  = NIKON_LENS_DATA << 16 | 0x240C;

    /**
     * Lens data version 0204: maximum focal length.
     */
    int NIKON_LD24_MAX_FOCAL_LENGTH         = NIKON_LENS_DATA << 16 | 0x240F;

    /**
     * Lens data version 0204: minimum focal length.
     */
    int NIKON_LD24_MIN_FOCAL_LENGTH         = NIKON_LENS_DATA << 16 | 0x240E;

    /**
     * Lens data version 0204: MCU version.
     */
    int NIKON_LD24_MCU_VERSION              = NIKON_LENS_DATA << 16 | 0x2412;

    /**
     * Lens F-stops.
     * <p>
     * Type: Undefined.
     */
    int NIKON_LENS_FSTOPS                   = 0x008B;

    /**
     * The type of lens used.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <td>bit 0 =&nbsp;</td><td>MF</td></tr>
     *      <td>bit 1 =&nbsp;</td><td>D</td></tr>
     *      <td>bit 2 =&nbsp;</td><td>G</td></tr>
     *      <td>bit 3 =&nbsp;</td><td>VR</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned byte.
     */
    int NIKON_LENS_TYPE                     = 0x0083;

    /**
     * Light source.
     * <p>
     * Type: ASCII.
     */
    int NIKON_LIGHT_SOURCE                  = 0x0090;

    /**
     * Manual focus distance.
     * <p>
     * Type: Unsigned rational.
     */
    int NIKON_MANUAL_FOCUS_DISTANCE         = 0x0085;

    /**
     * Noise reduction feature: on/off.
     * <p>
     * Type: ASCII.
     */
    int NIKON_NOISE_REDUCTION               = 0x0095;

    /**
     * Preview image IFD offset.
     * <p>
     * Type: Unsigned ong.
     */
    int NIKON_PREVIEW_IMAGE_IFD_POINTER     = 0x0011;

    /**
     * The length (in bytes) of the preview image, if any.
     * <p>
     * Type: Unsigned ong.
     * @see #NIKON_PREVIEW_IMAGE_OFFSET
     */
    int NIKON_PREVIEW_IMAGE_LENGTH =
        NIKON_PREVIEW_IMAGE_IFD_POINTER << 8 | 0x01;

    /**
     * The offset to the start of the preview image, if any.
     * This tag ID isn't an actual Nikon tag ID.  It's been invented to provide
     * a tag/value to store a preview image's offset/length that won't
     * overwrite either {@link TIFFTags#TIFF_JPEG_INTERCHANGE_FORMAT} or
     * {@link EXIFTags#EXIF_JPEG_INTERCHANGE_FORMAT} that may be used for a
     * thumbnail image.
     * <p>
     * Type: Unsigned long.
     */
    int NIKON_PREVIEW_IMAGE_OFFSET =
        NIKON_PREVIEW_IMAGE_IFD_POINTER << 8 | 0x02;

    /**
     * Photo quality.
     * <p>
     * Type: ASCII.
     */
    int NIKON_QUALITY                       = 0x0004;

    /**
     * Saturation.
     * <p>
     * Type: ASCII.
     */
    int NIKON_SATURATION                    = 0x00AA;

    /**
     * Scene mode.
     * <p>
     * Type: ASCII.
     */
    int NIKON_SCENE_MODE                    = 0x008F;

    /**
     * Serial number.
     * <p>
     * Type: ASCII.
     */
    int NIKON_SERIAL_NUMBER                 = 0x001D;

    /**
     * TODO
     */
    int NIKON_SERIAL_NUMBER_2               = 0x00A0;

    /**
     * Sensor pixel size.
     * <p>
     * Type: Unsigned rational.
     */
    int NIKON_SENSOR_PIXEL_SIZE             = 0x009A;

    /**
     * The sharpening setting for the photo.
     * <p>
     * Type: ASCII.
     */
    int NIKON_SHARPENING                    = 0x0006;

    /**
     * Shooting mode.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>bit 0 =&nbsp;</td><td>continuous</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>bit 1 =&nbsp;</td><td>delay</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>bit 2 =&nbsp;</td><td>PC control</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>bit 4 =&nbsp;</td><td>exposure bracketing</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>bit 5 =&nbsp;</td><td>unused LE-NR slowdown</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>bit 6 =&nbsp;</td><td>white-balance bracketing</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>bit 7 =&nbsp;</td><td>IR control</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int NIKON_SHOOTING_MODE                 = 0x0089;

    /**
     * The number of shots taken by the camera.
     * <p>
     * Type: Unsigned long.
     */
    int NIKON_SHUTTER_COUNT                 = 0x00A7;

    /**
     * Thumbnail dimensions (X,Y).
     * <p>
     * Type: Unsigned short.
     */
    int NIKON_THUMBNAIL_DIMENSIONS          = 0x0099;

    /**
     * The tone compensation.
     * <p>
     * Type: ASCII.
     */
    int NIKON_TONE_COMPENSATION             = 0x0081;

    /**
     * Vari-program.
     * <p>
     * Type: ASCII.
     */
    int NIKON_VARI_PROGRAM                  = 0x00AB;

    /**
     * White balance.
     * <p>
     * Type: ASCII.
     */
    int NIKON_WHITE_BALANCE                 = 0x0005;

    /**
     * Fine adjustment of white balance as set in the camera.
     * <p>
     * Type: Unsigned short.
     */
    int NIKON_WHITE_BALANCE_FINE_TUNE       = 0x000B;
}
/* vim:set et sw=4 ts=4: */
