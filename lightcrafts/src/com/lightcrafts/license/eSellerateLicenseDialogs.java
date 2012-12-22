/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

import static com.lightcrafts.license.ESDLicenseCheckerModule.trialLicenseRefDate;
import static com.lightcrafts.license.ESDLicenseDialogs.Response.*;
import static com.lightcrafts.license.Locale.LOCALE;
import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.toolkit.TextAreaFactory;
import com.lightcrafts.utils.TextUtil;
import com.lightcrafts.utils.Version;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.*;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * <code>eSellerateLicenseDialogs</code> implements {@link ESDLicenseDialogs}
 * for the eSellerate license library.
 *
 * @author Paul J. Lucas [plucas@lightcrafts.com]
 */
class eSellerateLicenseDialogs implements ESDLicenseDialogs {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public boolean showDeactivateDialog() {
        final AlertDialog dialog = Platform.getPlatform().getAlertDialog();
        final int button = dialog.showAlert(
            null,
            LOCALE.get( "DeactivateQuestionMajor" ),
            LOCALE.get( "DeactivateQuestionMinor" ), AlertDialog.WARNING_ALERT,
            LOCALE.get( "CancelButton" ), LOCALE.get( "DeactivateButton" )
        );
        return button == 1;
    }

    /**
     * {@inheritDoc}
     */
    public String showEnterKeyDialog( final Pattern keyPattern ) {
        final JTextArea prompt = TextAreaFactory.createTextArea(
            LOCALE.get( "EnterSNText1" ) + "\n\n" +
            "        XXXXXXXXXX-XXXX-XXXX-XXXX-XXXX-XXXX\n\n" +
            LOCALE.get( "EnterSNText2" ),
            30
        );
        final Box promptBox = Box.createHorizontalBox();
        promptBox.add( prompt );
        promptBox.add( Box.createHorizontalGlue() );

        final JTextField keyField = new JTextField( m_stickyKey );
        keyField.setBorder( BorderFactory.createLoweredBevelBorder() );

        final Font staticFont = prompt.getFont();
        final Font keyFont =
            new Font( "Monospaced", Font.PLAIN, staticFont.getSize() );
        keyField.setFont( keyFont );

        final JButton okButton = new JButton( LOCALE.get( "OKButton" ) );
        final JButton cancelButton =
            new JButton( LOCALE.get( "CancelButton" ) );
        final JButton pasteButton =
            new JButton( LOCALE.get( "PasteFromClipboardButton" ) );

        final Box buttons = Box.createHorizontalBox();
        buttons.add( pasteButton );
        buttons.add( Box.createHorizontalGlue() );
        buttons.add( Box.createHorizontalStrut( 4 ) );
        buttons.add( cancelButton);
        buttons.add( Box.createHorizontalStrut( 4 ) );
        buttons.add( okButton );

        final Box panel = Box.createVerticalBox();
        panel.add( promptBox );
        panel.add( Box.createVerticalStrut( 8 ) );
        panel.add( keyField );
        panel.add( Box.createVerticalStrut( 8 ) );
        panel.add( buttons );
        panel.setBorder( BorderFactory.createEmptyBorder( 8, 8, 8, 8 ) );

        // Allow ctrl-V (windows, linux) or cmd-V (mac) to cause the paste:
        panel.registerKeyboardAction(
            new ActionListener() {
                public void actionPerformed( ActionEvent e ) {
                    pasteButton.doClick();
                }
            },
            KeyStroke.getKeyStroke(
                KeyEvent.VK_V,
                Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
            ),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        final JDialog dialog = new JDialog(
            (Frame)null, Version.getApplicationName(), true
        );
        dialog.getContentPane().add( panel );
        dialog.getRootPane().setDefaultButton( okButton );
        dialog.pack();
        dialog.setLocationRelativeTo( null );
        dialog.setResizable( false );

        final Color background = dialog.getContentPane().getBackground();
        prompt.setBackground( background );

        dialog.addWindowFocusListener(
            new WindowFocusListener() {
                public void windowGainedFocus( WindowEvent e ) {
                    keyField.requestFocusInWindow();
                }
                public void windowLostFocus( WindowEvent e ) {
                    // do nothing
                }
            }
        );
        keyField.addFocusListener(
            new FocusAdapter() {
                public void focusGained( FocusEvent focusEvent ) {
                    keyField.selectAll();
                }
            }
        );
        okButton.addActionListener(
            new ActionListener() {
                public void actionPerformed( ActionEvent event ) {
                    m_stickyKey = keyField.getText();
                    m_response = ESD_ENTER_KEY;
                    dialog.dispose();
                }
            }
        );
        cancelButton.addActionListener(
            new ActionListener() {
                public void actionPerformed( ActionEvent event ) {
                    m_response = ESD_CANCEL;
                    dialog.dispose();
                }
            }
        );
        pasteButton.addActionListener(
            new ActionListener() {
                public void actionPerformed( ActionEvent event ) {
                    final String key = getKeyFromClipboard( keyPattern );
                    if ( key != null ) {
                        keyField.setText( key );
                        keyField.requestFocusInWindow();
                    }
                }
            }
        );

        dialog.setVisible( true );
        switch ( m_response ) {
            case ESD_ENTER_KEY:
                final String key = keyField.getText();
                return key != null ? key.trim().toUpperCase() : null;
            default:
                return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void showErrorAlert( String errorMessage, boolean isFatal ) {
        final AlertDialog dialog = Platform.getPlatform().getAlertDialog();
        dialog.showAlert(
            null, errorMessage, null, AlertDialog.ERROR_ALERT,
            isFatal ? LOCALE.get( "QuitButton" ) : LOCALE.get( "OKButton" )
        );
    }

    /**
     *{@inheritDoc}
     */
    public Response showTrialExpirationDialog( Date expiration ) {
        boolean expired = false;
        try {
            // When LightZone has been "unlicensed" (try deleting
            // Library/Application Support/LightZone/License), this call
            // generates an NPE.
            expired = trialLicenseRefDate.after( expiration );
        }
        catch (Throwable t) {
            return ESD_CONTINUE_TRIAL;
        }
        final String messageText;
        if ( expired ) {
            messageText = LOCALE.get( "TrialLicenseExpired" );
        } else {
            Date midnight = getPrecedingMidnight( expiration );
            if ( trialLicenseRefDate.compareTo( midnight ) > 0 ) {
                final String timeString = getTimeOfDayString( expiration );
                messageText = LOCALE.get(
                    "TrialLicenseExpiresToday", timeString
                );
            }
            else {
                midnight = getPrecedingMidnight( midnight );
                if ( trialLicenseRefDate.compareTo( midnight ) > 0 ) {
                    final String timeString = getTimeOfDayString( expiration );
                    messageText = LOCALE.get(
                        "TrialLicenseExpiresTomorrow", timeString
                    );
                }
                else {
                    final long days = getDaysUntil( expiration );
                    messageText = LOCALE.get(
                        "TrialLicenseExpiresDays", Long.toString(days)
                    );
                }
            }
        }
        final JButton continueTrialButton =
            new JButton( LOCALE.get( "ContinueTrialButton" ) );
        continueTrialButton.setContentAreaFilled(false);
        //continueTrialButton.setEnabled( !expired );

        final JButton buyNowButton =
            new JButton( LOCALE.get( "BuyNowButton" ) );
        buyNowButton.setContentAreaFilled(false);
        final JButton enterKeyButton =
            new JButton( LOCALE.get( "EnterSNButton" ) );
        enterKeyButton.setContentAreaFilled(false);

        List<JComponent> buttons = new LinkedList<JComponent>();
        buttons.add(buyNowButton);
        buttons.add(enterKeyButton);
        buttons.add(continueTrialButton);

        final DialogBackgroundContainer panel =
            new DialogBackgroundContainer(messageText, buttons, expired);

        final JDialog dialog = new JDialog(
            (Frame)null,
            LOCALE.get( "TrialExpirationDialogTitle", Version.getApplicationName() ),
            true
        );
        dialog.setUndecorated( true );
        dialog.getContentPane().add( panel );
        dialog.pack();
        dialog.setLocationRelativeTo( null );
        dialog.setResizable( false );

        //if ( !expired )
            continueTrialButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed( ActionEvent event ) {
                        dialog.dispose();
                        m_response = ESD_CONTINUE_TRIAL;
                    }
                }
            );
        buyNowButton.addActionListener(
            new ActionListener() {
                public void actionPerformed( ActionEvent event ) {
                    dialog.dispose();
                    m_response = ESD_BUY_NOW;
                }
            }
        );
        enterKeyButton.addActionListener(
            new ActionListener() {
                public void actionPerformed( ActionEvent event ) {
                    dialog.dispose();
                    m_response = ESD_ENTER_KEY;
                }
            }
        );

        m_response = ESD_QUIT;
        dialog.setVisible( true );
        return m_response;
    }


    /**
     *{@inheritDoc}
     */
    public String showTryNowDialog() {
        return "Not Used";
    }

    /**
     * {@inheritDoc}
     */
    public Response showUnlicensedDialog() {
        final Object[] buttons = {
            LOCALE.get( "TryNowButton" ),
            LOCALE.get( "EnterSNButton" ),
            LOCALE.get( "QuitButton" )
        };
        final int response = JOptionPane.showOptionDialog(
            null,
            LOCALE.get( "UnlicensedDialogMessage", Version.getApplicationName() ),
            LOCALE.get( "UnlicensedDialogTitle"),
            JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null,
            buttons, buttons[0]
        );
        switch ( response ) {
            case 0:
                return ESD_TRY_NOW;
            case 1:
                return ESD_ENTER_KEY;
            case 2:
                return ESD_QUIT;
            default:
                throw new IllegalStateException();
        }
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Construct an <code>eSellerateLicenseDialogs</code>.
     */
    eSellerateLicenseDialogs() {
        m_stickyKey = "";
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Compute the number of midnight crossings between now and the given Date.
     */
    private static long getDaysUntil( Date date ) {
        final Date givenMidnight = getPrecedingMidnight( date );
        final Date currentMidnight = getPrecedingMidnight( new Date() );
        final long interval =
            givenMidnight.getTime() - currentMidnight.getTime();
        return interval / (24L * 60L * 60L * 1000L);
    }

    /**
     * Get the license get from the clipboard.
     *
     * @param keyPattern The {@link Pattern} of the license key.
     * @return Returns said key or <code>null</code> if the clipboard doesn't
     * contain content that could be a license key.
     */
    private static String getKeyFromClipboard( Pattern keyPattern ) {
        String s = null;
        try {
            final Toolkit tk = Toolkit.getDefaultToolkit();
            final Clipboard cb = tk.getSystemClipboard();
            final Transferable cbContents = cb.getContents( null );
            s = (String)cbContents.getTransferData( DataFlavor.stringFlavor );
        }
        catch ( Exception e ) {
            // do nothing
        }
        if ( s == null )
            return null;
        s = s.trim().toUpperCase();
        return keyPattern.matcher( s ).matches() ? s : null;
    }

    /**
     * Get the Date of the last local midnight preceding the given Date.
     */
    private static Date getPrecedingMidnight(Date date) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis( date.getTime() - 1000 );
        cal.set( Calendar.HOUR_OF_DAY, 0 );
        cal.set( Calendar.MINUTE, 0 );
        cal.set( Calendar.SECOND, 0 );
        cal.set( Calendar.MILLISECOND, 0 );
        return cal.getTime();
    }

    /**
     * Get the short formatted time-of-day Date of the given Date.  (E.g.,
     * "h:mm a" in the US locale.)
     */
    private static String getTimeOfDayString( Date date ) {
        return TextUtil.dateFormat(
            DateFormat.getTimeInstance( DateFormat.SHORT ), date
        );
    }

    private static Response m_response;

    /**
     * The license key is stored here between calls to
     * {@link ESDLicenseDialogs#showEnterKeyDialog(Pattern)} so the text field
     * in the dialog can be  repopulated with the previous value so if the user
     * makes a mistake, s/he can edit the value rather than having to start
     * over.
     */
    private static String m_stickyKey;
}
/* vim:set et sw=4 ts=4: */
