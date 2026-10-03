package com.lightcrafts.application.prefs;

import com.lightcrafts.domain.prefs.InteractiveUpdateFrequency;
import com.lightcrafts.domain.prefs.SaveFormat;

public sealed interface PreferencesMessage {
    record LanguageChanged(String language) implements PreferencesMessage {}
    record MemoryLimitChanged(int value) implements PreferencesMessage {}
    record InteractiveUpdateChanged(InteractiveUpdateFrequency frequency)
        implements PreferencesMessage {}
    record ScratchDirectoryChanged(String path)
        implements PreferencesMessage {}
    record DisplayProfileChanged(String path)
        implements PreferencesMessage {}
    record BrowserCacheScopeChanged(boolean local)
        implements PreferencesMessage {}
    record CheckForUpdateChanged(boolean enabled)
        implements PreferencesMessage {}
    record ExportDirectoryModeChanged(boolean byOriginal)
        implements PreferencesMessage {}
    record SaveDirectoryModeChanged(boolean byOriginal)
        implements PreferencesMessage {}
    record AutoSaveChanged(boolean enabled) implements PreferencesMessage {}
    record IntegrationChanged(boolean enabled) implements PreferencesMessage {}
    record ToolTitlesChanged(boolean enabled) implements PreferencesMessage {}
    record SaveFormatChanged(SaveFormat format)
        implements PreferencesMessage {}
    record CopyrightChanged(String text) implements PreferencesMessage {}
    record CreatorChanged(String text) implements PreferencesMessage {}
    record RevertCopyrightDefaults() implements PreferencesMessage {}
    record Apply() implements PreferencesMessage {}
    record ChooseScratchDirectory() implements PreferencesMessage {}
    record ChooseDisplayProfile() implements PreferencesMessage {}
    record ClearBrowserCache() implements PreferencesMessage {}
    record ClearPreferences() implements PreferencesMessage {}
    record ScratchDirectoryChosen(String path)
        implements PreferencesMessage {}
    record DisplayProfileChosen(String path)
        implements PreferencesMessage {}
    record BrowserCacheCleared() implements PreferencesMessage {}
    record BrowserCacheClearConfirmed() implements PreferencesMessage {}
    record BrowserCacheClearCancelled() implements PreferencesMessage {}
    record ClearPreferencesConfirmed() implements PreferencesMessage {}
    record ClearPreferencesCancelled() implements PreferencesMessage {}
}
