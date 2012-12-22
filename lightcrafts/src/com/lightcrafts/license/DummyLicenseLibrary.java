/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.license;

import com.lightcrafts.utils.WebBrowser;

import java.util.Date;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

/**
 * An ESDLicenseLibrary that doesn't actually check licenses.  It can substitute
 * for eSellerateLicenseLibrary if you don't want to require native libraries or
 * network access.
 * <p>
 * This class is public just so it can expose getLicenseState() and
 * setLicenseState(), which are used to preserve license state when the user
 * clears preferences in com.lightcrafts.prefs.ClearPrefsItem.
 */

public final class DummyLicenseLibrary implements ESDLicenseLibrary {

    // The license key submitted by the user is stored in preferences.
    private static Preferences LicensePrefs = Preferences.userRoot().node(
        "/com/lightcrafts/license"
    );
    private final static String DummyLicenseKeyPrefsKey = "DummyLicenseKey";
    private final static String TrialLicenseStartPrefsKey = "TrialLicenseStart";

    private static final ResourceBundle Resources = ResourceBundle.getBundle(
        "com.lightcrafts.license.resources.eSellerate"
    );

    private static final String LICENSE_KEY_PATTERN_STRING =
        "\\w{10}(?:-[A-HJ-NP-TV-Y0-9]{4}){5}";

    private static final Pattern LICENSE_KEY_PATTERN =
        Pattern.compile( LICENSE_KEY_PATTERN_STRING );

    public ESDError activateKey( String key ) {
        setLicenseKey(key);
        return NO_ESD_ERROR;
    }

    public ESDError buyNow() {
        WebBrowser.browse( Resources.getString( "BuyNowURL" ) );
        return NO_ESD_ERROR;
    }

    public ESDError deactivateKey( String key ) {
        LicensePrefs.remove(DummyLicenseKeyPrefsKey);
        return NO_ESD_ERROR;
    }

    public void dispose() {
        // do nothing
    }

    public String getErrorMessage( ESDError error ) {
        return "Sorry, something's wrong with the licensing system.";
    }

    public String getLicenseKey() {
        return LicensePrefs.get(DummyLicenseKeyPrefsKey, null);
    }

    public static void setLicenseKey(String key) {
        LicensePrefs.put(DummyLicenseKeyPrefsKey, key);
    }

    // Expose the license state so it can be backed up and restored when
    // the user clears preferences.  See com.lightcrafts.prefs.ClearPrefsItem.
    public static Object getLicenseState() {
        String key = LicensePrefs.get(DummyLicenseKeyPrefsKey, null);
        if (key != null) {
            return key;
        }
        long start = LicensePrefs.getLong(TrialLicenseStartPrefsKey, 0);
        if (start > 0) {
            return start;
        }
        return null;
    }

    // Expose the license state so it can be backed up and restored when
    // the user clears preferences.  See com.lightcrafts.prefs.ClearPrefsItem.
    public static void setLicenseState(Object state) {
        // The static Preferences node is likely to have been removed, since
        // this function is normally called right after a user preferences
        // reset.
        LicensePrefs = Preferences.userRoot().node("/com/lightcrafts/license");

        if (state instanceof String) {
            LicensePrefs.put(DummyLicenseKeyPrefsKey, (String) state);
        }
        else if (state instanceof Long) {
            LicensePrefs.putLong(TrialLicenseStartPrefsKey, (Long) state);
        }
        // otherwise do nothing
    }

    public LicenseType getLicenseType() {
        if (getLicenseKey() != null) {
            return LicenseType.LICENSE_NORMAL;
        }
        if (getTrialLicenseExpirationDate() != null) {
            return LicenseType.LICENSE_TRIAL;
        }
        return LicenseType.LICENSE_INVALID;
    }

    public static long getTrialLicenseStart() {
        return LicensePrefs.getLong(TrialLicenseStartPrefsKey, 0);
    }

    public static void setTrialLicenseStartDate() {
        LicensePrefs.putLong(
            TrialLicenseStartPrefsKey, System.currentTimeMillis()
        );
    }

    public Date getTrialLicenseExpirationDate() {
        long start = getTrialLicenseStart();
        if (start > 0) {
            // Trial duration is 30 days from first launch.
            return new Date(start + 30 * 24 * 60 * 60 * 1000L);
        }
        return null;
    }

    public Pattern getLicenseKeyPattern() {
        return LICENSE_KEY_PATTERN;
    }

    public ESDError initialize() {
        return NO_ESD_ERROR;
    }

    public boolean isBasicKey( String key ) {
        return false;
    }

    public boolean isKeyActivated( String key ) {
        return false;
    }

    public boolean isKeyValid( String key ) {
        return LICENSE_KEY_PATTERN.matcher(key).matches();
    }

    public boolean isInternetError( ESDError error ) {
        return false;
    }

    public ESDError manuallyActivateKey( String key ) {
        setLicenseKey( key );
        return NO_ESD_ERROR;
    }

    public ESDError probeLicenseType() {
        return NO_ESD_ERROR;
    }

    public ESDError requestLicense() {
        return NO_ESD_ERROR;
    }

    public boolean saveLicenseKey( String key ) {
        return true;
    }

    public ESDError tryNow( String key ) {
        setTrialLicenseStartDate();
        return NO_ESD_ERROR;
    }

}
