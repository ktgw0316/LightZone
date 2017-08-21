/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.metadata.values.DateMetaValue;

import static com.lightcrafts.image.types.CIFFConstants.*;

/**
 * A <code>CIFFTags</code> defines the constants used for CIFF metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <i>CIFF 1.0r4</i>, Canon Incorporated, December 1997.
 */
public interface CIFFTags extends ImageMetaTags {

    /**
     * The camera's base ISO number.
     * <p>
     * Type: Unsigned short.
     */
    int CIFF_BASE_ISO                       = CIFF_FIELD_TYPE_USHORT | 0x1C;

    /**
     * The camera's production-identification code (e.g., serial number). This
     * is primarily to help with the manufacturer's service procedures.
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_BODY_ID                        = CIFF_FIELD_TYPE_ULONG | 0x0B;

    /**
     * Information on the camera that was used to take the picture.
     */
    int CIFF_CAMERA_OBJECT                  = CIFF_FIELD_TYPE_HEAP1 | 0x07;

    /**
     * Data block giving camera settings.
     * This has sub-fields; see all the <code>CIFF_CS_</code> tags.
     */
    int CIFF_CAMERA_SETTINGS                = CIFF_FIELD_TYPE_USHORT | 0x2D;

    /**
     * A heap storing information giving the camera's specifications.
     */
    int CIFF_CAMERA_SPECIFICATION           = CIFF_FIELD_TYPE_HEAP2 | 0x04;

    /**
     * The time the photo was taken.
     * 32-bit integer giving the time in seconds when the picture was taken,
     * followed by a 32-bit timezone in seconds.
     * <p>
     * Note, however, that, in {@link CIFFMetadataReader}, this value is
     * converted to a {@link DateMetaValue}.
     */
    int CIFF_CAPTURED_TIME                  = CIFF_FIELD_TYPE_ULONG | 0x0E;

    /**
     * The color-space.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 = &nbsp;</td><td>sRGB</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 = &nbsp;</td><td>Adobe RGB</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>-1 = &nbsp;</td><td>Uncalibrated</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int CIFF_COLOR_SPACE                    = CIFF_FIELD_TYPE_USHORT | 0xB4;

    /**
     * Color temperature in degrees Kelvin.
     * <p>
     * Type: Unsigned short.
     */
    int CIFF_COLOR_TEMPERATURE              = CIFF_FIELD_TYPE_USHORT | 0xAE;

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
     */
    int CIFF_CS_AF_POINT_SELECTED           = CIFF_CAMERA_SETTINGS << 8 | 0x13;

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
     */
    int CIFF_CS_CONTINUOUS_DRIVE_MODE       = CIFF_CAMERA_SETTINGS << 8 | 0x05;

    /**
     * Camera setting: contrast.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>-1 =&nbsp;</td><td>low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>high</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int CIFF_CS_CONTRAST                    = CIFF_CAMERA_SETTINGS << 8 | 0x0D;

    /**
     * Camera setting: digital zoom.
     */
    int CIFF_CS_DIGITAL_ZOOM                = CIFF_CAMERA_SETTINGS << 8 | 0x0C;

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
     */
    int CIFF_CS_EASY_SHOOTING_MODE          = CIFF_CAMERA_SETTINGS << 8 | 0x0B;

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
     */
    int CIFF_CS_EXPOSURE_MODE               = CIFF_CAMERA_SETTINGS << 8 | 0x14;

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
     */
    int CIFF_CS_FLASH_MODE                  = CIFF_CAMERA_SETTINGS << 8 | 0x04;

    /**
     * Camera setting: flash activity.
     */
    int CIFF_CS_FLASH_ACTIVITY              = CIFF_CAMERA_SETTINGS << 8 | 0x1C;

