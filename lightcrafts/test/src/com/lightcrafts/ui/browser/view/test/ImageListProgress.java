/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.browser.view.test;

import com.lightcrafts.platform.Platform;
import com.lightcrafts.platform.ProgressDialog;
import com.lightcrafts.ui.browser.model.ImageDatumComparator;
import com.lightcrafts.ui.browser.model.ImageList;
import com.lightcrafts.utils.TerseLoggingHandler;
import com.lightcrafts.utils.filecache.FileCacheFactory;
import com.lightcrafts.utils.thread.ProgressThread;

import java.awt.*;
import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * A static utility just for initializing an ImageList, including: library
 * loading, cache initialization, logger initialization, and a progress dialog
 * during the metadata scan.  Used in the ImageList display tests.
 */
public class ImageListProgress {

    private static ImageList Images;

    public static ImageList createImageList(
        final Frame frame, final File directory
    ) {
        System.loadLibrary("DCRaw");
        System.loadLibrary("LCJPEG");
        System.loadLibrary("LCTIFF");

        // Abbreviate metadata error messages, which can be scroll blinding.
        Logger logger = Logger.getLogger("com.lightcrafts.image.metadata");
        Handler handler = new TerseLoggingHandler(System.out);
        logger.addHandler(handler);
        logger.setUseParentHandlers(false);

        ProgressDialog dialog = Platform.getPlatform().getProgressDialog();
        ProgressThread thread = new ProgressThread(dialog) {
            public void run() {
                Images = new ImageList(
                    directory,
                    100,
                    FileCacheFactory.get(directory),
                    true,
                    ImageDatumComparator.CaptureTime,
                    getProgressIndicator()
                );
            }
        };
        dialog.showProgress(frame, thread, "Scanning...", 0, 1, false);
        Throwable thrown = dialog.getThrown();
        if (thrown != null) {
            throw new RuntimeException("Error initializing ImageList", thrown);
        }
        return Images;
    }

    public static void main(String[] args) {
        File folder = new File(args[0]);
        createImageList(null, folder);
    }
}