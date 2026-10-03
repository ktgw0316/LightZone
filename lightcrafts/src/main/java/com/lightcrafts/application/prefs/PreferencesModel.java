package com.lightcrafts.application.prefs;

import com.lightcrafts.domain.prefs.InteractiveUpdateFrequency;
import com.lightcrafts.domain.prefs.SaveFormat;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public record PreferencesModel(
    String language,
    List<String> availableLanguages,
    GeneralSettings general,
    SaveFormat saveFormat,
    CopyrightSettings copyright
) {
    public PreferencesModel {
        Objects.requireNonNull(language);
        availableLanguages = List.copyOf(availableLanguages);
        Objects.requireNonNull(general);
        Objects.requireNonNull(saveFormat);
        Objects.requireNonNull(copyright);
    }

    public PreferencesModel withLanguage(String value) {
        return new PreferencesModel(
            value, availableLanguages, general, saveFormat, copyright
        );
    }

    public PreferencesModel withGeneral(GeneralSettings value) {
        return new PreferencesModel(
            language, availableLanguages, value, saveFormat, copyright
        );
    }

    public PreferencesModel withSaveFormat(SaveFormat value) {
        return new PreferencesModel(
            language, availableLanguages, general, value, copyright
        );
    }

    public PreferencesModel withCopyright(CopyrightSettings value) {
        return new PreferencesModel(
            language, availableLanguages, general, saveFormat, value
        );
    }

    public record GeneralSettings(
        int minimumMemoryLimit,
        int maximumMemoryLimit,
        int memoryLimit,
        InteractiveUpdateFrequency interactiveUpdateFrequency,
        String scratchDirectory,
        boolean scratchDirectoryEnabled,
        String displayProfilePath,
        boolean displayProfileAvailable,
        boolean displayProfileEnabled,
        boolean localBrowserCache,
        boolean checkForUpdate,
        boolean exportByOriginal,
        boolean saveByOriginal,
        boolean autoSave,
        boolean integrationEnabled,
        boolean showToolTitles
    ) {
        public GeneralSettings {
            Objects.requireNonNull(interactiveUpdateFrequency);
            Objects.requireNonNull(scratchDirectory);
            Objects.requireNonNull(displayProfilePath);
        }

        public GeneralSettings withMemoryLimit(int value) {
            return copy(builder -> builder.memoryLimit = value);
        }

        public GeneralSettings withInteractiveUpdateFrequency(
            InteractiveUpdateFrequency value
        ) {
            return copy(builder ->
                builder.interactiveUpdateFrequency = value
            );
        }

        public GeneralSettings withScratchDirectory(String value) {
            return copy(builder -> {
                builder.scratchDirectory = value;
                builder.scratchDirectoryEnabled = true;
            });
        }

        public GeneralSettings withDisplayProfilePath(String value) {
            return copy(builder -> {
                builder.displayProfilePath = value;
                builder.displayProfileEnabled = true;
            });
        }

        public GeneralSettings withLocalBrowserCache(boolean value) {
            return copy(builder -> builder.localBrowserCache = value);
        }

        public GeneralSettings withCheckForUpdate(boolean value) {
            return copy(builder -> builder.checkForUpdate = value);
        }

        public GeneralSettings withExportByOriginal(boolean value) {
            return copy(builder -> builder.exportByOriginal = value);
        }

        public GeneralSettings withSaveByOriginal(boolean value) {
            return copy(builder -> builder.saveByOriginal = value);
        }

        public GeneralSettings withAutoSave(boolean value) {
            return copy(builder -> builder.autoSave = value);
        }

        public GeneralSettings withIntegrationEnabled(boolean value) {
            return copy(builder -> builder.integrationEnabled = value);
        }

        public GeneralSettings withShowToolTitles(boolean value) {
            return copy(builder -> builder.showToolTitles = value);
        }

        private GeneralSettings copy(Consumer<Builder> update) {
            Builder builder = new Builder(this);
            update.accept(builder);
            return builder.build();
        }

        private static final class Builder {
            private final int minimumMemoryLimit;
            private final int maximumMemoryLimit;
            private int memoryLimit;
            private InteractiveUpdateFrequency interactiveUpdateFrequency;
            private String scratchDirectory;
            private boolean scratchDirectoryEnabled;
            private String displayProfilePath;
            private boolean displayProfileAvailable;
            private boolean displayProfileEnabled;
            private boolean localBrowserCache;
            private boolean checkForUpdate;
            private boolean exportByOriginal;
            private boolean saveByOriginal;
            private boolean autoSave;
            private boolean integrationEnabled;
            private boolean showToolTitles;

            private Builder(GeneralSettings settings) {
                minimumMemoryLimit = settings.minimumMemoryLimit;
                maximumMemoryLimit = settings.maximumMemoryLimit;
                memoryLimit = settings.memoryLimit;
                interactiveUpdateFrequency =
                    settings.interactiveUpdateFrequency;
                scratchDirectory = settings.scratchDirectory;
                scratchDirectoryEnabled = settings.scratchDirectoryEnabled;
                displayProfilePath = settings.displayProfilePath;
                displayProfileAvailable = settings.displayProfileAvailable;
                displayProfileEnabled = settings.displayProfileEnabled;
                localBrowserCache = settings.localBrowserCache;
                checkForUpdate = settings.checkForUpdate;
                exportByOriginal = settings.exportByOriginal;
                saveByOriginal = settings.saveByOriginal;
                autoSave = settings.autoSave;
                integrationEnabled = settings.integrationEnabled;
                showToolTitles = settings.showToolTitles;
            }

            private GeneralSettings build() {
                return new GeneralSettings(
                    minimumMemoryLimit,
                    maximumMemoryLimit,
                    memoryLimit,
                    interactiveUpdateFrequency,
                    scratchDirectory,
                    scratchDirectoryEnabled,
                    displayProfilePath,
                    displayProfileAvailable,
                    displayProfileEnabled,
                    localBrowserCache,
                    checkForUpdate,
                    exportByOriginal,
                    saveByOriginal,
                    autoSave,
                    integrationEnabled,
                    showToolTitles
                );
            }
        }
    }

    public record CopyrightSettings(
        String defaultCopyright,
        String defaultCreator,
        String copyrightText,
        String creatorText
    ) {
        public CopyrightSettings withCopyrightText(String value) {
            return new CopyrightSettings(
                defaultCopyright, defaultCreator, value, creatorText
            );
        }

        public CopyrightSettings withCreatorText(String value) {
            return new CopyrightSettings(
                defaultCopyright, defaultCreator, copyrightText, value
            );
        }

        public CopyrightSettings reverted() {
            return new CopyrightSettings(
                defaultCopyright, defaultCreator,
                defaultCopyright, defaultCreator
            );
        }
    }
}