    /**
     * Camera setting: flash details.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>bit 15 =&nbsp;</td><td>external E-TTL</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>bit 13 =&nbsp;</td><td>internal flash</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>bit 11 =&nbsp;</td><td>FP sync used</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>bit 4 =&nbsp;</td><td>FP sync enabled</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int CIFF_CS_FLASH_DETAILS               = CIFF_CAMERA_SETTINGS << 8 | 0x1D;

    /**
     * Camera setting: focal units per mm.
     */
    int CIFF_CS_FOCAL_UNITS_PER_MM          = CIFF_CAMERA_SETTINGS << 8 | 0x19;

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
     */
    int CIFF_CS_FOCUS_MODE                  = CIFF_CAMERA_SETTINGS << 8 | 0x07;

    /**
     * Camera setting: focus mode (for G1).
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>single</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>continuous</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int CIFF_CS_FOCUS_MODE_G1               = CIFF_CAMERA_SETTINGS << 8 | 0x20;

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
     */
    int CIFF_CS_FOCUS_TYPE                  = CIFF_CAMERA_SETTINGS << 8 | 0x12;

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
     */
    int CIFF_CS_IMAGE_SIZE                  = CIFF_CAMERA_SETTINGS << 8 | 0x0A;

    /**
     * Camera setting: ISO.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td>
     *        <td>use {@link EXIFTags#EXIF_ISO_SPEED_RATINGS}</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>14 =&nbsp;</td><td>auto high</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>15 =&nbsp;</td><td>auto</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>16 =&nbsp;</td><td>50</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>17 =&nbsp;</td><td>100</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>18 =&nbsp;</td><td>200</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>19 =&nbsp;</td><td>400</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>20 =&nbsp;</td><td>800</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Short.
     */
    int CIFF_CS_ISO                         = CIFF_CAMERA_SETTINGS << 8 | 0x10;

    /**
     * Camera setting: lens type.
     * <p>
     * Type: Signed short.
     */
    int CIFF_CS_LENS_TYPE                   = CIFF_CAMERA_SETTINGS << 8 | 0x16;

    /**
     * Camera setting: long focal length in &quot;focal units.&quot;
     */
    int CIFF_CS_LONG_FOCAL_LENGTH           = CIFF_CAMERA_SETTINGS << 8 | 0x17;

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
     */
    int CIFF_CS_MACRO_MODE                  = CIFF_CAMERA_SETTINGS << 8 | 0x01;

    /**
     * Camera setting: Maximum lens aperture in F-number;
     * <p>
     * Type: Short.
     */
    int CIFF_CS_MAX_APERTURE                = CIFF_CAMERA_SETTINGS << 8 | 0x1A;

    /**
     * Camera setting: Minimum lens aperture in F-number;
     * <p>
     * Type: Short.
     */
    int CIFF_CS_MIN_APERTURE                = CIFF_CAMERA_SETTINGS << 8 | 0x1B;

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
     */
    int CIFF_CS_METERING_MODE               = CIFF_CAMERA_SETTINGS << 8 | 0x11;

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
     */
    int CIFF_CS_QUALITY                     = CIFF_CAMERA_SETTINGS << 8 | 0x03;

    /**
     * Camera setting: saturation.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>-1 =&nbsp;</td><td>low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>high</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int CIFF_CS_SATURATION                  = CIFF_CAMERA_SETTINGS << 8 | 0x0E;

    /**
     * Camera setting: self-timer delay.
     * Length of time in 10ths of a second.
     */
    int CIFF_CS_SELF_TIMER_DELAY            = CIFF_CAMERA_SETTINGS << 8 | 0x02;

    /**
     * Camera setting: sharpness.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>-1 =&nbsp;</td><td>low</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>normal</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>high</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int CIFF_CS_SHARPNESS                   = CIFF_CAMERA_SETTINGS << 8 | 0x0F;

    /**
     * Camera setting: short focal length in &quot;focal units.&quot;
     */
    int CIFF_CS_SHORT_FOCAL_LENGTH          = CIFF_CAMERA_SETTINGS << 8 | 0x18;

    /**
     * Camera setting: zoomed resolution.
     */
    int CIFF_CS_ZOOMED_RESOLUTION           = CIFF_CAMERA_SETTINGS << 8 | 0x24;

