/*
 * Copyright (c) 2026. Masahiro Kitagawa
 */

package com.lightcrafts.domain.prefs;

import java.util.Objects;

public final class SaveFormatPreference {

    private SaveFormatPreference() {
    }

    public static boolean usesDefaultOptions(
            SaveFormat defaultFormat, SaveFormat candidateFormat) {
        return Objects.requireNonNull(defaultFormat)
                == Objects.requireNonNull(candidateFormat);
    }
}
