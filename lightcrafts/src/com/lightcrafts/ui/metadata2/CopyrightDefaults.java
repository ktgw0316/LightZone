/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import java.util.prefs.Preferences;

/**
 * A global place for setting the default copyright text.
 */
public class CopyrightDefaults {

    private final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/metadata2"
    );
    private final static String CopyrightKey = "Copyright";

    private final static String CreatorKey = "Creator";

    public static void setDefaultCopyright(String text) {
        if (text != null) {
            Prefs.put(CopyrightKey, text);
        }
        else {
            Prefs.remove(CopyrightKey);
        }
    }

    public static String getDefaultCopyright() {
        return Prefs.get(CopyrightKey, null);
    }

    public static void setDefaultCreator(String text) {
        if (text != null) {
            Prefs.put(CreatorKey, text);
        }
        else {
            Prefs.remove(CreatorKey);
        }
    }

    public static String getDefaultCreator() {
        return Prefs.get(CreatorKey, null);
    }
}
