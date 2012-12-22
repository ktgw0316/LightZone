/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import java.awt.*;

import com.lightcrafts.utils.awt.NoInputEventQueue;

/**
 * A <code>CocoaMainThreadRunner</code> is used to run a {@link Runnable}'s
 * <code>run()</code> method on the main Cocoa thread.
 */
public final class CocoaMainThreadRunner {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Run the given {@link Runnable}'s <code>run()</code> method on the main
     * Cocoa thread and wait until it's done.
     *
     * @param runnable The {@link Runnable} whose <code>run()</code> method to
     * run.
     */
    public static void invokeAndWait( Runnable runnable ) {
        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        final EventQueue queue = toolkit.getSystemEventQueue();
        queue.push( NoInputEventQueue.INSTANCE );
        try {
            perform( runnable );
        }
        finally {
            NoInputEventQueue.drain();
            NoInputEventQueue.drain();
            NoInputEventQueue.drain();
            NoInputEventQueue.INSTANCE.pop();
        }
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * This is <code>private</code> to prevent construction of objects.
     */
    private CocoaMainThreadRunner() {
        // do nothing
    }

    /**
     * Run the given {@link Runnable}'s <code>run()</code> method on the main
     * Cocoa thread and wait until it's done.
     *
     * @param runnable The {@link Runnable} whose <code>run()</code> method to
     * run.
     */
    public static native void perform( Runnable runnable );

    static {
        System.loadLibrary( "MacOSX" );
    }
}
/* vim:set et sw=4 ts=4: */
