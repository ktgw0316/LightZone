/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import java.util.Arrays;
import java.util.List;

/**
 * An <code>EXIFTags</code> defines the constants used for EXIF metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see "Exchangeable image file format for digital still cameras: Exif Version
 * 2.2"
 */
public interface EXIFTags extends ImageMetaTags {

    /**
     * @see TIFFTags#TIFF_APERTURE_VALUE
     */
    int EXIF_APERTURE_VALUE                 = TIFFTags.TIFF_APERTURE_VALUE;

    /**
     * @see TIFFTags#TIFF_ARTIST
     */
    int EXIF_ARTIST                         = TIFFTags.TIFF_ARTIST;

    /**
     * @see TIFFTags#TIFF_BITS_PER_SAMPLE
     */
    int EXIF_BITS_PER_SAMPLE                = TIFFTags.TIFF_BITS_PER_SAMPLE;

    /**
     * @see TIFFTags#TIFF_BRIGHTNESS_VALUE
     */
    int EXIF_BRIGHTNESS_VALUE               = TIFFTags.TIFF_BRIGHTNESS_VALUE;

    /**
     * Indicates the color filter array (CFA) geometric pattern of the image
     * sensor when a one-chip color area sensor is used.  It does not apply to
     * all sensing methods.
     * <p>
     * Type: Undefined.
     */
    int EXIF_CFA_PATTERN                    = 0xA302;

    /**
     * Normally sRGB (=1) is used to define the color space based on the PC
     * monitor conditions and environment.  If a color space other than sRGB is
     * used, Uncalibrated (=FFFF.H) is set.  Image data recorded as
     * Uncalibrated can be treated as sRGB when it is converted to Flashpix.
     * <p>
     * Type: Unsigned short.
     */
    int EXIF_COLOR_SPACE                    = 0xA001;

    /**
     * Information specific to compressed data. The channels of each component
     * are arranged in order from the 1st component to the 4th.  For
     * uncompressed data the data arrangement is given in the
     * {@link #EXIF_PHOTOMETRIC_INTERPRETATION} tag.  However, since
     * PhotometricInterpretation can only express the order of Y, Cb and Cr,
     * this tag is provided for cases when compressed data uses components
     * other than Y, Cb, and Cr and to enable support of other sequences.  The
     * values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>n/a</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>Y</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>Cb</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>Cr</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>R</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>G</td></tr>
     *      <tr><td>6 =&nbsp;</td><td>B</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Undefined.
     */
    int EXIF_COMPONENTS_CONFIGURATION       = 0x9101;

    /**
     * Information specific to compressed data. The compression mode used for a
     * compressed image is indicated in unit bits per pixel.
     * <p>
     * Type: Unsigned rational.
     */
    int EXIF_COMPRESSED_BITS_PER_PIXEL      = 0x9102;

    /**
     * @see TIFFTags#TIFF_COMPRESSION
     */
    int EXIF_COMPRESSION                    = TIFFTags.TIFF_COMPRESSION;

    /**
     * This tag indicates the direction of contrast processing applied by the
     * camera when the image was shot.  The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>low saturation</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>hard</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int EXIF_CONTRAST                       = 0xA408;

    /**
     * @see TIFFTags#TIFF_COPYRIGHT
     */
    int EXIF_COPYRIGHT                      = TIFFTags.TIFF_COPYRIGHT;

    /**
     * This tag indicates the use of special processing on image data, such as
     * rendering geared to output.  When special ocessing is performed, the
     * reader is expected to disable or minimize any further processing.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>normal processing</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>custom processing</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int EXIF_CUSTOM_RENDERED                = 0xA401;

    /**
     * @see TIFFTags#TIFF_DATE_TIME
     */
    int EXIF_DATE_TIME                      = TIFFTags.TIFF_DATE_TIME;

