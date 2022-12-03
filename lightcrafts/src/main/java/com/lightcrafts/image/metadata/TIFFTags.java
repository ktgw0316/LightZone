/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

/**
 * A <code>TIFFTags</code> defines the constants used for TIFF metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <i>TIFF Revision 6.0</i>, Adobe Systems, Incorporated, June 1992.
 */
public interface TIFFTags extends ImageMetaTags {

    /**
     * The lens aperture.  The unit is APEX (Additive System of Photographic
     * Exposure).
     * <p>
     * To convert this value to an ordinary F-number (F-stop), calculate
     * this value's power of root 2 (=1.4142).  For example, if the aperture
     * value is '5', the F-number is 1.4142^5 = F5.6.
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_APERTURE_VALUE                 = 0x9202;

    /**
     * The person who created the image.
     * <p>
     * Type: ASCII.
     */
    int TIFF_ARTIST                         = 0x013B;

    /**
     * This tag preferably encodes the camera's battery level as a ratio of
     * fullness at the time of image capture.  For example, a full battery
     * level is indicated by 1/1, half-full battery by 1/2, etc.  Alternately,
     * an ASCII string describing the battery level is allowed.
     * <p>
     * Type: Unsigned rational or ASCII.
     */
    int TIFF_BATTERY_LEVEL                  = 0x828F;

    /**
     * Number of bits per component.  Note that this field allows a different
     * number of bits per component for each component corresponding to a
     * pixel.  For example, RGB color data could use a different number of bits
     * per component for each of the three color planes.  Most RGB files will
     * have the same number of BitsPerSample for each component.  Even in this
     * case, the writer must write all three values.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_BITS_PER_SAMPLE                = 0x0102;

    /**
     * The value of brightness.  The unit is APEX (Additive System of
     * Photographic Exposure).  Ordinarily it is given in the range of -99.99
     * to 99.99.  Note that the numerator of the recorded value is FFFFFFFF.H,
     * Unknown shall be indicated.
     * <p>
     * Type: Signed rational.
     */
    int TIFF_BRIGHTNESS_VALUE               = 0x9203;

    /**
     * The length of the dithering or halftoning matrix used to create a
     * dithered or halftoned bilevel file.  This field should only be present
     * if {@link #TIFF_THRESHHOLDING} = 2.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_CELL_LENGTH                    = 0x0109;

    /**
     * The width of the dithering or halftoning matrix used to create a
     * dithered or halftoned bilevel file.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_CELL_WIDTH                     = 0x0108;

    /**
     * This is intended to mirror the essentials of PostScript's path creation
     * functionality, so that the operators listed below can be easily
     * translated into PostScript, and, conversely, any PostScript path can be
     * represented as a TIFF ClipPath. However, the TIFF ClipPath list of
     * operators is not identical to the cur- rent list of PostScript
     * operators; for simplicity, some of the PostScript variants have been
     * dropped, and a few operators, such as polyto and rpolyto have been
     * added. polyto and rpolyto were added in order to make complex TIFF
     * polygonal clip paths more compact.
     * <p>
     * Type: Unsigned byte.
     */
    int TIFF_CLIP_PATH                      = 0x0157;

    /**
     * A color map for palette color images.  This field defines a
     * Red-Green-Blue color map (often called a lookup table) for palette-color
     * images.  In a palette-color image, a pixel value is used to index into
     * an RGB lookup table.  For example, a palette-color pixel having a value
     * of 0 would be displayed according to the 0th Red, Green, Blue triplet.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_COLOR_MAP                      = 0x0140;

    /**
     * Compression scheme used on the image data.  The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>1 =&nbsp;</td><td>No compression.</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>2 =&nbsp;</td>
     *        <td>CCITT modified Huffman run length encoding</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>3 =&nbsp;</td><td>CCITT group 3 fax</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>4 =&nbsp;</td><td>CCITT group 4 fax</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>5 =&nbsp;</td><td>LZW</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>6 =&nbsp;</td><td>JPEG (old)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>7 =&nbsp;</td><td>JPEG</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>8 =&nbsp;</td><td>deflate</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>9 =&nbsp;</td><td>JBIG B&amp;W</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>10 =&nbsp;</td><td>JBIG color</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>32773 =&nbsp;</td><td>PackBits</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_COMPRESSION                    = 0x0103;

    /**
     * Copyright notice.  Copyright notice of the person or organization that
     * claims the copyright to the image. The complete copyright statement
     * should be listed in this field including any dates and statements of
     * claims.  For example, &quot;Copyright, John Smith, 19xx.  All rights
     * reserved.&quot;
     * <p>
     * Type: ASCII.
     */
    int TIFF_COPYRIGHT                      = 0x8298;

    /**
     * Date and time of image creation.  The format is: &quot;YYYY:MM:DD
     * HH:MM:SS&quot;, with hours like those on a 24-hour clock, and one space
     * character between the date and the time. The length of the string,
     * including the terminating NUL, is 20 bytes.
     * <p>
     * Type: ASCII.
     */
    int TIFF_DATE_TIME                      = 0x0132;

    /**
     * The name of the document from which this image was scanned.
     * <p>
     * Type: ASCII.
     */
    int TIFF_DOCUMENT_NAME                  = 0x010D;

    /**
     * The component values that correspond to a 0% dot and 100% dot.
     * DotRange[0] corresponds to a 0% dot, and DotRange[1] corresponds to a
     * 100% dot.
     * <p>
     * If a DotRange pair is included for each component, the values for each
     * component are stored together, so that the pair for Cyan would be first,
     * followed by the pair for Magenta, and so on. Use of multiple dot ranges
     * is, however, strongly discouraged in the interests of simplicity and
     * compatibility with ANSI IT8 standards.
     * <p>
     * A number of prepress systems like to keep some &quot;headroom&quot; and
     * &quot;footroom&quot; on both ends of the range. What to do with
     * components that are less than the 0% aim point or greater than the 100%
     * aim point is not specified and is application-dependent.
     * <p>
     * It is strongly recommended that a CMYK TIFF writer not attempt to use
     * this field to reverse the sense of the pixel values so that smaller
     * values mean more ink instead of less ink. That is, DotRange[0] should be
     * less than DotRange[1].
     * <p>
     * DotRange[0] and DotRange[1] must be within the range [0,
     * (2**BitsPerSample) - 1].
     * <p>
     * Type: Unsigned byte or unsigned short.
     */
    int TIFF_DOT_RANGE                      = 0x0150;

    /**
     * The offset of the EXIF subdirectory.
     * <p>
     * Type: Unsigned long.
     */
    int TIFF_EXIF_IFD_POINTER               = 0x8769;

    /**
     * The exposure bias.  The unit is the APEX (Additive System of
     * Photographic Exposure).  Ordinarily it is given in the range of -99.99
     * to 99.99.
     * <p>
     * Type: Signed rational.
     */
    int TIFF_EXPOSURE_BIAS_VALUE            = 0x9204;

