/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import com.lightcrafts.jai.JAIContext;
import com.lightcrafts.jai.utils.Functions;

import java.awt.image.RenderedImage;
import java.awt.image.ColorModel;
import java.awt.color.ColorSpace;

/**
 * This static utility converts a given RenderedImage into a new
 * RenderedImage that has been optimized for rendering on the display:
 * <p>
 * <ul>
 * <li>
 *     Its colors have been converted to the display's color profile;
 * </li><li>
 *     If it was not a BufferedImage, its pipeline has been run; and
 * </li><li>
 *     Its color model has been set to sRGB, so AWT on Windows will not
 *     attempt further color space conversions.
 * </li>
 * </ul>
 */
class FastImageFactory {

    static RenderedImage systemColorSpaceImage(RenderedImage image) {
        ColorModel colors = image.getColorModel();
        ColorSpace space = colors.getColorSpace();
        if (space != null) {
            if (! space.equals(JAIContext.systemColorSpace)) {
                image = Functions.toColorSpace(
                    image, JAIContext.systemColorSpace, null
                );
            }
        }
        return new Functions.sRGBWrapper(image);
    }

    /**
     * Make a new image like the given image and that is suitable for fast
     * rendering on the screen.
     **/
    static RenderedImage createFastImage(RenderedImage image) {
        image = systemColorSpaceImage(image);
        image = Functions.toFastBufferedImage(image);
        return image;
    }
}