    /**
     * The date and time when the image was stored as digital data. If, for
     * example, an image was captured by DSC and at the same time the file was
     * recorded, then the DateTimeOriginal and DateTimeDigitized will have the
     * same contents. The format is "YYYY:MM:DD HH:MM:SS" with time shown in
     * 24-hour format, and the date and time separated by one blank character
     * [20.H].  When the date and time are unknown, all the character spaces
     * except colons (":") may be filled with blank characters, or else the
     * Interoperability field may be filled with blank characters. The
     * character string length is 20 bytes including NULL for termination. When
     * the field is left blank, it is treated as unknown.
     * <p>
     * Type: ASCII.
     */
    int EXIF_DATE_TIME_DIGITIZED            = 0x9004;

    /**
     * The date and time when the original image data was generated. For a DSC
     * the date and time the picture was taken are recorded. The format is
     * "YYYY:MM:DD HH:MM:SS" with time shown in 24-hour format, and the date
     * and time separated by one blank character [20.H]. When the date and time
     * are unknown, all the character spaces except colons (":") may be filled
     * with blank characters, or else the Interoperability field may be filled
     * with blank characters.  The character string length is 20 bytes
     * including NULL for termination. When the field is left blank, it is
     * treated as unknown.
     * <p>
     * Type: ASCII.
     */
    int EXIF_DATE_TIME_ORIGINAL             = 0x9003;

    /**
     * This tag indicates information on the picture-taking conditions of a
     * particular camera model.  The tag is used only to indicate the
     * picture-taking conditions in the reader.
     * <p>
     * Type: Undefined.
     */
    int EXIF_DEVICE_SETTING_DESCRIPTION     = 0xA40B;

    /**
     * This tag indicates the digital zoom ratio when the image was shot. If
     * the numerator of the recorded value is 0, this indicates that digital
     * zoom was not used.
     * <p>
     * Type: Unsigned rational.
     */
    int EXIF_DIGITAL_ZOOM_RATIO             = 0xA404;

    /**
     * @see TIFFTags#TIFF_EXPOSURE_BIAS_VALUE
     */
    int EXIF_DOCUMENT_NAME                  = TIFFTags.TIFF_DOCUMENT_NAME;

    /**
     * The version of this standard supported.  Nonexistence of this field is
     * taken to mean nonconformance to the standard.  Conformance to this
     * standard is indicated by recording &quot;0220&quot; as 4-byte ASCII.
     * Since the type is UNDEFINED, there is no NULL for termination.
     * <p>
     * Type: Undefined.
     */
    int EXIF_EXIF_VERSION                   = 0x9000;

    /**
     * @see TIFFTags#TIFF_EXPOSURE_BIAS_VALUE
     */
    int EXIF_EXPOSURE_BIAS_VALUE            = TIFFTags.TIFF_EXPOSURE_BIAS_VALUE;

    /**
     * @see TIFFTags#TIFF_EXPOSURE_INDEX
     */
    int EXIF_EXPOSURE_INDEX                 = 0xA215;

    /**
     * This tag indicates the exposure mode set when the image was shot.  In
     * auto-bracketing mode, the camera shoots a series of frames of the same
     * scene at different exposure settings.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>auto exposure</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>manual exposure</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>auto bracket</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int EXIF_EXPOSURE_MODE                  = 0xA402;

    /**
     * @see TIFFTags#TIFF_EXPOSURE_PROGRAM
     */
    int EXIF_EXPOSURE_PROGRAM               = TIFFTags.TIFF_EXPOSURE_PROGRAM;

    /**
     * @see TIFFTags#TIFF_EXPOSURE_TIME
     */
    int EXIF_EXPOSURE_TIME                  = TIFFTags.TIFF_EXPOSURE_TIME;

    /**
     * Indicates the image source. If a DSC recorded the image, this tag value
     * of this tag always be set to 3 indicating that the image was recorded on
     * a DSC.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>others</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>scanner of transparent type</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>scanner of reflex type</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>DSC</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Undefined.
     */
    int EXIF_FILE_SOURCE                    = 0xA300;

    /**
     * @see TIFFTags#TIFF_FLASH
     */
    int EXIF_FLASH                          = TIFFTags.TIFF_FLASH;