    /**
     * Indicates the exposure index selected on the camera or input device at
     * the time the image is captured.
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_EXPOSURE_INDEX                 = 0x9215;

    /**
     * The class of the program used by the camera to set exposure when the
     * picture is taken.  The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>undefined</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>manual</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>normal program</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>aperture priority</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>shutter priority</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>creative program</td></tr>
     *      <tr><td>6 =&nbsp;</td><td>action program</td></tr>
     *      <tr><td>7 =&nbsp;</td><td>portrait mode</td></tr>
     *      <tr><td>8 =&nbsp;</td><td>landscape mode</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int TIFF_EXPOSURE_PROGRAM               = 0x8822;

    /**
     * Exposure time in seconds.
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_EXPOSURE_TIME                  = 0x829A;

    /**
     * Description of extra components.  Specifies that each pixel has <i>m</i>
     * extra components whose interpretation is defined by one of the values
     * listed below.  When this field is used, the
     * {@link #TIFF_SAMPLES_PER_PIXEL} field has a value greater than the
     * {@link #TIFF_PHOTOMETRIC_INTERPRETATION} field suggests.
     * <p>
     * For example, full-color RGB data normally has SamplesPerPixel=3. If
     * SamplesPerPixel is greater than 3, then the ExtraSamples field describes
     * the meaning of the extra samples. If SamplesPerPixel is, say, 5 then
     * ExtraSamples will contain 2 values, one for each extra sample.
     * <p>
     * ExtraSamples is typically used to include non-color information, such as
     * opacity, in an image.  The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>0 =&nbsp;</td><td>unspecified</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>1 =&nbsp;</td>
     *        <td>associated alpha data (with pre-multiplied color)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>2 =&nbsp;</td><td>unassociated alpha data</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int TIFF_EXTRA_SAMPLES                  = 0x0152;

    /**
     * The logical order of bits within a byte.  The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>1 =&nbsp;</td>
     *        <td>
     *          Pixels are arranged within a byte such that pixels with lower
     *          column values are stored in the higher-order bits of the byte.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>2 =&nbsp;</td>
     *        <td>
     *          Pixels are arranged within a byte such that pixels with lower
     *          column values are stored in the lower-order bits of the byte.
     *        </td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int TIFF_FILL_ORDER                     = 0x010A;

    /**
     * This tag indicates the status of flash when the image was shot.  Bit 0
     * indicates the flash firing status, bits 1 and 2 indicate the flash
     * return status, bits 3 and 4 indicate the flash mode, bit 5 indicates
     * whether the flash function is present, and bit 6 indicates &quot;red
     * eye&quot; mode.
     * <p>
     * The values for bit 0 are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>flash did not fire</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>flash fired</td></tr>
     *    </table>
     *  </blockquote>
     * The values for bits 1 and 2 are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td>0 =&nbsp;</td>
     *        <td>no strobe return detection function</td>
     *      </tr>
     *      <tr><td>1 =&nbsp;</td><td>reserved</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>strobe return light not detected</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>strobe return light detected</td></tr>
     *    </table>
     *  </blockquote>
     * The values for bits 3 and 4 are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>unknown</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>compulsory flash firing</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>compulsory flash suppression</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>auto mode</td></tr>
     *    </table>
     *  </blockquote>
     * The values for bit 5 are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>flash function present</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>no flash function</td></tr>
     *    </table>
     *  </blockquote>
     * The values for bit 6 are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>no red-eye reduction</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>red-eye reduction</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int TIFF_FLASH                          = 0x9209;

    /**
     * This optional tag encodes the amount of flash energy that was used when
     * the image was captured.  The measurement units are Beam Candle Power
     * Seconds (BCPS).  The flash energy may be specified by using a single
     * number if the exact flash energy is known.  Alternately, two values may
     * be used to indicate the range of uncertainty in the flash energy
     * setting.  In this case, the first value shall be the minimum value and
     * the second shall be the maximum.
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_FLASH_ENERGY                   = 0xA20B;

    /**
     * The F-number (F-stop) of the lens when the image was taken.
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_FNUMBER                        = 0x829D;

    /**
     * For each string of contiguous unused bytes in a TIFF file, the number of
     * bytes in the string.  Not recommended for general interchange.
     * <p>
     * Type: Unsigned long.
     * @see #TIFF_FREE_OFFSETS
     */
    int TIFF_FREE_BYTE_COUNTS               = 0x0121;

    /**
     * For each string of contiguous unused bytes in a TIFF file, the byte
     * offset of the string.  Not recommended for general interchange.
     * <p>
     * Type: Unsigned long.
     * @see #TIFF_FREE_BYTE_COUNTS
     */
    int TIFF_FREE_OFFSETS                   = 0x0120;

    /**
     * The offset of the GPS subdirectory.
     * <p>
     * Type: Unsigned long.
     */
    int TIFF_GPS_IFD_POINTER                = 0x8825;

    /**
     * For grayscale data, the optical density of each possible pixel value.
     * The 0th value of GrayResponseCurve corresponds to the optical density of
     * a pixel having a value of 0, and so on.  This field may provide useful
     * information for sophisticated applications, but it is currently ignored
     * by most TIFF readers.
     * <p>
     * Type: Unsigned short.
     * @see #TIFF_GRAY_RESPONSE_UNIT
     * @see #TIFF_PHOTOMETRIC_INTERPRETATION
     */
    int TIFF_GRAY_RESPONSE_CURVE            = 0x0123;

    /**
     * The precision of the information contained in the GrayResponseCurve.
     * <p>
     * Because optical density is specified in terms of fractional numbers,
     * this field is necessary to interpret the stored integer information. For
     * example, if GrayScaleResponseUnits is set to 4 (ten-thousandths of a
     * unit), and a GrayScaleResponseCurve number for gray level 4 is 3455,
     * then the resulting actual value is 0.3455.
     * <p>
     * Optical densitometers typically measure densities within the range of
     * 0.0 to 2.0.  The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>tenths of a unit</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>hundredths of a unit</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>thousandths of a unit</td></tr>
     *      <tr><td>4 =&nbsp;</td><td>ten-thousandths of a unit</td></tr>
     *      <tr><td>5 =&nbsp;</td><td>hundred-thousandths of a unit</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     * @see #TIFF_GRAY_RESPONSE_CURVE
     */
    int TIFF_GRAY_RESPONSE_UNIT             = 0x0122;

    /**
     * The purpose of the HalftoneHints field is to convey to the halftone
     * function the range of gray levels within a colorimetrically-specified
     * image that should retain tonal detail. The field contains two values of
     * sixteen bits each and, therefore, is contained wholly within the field
     * itself; no offset is required. The first word specifies the highlight
     * gray level which should be halftoned at the lightest printable tint of
     * the final output device. The second word specifies the shadow gray level
     * which should be halftoned at the darkest printable tint of the final
     * output device. Portions of the image which are whiter than the highlight
     * gray level will quickly, if not immediately, fade to specular
     * highlights. There is no default value specified, since the highlight and
     * shadow gray levels are a function of the subject matter of a particular
     * image.
     * <p>
     * Appropriate values may be derived algorithmically or may be specified by
     * the user, either directly or indirectly.
     * <p>
     * The HalftoneHints field, as defined here, defines an achromatic
     * function. It can be used just as effectively with color images as with
     * monochrome images. When used with opponent color spaces such as CIE
     * L*a*b* or YCbCr, it refers to the achromatic component only; L* in the
     * case of CIELab, and Y in the case of YCbCr. When used with tri-stimulus
     * spaces such as RGB, it suggests to retain tonal detail for all colors
     * with an NTSC gray component within the bounds of the R=G=B=Highlight to
     * R=G=B=Shadow range.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_HALFTONE_HINTS                 = 0x0141;

    /**
     * The computer and/or operating system in use at the time of image
     * creation.
     * <p>
     * Type: ASCII.
     * @see #TIFF_MAKE
     * @see #TIFF_MODEL
     * @see #TIFF_SOFTWARE
     */
    int TIFF_HOST_COMPUTER                  = 0x013C;

