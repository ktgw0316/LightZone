/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.shell.prefs;

import java.io.File;
import java.io.IOException;
import java.util.prefs.Preferences;

public final class ScratchDirectoryOperations {

    private static final String PACKAGE = "/com/lightcrafts/jai/utils";
    private static final String KEY = "ScratchDirectory";
    private static final Preferences PREFERENCES =
        Preferences.userRoot().node(PACKAGE);

    private ScratchDirectoryOperations() {
    }

    public static String getScratchDirectory() {
        return PREFERENCES.get(KEY, null);
    }

    public static void setScratchDirectory(String path) {
        PREFERENCES.put(KEY, path);
    }

    public static void removeScratchDirectory() {
        PREFERENCES.remove(KEY);
    }

    public static String getDefaultTempPath() {
        try {
            File tempFile = File.createTempFile("lzScratchProbe", "");
            File tempDir = tempFile.getParentFile();
            tempFile.delete();
            if (tempDir != null) {
                return tempDir.getAbsolutePath();
            }
            return null;
        }
        catch (IOException e) {
            return null;
        }
    }
}
