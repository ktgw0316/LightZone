/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import com.lightcrafts.image.ImageFileFilter;
import com.lightcrafts.utils.file.FileUtil;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.filecache.FileCache;

import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * A dynamic data model for an image browser, consisting of a sorted list of
 * images that is kept in sync with all the image files in a given directory.
 * The list changes asynchronously all the time as metadata are discovered,
 * the sort order is updated, thumbnails are computed, resized, and rotated,
 * and files appear and disappear from the directory.
 * <p>
 * The ImageDatum List is touched from three threads:
 * <ol>
 *   <li>the event thread (to update displays);</li>
 *   <li>the polling thread (responding to file modifications);</li>
 *   <li>from the task thread (as background tasks complete).</l>
 * <ol>
 * <p>
 * It is presumed that all public access to this class happens on the event
 * thread, and it is promised that all ImageListListener notifications will
 * also run on that thread.
 */
public class ImageList {

    // The List of ImageDatums.
    private List<ImageDatum> list;

    // The uniform size for ImageDatum images.
    private int size;

    // A task queue, where ImageDatum updates are run serially
    private ImageTaskQueue queue;

    // A cache for costly image data, used in the ImageDatums
    private FileCache cache;

    // A thread that monitors for files added, removed, and modified
    private ImageListPoller poller;

    // The Comparator that defines the ordering of ImageDatums
    private ImageDatumComparator comp;

    // Sort order is also partly determined by ImageGroups, defined by this
    private ImageGroupProvider groups;

    // Listeners for add/remove/change of ImageDatums
    private LinkedList<ImageListListener> listeners;

    // Calls to pause() and resume() are balanced.
    private int pauseDepth;

    // True if the constructor scan was cancelled before it completed.
    private boolean wasCancelled;

    // The cancel() method can halt the directory scan in the constructor
    private static boolean cancel;

    /**
     * Construct an ImageList and initialize it from files in the given
     * directory.
     * <p>
     * The files will be scanned for metadata, which can take a long time.
     * The ProgressIndicator argument can provide feedback.
     * <p>
     * The background threads (for modification polling and thumbnailing)
     * are not started here.  See start().
     * @param directory A directory of image files to use.
     * @param size The initial size for thumbnail images.  See setSize().
     * @param cache A FileCache where thumbnail data can be saved.  May be
     * null.
     * @param useCache A flag to indicate whether the given FileCache should
     * be trusted to initialize the ImageList, or rather all data derived from
     * image files should be recomputed in this constructor.
     * @param comp The initial Comparator for sorting images.  See setSort().
     * @param progress A ProgressIndicator for feedback about the initial
     * metadata scan.
     */
    public ImageList(
        File directory,
        int size,
        FileCache cache,
        boolean useCache,
        ImageDatumComparator comp,
        ProgressIndicator progress
    ) {
        this.size = size;
        this.cache = cache;
        this.comp = comp;

        list = Collections.synchronizedList(new LinkedList<ImageDatum>());

        queue = new ImageTaskQueue();
        poller = new ImageListPoller(this, directory);

        listeners = new LinkedList<ImageListListener>();

        synchronized(list) {
            // Perform a synchronous, one-pass scan of the directory, to
            // preempt an avalanche of access to the ImageList when the
            // ImageTaskQueue thread starts.
            File[] files =
                FileUtil.listFiles(directory, ImageFileFilter.INSTANCE, false);
            if (files != null) {
                progress.setMinimum(0);
                progress.setMaximum(files.length);
                for (int n=0; n<files.length && !cancel; n++) {
                    File file = files[n];
                    ImageDatum datum = new ImageDatum(file, size, queue, cache);
                    // Just get metadata, let painting pull out thumbnails:
                    datum.getMetadata(useCache);
                    list.add(datum);
                    progress.incrementBy(1);
                }
            }
            else {
                cancel = true;
            }
            if (cancel) {
                queue.removeAllTasks();
                list.clear();
                wasCancelled = true;
                cancel = false;
            }
            sort(); // requires metadata, but not thumbnails
        }
    }

    /**
     * Cancel the scan in a running constructors.
     */
    public static void cancel() {
        cancel = true;
    }

