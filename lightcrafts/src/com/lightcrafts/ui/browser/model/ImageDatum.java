/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import com.lightcrafts.image.BadImageFileException;
import com.lightcrafts.image.ImageInfo;
import com.lightcrafts.image.UnknownImageTypeException;
import static com.lightcrafts.image.metadata.CoreTags.*;
import com.lightcrafts.image.metadata.*;
import static com.lightcrafts.image.metadata.TIFFTags.TIFF_XMP_PACKET;
import com.lightcrafts.image.metadata.values.ImageMetaValue;
import static com.lightcrafts.ui.browser.model.Locale.LOCALE;
import com.lightcrafts.utils.filecache.FileCache;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.*;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.LinkedList;

import lombok.val;

/**
 * A holder for all data that are derived from an image for browser purposes,
 * such as metadata, thumbnails, and a cache for ImageTasks.
 */
public class ImageDatum {

    // The File defining the image
    private File file;

    // The file's modification time when metadata were last cached
    private long fileCacheTime;

    // The XMP File updating the image metadata
    private File xmpFile;

    // The XMP file's modification time when metadata were last cached
    private long xmpFileCacheTime;

    // Selected metadata, updated asynchronously
    private ImageMetadata meta;

    // Thumbnail image, updated asynchronously
    private SoftReference<RenderedImage> image;

    // This ImageDatum's LZN encoding info, computed lazily
    private ImageDatumType type;

    // This flag indicates whether the ImageTask needs to run
    private boolean isDirty;

    // The current runnable for background work
    private ImageTask task;

    // The Thread container for the ImageTask
    private ImageTaskQueue queue;

    // The cache used by the ImageTask
    private FileCache cache;

    // The size for thumbnails, given to ImageTasks
    private int size;

    // Observers for asynchronous replies to getImage() and getMetadata()
    private LinkedList<ImageDatumObserver> observers;

    // PreviewUpdaters we are currently maintaining
    private LinkedList<PreviewUpdater> previews;

    // ImageDatums can be logically associated into groups
    private ImageGroup group;

    private boolean badFile = false;

    public ImageDatum(
        File file, int size, ImageTaskQueue queue, FileCache cache
    ) {
        this.file = file;
        this.size = size;
        this.queue = queue;
        this.cache = cache;

        markDirty();

        observers = new LinkedList<ImageDatumObserver>();
        previews = new LinkedList<PreviewUpdater>();

        group = new ImageGroup(this);
    }

    /**
     * Get the image File backing the data in this ImageDatum.  This File is
     * an immutable property of its ImageDatum.
     */
    public File getFile() {
        return file;
    }

    /**
     * Get the XMP file which extends the metadata in this ImageDatum.  This
     * may be null, if there was an error reading image file metadata.
     */
    public File getXmpFile() {
        return xmpFile;
    }

    public boolean isBadFile() {
        return badFile;
    }

    void setBadFile() {
        badFile = true;
    }

    /**
     * Rotate the image counterclockwise by 90 degrees, unless this image
     * has LZN data, in which case throw an IOException.
     */
    public void rotateLeft()
        throws IOException, BadImageFileException, UnknownImageTypeException
    {
        if ((type == null) || type.hasLznData()) {
            throw new IOException(LOCALE.get("CantRotateLzn"));
        }
        ImageInfo info = ImageInfo.getInstanceFor(file);
        ImageMetadata meta = info.getMetadata();
        meta.setOrientation(meta.getOrientation().get90CCW());
        commitRotate(info, 3);
    }

    /**
     * Rotate the image clockwise by 90 degrees, unless this image has LZN
     * data, in which case throw an IOException.
     */
    public void rotateRight()
        throws IOException, BadImageFileException, UnknownImageTypeException
    {
        if ((type == null) || type.hasLznData()) {
            throw new IOException(LOCALE.get("CantRotateLzn"));
        }
        ImageInfo info = ImageInfo.getInstanceFor(file);
        ImageMetadata meta = info.getMetadata();
        meta.setOrientation(meta.getOrientation().get90CW());
        commitRotate(info, 1);
    }