    /**
     * @see TIFFTags#TIFF_FLASH_ENERGY
     */
    int EXIF_FLASH_ENERGY                   = 0xA20B;

    /**
     * The Flashpix format version supported by a FPXR file. If the FPXR
     * function supports Flashpix format Ver. 1.0, this is indicated similarly
     * to {@link #EXIF_EXIF_VERSION} by recording &quot;0100&quot; as 4-byte
     * ASCII. Since the type is UNDEFINED, there is no NULL for termination.
     * <p>
     * Type: Undefined.
     */
    int EXIF_FLASHPIX_VERSION               = 0xA000;

    /**
     * @see TIFFTags#TIFF_FNUMBER
     */
    int EXIF_FNUMBER                        = TIFFTags.TIFF_FNUMBER;

    /**
     * The actual focal length of the lens, in mm.  Conversion is not made to
     * the focal length of a 35 mm film camera.
     * <p>
     * Type: Unsigned rational.
     */
    int EXIF_FOCAL_LENGTH                   = 0x920A;

    /**
     * This tag indicates the equivalent focal length assuming a 35mm film
     * camera, in mm.  A value of 0 means the focal length is unknown.  Note
     * that this tag differs from the {@link #EXIF_FOCAL_LENGTH} tag.
     * <p>
     * Type: Unsigned short.
     */
    int EXIF_FOCAL_LENGTH_IN_35MM_FILM      = 0xA405;

    /**
     * Indicates the unit for measuring {@link #EXIF_FOCAL_PLANE_X_RESOLUTION}
     * and {@link #EXIF_FOCAL_PLANE_Y_RESOLUTION}.  The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>none</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>inch</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>cm</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int EXIF_FOCAL_PLANE_RESOLUTION_UNIT    = 0xA210;

    /**
     * Indicates the number of pixels in the image width (X) direction per
     * {@link #EXIF_FOCAL_PLANE_RESOLUTION_UNIT} on the camera focal plane.
     * <p>
     * Type: Unsigned rational.
     */
    int EXIF_FOCAL_PLANE_X_RESOLUTION       = 0xA20E;

    /**
     * Indicates the number of pixels in the image height (Y) direction per
     * {@link #EXIF_FOCAL_PLANE_RESOLUTION_UNIT} on the camera focal plane.
     * <p>
     * Type: Unsigned rational.
     */
    int EXIF_FOCAL_PLANE_Y_RESOLUTION       = 0xA20F;

    /**
     * The degree of overall image gain adjustment.
     * <p>
     * Type: Unsigned short.
     */
    int EXIF_GAIN_CONTROL                   = 0xA407;

    /**
     * Indicates the value of coefficient gamma.  The formula of transfer
     * function used for image reproduction is:
     *  <blockquote>
     *    (Reproduced value) = (Input value)<sup><small>gamma</small></sup>
     *  </blockquote>
     * Both reproduced value and input value indicate normalized value, whose
     * minimum value is 0 and maximum value if 1.
     * <p>
     * Type: Rational.
     */
    int EXIF_GAMMA                          = 0xA500;

    /**
     * @see TIFFTags#TIFF_GPS_IFD_POINTER
     */
    int EXIF_GPS_IFD_POINTER                = TIFFTags.TIFF_GPS_IFD_POINTER;

    /**
     * @see TIFFTags#TIFF_HOST_COMPUTER
     */
    int EXIF_HOST_COMPUTER                  = TIFFTags.TIFF_HOST_COMPUTER;

    /**
     * @see TIFFTags#TIFF_ICC_PROFILE
     */
    int EXIF_ICC_PROFILE                    = TIFFTags.TIFF_ICC_PROFILE;

    /**
     * @see TIFFTags#TIFF_EXIF_IFD_POINTER
     */
    int EXIF_IFD_POINTER                    = TIFFTags.TIFF_EXIF_IFD_POINTER;