    /**
     * The ICC profile.
     * <p>
     * Type: Undefined.
     */
    int TIFF_ICC_PROFILE                    = 0x8773;

    /**
     * A string that describes the subject of the image.  For example, a user
     * may wish to attach a comment such as &quot;1988 company picnic&quot; to
     * an image.
     * <p>
     * Type: ASCII.
     */
    int TIFF_IMAGE_DESCRIPTION              = 0x010E;

    /**
     * This optional tag encodes a record of what has been done to the image.
     * The current information shall not be erased when adding new information
     * to the image history.  As changes are made, the additional information
     * about the changes should be concatenated to the previous string.  The
     * new information should be separated by one or more ASCII blank spaces,
     * and terminated with a NULL zero byte.
     * <p>
     * Type: ASCII.
     */
    int TIFF_IMAGE_HISTORY                  = 0x9213;

    /**
     * This is the full pathname of the original, high-resolution image, or any
     * other identifying string that uniquely identifies the original image.
     * <p>
     * The high-resolution image is not required to be in TIFF format. It can
     * be in any format that an OPI Consumer wishes to support.
     * <p>
     * Type: ASCII.
     */
    int TIFF_IMAGE_ID                       = 0x800D;

    /**
     * The number of rows (sometimes described as scanlines) in the image.
     * <p>
     * Type: Unsigned short or unsigned long.
     */
    int TIFF_IMAGE_LENGTH                   = 0x0101;

    /**
     * The number of columns in the image, i.e., the number of pixels per
     * scanline.
     * <p>
     * Type: Unsigned short or unsigned long.
     */
    int TIFF_IMAGE_WIDTH                    = 0x0100;

    /**
     * Indexed images are images where the "pixels" do not represent color
     * values, but rather an index (usually 8-bit) into a separate color table,
     * the {@link #TIFF_COLOR_MAP}.  {@link #TIFF_COLOR_MAP} is required for an
     * Indexed image.
     * <p>
     * The {@link #TIFF_PHOTOMETRIC_INTERPRETATION} type of PaletteColor may
     * still be used, and is equivalent to specifying an RGB image with the
     * Indexed flag set, a suitable {@link #TIFF_COLOR_MAP}, and
     * {@link #TIFF_SAMPLES_PER_PIXEL} = 1.
     * <p>
     * Do not use both the Indexed flag and
     * {@link #TIFF_PHOTOMETRIC_INTERPRETATION} = PaletteColor for the same
     * image.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_INDEXED                        = 0x015A;

    /**
     * The name of each ink used in a separated (PhotometricInterpretation=5)
     * image, written as a list of concatenated, NUL-terminated ASCII strings.
     * The number of strings must be equal to NumberOfInks.
     * <p>
     * Type: ASCII.
     * @see #TIFF_INK_SET
     * @see #TIFF_NUMBER_OF_INKS
     */
    int TIFF_INK_NAMES                      = 0x014D;

    /**
     * The set of inks used in a separated (PhotometricInterpretation=5) image.
     * The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>1 =&nbsp;</td>
     *        <td>
     *          CMYK.  The order of the components is cyan, magenta, yellow,
     *          black. Usually, a value of 0 represents 0% ink coverage and a
     *          value of 255 represents 100% ink coverage for that component,
     *          but see DotRange below. The InkNames field should not exist
     *          when InkSet=1.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>2 =&nbsp;</td>
     *        <td>
     *          Not CMYK.  See the {@link #TIFF_INK_NAMES} field for a
     *          description of the inks to be used.
     *        </td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int TIFF_INK_SET                        = 0x014C;

    /**
     * This field points to a list of offsets to the Huffman AC tables, one per
     * component.  The format of each table is as follows:
     *  <blockquote>
     *    16 BYTES of &quot;BITS&quot;, indicating the number of codes of
     *    lengths 1 to 16;
     *    <p>
     *    Up to 256 BYTES of &quot;VALUES&quot;, indicating the values
     *    associated with those codes, in order of length.
     *  </blockquote>
     * See the JPEG Draft International Standard (ISO DIS 10918-1) for more
     * details.
     * <p>
     * Type: Unsigned long.
     */
    int TIFF_JPEG_AC_TABLES                 = 0x0209;

    /**
     * This field points to a list of offsets to the DC Huffman tables or the
     * lossless Huffman tables, one per component.
     *  <blockquote>
     *    16 BYTES of &quot;BITS&quot;, indicating the number of codes of
     *    lengths 1 to 16;
     *    <p>
     *    Up to 17 BYTES of &quot;VALUES&quot;, indicating the values
     *    associated with those codes, in order of length.
     *  </blockquote>
     * See the JPEG Draft International Standard (ISO DIS 10918-1) for more
     * details.
     * <p>
     * Type: Unsigned long.
     */
    int TIFF_JPEG_DC_TABLES                 = 0x0208;

    /**
     * This field indicates whether a JPEG interchange format bitstream is
     * present in the TIFF file. If a JPEG interchange format bitstream is
     * present, then this field points the Start of Image (SOI) marker code.
     * <p>
     * If this field is zero or not present, a JPEG interchange format
     * bitstream is not present.
     * <p>
     * Type: Unsigned long.
     */
    int TIFF_JPEG_INTERCHANGE_FORMAT        = 0x0201;

    /**
     * This field indicates the length in bytes of the JPEG interchange format
     * bitstream.  This field is useful for extracting the JPEG interchange
     * format bitstream without parsing the bitstream.
     * <p>
     * This field is relevant only if the {@link #TIFF_JPEG_INTERCHANGE_FORMAT}
     * field is present and is non-zero.
     * <p>
     * Type: Unsigned long.
     */
    int TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH = 0x0202;