    /**
     * Camera setting: zoomed resolution.
     */
    int CIFF_CS_ZOOMED_RESOLUTION_BASE      = CIFF_CAMERA_SETTINGS << 8 | 0x25;

    /**
     * Data block giving custom functions.
     * TODO: values
     */
    int CIFF_CUSTOM_FUNCTIONS               = 0x1033;

    /**
     * Raw decoder table.
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_DECODER_TABLE                  = CIFF_FIELD_TYPE_ULONG | 0x35;

    /**
     * The subdirectory containing most of the JPEG/TIFF EXIF information.
     */
    int CIFF_EXIF_INFORMATION               = CIFF_FIELD_TYPE_HEAP2 | 0x0B;

    /**
     * Exposure information.
     * TODO: values
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_EXPOSURE_INFO                  = CIFF_FIELD_TYPE_ULONG | 0x18;

    /**
     * File format description, e.g., "EOS DIGITAL REBEL CMOS RAW".
     * <p>
     * Type: ASCII.
     */
    int CIFF_FILE_DESCRIPTION               = CIFF_FIELD_TYPE_ASCII | 0x05;

    /**
     * 32-bit integer giving the number of the file.
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_FILE_NUMBER                    = CIFF_FIELD_TYPE_ULONG | 0x17;

    /**
     * The camera's firmware version as a null-terminated string. This is for
     * service use, and to allow it to be displayed to the end user by the
     * application software.
     * <p>
     * Type: ASCII.
     */
    int CIFF_FIRMWARE_VERSION               = CIFF_FIELD_TYPE_ASCII | 0x0B;

    /**
     * Result of flash use.
     * TODO: values
     */
    int CIFF_FLASH_INFO                     = CIFF_FIELD_TYPE_ULONG | 0x13;

    /**
     * Focal length.
     * This has sub-fields; see all the <code>CIFF_FL_</code> tags.
     */
    int CIFF_FOCAL_LENGTH                   = 0x1029;

    /**
     * Focal length: focal length when the image was taken.
     */
    int CIFF_FL_FOCAL_LENGTH                = CIFF_FOCAL_LENGTH << 8 | 0x01;

    /**
     * Focal length: focal plane X size.
     */
    int CIFF_FL_FOCAL_PLANE_X_SIZE          = CIFF_FOCAL_LENGTH << 8 | 0x02;

    /**
     * Focal length: focal plane Y size.
     */
    int CIFF_FL_FOCAL_PLANE_Y_SIZE          = CIFF_FOCAL_LENGTH << 8 | 0x03;

    /**
     * Arbitrary, null-terminated character data.
     */
    int CIFF_IMAGE_DESCRIPTION              = CIFF_FIELD_TYPE_HEAP1 | 0x04;

    /**
     * This defines the format of the full view-image file.  It should be noted
     * that the information stored in the JPEG file of the thumbnail image is
     * information concerning the full view image. In other words, information
     * concerning the thumbnail image itself is not stored; only that
     * concerning the corresponding full view image is. This property is
     * present for this reason, and is significant particularly when the
     * principal image is not in JPEG format.
     * <p>
     * TODO: values
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_IMAGE_FORMAT                   = CIFF_FIELD_TYPE_ULONG | 0x03;

    /**
     * Data block giving image information.
     * This has sub-fields; see all the <code>CIFF_II_</code> tags.
     */
    int CIFF_IMAGE_INFO                     = CIFF_FIELD_TYPE_ULONG | 0x10;

    /**
     * Image info: image width.
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_II_IMAGE_WIDTH                 = CIFF_IMAGE_INFO << 8 | 0x00;

    /**
     * Image info: image height.
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_II_IMAGE_HEIGHT                = CIFF_IMAGE_INFO << 8 | 0x01;

    /**
     * Image info: pixel aspect ratio.
     * Ratio between the horizontal and vertical size of a pixel
     * (horizontal/vertical).
     * <p>
     * Type: Float.
     */
    int CIFF_II_PIXEL_ASPECT_RATIO          = CIFF_IMAGE_INFO << 8 | 0x02;