    /**
     * @see TIFFTags#TIFF_IMAGE_DESCRIPTION
     */
    int EXIF_IMAGE_DESCRIPTION              = TIFFTags.TIFF_IMAGE_DESCRIPTION;

    /**
     * @see TIFFTags#TIFF_IMAGE_LENGTH
     */
    int EXIF_IMAGE_HEIGHT                   = TIFFTags.TIFF_IMAGE_LENGTH;

    /**
     * @see TIFFTags#TIFF_IMAGE_WIDTH
     */
    int EXIF_IMAGE_WIDTH                    = TIFFTags.TIFF_IMAGE_WIDTH;

    /**
     * The offset of the interoperability subdirectory.
     * <p>
     * Type: Unsigned long.
     */
    int EXIF_INTEROPERABILITY_POINTER       = 0xA005;

    /**
     * Indicates the ISO Speed and ISO Latitude of the camera or input device
     * as specified in ISO 12232.
     * <p>
     * Type: Unsigned short.
     */
    int EXIF_ISO_SPEED_RATINGS              = 0x8827;

    /**
     * @see TIFFTags#TIFF_JPEG_INTERCHANGE_FORMAT
     */
    int EXIF_JPEG_INTERCHANGE_FORMAT = TIFFTags.TIFF_JPEG_INTERCHANGE_FORMAT;

    /**
     * @see TIFFTags#TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH
     */
    int EXIF_JPEG_INTERCHANGE_FORMAT_LENGTH
        = TIFFTags.TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH;

    /**
     * @see TIFFTags#TIFF_LIGHT_SOURCE
     */
    int EXIF_LIGHT_SOURCE                   = TIFFTags.TIFF_LIGHT_SOURCE;

    /**
     * Lens make name.
     * Type: ASCII.
     */
    int EXIF_LENS_MAKE                      = 0xA433;

    /**
     * Lens model name.
     * Type: ASCII.
     */
    int EXIF_LENS_MODEL                     = 0xA434;

    /**
     * @see TIFFTags#TIFF_MAKE
     */
    int EXIF_MAKE                           = TIFFTags.TIFF_MAKE;

    /**
     * A tag for manufacturers of Exif writers to record any desired
     * information.  The contents are up to the manufacturer, but this tag
     * should not be used for any other than its intended purpose.
     * <p>
     * Type: Undefined.
     */
    int EXIF_MAKER_NOTE                     = 0x927C;

    /**
     * @see TIFFTags#TIFF_MAX_APERTURE_VALUE
     */
    int EXIF_MAX_APERTURE_VALUE             = TIFFTags.TIFF_MAX_APERTURE_VALUE;

    /**
     * @see TIFFTags#TIFF_MS_RATING
     */
    int EXIF_MS_RATING                      = TIFFTags.TIFF_MS_RATING;

    /**
     * @see TIFFTags#TIFF_METERING_MODE
     */
    int EXIF_METERING_MODE                  = TIFFTags.TIFF_METERING_MODE;

    /**
     * @see TIFFTags#TIFF_MODEL
     */
    int EXIF_MODEL                          = TIFFTags.TIFF_MODEL;

    /**
     * @see TIFFTags#TIFF_NEW_SUBFILE_TYPE
     */
    int EXIF_NEW_SUBFILE_TYPE               = TIFFTags.TIFF_NEW_SUBFILE_TYPE;

    /**
     * Indicates the Opto-Electric Conversion Function (OECF) specified in ISO
     * 14524.  OECF is the relationship between the camera optical input and
     * the image values.
     * <p>
     * Type: Undefined.
     */
    int EXIF_OECF                           = 0x8828;
    int EXIF_OECF_2                         = 0x8829;

    /**
     * @see TIFFTags#TIFF_ORIENTATION
     */
    int EXIF_ORIENTATION                    = TIFFTags.TIFF_ORIENTATION;

    /**
     * @see TIFFTags#TIFF_PHOTOMETRIC_INTERPRETATION
     */
    int EXIF_PHOTOMETRIC_INTERPRETATION
        = TIFFTags.TIFF_PHOTOMETRIC_INTERPRETATION;

