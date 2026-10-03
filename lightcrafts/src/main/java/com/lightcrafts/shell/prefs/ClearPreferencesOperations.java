/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.shell.prefs;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public final class ClearPreferencesOperations {

    private ClearPreferencesOperations() {
    }

    public static void clear() throws BackingStoreException {
        // On Linux, the license state resides in preferences.
        Preferences node = Preferences.userRoot().node("/com/lightcrafts");
        node.removeNode();
        node.flush();
    }
}
