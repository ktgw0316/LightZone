/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import com.lightcrafts.image.metadata.TIFFTags;

/**
 * A <code>TIFFConstants</code> defines some constants for TIFF metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface TIFFConstants {

    /**
     * The byte sequence used to encode that TIFF metadata is in big-endian
     * byte order: 0x4D4D = &quot;MM&quot; = Motorola.
     * @see #TIFF_LITTLE_ENDIAN
     */
    short TIFF_BIG_ENDIAN = 0x4D4D;

    /**
     * TIFF compression: none.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_COMPRESSION} metadata tag.
     */
    short TIFF_COMPRESSION_NONE             = 1;

    /**
     * TIFF compression: CCITT modified Huffman run length encoding.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_COMPRESSION} metadata tag.
     */
    short TIFF_COMPRESSION_HUFFMAN_RLE      = 2;

    /**
     * TIFF compression: CCITT group 3 fax.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_COMPRESSION} metadata tag.
     */
    short TIFF_COMPRESSION_GROUP_3_FAX      = 3;

    /**
     * TIFF compression: CCITT group 4 fax.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_COMPRESSION} metadata tag.
     */
    short TIFF_COMPRESSION_GROUP_4_FAX      = 4;

    /**
     * TIFF compression: LZW.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_COMPRESSION} metadata tag.
     */
    short TIFF_COMPRESSION_LZW              = 5;

    /**
     * TIFF compression: old-style JPEG compression (not supported).
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_COMPRESSION} metadata tag.
     */
    short TIFF_COMPRESSION_JPEG_OLDSTYLE    = 6;

    /**
     * TIFF compression: JPEG.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_COMPRESSION} metadata tag.
     */
    short TIFF_COMPRESSION_JPEG             = 7;

    /**
     * TIFF compression: deflate.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_COMPRESSION} metadata tag.
     */
    short TIFF_COMPRESSION_DEFLATE          = 8;

    /**
     * TIFF compression: JBIG black-and-white.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_COMPRESSION} metadata tag.
     */
    short TIFF_COMPRESSION_JBIG_BW          = 9;

    /**
     * TIFF compression: JBIG color.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_COMPRESSION} metadata tag.
     */
    short TIFF_COMPRESSION_JBIG_COLOR       = 10;

    /**
     * TIFF extra samples: unspecified.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_EXTRA_SAMPLES} metadata tag.
     */
    int TIFF_EXTRA_SAMPLES_UNSPECIFIED      = 0;

    /**
     * TIFF extra samples: associated alpha data.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_EXTRA_SAMPLES} metadata tag.
     */
    int TIFF_EXTRA_SAMPLES_ASSOC_ALPHA  = 1;

    /**
     * TIFF extra samples: unassociated alpha data.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_EXTRA_SAMPLES} metadata tag.
     */
    int TIFF_EXTRA_SAMPLES_UNASSOS_ALPHA    = 2;

    /**
     * The size (in bytes) of each metadata field type.
     */
    int[] TIFF_FIELD_SIZE = {
        0,  //  0: not used
        1,  //  1: UBYTE
        1,  //  2: ASCII
        2,  //  3: USHORT
        4,  //  4: ULONG
        8,  //  5: URATIONAL
        1,  //  6: SBYTE
        1,  //  7: UNDEFINED
        2,  //  8: SSHORT
        4,  //  9: SLONG
        8,  // 10: SRATIONAL
        4,  // 11: FLOAT
        8,  // 12: DOUBLE
        4,  // 13: IFD
        2,  // 14: UNICODE
        8,  // 15: COMPLEX
        8,  // 16: UINT64
        8,  // 17: SINT64
        8,  // 18: IFD64
    };

    /**
     * TIFF unsigned byte (8 bits).
     */
    byte TIFF_FIELD_TYPE_UBYTE     = 1;

    /**
     * TIFF ASCII (one byte per character).
     */
    byte TIFF_FIELD_TYPE_ASCII     = 2;

    /**
     * TIFF unsigned short (16 bits).
     */
    byte TIFF_FIELD_TYPE_USHORT    = 3;

    /**
     * TIFF unsigned long (32 bits).
     * Note that this is not the same size as a Java <code>long</code>.
     */
    byte TIFF_FIELD_TYPE_ULONG     = 4;

    /**
     * TIFF unsigned rational (two unsigned longs).
     */
    byte TIFF_FIELD_TYPE_URATIONAL = 5;

    /**
     * TIFF signed byte (8 bits, 2's complement).
     */
    byte TIFF_FIELD_TYPE_SBYTE     = 6;

    /**
     * TIFF undefined (raw binary data).
     */
    byte TIFF_FIELD_TYPE_UNDEFINED = 7;

    /**
     * TIFF signed short (16 bits, 2's complement).
     */
    byte TIFF_FIELD_TYPE_SSHORT    = 8;

    /**
     * TIFF signed long (32 bits).
     * Note that this is not the same size as a Java <code>long</code>.
     */
    byte TIFF_FIELD_TYPE_SLONG     = 9;

    /**
     * TIFF signed rational (two signed longs).
     */
    byte TIFF_FIELD_TYPE_SRATIONAL = 10;

    /**
     * TIFF single-precision IEEE floating-point (32 bits).
     */
    byte TIFF_FIELD_TYPE_FLOAT     = 11;

    /**
     * TIFF double-precision IEEE floating-point (64 bits).
     */
    byte TIFF_FIELD_TYPE_DOUBLE    = 12;

    /**
     * TIFF IFD pointer (32 bits).
     */
    byte TIFF_FIELD_TYPE_IFD       = 13;

    /**
     * TIFF UNICODE
     */
    byte TIFF_FIELD_TYPE_UNICODE   = 14;

    /**
     * TIFF COMPLEX
     */
    byte TIFF_FIELD_TYPE_COMPLEX   = 15;

    /**
     * BigTIFF unsigned long long (64 bits).
     * Note that this is the same size as a Java <code>long</code>.
     */
    byte TIFF_FIELD_TYPE_UINT64    = 16;

    /**
     * BigTIFF signed long long (64 bits).
     * Note that this is the same size as a Java <code>long</code>.
     */
    byte TIFF_FIELD_TYPE_SINT64    = 17;

    /**
     * BigTIFF IFD pointer (64 bits).
     */
    byte TIFF_FIELD_TYPE_IFD64     = 18;

    /**
     * The size of the TIFF header (in bytes).
     * The bytes are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td>0-1&nbsp;</td><td>Byte order</td>
     *      </tr>
     *      <tr>
     *        <td>2-3</td><td>Magic number 42</td>
     *      </tr>
     *      <tr>
     *        <td>4-7</td><td>Offset of first IFD</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int TIFF_HEADER_SIZE = 8;

    /**
     * The size of a TIFF IFD entry (in bytes).
     * The bytes are:
     *  <blockquote>
     *    <table border="0" cellpadding="0">
     *      <tr>
     *        <td>0-1&nbsp;</td><td>Tag ID</td>
     *      </tr>
     *      <tr>
     *        <td>2-3</td><td>Field type</td>
     *      </tr>
     *      <tr>
     *        <td>4-7</td><td>Number of values</td>
     *      </tr>
     *      <tr>
     *        <td>8-11</td><td>Value/offset</td>
     *      </tr>
     *    </table>
     *  </blockquote>
     */
    int TIFF_IFD_ENTRY_SIZE = 12;

    /**
     * The maximum size in bytes that a metadata value can be inlined into a
     * directory entry.
     */
    int TIFF_INLINE_VALUE_MAX_SIZE = 4;

    /**
     * The size of an <code>int</code>/<code>long</code> (in bytes) used by the
     * TIFF specification.
     */
    int TIFF_INT_SIZE = 4;

    /**
     * The byte sequence used to encode that TIFF metadata is in little-endian
     * byte order: 0x4949 = &quot;II&quot; = Intel.
     * @see #TIFF_BIG_ENDIAN
     */
    short TIFF_LITTLE_ENDIAN = 0x4949;

    /**
     * A &quot;magic number&quot; used to help mark a file as being a TIFF
     * image file.
     */
    short TIFF_MAGIC_NUMBER = 42;

    /**
     * TIFF orientation: the image is landscape.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_ORIENTATION} metadata tag.
     */
    short TIFF_ORIENTATION_LANDSCAPE    = 1;

    /**
     * TIFF orientation: the image is horizontally flipped (seascape).
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_ORIENTATION} metadata tag.
     */
    short TIFF_ORIENTATION_SEASCAPE     = 2;

    /**
     * TIFF orientation: the image is rotated 180.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_ORIENTATION} metadata tag.
     */
    short TIFF_ORIENTATION_180          = 3;

    /**
     * TIFF orientation: the image is vertically flipped.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_ORIENTATION} metadata tag.
     */
    short TIFF_ORIENTATION_VFLIP        = 4;

    /**
     * TIFF orientation: the image is rotated 90 CCW and vertically flipped.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_ORIENTATION} metadata tag.
     */
    short TIFF_ORIENTATION_90CCW_VFLIP  = 5;

    /**
     * TIFF orientation: the image is rotated 90 CCW.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_ORIENTATION} metadata tag.
     */
    short TIFF_ORIENTATION_90CCW        = 6;

    /**
     * TIFF orientation: the image is rotated 90 CW and horizontally flipped.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_ORIENTATION} metadata tag.
     */
    short TIFF_ORIENTATION_90CW_HFLIP   = 7;

    /**
     * TIFF orientation: the image is rotated 90 CW.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_ORIENTATION} metadata tag.
     */
    short TIFF_ORIENTATION_90CW         = 8;

    /**
     * TIFF orientation: unknown.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_ORIENTATION} metadata tag.
     */
    short TIFF_ORIENTATION_UNKNOWN      = 9;

    /**
     * TIFF photometric interpretation: white is zero.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_PHOTOMETRIC_INTERPRETATION} metadata tag.
     */
    int TIFF_PHOTOMETRIC_WHITE_IS_ZERO	= 0;

    /**
     * TIFF photometric interpretation: black is zero.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_PHOTOMETRIC_INTERPRETATION} metadata tag.
     */
    int TIFF_PHOTOMETRIC_BLACK_IS_ZERO	= 1;

    /**
     * TIFF photometric interpretation: RGB.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_PHOTOMETRIC_INTERPRETATION} metadata tag.
     */
    int TIFF_PHOTOMETRIC_RGB		    = 2;

    /**
     * TIFF photometric interpretation: palette color.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_PHOTOMETRIC_INTERPRETATION} metadata tag.
     */
    int TIFF_PHOTOMETRIC_PALETTE        = 3;

    /**
     * TIFF photometric interpretation: transparency mask.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_PHOTOMETRIC_INTERPRETATION} metadata tag.
     */
    int TIFF_PHOTOMETRIC_TRANSPARENCY   = 4;

    /**
     * TIFF photometric interpretation: seperated.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_PHOTOMETRIC_INTERPRETATION} metadata tag.
     */
    int TIFF_PHOTOMETRIC_SEPARATED	    = 5;

    /**
     * TIFF photometric interpretation: YCbCr.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_PHOTOMETRIC_INTERPRETATION} metadata tag.
     */
    int TIFF_PHOTOMETRIC_YCBCR          = 6;

    /**
     * TIFF photometric interpretation: CIE lab.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_PHOTOMETRIC_INTERPRETATION} metadata tag.
     */
    int TIFF_PHOTOMETRIC_CIELAB		    = 8;

    /**
     * TIFF photometric interpretation: ICC lab.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_PHOTOMETRIC_INTERPRETATION} metadata tag.
     */
    int TIFF_PHOTOMETRIC_ICCLAB         = 9;

    /**
     * TIFF photometric interpretation: ITU lab.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_PHOTOMETRIC_INTERPRETATION} metadata tag.
     */
    int TIFF_PHOTOMETRIC_ITULAB         = 10;

    /**
     * TIFF planar configuration: chunky.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_PLANAR_CONFIGURATION} metadata tag.
     */
    int TIFF_PLANAR_CONFIGURATION_CHUNKY = 1;

    /**
     * TIFF planar configuration: planar.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_PLANAR_CONFIGURATION} metadata tag.
     */
    int TIFF_PLANAR_CONFIGURATION_PLANAR = 2;

    /**
     * TIFF planar configuration: none.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_RESOLUTION_UNIT} metadata tag.
     */
    int TIFF_RESOLUTION_UNIT_NONE        = 1;

    /**
     * TIFF planar configuration: inch.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_RESOLUTION_UNIT} metadata tag.
     */
    int TIFF_RESOLUTION_UNIT_INCH        = 2;

    /**
     * TIFF planar configuration: cm.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_RESOLUTION_UNIT} metadata tag.
     */
    int TIFF_RESOLUTION_UNIT_CM          = 3;

    /**
     * TIFF sample format: unsigned integer.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_SAMPLE_FORMAT} metadata tag.
     */
    int TIFF_SAMPLE_FORMAT_UINT         = 1;

    /**
     * TIFF sample format: two's complement signed integer.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_SAMPLE_FORMAT} metadata tag.
     */
    int TIFF_SAMPLE_FORMAT_INT          = 2;

    /**
     * TIFF sample format: IEEE floating point.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_SAMPLE_FORMAT} metadata tag.
     */
    int TIFF_SAMPLE_FORMAT_IEEE_FP      = 3;

    /**
     * TIFF sample format: undefined.
     * This is one of the possible values for the
     * {@link TIFFTags#TIFF_SAMPLE_FORMAT} metadata tag.
     */
    int TIFF_SAMPLE_FORMAT_UNDEF        = 4;

    /**
     * The size of a <code>short</code> (in bytes) used by the TIFF
     * specification.
     */
    int TIFF_SHORT_SIZE = 2;
}
/* vim:set et sw=4 ts=4: */
