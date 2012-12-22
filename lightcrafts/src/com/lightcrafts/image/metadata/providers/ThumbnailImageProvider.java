/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

import java.awt.image.RenderedImage;
import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ColorProfileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;

/**
 * A <code>ThumbnailImageProvider</code> provides the thumbnail image from an
 * image file.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ThumbnailImageProvider extends ImageMetadataProvider {

    /**
     * Gets the actual thumbnail image data of an image.
     *
     * @param imageInfo The {@link ImageInfo} to get the actual thumbnail image
     * from.
     * @return Returns said image.
     */
    RenderedImage getThumbnailImage( ImageInfo imageInfo )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException;

}
/* vim:set et sw=4 ts=4: */
