/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import com.lightcrafts.license.LicenseChecker;
import static com.lightcrafts.prefs.Locale.LOCALE;

import javax.swing.*;
import java.util.prefs.Preferences;

/**
 * A model for the "basic" feature, which used to be called "RT".
 * <p>
 * In this mode:
 * <ul>
 * <li>ComboFrame limits layouts to the EditorLayout;</li>
 * <li>all the layout menu items are disabled;</li>
 * <li>ApertureLogic becomes active, causing the TIFF writeback behavior.</li>
 * </ul>
 * <p>
 * This mode may be determined by the current license type.  If it is not, then
 * it may be set programmatically.  The "basic" feature is off by default.
 */
public class ApplicationMode {

    public final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/app"
    );
    private final static String ModeKey = "AppMode";

    private final static String FullModeValue = "Full";
    private final static String BasicModeValue = "Basic";

    private final static String BasicToolTip = LOCALE.get("UpgradeToolTip");

    public static boolean isBasicMode() {
        return
            LicenseChecker.isBasic() ||
            Prefs.get(ModeKey, FullModeValue).equals(BasicModeValue);
    }
    
    public static boolean canSetBasicMode() {
        return ! LicenseChecker.isBasic();
    }

    public static void maybeSetToolTip(JComponent comp) {
        if (isBasicMode()) {
            String text = comp.getToolTipText();
            comp.setToolTipText(text + " - " + BasicToolTip);
        }
    }

    // After licensing, this preference must be reset so the licensed
    // functionality will be apparent.
    public static void resetPreference() {
        Prefs.remove(ModeKey);
    }

    static void setBasicMode(boolean on) {
        if (! LicenseChecker.isBasic()) {
            Prefs.putBoolean(ModeKey, on);
        }
    }
}
