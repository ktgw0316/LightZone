/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.types;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.media.jai.PlanarImage;

import com.lightcrafts.utils.filecache.FileCache;
import com.lightcrafts.utils.filecache.FileCacheFactory;
import com.lightcrafts.utils.DCRaw;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.thread.ProgressThread;
import com.lightcrafts.image.libs.LCTIFFWriter;
import com.lightcrafts.image.libs.LCImageLibException;
import com.lightcrafts.image.libs.LCTIFFReader;
import com.lightcrafts.image.metadata.TIFFTags;
import com.lightcrafts.image.metadata.ImageMetadata;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.UnknownImageTypeException;
import com.lightcrafts.jai.JAIContext;

/**
 * TODO.
 *
 * @author Fabio Riccardi [fabio@lightcrafts.com]
 */
class RawImageCache extends Thread {
    private static String version = "V5";

    private static final class ImageToCache {
        final String cacheKey;
        final RenderedImage image;

        ImageToCache(String cacheKey, RenderedImage image) {
            this.cacheKey = cacheKey;
            this.image = image;
        }
    }

    private static final RawImageCache INSTANCE;

    static {
        INSTANCE = new RawImageCache();
        INSTANCE.start();
    }

    private ImageToCache currentJob;

    private RawImageCache() {
        super( "RawImageCache" );
        setPriority( Thread.NORM_PRIORITY - 1 );
        setDaemon( true );
    }

    static void add( String cacheKey, RenderedImage rawImage ) {
        synchronized ( INSTANCE ) {
            INSTANCE.currentJob = new ImageToCache( cacheKey, rawImage );
            INSTANCE.notify();
        }
    }

    static String getCacheKeyFor( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final ImageMetadata metadata = imageInfo.getMetadata();
        Date captureDate = metadata.getCaptureDateTime();
        if ( captureDate == null ) {
            final RawImageInfo rawInfo = (RawImageInfo)imageInfo.getAuxiliaryInfo();
            final DCRaw dcRaw = rawInfo.getDCRaw();
            captureDate = dcRaw.getCaptureDateTime();
        }
        if ( captureDate != null ) {
            final long time = captureDate.getTime();
            return imageInfo.getFile().getName() + time + version;
        }
        return null;
    }

    /**
     * Gets the {@link FileCache} that would be used for the given image.
     *
     * @param imageInfo The {@link ImageInfo} to get the {@link FileCache} for.
     * @return Returns said {@link FileCache}.
     */
    static FileCache getCacheFor( ImageInfo imageInfo ) {
        return FileCacheFactory.getGlobalCache();
    }

    static File getCachedImageFileFor( ImageInfo imageInfo )
        throws BadImageFileException, IOException, UnknownImageTypeException
    {
        final String cacheKey = getCacheKeyFor( imageInfo );
        return cacheKey != null ? getCachedImageFileFor( cacheKey ) : null;
    }

    static File getCachedImageFileFor( String cacheKey ) {
        final FileCache fileCache = FileCacheFactory.getGlobalCache();
        return fileCache != null ? fileCache.getFileFor( cacheKey ) : null;
    }

    static PlanarImage getImage( ImageInfo imageInfo, ProgressThread thread )
        throws BadImageFileException, IOException, UnknownImageTypeException,
               UserCanceledException
    {
        final File imageFile = getCachedImageFileFor( imageInfo );
        if ( imageFile != null ) {
            final String fileName = imageFile.getAbsolutePath();
            try {
                /* final LCTIFFReader reader = new LCTIFFReader( fileName, true );
                return reader.getImage( thread ); */
                return new LCTIFFReader.TIFFImage(fileName);
            }
            catch ( LCImageLibException e ) {
                // never mind, don't use the cache
                e.printStackTrace();
            }
        }
        return null;
    }

    public void run() {
        while (true) {
            synchronized (this) {
                try {
                    while (currentJob == null)
                        wait();
                } catch (InterruptedException e) {

                }
            }

            System.out.println("Caching image: " + currentJob.cacheKey);

            long t1 = System.currentTimeMillis();

            final FileCache fileCache = FileCacheFactory.getGlobalCache();
            if (fileCache != null) {
                try {
                    final File cacheFile = fileCache.putToFile(currentJob.cacheKey);

                    try {
                        LCTIFFWriter writer = new LCTIFFWriter(cacheFile.getAbsolutePath(),
                                                               currentJob.image.getWidth(),
                                                               currentJob.image.getHeight());
                        writer.setByteField( TIFFTags.TIFF_ICC_PROFILE, JAIContext.linearProfile.getData());
                        writer.putImageTiled(currentJob.image, null);
                    } catch ( LCImageLibException e) {
                        e.printStackTrace();
                    } finally {
                        fileCache.notifyAboutCloseOf(cacheFile);
                    }
                } catch ( IOException e ) {
                    // nevermind, do without cache...
                    e.printStackTrace();
                }
            }

            currentJob = null;

            long t2 = System.currentTimeMillis();
            System.out.println("Image cached in " + (t2 - t1) + "ms");
        }
    }
}
/* vim:set et sw=4 ts=4: */