    /**
     * Bring this ImageList to life, commencing ImageDatum updates.
     */
    public void start() {
        if (! wasCancelled) {
            queue.start();
            poller.start();
        }
    }

    /**
     * Pause dynamic updates.  Background work will cease as soon as
     * the current task completes.  File modification polling will cease
     * as soon as the current pass is completed.
     */
    public void pause() {
        // Debug code for checking pause/resume balance.
//        showPauseContext();
        if (pauseDepth++ == 0) {
            if (! wasCancelled) {
                queue.pause();
                poller.pause();
            }
        }
    }

    /**
     * Resume dynamic updates and modification polling.
     */
    public void resume() {
        // Debug code for checking pause/resume balance.
//        showResumeContext();
        assert (pauseDepth > 0) : "Unbalanced browser resume";
        if (--pauseDepth == 0) {
            if (! wasCancelled) {
                queue.resume();
                poller.resume();
            }
        }
    }

    /**
     * The pause depth must be externally accessible, because the life cycle
     * of a browser's container may be greater than the life cycle of the
     * browser itself.  (The current folder may change while this ImageList
     * is paused.)
     */
    public int getPauseDepth() {
        return pauseDepth;
    }

    /**
     * Halt all background processing in this ImageList forever.  This method
     * must be called or the background threads will leak.
     */
    public void stop() {
        if (! wasCancelled) {
            queue.stop();
            poller.stop();
        }
    }

    /**
     * Tell if someone called cancel() to interrupt this ImageList's constructor
     * scan.  If true, then this ImageList has inactive threads and contains no
     * images.
     */
    public boolean wasCancelled() {
        return wasCancelled;
    }

    public void addImageListListener(ImageListListener listener) {
        listeners.add(listener);
    }

    public void removeImageListListener(ImageListListener listener) {
        listeners.remove(listener);
    }

    /**
     * Specify an ImageDatumComparator to define the ordering of this ImageList.
     * Immediately triggers a sort of the list.
     */
    public void setSort(ImageDatumComparator comp) {
        this.comp = comp;
        sort();
        notifyReordered();
    }

    /**
     * Invert the current sort order.  Immediately triggers a sort of the list.
     */
    public void setSortInverted(boolean inverted) {
        comp.setReversed(inverted);
        sort();
        notifyReordered();
    }

    /**
     * Signal this ImageList that its ImageGroupProvider may have something
     * new to say about how ImageGroups should be defined.
     */
    public void regroup() {
        sort();
        notifyReordered();
    }

    public void setImageGroupProvider(ImageGroupProvider groups) {
        this.groups = groups;
        sort();
        notifyReordered();
    }

    /**
     * Specify a new thumbnal size.  This immediately queues a background task
     * for each image.
     */
    public void setSize(int size) {
        this.size = size;
        synchronized(list) {
            for (ImageDatum datum : list) {
                datum.setSize(size);
            }
        }
    }

    /**
     * Get a snapshot of the List of ImageDatums in this ImageList.  Note that
     * this List is continuously updated on multiple threads.  The snapshot
     * may be stale by the time you get it.  See ImageListListener.
     */
    public ArrayList<ImageDatum> getAllImageData() {
        synchronized(list) {
            return new ArrayList<ImageDatum>(list);
        }
    }

    /**
     * Get the current size of thumbnail images, for presentation and layout.
     */
    public int getSize() {
        return size;
    }

    public void addQueueListener(ImageTaskQueueListener listener) {
        queue.addListener(listener);
    }

    public void removeQueueListener(ImageTaskQueueListener listener) {
        queue.removeListener(listener);
    }