    /**
     * Information specific to compressed data.  When a compressed file is
     * recorded, the valid width of the meaningful image shall be recorded in
     * this tag, whether or not there is padding data or a restart marker.
     * This tag should not exist in an uncompressed file.
     * <p>
     * Type: Unsigned short or unsigned long.
     */
    int EXIF_PIXEL_X_DIMENSION              = 0xA002;

    /**
     * Information specific to compressed data. When a compressed file is
     * recorded, the valid height of the meaningful image shall be recorded in
     * this tag, whether or not there is padding data or a restart marker. This
     * tag should not exist in an uncompressed file.  Since data padding is
     * unnecessary in the vertical direction, the number of lines recorded in
     * this valid image height tag will in fact be the same as that recorded in
     * the SOF.
     * <p>
     * Type: Unsigned short or unsigned long.
     */
    int EXIF_PIXEL_Y_DIMENSION              = 0xA003;

    /**
     * @see TIFFTags#TIFF_PLANAR_CONFIGURATION
     */
    int EXIF_PLANAR_CONFIGURATION = TIFFTags.TIFF_PLANAR_CONFIGURATION;

    /**
     * @see TIFFTags#TIFF_PREDICTOR
     */
    int EXIF_PREDICTOR                      = TIFFTags.TIFF_PREDICTOR;

    /**
     * @see TIFFTags#TIFF_PRIMARY_CHROMATICITIES
     */
    int EXIF_PRIMARY_CHROMATICITIES = TIFFTags.TIFF_PRIMARY_CHROMATICITIES;

    /**
     * @see TIFFTags#TIFF_REFERENCE_BLACK_WHITE
     */
    int EXIF_REFERENCE_BLACK_WHITE = TIFFTags.TIFF_REFERENCE_BLACK_WHITE;

    /**
     * This tag is used to record the name of an audio file related to the
     * image data.  The only relational information recorded here is the Exif
     * audio file name and extension (an ASCII string consisting of 8
     * characters + '.' + 3 characters).  The path is not recorded.
     * <p>
     * Type: ASCII.
     */
    int EXIF_RELATED_SOUND_FILE             = 0xA004;

    /**
     * @see TIFFTags#TIFF_RESOLUTION_UNIT
     */
    int EXIF_RESOLUTION_UNIT                = TIFFTags.TIFF_RESOLUTION_UNIT;

    /**
     * @see TIFFTags#TIFF_ROWS_PER_STRIP
     */
    int EXIF_ROWS_PER_STRIP                 = TIFFTags.TIFF_ROWS_PER_STRIP;

    /**
     * @see TIFFTags#TIFF_SAMPLES_PER_PIXEL
     */
    int EXIF_SAMPLES_PER_PIXEL              = TIFFTags.TIFF_SAMPLES_PER_PIXEL;

    /**
     * This tag indicates the direction of saturation processing applied by the
     * camera when the image was shot.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 = &nbsp;</td><td>normal</td></tr>
     *      <tr><td>1 = &nbsp;</td><td>low</td></tr>
     *      <tr><td>2 = &nbsp;</td><td>high</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int EXIF_SATURATION                     = 0xA409;

    /**
     * This tag indicates the type of scene that was shot.  It can also be used
     * to record the mode in which the image was shot.  Note that this differs
     * from the scene type {@link #EXIF_SCENE_TYPE} tag.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>standard</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>landscape</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>portrait</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>night scene</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int EXIF_SCENE_CAPTURE_TYPE             = 0xA406;

    /**
     * Indicates the type of scene. If a DSC recorded the image, this tag value
     * shall always be set to 1, indicating that the image was directly
     * photographed.
     * <p>
     * Type: Undefined.
     */
    int EXIF_SCENE_TYPE                     = 0xA301;

