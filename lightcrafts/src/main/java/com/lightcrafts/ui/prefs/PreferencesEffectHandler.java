package com.lightcrafts.ui.prefs;

import com.lightcrafts.application.prefs.PreferencesEffect;
import com.lightcrafts.application.prefs.PreferencesMessage;
import com.lightcrafts.image.export.ImageExportOptions;

import java.awt.Component;
import java.util.function.Consumer;

public interface PreferencesEffectHandler {
    void handle(
        PreferencesEffect effect,
        Component parent,
        ImageExportOptions selectedExportOptions,
        Consumer<PreferencesMessage> dispatch
    );
}
