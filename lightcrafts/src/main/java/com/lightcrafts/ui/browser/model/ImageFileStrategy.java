/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.JPEGImageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.RenderedImage;
import java.io.File;

/**
 * There are many different ways of deriving a browser thumbnail from an
 * image file.  Each is statically implemented in this class.
 */
abstract class ImageFileStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ImageFileStrategy.class);

    static ImageFileStrategy JPEGStrategy =
        new ImageFileStrategy() {
            RenderedImage maybeGetImage(ImageInfo info, int maxImageSize)
                throws Exception
            {
                ImageType imageType = info.getImageType();
                if (imageType instanceof JPEGImageType jpeg) {
                    File file = info.getFile();
                    long length = file.length();
                    if (length < 50 * 1024 * 1024) {
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
        } catch (Exception e) {
            logNonFatal(e, info);
            return null;
        }
    }

    /**
     * Get the thumbnail image for the given ImageInfo, or throw a Throwable.
     */
    abstract RenderedImage maybeGetImage(ImageInfo info, int maxImageSize)
        throws Exception;

    private void logNonFatal(Exception e, ImageInfo info) {
        final String path = info.getFile().getAbsolutePath();
        logger.debug("{} while fetching preview for {}: {}",
                e.getClass().getName(), path, e.getMessage(), e);
    }
}
