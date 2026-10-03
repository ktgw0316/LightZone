/* Copyright (c) 2026. Masahiro Kitagawa */

package com.lightcrafts.application.prefs;

import com.lightcrafts.domain.prefs.LanguagePreference;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;

public final class LocalePreferenceWorkflow {

    private LocalePreferenceWorkflow() {
    }

    @NotNull
    public static String resolveStoredLanguage(
            @NotNull String storedLanguage, @NotNull List<String> availableLanguages) {
        final var normalized = LanguagePreference.normalize(storedLanguage);
        return LanguagePreference.isSupported(normalized, availableLanguages)
                ? normalized
                : LanguagePreference.DEFAULT_LANGUAGE;
    }

    @NotNull
    public static Optional<String> resolveSelectedLanguage(
            @NotNull String selectedLanguage, @NotNull List<String> availableLanguages) {
        final var normalized = LanguagePreference.normalize(selectedLanguage);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }
        if (!LanguagePreference.isSupported(normalized, availableLanguages)) {
            throw new IllegalArgumentException("Unsupported language: " + normalized);
        }
        return Optional.of(normalized);
    }
}
