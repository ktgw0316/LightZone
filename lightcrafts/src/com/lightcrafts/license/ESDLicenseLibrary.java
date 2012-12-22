/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

import java.util.Date;

/**
 * An <code>ESDLicenseLibrary</code> is a current best guess at having an
 * abstract interface for an ESD license library.
 *
 * @author Paul J. Lucas [plucas@lightcrafts.com]
 */
public interface ESDLicenseLibrary extends LicenseLibrary {

    enum LicenseType {
        LICENSE_INVALID,
        LICENSE_TRIAL,
        LICENSE_NORMAL,
    }

    /**
     * An <code>ESDError</code> represents an error when trying to perform a
     * method in the {@link ESDLicenseLibrary}.  It is a type-safe enumeration
     * that allows an implementing library to store an error, be it an integer
     * code or a message or something else.
     * @see ESDLicenseLibrary#getErrorMessage(ESDError)
     */
    abstract class ESDError {
        protected ESDError() {
        }
    }

    /**
     * A <code>NoError</code> is-an {@link ESDError} that is a type-safe
     * enumeration that represents &quot;no error.&quot;
     * @see ESDLicenseLibrary#NO_ESD_ERROR
     */
    final class NoError extends ESDError {
        private NoError() {
        }
    }

    /**
     * This is a singleton instance of {@link NoError} that all library methods
     * shall return when there is no error.
     */
    NoError NO_ESD_ERROR = new NoError();

    /**
     * Obtain a permanent license via the internet using the given license key.
     *
     * @param key The license key.
     * @return Returns an {@link ESDError} or {@link #NO_ESD_ERROR} if none.
     * @see #deactivateKey(String)
     * @see #manuallyActivateKey(String)
     */
    ESDError activateKey( String key );

    /**
     * Initiate the &quot;Buy Now&quot; sequence (usually directs the system's
     * web browser to go to the URL for the web store).
     *
     * @return Returns an {@link ESDError} or {@link #NO_ESD_ERROR} if none.
     */
    ESDError buyNow();

    /**
     * Deactivate a license via the internet using the given license key.
     *
     * @param key The license key.
     * @return Returns an {@link ESDError} or {@link #NO_ESD_ERROR} if none.
     * @see #activateKey(String)
     * @see #manuallyActivateKey(String)
     */
    ESDError deactivateKey( String key );

    /**
     * Gets the error message for the given {@link ESDError}.
     *
     * @param error The {@link ESDError} to get the error message for.
     * @return Returns said error message.
     */
    String getErrorMessage( ESDError error );

    /**
     * Gets the recently probed license type.
     *
     * @return Returns one of {@link LicenseType#LICENSE_INVALID},
     * {@link LicenseType#LICENSE_TRIAL}, or
     * {@link LicenseType#LICENSE_NORMAL}.
     * @see #probeLicenseType()
     */
    LicenseType getLicenseType();

    /**
     * Returns the expiration date of the trial license currently being used.
     *
     * @return Returns said date or <code>null</code> if the license isn't a
     * trial license.
     */
    Date getTrialLicenseExpirationDate();

    /**
     * Initialize the <code>ESDLicenseLibrary</code>.
     *
     * @return Returns an {@link ESDError} or {@link #NO_ESD_ERROR} if none.
     */
    ESDError initialize();

    /**
     * Test a license key to see whether to run in "basic" mode.
     * @param key A license key.
     * @return True if the key licenses the app for "basic" mode only.
     */
    boolean isBasicKey( String key );

    /**
     * Checks whether the given license key has previously been activated.
     *
     * @param key The license key to check.  It is assumed to have been first
     * checked for validity via {@link #isKeyValid(String)}.
     * @return Returns <code>true</code> only if the key has previously been
     * activated.
     */
    boolean isKeyActivated( String key );

    /**
     * Checks whether the given {@link ESDError} is an internet, connection, or
     * communications error.
     *
     * @param error The {@link ESDError} to check.
     * @return Returns <code>true</code> only if the error is an internet,
     * connection, or communications error.
     */
    boolean isInternetError( ESDError error );

    /**
     * Manually obtain a permanent license using the given license key.
     *
     * @param key The license key.
     * @return Returns an {@link ESDError} or {@link #NO_ESD_ERROR} if none.
     * @see #activateKey(String)
     * @see #deactivateKey(String)
     */
    ESDError manuallyActivateKey( String key );

    /**
     * Probes the current license type.  To obtain the type of license found,
     * use {@link #getLicenseType()}.
     *
     * @return Returns an {@link ESDError} or {@link #NO_ESD_ERROR} if none.
     * @see #getLicenseType()
     */
    ESDError probeLicenseType();

    /**
     * Request the use of the license.
     *
     * @return Returns an {@link ESDError} or {@link #NO_ESD_ERROR} if none.
     */
    ESDError requestLicense();

    /**
     * Obtain a trial license.
     *
     * @param key The trial license key.
     * @return Returns an {@link ESDError} or {@link #NO_ESD_ERROR} if none.
     */
    ESDError tryNow( String key );

}
/* vim:set et sw=4 ts=4: */
