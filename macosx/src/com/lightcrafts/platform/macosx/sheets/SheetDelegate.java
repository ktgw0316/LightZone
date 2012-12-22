/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx.sheets;

import com.lightcrafts.utils.awt.NoInputEventQueue;

import java.awt.*;

/**
 * A <code>SheetDelegate</code> is used to show a Mac&nbsp;OS&nbsp;X
 * &quot;sheet&quot; to the user and wait until the user is done with it.  This
 * is done in such a way as not to block the AWT event thread.  All sheets
 * should be shown via this class.
 * <p>
 * In pure-Java dialogs, the call to Dialog.show() blocks until the dialog
 * is disposed.  To allow even dispatch during a call to Dialog.show() on the
 * event thread, Dialog.show() activates its own even pump.  So to remain
 * compatible with pure-Java dialogs, this class' <code>showAndWait()</code>
 * must do the same thing.
 * <p>
 * It does this by pushing a fresh EventQueue onto the EventQueue stack
 * before showing the sheet.  To avoid leaking EventQueues (and their
 * corresponding EventThreads), these EventQueues must be popped.  But it
 * doesn't work to pop the thread on the same EventQueue task that performed
 * the push.  First, experience has shown that calling push and pop on the
 * same task leaks EventThreads.  And second, since the native sheets are
 * modal with respect to their window only, there is no guarantee that sheets
 * will be disposed in any particular order, so pushes and pops can not be
 * balanced for each sheet individually.
 * <p>
 * This class therefore maintains a static list of pushed EventQueues.  Each
 * sheet pushes one before starting, and then pops the latest one after it
 * ends on a separate EventQueue task.
 *
 * @author Paul J. Lucas [plucas@lightcrafts.com]
 * @author Anton Kast [anton@lightcrafts.com]
 */
public final class SheetDelegate {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Show a &quot;sheet&quot; and wait until the user is done with it while
     * not blocking either the AWT event or AppKit threads.
     *
     * @param shower The {@link SheetShower} to delegate the actual showing of
     * the sheet to.
     */
    public static void showAndWait( SheetShower shower ) {
        final Toolkit toolkit = Toolkit.getDefaultToolkit();
        final EventQueue queue = toolkit.getSystemEventQueue();
        queue.push( NoInputEventQueue.INSTANCE );
        try {
            shower.showAndWait();
        }
        finally {
            NoInputEventQueue.drain();
            NoInputEventQueue.drain();
            NoInputEventQueue.drain();
            NoInputEventQueue.INSTANCE.pop();
        }
    }

}
/* vim:set et sw=4 ts=4: */
