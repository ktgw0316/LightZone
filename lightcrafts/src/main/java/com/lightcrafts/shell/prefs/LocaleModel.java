/*
 * Copyright (c) 2019. Masahiro Kitagawa
 */

package com.lightcrafts.shell.prefs;

import com.lightcrafts.application.prefs.LocalePreferenceWorkflow;
import com.lightcrafts.domain.prefs.LanguagePreference;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.Locale;
import java.util.prefs.Preferences;

public class LocaleModel {
    private static final String defaultLanguage = LanguagePreference.DEFAULT_LANGUAGE;
    private static final ResourceBundle resource =
            ResourceBundle.getBundle("com.lightcrafts.ui.prefs.LocaleModel_ALL");

    private static final String Package = "/com/lightcrafts/app";
    private static final String Key = "Locale";
    private static final Preferences Prefs = Preferences.userRoot().node(Package);

    @Getter
    private static final List<String> availableLanguages = new ArrayList<>();
    static {
        availableLanguages.add(defaultLanguage);
        final var langList = Arrays.asList(resource.getString("availableLanguages").split(","));
        availableLanguages.addAll(langList);
    }

    @NotNull
    public static String setDefaultFromPreference() {
        final var lang = LocalePreferenceWorkflow.resolveStoredLanguage(
                getStoredLanguage(), availableLanguages);
        applyResolvedLanguage(lang);
        return lang;
    }

    public static String getStoredLanguage() {
        return Prefs.get(Key, defaultLanguage);
    }

    public static void applyResolvedLanguage(String language) {
        if (!language.isEmpty()) {
            Locale.setDefault(new Locale(language));
        }
    }

    public static void setPreference(String lang) {
        final var normalized = LanguagePreference.normalize(lang);
        if (normalized.isEmpty()) {
            removePreference();
            return;
        }

        Prefs.put(Key, normalized);
        Locale.setDefault(new Locale(normalized));
        ResourceBundle.clearCache();
    }

    public static void removePreference() {
        Prefs.remove(Key);
        ResourceBundle.clearCache();
    }

    private LocaleModel() {}
}
