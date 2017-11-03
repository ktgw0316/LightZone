/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

/**
 * A <code>DNGTags</code> defines the constants used for DNG metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface DNGTags extends TIFFTags {

    /**
     * This rectangle defines the active (non-masked) pixels of the sensor. The
     * order of the rectangle coordinates is: top, left, bottom, right.
     * <p>
     * Type: Unsigned short or unsigned long.
     */
    int DNG_ACTIVE_AREA                = 0xC68D;

    /**
     * Normally the stored raw values are not white balanced, since any digital
     * white balancing will reduce the dynamic range of the final image if the
     * user decides to later adjust the white balance; however, if camera
     * hardware is capable of white balancing the color channels before the
     * signal is digitized, it can improve the dynamic range of the final
     * image.
     * <p>
     * AnalogBalance defines the gain, either analog (recommended) or digital
     * (not recommended) that has been applied the stored raw values.
     * <p>
     * Type: Unsigned rational.
     */
    int DNG_ANALOG_BALANCE             = 0xC627;

    /**
     * AntiAliasStrength provides a hint to the DNG reader about how strong the
     * camera's anti-alias filter is. A value of 0.0 means no anti-alias filter
     * (i.e., the camera is prone to aliasing artifacts with some subjects),
     * while a value of 1.0 means a strong anti-alias filter (i.e., the camera
     * almost never has aliasing artifacts).
     * <p>
     * Type: Unsigned rational.
     */
    int DNG_ANTI_ALIAS_STRENGTH        = 0xC632;

    /**
     * This tag contains an ICC profile that, in conjunction with the
     * {@link #DNG_AS_SHOT_PRE_PROFILE_MATRIX} tag, provides the camera
     * manufacturer with a way to specify a default color rendering from camera
     * color space coordinates (linear reference values) into the ICC profile
     * connection space.
     * <p>
     * Type: Undefined.
     */
    int DNG_AS_SHOT_ICC_PROFILE        = 0xC68F;

    /**
     * AsShotNeutral specifies the selected white balance at time of capture,
     * encoded as the coordinates of a perfectly neutral color in linear
     * reference space values. The inclusion of this tag precludes the
     * inclusion of the {@link #DNG_AS_SHOT_WHITE_XY} tag.
     * <p>
     * Type: Unsigned short or unsigned rational.
     */
    int DNG_AS_SHOT_NEUTRAL            = 0xC628;

    /**
     * This tag is used in conjunction with the AsShotICCProfile tag.  It
     * specifies a matrix that should be applied to the camera color space
     * coordinates before processing the values through the ICC profile
     * specified in the {@link #DNG_AS_SHOT_ICC_PROFILE} tag.
     * <p>
     * The matrix is stored in the row scan order.  If ColorPlanes is greater
     * than three, then this matrix can (but is not required to) reduce the
     * dimensionality of the color data down to three components, in which case
     * the {@link #DNG_AS_SHOT_ICC_PROFILE} should have three rather than
     * ColorPlanes input components.
     * <p>
     * Type: Signed rational.
     */
    int DNG_AS_SHOT_PRE_PROFILE_MATRIX = 0xC690;

    /**
     * AsShotWhiteXY specifies the selected white balance at time of capture,
     * encoded as x-y chromaticity coordinates. The inclusion of this tag
     * precludes the inclusion of the {@link #DNG_AS_SHOT_NEUTRAL} tag.
     * <p>
     * Type: Unsigned rational.
     */
    int DNG_AS_SHOT_WHITE_XY           = 0xC629;

    /**
     * This tag specifies the oldest version of the Digital Negative
     * specification for which a file is compatible. Readers should not attempt
     * to read a file if this tag specifies a version number that is higher
     * than the version number of the specification the reader was based on.
     * <p>
     * In addition to checking the version tags, readers should, for all tags,
     * check the types, counts, and values, to verify it is able to correctly
     * read the file.
     * <p>
     * Type: Unsigned yte.
     */
    int DNG_BACKWARD_VERSION           = 0xC613;

    /**
     * Camera models vary in the trade-off they make between highlight headroom
     * and shadow noise. Some leave a significant amount of highlight headroom
     * during a normal exposure. This allows significant negative exposure
     * compensation to be applied during raw conversion, but also means normal
     * exposures will contain more shadow noise. Other models leave less
     * headroom during normal exposures. This allows for less negative exposure
     * compensation, but results in lower shadow noise for normal exposures.
     * <p>
     * Because of these differences, a raw converter needs to vary the zero
     * point of its exposure compensation control from model to model.
     * BaselineExposure specifies by how much (in EV units) to move the zero
     * point. Positive values result in brighter default results, while
     * negative values result in darker default results.
     * <p>
     * Type: Signed rational.
     */
    int DNG_BASELINE_EXPOSURE          = 0xC62A;

    /**
     * BaselineNoise specifies the relative noise level of the camera model at
     * a baseline ISO value of 100, compared to a reference camera model.
     * <p>
     * Since noise levels tend to vary approximately with the square root of
     * the ISO value, a raw converter can use this value, combined with the
     * current ISO, to estimate the relative noise level of the current image.
     * <p>
     * Type: Unsigned rational.
     */
    int DNG_BASELINE_NOISE             = 0xC62B;

    /**
     * BaselineSharpness specifies the relative amount of sharpening required
     * for this camera model, compared to a reference camera model. Camera
     * models vary in the strengths of their anti-aliasing filters. Cameras
     * with weak or no filters require less sharpening than cameras with strong
     * anti-aliasing filters.
     * <p>
     * Type: Unsigned rational.
     */
    int DNG_BASELINE_SHARPNESS         = 0xC62C;

    /**
     * BayerGreenSplit only applies to CFA images using a Bayer pattern filter
     * array. This tag specifies, in arbitrary units, how closely the values of
     * the green pixels in the blue/green rows track the values of the green
     * pixels in the red/green rows.
     * <p>
     * A value of zero means the two kinds of green pixels track closely, while
     * a non-zero value means they sometimes diverge. The useful range for this
     * tag is from 0 (no divergence) to about 5000 (quite large divergence).
     * <p>
     * Type: Unsigned long.
     */
    int DNG_BAYER_GREEN_SPLIT          = 0xC62D;

    /**
     * For some cameras, the best possible image quality is not achieved by
     * preserving the total pixel count during conversion. For example,
     * Fujifilm SuperCCD images have maximum detail when their total pixel
     * count is doubled.
     * <p>
     * This tag specifies the amount by which the values of the DefaultScale
     * tag need to be multiplied to achieve the best quality image size.
     * <p>
     * Type: Unsigned rational.
     */
    int DNG_BEST_QUALITY_SCALE         = 0xC65C;

    /**
     * This tag specifies the zero light (a.k.a. thermal black or black
     * current) encoding level, as a repeating pattern. The origin of this
     * pattern is the top-left corner of the {@link #DNG_ACTIVE_AREA}
     * rectangle.  The values are stored in row-column-sample scan order.
     * <p>
     * Type: Unsigned short, unsigned long, or unsigned rational.
     */
    int DNG_BLACK_LEVEL                = 0xC61A;

    /**
     * If the zero light encoding level is a function of the image column,
     * BlackLevelDeltaH specifies the difference between the zero light
     * encoding level for each column and the baseline zero light encoding
     * level.
     * <p>
     * If {@link #DNG_SAMPLES_PER_PIXEL} is not equal to one, this single table
     * applies to all the samples for each xel.
     * <p>
     * Type: Signed rational.
     */
    int DNG_BLACK_LEVEL_DELTA_H        = 0xC61B;

    /**
     * If the zero light encoding level is a function of the image row, this
     * tag specifies the difference between the zero light encoding level for
     * each row and the baseline zero light encoding level.
     * <p>
     * If {@link #DNG_SAMPLES_PER_PIXEL} is not equal to one, this single table
     * applies to all the samples for each pixel.
     * <p>
     * Type: Signed rational.
     */
    int DNG_BLACK_LEVEL_DELTA_V        = 0xC61C;

    /**
     * This tag specifies repeat pattern size for the {@link #DNG_BLACK_LEVEL}
     * tag.
     * <p>
     * Type: Unsigned short.
     */
    int DNG_BLACK_LEVEL_REPEAT_DIM     = 0xC619;

    /**
     * The illuminant used for the first set of color calibration tags
     * ({@link #DNG_COLOR_MATRIX_1}, {@link #DNG_CAMERA_CALIBRATION_1},
     * {@link #DNG_REDUCTION_MATRIX_1}). The legal values
     * for this tag are the same as the legal values for the LightSource EXIF
     * tag.
     * <p>
     * Type: Unsigned short.
     */
    int DNG_CALIBRATION_ILLUMINANT_1   = 0xC65A;

    /**
     * The illuminant used for the second set of color calibration tags
     * ({@link #DNG_COLOR_MATRIX_2}, {@link #DNG_CAMERA_CALIBRATION_2},
     * {@link #DNG_REDUCTION_MATRIX_2}). The legal values
     * for this tag are the same as the legal values for the
     * {@link #DNG_CALIBRATION_ILLUMINANT_1} tag; however, if both are
     * included, neither is allowed to have a value of 0 (unknown).
     * <p>
     * Type: Unsigned short.
     */
    int DNG_CALIBRATION_ILLUMINANT_2   = 0xC65B;

    /**
     * CameraClalibration1 defines a calibration matrix that transforms
     * reference camera native space values to individual camera native space
     * values under the first calibration illuminant.  The matrix is stored in
     * row scan order.
     * <p>
     * This matrix is stored separately from the matrix specified by the
     * {@link #DNG_COLOR_MATRIX_1} tag to allow raw converters to swap in
     * replacement color matrices based on {@link #DNG_UNIQUE_CAMERA_MODEL}
     * tag, while still taking advantage of any per-individual camera
     * calibration performed by the camera manufacturer.
     * <p>
     * Type: Signed rational.
     */
    int DNG_CAMERA_CALIBRATION_1       = 0xC623;

    /**
     * CameraClalibration2 defines a calibration matrix that transforms
     * reference camera native space values to individual camera native space
     * values under the first calibration illuminant.  The matrix is stored in
     * row scan order.
     * <p>
     * This matrix is stored separately from the matrix specified by the
     * {@link #DNG_COLOR_MATRIX_2} tag to allow raw converters to swap in
     * replacement color matrices based on {@link #DNG_UNIQUE_CAMERA_MODEL}
     * tag, while still taking advantage of any per-individual camera
     * calibration performed by the camera manufacturer.
     * <p>
     * Type: Signed rational.
     */
    int DNG_CAMERA_CALIBRATION_2       = 0xC624;

    /**
     * CameraSerialNumber contains the serial number of the camera or camera
     * body that captured the image.
     * <p>
     * Type: ASCII.
     */
    int DNG_CAMERA_SERIAL_NUMBER       = 0xC62F;

    /**
     * CFALayout describes the spatial layout of the CFA. The currently defined
     * values are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>1 =&nbsp;</td>
     *        <td>
     *          Rectangular (or square) layout.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td>2 =&nbsp;</td>
     *        <td>
     *          Staggered layout A: even columns are offset down by 1/2 row.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td>3 =&nbsp;</td>
     *        <td>
     *          Staggered layout B: even columns are offset up by 1/2 row.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td>4 =&nbsp;</td>
     *        <td>
     *          Staggered layout C: even rows are offset right by 1/2 column.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td>5 =&nbsp;</td>
     *        <td>
     *          Staggered layout D: even rows are offset left by 1/2 column.
     *        </td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int DNG_CFA_LAYOUT                 = 0xC617;

    /**
     * CFAPlaneColor provides a mapping between the values in the CFAPattern
     * tag and the plane numbers in LinearRaw space. This is a required tag for
     * non-RGB CFA images.
     * <p>
     * Type: Unsigned byte.
     */
    int DNG_CFA_PLANE_COLOR            = 0xC616;

    /**
     * ChromaBlurRadius provides a hint to the DNG reader about how much chroma
     * blur should be applied to the image. If this tag is omitted, the reader
     * will use its default amount of chroma blurring.
     * <p>
     * Normally this tag is only included for non-CFA images, since the amount
     * of chroma blur required for mosaic images is highly dependent on the
     * de-mosaic algorithm, in which case the DNG reader's default value is
     * likely optimized for its particular de-mosaic algorithm.
     * <p>
     * Type: Unsigned rational.
     */
    int DNG_CHROMA_BLUR_RADIUS         = 0xC631;

    /**
     * ColorMatrix1 defines a transformation matrix that converts XYZ values to
     * reference camera native color space values, under the first calibration
     * illuminant. The matrix values are stored in row scan order.
     * <p>
     * The ColorMatrix1 tag is required for all non-monochrome DNG files.
     * <p>
     * Type: Signed rational.
     */
    int DNG_COLOR_MATRIX_1             = 0xC621;

    /**
     * ColorMatrix2 defines a transformation matrix that converts XYZ values to
     * reference camera native color space values, under the second calibration
     * illuminant. The matrix values are stored in row scan order.
     * <p>
     * Type: Signed rational.
     */
    int DNG_COLOR_MATRIX_2             = 0xC622;

    /**
     * This tag is used in conjunction with the
     * {@link #DNG_CURRENT_PRE_PROFILE_MATRIX} tag.
     * <p>
     * The CurrentICCProfile and {@link #DNG_CURRENT_PRE_PROFILE_MATRIX} tags
     * have the same purpose and usage as the {@link #DNG_AS_SHOT_ICC_PROFILE}
     * and {@link #DNG_AS_SHOT_PRE_PROFILE_MATRIX} tag pair, except they are
     * for use by raw file editors rather than camera manufacturers.
     * <p>
     * Type: Undefined.
     */
    int DNG_CURRENT_ICC_PROFILE        = 0xC691;

    /**
     * This tag is used in conjunction with the
     * {@link #DNG_CURRENT_ICC_PROFILE} tag.
     * <p>
     * The {@link #DNG_CURRENT_ICC_PROFILE} and CurrentPreProfileMatrix tags
     * have the same purpose and usage as the {@link #DNG_AS_SHOT_ICC_PROFILE}
     * and {@link #DNG_AS_SHOT_PRE_PROFILE_MATRIX} tag pair, except they are
     * for use by raw file editors rather than camera manufacturers.
     * <p>
     * Type: Signed rational.
     */
    int DNG_CURRENT_PRE_PROFILE_MATRIX = 0xC692;

    /**
     * Raw images often store extra pixels around the edges of the final image.
     * These extra pixels help prevent interpolation artifacts near the edges
     * of the final image.
     * <p>
     * DefaultCropOrigin specifies the origin of the final image area, in raw
     * image coordinates (i.e., before the {@link #DNG_DEFAULT_SCALE} has been
     * applied), relative to the top-left corner of the ActiveArea rectangle.
     * <p>
     * Type: Unsigned short, unsigned long, or unsigned rational.
     */
    int DNG_DEFAULT_CROP_ORIGIN        = 0xC61F;

    /**
     * Raw images often store extra pixels around the edges of the final image.
     * These extra pixels help prevent interpolation artifacts near the edges
     * of the final image.
     * <p>
     * Type: Unsigned short, unsigned long, or unsigned rational.
     */
    int DNG_DEFAULT_CROP_SIZE          = 0xC620;

    /**
     * DefaultScale is required for cameras with non-square pixels. It
     * specifies the default scale factors for each direction to convert the
     * image to square pixels. Typically these factors are selected to
     * approximately preserve total pixel count.
     * <p>
     * For CFA images that use {@link #DNG_CFA_LAYOUT} equal to 2, 3, 4, or 5,
     * such as the Fujifilm SuperCCD, these two values should usually differ by
     * a factor of 2.0.
     * <p>
     * Type: Unsigned rational.
     */
    int DNG_DEFAULT_SCALE              = 0xC61E;

    /**
     * LensInfo contains information about the lens that captured the image. If
     * the minimum f-stops are unknown, they should be encoded as 0/0.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr valign="top">
     *        <td>Value 0:&nbsp;</td>
     *        <td>
     *          Minimum focal length in mm.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td>Value 1:&nbsp;</td>
     *        <td>
     *          Maximum focal length in mm.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td>Value 2:&nbsp;</td>
     *        <td>
     *          Minimum (maximum aperture) f-stop at minimum focal length.
     *        </td>
     *      </tr>
     *      <tr valign="top">
     *        <td>Value 3:&nbsp;</td>
     *        <td>
     *          Minimum (maximum aperture) f-stop at maximum focal length.
     *        </td>
     *      </tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned rational.
     */
    int DNG_LENS_INFO                  = 0xC630;

    /**
     * LinearizationTable describes a lookup table that maps stored values into
     * linear values. This tag is typically used to increase compression ratios
     * by storing the raw data in a non-linear, more visually uniform space
     * with fewer total encoding levels.
     * <p>
     * If {@link #DNG_SAMPLES_PER_PIXEL} is not equal to one, this single table
     * applies to all the samples for each xel.
     * <p>
     * Type: Unsigned short.
     */
    int DNG_LINEARIZATION_TABLE        = 0xC618;

    /**
     * Some sensors have an unpredictable non-linearity in their response as
     * they near the upper limit of their encoding range. This non-linearity
     * results in color shifts in the highlight areas of the resulting image
     * unless the raw converter compensates for this effect.
     * <p>
     * LinearResponseLimit specifies the fraction of the encoding range above
     * which the response may become significantly non-linear.
     * <p>
     * Type: Unsigned rational.
     */
    int DNG_LINEAR_RESPONSE_LIMIT      = 0xC62E;

    /**
     * Similar to the {@link #DNG_UNIQUE_CAMERA_MODEL} field, except the name
     * can be localized for different markets to match the localization of the
     * camera name.
     * <p>
     * Type: ASCII or unsigned byte.
     */
    int DNG_LOCALIZED_CAMERA_MODEL     = 0xC615;

    /**
     * MakerNoteSafety lets the DNG reader know whether the EXIF MakerNote tag
     * is safe to preserve along with the rest of the EXIF data. File browsers
     * and other image management software processing an image with a preserved
     * MakerNote should be aware that any thumbnail image embedded in the
     * MakerNote may be stale, and may not reflect the current state of the
     * full size image.
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr><td>0 =&nbsp;</td><td>unsafe</td></tr>
     *      <tr><td>1 =&nbsp;</td><td>safe</td></tr>
     *    </table>
     *  </blockquote>
     * Type: Unsigned short.
     */
    int DNG_MAKER_NOTE_SAFETY          = 0xC635;

    /**
     * This tag contains a list of non-overlapping rectangle coordinates of
     * fully masked pixels, which can be optionally used by DNG readers to
     * measure the black encoding level.
     * <p>
     * The order of each rectangle's coordinates is: top, left, bottom, right.
     * <p>
     * If the raw image data has already had its black encoding level
     * subtracted, then this tag should not be used, since the masked pixels
     * are no longer useful.
     * <p>
     * Note that DNG writers are still required to include estimate and store
     * the black encoding level using the black level DNG tags. Support for the
     * MaskedAreas tag is not required of DNG readers.
     * <p>
     * Type: Unsigned short or unsigned long.
     */
    int DNG_MASKED_AREAS               = 0xC68E;

    /**
     * If the DNG file was converted from a non-DNG raw file, then this tag
     * contains the compressed contents of that original raw file.
     * <p>
     * The contents of this tag always use the big-endian byte order.
     * <p>
     * The tag contains a sequence of data blocks. Future versions of the DNG
     * specification may define additional data blocks, so DNG readers should
     * ignore extra bytes when parsing this tag.  DNG readers should also
     * detect the case where data blocks are missing from the end of the
     * sequence, and should assume a default value for all the missing blocks.
     * <p>
     * Type: Undefined.
     */
    int DNG_ORIGINAL_RAW_FILE_DATA     = 0xC68C;

    /**
     * If the DNG file was converted from a non-DNG raw file, then this tag
     * contains the file name of that original raw file.
     * <p>
     * Type: ASCII or unsigned byte.
     */
    int DNG_ORIGINAL_RAW_FILE_NAME     = 0xC68B;

    /**
     * DNGPrivateData provides a way for camera manufacturers to store private
     * data in the DNG file for use by their own raw converters, and to have
     * that data preserved by programs that edit DNG files.
     * <p>
     * Type: Unsigned byte.
     */
    int DNG_PRIVATE_DATA               = 0xC634;

    /**
     * This tag contains a 16-byte unique identifier for the raw image data in
     * the DNG file. DNG readers can use this tag to recognize a particular raw
     * image, even if the file's name or the metadata contained in the file has
     * been changed.
     * <p>
     * If a DNG writer creates such an identifier, it should do so using an
     * algorithm that will ensure that it is very unlikely two different images
     * will end up having the same identifier.
     * <p>
     * Type: Unsigned byte.
     */
    int DNG_RAW_DATA_UNIQUE_ID         = 0xC65D;

    /**
     * ReductionMatrix1 defines a dimensionality reduction matrix for use as
     * the first stage in converting color camera native space values to XYZ
     * values, under the first calibration illuminant. This tag may only be
     * used if ColorPlanes is greater than 3. The matrix is stored in row scan
     * order.
     * <p>
     * Type: Signed rational.
     */
    int DNG_REDUCTION_MATRIX_1         = 0xC625;

    /**
     * ReductionMatrix2 defines a dimensionality reduction matrix for use as
     * the first stage in converting color camera native space values to XYZ
     * values, under the second calibration illuminant. This tag may only be
     * used if ColorPlanes is greater than 3. The matrix is stored in row scan
     * order.
     * <p>
     * Type: Signed rational.
     */
    int DNG_REDUCTION_MATRIX_2         = 0xC626;

    /**
     * This tag is used by Adobe Camera Raw to control the sensitivity of its
     * "Shadows" slider.
     * <p>
     * Type: Unsigned rational.
     */
    int DNG_SHADOW_SCALE               = 0xC633;

    /**
     * UniqueCameraModel defines a unique, non-localized name for the camera
     * model that created the image in the raw file. This name should include
     * the manufacturer's name to avoid conflicts, and should not be localized,
     * even if the camera name itself is localized for different markets (see
     * {@link #DNG_LOCALIZED_CAMERA_MODEL}).
     * <p>
     * This string may be used by reader software to index into per-model
     * preferences and replacement profiles.
     * <p>
     * Type: ASCII.
     */
    int DNG_UNIQUE_CAMERA_MODEL        = 0xC614;

    /**
     * This tag encodes the DNG four-tier version number. For files compliant
     * with this version of the DNG specification (1.1.0.0), this tag should
     * contain the bytes: 1, 1, 0, 0.
     * <p>
     * Type: Unsigned byte.
     */
    int DNG_VERSION                    = 0xC612;

    /**
     * This tag specifies the fully saturated encoding level for the raw sample
     * values. Saturation is caused either by the sensor itself becoming highly
     * non-linear in response, or by the camera's analog to digital converter
     * clipping.
     * <p>
     * Type: Unsigned short or unsigned long.
     */
    int DNG_WHITE_LEVEL                = 0xC61D;

    ////////// Relabled from TIFF /////////////////////////////////////////////

    int DNG_ARTIST                         = TIFF_ARTIST;
    int DNG_BITS_PER_SAMPLE                = TIFF_BITS_PER_SAMPLE;
    int DNG_CELL_LENGTH                    = TIFF_CELL_LENGTH;
    int DNG_CELL_WIDTH                     = TIFF_CELL_WIDTH;
    int DNG_COLOR_MAP                      = TIFF_COLOR_MAP;
    int DNG_COMPRESSION                    = TIFF_COMPRESSION;
    int DNG_COPYRIGHT                      = TIFF_COPYRIGHT;
    int DNG_DATE_TIME                      = TIFF_DATE_TIME;
    int DNG_DOCUMENT_NAME                  = TIFF_DOCUMENT_NAME;
    int DNG_DOT_RANGE                      = TIFF_DOT_RANGE;
    int DNG_EXIF_IFD_POINTER               = TIFF_EXIF_IFD_POINTER;
    int DNG_EXTRA_SAMPLES                  = TIFF_EXTRA_SAMPLES;
    int DNG_FILL_ORDER                     = TIFF_FILL_ORDER;
    int DNG_FREE_BYTE_COUNTS               = TIFF_FREE_BYTE_COUNTS;
    int DNG_FREE_OFFSETS                   = TIFF_FREE_OFFSETS;
    int DNG_GRAY_RESPONSE_CURVE            = TIFF_GRAY_RESPONSE_CURVE;
    int DNG_GRAY_RESPONSE_UNIT             = TIFF_GRAY_RESPONSE_UNIT;
    int DNG_HALFTONE_HINTS                 = TIFF_HALFTONE_HINTS;
    int DNG_HOST_COMPUTER                  = TIFF_HOST_COMPUTER;
    int DNG_ICC_PROFILE                    = TIFF_ICC_PROFILE;
    int DNG_IMAGE_DESCRIPTION              = TIFF_IMAGE_DESCRIPTION;
    int DNG_IMAGE_ID                       = TIFF_IMAGE_ID;
    int DNG_IMAGE_LENGTH                   = TIFF_IMAGE_LENGTH;
    int DNG_IMAGE_WIDTH                    = TIFF_IMAGE_WIDTH;
    int DNG_INDEXED                        = TIFF_INDEXED;
    int DNG_INK_NAMES                      = TIFF_INK_NAMES;
    int DNG_INK_SET                        = TIFF_INK_SET;
    int DNG_JPEG_AC_TABLES                 = TIFF_JPEG_AC_TABLES;
    int DNG_JPEG_DC_TABLES                 = TIFF_JPEG_DC_TABLES;
    int DNG_JPEG_INTERCHANGE_FORMAT        = TIFF_JPEG_INTERCHANGE_FORMAT;
    int DNG_JPEG_INTERCHANGE_FORMAT_LENGTH = TIFF_JPEG_INTERCHANGE_FORMAT_LENGTH;
    int DNG_JPEG_LOSSLESS_PREDICTORS       = TIFF_JPEG_LOSSLESS_PREDICTORS;
    int DNG_JPEG_POINT_TRANSFORMS          = TIFF_JPEG_POINT_TRANSFORMS;
    int DNG_JPEG_PROC                      = TIFF_JPEG_PROC;
    int DNG_JPEG_Q_TABLES                  = TIFF_JPEG_Q_TABLES;
    int DNG_JPEG_RESTART_INTERVAL          = TIFF_JPEG_RESTART_INTERVAL;
    int DNG_MAKE                           = TIFF_MAKE;
    int DNG_MAX_SAMPLE_VALUE               = TIFF_MAX_SAMPLE_VALUE;
    int DNG_MIN_SAMPLE_VALUE               = TIFF_MIN_SAMPLE_VALUE;
    int DNG_MODEL                          = TIFF_MODEL;
    int DNG_NEW_SUBFILE_TYPE               = TIFF_NEW_SUBFILE_TYPE;
    int DNG_NUMBER_OF_INKS                 = TIFF_NUMBER_OF_INKS;
    int DNG_OPI_PROXY                      = TIFF_OPI_PROXY;
    int DNG_ORIENTATION                    = TIFF_ORIENTATION;
    int DNG_PAGE_NAME                      = TIFF_PAGE_NAME;
    int DNG_PAGE_NUMBER                    = TIFF_PAGE_NUMBER;
    int DNG_PHOTOMETRIC_INTERPRETATION     = TIFF_PHOTOMETRIC_INTERPRETATION;
    int DNG_PLANAR_CONFIGURATION           = TIFF_PLANAR_CONFIGURATION;
    int DNG_PREDICTOR                      = TIFF_PREDICTOR;
    int DNG_PRIMARY_CHROMATICITIES         = TIFF_PRIMARY_CHROMATICITIES;
    int DNG_REFERENCE_BLACK_WHITE          = TIFF_REFERENCE_BLACK_WHITE;
    int DNG_RESOLUTION_UNIT                = TIFF_RESOLUTION_UNIT;
    int DNG_RICH_TIFF_IPTC                 = TIFF_RICH_TIFF_IPTC;
    int DNG_ROWS_PER_STRIP                 = TIFF_ROWS_PER_STRIP;
    int DNG_SAMPLE_FORMAT                  = TIFF_SAMPLE_FORMAT;
    int DNG_SAMPLES_PER_PIXEL              = TIFF_SAMPLES_PER_PIXEL;
    int DNG_SMAX_SAMPLE_VALUE              = TIFF_SMAX_SAMPLE_VALUE;
    int DNG_SMIN_SAMPLE_VALUE              = TIFF_SMIN_SAMPLE_VALUE;
    int DNG_SOFTWARE                       = TIFF_SOFTWARE;
    int DNG_STRIP_BYTE_COUNTS              = TIFF_STRIP_BYTE_COUNTS;
    int DNG_STRIP_OFFSETS                  = TIFF_STRIP_OFFSETS;
    int DNG_SUBFILE_TYPE                   = TIFF_SUBFILE_TYPE;
    int DNG_T4_OPTIONS                     = TIFF_T4_OPTIONS;
    int DNG_T6_OPTIONS                     = TIFF_T6_OPTIONS;
    int DNG_TARGET_PRINTER                 = TIFF_TARGET_PRINTER;
    int DNG_THRESHHOLDING                  = TIFF_THRESHHOLDING;
    int DNG_TILE_BYTE_COUNTS               = TIFF_TILE_BYTE_COUNTS;
    int DNG_TILE_LENGTH                    = TIFF_TILE_LENGTH;
    int DNG_TILE_OFFSETS                   = TIFF_TILE_OFFSETS;
    int DNG_TILE_WIDTH                     = TIFF_TILE_WIDTH;
    int DNG_TRANSFER_FUNCTION              = TIFF_TRANSFER_FUNCTION;
    int DNG_TRANSFER_RANGE                 = TIFF_TRANSFER_RANGE;
    int DNG_WHITE_POINT                    = TIFF_WHITE_POINT;
    int DNG_X_CLIP_PATH_UNITS              = TIFF_X_CLIP_PATH_UNITS;
    int DNG_XMP_PACKET                     = TIFF_XMP_PACKET;
    int DNG_X_POSITION                     = TIFF_X_POSITION;
    int DNG_X_RESOLUTION                   = TIFF_X_RESOLUTION;
    int DNG_YCBCR_COEFFICIENTS             = TIFF_YCBCR_COEFFICIENTS;
    int DNG_YCBCR_POSITIONING              = TIFF_YCBCR_POSITIONING;
    int DNG_YCBCR_SUBSAMPLING              = TIFF_YCBCR_SUBSAMPLING;
    int DNG_Y_CLIP_PATH_UNITS              = TIFF_Y_CLIP_PATH_UNITS;
    int DNG_Y_POSITION                     = TIFF_Y_POSITION;
    int DNG_Y_RESOLUTION                   = TIFF_Y_RESOLUTION;
}
/* vim:set et sw=4 ts=4: */
