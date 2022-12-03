/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.thread;

/**
 * A <code>CancelableThreadMonitor</code> is-a {@link Thread} that monitors a
 * {@link CancelableThread}.  Specifically, it will catch any {@link Throwable}
 * it may throw.  It will also optionally notify a listener upon the thread's
 * termination.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class CancelableThreadMonitor extends Thread {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * A <code>Listener</code> is notified when a {@link CancelableThread}
     * terminates.
     */
    public interface Listener {
        /**
         * This method is called when the given {@link CancelableThread} has
         * terminated.
         *
         * @param t The {@link CancelableThread} that terminated.
         */
        void threadTerminated( CancelableThread t );
    }

    /**
     * Construct a <code>CancelableThreadMonitor</code>.
     *
     * @param t The {@link CancelableThread} to monitor.
     * @param listener The {@link Listener} to notify or <code>null</code> for
     * none.
     */
    public CancelableThreadMonitor( CancelableThread t, Listener listener ) {
        super( "CancelableThreadMonitor" );
        m_listener = listener;
        m_monitoredThread = t;
    }

    /**
     * Gets the monitored {@link CancelableThread}.
     *
     * @return Returns said thread.
     */
    public CancelableThread getMonitoredThread() {
        return m_monitoredThread;
    }

    /**
     * Gets whatever {@link Throwable} the {@link CancelableThread} may have
     * thrown during its execution.
     *
     * @return Returns said {@link Throwable} or <code>null</code> if none.
     */
    public Throwable getThrown() {
        return m_thrown;
    }

    /**
     * Checks whether the monitored {@link CancelableThread} has been requested
     * to have been canceled.
     *
     * @return Returns <code>true</code> only if it has been canceled.
     */
    public boolean isCancelled() {
        return m_monitoredThread.isCanceled();
    }

    /**
     * Request that the monitored {@link CancelableThread} be canceled.
     */
    public void requestCancel() {
        m_monitoredThread.requestCancel();
    }

    /**
     * Runs this thread which calls the {@link CancelableThread}'s
     * <code>run()</code> method and waits for it to terminate.  If the
     * {@link CancelableThread} throws any {@link Throwable} during its
     * execution, this can be obtained via {@link #getThrown()}.  Upon
     * {@link CancelableThread} termination, the {@link Listener}, if any, is
     * notified.
     */
    public void run() {
        try {
            //
            // Note that we don't call start() on the monitored thread, but
            // rather call it's run() method directly.  This is so we can catch
            // any Throwable it might throw.  (There's also no reason to start
            // yet another thread.)
            //
            m_monitoredThread.run();
        }
        catch ( Throwable t ) {
            //
            // Since Thread.run() can't throw unchecked exceptions, the only
            // way for a thread to throw an unchecked exception is to wrap it
            // inside a RuntimeException.
            //
            // If we catch a RuntimeException exactly (i.e., not an exception
            // derived from it), assume that it's one that is wrapped around a
            // checked exception.
            //
            // Therefore, strip off all such wrapping RuntimeExceptions
            // replacing them by their causes.
            //
            while ( t.getClass() == RuntimeException.class ) {
                final Throwable cause = t.getCause();
                if ( cause == null )
                    break;
                t = cause;
            }
            m_thrown = t;
        }
        if ( m_listener != null )
            m_listener.threadTerminated( m_monitoredThread );
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The {@link Listener} to notify upon the termination of the
     * {@link CancelableThread}.
     */
    private final Listener m_listener;

    /**
     * The {@link CancelableThread} to monitor.
     */
    private final CancelableThread m_monitoredThread;

    /**
     * The {@link Throwable} that was thrown by the {@link CancelableThread},
     * if any.
     */
    private Throwable m_thrown;
}
/* vim:set et sw=4 ts=4: */
