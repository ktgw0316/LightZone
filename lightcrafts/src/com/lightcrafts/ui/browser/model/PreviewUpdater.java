/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import com.lightcrafts.image.libs.LCImageLibException;
import com.lightcrafts.image.libs.LCJPEGReader;
import com.lightcrafts.image.libs.LCJPEGWriter;
import com.lightcrafts.image.libs.OutputStreamImageDataReceiver;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.types.ImageType;
import com.lightcrafts.image.types.LZNImageType;
import com.lightcrafts.image.types.RawImageType;
import com.lightcrafts.jai.utils.Functions;
import com.lightcrafts.utils.filecache.FileCache;
import com.lightcrafts.utils.filecache.FileCacheFactory;

import static com.lightcrafts.image.libs.LCJPEGConstants.CS_RGB;

/**
 * This is a container class for "preview" images, which are high quality
 * RenderedImages that can be generated incrementally.  See
 * ImageDatum.getPreview().
 * <p>
 * Be sure to call dispose().
 */
public class PreviewUpdater extends Thread {

    /**
     * PreviewUpdater may optionally be initialized with an externally
     * defined algorithm for generating preview images from image files.
     */
    public static interface Provider {
        /**
         * Render a preview image for the given file that is bounded by the
         * given size, or return null to cause PreviewUpdater to fall back to
         * its built in algorithms.
         */
        RenderedImage getPreviewImage(File file, int size);
        /**
         * The preview updater thread has started work.
         */
        void previewStarted();
        /**
         * The preview updater thread has finished work.
         */
        void previewEnded();
    }
    /**
     * Users can pass an implementation of this to getImage(), to follow
     * the progress towards the preview image.
     */
    public static interface Observer {
        /**
         * An improved preview image has become available.
         */
        void imageChanged(PreviewUpdater updater, RenderedImage image);
    }

    // A global lock, so only one PreviewUpdater runs at a time.
    private final static Object Monitor = new Object();

    // A bounding size for preview images.
    public final static int PreviewSize = 1536;

    // For debugging, a counter to ensure previews get disposed.
    private static int DebugPreviewCount;
    private static boolean Debug;

    private FileCache cache;
    private RenderedImage image;
    private ImageMetadata meta;
    private Set<Observer> observers;
    private Provider provider;
    private boolean stillInterested;
    private boolean done;

    // PreviewUpdaters can be chained.
    //
    // When there is a user-commanded browser rotation, a new PreviewUpdater
    // messages Observers while the the original PreviewUpdater is still sent
    // in the Observer callbacks.
    //
    // When PreviewUpdaters are chained, each must wait for its predecessor to
    // complete, so that Observer callbacks happen in the correct order and no
    // collisions happen in the preview cache.

    private PreviewUpdater prevUpdater;

    /**
     * Accept a precomputed in-memory image to cache as the preview for
     * an image file.
     */
    public static void cachePreviewForImage(File file, RenderedImage image) {
        final FileCache cache = FileCacheFactory.get(file);
        if (cache == null) {
            return;
        }
        // Write the given preview to the cache
        final String key = getImageKey(file);
        try (OutputStream out = cache.putToStream(key)) {
            final OutputStreamImageDataReceiver receiver = new OutputStreamImageDataReceiver(out);
            try {
                final LCJPEGWriter writer = new LCJPEGWriter(
                        receiver, 32 * 1024,
                        image.getWidth(), image.getHeight(),
                        image.getColorModel().getNumComponents(),
                        CS_RGB, 90
                );
                writer.putImage(image);
                writer.dispose();
            } catch (LCImageLibException e) {
                logNonFatalStatic(file, e, "caching preview");
                cache.remove(key);
            }
            receiver.dispose();
        }
        catch (IOException e) {
            logNonFatalStatic(file, e, "caching preview");
            removeCacheSilentStatic(cache, file);
        }
    }

    public static void clearCachedPreviewForImage(
        ImageMetadata meta, FileCache cache
    ) {
        final PreviewUpdater updater = new PreviewUpdater(cache, null, meta);
        updater.removeCacheSilent();
        updater.dispose();
        // GC the Thread without starting it.  This is just a simple way
        // to access the writeCache() instance method.
    }