    /**
     * This field points to a list of lossless predictor-selection values, one
     * per component.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_JPEG_LOSSLESS_PREDICTORS       = 0x0205;

    /**
     * This field points to a list of point transform values, one per
     * component. This field is relevant only for lossless processes.
     * <p>
     * If the point transformation value is nonzero for a component, a point
     * transformation of the input is performed prior to the lossless coding.
     * The input is divided by 2**Pt, where Pt is the point transform value.
     * The output of the decoder is rescaled to the input range by multiplying
     * by 2**Pt. Note that the scaling of input and output can be performed by
     * arithmetic shifts.
     * <p>
     * See the JPEG Draft International Standard (ISO DIS 10918-1) for more
     * details.  The default value of this field is 0 for each component (no
     * scaling).
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_JPEG_POINT_TRANSFORMS          = 0x0206;

    /**
     * This field indicates the JPEG process used to produce the compressed
     * data. The values for this field are defined to be consistent with the
     * numbering convention used in ISO DIS 10918-2. Two values are defined at
     * this time.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td align="right">1 =&nbsp;</td>
     *        <td>baseline sequential process</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right">14 =&nbsp;</td>
     *        <td>lossless process with Huffman coding</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * When the lossless process with Huffman coding is selected by this field,
     * the Huffman tables used to encode the image are specified by the
     * {@link #TIFF_JPEG_DC_TABLES} field, and the {@link #TIFF_JPEG_AC_TABLES}
     * field is not used.
     * <p>
     * Values indicating JPEG processes other than those specified above will
     * be defined in the future.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_JPEG_PROC                      = 0x0200;

    /**
     * This field points to a list of offsets to the quantization tables, one
     * per component.  Each table consists of 64 BYTES (one for each DCT
     * coefficient in the 8x8 block).  The quantization tables are stored in
     * zigzag order.
     * <p>
     * See the JPEG Draft International Standard (ISO DIS 10918-1) for more
     * details.
     * <p>
     * Type: Unsigned long.
     */
    int TIFF_JPEG_Q_TABLES                  = 0x0207;

    /**
     * This field indicates the length of the restart interval used in the
     * compressed image data. The restart interval is defined as the number of
     * Minimum Coded Units (MCUs) between restart markers.
     * <p>
     * Restart intervals are used in JPEG compressed images to provide support
     * for multiple strips or tiles. At the start of each restart interval, the
     * coding state is reset to default values, allowing every restart interval
     * to be decoded independently of previously decoded data. TIFF strip and
     * tile offsets shall always point to the start of a restart interval.
     * Equivalently, each strip or tile contains an integral number of restart
     * intervals. Restart markers need not be present in a TIFF file; they are
     * implicitly coded at the start of every strip or tile.
     * <p>
     * See the JPEG Draft International Standard (ISO DIS 10918-1) for more
     * information about the restart interval and restart markers.
     * <p>
     * If this field is zero or is not present, the compressed data does not
     * contain restart markers.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_JPEG_RESTART_INTERVAL          = 0x0203;

    /**
     * The kind of light source.  The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td align="right">0 =&nbsp;</td><td>natural</td>
     *      </tr>
     *      <tr>
     *        <td align="right">1 =&nbsp;</td><td>daylight</td>
     *      </tr>
     *      <tr>
     *        <td align="right">2 =&nbsp;</td><td>fluorescent</td>
     *      </tr>
     *      <tr>
     *        <td align="right">3 =&nbsp;</td><td>incandescent</td>
     *      </tr>
     *      <tr>
     *        <td align="right">4 =&nbsp;</td><td>flash</td>
     *      </tr>
     *      <tr>
     *        <td align="right">9 =&nbsp;</td><td>fine weather</td>
     *      </tr>
     *      <tr>
     *        <td align="right">10 =&nbsp;</td><td>cloudy weather</td>
     *      </tr>
     *      <tr>
     *        <td align="right">11 =&nbsp;</td><td>shade</td>
     *      </tr>
     *      <tr>
     *        <td align="right">12 =&nbsp;</td><td>daylight fluorescent</td>
     *      </tr>
     *      <tr>
     *        <td align="right">13 =&nbsp;</td><td>day white fluorescent</td>
     *      </tr>
     *      <tr>
     *        <td align="right">14 =&nbsp;</td><td>cool white fluorescent</td>
     *      </tr>
     *      <tr>
     *        <td align="right">15 =&nbsp;</td><td>white fluorescent</td>
     *      </tr>
     *      <tr>
     *        <td align="right">17 =&nbsp;</td><td>standard light A</td>
     *      </tr>
     *      <tr>
     *        <td align="right">18 =&nbsp;</td><td>standard light B</td>
     *      </tr>
     *      <tr>
     *        <td align="right">19 =&nbsp;</td><td>standard light C</td>
     *      </tr>
     *      <tr>
     *        <td align="right">20 =&nbsp;</td><td>D55</td>
     *      </tr>
     *      <tr>
     *        <td align="right">21 =&nbsp;</td><td>D65</td>
     *      </tr>
     *      <tr>
     *        <td align="right">22 =&nbsp;</td><td>D75</td>
     *      </tr>
     *      <tr>
     *        <td align="right">23 =&nbsp;</td><td>D50</td>
     *      </tr>
     *      <tr>
     *        <td align="right">24 =&nbsp;</td><td>ISO studio tungsten</td>
     *      </tr>
     *      <tr>
     *        <td align="right">255 =&nbsp;</td><td>other</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int TIFF_LIGHT_SOURCE                   = 0x9208;

    /**
     * LightZone's tool stack in XML.
     * <p>
     * Type: Unsigned byte.
     */
    int TIFF_LIGHTZONE                      = 0xC6E7;

    /**
     * Manufacturer of the scanner, video digitizer, or other type of equipment
     * used to generate the image.  Synthetic images should not include this
     * field.
     * <p>
     * Type: ASCII.
     */
    int TIFF_MAKE                           = 0x010F;

    /**
     * This optional tag encodes the maximum possible aperture opening (minimum
     * lens f-number) of the camera or image capturing device, using APEX
     * (Additive System of Photographic Exposure) units.  The allowed range is
     * 0.00 to 99.99.
     * <p>
     * You can convert to F-number by using the same process as
     * {@link EXIFTags#EXIF_APERTURE_VALUE}.
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_MAX_APERTURE_VALUE             = 0x9205;

    /**
     * The maximum component value used.  This field is not to be used to
     * affect the visual appearance of an image when it is displayed or
     * printed.  Nor should this field affect the interpretation of any other
     * field; it is used only for statistical purposes.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_MAX_SAMPLE_VALUE               = 0x0119;

    /**
     * The metering mode.  The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td align="right">0 =&nbsp;</td><td>unknown</td>
     *      </tr>
     *      <tr>
     *        <td align="right">1 =&nbsp;</td><td>average</td>
     *      </tr>
     *      <tr>
     *        <td align="right">2 =&nbsp;</td><td>center-weighted average</td>
     *      </tr>
     *      <tr>
     *        <td align="right">3 =&nbsp;</td><td>spot</td>
     *      </tr>
     *      <tr>
     *        <td align="right">4 =&nbsp;</td><td>multispot</td>
     *      </tr>
     *      <tr>
     *        <td align="right">5 =&nbsp;</td><td>pattern</td>
     *      </tr>
     *      <tr>
     *        <td align="right">6 =&nbsp;</td><td>partial</td>
     *      </tr>
     *      <tr>
     *        <td align="right">255 =&nbsp;</td><td>other</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_METERING_MODE                  = 0x9207;

    /**
     * The minimum component value used.
     * <p>
     * Type: Unsigned short.
     * @see #TIFF_MAX_SAMPLE_VALUE
     */
    int TIFF_MIN_SAMPLE_VALUE               = 0x0118;

    /**
     * The model name or number of the scanner, video digitizer, or other type
     * of equipment used to generate the image.
     * <p>
     * Type: ASCII.
     */
    int TIFF_MODEL                          = 0x0110;

