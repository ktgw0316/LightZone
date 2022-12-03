/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.thread;

/**
 * A <code>CancelableThread</code> simulates a {@link Thread} that provides a
 * safe way to cancel a thread.  It must be used in conjunction with
 * {@link CancelableThreadMonitor}.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class CancelableThread implements Runnable {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Checks whether this thread has been requested to have been canceled.
     * Derived classes are expected to poll this and commit suicide if this
     * method returns <code>true</code>.  Alternatively, {@link #cancel()} may
     * be overridden.
     *
     * @return Returns <code>true</code> only if it has been canceled.
     */
    public final synchronized boolean isCanceled() {
        return m_canceled;
    }

    /**
     * Request that this thread be canceled.
     */
    public final synchronized void requestCancel() {
        //
        // Ensure that cancel() is called at most once regardless of the number
        // of times this method is called.
        //
        final boolean oldCanceled = m_canceled;
        m_canceled = true;
        if ( !oldCanceled )
            cancel();
    }

    /**
     * Run this thread.
     */
    public abstract void run();

    ////////// protected //////////////////////////////////////////////////////

    /**
     * As an alternative to polling {@link #isCanceled()}, a derived class may
     * override this method so that it will be assertively requested to cancel.
     * This method will be called at most once regardless of the number of
     * times {@link #requestCancel()} is called.
     */
    protected void cancel() {
        // do nothing by default
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Whether this thread was requested to be canceled.
     */
    private boolean m_canceled;
}
/* vim:set et sw=4 ts=4: */