    // Report the modification time for the cached preview corresponding to
    // the given metadata, or -1 if no preview is cached.
    //
    // This is used at refreshes, to recognize situations where a preview has
    // been deliberately cached via cachePreviewForImage() immediately before
    // the corresponding image file was modified.
    public static long getCachedPreviewTime(
        ImageMetadata meta, FileCache cache
    ) {
        final File file = meta.getFile();
        final String key = getImageKey(file);
        final File cacheFile = cache.getFileFor(key);
        if (cacheFile != null) {
            return cacheFile.lastModified();
        }
        return -1;
    }

    // Called from ImageDatum.getPreview().
    PreviewUpdater(
        FileCache cache,
        RenderedImage image,
        ImageMetadata meta,
        Provider provider
    ) {
        this(cache, image, meta);
        this.provider = provider;
    }

    /**
     * This constructor is to create a follow-on PreviewUpdater that reuses
     * an earlier PreviewUpdater's Observers but computes fresh updates, for
     * instance after a user-commanded browser rotation or other asynchronous
     * image file modification.
     */
    // Called from ImageDatum.updatePreviews(), which is called from
    // ImageDatum.markClean(), which is called at the end of ImageTask.run().
    PreviewUpdater(
        PreviewUpdater oldUpdater, RenderedImage image, ImageMetadata meta
    ) {
        this(oldUpdater.cache, image, meta);
        provider = oldUpdater.provider;
        observers.addAll(oldUpdater.observers);
        prevUpdater = oldUpdater;
    }

    // Called from the other two constructors, plus clearCachedPreviewForImage().
    private PreviewUpdater(
        FileCache cache, RenderedImage image, ImageMetadata meta
    ) {
        DebugPreviewCount++;
        if (Debug) {
            System.out.println(
                meta.getFile().getName() +
                " PreviewUpdater " + DebugPreviewCount + " instantiated"
            );
        }
        this.cache = cache;
        this.image = image;
        this.meta = meta;
        observers = new HashSet<Observer>();
        stillInterested = true;
        setName("Preview Image " + meta.getFile().getName());
    }

    /**
     * Report our File, as defined in ths PreviewUpdater's ImageMetadata.
     * This is useful in the application logic for deciding what to do when
     * someone clicks on a preview image.
     */
    public File getFile() {
        return meta.getFile();
    }

    public synchronized RenderedImage getImage(Observer observer) {
        if ( !done && !isAlive() ) {
            // Try for the cached result synchronously, and maybe avoid
            // spawning the thread:

            RenderedImage cachedImage = null;
            final ImageType t = meta.getImageType();

            if ( t != null && t.hasFastPreview() ) {
                cachedImage =
                    Thumbnailer.getImage( meta.getFile(), PreviewSize, false );
            }
            if ( cachedImage == null )
                cachedImage = readCache();
            if ( cachedImage != null ) {
                image = Thumbnailer.rotate( cachedImage, meta );
                image = Functions.systemColorSpaceImage( image );
                done = true;
            } else {
                start();
            }
        }
        if ( observer != null ) {
            observers.add( observer );
        }
        return image;
    }

    private synchronized void updateImage(RenderedImage image) {
        this.image = image;
        notifyObservers();
    }

