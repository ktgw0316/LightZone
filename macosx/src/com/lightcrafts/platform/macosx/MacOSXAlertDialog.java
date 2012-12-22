/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform.macosx;

import java.awt.*;

import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.macosx.sheets.AlertListener;
import com.lightcrafts.platform.macosx.sheets.SheetDelegate;
import com.lightcrafts.platform.macosx.sheets.SheetShower;

/**
 * A <code>MacOSXAlertDialog</code> implements {@link AlertDialog} for
 * Mac&nbsp;OS&nbsp;X.
 *
 * @author Paul J. Lucas [plucas@lightcrafts.com]
 */
public final class MacOSXAlertDialog implements AlertDialog {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    public int showAlert( Frame parentFrame, String msgText, String infoText,
                          int alertType, String... buttons ) {
        final AlertSheetShower ali = new AlertSheetShower(
            alertType, parentFrame, msgText, infoText, buttons
        );
        SheetDelegate.showAndWait( ali );
        return m_buttonClicked;
    }

    public int showAlert( Frame parentFrame, String msgText, String infoText,
                          int alertType, int destructive, String... buttons )
    {
        return showAlert(parentFrame, msgText,  infoText, alertType, buttons);
    }

    ////////// private ////////////////////////////////////////////////////////

    //
    // These constants have to be repeated here so they are made available via
    // javah command to native code.
    //
    private static final int WARNING_ALERT = AlertDialog.WARNING_ALERT;
    private static final int ERROR_ALERT   = AlertDialog.ERROR_ALERT;

    /**
     * An <code>AlertSheetShower</code> is-a {@link SheetShower} and an
     * {@link AlertListener}.  The reason for having this class as a nested,
     * private class rather than having {@link MacOSXAlertDialog} implement
     * {@link AlertListener} directly is not to expose the
     * {@link AlertListener} API to the user of {@link MacOSXAlertDialog}.
     */
    private final class AlertSheetShower extends SheetShower
        implements AlertListener  {

        /**
         * Construct an <code>AlertSheetShower</code>.
         * @param alertType The type of alert, either
         * {@link AlertDialog#WARNING_ALERT} or
         * {@link AlertDialog#ERROR_ALERT}.
         * @param parentFrame The parent window to attach the sheet to.
         * @param msgText The message to display.
         * @param infoText The additional message to display.  It may be
         * <code>null</code>.
         * @param buttons An array of 1 to 3 strings that contain the text for
         * at most 3 buttons.  For a left-to-right language, the button text is
         * specified in right-to-left order.  Hence, <code>buttons[0]</code> is
         * the right-most, default button.
         */
        AlertSheetShower( int alertType, Frame parentFrame, String msgText,
                          String infoText, String[] buttons ) {
            m_alertType = alertType;
            m_buttons = buttons;
            m_infoText = infoText;
            m_parentFrame = parentFrame;
            m_msgText = msgText;
        }

        /**
         * {@inheritDoc}
         */
        public void sheetDone( int buttonClicked ) {
            //
            // Copy the index of the button the user clicked and wake up the
            // thread that called showAndWait() allowing it to continue.
            //
            m_buttonClicked = buttonClicked;
            done();
        }

        ////////// protected //////////////////////////////////////////////////

        /**
         * {@inheritDoc}.
         */
        protected void showSheet() {
            showNativeSheet(
                m_alertType, m_parentFrame, m_msgText, m_infoText, m_buttons,
                this
            );
        }

        ////////// private ////////////////////////////////////////////////////

        private final int m_alertType;
        private final String[] m_buttons;
        private final String m_infoText;
        private final String m_msgText;
        private final Frame m_parentFrame;
    }

    /**
     * Initialize the native code.
     */
    private static native void init();

    /**
     * This is called by the native code when the alert is done because the
     * user clicked a button.
     *
     * @param al The {@link AlertListener} to notify.
     * @param buttonClicked The index of the button the user clicked where 0 is
     * the right-most button (in a left-to-right language).
     */
    private static void sheetDoneCallback( AlertListener al,
                                           int buttonClicked ) {
        al.sheetDone( buttonClicked );
    }

    /**
     * Call native code to show an alert sheet.
     *
     * @param alertType The type of alert, either
     * {@link AlertDialog#WARNING_ALERT} or
     * {@link AlertDialog#ERROR_ALERT}.
     * @param parent The parent window to attach the sheet to.
     * @param msgText The message to display.
     * @param infoText An additional message to display.  It may be
     * <code>null</code>.
     * @param buttons An array of 1 to 3 strings that contain the text for at
     * most 3 buttons.  For a left-to-right language, the button text is
     * specified in right-to-left order.  Hence, <code>buttons[0]</code>
     * is the right-most, default button.
     * @param al The {@link AlertListener} to use.
     */
    private static native void showNativeSheet( int alertType,
                                                Component parent,
                                                String msgText,
                                                String infoText,
                                                String[] buttons,
                                                AlertListener al );

    /**
     * The index of the button the user clicked.
     */
    private int m_buttonClicked;

    static {
        System.loadLibrary( "MacOSX" );
        init();
    }
}
/* vim:set et sw=4 ts=4: */