    /**
     * Flip the image horizontally, unless this image
     * has LZN data, in which case throw an IOException.
     */
    public void flipHorizontal()
            throws IOException, BadImageFileException, UnknownImageTypeException
    {
        if ((type == null) || type.hasLznData()) {
            throw new IOException(LOCALE.get("CantFlipLzn"));
        }
        val info = ImageInfo.getInstanceFor(file);
        val meta = info.getMetadata();
        meta.setOrientation(meta.getOrientation().getHFlip());
        commitFlip(info, true, false);
    }

    /**
     * Flip the image vertically, unless this image
     * has LZN data, in which case throw an IOException.
     */
    public void flipVertical()
            throws IOException, BadImageFileException, UnknownImageTypeException
    {
        if ((type == null) || type.hasLznData()) {
            throw new IOException(LOCALE.get("CantFlipLzn"));
        }
        val info = ImageInfo.getInstanceFor(file);
        val meta = info.getMetadata();
        meta.setOrientation(meta.getOrientation().getVFlip());
        commitFlip(info, false, true);
    }

    /**
     * Set the rating number on this image, 1 to 5.
     */
    public void setRating(int rating)
        throws IOException, BadImageFileException, UnknownImageTypeException
    {
        ImageInfo info = ImageInfo.getInstanceFor(file);
        ImageMetadata meta = info.getMetadata();
        meta.setRating(rating);
        writeToXmp(info);
        rateInMemory(rating);
    }

    /**
     * Clear the rating number on this image.
     */
    public void clearRating()
        throws IOException, BadImageFileException, UnknownImageTypeException
    {
        ImageInfo info = ImageInfo.getInstanceFor(file);
        ImageMetadata meta = info.getMetadata();
        meta.clearRating();
        writeToXmp(info);
        rateInMemory(0);
    }

    /**
     * Discard all computed results for the File and enqueue a new task
     * to recompute metadata and thumbnail data.
     */
    public void refresh(boolean useImageCache) {
        if ((! useImageCache) && (meta != null)) {
            // Clear the cached preview, but only if the preview is older than
            // ten seconds.  (Sometimes, a preview is deliberately cached
            // right before an image file is modified.)
            long now = System.currentTimeMillis();
            long mod = PreviewUpdater.getCachedPreviewTime(meta, cache);
            if (now - mod > 10000) {
                clearPreview();
            }
        }
        meta = null;
        type = null;
        image = null;
        clearMetadataCache();
        restartTask(useImageCache);
    }

    /**
     * Remove this ImageDatum's ImageTask from the ImageTaskQueue, if it is
     * not already running.
     */
    public void cancel() {
        if (task != null) {
            queue.removeTask(task);
        }
    }

    // Called from ImageList.
    void setSize(int size) {
        if ((size != this.size) && (size > 0)) {
            this.size = size;
            restartTask(true);
        }
    }

    // Synchronized because the ImageTask modifies the image.
    public synchronized RenderedImage getImage(ImageDatumObserver observer) {
        if ((observer != null) && ! observers.contains(observer)) {
            observers.add(observer);
        }
        RenderedImage image = (this.image != null) ? this.image.get() : null;

        if (! badFile) {
            if ((task == null) || (image == null) || isDirty) {
                restartTask(true);  // queue slow thumbnailing things
            }
            queue.raiseTask(task);
        }
        if (image != null) {
            return image;
        }
        return EggImage.getEggImage(size);
    }

    // Synchronized because the ImageTask modifies the taskCache and the image.
    public synchronized PreviewUpdater getPreview(
        PreviewUpdater.Provider provider    // null is OK
    ) {
        // First, find the best preview currently available:
        RenderedImage preview = getImage(null);

        // Don't assume that our metadata member "meta" is non-null--
        // this method may get called after a refresh and before our task runs.
        PreviewUpdater updater =
            new PreviewUpdater(cache, preview, getMetadata(true), provider);

        previews.add(updater);

        return updater;
    }

    public void disposePreviews() {
        for (PreviewUpdater preview : previews) {
            preview.dispose();
        }
        previews.clear();
    }

    // Called from ImageTask when a thumbnail is ready
    synchronized void setImage(RenderedImage image) {
        this.image = new SoftReference<RenderedImage>(image);
    }

    long getFileCacheTime() {
        return fileCacheTime;
    }

    long getXmpFileCacheTime() {
        return xmpFileCacheTime;
    }

