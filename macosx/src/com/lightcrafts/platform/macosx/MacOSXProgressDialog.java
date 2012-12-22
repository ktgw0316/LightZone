/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import java.awt.Frame;

import javax.swing.*;

import com.lightcrafts.platform.ProgressDialog;
import com.lightcrafts.platform.macosx.sheets.SheetShower;
import com.lightcrafts.platform.macosx.sheets.SheetDelegate;
import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.ProgressListener;
import com.lightcrafts.utils.thread.CancelableThread;
import com.lightcrafts.utils.thread.CancelableThreadMonitor;
import com.lightcrafts.utils.thread.ProgressThread;

/**
 * A <code>MacOSXProgressDialog</code> implements {@link ProgressDialog} for
 * Mac&nbsp;OS&nbsp;X.
 *
 * @author Paul J. Lucas [plucas@lightcrafts.com]
 */
public final class MacOSXProgressDialog implements ProgressDialog {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public Throwable getThrown() {
        return m_threadMonitor.getThrown();
    }

    /**
     * {@inheritDoc}
     */
    public void showProgress( Frame parent, CancelableThread thread,
                              String message, int minValue, int maxValue,
                              boolean hasCancelButton ) {
        final ProgressListenerShower pls = new ProgressListenerShower(
            parent, thread, message, false, minValue, maxValue, hasCancelButton
        );
        SheetDelegate.showAndWait( pls );
    }

    /**
     * {@inheritDoc}
     */
    public void showProgress( Frame parent, CancelableThread thread,
                              String message, boolean hasCancelButton ) {
        final ProgressListenerShower pls = new ProgressListenerShower(
            parent, thread, message, true, 0, 0, hasCancelButton
        );
        SheetDelegate.showAndWait( pls );
    }

    /**
     * {@inheritDoc}
     */
    public native void incrementBy( int delta );

    /**
     * {@inheritDoc}
     */
    public native void setIndeterminate( boolean indeterminate );

    /**
     * {@inheritDoc}
     */
    public native void setMaximum( int maximum );

    /**
     * {@inheritDoc}
     */
    public native void setMinimum( int minimum );

    ////////// private ////////////////////////////////////////////////////////

    /**
     * An <code>ProgressListenerShower</code> is-a {@link SheetShower} and
     * implements  {@link ProgressListener}.  The reason for having this class
     * as a nested, private class rather than having
     * {@link MacOSXProgressDialog} implement {@link ProgressListener} directly
     * is not to expose the {@link ProgressListener} API to the user of
     * {@link MacOSXProgressDialog}.
     */
    private final class ProgressListenerShower extends SheetShower
        implements CancelableThreadMonitor.Listener, ProgressListener {

        /**
         * Construct an <code>ProgressListenerShower</code>.
         */
        ProgressListenerShower( Frame parent, CancelableThread thread,
                                String message, boolean indeterminate,
                                int minValue, int maxValue,
                                boolean hasCancelButton ) {
            m_hasCancelButton = hasCancelButton;
            m_indeterminate = indeterminate;
            m_maxValue = maxValue;
            m_minValue = minValue;
            m_message = message;
            m_parentFrame = parent;
            m_threadMonitor = new CancelableThreadMonitor( thread, this );
        }

        /**
         * {@inheritDoc}
         */
        public void progressCancelled() {
            //
            // Cancel the thread: this will subsequently cause
            // threadTerminated() below to be called that will call notify() to
            // unblock the wait() in showAndWait() below.
            //
            m_threadMonitor.requestCancel();
        }

        /**
         * {@inheritDoc}
         */
        public synchronized void threadTerminated( CancelableThread t ) {
            if ( t != m_threadMonitor.getMonitoredThread() )
                throw new IllegalStateException();
            if ( !t.isCanceled() ) {
                //
                // If the user clicks Cancel, the sheet is automatically
                // hidden; if the thread terminates naturally, we have to hide
                // it ourselves.
                //
                hideSheet();
            }
            done();
        }

        ////////// protected //////////////////////////////////////////////////

        /**
         * {@inheritDoc}.
         */
        protected void showSheet() {
            showNativeSheet(
                m_parentFrame, m_message, m_indeterminate, m_minValue,
                m_maxValue, m_hasCancelButton, this
            );
            m_threadMonitor.start();
        }

        ////////// private ////////////////////////////////////////////////////

        private final boolean m_hasCancelButton;
        private final boolean m_indeterminate;
        private final int m_maxValue, m_minValue;
        private final String m_message;
        private final Frame m_parentFrame;
    }

    /**
     * Hide the sheet.  This is called if/when the {@link CancelableThread}
     * terminates naturally, i.e., the user didn't click Cancel.
     */
    private native void hideSheet();

    /**
     * Initialize the native code.
     *
     * @param nibPath The path to the NIB file used for the progress sheet.
     */
    private static native void init( String nibPath );

    /**
     * This is called by the native code only if/when the user clicks the
     * Cancel button.
     *
     * @param pl The {@link ProgressListener} to notify.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private static void progressCanceledCallback( ProgressListener pl ) {
        pl.progressCancelled();
    }

    /**
     * Call native code to show an alert sheet.
     *
     * @param parent The parent window.
     * @param indeterminate If <code>true</code>, the progress indicator will
     * be indeterminate.
     * @param message The message to display in the progress dialog.
     * @param minValue The minimum value of the progress indicator.
     * @param maxValue The maximum value of the progress indicator.
     * @param hasCancelButton If <code>true</code>, the dialog will contain an
     * enabled Cancel button the user can click to terminate the
     * {@link CancelableThread} prematurely.
     * @param pl The {@link ProgressListener} to use.
     */
    private native void showNativeSheet( Frame parent, String message,
                                         boolean indeterminate, int minValue,
                                         int maxValue, boolean hasCancelButton,
                                         ProgressListener pl );

    /**
     * This is used by the native side to store a pointer to an Objective C
     * object.  The Java side must not touch this.
     */
    @SuppressWarnings({"UNUSED_SYMBOL"})
    private long m_nativePtr;

    private CancelableThreadMonitor m_threadMonitor;

    static {
        System.loadLibrary( "MacOSX" );
        init( MacOSXFileUtil.getNIBPathOf( "Progress.nib" ) );
    }

    ////////// main() for testing /////////////////////////////////////////////

    private static final class TestThread extends ProgressThread {
        TestThread( ProgressIndicator indicator ) {
            super( indicator );
        }

        public void run() {
            for ( int i = 0; i < 20; ++i ) {
                if ( isCanceled() )
                    break;
                System.out.println( i );
                getProgressIndicator().incrementBy( 1 );
                try {
                    Thread.sleep( 250 );
                }
                catch ( InterruptedException e ) {
                    // ignore
                }
            }
        }
    }

    public static void main( String[] args ) {
        final JFrame frame = new JFrame( "TestProgress" );
        frame.setBounds( 100, 100, 500, 300 );
        frame.setVisible( true );

        final MacOSXProgressDialog dialog = new MacOSXProgressDialog();
        final ProgressThread t = new TestThread( dialog );

        dialog.showProgress( frame, t, "Working...", 0, 20, true );
        System.exit( 0 );
    }

}
/* vim:set et sw=4 ts=4: */