    /**
     * Indicates the image sensor type on the camera or input device. The
     * values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>undefined</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>one-chip color area sensor</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>two-chip color area sensor</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>three-chip color area sensor</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>color sequential area sensor</td></tr>
     *      <tr><td>7 =&nbsp;</td><td>trilinear sensor</td></tr>
     *      <tr><td>8 =&nbsp;</td><td>color sequential linear sensor</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int EXIF_SENSING_METHOD                 = 0xA217;

    /**
     * This tag indicates the direction of sharpness processing applied by the
     * camera when the image was shot.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>normal</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>soft</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>hard</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int EXIF_SHARPNESS                      = 0xA40A;

    /**
     * @see TIFFTags#TIFF_SHUTTER_SPEED_VALUE
     */
    int EXIF_SHUTTER_SPEED_VALUE            = TIFFTags.TIFF_SHUTTER_SPEED_VALUE;

    /**
     * @see TIFFTags#TIFF_SOFTWARE
     */
    int EXIF_SOFTWARE                       = TIFFTags.TIFF_SOFTWARE;

    /**
     * This tag records the camera or input device spatial frequency table and
     * SFR values in the direction of image width, image height, and diagonal
     * direction, as specified in ISO 12233.
     * <p>
     * Type: Undefined.
     */
    int EXIF_SPATIAL_FREQUENCY_RESPONSE     = 0xA20C;

    /**
     * @see TIFFTags#TIFF_SPECTRAL_SENSITIVITY
     */
    int EXIF_SPECTRAL_SENSITIVITY           = TIFFTags.TIFF_SPECTRAL_SENSITIVITY;

    /**
     * @see TIFFTags#TIFF_STRIP_BYTE_COUNTS
     */
    int EXIF_STRIP_BYTE_COUNTS              = TIFFTags.TIFF_STRIP_BYTE_COUNTS;

    /**
     * @see TIFFTags#TIFF_STRIP_OFFSETS
     */
    int EXIF_STRIP_OFFSETS                  = TIFFTags.TIFF_STRIP_OFFSETS;

    /**
     * @see TIFFTags#TIFF_SUBFILE_TYPE
     */
    int EXIF_SUBFILE_TYPE                   = TIFFTags.TIFF_SUBFILE_TYPE;

    /**
     * @see TIFFTags#TIFF_SUB_IFDS
     */
    int EXIF_SUB_IFDS                       = TIFFTags.TIFF_SUB_IFDS;

    /**
     * @see TIFFTags#TIFF_SUBJECT_LOCATION
     */
    int EXIF_SUBJECT_AREA                   = TIFFTags.TIFF_SUBJECT_LOCATION;

    /**
     * @see TIFFTags#TIFF_SUBJECT_DISTANCE
     */
    int EXIF_SUBJECT_DISTANCE               = TIFFTags.TIFF_SUBJECT_DISTANCE;

    /**
     * This tag indicates the distance to the subject.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>unknown</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>macro</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>close</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>distant</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int EXIF_SUBJECT_DISTANCE_RANGE         = 0xA40C;

    /**
     * Indicates the location of the main subject in the scene. The value of
     * this tag represents the pixel at the center of the main subject relative
     * to the left edge, prior to rotation processing as per the Rotation tag.
     * The first value indicates the X column number and second indicates the Y
     * row number.
     * <p>
     * When a camera records the main subject location, it is recommended that
     * the {@link #EXIF_SUBJECT_AREA} tag be used instead of this tag.
     * <p>
     * Type: Unsigned short.
     */
    int EXIF_SUBJECT_LOCATION               = 0xA214;

    /**
     * A tag used to record fractions of seconds for the
     * {@link #EXIF_DATE_TIME} tag.
     * <p>
     * Type: ASCII.
     */
    int EXIF_SUBSEC_TIME                    = 0x9290;

    /**
     * A tag used to record fractions of seconds for the
     * {@link #EXIF_DATE_TIME_DIGITIZED} tag.
     * <p>
     * Type: ASCII.
     */
    int EXIF_SUBSEC_TIME_DIGITIZED          = 0x9292;

