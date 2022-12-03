/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.platform;

import com.lightcrafts.utils.ProgressIndicator;
import com.lightcrafts.utils.ProgressListener;
import com.lightcrafts.utils.Version;
import com.lightcrafts.utils.thread.CancelableThread;
import com.lightcrafts.utils.thread.CancelableThreadMonitor;
import com.lightcrafts.utils.thread.ProgressThread;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

/**
 * A <code>DefaultProgressDialog</code> implements {@link ProgressDialog} for
 * the default platform.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class DefaultProgressDialog implements ProgressDialog {

    ////////// public /////////////////////////////////////////////////////////

    public DefaultProgressDialog() {
        m_progressBar = new JProgressBar();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Throwable getThrown() {
        return m_threadMonitor.getThrown();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementBy( final int delta ) {
        EventQueue.invokeLater(() -> {
            if (m_progressBar != null)
                m_progressBar.setValue( m_progressBar.getValue() + delta );
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setIndeterminate( final boolean indeterminate ) {
        EventQueue.invokeLater(() -> {
            if (m_progressBar != null) {
                // There's a bug in Java: if you make a determinate
                // progress bar indeterminate, the "barber pole" is
                // partially "frozen" from where the old value was to
                // the right.  To fix it, first set the value to the
                // maximum value.
                m_progressBar.setValue( m_progressBar.getMaximum() );
                m_progressBar.setIndeterminate( indeterminate );
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMaximum( final int maximum ) {
        EventQueue.invokeLater(() -> {
            if (m_progressBar != null)
                m_progressBar.setMaximum( maximum );
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setMinimum( final int minimum ) {
        EventQueue.invokeLater(() -> {
            if (m_progressBar != null)
                m_progressBar.setMinimum( minimum );
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showProgress( Frame parent, CancelableThread thread,
                              String message, int minValue, int maxValue,
                              boolean hasCancelButton ) {
        final ProgressListenerImpl pli = new ProgressListenerImpl(
            parent, thread, message, false, minValue, maxValue, hasCancelButton
        );
        pli.showAndWait();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void showProgress( Frame parent, CancelableThread thread,
                              String message, boolean hasCancelButton ) {
        final ProgressListenerImpl pli = new ProgressListenerImpl(
            parent, thread, message, true, 0, 0, hasCancelButton
        );
        pli.showAndWait();
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * A <code>JProgressDialog</code> is-a {@link JDialog} that displays a
     * short message, the progress bar, and a Cancel button.
     */
    private final class JProgressDialog extends JDialog {

        ////////// public /////////////////////////////////////////////////////

        /**
         * Construct a <code>JProgressDialog</code>.
         * <p>
         * This <b>must</b> be called on the event dispatch thread because it
         * touches the progress bar.
         *
         * @param parent The parent window.
         * @param message The message to display in the progress dialog.
         * @param hasCancelButton If <code>true</code>, the dialog will contain
         * an enabled Cancel button the user can click to terminate the
         * {@link CancelableThread} prematurely.
         * @param pl The {@link ProgressListener} to notify if the user clicks
         * Cancel.
         */
        JProgressDialog( Frame parent, String message,
                         boolean hasCancelButton, final ProgressListener pl ) {
            super( parent, Version.getApplicationName(), true );
            final JLabel messageLabel = new JLabel( message );

            // Don't allow users to dismiss the dialog box
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

            //
            // Trying to use something simple like Box for doing the layout of
            // the progress bar doesn't work, i.e., it generates a funny
            // looking layout; so do the layout ourselves.
            //
            final JPanel layoutPanel = new JPanel( null ) {
                @Override
                public void doLayout() {
                    final Dimension size = getSize();
                    messageLabel.setLocation( 0, 0 );
                    messageLabel.setSize( messageLabel.getPreferredSize() );
                    final Dimension mSize = messageLabel.getPreferredSize();
                    final Dimension pSize = m_progressBar.getPreferredSize();
                    m_progressBar.setLocation(
                        0, mSize.height + msgBarPadding
                    );
                    m_progressBar.setSize( size.width, pSize.height );
                }

                @Override
                public Dimension getPreferredSize() {
                    final Dimension mSize = messageLabel.getPreferredSize();
                    final Dimension pSize = m_progressBar.getPreferredSize();
                    return new Dimension(
                        Math.max( mSize.width, pSize.width ),
                        mSize.height + pSize.height + msgBarPadding
                    );
                }
            };
            layoutPanel.add( messageLabel );
            layoutPanel.add( m_progressBar );
            final JButton cancelButton = new JButton( "Cancel ");

            final JOptionPane panel = new JOptionPane(
                layoutPanel, JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, new Object[]{ cancelButton }
            );

            if ( hasCancelButton ) {
                cancelButton.addActionListener(event -> {
                    messageLabel.setText( "Cancelling..." );
                    pl.progressCancelled();
                });
            } else {
                cancelButton.setEnabled(false);
            }
            getContentPane().setLayout( new BorderLayout() );
            getContentPane().add( panel );
            pack();
            setLocationRelativeTo( parent );
            setResizable( false );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void dispose() {
            // apple.laf.AquaProgressBarUI leaks animation Timers,
            // which hold references to the progress bar member:
            if ( m_progressBar != null ) {
                final Container barParent = m_progressBar.getParent();
                if ( barParent != null ) {
                    final Component progreesBarCopy = m_progressBar;
                    EventQueue.invokeLater(() -> barParent.remove(progreesBarCopy));
                }
                m_progressBar = null;
            }
            super.dispose();
        }

        ////////// private ////////////////////////////////////////////////////

        /**
         * There needs to be some padding between the bottom of the message and
         * the top of the progress bar.  This is it.
         */
        private static final int msgBarPadding = 5;
    }

    /**
     * An <code>ProgressListenerImpl</code> implements
     * {@link ProgressListener}.  The reason for having this class as a nested,
     * private class rather than having {@link DefaultProgressDialog} implement
     * {@link ProgressListener} directly is not to expose the
     * {@link ProgressListener} API to the user of
     * {@link DefaultProgressDialog}.
     */
    private final class ProgressListenerImpl
        implements CancelableThreadMonitor.Listener, ProgressListener {

        ////////// public /////////////////////////////////////////////////////

        /**
         * Construct a <code>ProgressListenerImpl</code>.
         *
         * @param parent The parent window.
         * @param thread The {@link CancelableThread} to run while showing the
         * progress dialog.
         * @param message The message to display in the progress dialog.
         * @param indeterminate If <code>true</code>, makes this indicator an
         * interminate progress indicator.
         * @param minValue The minimum value of the progress indicator.
         * @param maxValue The maximum value of the progress indicator.
         * @param hasCancelButton If <code>true</code>, the dialog will contain an
         * enabled Cancel button the user can click to terminate the
         * {@link CancelableThread} prematurely.
         */
        ProgressListenerImpl( Frame parent, CancelableThread thread,
                              String message, boolean indeterminate,
                              int minValue, int maxValue,
                              boolean hasCancelButton ) {
            init(
                parent, message, indeterminate, minValue, maxValue,
                hasCancelButton
            );
            m_threadMonitor = new CancelableThreadMonitor( thread, this );
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void progressCancelled() {
            //
            // Cancel the thread: this will subsequently cause
            // threadTerminated() below to be called that will dispose of the
            // dialog and unblock the show() in showAndWait() below.
            //
            m_threadMonitor.requestCancel();
        }

        /**
         * Start the thread, show the dialog, and wait until the dialog is
         * dismissed either because the thread terminated naturally or the user
         * clicks Cancel.
         */
        public void showAndWait() {
            m_threadMonitor.start();
            m_jProgressDialog.setVisible(true); // blocks until dialog goes away
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void threadTerminated( CancelableThread t ) {
            if ( t != m_threadMonitor.getMonitoredThread() )
                throw new IllegalStateException();
            EventQueue.invokeLater(() -> m_jProgressDialog.dispose());
        }

        ////////// private ////////////////////////////////////////////////////

        /**
         * Initialize a <code>ProgressListenerImpl</code>.  The reason this
         * method is needed is because this initialization must occur on the
         * event dispatch thread synchronously.  The obvious thing to use is
         * {@link EventQueue#invokeAndWait(Runnable)}.  However, that
         * (stupidly) dies if it's called from the event dispatch thread.
         * (Why can't it just call the <code>run()</code> method directly
         * instead?)  So we have to write the code that Sun should have written
         * in the first place, i.e., check whether we're already running on the
         * event dispatch thread: if so, just initialize; if not, call
         * {@link EventQueue#invokeAndWait(Runnable)}.
         *
         * @param parent The parent window.
         * @param message The message to display in the progress dialog.
         * @param indeterminate If <code>true</code>, makes this indicator an
         * interminate progress indicator.
         * @param minValue The minimum value of the progress indicator.
         * @param maxValue The maximum value of the progress indicator.
         * @param hasCancelButton If <code>true</code>, the dialog will contain an
         * enabled Cancel button the user can click to terminate the
         * {@link CancelableThread} prematurely.
         */
        private void init( final Frame parent, final String message,
                           final boolean indeterminate, final int minValue,
                           final int maxValue, final boolean hasCancelButton ) {
            if ( !EventQueue.isDispatchThread() ) {
                try {
                    EventQueue.invokeAndWait(() -> init(
                        parent, message, indeterminate, minValue,
                        maxValue, hasCancelButton
                    ));
                }
                catch ( InterruptedException e ) {
                    // ignore (?)
                }
                catch ( InvocationTargetException e ) {
                    // Have you got a better idea?
                    throw new RuntimeException( e.getCause() );
                }
                return;
            }
            if ( indeterminate ) {
                m_progressBar.setIndeterminate( true );
            } else {
                m_progressBar.setMaximum( maxValue );
                m_progressBar.setMinimum( minValue );
            }
            m_jProgressDialog = new JProgressDialog(
                parent, message, hasCancelButton, this
            );
        }

        private JProgressDialog m_jProgressDialog;
    }

    /**
     * The progress bar that's displayed in the dialog.
     */
    private JProgressBar m_progressBar;

    private CancelableThreadMonitor m_threadMonitor;

    ///////////////////////////////////////////////////////////////////////////

    private static final class TestThread extends ProgressThread {
        TestThread( ProgressIndicator indicator ) {
            super( indicator );
        }

        @Override
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

        final DefaultProgressDialog dialog = new DefaultProgressDialog();
        final ProgressThread t = new TestThread( dialog );

        dialog.showProgress( frame, t, "Working...", 0, 20, true );
        System.exit( 0 );
    }
}
/* vim:set et sw=4 ts=4: */