    /**
     * Microsoft rating: 1-5 or 0 for unrated.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_MS_RATING                      = 0x4746;

    /**
     * A general indication of the kind of data contained in this subfile.
     * Replaces the old {@link #TIFF_SUBFILE_TYPE} field, due to limitations in
     * the definition of that field.  NewSubfileType is mainly useful when
     * there are multiple subfiles in a single TIFF file.  This field is made
     * up of a set of 32 flag bits. Unused bits are expected to be 0. Bit 0 is
     * the low-order bit.  The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>bit 0&nbsp;</td>
     *        <td>
     *          is 1 if the image is a reduced-resolution version of another
     *          image in this TIFF file; else the bit is 0.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>bit 1&nbsp;</td>
     *        <td>
     *          is 1 if the image is a single page of a multi-page image (see
     *          the {@link #TIFF_PAGE_NUMBER} field description); else the bit
     *          is 0.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>bit 2&nbsp;</td>
     *        <td>
     *          is 1 if the image defines a transparency mask for another image
     *          in this TIFF file.  The {@link #TIFF_PHOTOMETRIC_INTERPRETATION}
     *          value must be 4, designating a transparency mask.
     *        </td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int TIFF_NEW_SUBFILE_TYPE               = 0x00FE;

    /**
     * The number of inks.  Usually equal to {@link #TIFF_SAMPLES_PER_PIXEL},
     * unless there are extra samples.
     * <p>
     * Type: Unsigned short.
     * @see #TIFF_EXTRA_SAMPLES
     */
    int TIFF_NUMBER_OF_INKS                 = 0x014E;

    /**
     * This gives information concerning whether this image is a low-resolution
     * proxy of a high-resolution image.
     * <p>
     * A value of 1 means that a higher-resolution version of this image
     * exists, and the name of that image is found in the
     * {@link #TIFF_IMAGE_ID} tag.
     * <p>
     * If this tag does not exist, or the value is 0, then a higher-resolution
     * version of this image does not exist.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_OPI_PROXY                      = 0x015F;

    /**
     * The orientation of the image with respect to the rows and columns.  The
     * values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>1 =&nbsp;</td>
     *        <td>
     *          The 0th row represents the visual top of the image, and the 0th
     *          column represents the visual left-hand side.
     *          (This means the image is landscape.)
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>2 =&nbsp;</td>
     *        <td>
     *          The 0th row represents the visual top of the image, and the 0th
     *          column represents the visual right-hand side.
     *          (This means the image is seascape.)
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>3 =&nbsp;</td>
     *        <td>
     *          The 0th row represents the visual bottom of the image, and the
     *          0th column represents the visual right-hand side.
     *          (This means the image is rotated 180 degrees.)
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>4 =&nbsp;</td>
     *        <td>
     *          The 0th row represents the visual bottom of the image, and the
     *          0th column represents the visual left-hand side.
     *          (This means the image is vertically flipped.)
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>5 =&nbsp;</td>
     *        <td>
     *          The 0th row represents the visual left-hand side of the image,
     *          and the 0th column represents the visual top.
     *          (This means the image is rotated 90 CCW and vertically flipped.)
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>6 =&nbsp;</td>
     *        <td>
     *          The 0th row represents the visual right-hand side of the image,
     *          and the 0th column represents the visual top.
     *          (This means the image is rotated 90 CCW.)
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>7 =&nbsp;</td>
     *        <td>
     *          The 0th row represents the visual right-hand side of the image,
     *          and the 0th column represents the visual bottom.
     *          (This means the image is rotated 90 CW and horizontally
     *          flipped.)
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>8 =&nbsp;</td>
     *        <td>
     *          The 0th row represents the visual left-hand side of the image,
     *          and the 0th column represents the visual bottom.
     *          (This means the image is rotated 90 CW.)
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>9 =&nbsp;</td>
     *        <td>unknown</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int TIFF_ORIENTATION                    = 0x0112;

    /**
     * The name of the page from which this image was scanned.
     * <p>
     * Type: ASCII.
     * @see #TIFF_DOCUMENT_NAME
     */
    int TIFF_PAGE_NAME                      = 0x011D;

    /**
     * The page number of the page from which this image was scanned.
     * This field is used to specify page numbers of a multiple page (e.g.
     * facsimile) document. PageNumber[0] is the page number; PageNumber[1] is
     * the total number of pages in the document. If PageNumber[1] is 0, the
     * total number of pages in the document is not available.
     * <p>
     * Pages need not appear in numerical order.  The first page is numbered 0
     * (zero).
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_PAGE_NUMBER                    = 0x0129;

    /**
     * Photometric interpretation.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td align="right" nowrap>0 =&nbsp;</td><td>white is zero</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right" nowrap>1 =&nbsp;</td><td>black is zero</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right" nowrap>2 =&nbsp;</td><td>RGB</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right" nowrap>3 =&nbsp;</td><td>palette color</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right" nowrap>4 =&nbsp;</td><td>transparency mask</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right" nowrap>5 =&nbsp;</td><td>seperated (CMYK)</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right" nowrap>6 =&nbsp;</td><td>YCbCr</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right" nowrap>8 =&nbsp;</td><td>CIE lab</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right" nowrap>9 =&nbsp;</td><td>ICC lab</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right" nowrap>10 =&nbsp;</td><td>ITU lab</td>
     *      </tr>
     *      <tr valign="top">
     *        <td align="right" nowrap>34892 =&nbsp;</td><td>linear raw</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int TIFF_PHOTOMETRIC_INTERPRETATION     = 0x0106;

    /**
     * Collection of Photoshop &quot;Image Resource Blocks.&quot;
     * <p>
     * Type: Unsigned byte.
     */
    int TIFF_PHOTOSHOP_IMAGE_RESOURCES      = 0x8649;

    /**
     * How the components of each pixel are stored.  The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>1 =&nbsp;</td>
     *        <td>
     *          Chunky format. The component values for each pixel are stored
     *          contiguously.  The order of the components within the pixel is
     *          specified by {@link #TIFF_PHOTOMETRIC_INTERPRETATION}.  For
     *          example, for RGB data, the data is stored as RGBRGBRGB...
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>2 =&nbsp;</td>
     *        <td>
     *          Planar format. The components are stored in separate
     *          &quot;component planes.&quot; The values in
     *          {@link #TIFF_STRIP_OFFSETS} and {@link #TIFF_STRIP_BYTE_COUNTS}
     *          are then arranged as a 2-dimensional array, with
     *          {@link #TIFF_SAMPLES_PER_PIXEL} rows and StripsPerImage
     *          columns. (All of the columns for row 0 are stored first,
     *          followed by the columns of row 1, and so on.)
     *          PhotometricInterpretation describes the type of data stored in
     *          each component plane. For example, RGB data is stored with the
     *          Red components in one component plane, the Green in another,
     *          and the Blue in another.
     *        </td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     * @see #TIFF_BITS_PER_SAMPLE
     * @see #TIFF_SAMPLES_PER_PIXEL
     */
    int TIFF_PLANAR_CONFIGURATION           = 0x011C;

