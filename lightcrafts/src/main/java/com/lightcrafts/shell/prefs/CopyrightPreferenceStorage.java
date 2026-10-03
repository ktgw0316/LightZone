package com.lightcrafts.shell.prefs;

import java.util.prefs.Preferences;

public final class CopyrightPreferenceStorage {

    private static final String PACKAGE = "/com/lightcrafts/metadata2";
    private static final String COPYRIGHT_KEY = "Copyright";
    private static final String CREATOR_KEY = "Creator";
    private static final Preferences PREFERENCES =
        Preferences.userRoot().node(PACKAGE);

    private CopyrightPreferenceStorage() {
    }

    public static void setDefaultCopyright(String text) {
        if (text != null) {
            PREFERENCES.put(COPYRIGHT_KEY, text);
        }
        else {
            PREFERENCES.remove(COPYRIGHT_KEY);
        }
    }

    public static String getDefaultCopyright() {
        return PREFERENCES.get(COPYRIGHT_KEY, null);
    }

    public static void setDefaultCreator(String text) {
        if (text != null) {
            PREFERENCES.put(CREATOR_KEY, text);
        }
        else {
            PREFERENCES.remove(CREATOR_KEY);
        }
    }

    public static String getDefaultCreator() {
        return PREFERENCES.get(CREATOR_KEY, null);
    }
}
