/*
 * Copyright (c) 2019. Masahiro Kitagawa
 */

package com.lightcrafts.prefs;

import lombok.val;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

public class LocaleModel {
    private final static String Package = "/com/lightcrafts/app";
    private final static String Key = "Locale";
    private final static Preferences Prefs = Preferences.userRoot().node(Package);

    public static String setDefaultFromPreference() {
        val lang = Prefs.get(Key, "");
        if (!lang.isEmpty()) {
            Locale.setDefault(new Locale(lang));
        }
        return lang;
    }

    public static void setPreference(String lang) {
        Prefs.put(Key, lang);
        Locale.setDefault(new Locale(lang));
        ResourceBundle.clearCache();
    }

    public static void removePreference() {
        Prefs.remove(Key);
        ResourceBundle.clearCache();
    }

    private LocaleModel() {}
}
