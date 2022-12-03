/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.libs;

/**
 * An <code>LCJPEGConstants</code> contains constants for use with the LCJPEG library classes.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 * @see <a href="http://www.ijg.org/">LibJPEG</a>
 */
public interface LCJPEGConstants {
    //
    // These color space values match those in the libjpeg library's J_COLOR_SPACE enum.
    // They are not arbitrary.
    //
    int CS_UNKNOWN   = 0;
    int CS_GRAYSCALE = 1;
    int CS_RGB       = 2;
    int CS_YCbRr     = 3;
    int CS_CMYK      = 4;
    int CS_YCCK      = 5;
}
/* vim:set et sw=4 ts=4: */
