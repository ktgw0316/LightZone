/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import com.lightcrafts.image.metadata.CIFFTags;

/**
 * A <code>CIFFConstants</code> defines some constants for CIFF metadata.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <i>CIFF 1.0r4</i>, Canon Incorporated, December 1997.
 */
public interface CIFFConstants {

    /**
     * CIFF auto rotate: none.
     * This is one of the possible values for the
     * {@link CIFFTags#CIFF_SI_AUTO_ROTATE} metadata tag.
     */
    short CIFF_AUTO_ROTATE_NONE     = 0;

    /**
     * CIFF auto rotate: the image is rotated 90 CCW.
     * This is one of the possible values for the
     * {@link CIFFTags#CIFF_SI_AUTO_ROTATE} metadata tag.
     */
    short CIFF_AUTO_ROTATE_90CCW    = 1;

    /**
     * CIFF auto rotate: 180.
     * This is one of the possible values for the
     * {@link CIFFTags#CIFF_SI_AUTO_ROTATE} metadata tag.
     */
    short CIFF_AUTO_ROTATE_180      = 2;

    /**
     * CIFF auto rotate: the image is rotated 90 CW.
     * This is one of the possible values for the
     * {@link CIFFTags#CIFF_SI_AUTO_ROTATE} metadata tag.
     */
    short CIFF_AUTO_ROTATE_90CW     = 3;

    /**
     * @see TIFFConstants#TIFF_BIG_ENDIAN
     */
    short CIFF_BIG_ENDIAN = TIFFConstants.TIFF_BIG_ENDIAN;

    /**
     * CIFF color space: sRGB.
     * This is one of the possible values for the
     * {@link CIFFTags#CIFF_COLOR_SPACE} metadata tag.
     */
    short CIFF_COLOR_SPACE_SRGB = 1;

    /**
     * CIFF color space: Adobe RGB.
     * This is one of the possible values for the
     * {@link CIFFTags#CIFF_COLOR_SPACE} metadata tag.
     */
    short CIFF_COLOR_SPACE_ADOBE_RGB = 2;

    /**
     * Bit mask used to obtain the data type from the CIFF tag bits.
     */
    int CIFF_DATA_TYPE_MASK = 0x3800;

    /**
     * Bit value indicating that a CIFF metadata value is in the entry itself.
     */
    int CIFF_DATA_LOC_IN_ENTRY = 0x4000;

    /**
     * Ths size (in bytes) of a CIFF unsigned long.
     * Note that this is not the same size as a Java <code>long</code>.
     */
    int CIFF_FIELD_SIZE_ULONG    = 4;

    /**
     * Ths size (in bytes) of a CIFF unsigned short.
     */
    int CIFF_FIELD_SIZE_USHORT   = 2;

    /**
     * TIFF ASCII (one byte per character).
     */
    short CIFF_FIELD_TYPE_ASCII  = 0x0800;

    /**
     * TIFF unsigned byte (8 bits).
     */
    short CIFF_FIELD_TYPE_UBYTE  = 0x0000;

    /**
     * CIFF unsigned long (32 bits).
     * Note that this is not the same size as a Java <code>long</code>.
     */
    short CIFF_FIELD_TYPE_ULONG  = 0x1800;

    /**
     * CIFF unsigned short (16 bits).
     */
    short CIFF_FIELD_TYPE_USHORT = 0x1000;

    /**
     * The length of the CIFF header (in bytes).
     */
    int CIFF_HEADER_SIZE = 14;

    /**
     * The length of a directory entry (in bytes).
     */
    int CIFF_IFD_ENTRY_SIZE = 10;

    /**
     * @see TIFFConstants#TIFF_INT_SIZE
     */
    int CIFF_INT_SIZE = TIFFConstants.TIFF_INT_SIZE;

    /**
     * @see TIFFConstants#TIFF_LITTLE_ENDIAN
     */
    short CIFF_LITTLE_ENDIAN = TIFFConstants.TIFF_LITTLE_ENDIAN;

    /**
     * @see TIFFConstants#TIFF_SHORT_SIZE
     */
    int CIFF_SHORT_SIZE = TIFFConstants.TIFF_SHORT_SIZE;

    /**
     * Bit mask used to obtain the tag ID from the CIFF tag bits.
     */
    int CIFF_TAG_ID_MASK = 0x3FFF;

    short CIFF_FIELD_TYPE_MIXED  = 0x2000;
    short CIFF_FIELD_TYPE_HEAP1  = 0x2800;
    short CIFF_FIELD_TYPE_HEAP2  = 0x3000;
}
/* vim:set et sw=4 ts=4: */