    /**
     * A tag used to record fractions of seconds for the
     * {@link #EXIF_DATE_TIME_ORIGINAL} tag.
     * <p>
     * Type: ASCII.
     */
    int EXIF_SUBSEC_TIME_ORIGINAL           = 0x9291;

    /**
     * @see TIFFTags#TIFF_TILE_BYTE_COUNTS
     */
    int EXIF_TILE_BYTE_COUNTS               = TIFFTags.TIFF_TILE_BYTE_COUNTS;

    /**
     * @see TIFFTags#TIFF_TILE_LENGTH
     */
    int EXIF_TILE_LENGTH                    = TIFFTags.TIFF_TILE_LENGTH;

    /**
     * @see TIFFTags#TIFF_TILE_OFFSETS
     */
    int EXIF_TILE_OFFSETS                   = TIFFTags.TIFF_TILE_OFFSETS;

    /**
     * @see TIFFTags#TIFF_TRANSFER_FUNCTION
     */
    int EXIF_TRANSFER_FUNCTION              = TIFFTags.TIFF_TRANSFER_FUNCTION;

    /**
     * Undefined.  A tag for Exif users to write keywords or comments on the
     * image besides those in ImageDescription, and without the character code
     * limitations of the {@link #EXIF_IMAGE_DESCRIPTION} tag.
     * <p>
     * The character code used in the UserComment tag is identified based on an
     * ID code in a fixed 8-byte area at the start of the tag data area. The
     * unused portion of the area is padded with NULL ("00.H"). ID codes are
     * assigned by means of registration. The designation method and references
     * for each character code are given in Table 6 . The value of Count N is
     * determined based on the 8 bytes in the character code area and the
     * number of bytes in the user comment part. Since the TYPE is not ASCII,
     * NULL termination is not necessary.
     *  <blockquote>
     *    <table>
     *      <tr valign="top">
     *        <th>Character Code</th>
     *        <th>Code Designation (8 bytes)</th>
     *        <th>References</th>
     *      </tr>
     *      <tr valign="top">
     *        <td>ASCII</td>
     *        <td>41.H, 53.H, 43.H, 49.H, 49.H, 00.H, 00.H, 00.H</td>
     *        <td>ITU-T T.50 IA5</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>JIS</td>
     *        <td>4A.H, 49.H, 53.H, 00.H, 00.H, 00.H, 00.H, 00.H</td>
     *        <td>JIS X208-1990</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>Unicode</td>
     *        <td>55.H, 4E.H, 49.H, 43.H, 4F.H, 44.H, 45.H, 00.H</td>
     *        <td>Unicode Standard</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>Undefined</td>
     *        <td>00.H, 00.H, 00.H, 00.H, 00.H, 00.H, 00.H, 00.H</td>
     *        <td>Undefined</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * The ID code for the UserComment area may be a Defined code such as JIS
     * or ASCII, or may be Undefined. The Undefined name is UndefinedText, and
     * the ID code is filled with 8 bytes of all "NULL" ("00.H"). An Exif
     * reader that reads the UserComment tag shall have a function for
     * determining the ID code. This function is not required in Exif readers
     * that do not use the UserComment tag.
     * <p>
     * When a UserComment area is set aside, it is recommended that the ID code
     * be ASCII and that the following user comment part be filled with blank
     * characters [20.H].
     * <p>
     * Type: Undefined.
     */
    int EXIF_USER_COMMENT                   = 0x9286;

    /**
     * This tag indicates the white balance mode set when the image was shot.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>Auto white balance</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>Manual white balance</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int EXIF_WHITE_BALANCE                  = 0xA403;

    /**
     * The chromaticity of the white point of the image. Normally this tag is
     * not necessary, since color space is specified in the color space
     * information tag {@link #EXIF_COLOR_SPACE}.
     */
    int EXIF_WHITE_POINT                    = TIFFTags.TIFF_WHITE_POINT;

    /**
     * @see TIFFTags#TIFF_X_RESOLUTION
     */
    int EXIF_X_RESOLUTION                   = TIFFTags.TIFF_X_RESOLUTION;