    // Synchronized because the poller and the painting read it, and the
    // task writes it.
    public synchronized ImageMetadata getMetadata(boolean useCache) {
        // Backwards compatibility:
        if (readRotateCache() != 0) {
            migrateRotateCacheToXmp();
            useCache = false;
        }
        if ((meta == null) && useCache) {
            readMetadataCache();
        }
        if (meta != null) {
            return meta;
        }
        File file = getFile();
        ImageInfo info = ImageInfo.getInstanceFor(file);
        try {
            ImageMetadata meta = info.getMetadata();
            try {
                xmpFile = new File(info.getXMPFilename());
            }
            catch (Throwable e) {
                badFile = true;
                logMetadataError(e);
                xmpFile = null;
            }
            // Limit the metadata to data used for sorting and display.
            updateMetadata(meta);
            // Note file modification times, used for metadata cache keys.
            updateFileTimes();
            // Write limited, timestamped metadata to the cache.
            writeMetadataCache();
        }
        catch (Throwable e) {
            badFile = true;
            logMetadataError(e);
            meta = EggImage.getEggMetadata(file);
        }
        return meta;
    }

    public ImageGroup getGroup() {
        return group;
    }

    public void setGroup(ImageGroup group) {
        this.group.removeImageDatum(this);
        this.group = group;
        group.addImageDatum(this);
    }

    public ImageGroup newGroup() {
        group.removeImageDatum(this);
        group = new ImageGroup(this);
        return group;
    }

    public ImageDatumType getType() {
        if (type == null) {
            type = ImageDatumType.getTypeOf(this);
        }
        return type;
    }

    // Called before enqueueing the task, so in case it gets cancelled,
    // we know to resume later on.
    void markDirty() {
        isDirty = true;
    }

