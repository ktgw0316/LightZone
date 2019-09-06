/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import com.lightcrafts.image.libs.*;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.utils.filecache.FileCache;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This ImageTask handles everything expensive about reading image data:
 * preview extraction, preview caching, thumbnail scaling, and thumbnail
 * rotation.
 * <p>
 * A high resolution preview gets cached in the BrowserCache and also the
 * weakly referenced in-memory ImageDatum cache.  The final scaling and
 * rotation results are never cached and are recomputed every time.
 */
public class ImageTask implements Runnable {

    // The fixed size for cached intermediate preview images
    public final static int CacheImageSize = 320;

    // The ImageDatum whose data are computed by this task
    private ImageDatum datum;

    // A cache for fallback if the weak reference cache in datum is lost
    private FileCache cache;

    // The size for final stage thumbnails
    private int size;

    ImageTask(
        ImageDatum datum,
        FileCache cache,
        int size,
        boolean useCache
    ) {
        this.datum = datum;
        this.cache = cache;
        this.size = size;

        // If this is a refresh task, then flush all cached results.
        if (! useCache) {
            clearImageCache();
        }
    }

    public void run() {
        // First initialize metadata, if it's not already read.
        ImageMetadata meta = datum.getMetadata(true);

        RenderedImage image = null;

        // If there's a file cache entry, use that.
        if (!datum.isBadFile() && cache != null) {
            image = readImageCache();
            if (image != null) {
                // Fix the orientation.
                image = Thumbnailer.rotate(image, meta);
            }
        }
        // If the file cache didn't answer, try to read the File.
        if (!datum.isBadFile() && image == null) {
            File file = datum.getFile();
            image = Thumbnailer.getImage(file, CacheImageSize);
            if (image != null) {
                // If there's an image and a cache, then cache the image.
                if (cache != null) {
                    writeImageCache(image);
                }
                // Fix the orientation.
                image = Thumbnailer.rotate(image, meta);
            }
            else {
                datum.setBadFile(true);
            }
        }
        // If anything worked, scale and optimize for the ImageDatum.
        if (image != null) {
            image = fixSizeAndColors(image);
            datum.setImage(image);
        }
        datum.markClean();
    }

    private RenderedImage fixSizeAndColors(RenderedImage image) {
        image = Thumbnailer.constrainImage(image, size);
        image = FastImageFactory.createFastImage(image);
        return image;
    }

    private void logNonFatal(Throwable t, String message) {
        File file = datum.getFile();
        StringBuffer buffer = new StringBuffer();
        buffer.append(file.getAbsolutePath());
        buffer.append(" ");
        buffer.append(message);
        buffer.append(" ");
        buffer.append(t.getClass().getName());
        if (t.getMessage() != null) {
            buffer.append(": ");
            buffer.append(t.getMessage());
        }
        System.err.println(buffer);
    }

    private RenderedImage readImageCache() {
        String key = getImageKey(CacheImageSize);
        if ((cache == null) || ! cache.contains(key)) {
            return null;
        }
        try (InputStream in = cache.getStreamFor(key)) {
            ImageProviderReceiver provRecv = new ImageProviderReceiver();
            provRecv.fill(in);
            LCJPEGReader jpeg = new LCJPEGReader(provRecv, CacheImageSize, CacheImageSize);
            return jpeg.getImage();
        }
        catch (Throwable t1) {
            logNonFatal(t1, "reading cached image");
            removeCacheSilent(key, false);
            return null;
        }
    }

    private void writeImageCache(RenderedImage image) {
        String key = getImageKey(CacheImageSize);
        if (cache == null) {
            return;
        }
        if (image == null) {
            return;
        }
        // Write the image to the cache
        try (OutputStream out = cache.putToStream(key)) {
            OutputStreamImageDataReceiver receiver = new OutputStreamImageDataReceiver(out);
            try {
                LCJPEGWriter writer = new LCJPEGWriter(
                        receiver, 32 * 1024,
                        image.getWidth(), image.getHeight(),
                        image.getColorModel().getNumComponents(),
                        LCJPEGConstants.CS_RGB,
                        90
                );
                writer.putImage(image);
            } catch (LCImageLibException e) {
                logNonFatal(e, "caching image");
                cache.remove(key);
            }
            out.flush();
            receiver.dispose();
        }
        catch (IOException e) {
            logNonFatal(e, "caching image");
            removeCacheSilent(key, false);
        }
    }

    private String getImageKey(int size) {
        File file = datum.getFile();
        return getImageKey(file, size);
    }

    public static String getImageKey(File file) {
        return getImageKey(file, CacheImageSize);
    }

    public static String getImageKey(File file, int size) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(file.getAbsolutePath());
        buffer.append("_thumbnail_image_");
        buffer.append(size);
        return buffer.toString();
    }

    private void clearImageCache() {
        String key = getImageKey(CacheImageSize);
        if ((cache != null) && cache.contains(key)) {
            removeCacheSilent(key, false);
        }
    }

    private void removeCacheSilent(String key, boolean log) {
        try {
            cache.remove(key);
        }
        catch (Throwable t) {
            if (log) {
                logNonFatal(t, "removing cached image");
            }
        }
    }
}
