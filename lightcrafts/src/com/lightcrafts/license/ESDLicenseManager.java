/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

import java.util.Date;
import java.util.regex.Pattern;

import com.lightcrafts.license.ESDLicenseLibrary.LicenseType;
import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.Platform;

import static com.lightcrafts.license.ESDLicenseLibrary.LicenseType.*;
import static com.lightcrafts.license.ESDLicenseLibrary.NO_ESD_ERROR;
import static com.lightcrafts.license.Locale.LOCALE;

/**
 * An <code>ESDLicenseManager</code> obtains a license in the case where the
 * application was downloaded (Electronic Software Distribution).
 *
 * @author Paul J. Lucas [plucas@lightcrafts.com]
 */
final class ESDLicenseManager {

    ////////// package ////////////////////////////////////////////////////////

    /**
     * The number of days a trial license is good for (in seconds).
     */
    static final long TRIAL_LICENSE_DURATION =
        60*60*24 /* seconds/day */ * 30 /* days */;

    /**
     * Get a license.
     *
     * @param lib The {@link ESDLicenseLibrary} to use.
     * @param dialogs The {@link ESDLicenseDialogs} to use.
     * @return Returns <code>true</code> if a license was obtained and the
     * application should continue; returns <code>false</code> if the
     * application should quit.
     */
    static boolean getLicense( ESDLicenseLibrary lib,
                               ESDLicenseDialogs dialogs ) {
        boolean badKey = false;
        while ( true ) {
            ESDLicenseLibrary.ESDError error = lib.probeLicenseType();
            if ( error != NO_ESD_ERROR ) {
                dialogs.showErrorAlert( lib.getErrorMessage( error ), true );
                return false;
            }

            final LicenseType licenseType = badKey ?
                LICENSE_INVALID : lib.getLicenseType();
            switch ( licenseType ) {

                case LICENSE_NORMAL:
                    //
                    // Get the license key and make sure it's still valid.
                    // This prevents somebody from simply copying the ESLF
                    // file to other computers.
                    //
                    final String key = lib.getLicenseKey();
                    if ( !isKeyValid( key, lib, dialogs ) ||
                         !activateKey( key, lib, dialogs ) ) {
                        badKey = true;
                        continue;
                    }
                    break;

                case LICENSE_TRIAL:
                    final Date expiration = lib.getTrialLicenseExpirationDate();
                    switch ( dialogs.showTrialExpirationDialog( expiration ) ) {
                        case ESD_BUY_NOW:
                            lib.buyNow();
                            continue;
                        case ESD_CONTINUE_TRIAL:
                            break;
                        case ESD_ENTER_KEY:
                            if ( !enterAndValidateKey( lib, dialogs ) )
                                continue;
                            badKey = false;
                            break;
                        case ESD_QUIT:
                            return false;
                        default:
                            throw new IllegalStateException();
                    }
                    break;

                default:
                    switch ( dialogs.showUnlicensedDialog() ) {
                        case ESD_ENTER_KEY:
                            if ( !enterAndValidateKey( lib, dialogs ) )
                                continue;
                            badKey = false;
                            break;
                        case ESD_TRY_NOW:
                            if ( !tryNow( lib, dialogs ) )
                                continue;
                            break;
                        case ESD_QUIT:
                            return false;
                        default:
                            throw new IllegalStateException();
                    }
                    break;
            }
            if ( (error = lib.requestLicense()) != NO_ESD_ERROR )
                dialogs.showErrorAlert( lib.getErrorMessage( error ), false );
            else
                return true;
        }
    }

    /**
     * Relicenses the application with a new license key.
     *
     * @param lib The {@link ESDLicenseLibrary} to use.
     * @param dialogs The {@link ESDLicenseDialogs} to use.
     * @return Returns <code>true</code> only if relicensing occurred.
     */
    static boolean relicense( ESDLicenseLibrary lib,
                              ESDLicenseDialogs dialogs ) {
        LicenseChecker.m_enteredLicenseKey = false;
        final Pattern keyPattern = lib.getLicenseKeyPattern();
        while ( true ) {
            final String key = dialogs.showEnterKeyDialog( keyPattern );
            if ( key == null )
                return false;
            if ( isKeyValid( key, lib, dialogs ) &&
                 activateKey( key, lib, dialogs ) &&
                 saveKey( key, lib, dialogs ) ) {
                LicenseChecker.m_enteredLicenseKey = true;
                return true;
            }
        }
    }

    /**
     * Unlicenses the application.
     *
     * @param lib The {@link ESDLicenseLibrary} to use.
     * @param dialogs The {@link ESDLicenseDialogs} to use.
     * @return Returns <code>true</code> only if unlicensing occurred.
     */
    static boolean unlicense( ESDLicenseLibrary lib,
                              ESDLicenseDialogs dialogs ) {
        if ( !dialogs.showDeactivateDialog() )
            return false;
        final ESDLicenseLibrary.ESDError error =
            lib.deactivateKey( lib.getLicenseKey() );
        if ( error != NO_ESD_ERROR ) {
            dialogs.showErrorAlert( lib.getErrorMessage( error ), false );
            return false;
        }
        Platform.getPlatform().getAlertDialog().showAlert(
            null, LOCALE.get( "DeactivationSucceededMajor" ),
            LOCALE.get( "DeactivationSucceededMinor" ),
            AlertDialog.WARNING_ALERT, LOCALE.get( "QuitButton" )
        );
        return true;
    }

