/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

/**
 * A {@code PanasonicRawTags} defines the constants used for Panasonic raw
 * metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
@SuppressWarnings( { "UnusedDeclaration" } )
public interface PanasonicRawTags extends ImageMetaTags {

    /**
     * Blue balance.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_BLUE_BALANCE          = 0x0012;

    /**
     * @see TIFFTags#TIFF_EXIF_IFD_POINTER
     */
    int PANASONIC_EXIF_IFD_POINTER      = TIFFTags.TIFF_EXIF_IFD_POINTER;

    /**
     * @see TIFFTags#TIFF_GPS_IFD_POINTER
     */
    int PANASONIC_GPS_IFD_POINTER       = TIFFTags.TIFF_GPS_IFD_POINTER;

    /**
     * Image height.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_IMAGE_HEIGHT          = 0x0006;

    /**
     * Image width.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_IMAGE_WIDTH           = 0x0007;

    /**
     * @see TIFFTags#TIFF_RICH_TIFF_IPTC
     */
    int PANASONIC_IPTC  = TIFFTags.TIFF_RICH_TIFF_IPTC;

    /**
     * ISO.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_ISO                   = 0x0017;

    /**
     * The embedded JPEG preview image.
     * <p>
     * Type: Undefined.
     */
    int PANASONIC_JPEG_FROM_RAW         = 0x002E;

    /**
     * @see TIFFTags#TIFF_MAKE
     */
    int PANASONIC_MAKE                  = TIFFTags.TIFF_MAKE;

    /**
     * @see TIFFTags#TIFF_MODEL
     */
    int PANASONIC_MODEL                 = TIFFTags.TIFF_MODEL;

    /**
     * @see TIFFTags#TIFF_ORIENTATION
     */
    int PANASONIC_ORIENTATION           = TIFFTags.TIFF_ORIENTATION;

    /**
     * Panasonic raw version.
     * <p>
     * Type: Undefined.
     */
    int PANASONIC_RAW_VERSION           = 0x0001;

    /**
     * Red balance.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_RED_BALANCE           = 0x0011;

    /**
     * Sensor height.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_SENSOR_HEIGHT         = 0x0003;

    /**
     * Sensor left border.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_SENSOR_LEFT_BORDER    = 0x0005;

    /**
     * Sensor top border.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_SENSOR_TOP_BORDER     = 0x0004;

    /**
     * Sensor width.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_SENSOR_WIDTH          = 0x0002;

    /**
     * @see TIFFTags#TIFF_STRIP_OFFSETS
     */
    int PANASONIC_STRIP_OFFSETS         = TIFFTags.TIFF_STRIP_OFFSETS;

    /**
     * White-balance blue level.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_WB_BLUE_LEVEL         = 0x0026;

    /**
     * White-balance green level.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_WB_GREEN_LEVEL        = 0x0025;

    /**
     * White-balance red level.
     * <p>
     * Type: Unsigned short.
     */
    int PANASONIC_WB_RED_LEVEL          = 0x0024;

}
/* vim:set et sw=4 ts=4: */
