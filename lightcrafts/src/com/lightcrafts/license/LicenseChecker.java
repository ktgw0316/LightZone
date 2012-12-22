/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

import java.awt.EventQueue;
import java.util.Date;

import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.Platform;

import static com.lightcrafts.license.Locale.LOCALE;

/**
 * This class is used to check for a valid license.
 *
 * @author Paul J. Lucas [paul@lightcrafts.com]
 */
public final class LicenseChecker {

    ////////// public /////////////////////////////////////////////////////////

    /**
     * Check for a valid license.  If none found, complain and exit.
     *
     * @return Returns the license text to be displayed in the splash screen.
     */
    public static String checkLicense() {
        final String licenseType =
            System.getProperty( "com.lightcrafts.licensetype" );

        LicenseCheckerModule licenseChecker = null;
        if ( "ESD".equals( licenseType ) )
            licenseChecker = new ESDLicenseCheckerModule();
        else if ( "TB".equals( licenseType ) )
            licenseChecker = new ExpirationLicenseCheckerModule();
        else {
            final AlertDialog dialog = Platform.getPlatform().getAlertDialog();
            dialog.showAlert(
                null, LOCALE.get( "NoLicenseTypeDefinedMajor" ),
                LOCALE.get( "NoLicenseTypeDefinedMinor" ),
                AlertDialog.ERROR_ALERT,
                LOCALE.get( "QuitButton" )
            );
            System.exit( 0 );
        }

        try {
            EventQueue.invokeAndWait( licenseChecker );
            m_licenseKey = licenseChecker.getLicenseKey();
            m_hasExpiredTrialLicense = licenseChecker.hasExpiredTrialLicense();
            return licenseChecker.getMessage();
        }
        catch ( Throwable t ) {
            t.printStackTrace();
            return null;
        }
    }

    /**
     * Checks whether the user actually entered a valid license key during the
     * call to {@link #checkLicense()}.
     *
     * @return Returns <code>true</code> only if the user entered a valid
     * license key during the call to {@link #checkLicense()}.
     */
    public static boolean enteredLicenseKey() {
        return m_enteredLicenseKey;
    }

    /**
     * Returns whether the license is for the basic version of LightZone.
     *
     * @return Returns <code>true</code> only if the license is for the basic
     * version of LightZone.
     */
    public static boolean isBasic() {
        if ( m_licenseKey == null )
            return false;
        final ESDLicenseDialogs dialogs = new eSellerateLicenseDialogs();
        final ESDLicenseLibrary lib =
            ESDLicenseCheckerModule.getLicenseLibrary( dialogs );
        return lib != null && lib.isBasicKey( m_licenseKey );
    }

    /**
     * Gets the license key.  Note that {@link #checkLicense()} must have been
     * called prior to calling this method.
     *
     * @return Returns the license key or <code>null</code> if none.
     */
    public static String getLicenseKey() {
        return m_licenseKey;
    }

    /**
     * Checks whether the trial license has expired.
     *
     * @return Returns <code>true</code> only if it has.
     */
    public static boolean hasExpiredTrialLicense() {
        return m_hasExpiredTrialLicense;
    }

    /**
     * Show the user a dialog with "Buy Now" and "Enter Serial #" buttons,
     * and maybe use the key to define a license.
     *
     * @return Returns <code>true</code> if a valid license key was entered.
     */
    public static boolean license() {
        final String licenseType =
            System.getProperty( "com.lightcrafts.licensetype" );
        if ( !"ESD".equals( licenseType ) )
            throw new IllegalStateException( "Can not license non-ESD.");
        final ESDLicenseCheckerModule licenseChecker =
            new ESDLicenseCheckerModule();
        final ESDLicenseDialogs dialogs = new eSellerateLicenseDialogs();
        final ESDLicenseLibrary lib = ESDLicenseCheckerModule.getLicenseLibrary( dialogs );
        final Date date = lib.getTrialLicenseExpirationDate();
        final ESDLicenseDialogs.Response response = dialogs.showTrialExpirationDialog( date );
        switch ( response ) {
            case ESD_BUY_NOW:
                lib.buyNow();
                return false;
            case ESD_CONTINUE_TRIAL:
                return false;
            case ESD_ENTER_KEY:
                return ESDLicenseManager.enterAndValidateKey( lib, dialogs );
        }
        m_licenseKey = licenseChecker.getLicenseKey();
        return m_licenseKey != null;
    }

    /**
     * Relicenses the application with a new license key.
     *
     * @return Returns <code>true</code> only if relicensing occurred.
     */
    public static boolean relicense() {
        final String licenseType =
            System.getProperty( "com.lightcrafts.licensetype" );
        if ( !"ESD".equals( licenseType ) )
            throw new IllegalStateException( "Can not relicense non-ESD.");
        final ESDLicenseCheckerModule licenseChecker =
            new ESDLicenseCheckerModule();
        if ( licenseChecker.relicense() ) {
            m_licenseKey = licenseChecker.getLicenseKey();
            return true;
        }
        return false;
    }

    /**
     * Unlicenses the application.
     *
     * @return Returns <code>true</code> only if unlicensing occurred.
     */
    public static boolean unlicense() {
        final String licenseType =
            System.getProperty( "com.lightcrafts.licensetype" );
        if ( !"ESD".equals( licenseType ) )
            throw new IllegalStateException( "Can not unlicense non-ESD.");
        final ESDLicenseCheckerModule licenseChecker =
            new ESDLicenseCheckerModule();
        if ( licenseChecker.unlicense() ) {
            m_licenseKey = null;
            return true;
        }
        return false;
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * Ensure that the given {@link LicenseLibrary}'s <code>dispose()</code>
     * method is called before the application terminates.
     *
     * @param lib The {@link LicenseLibrary} to ensure the call of
     * <code>dispose()</code> for.
     */
    static void ensureDisposeOf( final LicenseLibrary lib ) {
        Runtime.getRuntime().addShutdownHook(
            new Thread( "LicenseLibrary Disposer" ) {
                public void run() {
                    lib.dispose();
                }
            }
        );
    }

    static boolean m_enteredLicenseKey;

    ////////// private ////////////////////////////////////////////////////////

    /**
     * Private so that no instances can be created.
     */
    private LicenseChecker() {
        // do nothing
    }

    private static boolean m_hasExpiredTrialLicense;

    private static String m_licenseKey;
}
/* vim:set et sw=4 ts=4: */
