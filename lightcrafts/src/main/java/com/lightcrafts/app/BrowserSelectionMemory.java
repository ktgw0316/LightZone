/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.utils.file.FileUtil;

import java.io.File;
import java.util.prefs.Preferences;

/**
 * A static utility to notice browser selections and remember the most
 * recent lead-selected file for each folder, so it can be restored the next
 * time the same folder is browsed.
 */
class BrowserSelectionMemory {

    private final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/app"
    );
    private final static String Key = "BrowserSelectionMemory";

    static void setRememberedFile(File file) {
        File folder = file.getParentFile();
        String key = getKeyForFolder(folder);
        file = FileUtil.resolveAliasFile(file);
        String path = file.getAbsolutePath();
        Prefs.put(key, path);
    }
    
    static File getRememberedFile(File folder) {
        String key = getKeyForFolder(folder);
        String path = Prefs.get(key, null);
        return (path != null) ? new File(path) : null;
    }

    private static String getKeyForFolder(File folder) {
        folder = FileUtil.resolveAliasFile(folder);
        return Key + folder.getAbsolutePath().hashCode();
    }
}
