/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

import java.util.Date;
import java.util.regex.Pattern;

/**
 * <code>ESDLicenseDialogs</code> is an interface for displaying all the
 * dialogs needed for ESD licensing.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public interface ESDLicenseDialogs {

    /**
     * Response codes for some dialogs.
     */
    enum Response {
        ESD_BUY_NOW,
        ESD_CONTINUE_TRIAL,
        ESD_ENTER_KEY,
        ESD_TRY_NOW,
        ESD_CANCEL,
        ESD_QUIT
    }

    /**
     * Shows a dialog explaining what unlicensing does and asking whether to
     * proceed.
     *
     * @return Returns <code>true</code> only if unlicensing should proceed.
     */
    boolean showDeactivateDialog();

    /**
     * Shows a dialog wherein the user can enter the license key.
     *
     * @param keyPattern The {@link Pattern} of the license key.
     * @return Returns either the license key or <code>null</code> if the user
     * clicked Cancel.
     */
    String showEnterKeyDialog( Pattern keyPattern );

    /**
     * Show an error alert to the user.
     *
     * @param errorMessage The error message.
     * @param isFatal If <code>true</code>, what would ordinarily be the OK
     * button is a Quit button instead.
     */
    void showErrorAlert( String errorMessage, boolean isFatal );

    /**
     * Show a dialog telling the user when the trial license expires (or that
     * it has already expired).
     *
     * @return Returns a {@link Response}.
     */
    Response showTrialExpirationDialog( Date expiration );

    /**
     * Shows a dialog wherein the user can enter the trial license key.
     *
     * @return Returns either the trial key or <code>null</code> if the user
     * clicked Cancel.
     */
    String showTryNowDialog();

    /**
     * Show a dialog for the case where there is no license of any kind.
     *
     * @return Returns a {@link Response}.
     */
    Response showUnlicensedDialog();
}
/* vim:set et sw=4 ts=4: */