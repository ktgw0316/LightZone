/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx.sheets;

/**
 * A <code>SheetShower</code> is an object that can show a Mac&nbsp;OS&nbsp;X
 * &quot;sheet&quot; and wait for it to be dismissed by the user by clicking a
 * button.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class SheetShower {

    ////////// public ////////////////////////////////////////////////////////

    /**
     * Show a &quot;sheet&quot; and wait until the user is done with it.
     */
    public final synchronized void showAndWait() {
        showSheet();
        while ( !m_done ) {
            try {
                wait();
            }
            catch ( InterruptedException e ) {
                // ignore
            }
        }
    }

    ////////// protected /////////////////////////////////////////////////////

    /**
     * Signal that we're done by virtue of the user clicking a button.
     */
    protected final synchronized void done() {
        m_done = true;
        notify();
    }

    /**
     * Show the sheet.
     */
    protected abstract void showSheet();

    ////////// private ///////////////////////////////////////////////////////

    /**
     * A flag to indicate that we're done, i.e., the user has clicked a button
     * on the sheet.
     */
    private boolean m_done;
}
/* vim:set et sw=4 ts=4: */