    /**
     * Perform the &quot;Enter Key&quot; sequence.
     *
     * @param lib The {@link ESDLicenseLibrary} to use.
     * @param dialogs The {@link ESDLicenseDialogs} to use.
     * @return Returns <code>true</code> only if a key was entered and it was
     * determined to be valid by the license library.
     */
    static boolean enterAndValidateKey( ESDLicenseLibrary lib,
                                                ESDLicenseDialogs dialogs ) {
        LicenseChecker.m_enteredLicenseKey = false;
        final Pattern keyPattern = lib.getLicenseKeyPattern();
        final String key = dialogs.showEnterKeyDialog( keyPattern );
        LicenseChecker.m_enteredLicenseKey =
            key != null &&
            isKeyValid( key, lib, dialogs ) &&
            activateKey( key, lib, dialogs ) &&
            saveKey( key, lib, dialogs );
        return LicenseChecker.m_enteredLicenseKey;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Activate a license key if necessary.
     *
     * @param key The license key.
     * @param lib The {@link ESDLicenseLibrary} to use.
     * @param dialogs The {@link ESDLicenseDialogs} to use.
     * @return Returns <code>true</code> only if the key is (or was) activated
     * successfully.
     */
    private static boolean activateKey( String key, ESDLicenseLibrary lib,
                                        ESDLicenseDialogs dialogs ) {
        if ( !lib.isKeyActivated( key ) ) {
            ESDLicenseLibrary.ESDError error = lib.activateKey( key );
            if ( lib.isInternetError( error ) )
                error = lib.manuallyActivateKey( key );
            if ( error != NO_ESD_ERROR ) {
                dialogs.showErrorAlert( lib.getErrorMessage( error ), false );
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the license key is valid.
     *
     * @param key The license key.
     * @param lib The {@link ESDLicenseLibrary} to use.
     * @param dialogs The {@link ESDLicenseDialogs} to use.
     * @return Returns <code>true</code> only if the key is valid.
     */
    private static boolean isKeyValid( String key, ESDLicenseLibrary lib,
                                       ESDLicenseDialogs dialogs ) {
        if ( lib.isKeyValid( key ) )
            return true;
        if ( key.startsWith( "0101605001" ) ||  // LZ 1.x
             key.startsWith( "LZRT" ) || key.startsWith( "LZ2" ) ) {
            final AlertDialog dialog = Platform.getPlatform().getAlertDialog();
            final int button = dialog.showAlert(
                null,
                LOCALE.get( "Pre3SNErrorMajor" ),
                LOCALE.get( "Pre3SNErrorMinor" ),
                AlertDialog.ERROR_ALERT,
                LOCALE.get( "BuyNowButton" ),
                LOCALE.get( "CancelButton" )
            );
            if ( button == 0 )
                lib.buyNow();
        } else
            dialogs.showErrorAlert( LOCALE.get( "InvalidSNError" ), false );
        return false;
    }

    /**
     * Save a license key to disk.
     *
     * @param key The license key to validate.
     * @param lib The {@link ESDLicenseLibrary} to use.
     * @param dialogs The {@link ESDLicenseDialogs} to use.
     * @return Returns <code>true</code> only if a key was entered and it was
     * determined to be valid by the license library.
     */
    private static boolean saveKey( String key, ESDLicenseLibrary lib,
                                    ESDLicenseDialogs dialogs ) {
        if ( !lib.saveLicenseKey( key ) ) {
            dialogs.showErrorAlert(
                LOCALE.get( "LC_WRITE_LICENSE_KEY_FAILED" ), false
            );
            return false;
        }
        final AlertDialog dialog = Platform.getPlatform().getAlertDialog();
        dialog.showAlert(
            null, LOCALE.get( "ActivationSucceededMajor" ),
            LOCALE.get( "ActivationSucceededMinor" ),
            AlertDialog.WARNING_ALERT, LOCALE.get( "OKButton" )
        );
        return true;
    }

    /**
     * Perform the &quot;Try Now&quot; sequence.
     *
     * @param lib The {@link ESDLicenseLibrary} to use.
     * @param dialogs The {@link ESDLicenseDialogs} to use.
     * @return Returns <code>true</code> only if a key was entered and it was
     * determined to be valid by the license library.
     */
    private static boolean tryNow( ESDLicenseLibrary lib,
                                   ESDLicenseDialogs dialogs ) {
        final String key = dialogs.showTryNowDialog();
        if ( key != null ) {
            final ESDLicenseLibrary.ESDError error = lib.tryNow( key );
            if ( error == NO_ESD_ERROR )
                return true;
            dialogs.showErrorAlert( lib.getErrorMessage( error ), false );
        }
        return false;
    }
}
/* vim:set et sw=4 ts=4: */
