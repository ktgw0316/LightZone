/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.JPEGImageType;

import java.awt.image.RenderedImage;
import java.io.File;

/**
 * There are many different ways of deriving a browser thumbnail from an
 * image file.  Each is statically implemented in this class.
 */
abstract class ImageFileStrategy {

    static ImageFileStrategy JPEGStrategy =
        new ImageFileStrategy() {
            RenderedImage maybeGetImage(ImageInfo info, int maxImageSize)
                throws Exception
            {
                ImageType imageType = info.getImageType();
                if (imageType instanceof JPEGImageType) {
                    File file = info.getFile();
                    long length = file.length();
                    if (length < 50 * 1024 * 1024) {
                        JPEGImageType jpeg = (JPEGImageType) imageType;
                        return jpeg.getImage(
                            info, null,
                            maxImageSize, maxImageSize
                        );
                    }
                }
                return null;
            }
        };

    static ImageFileStrategy PreviewStrategy =
        new ImageFileStrategy() {
            RenderedImage maybeGetImage(ImageInfo info, int maxImageSize)
                throws Exception {
                return info.getPreviewImage(
                        maxImageSize, maxImageSize
                );
            }
        };

    static ImageFileStrategy ThumbnailStrategy =
        new ImageFileStrategy() {
            RenderedImage maybeGetImage(ImageInfo info, int maxImageSize)
                throws Exception
            {
                if (maxImageSize <= 320) {
                    // These are only good at low resolution.
                    return info.getThumbnailImage();
                }
                return null;
            }
        };

    static ImageFileStrategy FullStrategy =
        new ImageFileStrategy() {
            RenderedImage maybeGetImage(ImageInfo info, int maxImageSize)
                throws Exception
            {
                File file = info.getFile();
                long length = file.length();
                if (length < 200 * 1024 * 1024) {
                    return info.getImage(null);
                }
                return null;
            }
        };

    static ImageFileStrategy[] Strategies = new ImageFileStrategy[] {
        JPEGStrategy, ThumbnailStrategy, PreviewStrategy, FullStrategy
    };

    /**
     * Get the thumbnail image for the given ImageInfo, or return null.
     */
    RenderedImage getImage(ImageInfo info, int maxImageSize) {
        try {
            return maybeGetImage(info, maxImageSize);
        }
        catch (Throwable t) {
            logNonFatal(t, info);
            return null;
        }
    }

    /**
     * Get the thumbnail image for the given ImageInfo, or throw a Throwable.
     */
    abstract RenderedImage maybeGetImage(ImageInfo info, int maxImageSize)
        throws Exception;

    private void logNonFatal(Throwable t, ImageInfo info) {
        File file = info.getFile();
        StringBuffer buffer = new StringBuffer();
        buffer.append(t.getClass().getName());
        buffer.append(" while fetching preview for ");
        buffer.append(file.getAbsolutePath());
        buffer.append(": ");
        buffer.append(t.getMessage());
        System.err.println(buffer);
    }
}
