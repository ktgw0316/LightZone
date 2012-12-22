/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

/**
 * A <code>LicenseCheckerModule</code> defines the base API for all license
 * checker modules.
 *
 * @author Paul J. Lucas [plucas@lightcrafts.com]
 */
abstract class LicenseCheckerModule implements Runnable {

    /**
     * Gets the license key, if any.
     *
     * @return Returns the license key or <code>null</code> if none.
     */
    String getLicenseKey() {
        return null;
    }

    /**
     * Gets a message, if any, to be displayed to the user.
     *
     * @return Returns said message or <code>null</code> if none.
     */
    String getMessage() {
        return null;
    }

    /**
     * Checks whether the trial license has expired.
     *
     * @return Returns <code>true</code> only if it has.
     */
    boolean hasExpiredTrialLicense() {
        return false;
    }

}
/* vim:set et sw=4 ts=4: */
