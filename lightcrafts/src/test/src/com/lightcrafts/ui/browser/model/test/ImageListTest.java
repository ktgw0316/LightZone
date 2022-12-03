/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.model.test;

import com.lightcrafts.ui.browser.model.*;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.filecache.FileCacheFactory;

import java.io.File;
import java.util.Iterator;
import java.util.List;

/**
 * This is a basic test of ImageList, the data model for the new browser.
 * It takes a directory and initializes an ImageList, which is a long blocking
 * operation with ProgressIndicator feedback.  It then attaches a listener to
 * log all the thumbnail completions, and another listener to report detected
 * file modifications.
 */
public class ImageListTest {

    public static void main(String[] args) {

        // Chew on files in this directory:
        File folder = new File(args[0]);

        // Scan the directory for image files, reporting as the metadata
        // are read out:
        ImageList list = new ImageList(
            folder,
            100,
            FileCacheFactory.get(folder),
            true,
            ImageDatumComparator.Name,
            new ProgressIndicator() {
                int count;
                public void incrementBy(int delta) {
                    count += delta;
                    System.out.println(count);
                }
                public void setIndeterminate(boolean indeterminate) {
                }
                public void setMaximum(int maxValue) {
                }
                public void setMinimum(int minValue) {
                }
            }
        );
        // Monitor every ImageDatum for thumbnail updates:
        List datums = list.getAllImageData();
        for (Iterator i=datums.iterator(); i.hasNext(); ) {
            ImageDatum datum = (ImageDatum) i.next();
            datum.getImage(
                new ImageDatumObserver() {
                    public void imageChanged(ImageDatum datum) {
                        File file = datum.getFile();
                        System.out.println("image " + file.getName());
                    }
                }
            );
        }
        // Listen for changes triggered by the file system poller:
        list.addImageListListener(
            new ImageListListener() {
                public void imageAdded(ImageList source, ImageDatum datum, int index) {
                    System.out.println("added" + datum.getFile().getName());
                }
                public void imageRemoved(ImageList source, ImageDatum datum, int index) {
                    System.out.println("removed" + datum.getFile().getName());
                }
                public void imagesReordered(ImageList source) {
                    System.out.println("sort changed");
                }
            }
        );
        list.start();
    }
}
