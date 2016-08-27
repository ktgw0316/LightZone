/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.awt;

import java.awt.EventQueue;
import java.util.EmptyStackException;
import java.lang.reflect.InvocationTargetException;

/**
 * A <code>PoppableEventQueue</code> is-an {@link EventQueue} that merely
 * makes the ordinarily <code>protected</code> method <code>pop()</code>
 * <code>public</code>.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public class PoppableEventQueue extends EventQueue {

    /**
     * {@inheritDoc}
     */
    public void pop() throws EmptyStackException {
        super.pop();
    }

    // Enqueue a placeholder task and wait until it (and all preceding tasks)
    // are dequeued.  This helps prevent tasks from running after the pop().
    public static void drain() {
        if (EventQueue.isDispatchThread()) {
            throw new IllegalThreadStateException(
                "Can't drain the event queue from the event thread."
            );
        }
        try {
            EventQueue.invokeAndWait(
                new Runnable() {
                    public void run() {
                        // do nothing
                    }
                }
            );
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
/* vim:set et sw=4 ts=4: */