    /**
     * @see TIFFTags#TIFF_YCBCR_COEFFICIENTS
     */
    int EXIF_YCBCR_COEFFICIENTS             = TIFFTags.TIFF_YCBCR_COEFFICIENTS;

    /**
     * @see TIFFTags#TIFF_YCBCR_POSITIONING
     */
    int EXIF_YCBCR_POSITIONING              = TIFFTags.TIFF_YCBCR_POSITIONING;

    /**
     * @see TIFFTags#TIFF_YCBCR_SUBSAMPLING
     */
    int EXIF_YCBCR_SUBSAMPLING              = TIFFTags.TIFF_YCBCR_SUBSAMPLING;

    /**
     * @see TIFFTags#TIFF_Y_RESOLUTION
     */
    int EXIF_Y_RESOLUTION                   = TIFFTags.TIFF_Y_RESOLUTION;

    int EXIF_CFA_PATTERN_2                  = 0x828E;
    int EXIF_EXPOSURE_INDEX_2               = 0x9215;

    List<Integer> TIFFCommonTags = Arrays.asList(
            EXIF_APERTURE_VALUE,
            EXIF_ARTIST,
            EXIF_BITS_PER_SAMPLE,
            EXIF_BRIGHTNESS_VALUE,
            EXIF_COMPRESSION,
            EXIF_COPYRIGHT,
            EXIF_DATE_TIME,
            EXIF_DOCUMENT_NAME,
            EXIF_EXPOSURE_BIAS_VALUE,
            EXIF_EXPOSURE_PROGRAM,
            EXIF_EXPOSURE_TIME,
            EXIF_FLASH,
            EXIF_FNUMBER,
            EXIF_GPS_IFD_POINTER,
            EXIF_HOST_COMPUTER,
            EXIF_ICC_PROFILE,
            EXIF_IFD_POINTER,
            EXIF_IMAGE_DESCRIPTION,
            EXIF_IMAGE_HEIGHT,
            EXIF_IMAGE_WIDTH,
            EXIF_JPEG_INTERCHANGE_FORMAT,
            EXIF_JPEG_INTERCHANGE_FORMAT_LENGTH,
            EXIF_LIGHT_SOURCE,
            EXIF_MAKE,
            EXIF_MAX_APERTURE_VALUE,
            EXIF_MS_RATING,
            EXIF_METERING_MODE,
            EXIF_MODEL,
            EXIF_NEW_SUBFILE_TYPE,
            EXIF_ORIENTATION,
            EXIF_PHOTOMETRIC_INTERPRETATION,
            EXIF_PLANAR_CONFIGURATION,
            EXIF_PREDICTOR,
            EXIF_PRIMARY_CHROMATICITIES,
            EXIF_REFERENCE_BLACK_WHITE,
            EXIF_RESOLUTION_UNIT,
            EXIF_ROWS_PER_STRIP,
            EXIF_SAMPLES_PER_PIXEL,
            EXIF_SHUTTER_SPEED_VALUE,
            EXIF_SOFTWARE,
            EXIF_SPECTRAL_SENSITIVITY,
            EXIF_STRIP_BYTE_COUNTS,
            EXIF_STRIP_OFFSETS,
            EXIF_SUBFILE_TYPE,
            EXIF_SUB_IFDS,
            EXIF_SUBJECT_AREA,
            EXIF_SUBJECT_DISTANCE,
            EXIF_TILE_BYTE_COUNTS,
            EXIF_TILE_LENGTH,
            EXIF_TILE_OFFSETS,
            EXIF_TRANSFER_FUNCTION,
            EXIF_WHITE_POINT,
            EXIF_X_RESOLUTION,
            EXIF_YCBCR_COEFFICIENTS,
            EXIF_YCBCR_POSITIONING,
            EXIF_YCBCR_SUBSAMPLING,
            EXIF_Y_RESOLUTION
    );
}
/* vim:set et sw=4 ts=4: */
