package com.lightcrafts.ui.prefs;

import com.lightcrafts.application.prefs.PreferencesEffect;
import com.lightcrafts.application.prefs.PreferencesMessage;
import com.lightcrafts.image.export.ImageExportOptions;

import java.util.function.Consumer;

public interface PreferencesEffectHandler {
    void handle(
        PreferencesEffect effect,
        ImageExportOptions selectedExportOptions,
        Consumer<PreferencesMessage> dispatch,
        PreferencesUi ui
    );
}
