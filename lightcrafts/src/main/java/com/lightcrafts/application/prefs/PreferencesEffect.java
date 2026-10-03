package com.lightcrafts.application.prefs;

public sealed interface PreferencesEffect {
    record ApplyPreferences(PreferencesModel settings)
        implements PreferencesEffect {}
    record ChooseScratchDirectory(String currentPath)
        implements PreferencesEffect {}
    record ChooseDisplayProfile(String currentPath, boolean useHomeDirectory)
        implements PreferencesEffect {}
    record ConfirmClearBrowserCache() implements PreferencesEffect {}
    record ConfirmClearPreferences() implements PreferencesEffect {}
    record ClearGlobalBrowserCache() implements PreferencesEffect {}
    record ClearAllPreferences() implements PreferencesEffect {}
}
