/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;

/**
 * A <code>TrueImageTypeProvider</code> is used to probe an image in some way
 * to determine its "true" image type, i.e., a type that can not be determined
 * by its filename extension alone.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface TrueImageTypeProvider {

    /**
     * Probes the given image to see what type it really is.
     *
     * @param imageInfo The image to probe.
     * @return If the image is really a type that is different from what its
     * filename extension purports it to be, returns the correct
     * {@link ImageType}; otherwise returns <code>null</code>.
     */
    ImageType getTrueImageTypeOf( ImageInfo imageInfo )
        throws BadImageFileException, IOException;

}
/* vim:set et sw=4 ts=4: */
