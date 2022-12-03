/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

import com.lightcrafts.image.metadata.TIFFTags;
import com.lightcrafts.image.types.TIFFConstants;

/**
 * A <code>FlashProvider</code> provides the state of the flash at the time an
 * image was captured.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface FlashProvider extends ImageMetadataProvider {

    /**
     * @see TIFFConstants#TIFF_FLASH_FIRED_BIT
     */
    int FLASH_FIRED_BIT = TIFFConstants.TIFF_FLASH_FIRED_BIT;

    /**
     * @see TIFFConstants#TIFF_FLASH_MODE_AUTO
     */
    int FLASH_MODE_AUTO = TIFFConstants.TIFF_FLASH_MODE_AUTO;

    /**
     * @see TIFFConstants#TIFF_FLASH_MODE_COMPULSORY_OFF
     */
    int FLASH_MODE_COMPULSORY_OFF = TIFFConstants.TIFF_FLASH_MODE_COMPULSORY_OFF;

    /**
     * @see TIFFConstants#TIFF_FLASH_MODE_COMPULSORY_ON
     */
    int FLASH_MODE_COMPULSORY_ON = TIFFConstants.TIFF_FLASH_MODE_COMPULSORY_ON;

    /**
     * @see TIFFConstants#TIFF_FLASH_MODE_UNKNOWN
     */
    int FLASH_MODE_UNKNOWN = TIFFConstants.TIFF_FLASH_MODE_UNKNOWN;

    /**
     * Note that since this bit is backwards (set = <i>not</i> present),
     * implementations of {@link #getFlash()} are guaranteed to return -1 if
     * this bit is set.
     * @see TIFFConstants#TIFF_FLASH_NOT_PRESENT_BIT
     */
    int FLASH_NOT_PRESENT_BIT = TIFFConstants.TIFF_FLASH_NOT_PRESENT_BIT;

    /**
     * @see TIFFConstants#TIFF_FLASH_RED_EYE_BIT
     */
    int FLASH_RED_EYE_BIT = TIFFConstants.TIFF_FLASH_RED_EYE_BIT;

    /**
     * @see TIFFConstants#TIFF_FLASH_STROBE_RETURN_BITS
     */
    int FLASH_STROBE_RETURN_BITS = TIFFConstants.TIFF_FLASH_STROBE_RETURN_BITS;

    /**
     * Gets the state of the flash at the time the image was captured.  The
     * value returned is the value of {@link TIFFTags#TIFF_FLASH TIFF_FLASH}.
     * The value can be analyzed by using the constants declared in this
     * interface.
     *
     * @return Returns the flash state or -1 if it's unavailable.
     */
    int getFlash();
}
/* vim:set et sw=4 ts=4: */