    /**
     * A predictor is a mathematical operator that is applied to the image data
     * before an encoding scheme is applied.  Currently this field is used only
     * with LZW (Compression=5) encoding because LZW is probably the only TIFF
     * encoding scheme that benefits significantly from a predictor step.  The
     * values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>1 =&nbsp;</td>
     *        <td>No prediction scheme used before coding.</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>2 =&nbsp;</td>
     *        <td>Horizontal differencing.</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int TIFF_PREDICTOR                      = 0x013D;

    /**
     * The chromaticities of the primaries of the image.  This is the
     * chromaticity for each of the primaries when it has its ReferenceWhite
     * value and the other primaries have their ReferenceBlack values.  These
     * values are described using the 1931 CIE xy chromaticity diagram and only
     * the chromaticities are specified.  These values can correspond to the
     * chromaticities of the phosphors of a monitor, the filter set and light
     * source combination of a scanner or the imaging model of a rendering
     * package.  The ordering is red[x], red[y], green[x], green[y], blue[x],
     * and blue[y].
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_PRIMARY_CHROMATICITIES         = 0x013F;

    /**
     * Specifies a pair of headroom and footroom image data values (codes) for
     * each pixel component. The first component code within a pair is
     * associated with ReferenceBlack, and the second is associated with
     * ReferenceWhite.  The ordering of pairs is the same as those for pixel
     * components of the {@link #TIFF_PHOTOMETRIC_INTERPRETATION} type.
     * ReferenceBlackWhite can be applied to images with a
     * {@link #TIFF_PHOTOMETRIC_INTERPRETATION} value of RGB or YCbCr.
     * ReferenceBlackWhite is not used with other
     * {@link #TIFF_PHOTOMETRIC_INTERPRETATION} values.
     * <p>
     * Computer graphics commonly places black and white at the extremities of
     * the binary representation of image data; for example, black at code 0
     * and white at code 255.  In other disciplines, such as printing, film,
     * and video, there are practical reasons to provide footroom codes below
     * ReferenceBlack and headroom codes above ReferenceWhite.
     * <p>
     * In film applications, they correspond to the densities Dmax and Dmin.
     * In video applications, ReferenceBlack corresponds to 7.5 IRE and 0 IRE
     * in systems with and without setup respectively, and ReferenceWhite
     * corresponds to 100 IRE units.
     * <p>
     * Using YCbCr (See Section 21) and the CCIR Recommendation 601.1 video
     * standard as an example, code 16 represents ReferenceBlack, and code 235
     * represents ReferenceWhite for the luminance component (Y).  For the
     * chrominance components, Cb and Cr, code 128 represents ReferenceBlack,
     * and code 240 represents ReferenceWhite.  With Cb and Cr, the
     * ReferenceWhite value is used to code reference blue and reference red
     * respectively.
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_REFERENCE_BLACK_WHITE          = 0x0214;

    /**
     * The values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>1 =&nbsp;</td><td>none</td></tr>
     *      <tr><td>2 =&nbsp;</td><td>inch</td></tr>
     *      <tr><td>3 =&nbsp;</td><td>cm</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int TIFF_RESOLUTION_UNIT                = 0x0128;

    /**
     * IPTC (International Press Telecommunications Council) metadata.
     * <p>
     * Type: Undefined or byte (often incorrectly unsigned long).
     */
    int TIFF_RICH_TIFF_IPTC                 = 0x83BB;

    /**
     * The number of rows in each strip (except possibly the last strip.) For
     * example, if {@link #TIFF_IMAGE_LENGTH} is 24, and RowsPerStrip is 10,
     * then there are 3 strips, with 10 rows in the first strip, 10 rows in the
     * second strip, and 4 rows in the third strip. (The data in the last strip
     * is not padded with 6 extra rows of dummy data.)
     * <p>
     * Type: Unsigned short or unsigned long.
     */
    int TIFF_ROWS_PER_STRIP                 = 0x0116;

    /**
     * This field specifies how to interpret each data sample in a pixel.
     * Possible values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>1 =&nbsp;</td>
     *        <td>unsigned integer data</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>2 =&nbsp;</td>
     *        <td>two's complement signed integer data</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>3 =&nbsp;</td>
     *        <td>IEEE floating point data</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>4 =&nbsp;</td>
     *        <td>undefined data format</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Note that the SampleFormat field does not specify the size of data
     * samples; this is still done by the {@link #TIFF_BITS_PER_SAMPLE} field.
     * A field value of &quot;undefined&quot; is a statement by the writer that
     * it did not know how to interpret the data samples; for example, if it
     * were copying an existing image. A reader would typically treat an image
     * with &quot;undefined&quot; data as if the field were not present (i.e.
     * as unsigned integer data).
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_SAMPLE_FORMAT                  = 0x0153;

    /**
     * The number of components per pixel. This number is 3 for RGB images,
     * unless extra samples are present. See the {@link #TIFF_EXTRA_SAMPLES}
     * field for further information.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_SAMPLES_PER_PIXEL              = 0x0115;

    /**
     * The allowed single characters are T (= Top Secret), S (= Secret), C (=
     * Confidential), R (= Restricted), U (= Unclassified).  These definitions
     * are based on the NITF security classifications defined in the NITF
     * (National Imagery Transmission Format) specification (MIL-STD-2500).
     * <p>
     * The multi-character ASCII string tag-value can include one or more of
     * the following NITF fields.  Refer to MIL-STD- 2500 for a complete
     * description of these fields.  A NULL is inserted between each NITF field
     * and its associated value.  When multiple NITF fields are used, a NULL
     * character is inserted between each NITF field.
     * <p>
     * Type: ASCII.
     */
    int TIFF_SECURITY_CLASSIFICATION        = 0x9212;

    /**
     * This mandatory tag encodes the type of image sensor used in the camera
     * or image capturing device.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>0 =&nbsp;</td>
     *        <td>undefined</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td>
     *        <td>monochrome area sensor</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td>
     *        <td>one chip color area sensor</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td>
     *        <td>two chip color area sensor</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td>
     *        <td>three chip color area sensor</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td>
     *        <td>color sequential area sensor</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>6 =&nbsp;</td>
     *        <td>monochrome linear sensor</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>7 =&nbsp;</td>
     *        <td>trilinear sensor</td>
     *      </tr>
     *      <tr valign="top">
     *        <td>8 =&nbsp;</td>
     *        <td>color sequential linear sensor</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int TIFF_SENSING_METHOD                 = 0x9217;

    /**
     * Shutter speed.  The unit is APEX (Additive System of Photographic
     * Exposure).
     * <p>
     * Type: Signed rational.
     */
    int TIFF_SHUTTER_SPEED_VALUE            = 0x9201;

    /**
     * This new field specifies the maximum sample value.
     * <p>
     * Type: The field type that best matches the sample data.
     */
    int TIFF_SMAX_SAMPLE_VALUE              = 0x0155;

    /**
     * This field specifies the minimum sample value. Note that a value should
     * be given for each data sample. That is, if the image has 3
     * SamplesPerPixel, 3 values must be specified.
     * <p>
     * Type: The field type that best matches the sample data.
     */
    int TIFF_SMIN_SAMPLE_VALUE              = 0x0154;

    /**
     * Name and version number of the software package(s) used to create the
     * image.
     * <p>
     * Type: ASCII.
     * @see #TIFF_MAKE
     * @see #TIFF_MODEL
     */
    int TIFF_SOFTWARE                       = 0x0131;

