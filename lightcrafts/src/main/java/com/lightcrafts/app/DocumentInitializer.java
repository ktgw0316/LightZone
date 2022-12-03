/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.UserCanceledException;
import com.lightcrafts.utils.thread.ProgressThread;

import java.awt.*;
import java.io.File;

/**
 * This class takes a request for the time-consuming Document construction
 * procedure Application.createDocument(), fires up the method in the
 * background, and is always ready to cancel the job and start a new one when
 * the next request arrives.
 * <p>
 * This class is only to be accessed on the event dispatch thread.
 */
class DocumentInitializer {

    private static final Object Monitor = new Object();
    private static ProgressThread Cancellable;
    private static boolean IsRunning;

    static void createDocument(
        File file,
        ComboFrame frame,
        DocumentInitializerListener listener
    ) {
        synchronized(Monitor) {
            if (IsRunning) {

                // Shunt away the cancel mechanism, because the cancel
                // surfaces as an IOException from the Engine constructor,
                // and just do nothing instead.
                return;

//                Cancellable.requestCancel();
//                while (IsRunning) {
//                    try {
//                        Monitor.wait();
//                    }
//                    catch (InterruptedException e) {
//                        // just continue
//                    }
//                }
            }
            createDocumentInBackground(file, frame, listener);
        }
    }

    private static void createDocumentInBackground(
        final File file,
        final ComboFrame frame,
        final DocumentInitializerListener listener
    ) {
        synchronized(Monitor) {
            Cancellable = new ProgressThread(DummyIndicator) {
                public void run() {
                    try {
                        frame.showWait(LOCALE.get("LoadMessage"));
                        notifyListenerStart(listener);
                        Document doc = Application.createDocument(
                            file, frame, Cancellable
                        );
                        notifyListener(doc, listener);
                    }
                    catch (Throwable t) {
                        notifyListener(t, listener);
                    }
                    finally {
                        frame.hideWait();
                    }
                    synchronized(Monitor) {
                        Monitor.notifyAll();
                        Cancellable = null; // for GC
                        IsRunning = false;
                    }
                    // Tell the next guy that we're done.
                }
            };
            Thread thread = new Thread(Cancellable, "Document Initialization");
            thread.start();
            IsRunning = true;
        }
    }

    private static void notifyListenerStart(
        final DocumentInitializerListener listener
    ) {
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    listener.documentStarted();
                }
            }
        );
    }

    private static void notifyListener(
        final Document doc, final DocumentInitializerListener listener
    ) {
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    listener.documentInitialized(doc);
                }
            }
        );
    }

    private static void notifyListener(
        final Throwable t, final DocumentInitializerListener listener
    ) {
        EventQueue.invokeLater(
            new Runnable() {
                public void run() {
                    if (t instanceof UserCanceledException) {
                        listener.documentCancelled();
                    }
                    else {
                        listener.documentFailed(t);
                    }
                }
            }
        );
    }

    private static ProgressIndicator DummyIndicator = new ProgressIndicator() {
        public void incrementBy(int delta) {
        }
        public void setIndeterminate(boolean indeterminate) {
        }
        public void setMaximum(int maxValue) {
        }
        public void setMinimum(int minValue) {
        }
    };
}
