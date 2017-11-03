/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata;

/**
 * A <code>CoreTags</code> defines the constants used for core metadata tags.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface CoreTags extends ImageMetaTags {

    ////////// File tags //////////////////////////////////////////////////////

    /**
     * File last modified date/time.
     * <p>
     * Type: Date.
     */
    int CORE_FILE_DATE_TIME         = 0x0001;

    /**
     * Directory/path name.
     * <p>
     * Type: ASCII.
     */
    int CORE_DIR_NAME               = 0x0002;

    /**
     * File name.
     * <p>
     * Type: ASCII.
     */
    int CORE_FILE_NAME              = 0x0003;

    /**
     * File size in bytes.
     * <p>
     * Type: Unsigned long.
     */
    int CORE_FILE_SIZE              = 0x0004;

    /**
     * File creation date/time.
     * <p>
     * Type: Date.
     */
    int CORE_FILE_CREATION_DATE_TIME    = 0x0005;

    ////////// Shot tags //////////////////////////////////////////////////////

    /**
     * Aperture.
     * <p>
     * Type: Float.
     */
    int CORE_APERTURE               = 0x0101;

    /**
     * ISO.
     * <p>
     * Type: Unsigned short.
     */
    int CORE_ISO                    = 0x0102;

    /**
     * Shutter speed.
     * <p>
     * Type: Float.
     */
    int CORE_SHUTTER_SPEED          = 0x0103;

    /**
     * Lens.
     * <p>
     * Type: ASCII.
     */
    int CORE_LENS                   = 0x0104;

    /**
     * Lens focal length.
     * <p>
     * Type: Float.
     */
    int CORE_FOCAL_LENGTH           = 0x0105;

    /**
     * Color temperature in degrees Kelvin.
     * <p>
     * Type: Unsigned short.
     */
    int CORE_COLOR_TEMPERATURE      = 0x0106;

    /**
     * Camera make and model, e.g., "Canon EOS-1DS".
     * <p>
     * Type: ASCII.
     */
    int CORE_CAMERA                 = 0x0107;

    /**
     * Capture date/time.
     * <p>
     * Type: Date.
     */
    int CORE_CAPTURE_DATE_TIME      = 0x0108;

    /**
     * Flash.
     * <p>
     * Type: ASCII.
     */
    int CORE_FLASH                  = 0x0109;

    ////////// Image tags /////////////////////////////////////////////////////

    /**
     * THe name of the color profile.
     * <p>
     * Type: ASCII.
     */
    int CORE_COLOR_PROFILE          = 0x0200;

    /**
     * Image height in pixels.
     * <p>
     * Type: Unsigned short.
     */
    int CORE_IMAGE_HEIGHT           = 0x0201;

    /**
     * Image width in pixels.
     * <p>
     * Type: Unsigned short.
     */
    int CORE_IMAGE_WIDTH            = 0x0202;

    /**
     * Image resolution, e.g., "72 pixels/inch".
     * <p>
     * Type: ASCII.
     */
    int CORE_IMAGE_RESOLUTION       = 0x0203;

    /**
     * @see TIFFTags#TIFF_ORIENTATION
     */
    int CORE_IMAGE_ORIENTATION      = 0x0204;

    /**
     * This is the image orientation of the original image (before any
     * orientation from an XMP file is applied).
     */
    int CORE_ORIGINAL_ORIENTATION   = 0x0205;

    /**
     * User rating.
     * <p>
     * Type: Unsigned short.
     */
    int CORE_RATING                 = 0x0206;

    /**
     * Original image height in pixels.
     * <p>
     * Type: Unsigned short.
     */
    int CORE_ORIGINAL_IMAGE_HEIGHT  = 0x0207;

    /**
     * Original image width in pixels.
     * <p>
     * Type: Unsigned short.
     */
    int CORE_ORIGINAL_IMAGE_WIDTH   = 0x0208;

    ////////// Metadata tags //////////////////////////////////////////////////

    /**
     * Artist.
     * <p>
     * Type: ASCII.
     */
    int CORE_ARTIST                 = 0x0300;

    /**
     * Caption.
     * <p>
     * Type: ASCII.
     */
    int CORE_CAPTION                = 0x0301;

    /**
     * Copyright.
     * <p>
     * Type: ASCII.
     */
    int CORE_COPYRIGHT              = 0x0302;

    /**
     * Title.
     * <p>
     * Type: ASCII.
     */
    int CORE_TITLE                  = 0x0303;
}
/* vim:set et sw=4 ts=4: */
