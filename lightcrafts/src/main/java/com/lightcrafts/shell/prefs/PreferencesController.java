package com.lightcrafts.shell.prefs;

import com.lightcrafts.application.prefs.LocalePreferenceWorkflow;
import com.lightcrafts.application.prefs.PreferencesEffect;
import com.lightcrafts.application.prefs.PreferencesMessage;
import com.lightcrafts.application.prefs.PreferencesModel;
import com.lightcrafts.domain.prefs.InteractiveUpdateFrequency;
import com.lightcrafts.domain.prefs.SaveFormat;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.image.types.JPEGImageType;
import com.lightcrafts.image.types.TIFFImageType;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.prefs.PreferencesEffectHandler;
import com.lightcrafts.ui.prefs.PreferencesUi;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;

public final class PreferencesController implements PreferencesEffectHandler {
    private static final String DefaultDisplayProfile = "(sRGB default)";
    private static final Logger logger =
        LoggerFactory.getLogger(PreferencesController.class);

    private PreferencesController() {
    }

    public static void showDialog(PreferencesUi ui) {
        ImageExportOptions exportOptions =
            SavePreferencesStorage.getDefaultExportOptions();
        PreferencesModel model = load(exportOptions, ui);
        ui.showDialog(model, exportOptions, new PreferencesController());
    }

    @Contract("_, _ -> new")
    private static @NotNull PreferencesModel load(
        ImageExportOptions exportOptions, PreferencesUi ui
    ) {
        String language = LocalePreferenceWorkflow.resolveStoredLanguage(
            LocaleModel.getStoredLanguage(), LocaleModel.getAvailableLanguages()
        );
        LocaleModel.applyResolvedLanguage(language);

        int minimumMemory = PreferenceSettingsStorage.getMinimumMemoryLimit();
        int maximumMemory = PreferenceSettingsStorage.getMaximumMemoryLimit();
        int memoryLimit = PreferenceSettingsStorage.getMemoryLimit();
        if (maximumMemory > minimumMemory) {
            memoryLimit = Math.clamp(memoryLimit, minimumMemory, maximumMemory);
        }

        int updatePreference =
            PreferenceSettingsStorage.getInteractiveUpdatePreferenceValue(
                InteractiveUpdateFrequency.NORMAL.preferenceValue
            );
        InteractiveUpdateFrequency updateFrequency =
            InteractiveUpdateFrequency.fromPreferenceValue(updatePreference);

        String scratchDirectory =
            ScratchDirectoryOperations.getScratchDirectory();
        if (scratchDirectory == null) {
            scratchDirectory = ScratchDirectoryOperations.getDefaultTempPath();
        }
        boolean scratchDirectoryEnabled = scratchDirectory != null;
        if (scratchDirectory == null) {
            scratchDirectory = "";
        }

        boolean displayProfileAvailable = Platform.isLinux();
        String displayProfile = displayProfileAvailable
            ? DisplayProfileOperations.getCurrentPath()
            : null;
        boolean displayProfileEnabled = displayProfile != null;
        if (displayProfile == null) {
            displayProfile = DefaultDisplayProfile;
        }

        var general = new PreferencesModel.GeneralSettings(
            minimumMemory,
            maximumMemory,
            memoryLimit,
            updateFrequency,
            scratchDirectory,
            scratchDirectoryEnabled,
            displayProfile,
            displayProfileAvailable,
            displayProfileEnabled,
            BrowserCacheOperations.isLocalScope(),
            PreferenceSettingsStorage.getCheckForUpdateEnabled(),
            PreferenceSettingsStorage.getDirectoryByOriginal("ExportByOriginal"),
            PreferenceSettingsStorage.getDirectoryByOriginal("SaveByOriginal"),
            PreferenceSettingsStorage.getAutoSaveEnabled(),
            PreferenceSettingsStorage.getOtherAppIntegrationEnabled(),
            ui.isShowToolTitles()
        );
        String defaultCopyright = nonEmptyOrNull(
            CopyrightPreferenceStorage.getDefaultCopyright()
        );
        String defaultCreator = nonEmptyOrNull(
            CopyrightPreferenceStorage.getDefaultCreator()
        );
        var copyright = new PreferencesModel.CopyrightSettings(
            defaultCopyright, defaultCreator, defaultCopyright, defaultCreator
        );
        return new PreferencesModel(
            language,
            LocaleModel.getAvailableLanguages(),
            general,
            formatFor(exportOptions),
            copyright
        );
    }

    @Override
    public void handle(
            @NotNull PreferencesEffect effect,
            ImageExportOptions selectedExportOptions,
            Consumer<PreferencesMessage> dispatch,
            PreferencesUi ui
    ) {
        switch (effect) {
            case PreferencesEffect.ApplyPreferences apply ->
                    persist(apply.settings(), selectedExportOptions, ui);
            case PreferencesEffect.ChooseScratchDirectory choose ->
                    chooseScratchDirectory(choose.currentPath(), ui, dispatch);
            case PreferencesEffect.ChooseDisplayProfile choose -> chooseDisplayProfile(
                    choose.currentPath(), choose.useHomeDirectory(), ui,
                    dispatch
            );
            case PreferencesEffect.ConfirmClearBrowserCache _ ->
                    confirmClearBrowserCache(ui, dispatch);
            case PreferencesEffect.ConfirmClearPreferences _ ->
                    confirmClearPreferences(ui, dispatch);
            case PreferencesEffect.ClearGlobalBrowserCache _ -> {
                try {
                    BrowserCacheOperations.clearGlobalCache();
                    dispatch.accept(new PreferencesMessage.BrowserCacheCleared());
                } catch (IOException e) {
                    logger.error("Error clearing the global browser cache", e);
                    ui.showBrowserCacheClearError(e);
                }
            }
            case PreferencesEffect.ClearAllPreferences _ -> {
                try {
                    ClearPreferencesOperations.clear();
                } catch (BackingStoreException e) {
                    logger.error(
                            "ClearPrefs failed to reset Preferences", e
                    );
                    ui.showClearPreferencesError(e);
                    return;
                }
                ui.showClearPreferencesSuccess();
                System.exit(0);
            }
            default -> throw new IllegalStateException(
                    "Unhandled preferences effect: " +
                            effect.getClass().getName()
            );
        }
    }

