/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.platform;

import java.awt.Frame;

/**
 * An <code>AlertDialog</code> is a platform-indepentent API to display an
 * alert dialog box to the user and wait for a response.
 *
 * @author Paul J. Lucas [plucas@lightcrafts.com]
 */
public interface AlertDialog {

    int WARNING_ALERT = 1;
    int ERROR_ALERT   = 2;

    /**
     * Show an alert sheet to the user and wait for the user to dismiss it.
     *
     * @param parentFrame The parent window to attach the alert to.
     * @param msgText The message to display.
     * @param infoText An additional message to display.  It may be
     * <code>null</code>.
     * @param alertType The type of alert, either {@link #WARNING_ALERT} or
     * {@link #ERROR_ALERT}.
     * @param buttons An array of 1 to 3 strings that contain the text for at
     * most 3 buttons.  For a left-to-right language, the button text is
     * specified in right-to-left order.  Hence, <code>buttons[0]</code>
     * is the right-most, default button.
     * @return Returns the index of the button the user clicked on.
     */
    int showAlert( Frame parentFrame, String msgText, String infoText,
                   int alertType, String... buttons );

    int showAlert( Frame parentFrame, String msgText, String infoText,
                   int alertType, int destructive, String... buttons );
}
/* vim:set et sw=4 ts=4: */
