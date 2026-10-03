/* Copyright (c) 2026. Masahiro Kitagawa */

package com.lightcrafts.domain.prefs;

import org.jetbrains.annotations.NotNull;
import java.util.Arrays;

public enum InteractiveUpdateFrequency {
    FREQUENT(0),
    NORMAL(1),
    INFREQUENT(2);

    public final int preferenceValue;

    InteractiveUpdateFrequency(int preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public static @NotNull InteractiveUpdateFrequency fromPreferenceValue(int value) {
        return Arrays.stream(values())
                .filter(f -> f.preferenceValue == value)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid interactive update frequency: " + value));
    }
}
