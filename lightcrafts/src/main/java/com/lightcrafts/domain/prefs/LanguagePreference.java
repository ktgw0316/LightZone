/* Copyright (c) 2026. Masahiro Kitagawa */

package com.lightcrafts.domain.prefs;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class LanguagePreference {
    public static final String DEFAULT_LANGUAGE = "";

    private LanguagePreference() {
    }

    @NotNull
    public static String normalize(@NotNull String language) {
        return language.trim();
    }

    public static boolean isSupported(
            @NotNull String language, @NotNull List<String> availableLanguages) {
        final var normalized = normalize(language);
        return availableLanguages.stream().anyMatch(l -> l.equals(normalized));
    }
}