    // Used in ImageListPoller when a new File is discovered.
    void addFile(File file) {
        final ImageDatum datum = new ImageDatum(file, size, queue, cache);
        datum.refresh(false); // reads metadata, enqueues thumbnailing
        synchronized(list) {
            list.add(datum);
            sort();
            final int index = list.indexOf(datum);
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        notifyAdded(datum, index);
                    }
                }
            );
        }
    }

    // Used in ImageListPoller when a File corresponding to an ImageDatum
    // no longer exists.
    void removeImageData(final ImageDatum datum) {
        synchronized(list) {
            final int index = list.indexOf(datum);
            list.remove(datum);
            sort();
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        notifyRemoved(datum, index);
                    }
                }
            );
        }
    }

    // Called from ImageListPoller when metadata update, to update the sort.
    void metadataChanged(final ImageDatum datum) {
        final int oldIndex, newIndex;
        synchronized(list) {
            oldIndex = list.indexOf(datum);
            sort();
            newIndex = list.indexOf(datum);
        }
        if (oldIndex != newIndex) {
            EventQueue.invokeLater(
                new Runnable() {
                    public void run() {
                        notifyReordered();
                    }
                }
            );
        }
    }

    // Sorting means first determining the ImageGroups, then applying the
    // ImageDatumComparator to sort the ImageGroup leaders, then finally
    // placing all the other ImageGroup members by their respective leaders.
    private void sort() {
        synchronized(list) {
            // Group ImageDatums:
            if (groups != null) {
                groups.cluster(list);
            }
            // Test code, to discover if the ImageGroupProvider is
            // generating bogus ImageGroup assignments:
//            ImageGroup.checkConsistency(list);

            // Identify the distinct ImageGroups:
            Set<ImageGroup> groups = new HashSet<ImageGroup>();
            for (ImageDatum datum : list) {
                ImageGroup group = datum.getGroup();
                groups.add(group);
            }
            // Put the leaders into a sortable list:
            List<ImageDatum> leaders = new LinkedList<ImageDatum>();
            for (ImageGroup group : groups) {
                ImageDatum leader = group.getLeader();
                if (leaders.contains(leader)) {
                    System.out.println("redundant leader");
                }
                leaders.add(leader);
            }
            // Sort the leaders:
            Collections.sort(leaders, comp);

            // Rebuild the list, putting members by leaders:
            list.clear();
            for (ImageDatum leader : leaders) {
                list.add(leader);
                ImageGroup group = leader.getGroup();
                List<ImageDatum> members = group.getImageDatums();

                // Sort group members by file modification time:
                Collections.sort(
                    members,
                    new Comparator<ImageDatum>() {
                        public int compare(ImageDatum left, ImageDatum right) {
                            File leftFile = left.getFile();
                            File rightFile = right.getFile();
                            long leftTime = leftFile.lastModified();
                            long rightTime = rightFile.lastModified();
                            return (int) Math.signum(leftTime - rightTime);
                        }
                    }
                );
                for (ImageDatum member : members) {
                    if (! member.equals(leader)) {
                        list.add(member);
                    }
                }
            }
        }
    }

    private void notifyAdded(ImageDatum datum, int index) {
        for (ImageListListener listener : listeners) {
            listener.imageAdded(this, datum, index);
        }
    }

    private void notifyRemoved(ImageDatum datum, int index) {
        for (ImageListListener listener : listeners) {
            listener.imageRemoved(this, datum, index);
        }
    }

    private void notifyReordered() {
        for (ImageListListener listener : listeners) {
            listener.imagesReordered(this);
        }
    }

    // Debug code for checking pause/resume balance.
    private void showPauseContext() {
        System.out.println("PAUSE " + (pauseDepth + 1));
        System.out.println('\t' + Thread.currentThread().getName());
        System.out.println('\t' + this.toString());
        Throwable t = new Throwable();
        StackTraceElement[] stack = t.getStackTrace();
        int n = 0;
        for (StackTraceElement frame : stack) {
            if (++n < 4) {
                continue;
            }
            String name = frame.getClassName();
            if (name.contains("java.awt.") || name.contains("javax.swing.")) {
                continue;
            }
            System.out.println("\tat " + frame);
        }
    }

    // Debug code for checking pause/resume balance.
    private void showResumeContext() {
        System.out.println("RESUME " + pauseDepth);
        System.out.println('\t' + Thread.currentThread().getName());
        System.out.println('\t' + this.toString());
        Throwable t = new Throwable();
        StackTraceElement[] stack = t.getStackTrace();
        int n = 0;
        for (StackTraceElement frame : stack) {
            if (++n < 4) {
                continue;
            }
            String name = frame.getClassName();
            if (name.contains("java.awt.") || name.contains("javax.swing.")) {
                continue;
            }
            System.out.println("\tat " + frame);
        }
    }
}
