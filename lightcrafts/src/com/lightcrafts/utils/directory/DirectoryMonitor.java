/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.directory;

import java.io.File;
import java.util.*;

/**
 * A <code>DirectoryMonitor</code> is a class that monitors a collection of
 * directories in the filesystem for changes.  Upon a change, a collection of
 * previously registered listeners is notified.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public abstract class DirectoryMonitor {

    static final boolean DEBUG = false;

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Add a directory to be monitored.  Adding the same directory more than
     * once is guaranteed to be harmless.
     *
     * @param directory The directory to be monitored.
     */
    public abstract void addDirectory( File directory );

    /**
     * Add a {@link DirectoryListener} to the collection of
     * {@link DirectoryListener}s that receive notifications that the monitored
     * collection of directories have changed.
     *
     * @param listener The {@link DirectoryListener} to add.
     */
    public final void addListener( DirectoryListener listener ) {
        synchronized ( m_listeners ) {
            m_listeners.add( listener );
        }
    }

    /**
     * Dispose of this <code>DirectoryMonitor</code> by stopping its thread.
     */
    public void dispose() {
        m_monitorThread.stopMonitoring();
    }

    /**
     * Resume monitoring of and notification about directories (but only if the
     * call to <code>resume()</code> has balanced all previous calls to
     * {@link #suspend()}.
     *
     * @param force If <code>true</code>, force resumption and monitoring and
     * notification.
     */
    public final synchronized void resume( boolean force ) {
        if ( force || --m_suspendCount < 0 )
            m_suspendCount = 0;
        //noinspection ConstantConditions
        if ( DEBUG )
            System.out.println(
                "DirectoryMonitor: resuming (" + m_suspendCount + ')'
            );
    }

    /**
     * Remove a directory from being monitored.
     *
     * @param directory The directory to remove.
     * @return Returns <code>true</code> only if the directory was being
     * monitored and thus removed.
     */
    public abstract boolean removeDirectory( File directory );

    /**
     * Remove a {@link DirectoryListener} from receiving notifications about
     * changed directories.
     *
     * @param listener The {@link DirectoryListener} to remove.
     * @return Returns <code>true</code> only if the {@link DirectoryListener}
     * was removed.
     */
    public final boolean removeListener( DirectoryListener listener ) {
        synchronized ( m_listeners ) {
            return m_listeners.remove( listener );
        }
    }

    /**
     * Suspend monitoring of and notification about directories.  This method
     * may be called multiple times.  Every call must be balanced by a call to
     * {@link #resume(boolean)} in order for monitoring and notification to
     * resume.
     */
    public final synchronized void suspend() {
        ++m_suspendCount;
        //noinspection ConstantConditions
        if ( DEBUG )
            System.out.println(
                "DirectoryMonitor: suspending (" + m_suspendCount + ')'
            );
    }

    ////////// package ///////////////////////////////////////////////////////

    /**
     * This method is just like {@link Thread#sleep(long)} except it handles
     * the annoying {@link InterruptedException}.
     *
     * @param millis The length of time to sleep in milliseconds.
     */
    static void doze( long millis ) {
        try {
            Thread.sleep( millis );
        }
        catch ( InterruptedException e ) {
            // ignore
        }
    }

    ////////// protected //////////////////////////////////////////////////////

    /**
     * Finalize this <code>DirectoryMonitor</code> by calling
     * {@link #dispose()}.
     */
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    /**
     * Gets the set of monitored directories.
     *
     * @return Returns an array of said directories.
     */
    protected abstract File[] getMonitoredDirectories();

    /**
     * Checks whether the given directory has changed.
     *
     * @param directory The directory to check.  It must have been previously
     * added via {@link #addDirectory(File)}.
     * @return Returns <code>true</code> only if the directory changed.
     */
    protected abstract boolean hasChanged( File directory );

    /**
     * Start monitoring.  This must be called by derived class constructors.
     * This can not be put into this class's constructor because then the
     * thread will start before the derived class has finished construction.
     */
    protected void start() {
        m_monitorThread.start();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A <code>MonitorThread</code> is-a {@link Thread} that monitors a
     * collection of directories for changes.
     */
    private final class MonitorThread extends Thread {

        ////////// public /////////////////////////////////////////////////////

        /**
         * Monitor all the requested directories for changes.
         */
        public void run() {
            while ( !m_stop ) {
                final int suspendCount;
                synchronized ( DirectoryMonitor.this ) {
                    //
                    // Make a copy of the current value of m_suspendCount so
                    // the DirectoryMonitor object isn't locked the entire time
                    // the directories are being checked below.
                    //
                    suspendCount = m_suspendCount;
                }
                if ( suspendCount == 0 ) {
                    for ( File dir : getMonitoredDirectories() )
                        if ( hasChanged( dir ) )
                            notifyListenersAbout( dir );
                }
                doze( 3000 );
            }
        }

        ////////// package ////////////////////////////////////////////////////

        /**
         * Stop this thread.
         */
        void stopMonitoring() {
            m_stop = true;
        }

        ////////// private ////////////////////////////////////////////////////

        /**
         * Construct a <code>MonitorThread</code>.
         */
        private MonitorThread() {
            super( "DirectoryMonitor.MonitorThread" );
            setDaemon( true );
            setPriority( MIN_PRIORITY );
        }

        /**
         * A flag to indicate when this thread should stop.
         */
        private boolean m_stop;
    }

    /**
     * Notify all the listeners that a directory has changed.  The listeners
     * should check whether the directory still exists.
     *
     * @param dir The directory to notify about.
     */
    private void notifyListenersAbout( File dir ) {
        synchronized ( m_listeners ) {
            for ( DirectoryListener listener : m_listeners )
                listener.directoryChanged( dir );
        }
    }

    /**
     * The collection of listeners to notify whenever any monitored directory
     * changes.
     */
    private final Collection<DirectoryListener> m_listeners =
        new ArrayList<DirectoryListener>();

    /**
     * The <code>MonitorThread</code> we're using.
     */
    private final MonitorThread m_monitorThread = new MonitorThread();

    /**
     * When greater than zero, checking and notification is suspended.
     */
    private int m_suspendCount;
}
/* vim:set et sw=4 ts=4: */
