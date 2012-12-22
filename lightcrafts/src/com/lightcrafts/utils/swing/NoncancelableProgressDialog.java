/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils.swing;

import javax.swing.*;
import java.awt.*;

/**
 * A <code>NoncancelableProgressDialog</code> is-a {@link JDialog} that
 * displays a dialog containing a message (e.g., "Please wait...") and an
 * indeterminate progress indicator.
 * <p>
 * The dialog is <em>not</em> modal, so the canonical usage is:
 * <pre>
 *  JDialog d = new NoncancelableProgressDialog( parent, "Please wait..." );
 *  d.show();
 *  // ... do something that takes a while ...
 *  d.dispose();
 * </pre>
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class NoncancelableProgressDialog extends JDialog {

    ////////// public /////////////////////////////////////////////////////

    /**
     * Construct a <code>JProgressDialog</code>.
     * <p>
     * This <b>must</b> be called on the event dispatch thread because it
     * touches the progress bar.
     *
     * @param parent The parent window.
     * @param message The message to display in the progress dialog.
     */
    public NoncancelableProgressDialog( Frame parent, String message ) {
        super( parent, "", true );
        setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );
        setModal( false );

        final JLabel messageLabel = new JLabel( message );

        m_progressBar = new JProgressBar();
        m_progressBar.setIndeterminate( true );

        //
        // Trying to use something simple like Box for doing the layout of the
        // progress bar doesn't work, i.e., it generates a funny looking layout;
        // so do the layout ourselves.
        //
        final JPanel layoutPanel = new JPanel( null ) {
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

            public Dimension getPreferredSize() {
                final Dimension mSize = messageLabel.getPreferredSize();
                final Dimension pSize = m_progressBar.getPreferredSize();
                return new Dimension(
                    Math.max( mSize.width, pSize.width ),
                    mSize.height + pSize.height + msgBarPadding
                );
            }
        };

        final JButton cancelButton = new JButton( "Cancel "); // TODO: localize
        cancelButton.setEnabled( false );
        final JOptionPane panel = new JOptionPane(
            layoutPanel, JOptionPane.INFORMATION_MESSAGE,
            0, NoIcon.INSTANCE, new Object[]{ cancelButton }
        );

        layoutPanel.add( messageLabel );
        layoutPanel.add( m_progressBar );
        getContentPane().setLayout( new BorderLayout() );
        getContentPane().add( panel );
        pack();
        setLocationRelativeTo( parent );
        setResizable( false );
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        // apple.laf.AquaProgressBarUI leaks animation Timers,
        // which hold references to the progress bar member:
        if ( m_progressBar != null ) {
            final Container barParent = m_progressBar.getParent();
            if ( barParent != null ) {
                final Component progreesBarCopy = m_progressBar;
                EventQueue.invokeLater(
                    new Runnable() {
                        public void run() {
                            barParent.remove( progreesBarCopy );
                        }
                    }
                );
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

    private JProgressBar m_progressBar;
}

/* vim:set et sw=4 ts=4: */
