/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.awt;

import com.lightcrafts.utils.awt.NoInputEventQueue;

import java.awt.*;

/**
 * A static utility to execute tasks in a background way that allows painting
 * and validation while blocking user input, like a modal dialog.
 * <p>
 * Call execute() on the event thread with the task to run.  The task will
 * run synchronously.  A background event pump will dispatch the display
 * events and drop input events until the task completes.
 */
public class BlockingExecutor {

    public static interface BlockingRunnable {
        void run() throws Exception;
    }

    private static Throwable Error;

    public static Throwable execute(final BlockingRunnable task) {
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        EventQueue queue = toolkit.getSystemEventQueue();
        queue.push(NoInputEventQueue.INSTANCE);
        try {
            Error = null;
            task.run();
        }
        catch (Throwable t) {
            Error = t;
        }
        NoInputEventQueue.drain();
        NoInputEventQueue.INSTANCE.pop();

        return Error;
    }
}