    private static void persist(
            @NotNull PreferencesModel model,
            ImageExportOptions exportOptions,
            PreferencesUi ui
    ) {
        var general = model.general();
        Optional<String> language = LocalePreferenceWorkflow
            .resolveSelectedLanguage(
                model.language(), model.availableLanguages()
            );
        if (language.isPresent()) {
            LocaleModel.setPreference(language.get());
        } else {
            LocaleModel.removePreference();
        }
        if (general.maximumMemoryLimit() > general.minimumMemoryLimit()) {
            PreferenceSettingsStorage.setMemoryLimit(general.memoryLimit());
        }
        PreferenceSettingsStorage.setInteractiveUpdatePreferenceValue(
            general.interactiveUpdateFrequency().preferenceValue
        );
        if (general.scratchDirectoryEnabled()) {
            ScratchDirectoryOperations.setScratchDirectory(
                general.scratchDirectory()
            );
        } else {
            ScratchDirectoryOperations.removeScratchDirectory();
        }
        if (Platform.isLinux()) {
            if (general.displayProfileEnabled()) {
                DisplayProfileOperations.setCurrentPath(
                    general.displayProfilePath()
                );
            } else {
                DisplayProfileOperations.removeCurrentPath();
            }
        }
        BrowserCacheOperations.setLocalScope(general.localBrowserCache());
        PreferenceSettingsStorage.setCheckForUpdateEnabled(
            general.checkForUpdate()
        );
        PreferenceSettingsStorage.setDirectoryByOriginal(
            "ExportByOriginal", general.exportByOriginal()
        );
        PreferenceSettingsStorage.setDirectoryByOriginal(
            "SaveByOriginal", general.saveByOriginal()
        );
        PreferenceSettingsStorage.setAutoSaveEnabled(general.autoSave());
        PreferenceSettingsStorage.setOtherAppIntegrationEnabled(
            general.integrationEnabled()
        );
        ui.setShowToolTitles(general.showToolTitles());
        SavePreferencesStorage.setDefaultExportOptions(exportOptions);

        var copyright = model.copyright();
        CopyrightPreferenceStorage.setDefaultCopyright(
            emptyToNull(copyright.copyrightText())
        );
        CopyrightPreferenceStorage.setDefaultCreator(
            emptyToNull(copyright.creatorText())
        );
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static String nonEmptyOrNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static void chooseScratchDirectory(
        String currentPath, PreferencesUi ui,
        Consumer<PreferencesMessage> dispatch
    ) {
        File chosen = ui.chooseScratchDirectory(currentPath);
        if (chosen != null) {
            if (!chosen.isDirectory()) {
                chosen = chosen.getParentFile();
            }
            if (chosen != null) {
                dispatch.accept(new PreferencesMessage.ScratchDirectoryChosen(
                    chosen.getAbsolutePath()
                ));
            }
        }
    }

    private static void chooseDisplayProfile(
        String currentPath, boolean useHomeDirectory,
        PreferencesUi ui,
        Consumer<PreferencesMessage> dispatch
    ) {
        if (!Platform.isLinux()) {
            throw new IllegalStateException(
                "Display profile selection is only available on Linux"
            );
        }
        File file = ui.chooseDisplayProfile(
            currentPath, useHomeDirectory
        );
        if (file == null) {
            return;
        }
        if (!file.isFile()) {
            ui.showDisplayProfileNotFile(file.getName());
            return;
        }
        try {
            DisplayProfileOperations.validate(file);
        } catch (IOException e) {
            ui.showDisplayProfileReadError(file.getName(), e);
            return;
        } catch (IllegalArgumentException e) {
            ui.showDisplayProfileInvalid(file.getName(), e);
            return;
        }
        dispatch.accept(new PreferencesMessage.DisplayProfileChosen(
            file.getAbsolutePath()
        ));
        ui.showDisplayProfileAccepted(file.getName());
    }

    private static void confirmClearBrowserCache(
            PreferencesUi ui, @NotNull Consumer<PreferencesMessage> dispatch
    ) {
        dispatch.accept(ui.confirmClearBrowserCache()
            ? new PreferencesMessage.BrowserCacheClearConfirmed()
            : new PreferencesMessage.BrowserCacheClearCancelled());
    }

    private static void confirmClearPreferences(
            PreferencesUi ui, @NotNull Consumer<PreferencesMessage> dispatch
    ) {
        dispatch.accept(ui.confirmClearPreferences()
            ? new PreferencesMessage.ClearPreferencesConfirmed()
            : new PreferencesMessage.ClearPreferencesCancelled());
    }

    private static SaveFormat formatFor(@NotNull ImageExportOptions options) {
        if (options.getImageType() == TIFFImageType.INSTANCE) {
            return SaveFormat.TIFF;
        }
        if (options.getImageType() == JPEGImageType.INSTANCE) {
            return SaveFormat.JPEG;
        }
        throw new IllegalArgumentException(
            "Unsupported save image type: " + options.getImageType().getName()
        );
    }

    static void main() throws Exception {
        PreferencesUi.setLookAndFeel();
        showDialog(new PreferencesUi(null));
        System.exit(0);
    }
}
