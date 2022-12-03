/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

import com.lightcrafts.image.types.TIFFConstants;

import static com.lightcrafts.image.metadata.EXIFTags.EXIF_EXPOSURE_TIME;
import static com.lightcrafts.image.types.TIFFConstants.*;

/**
 * An <code>EXIFConstants</code> defines some constants for EXIF metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface EXIFConstants {

    /**
     * The size in bytes of each metadata field type.
     */
    int[] EXIF_FIELD_SIZE = TIFF_FIELD_SIZE;

    /**
     * The size of an EXIF header (in bytes).  This is the sum of
     * {@link #EXIF_HEADER_START_SIZE} and
     * {@link TIFFConstants#TIFF_HEADER_SIZE} since an EXIF header has a TIFF
     * header embedded inside of it.
     */
    int EXIF_HEADER_SIZE = 14;

    /**
     * The size of the EXIF start bytes in the header (in bytes).
     * (The start bytes are: &quot;Exif\0\0&quot;.)
     */
    int EXIF_HEADER_START_SIZE = 6;

    /**
     * The size of an EXIF IFD entry (in bytes).
     * @see TIFFConstants#TIFF_IFD_ENTRY_SIZE
     */
    int EXIF_IFD_ENTRY_SIZE = TIFF_IFD_ENTRY_SIZE;

    /**
     * The size of an <code>int</code> used by the EXIF specification.
     */
    int EXIF_INT_SIZE = TIFF_INT_SIZE;

    /**
     * The size of a <code>short</code> used by the EXIF specification.
     */
    int EXIF_SHORT_SIZE = TIFF_SHORT_SIZE;

    /**
     * EXIF contrast: normal.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_CONTRAST} metadata tag.
     */
    short EXIF_CONTRAST_NORMAL         = 0;

    /**
     * EXIF contrast: low saturation.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_CONTRAST} metadata tag.
     */
    short EXIF_CONTRAST_LOW_SATURATION = 1;

    /**
     * EXIF contrast: hard.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_CONTRAST} metadata tag.
     */
    short EXIF_CONTRAST_HARD           = 2;

    /**
     * EXIF exposure program: undefined.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_EXPOSURE_PROGRAM} metadata tag.
     */
    short EXIF_EXPOSURE_PROGRAM_UNDEFINED         = 0;

    /**
     * EXIF exposure program: manual.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_EXPOSURE_PROGRAM} metadata tag.
     */
    short EXIF_EXPOSURE_PROGRAM_MANUAL            = 1;

    /**
     * EXIF exposure program: normal.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_EXPOSURE_PROGRAM} metadata tag.
     */
    short EXIF_EXPOSURE_PROGRAM_NORMAL            = 2;

    /**
     * EXIF exposure program: aperture priority.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_EXPOSURE_PROGRAM} metadata tag.
     */
    short EXIF_EXPOSURE_PROGRAM_APERTURE_PRIORITY = 3;

    /**
     * EXIF exposure program: shutter priority.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_EXPOSURE_PROGRAM} metadata tag.
     */
    short EXIF_EXPOSURE_PROGRAM_SHUTTER_PRIORITY  = 4;

    /**
     * EXIF exposure program: creative.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_EXPOSURE_PROGRAM} metadata tag.
     */
    short EXIF_EXPOSURE_PROGRAM_CREATIVE          = 5;

    /**
     * EXIF exposure program: action.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_EXPOSURE_PROGRAM} metadata tag.
     */
    short EXIF_EXPOSURE_PROGRAM_ACTION            = 6;

    /**
     * EXIF exposure program: portrait.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_EXPOSURE_PROGRAM} metadata tag.
     */
    short EXIF_EXPOSURE_PROGRAM_PORTRAIT          = 7;

    /**
     * EXIF exposure program: landscape.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_EXPOSURE_PROGRAM} metadata tag.
     */
    short EXIF_EXPOSURE_PROGRAM_LANDSCAPE         = 8;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_UBYTE}.
     */
    byte EXIF_FIELD_TYPE_UBYTE     = TIFF_FIELD_TYPE_UBYTE;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_ASCII}.
     */
    byte EXIF_FIELD_TYPE_STRING    = TIFF_FIELD_TYPE_ASCII;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_USHORT}.
     */
    byte EXIF_FIELD_TYPE_USHORT    = TIFF_FIELD_TYPE_USHORT;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_ULONG}.
     */
    byte EXIF_FIELD_TYPE_ULONG     = TIFF_FIELD_TYPE_ULONG;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_URATIONAL}.
     */
    byte EXIF_FIELD_TYPE_URATIONAL = TIFF_FIELD_TYPE_URATIONAL;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_SBYTE}.
     */
    byte EXIF_FIELD_TYPE_SBYTE     = TIFF_FIELD_TYPE_SBYTE;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_UNDEFINED}.
     */
    byte EXIF_FIELD_TYPE_UNDEFINED = TIFF_FIELD_TYPE_UNDEFINED;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_SSHORT}.
     */
    byte EXIF_FIELD_TYPE_SSHORT    = TIFF_FIELD_TYPE_SSHORT;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_SLONG}.
     */
    byte EXIF_FIELD_TYPE_SLONG     = TIFF_FIELD_TYPE_SLONG;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_SRATIONAL}.
     */
    byte EXIF_FIELD_TYPE_SRATIONAL = TIFF_FIELD_TYPE_SRATIONAL;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_FLOAT}.
     */
    byte EXIF_FIELD_TYPE_FLOAT     = TIFF_FIELD_TYPE_FLOAT;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_DOUBLE}.
     */
    byte EXIF_FIELD_TYPE_DOUBLE    = TIFF_FIELD_TYPE_DOUBLE;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_UNICODE}.
     */
    byte EXIF_FIELD_TYPE_UNICODE   = TIFF_FIELD_TYPE_UNICODE;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_COMPLEX}.
     */
    byte EXIF_FIELD_TYPE_COMPLEX   = TIFF_FIELD_TYPE_COMPLEX;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_UINT64}.
     */
    byte EXIF_FIELD_TYPE_UINT64    = TIFF_FIELD_TYPE_UINT64;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_SINT64}.
     */
    byte EXIF_FIELD_TYPE_SINT64    = TIFF_FIELD_TYPE_SINT64;

    /**
     * @see {@link TIFFConstants#TIFF_FIELD_TYPE_IFD64}.
     */
    byte EXIF_FIELD_TYPE_IFD64     = TIFF_FIELD_TYPE_IFD64;

    /**
     * EXIF metering mode: unknown.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_METERING_MODE} metadata tag.
     */
    short EXIF_METERING_MODE_UNKNOWN                 = 0;

    /**
     * EXIF metering mode: average.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_METERING_MODE} metadata tag.
     */
    short EXIF_METERING_MODE_AVERAGE                 = 1;

    /**
     * EXIF metering mode: center-weighted average.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_METERING_MODE} metadata tag.
     */
    short EXIF_METERING_MODE_CENTER_WEIGHTED_AVERAGE = 2;

    /**
     * EXIF metering mode: spot.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_METERING_MODE} metadata tag.
     */
    short EXIF_METERING_MODE_SPOT                    = 3;

    /**
     * EXIF metering mode: multispot.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_METERING_MODE} metadata tag.
     */
    short EXIF_METERING_MODE_MULTISPOT               = 4;

    /**
     * EXIF metering mode: pattern.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_METERING_MODE} metadata tag.
     */
    short EXIF_METERING_MODE_PATTERN                 = 5;

    /**
     * EXIF metering mode: partial.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_METERING_MODE} metadata tag.
     */
    short EXIF_METERING_MODE_PARTIAL                 = 6;

    /**
     * EXIF metering mode: other.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_METERING_MODE} metadata tag.
     */
    short EXIF_METERING_MODE_OTHER                   = 255;

    /**
     * EXIF sharpness: normal.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_SHARPNESS} metadata tag.
     */
    short EXIF_SHARPNESS_NORMAL = 0;

    /**
     * EXIF sharpness: soft.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_SHARPNESS} metadata tag.
     */
    short EXIF_SHARPNESS_SOFT   = 1;

    /**
     * EXIF sharpness: hard.
     * This is one of the possible values for the
     * {@link EXIFTags#EXIF_SHARPNESS} metadata tag.
     */
    short EXIF_SHARPNESS_HARD   = 2;

    /**
     * All EXIF tags greater than or equal to this tag ID are in the subEXIF
     * directory.
     */
    int EXIF_SUBEXIF_TAG_ID_START = EXIF_EXPOSURE_TIME;
}
/* vim:set et sw=4 ts=4: */