    /**
     * Indicates the spectral sensitivity of each channel of the camera used.
     * The tag value is an ASCII string compatible with the standard developed
     * by the ASTM Technical committee.
     * <p>
     * Type: ASCII.
     */
    int TIFF_SPECTRAL_SENSITIVITY           = 0x8824;

    /**
     * For each strip, the number of bytes in that strip after any compression.
     * <p>
     * Type: Unsigned short or unsigned long.
     */
    int TIFF_STRIP_BYTE_COUNTS              = 0x0117;

    /**
     * For each strip, the byte offset of that strip.
     * <p>
     * Type: Unsigned short or unsigned long.
     */
    int TIFF_STRIP_OFFSETS                  = 0x0111;

    /**
     * A general indication of the kind of data contained in this subfile.  The
     * values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>1 =&nbsp;</td>
     *        <td>full-resolution image</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>2 =&nbsp;</td>
     *        <td>reduced resolution image</td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>3 =&nbsp;</td>
     *        <td>a single page of a multi-page image</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Note that several image types may be found in a single TIFF file, with
     * each subfile described by its own IFD.  This field is deprecated. The
     * {@link #TIFF_NEW_SUBFILE_TYPE} field should be used instead.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_SUBFILE_TYPE                   = 0x00FF;

    /**
     * Each value is an offset (from the beginning of the TIFF file, as always)
     * to a child IFD. Child images provide extra information for the parent
     * image -- such as a subsampled version of the parent image.
     * <p>
     * Type: Unsigned long or IFD.
     */
    int TIFF_SUB_IFDS                       = 0x014A;

    /**
     * The distance to the subject, given in meters.  Note that if the
     * numerator of the recorded value is FFFFFFFF.H, Infinity shall be
     * indicated; and if the numerator is 0, Distance unknown shall be
     * indicated.
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_SUBJECT_DISTANCE               = 0x9206;

    /**
     * This optional tag identifies the approximate location of the subject in
     * the scene.  The subject location may be specified as a point, a circle,
     * or a rectangle.  In all cases, the first two values provide the X column
     * number and Y row number that corresponds to the center of the subject
     * location.  If three values are given, the third value is the diameter of
     * a circle centered at the point specified by the first two values.  If
     * four values are given, the third value is the width, and the fourth
     * value is the height of a rectangle centered at the point specified by
     * the first two values.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_SUBJECT_LOCATION               = 0x9214;

    /**
     * This field is made up of a set of 32 flag bits.  Unused bits st be set
     * to 0. Bit 0 is the low-order bit.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>bit 0&nbsp;</td>
     *        <td>
     *          is 1 for 2-dimensional coding (otherwise 1-dimensional is
     *          assumed). For 2-D coding, if more than one strip is specified,
     *          each strip must begin with a 1-dimensionally coded line. That
     *          is, {@link #TIFF_ROWS_PER_STRIP} should be a multiple of
     *          &quot;Parameter K,&quot; as documented in the CCITT
     *          specification.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>bit 1&nbsp;</td>
     *        <td>
     *          is 1 if uncompressed mode is used.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>bit 2&nbsp;</td>
     *        <td>
     *          is 1 if fill bits have been added as necessary before EOL codes
     *          such that EOL always ends on a byte boundary, thus ensuring an
     *          EOL-sequence of 1 byte preceded by a zero nibble: xxxx-0000
     *          0000-0001.
     *        </td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     * @see #TIFF_COMPRESSION
     */
    int TIFF_T4_OPTIONS                     = 0x0124;

    /**
     * This field is made up of a set of 32 flag bits. Unused bits must be set
     * to 0. Bit 0 is the low-order bit.  The default value is 0 (all bits 0).
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>bit 0&nbsp;</td>
     *        <td>
     *          is unused and always 0.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>bit 1&nbsp;</td>
     *        <td>
     *          is 1 if uncompressed mode is allowed in the encoding.
     *        </td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned long.
     */
    int TIFF_T6_OPTIONS                     = 0x0125;

    /**
     * A description of the printing environment for which this separation is
     * intended.
     * <p>
     * Type: ASCII.
     */
    int TIFF_TARGET_PRINTER                 = 0x0151;

    /**
     * For black and white TIFF files that represent shades of gray, the
     * technique used to convert from gray to black and white pixels.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td nowrap>1 =&nbsp;</td>
     *        <td>
     *          No dithering or halftoning has been applied to the image data.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>2 =&nbsp;</td>
     *        <td>
     *          An ordered dither or halftone technique has been applied to the
     *          image data.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td nowrap>3 =&nbsp;</td>
     *        <td>
     *          A randomized process such as error diffusion has been applied
     *          to the image data.
     *        </td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     * @see #TIFF_CELL_LENGTH
     * @see #TIFF_CELL_WIDTH
     */
    int TIFF_THRESHHOLDING                  = 0x0107;

    /**
     * This tag encodes the version of this TIFF/EP file as a four tier
     * revision number, for example 1.0.0.0.  This revision number has the form
     * of w.x.y.z where w=0-255, x=0-255, y=0-255, and z=0-255.  The purpose of
     * this tag is to allow a TIFF/EP compliant file to identify itself to a
     * TIFF/EP aware reader.
     * <p>
     * Type: Unsigned byte.
     */
    int TIFF_TIFF_EP_STANDARD_ID            = 0x9216;

    /**
     * For each tile, the number of (compressed) bytes in that tile.
     * <p>
     * Type: Unsigned short or unsigned long.
     * @see #TIFF_TILE_OFFSETS
     * @see #TIFF_TILE_LENGTH
     * @see #TIFF_TILE_WIDTH
     */
    int TIFF_TILE_BYTE_COUNTS               = 0x0145;

    /**
     * The tile length (height) in pixels. This is the number of rows in each
     * tile.  TileLength must be a multiple of 16 for compatibility with
     * compression schemes such as JPEG.  Replaces {@link #TIFF_ROWS_PER_STRIP}
     * in tiled TIFF files.
     * <p>
     * Type: Unsigned short or unsigned long.
     * @see #TIFF_TILE_BYTE_COUNTS
     * @see #TIFF_TILE_OFFSETS
     * @see #TIFF_TILE_WIDTH
     */
    int TIFF_TILE_LENGTH                    = 0x0143;

    /**
     * For each tile, the byte offset of that tile, as compressed and stored on
     * disk. The offset is specified with respect to the beginning of the TIFF
     * file. Note that this implies that each tile has a location independent
     * of the locations of other tiles.
     * <p>
     * Offsets are ordered left-to-right and top-to-bottom.  For
     * {@link #TIFF_PLANAR_CONFIGURATION} = 2, the offsets for the first
     * component plane are stored first, followed by all the offsets for the
     * second component plane, and so on.
     * <p>
     * Type: Unsigned long.
     */
    int TIFF_TILE_OFFSETS                   = 0x0144;

