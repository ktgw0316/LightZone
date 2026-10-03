/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.metadata2;

import com.lightcrafts.shell.prefs.CopyrightPreferenceStorage;

/**
 * A global place for setting the default copyright text.
 */
public class CopyrightDefaults {

    public static void setDefaultCopyright(String text) {
        CopyrightPreferenceStorage.setDefaultCopyright(text);
    }

    public static String getDefaultCopyright() {
        return CopyrightPreferenceStorage.getDefaultCopyright();
    }

    public static void setDefaultCreator(String text) {
        CopyrightPreferenceStorage.setDefaultCreator(text);
    }

    public static String getDefaultCreator() {
        return CopyrightPreferenceStorage.getDefaultCreator();
    }
}