    /**
     * Image info: rotation.
     * This indicates how many degrees in the counter-clockwise direction the
     * original data file should be rotated for correct viewing of the image.
     * If the image has already been rotated in the JPEG format, this value
     * will be zero.
     * <p>
     * When this value is modified, it should be modified in both the thumbnail
     * and full view-image files.  Nonetheless, if the two values differ, the
     * player should give final precedence to the data stored in the full
     * view-image file. If this value is then used, for example, to rotate the
     * data stored in the JPEG file structure itself, the
     * {@link #CIFF_II_IMAGE_WIDTH}, {@link #CIFF_II_IMAGE_HEIGHT},
     * {@link #CIFF_II_PIXEL_ASPECT_RATIO} and other data should be updated
     * correspondingly.  This value should be set to 0 when the image is not
     * sure to be rotated.
     * <p>
     * Type: Signed long.
     */
    int CIFF_II_ROTATION                    = CIFF_IMAGE_INFO << 8 | 0x03;

    /**
     * Image info: component bit depth.
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_II_COMPONENT_BIT_DEPTH         = CIFF_IMAGE_INFO << 8 | 0x04;

    /**
     * Image info: color bit depth.
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_II_COLOR_BIT_DEPTH             = CIFF_IMAGE_INFO << 8 | 0x05;

    /**
     * Image info: color or black &amp; white.
     * TODO
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_II_COLOR_BW                    = CIFF_IMAGE_INFO << 8 | 0x06;

    /**
     * Filename of the full view image as a null-terminated string.
     * <p>
     * Type: ASCII.
     */
    int CIFF_IMAGE_FILE_NAME                = CIFF_FIELD_TYPE_ASCII | 0x16;

    /**
     * The main subdirectory containing all meta information.
     */
    int CIFF_IMAGE_PROPS                    = CIFF_FIELD_TYPE_HEAP2 | 0x0A;

    /**
     * Type of file, e.g., "CRW:EOS DIGITAL REBEL CMOS RAW".
     * <p>
     * Type: ASCII.
     */
    int CIFF_IMAGE_TYPE                     = CIFF_FIELD_TYPE_ASCII | 0x15;

    /**
     * The embedded JPEG image data (2048x1360 pixels for the Canon 300D).
     */
    int CIFF_JPG_FROM_RAW                   = CIFF_FIELD_TYPE_MIXED | 0x07;

    /**
     * The camera model-name as two null-terminated stings, with an additional
     * null at the end: the first is the manufacturer name (e.g.,
     * &quot;Canon&quot;) and the next is the model name (e.g., &quot;PowerShot
     * ver1.00&quot;), followed by a null.  There will therefore be a total of
     * two consecutive nulls at the end of this entry.
     * <p>
     * Type: ASCII.
     */
    int CIFF_MAKE_MODEL                     = CIFF_FIELD_TYPE_ASCII | 0x0A;

    /**
     * Measured EV value (Luminance Value).
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_MEASURED_EV                    = CIFF_FIELD_TYPE_ULONG | 0x14;

    /**
     * The Measured Info subdirectory.
     */
    int CIFF_MEASURED_INFO                  = CIFF_FIELD_TYPE_HEAP2 | 0x03;

    /**
     * The owner-name setting entered into the camera as a null-terminated
     * string.
     * <p>
     * Type: ASCII.
     */
    int CIFF_OWNER_NAME                     = CIFF_FIELD_TYPE_ASCII | 0x10;

    /**
     * Data block giving picture-specific information.
     * This has sub-fields; see all the <code>CIFF_PI_</code> tags.
     */
    int CIFF_PICTURE_INFO                   = 0x1038;

    /**
     * Picture information: AF points used.
     */
    int CIFF_PI_AF_POINTS_USED              = CIFF_PICTURE_INFO << 8 | 0x16;

    /**
     * Picture information: image height.
     */
    int CIFF_PI_IMAGE_HEIGHT                = CIFF_PICTURE_INFO << 8 | 0x03;

