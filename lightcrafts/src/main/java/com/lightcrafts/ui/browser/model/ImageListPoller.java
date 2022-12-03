/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model;

import com.lightcrafts.image.ImageFileFilter;
import com.lightcrafts.utils.file.FileUtil;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Given a directory, this Thread polls image Files for changes in their
 * modification times and triggers appropriate refreshes on ImageDatums.
 */
class ImageListPoller implements Runnable {

    // The wait between scans for modifications, in milliseconds
    private final static long Interval = 3000;

    private ImageList list;

    private File directory;

    private Thread thread;

    private boolean pause;

    private boolean stop;

    ImageListPoller(ImageList list, File directory) {
        thread = new Thread(this, "Image List Poller");
        thread.setPriority(Thread.MIN_PRIORITY);
        this.list = list;
        this.directory = directory;
    }

    public void run() {
        while (! stop) {
            synchronized (this) {
                try {
                    poll();
                    wait(Interval);
                    if (pause) {
                        wait();
                    }
                }
                catch (Throwable e) {
                    if (e instanceof InterruptedException) {
                        // Probably a call to stop()
                        continue;
                    }
                    System.err.println("Error in ImageListPoller:");
                    e.printStackTrace();
                    // Keep going anyway
                }
            }
        }
    }

    void start() {
        if (! thread.isAlive()) {
            stop = false;
            thread.start();
        }
    }

    void stop() {
        stop = true;
        thread.interrupt();
    }

    // Blocks until the poller has paused.
    void pause() {
        synchronized (this) {
            pause = true;
        }
    }

    void resume() {
        synchronized (this) {
            pause = false;
            notifyAll();
        }
    }

    private void poll() {
        List<ImageDatum> data = list.getAllImageData();
        Set<File> listFiles = new HashSet<File>();
        for (Iterator<ImageDatum> i=data.iterator(); i.hasNext() && !stop; )  {
            ImageDatum datum = i.next();
            long oldTime = datum.getFileCacheTime();
            // If oldTime == 0, the datum is still initializing
            File file = datum.getFile();
            listFiles.add(file);
            if (oldTime > 0) {
                if (! file.isFile()) {
                    log("file disappeared", file);
                    list.removeImageData(datum);
                }
                else {
                    long newTime = file.lastModified();
                    if (newTime > oldTime) {
                        log("modification detected", file);
                        datum.refresh(false);
                        list.metadataChanged(datum);
                    }
                }
                File xmpFile = datum.getXmpFile();
                if (xmpFile == null) {
                    // XMP couldn't be determined, probably a metadata error
                    continue;
                }
                long oldXmpTime = datum.getXmpFileCacheTime();
                if (! xmpFile.isFile() && (oldXmpTime > 0)) {
                    log("XMP file disappeared", file);
                    datum.refresh(true);
                    list.metadataChanged(datum);
                }
                else if (xmpFile.lastModified() > oldXmpTime) {
                    log("XMP modification detected", file);
                    datum.refresh(true);
                    list.metadataChanged(datum);
                }
            }
        }
        File[] dirFiles =
            FileUtil.listFiles(directory, ImageFileFilter.INSTANCE, false );
        if (dirFiles != null) {
            for (int n=0; n<dirFiles.length && !stop; n++) {
                File file = dirFiles[n];
                if (! listFiles.contains(file)) {
                    log("file appeared", file);
                    list.addFile(file);
                }
            }
        }
    }

    private static void log(String message, File file) {
        System.out.println(message + " at " + file);
    }
}