    private synchronized void notifyObservers() {
        final LinkedList<Observer> obs = new LinkedList<Observer>(observers);
        final RenderedImage im = image;
        final PreviewUpdater updater = getOriginalUpdater();
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    for (Observer observer : obs) {
                        observer.imageChanged(updater, im);
                    }
                }
            }
        );
    }

    public void run() {
        // If there is a preceding updater, make sure it has finished so that
        // updates to Observers arrive in the right order and we don't collide
        // in the cache.
        if (prevUpdater != null) {
            // Test for "done", but also "isAlive()" to avoid leaks,
            // just in case a thread dies without calling dispose().
            while (! prevUpdater.done && prevUpdater.isAlive()) {
                synchronized(Monitor) {
                    try {
                        // Just wait long enough to let another job get ahead.
                        Monitor.wait(100);
                    }
                    catch (InterruptedException e) {
                        // loop around
                    }
                }
            }
        }
        // Now compute the preview, notify Observers, and write to the cache:

        notifyStart();

        try {
            File file = meta.getFile();
            RenderedImage preview;
            synchronized(Monitor) {
                if (stillInterested) {
                    if (Debug) {
                        System.out.println(
                            meta.getFile().getName() + " PreviewUpdater running"
                        );
                    }
                    preview = readCache();
                    if (preview == null) {
                        if (provider != null) {
                            preview = provider.getPreviewImage(
                                file, PreviewSize
                            );
                        }
                        if (preview == null) {
                            if (meta.getImageType() instanceof RawImageType) {
                                preview = Thumbnailer.getImage(
                                    file, PreviewSize
                                );
                            }
                            else if (
                                meta.getImageType() == LZNImageType.INSTANCE
                            ) {
                                preview = Thumbnailer.getImage(
                                    file, PreviewSize
                                );
                            }
                            else {
                                preview = Thumbnailer.getImage(
                                    file, PreviewSize
                                );
                            }
                        }
                        if (preview == null) {
                            // Some image files just can't be previewed.
                            System.out.println(
                                "All preview methods fail for " + file
                            );
                            notifyEnd();
                            done = true;
                            return;
                        }
                        if (cache != null) {
                            writeCache(preview);
                        }
                    }
                    preview = Thumbnailer.rotate(preview, meta);
                    preview = Functions.systemColorSpaceImage(preview);
                    updateImage(preview);
                }
            }
        }
        finally {
            notifyEnd();
            done = true;
        }
    }

    void dispose() {
        if (Debug) {
            System.out.println(
                meta.getFile().getName() +
                " PreviewUpdater " + DebugPreviewCount + " disposed"
            );
        }
        DebugPreviewCount--;
        if (prevUpdater != null) {
            prevUpdater.dispose();
        }
        stillInterested = false;
        done = true;
        image = null;
    }

    private void notifyStart() {
        if (provider != null) {
            provider.previewStarted();
        }
    }

    private void notifyEnd() {
        if (provider != null) {
            provider.previewEnded();
        }
    }

    private void writeCache(RenderedImage image) {
        if (cache == null) {
            return;
        }
        // Write the preview to the cache
        final String key = getImageKey();
        try (OutputStream out = cache.putToStream(key)) {
            OutputStreamImageDataReceiver receiver = new OutputStreamImageDataReceiver(out);
            try {
                final LCJPEGWriter writer = new LCJPEGWriter(
                        receiver, 32 * 1024,
                        image.getWidth(), image.getHeight(),
                        image.getColorModel().getNumComponents(),
                        CS_RGB, 90
                );
                writer.putImage(image);
                writer.dispose();
            } catch (LCImageLibException e) {
                logNonFatal(e, "caching preview");
                cache.remove(key);
            }
            receiver.dispose();
        }
        catch (IOException e) {
            logNonFatal(e, "caching preview");
            removeCacheSilent();
        }
    }

    private RenderedImage readCache() {
        final String key = getImageKey();
        if ((cache == null) || ! cache.contains(key)) {
            return null;
        }
        try (InputStream in = cache.getStreamFor(key)) {
            ImageProviderReceiver provRecv = new ImageProviderReceiver();
            provRecv.fill(in);
            final LCJPEGReader jpeg = new LCJPEGReader(
                    provRecv, PreviewSize, PreviewSize
            );
            return jpeg.getImage();
        }
        catch (Throwable t1) {
            logNonFatal(t1, "reading cached preview");
            removeCacheSilent();
            return null;
        }
    }

    private String getImageKey() {
        File file = meta.getFile();
        return getImageKey(file);
    }

    private static String getImageKey(File file) {
        return file.getAbsolutePath() + "_preview_image";
    }

    private PreviewUpdater getOriginalUpdater() {
        if (prevUpdater != null) {
            return prevUpdater.getOriginalUpdater();
        }
        else {
            return this;
        }
    }

    private void logNonFatal(Throwable t, String message) {
        final File file = meta.getFile();
        logNonFatalStatic(file, t, message);
    }

    private static void logNonFatalStatic(
        File file, Throwable t, String message
    ) {
        final StringBuilder sb = new StringBuilder();
        sb.append(file.getAbsolutePath());
        sb.append( ' ' );
        sb.append(message);
        sb.append( ' ' );
        sb.append(t.getClass().getName());
        if (t.getMessage() != null) {
            sb.append(": ");
            sb.append(t.getMessage());
        }
        System.err.println(sb );
    }

    private void removeCacheSilent() {
        final File file = meta.getFile();
        removeCacheSilentStatic(cache, file);
    }

    private static void removeCacheSilentStatic(FileCache cache, File file) {
        final String key = getImageKey(file);
        try {
            cache.remove(key);
        }
        catch (Throwable t) {
            logNonFatalStatic(file, t, "removing cached preview");
        }
    }
}
/* vim:set et sw=4 ts=4: */
