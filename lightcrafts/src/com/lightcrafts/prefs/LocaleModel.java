/*
 * Copyright (c) 2019. Masahiro Kitagawa
 */

package com.lightcrafts.prefs;

import lombok.Getter;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Locale;
import java.util.prefs.Preferences;

public class LocaleModel {
    private final static String defaultLanguage = "";
    private static final ResourceBundle resource =
            ResourceBundle.getBundle(LocaleModel.class.getName() + "_ALL");

    private final static String Package = "/com/lightcrafts/app";
    private final static String Key = "Locale";
    private final static Preferences Prefs = Preferences.userRoot().node(Package);

    @Getter
    private static final List<String> availableLanguages = new ArrayList<>();
    static {
        availableLanguages.add(defaultLanguage);
        val langList = Arrays.asList(resource.getString("availableLanguages").split(","));
        availableLanguages.addAll(langList);
    }

    @NotNull
    public static String setDefaultFromPreference() {
        val lang = Prefs.get(Key, defaultLanguage);
        if (!lang.equals(defaultLanguage) && availableLanguages.contains(lang)) {
            Locale.setDefault(new Locale(lang));
            return lang;
        }
        return defaultLanguage;
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
