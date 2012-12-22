/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

import java.util.regex.Pattern;

/**
 * A <code>LicenseLibrary</code> is an abstract interface for a license
 * library.
 *
 * @author Paul J. Lucas [plucas@lightcrafts.com]
 */
public interface LicenseLibrary {

    /**
     * Dispose of the <code>LicenseLibrary</code> and all of its resources.
     */
    void dispose();

    /**
     * Gets the license key.
     *
     * @return Returns said key.
     */
    String getLicenseKey();

    /**
     * Gets the {@link Pattern} of license keys.
     *
     * @return Returns said {@link Pattern}.
     */
    Pattern getLicenseKeyPattern();

    /**
     * Checks whether the given license key is valid.
     *
     * @param key The license key to check.
     * @return Returns <code>true</code> only if the given license key is
     * valid.
     */
    boolean isKeyValid( String key );

    /**
     * Save the license key to disk.
     *
     * @param key The license key to save.
     */
    boolean saveLicenseKey( String key );

}
/* vim:set et sw=4 ts=4: */