    // Called from ImageTask, when there is no more work to do.
    void markClean() {
        isDirty = false;
        updatePreviews();
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    notifyImageObservers();
                }
            }
        );
    }

    // Keep only the metadata fields used for sorting and display.
    private void updateMetadata(ImageMetadata meta) {
        this.meta = new ImageMetadata();

        ImageMetadataDirectory core =
            meta.getDirectoryFor(CoreDirectory.class, true);
        ImageMetadataDirectory thisCore =
            this.meta.getDirectoryFor(CoreDirectory.class, true);

        // Tags used for presentation:
        int[] tags = new int[] {
            CORE_FILE_NAME,
            CORE_DIR_NAME,
            CORE_IMAGE_ORIENTATION,
            CORE_RATING
        };
        for (int tag : tags) {
            ImageMetaValue value = core.getValue(tag);
            if (value != null) {
                thisCore.putValue(tag, value);
            }
        }
        // Tags used for sorting:
        ImageDatumComparator[] comps = ImageDatumComparator.getAll();
        for (ImageDatumComparator comp : comps) {
            int tagId = comp.getTagId();
            ImageMetaValue value = core.getValue(tagId);
            if (value != null) {
                thisCore.putValue(tagId, value);
            }
        }
        // One more tag, used to determine the ImageDatumType for TIFFs
        ImageMetaValue xmpValue = meta.getValue(
            TIFFDirectory.class, TIFF_XMP_PACKET
        );
        if (xmpValue != null) {
            ImageMetadataDirectory thisTiff =
                this.meta.getDirectoryFor(TIFFDirectory.class, true);
            thisTiff.putValue(TIFF_XMP_PACKET, xmpValue);
        }
    }

    // Perform the operations common to rotateLeft() and rotateRight().
    private void commitRotate(ImageInfo info, int multiple)
            throws IOException, BadImageFileException, UnknownImageTypeException {
        commitRotateFlip(info, multiple, false, false);
    }

    // Perform the operations common to flipHorizontal() and flipVertical().
    private void commitFlip(ImageInfo info, boolean horizontal, boolean vertical)
            throws IOException, BadImageFileException, UnknownImageTypeException {
        commitRotateFlip(info, 0, horizontal, vertical);
    }

    // Rotate and flip the in-memory thumbnail; notify observers, so the display
    // will update; and write the modified metadata to XMP.
    private synchronized void commitRotateFlip(ImageInfo info, int multiple,
                                           boolean horizontal, boolean vertical)
            throws IOException, BadImageFileException, UnknownImageTypeException
    {
        // Update the in-memory image immediately, for interactive response.
        rotateFlipInMemory(multiple, horizontal, vertical);
        // This triggers a refresh through the file modification polling
        try {
            writeToXmp(info);
        }
        // If the XMP write didn't work out, we better undo the rotation:
        // TODO: Java7 multi-catch
        catch (IOException e) {
            rotateFlipInMemory(-multiple, horizontal, vertical);
            throw e;
        }
        catch (BadImageFileException e) {
            rotateFlipInMemory(-multiple, horizontal, vertical);
            throw e;
        }
        catch (UnknownImageTypeException e) {
            rotateFlipInMemory(-multiple, horizontal, vertical);
            throw e;
        }
    }

    // Rotate and flip the in-memory thumbnail image directly. This is called from
    // commitRotateFlip() to update the painted image quickly, until the polling
    // can catch up with an authoritative image.
    private void rotateFlipInMemory(int multiple,
                                    boolean horizontal, boolean vertical) {
        if (this.image == null) {
            return;
        }

        final RenderedImage image = Thumbnailer.rotateNinetyTimesThenFlip(
                this.image.get(), multiple, horizontal, vertical);
        this.image = new SoftReference<RenderedImage>(image);
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                notifyImageObservers();
            }
        });
    }

    // Set the in-memory rating value directly.  This is called from
    // setRating() to update the painted image quickly, until the polling
    // can catch up with the authoritative metadata.
    private void rateInMemory(int rating) {
        if (meta != null) {
            if (rating > 0) {
                meta.setRating(rating);
            }
            else {
                meta.clearRating();
            }
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        notifyImageObservers();
                    }
                }
            );
        }
    }

    private void restartTask(boolean useCache) {
        if (task != null) {
            queue.removeTask(task);
        }
        task = new ImageTask(this, cache, size, useCache);
        markDirty();
        queue.addTask(task);
    }

    private synchronized void updatePreviews() {
        // Push a rotation change out to all running PreviewUpdaters.
        LinkedList<PreviewUpdater> newRefs = new LinkedList<PreviewUpdater>();
        for (Iterator<PreviewUpdater> i=previews.iterator(); i.hasNext(); ) {
            PreviewUpdater updater = i.next();
            if (updater != null) {
                RenderedImage image =
                    (this.image != null) ? this.image.get() : null;
                if (image != null) {
                    i.remove();
                    updater = new PreviewUpdater(updater, image, meta);
                    newRefs.add(updater);
                }
            }
        }
        previews.addAll(newRefs);
    }

    private void notifyImageObservers() {
        for (ImageDatumObserver observer : observers) {
            observer.imageChanged(this);
        }
    }

    // Detect legacy user-commanded orientation changes, for files that were
    // oriented through the browser before XMP support.
    private int readRotateCache() {
        String key = getRotateKey();
        int rotate = 0;
        if ((cache != null) && cache.contains(key)) {
            try {
                InputStream in = cache.getStreamFor(key);
                try {
                    rotate = in.read();
                }
                finally {
                    in.close();
                }
            }
            catch (IOException e) {
                // rotate defaults to zero
            }
        }
        return rotate;
    }

    // Detect cached orientation changes and migrate them to XMP.  This is
    // called from getMetadata(), and exists for backwards compatibility.
    private void migrateRotateCacheToXmp() {
        int rotate = readRotateCache();
        if (rotate != 0) {
            try {
                ImageInfo info = ImageInfo.getInstanceFor(file);
                ImageMetadata meta = info.getMetadata();
                ImageOrientation orient = meta.getOrientation();
                switch (rotate) {
                    case 1:
                        orient = orient.get90CW();
                        break;
                    case 2:
                        orient = orient.get180();
                        break;
                    case 3:
                        orient = orient.get90CCW();
                }
                meta.setOrientation(orient);
                // Don't let migration clobber a preexisting XMP file.
                File xmpFile = new File(info.getXMPFilename());
                if (! xmpFile.isFile()) {
                    writeToXmp(info);
                    System.out.println(
                        "Migrated rotate cache to XMP for " +
                        file.getAbsolutePath()
                    );
                }
                else {
                    System.out.println(
                        "Rotate cache migration aborted for " +
                        file.getAbsolutePath() +
                        " (" + xmpFile.getAbsolutePath() + " already exists)"
                    );
                }
            }
            catch (Throwable t) {
                // BadImageFileException, IOException, UnknownImageTypeException
                System.err.println(
                    "Failed to migrate rotate cache to XMP for " +
                    file.getAbsolutePath()
                );
                t.printStackTrace();
            }
            String key = getRotateKey();
            try {
                cache.remove(key);
                System.out.println(
                    "Cleared rotate cache to XMP for " + file.getAbsolutePath()
                );
            }
            catch (IOException e) {
                // Try again next time.
                System.err.println(
                    "Failed to clear rotate cache for " + file.getAbsolutePath()
                );
                e.printStackTrace();
            }
        }
    }

    private void writeMetadataCache() {
        if (cache == null) {
            return;
        }
        String metaKey = getMetadataKey();
        ObjectOutputStream out = null;
        try {
            out = new ObjectOutputStream(cache.putToStream(metaKey));
            out.writeObject(meta);
        }
        catch (IOException e) {
            // metadata will be reread next time
            System.err.println("metadata cache error: " + e.getMessage());
        }
        finally {
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException e) {
                    System.err.println(
                        "metadata cache error: " + e.getMessage()
                    );
                }
            }
        }
        String fileTimeKey = getFileTimeCacheKey();
        try {
            out = new ObjectOutputStream(cache.putToStream(fileTimeKey));
            out.writeObject(fileCacheTime);
        }
        catch (IOException e) {
            System.err.println("file time cache error: " + e.getMessage());
        }
        finally {
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException e) {
                    System.err.println(
                        "file time cache error: " + e.getMessage()
                    );
                }
            }
        }
        if (xmpFile == null) {
            return;
        }
        String xmpFileKey = getXmpKey();
        try {
            out = new ObjectOutputStream(cache.putToStream(xmpFileKey));
            out.writeObject(xmpFile);
        }
        catch (IOException e) {
            System.err.println("file time cache error: " + e.getMessage());
        }
        finally {
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException e) {
                    System.err.println(
                        "file time cache error: " + e.getMessage()
                    );
                }
            }
        }
        String xmpFileTimeKey = getXmpFileTimeCacheKey();
        try {
            out = new ObjectOutputStream(cache.putToStream(xmpFileTimeKey));
            out.writeObject(xmpFileCacheTime);
        }
        catch (IOException e) {
            System.err.println("XMP file time cache error: " + e.getMessage());
        }
        finally {
            if (out != null) {
                try {
                    out.close();
                }
                catch (IOException e) {
                    System.err.println(
                        "XMP file time cache error: " + e.getMessage()
                    );
                }
            }
        }
    }

    private void readMetadataCache() {
        if (cache == null) {
            return;
        }
        String fileTimeKey = getFileTimeCacheKey();
        if (cache.contains(fileTimeKey)) {
            ObjectInputStream oin = null;
            try {
                InputStream in = cache.getStreamFor(fileTimeKey);
                if (in != null) {
                    oin = new ObjectInputStream(in);
                    fileCacheTime = (Long) oin.readObject();
                }
            }
            catch (IOException e) {
                fileCacheTime = 0;
            }
            catch (ClassNotFoundException e) {
                fileCacheTime = 0;
            }
            finally {
                if (oin != null) {
                    try {
                        oin.close();
                    }
                    catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
        String xmpFileKey = getXmpKey();
        if (cache.contains(xmpFileKey)) {
            ObjectInputStream oin = null;
            try {
                InputStream in = cache.getStreamFor(xmpFileKey);
                if (in != null) {
                    oin = new ObjectInputStream(in);
                    xmpFile = (File) oin.readObject();
                }
            }
            catch (IOException e) {
                xmpFile = null;
            }
            catch (ClassNotFoundException e) {
                xmpFile = null;
            }
            finally {
                if (oin != null) {
                    try {
                        oin.close();
                    }
                    catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
        if (xmpFile != null) {
            String xmpFileTimeKey = getXmpFileTimeCacheKey();
            if (cache.contains(xmpFileTimeKey)) {
                ObjectInputStream oin = null;
                try {
                    InputStream in = cache.getStreamFor(xmpFileTimeKey);
                    if (in != null) {
                        oin = new ObjectInputStream(in);
                        xmpFileCacheTime = (Long) oin.readObject();
                    }
                }
                catch (IOException e) {
                    xmpFileCacheTime = 0;
                }
                catch (ClassNotFoundException e) {
                    xmpFileCacheTime = 0;
                }
                finally {
                    if (oin != null) {
                        try {
                            oin.close();
                        }
                        catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }
        }
        else {
            xmpFileCacheTime = 0;
        }
        String metaKey = getMetadataKey();
        if (cache.contains(metaKey)) {
            ObjectInputStream oin = null;
            try {
                InputStream in = cache.getStreamFor(metaKey);
                if (in != null) {
                    oin = new ObjectInputStream(in);
                    meta = (ImageMetadata) oin.readObject();
                }
            }
            catch (IOException e) {
                // getMetadata() will fall back to parsing out metadata
            }
            catch (ClassNotFoundException e) {
                // getMetadata() will fall back to parsing out metadata
            }
            finally {
                if (oin != null) {
                    try {
                        oin.close();
                    }
                    catch (IOException e) {
                        // ignore
                    }
                }
            }
        }
    }

    private void clearMetadataCache() {
        if (cache == null) {
            return;
        }
        String key = getMetadataKey();
        if (cache.contains(key)) {
            try {
                cache.remove(key);
            }
            catch (IOException e) {
                System.err.println(
                    "metadata cache clear error: " + e.getMessage()
                );
            }
        }
        String fileTimeKey = getFileTimeCacheKey();
        if (cache.contains(fileTimeKey)) {
            try {
                cache.remove(fileTimeKey);
            }
            catch (IOException e) {
                System.err.println(
                    "metadata cache clear error: " + e.getMessage()
                );
            }
        }
        String xmpFileKey = getXmpKey();
        if (cache.contains(xmpFileKey)) {
            try {
                cache.remove(xmpFileKey);
            }
            catch (IOException e) {
                System.err.println(
                    "metadata cache clear error: " + e.getMessage()
                );
            }
        }
        if (xmpFile == null) {
            return;
        }
        String xmpFileTimeKey = getXmpFileTimeCacheKey();
        if (cache.contains(xmpFileTimeKey)) {
            try {
                cache.remove(xmpFileTimeKey);
            }
            catch (IOException e) {
                System.err.println(
                    "metadata cache clear error: " + e.getMessage()
                );
            }
        }
    }

    // The cache key for the rotate value.
    private String getRotateKey() {
        StringBuffer buffer = new StringBuffer();
        buffer.append(file.getAbsolutePath());
        buffer.append("_rotate");
        return buffer.toString();
    }

    // The cache key for metadata.
    private String getMetadataKey() {
        long time = Math.max(fileCacheTime, xmpFileCacheTime);
        return file.getAbsolutePath() + "_" + time;
    }

    private String getXmpKey() {
        return file.getAbsolutePath() + "_xmp_file";
    }

    // Observe modification times for file and xmpFile.  These times are used
    // for modification polling in ImageListPoller and also to timestamp
    // cached metadata.
    private void updateFileTimes() {
        if (cache == null) {
            return;
        }
        fileCacheTime = file.lastModified();
        if (xmpFile != null) {
            xmpFileCacheTime = xmpFile.lastModified();
        }
        else {
            xmpFileCacheTime = 0;
        }
    }

    private String getFileTimeCacheKey() {
        return file.getAbsolutePath() + "_cache_time";
    }

    private String getXmpFileTimeCacheKey() {
        return xmpFile.getAbsolutePath() + "_cache_time";
    }

    private void clearPreview() {
        PreviewUpdater.clearCachedPreviewForImage(meta, cache);
    }

    private void logMetadataError(Throwable t) {
        StringBuffer buffer = new StringBuffer();
        buffer.append(file.getAbsolutePath());
        buffer.append(" reading metadata ");
        buffer.append(t.getClass().getName());
        if (t.getMessage() != null) {
            buffer.append(": ");
            buffer.append(t.getMessage());
        }
        System.err.println(buffer);
    }

    private void writeToXmp(ImageInfo info)
        throws IOException, BadImageFileException, UnknownImageTypeException
    {
        info.getImageType().writeMetadata(info);
        try {
            this.xmpFile = new File(info.getXMPFilename());
        }
        catch (Throwable e) {
            logMetadataError(e);
            this.xmpFile = null;
        }
    }
}