    /**
     * Picture information: image height as shot.
     */
    int CIFF_PI_IMAGE_HEIGHT_AS_SHOT        = CIFF_PICTURE_INFO << 8 | 0x05;

    /**
     * Picture information: image width.
     */
    int CIFF_PI_IMAGE_WIDTH                 = CIFF_PICTURE_INFO << 8 | 0x02;

    /**
     * Picture information: image width as shot.
     */
    int CIFF_PI_IMAGE_WIDTH_AS_SHOT         = CIFF_PICTURE_INFO << 8 | 0x04;

    /**
     * The length (in bytes) of the preview image, if any.
     * <p>
     * Type: Unsigned long.
     * @see #CIFF_PREVIEW_IMAGE_OFFSET
     */
    int CIFF_PREVIEW_IMAGE_LENGTH           = CIFF_JPG_FROM_RAW << 8 | 0x01;

    /**
     * The offset to the start of the preview image, if any.
     * This tag ID isn't an actual CIFF tag ID.  It's been invented to provide
     * a tag/value to store a preview image's offset/length.
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_PREVIEW_IMAGE_OFFSET           = CIFF_JPG_FROM_RAW << 8 | 0x02;

    /**
     * The raw image data itself.
     */
    int CIFF_RAW_DATA                       = CIFF_FIELD_TYPE_MIXED | 0x05;

    /**
     * The number of pictures taken since the camera was manufactured.
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_RECORD_ID                      = CIFF_FIELD_TYPE_ULONG | 0x04;

    /**
     * Unknown.
     */
    int CIFF_RELEASE_SETTING                = CIFF_FIELD_TYPE_USHORT | 0x16;

    /**
     * The string "USA" for 300D's sold in North America.
     * <p>
     * Type: ASCII.
     */
    int CIFF_ROM_OPERATION_MODE             = CIFF_FIELD_TYPE_ASCII | 0x0D;

    /**
     * Time in milliseconds until release when using auto-release timer
     * function.
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_SELF_TIMER_TIME                = CIFF_FIELD_TYPE_ULONG | 0x06;

    /**
     * Sensor size and resolution information.
     * This has sub-fields; see all the <code>CIFF_SSI_</code> tags.
     */
    int CIFF_SENSOR_INFO                    = 0x1031;

    /**
     * Heap for storing photo-shooting data.
     */
    int CIFF_SHOOTING_RECORD                = CIFF_FIELD_TYPE_HEAP2 | 0x02;

    /**
     * Data block giving shot information.
     * This has sub-fields; see all the <code>CIFF_SI_</code> tags.
     */
    int CIFF_SHOT_INFO                      = 0x102A;

    /**
     * Method of taking pictures (i.e., shutter release).
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 = &nbsp;</td><td>Single shot</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 = &nbsp;</td><td>Continuous/successive exposures</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int CIFF_SHUTTER_RELEASE_METHOD         = CIFF_FIELD_TYPE_USHORT | 0x10;

    /**
     * Release timing.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 = &nbsp;</td><td>Shutter priority</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 = &nbsp;</td><td>Aperture priority</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int CIFF_SHUTTER_RELEASE_TIMING         = CIFF_FIELD_TYPE_USHORT | 0x11;

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
     */
    int CIFF_SI_AF_POINT_USED               = CIFF_SHOT_INFO << 8 | 0x0E;

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
     */
    int CIFF_SI_AUTO_EXPOSURE_BRACKETING    = CIFF_SHOT_INFO << 8 | 0x10;

    /**
     * Shot information: auto-rotate.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>-1 =&nbsp;</td><td>software rotate</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td><td>none</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td><td>90 CCW</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td><td>180</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td><td>90 CW</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int CIFF_SI_AUTO_ROTATE                 = CIFF_SHOT_INFO << 8 | 0x1B;

    /**
     * Shot information: bulb duration.
     * <p>
     * Type: Long.
     */
    int CIFF_SI_BULB_DURATION               = CIFF_SHOT_INFO << 8 | 0x18;