    /**
     * The tile width in pixels.  This is the number of columns in each tile.
     * Assuming integer arithmetic, three computed values that are useful in
     * the following field descriptions are:
     *  <blockquote>
     *    TilesAcross = (ImageWidth + TileWidth - 1) / TileWidth<br>
     *    TilesDown = (ImageLength + TileLength - 1) / TileLength<br>
     *    TilesPerImage = TilesAcross * TilesDown
     *  </blockquote>
     * These computed values are not TIFF fields; they are simply values
     * determined by the {@link #TIFF_IMAGE_WIDTH}, TIFF_TILE_WIDTH,
     * {@link #TIFF_IMAGE_LENGTH}, and {@link #TIFF_TILE_LENGTH} fields.
     * <p>
     * TileWidth and {@link #TIFF_IMAGE_WIDTH} together determine the number of
     * tiles that span the width of the image (TilesAcross).  TileLength and
     * {@link #TIFF_IMAGE_LENGTH} together determine the number of tiles that
     * span the length of the image (TilesDown).
     * <p>
     * We recommend choosing TileWidth and TileLength such that the resulting
     * tiles are about 4K to 32K bytes before compression. This seems to be a
     * reasonable value for most applications and compression schemes.
     * <p>
     * TileWidth must be a multiple of 16. This restriction improves
     * performance in some graphics environments and enhances compatibility
     * with compression schemes such as JPEG.
     * <p>
     * Tiles need not be square.
     * <p>
     * Note that {@link #TIFF_IMAGE_WIDTH} can be less than TileWidth, although
     * this means that the tiles are too large or that you are using tiling on
     * really small images, neither of which is recommended. The same
     * observation holds for {@link #TIFF_IMAGE_LENGTH} and
     * {@link #TIFF_TILE_LENGTH}.
     * <p>
     * Type: Unsigned short or unsigned long.
     * @see #TIFF_TILE_BYTE_COUNTS
     * @see #TIFF_TILE_LENGTH
     * @see #TIFF_TILE_OFFSETS
     */
    int TIFF_TILE_WIDTH                     = 0x0142;

    /**
     * Describes a transfer function for the image in tabular style.  Pixel
     * components can be gamma-compensated, companded, non-uniformly quantized,
     * or coded in some other way.  The TransferFunction maps the pixel
     * components from a non-linear BitsPerSample (e.g. 8-bit) form into a
     * 16-bit linear form without a perceptible loss of accuracy.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_TRANSFER_FUNCTION              = 0x012D;

    /**
     * Expands the range of the TransferFunction.  The first value within a
     * pair is associated with TransferBlack and the second is associated with
     * TransferWhite.  The ordering of pairs is the same as for pixel
     * components of the {@link #TIFF_PHOTOMETRIC_INTERPRETATION} type.  By
     * default, theTransferFunction is defined over a range from a minimum
     * intensity, 0 or nominal black, to a maximum intensity, (1 <<
     * {@link #TIFF_BITS_PER_SAMPLE}) - 1 or nominal white.  Kodak PhotoYCC
     * uses an extended range TransferFunction in order to describe highlights,
     * saturated colors and shadow detail beyond this range.  The TransferRange
     * expands the TransferFunction to support these values.  It is defined
     * only for RGB and YCbCr {@link #TIFF_PHOTOMETRIC_INTERPRETATION}.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_TRANSFER_RANGE                 = 0x0156;

    /**
     * The chromaticity of the white point of the image.  This is the
     * chromaticity when each of the primaries has its ReferenceWhite value.
     * The value is described using the 1931 CIE xy chromaticity diagram and
     * only the chromaticity is specified.  This value can correspond to the
     * chromaticity of the alignment white of a monitor, the filter set and
     * light source combination of a scanner or the imaging model of a
     * rendering package.  The ordering is white[x], white[y].
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_WHITE_POINT                    = 0x013E;

    /**
     * The number of units that span the width of the image, in terms of
     * integer ClipPath coordinates.
     * <p>
     * All horizontal ClipPath coordinates will be divided by this value in
     * order to get a number that is (usually) between 0.0 and 1.0, where 0.0
     * represents the left side of the image and 1.0 represents the right side
     * of the image.
     * <p>
     * Note that the choice of value for XClipPathUnits will influence the
     * choice of DataType for the commands, since SSHORT or even SBYTE values
     * may be usable if XClipPathUnits is smaller, while SLONG will be required
     * if XClipPathUnits is larger.
     * <p>
     * Type: Unsigned long.
     */
    int TIFF_X_CLIP_PATH_UNITS              = 0x0158;

    /**
     * XMP metadata.
     * <p>
     * Type: Unsigned byte.
     */
    int TIFF_XMP_PACKET                     = 0x02BC;

    /**
     * X position of the image.  The X offset in {@link #TIFF_RESOLUTION_UNIT}s
     * of the left side of the image, with respect to the left side of the
     * page.
     * <p>
     * Type: Unsigned rational.
     * @see #TIFF_Y_POSITION
     */
    int TIFF_X_POSITION                     = 0x011E;

    /**
     * The number of pixels per {@link #TIFF_RESOLUTION_UNIT} in the
     * {@link #TIFF_IMAGE_WIDTH} (typically, horizontal - see
     * {@link #TIFF_ORIENTATION}) direction.
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_X_RESOLUTION                   = 0x011A;

    /**
     * The transformation from RGB to YC bCr image data. The transformation is
     * specified as three rational values that represent the coefficients used
     * to compute luminance, Y.
     * <p>
     * The three rational coefficient values, LumaRed, LumaGreen and LumaBlue,
     * are the proportions of red, green, and blue respectively in luminance,
     * Y.
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_YCBCR_COEFFICIENTS             = 0x0211;

    /**
     * Specifies the positioning of subsampled chrominance components relative
     * to luminance samples.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_YCBCR_POSITIONING              = 0x0213;

    /**
     * Specifies the subsampling factors used for the chrominance components of
     * a YCbCr image. The two fields of this field, YCbCrSubsampleHoriz and
     * YCbCrSubsampleVert, specify the horizontal and vertical subsampling
     * factors respectively.
     * <p>
     * Type: Unsigned short.
     */
    int TIFF_YCBCR_SUBSAMPLING              = 0x0212;

    /**
     * The number of units that span the height of the image, in terms of
     * integer ClipPath coordinates.
     * <p>
     * All vertical ClipPath coordinates will be divided by this value in order
     * to get a number that is (usually) between 0.0 and 1.0, where 0.0
     * represents the top of the image and 1.0 represents the bottom of the
     * image.
     * <p>
     * Use this if you want to be able to specify your ClipPath coordinates
     * using integer values that match the aspect ratio of an image.
     * <p>
     * Type: Unisgned long.
     */
    int TIFF_Y_CLIP_PATH_UNITS              = 0x0159;

    /**
     * The Y offset in {@link #TIFF_RESOLUTION_UNIT}s of the top of the image,
     * with respect to the top of e page. In the TIFF coordinate scheme, the
     * positive Y direction is down, so that osition is always positive.
     * <p>
     * Type: Unsigned rational.
     * @see #TIFF_X_POSITION
     */
    int TIFF_Y_POSITION                     = 0x011F;

    /**
     * The number of pixels per {@link #TIFF_RESOLUTION_UNIT} in the
     * {@link #TIFF_IMAGE_LENGTH} (typically, vertical) direction.
     * <p>
     * Type: Unsigned rational.
     */
    int TIFF_Y_RESOLUTION                   = 0x011B;

}
/* vim:set et sw=4 ts=4: */
