/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.shell.prefs;

import com.lightcrafts.utils.MemoryLimits;

import java.util.prefs.Preferences;

public final class PreferenceSettingsStorage {

    private static final String APP_PACKAGE = "/com/lightcrafts/app";
    private static final String AUTO_SAVE_KEY = "AutoSave";
    private static final String CHECK_FOR_UPDATE_KEY = "CheckForUpdate";
    private static final String VIDEO_LEARNING_CENTER_POPUP_KEY =
        "VideoLearningCenterPopup";
    private static final String MAX_MEMORY_KEY = "MaxMemory";
    private static final String INTEGRATION_PACKAGE = "/com/lightcrafts/app/other";
    private static final String INTEGRATION_ENABLED_KEY = "IntegrationEnabled";
    private static final String IMAGE_EDITOR_PACKAGE =
        "/com/lightcrafts/model/ImageEditor";
    private static final String INTERACTIVE_UPDATE_KEY = "InteractiveUpdate";

    private PreferenceSettingsStorage() {
    }

    public static boolean getAutoSaveEnabled() {
        return appPreferences().getBoolean(AUTO_SAVE_KEY, true);
    }

    public static void setAutoSaveEnabled(boolean enabled) {
        appPreferences().putBoolean(AUTO_SAVE_KEY, enabled);
    }

    public static boolean getCheckForUpdateEnabled() {
        return appPreferences().getBoolean(CHECK_FOR_UPDATE_KEY, true);
    }

    public static void setCheckForUpdateEnabled(boolean enabled) {
        appPreferences().putBoolean(CHECK_FOR_UPDATE_KEY, enabled);
    }

    public static boolean getVideoLearningCenterPopupEnabled() {
        return appPreferences().getBoolean(VIDEO_LEARNING_CENTER_POPUP_KEY, true);
    }

    public static void setVideoLearningCenterPopupEnabled(boolean enabled) {
        appPreferences().putBoolean(VIDEO_LEARNING_CENTER_POPUP_KEY, enabled);
    }

    public static boolean getDirectoryByOriginal(String key) {
        return appPreferences().getBoolean(key, true);
    }

    public static void setDirectoryByOriginal(String key, boolean byOriginal) {
        appPreferences().putBoolean(key, byOriginal);
    }

    public static boolean getOtherAppIntegrationEnabled() {
        return integrationPreferences().getBoolean(INTEGRATION_ENABLED_KEY, true);
    }

    public static void setOtherAppIntegrationEnabled(boolean enabled) {
        integrationPreferences().putBoolean(INTEGRATION_ENABLED_KEY, enabled);
    }

    public static int getMinimumMemoryLimit() {
        return MemoryLimits.getMinimum();
    }

    public static int getMaximumMemoryLimit() {
        return MemoryLimits.getMaximum();
    }

    public static int getMemoryLimit() {
        return appPreferences().getInt(MAX_MEMORY_KEY, MemoryLimits.getDefault());
    }

    public static void setMemoryLimit(int limit) {
        appPreferences().putInt(MAX_MEMORY_KEY, limit);
    }

    public static int getInteractiveUpdatePreferenceValue(int defaultValue) {
        return imageEditorPreferences().getInt(INTERACTIVE_UPDATE_KEY, defaultValue);
    }

    public static void setInteractiveUpdatePreferenceValue(int value) {
        imageEditorPreferences().putInt(INTERACTIVE_UPDATE_KEY, value);
    }

    private static Preferences appPreferences() {
        return Preferences.userRoot().node(APP_PACKAGE);
    }

    private static Preferences integrationPreferences() {
        return Preferences.userRoot().node(INTEGRATION_PACKAGE);
    }

    private static Preferences imageEditorPreferences() {
        return Preferences.userRoot().node(IMAGE_EDITOR_PACKAGE);
    }
}
