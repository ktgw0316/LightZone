/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.metadata.providers;

import java.awt.image.RenderedImage;
import java.io.IOException;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ColorProfileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;

/**
 * A <code>PreviewImageProvider</code> provides the preview image from an image
 * file.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface PreviewImageProvider extends ImageMetadataProvider {

    /**
     * Gets the actual preview image data of an image.
     *
     * @param imageInfo The {@link ImageInfo} to get the actual preview image
     * from.
     * @param maxWidth The maximum width of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @param maxHeight The maximum height of the image to get, rescaling if
     * necessary.  A value of 0 means don't scale.
     * @return Returns said image.
     */
    RenderedImage getPreviewImage( ImageInfo imageInfo, int maxWidth,
                                   int maxHeight )
        throws BadImageFileException, ColorProfileException, IOException,
               UnknownImageTypeException;

}
/* vim:set et sw=4 ts=4: */
