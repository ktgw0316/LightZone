/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

import java.util.Date;

import static com.lightcrafts.license.ESDLicenseLibrary.LicenseType.*;
import static com.lightcrafts.license.ESDLicenseLibrary.NO_ESD_ERROR;
import static com.lightcrafts.license.Locale.LOCALE;
import com.lightcrafts.platform.Platform;

/**
 * A <code>ESDLicenseChecker</code> checks that we have a valid license for
 * the ESD version.
 */
class ESDLicenseCheckerModule extends LicenseCheckerModule {

    ////////// public /////////////////////////////////////////////////////////

    public void run() {
        final ESDLicenseDialogs dialogs = new eSellerateLicenseDialogs();
        final ESDLicenseLibrary lib = getLicenseLibrary( dialogs );
        if ( lib == null || !ESDLicenseManager.getLicense( lib, dialogs ) )
            System.exit( 0 );
    }

    ////////// package ////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    String getLicenseKey() {
        return m_lib.getLicenseKey();
    }

    /**
     * {@inheritDoc}
     */
    String getMessage() {
        final String key = getLicenseKey();
        return  LOCALE.get( "LicenseLabel" ) + ": " +
                (key != null && key.length() > 0 ?
                    key : LOCALE.get( "TrialLicenseSN" ));
    }

    /**
     * {@inheritDoc}
     */
    boolean hasExpiredTrialLicense() {
        if ( m_lib.getLicenseType() == LICENSE_NORMAL )
            return false;
        final Date expirationDate = m_lib.getTrialLicenseExpirationDate();
        if ( expirationDate == null )
            return false;
        return trialLicenseRefDate.after( expirationDate );
    }

    /**
     * Relicenses the application with a new license key.
     *
     * @return Returns <code>true</code> only if relicensing occurred.
     */
    @SuppressWarnings({"MethodMayBeStatic"})
    boolean relicense() {
        final ESDLicenseDialogs dialogs = new eSellerateLicenseDialogs();
        final ESDLicenseLibrary lib = getLicenseLibrary( dialogs );
        return lib != null && ESDLicenseManager.relicense( lib, dialogs );
    }

    /**
     * Unlicenses the application.
     *
     * @return Returns <code>true</code> only if unlicensing occurred.
     */
    @SuppressWarnings({"MethodMayBeStatic"})
    boolean unlicense() {
        final ESDLicenseDialogs dialogs = new eSellerateLicenseDialogs();
        final ESDLicenseLibrary lib = getLicenseLibrary( dialogs );
        return lib != null && ESDLicenseManager.unlicense( lib, dialogs );
    }

    /**
     * The {@link Date} to use to to compare to the trial license expiration
     * date to know whether it has expired.
     */
    static final Date trialLicenseRefDate = new Date();

    /**
     * Gets the {@link ESDLicenseLibrary} to use.  It is guaranteed to have
     * been initialized.
     *
     * @param dialogs The {@link ESDLicenseDialogs} to use.
     * @return Returns said {@link ESDLicenseLibrary}.
     */
    synchronized static ESDLicenseLibrary
    getLicenseLibrary( ESDLicenseDialogs dialogs ) {
        if ( m_lib == null ) {
            if ( Platform.getType() != Platform.Linux ) {
                if ( MakeItWorkLicenseLibrary.fileExists() )
                    m_lib = new MakeItWorkLicenseLibrary();
                else
                    m_lib = new eSellerateLicenseLibrary();
            } else {
                m_lib = new DummyLicenseLibrary();
            }
            final ESDLicenseLibrary.ESDError error = m_lib.initialize();
            if ( error != NO_ESD_ERROR ) {
                dialogs.showErrorAlert( m_lib.getErrorMessage( error ), true );
                return null;
            }
            LicenseChecker.ensureDisposeOf( m_lib );
        }
        return m_lib;
    }

    ////////// private ////////////////////////////////////////////////////////

    /**
     * The singleton instance of the {@link ESDLicenseLibrary}.
     */
    private static ESDLicenseLibrary m_lib;
}
/* vim:set et sw=4 ts=4: */
