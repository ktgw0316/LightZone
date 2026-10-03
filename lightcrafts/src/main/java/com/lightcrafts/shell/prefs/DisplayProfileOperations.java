/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.shell.prefs;

import java.awt.color.ICC_Profile;
import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

public final class DisplayProfileOperations {

    private static final String PACKAGE = "/com/lightcrafts/platform/linux";
    private static final String KEY = "DisplayProfile";
    private static final Preferences preferences =
        Preferences.userRoot().node(PACKAGE);

    private DisplayProfileOperations() {
    }

    public static String getCurrentPath() {
        return preferences.get(KEY, null);
    }

    public static void setCurrentPath(String path) {
        preferences.put(KEY, path);
    }

    public static void removeCurrentPath() {
        preferences.remove(KEY);
    }

    public static void validate(File file) throws IOException {
        ICC_Profile.getInstance(file.getAbsolutePath());
    }
}