    /**
     * Shot information: F-stop.
     */
    int CIFF_SI_FNUMBER                     = CIFF_SHOT_INFO << 8 | 0x15;

    /**
     * Shot information: ISO.
     */
    int CIFF_SI_ISO                         = CIFF_SHOT_INFO << 8 | 0x02;

    /**
     * Shot information: exposure compensation.
     */
    int CIFF_SI_EXPOSURE_COMPENSATION       = CIFF_SHOT_INFO << 8 | 0x06;

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
     */
    int CIFF_SI_FLASH_BIAS                  = CIFF_SHOT_INFO << 8 | 0x0F;

    /**
     * Shot information: focus distance upper.
     */
    int CIFF_SI_FOCUS_DISTANCE_LOWER        = CIFF_SHOT_INFO << 8 | 0x14;

    /**
     * Shot information: focus distance lower.
     */
    int CIFF_SI_FOCUS_DISTANCE_UPPER        = CIFF_SHOT_INFO << 8 | 0x13;

    /**
     * Shot information: sequence number (if in a continuous burst).
     */
    int CIFF_SI_SEQUENCE_NUMBER             = CIFF_SHOT_INFO << 8 | 0x09;

    /**
     * Shot information: shutter speed expressed in APEX (Additive System of
     * Photographic Exposure) units.
     */
    int CIFF_SI_SHUTTER_SPEED               = CIFF_SHOT_INFO << 8 | 0x16;

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
     */
    int CIFF_SI_WHITE_BALANCE               = CIFF_SHOT_INFO << 8 | 0x07;

    /**
     * Sensor information: bottom border.
     */
    int CIFF_SSI_SENSOR_BOTTOM_BORDER       = CIFF_SENSOR_INFO << 8 | 0x08;

    /**
     * Sensor information: height.
     */
    int CIFF_SSI_SENSOR_HEIGHT              = CIFF_SENSOR_INFO << 8 | 0x02;

    /**
     * Sensor information: left border.
     */
    int CIFF_SSI_SENSOR_LEFT_BORDER         = CIFF_SENSOR_INFO << 8 | 0x05;

    /**
     * Sensor information: right border.
     */
    int CIFF_SSI_SENSOR_RIGHT_BORDER        = CIFF_SENSOR_INFO << 8 | 0x07;

    /**
     * Sensor information: top border.
     */
    int CIFF_SSI_SENSOR_TOP_BORDER          = CIFF_SENSOR_INFO << 8 | 0x06;

    /**
     * Sensor information: width.
     */
    int CIFF_SSI_SENSOR_WIDTH               = CIFF_SENSOR_INFO << 8 | 0x01;

    /**
     * This holds the distance to the subject, in one-millimeter units.
     * <p>
     * Type: Unsigned long.
     */
    int CIFF_TARGET_DISTANCE_SETTING        = CIFF_FIELD_TYPE_ULONG | 0x07;

    /**
     * This indicates whether the photograph was taken with the intention of
     * recording a real-world subject, or a written document.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 = &nbsp;</td><td>Real-world subject</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 = &nbsp;</td><td>Written document</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int CIFF_TARGET_IMAGE_TYPE              = CIFF_FIELD_TYPE_USHORT | 0x0A;

    /**
     * Filename of the thumbnail file as a null-terminated string.
     * <p>
     * Type: ASCII.
     */
    int CIFF_THUMBNAIL_FILE_NAME            = CIFF_FIELD_TYPE_ASCII | 0x17;

    /**
     * Thumbnail image data (JPEG 160x120 pixels).
     */
    int CIFF_THUMBNAIL_IMAGE                = CIFF_FIELD_TYPE_MIXED | 0x08;

    /**
     * User comment (usually blank).
     * <p>
     * Type: ASCII.
     */
    int CIFF_USER_COMMENT                   = CIFF_FIELD_TYPE_ASCII | 0x05;

    /**
     * TODO
     */
    int CIFF_WHITE_BALANCE_TABLE            = 0x10A9;

}
/* vim:set et sw=4 ts=4: */
