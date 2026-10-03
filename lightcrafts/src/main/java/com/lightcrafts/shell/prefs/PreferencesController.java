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
import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.layout.ToggleTitleBorder;
import com.lightcrafts.ui.prefs.PreferencesDialog;
import com.lightcrafts.ui.prefs.PreferencesEffectHandler;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Component;
import java.awt.Frame;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;

import static com.lightcrafts.ui.prefs.Locale.LOCALE;

public final class PreferencesController implements PreferencesEffectHandler {
    private static final String DefaultDisplayProfile = "(sRGB default)";
    private static final Logger logger =
        LoggerFactory.getLogger(PreferencesController.class);

    private PreferencesController() {
    }

    public static void showDialog(Component parent) {
        ImageExportOptions exportOptions =
            SavePreferencesStorage.getDefaultExportOptions();
        PreferencesModel model = load(exportOptions);
        PreferencesDialog.showDialog(
            parent, model, exportOptions, new PreferencesController()
        );
    }

    @Contract("_ -> new")
    private static @NotNull PreferencesModel load(
        ImageExportOptions exportOptions
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
            ToggleTitleBorder.isShowBorders()
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
            Component parent,
            ImageExportOptions selectedExportOptions,
            Consumer<PreferencesMessage> dispatch
    ) {
        switch (effect) {
            case PreferencesEffect.ApplyPreferences apply ->
                    persist(apply.settings(), selectedExportOptions);
            case PreferencesEffect.ChooseScratchDirectory choose ->
                    chooseScratchDirectory(choose.currentPath(), parent, dispatch);
            case PreferencesEffect.ChooseDisplayProfile choose -> chooseDisplayProfile(
                    choose.currentPath(), choose.useHomeDirectory(), parent,
                    dispatch
            );
            case PreferencesEffect.ConfirmClearBrowserCache _ ->
                    confirmClearBrowserCache(parent, dispatch);
            case PreferencesEffect.ConfirmClearPreferences _ ->
                    confirmClearPreferences(parent, dispatch);
            case PreferencesEffect.ClearGlobalBrowserCache _ -> {
                try {
                    BrowserCacheOperations.clearGlobalCache();
                    dispatch.accept(new PreferencesMessage.BrowserCacheCleared());
                } catch (IOException e) {
                    logger.error("Error clearing the global browser cache", e);
                    Platform.getPlatform().getAlertDialog().showAlert(
                            frameFor(parent),
                            LOCALE.get("BrowserCacheItemError"),
                            e.getMessage(),
                            AlertDialog.ERROR_ALERT,
                            LOCALE.get("BrowserCacheItemErrorButton"));
                }
            }
            case PreferencesEffect.ClearAllPreferences _ -> {
                try {
                    ClearPreferencesOperations.clear();
                } catch (BackingStoreException e) {
                    logger.error(
                            "ClearPrefs failed to reset Preferences", e
                    );
                    JOptionPane.showMessageDialog(
                            parent,
                            LOCALE.get("ClearPrefsErrorMessage") + ": " +
                                    e.getClass().getName() + " " + e.getMessage(),
                            LOCALE.get("ClearPrefsErrorTitle"),
                            JOptionPane.ERROR_MESSAGE
                    );
                    return;
                }
                JOptionPane.showOptionDialog(
                        parent,
                        LOCALE.get("ClearPrefsSuccessMessage"),
                        LOCALE.get("ClearPrefsSuccessTitle"),
                        JOptionPane.OK_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        new Object[]{LOCALE.get("ClearPrefsSuccessButton")},
                        LOCALE.get("ClearPrefsSuccessButton")
                );
                System.exit(0);
            }
            default -> throw new IllegalStateException(
                    "Unhandled preferences effect: " +
                            effect.getClass().getName()
            );
        }
    }

    private static Frame frameFor(Component parent) {
        return parent instanceof Frame
            ? (Frame) parent
            : parent == null
                ? null
                : (Frame) SwingUtilities.getAncestorOfClass(
                    Frame.class, parent
                );
    }

    private static void persist(
            @NotNull PreferencesModel model, ImageExportOptions exportOptions
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
        ToggleTitleBorder.setShowBorders(general.showToolTitles());
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
        String currentPath, Component parent,
        Consumer<PreferencesMessage> dispatch
    ) {
        FileChooser chooser = Platform.getPlatform().getFileChooser();
        Window window = parent == null
            ? null
            : SwingUtilities.getWindowAncestor(parent);
        File directory = new File(currentPath);
        File chosen = chooser.chooseDirectory(
            LOCALE.get("ScratchFileDialogTitle"),
            directory,
            window,
            true
        );
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
        Component parent,
        Consumer<PreferencesMessage> dispatch
    ) {
        FileChooser chooser = Platform.getPlatform().getFileChooser();
        AlertDialog alert = Platform.getPlatform().getAlertDialog();
        Frame frame = parent instanceof Frame
            ? (Frame) parent
            : parent == null
                ? null
                : (Frame) SwingUtilities.getAncestorOfClass(
                    Frame.class, parent
                );
        File current = new File(currentPath);
        if (!Platform.isLinux()) {
            throw new IllegalStateException(
                "Display profile selection is only available on Linux"
            );
        }
        if (useHomeDirectory) {
            current = new File(System.getProperty("user.home"));
        }
        File file = chooser.openFile(
            LOCALE.get("DisplayProfileDialogTitle"), current, null, null
        );
        if (file == null) {
            return;
        }
        if (!file.isFile()) {
            alert.showAlert(
                frame,
                '"' + file.getName() + "\" is not a file.",
                "A color profile is a file, usually ending with \".icc\" or \".icm\".",
                AlertDialog.WARNING_ALERT,
                "OK"
            );
            return;
        }
        try {
            DisplayProfileOperations.validate(file);
        } catch (IOException e) {
            alert.showAlert(
                frame,
                '"' + file.getName() + "\" could not be read.",
                e.getClass().getName() + ": " + e.getMessage(),
                AlertDialog.WARNING_ALERT,
                "OK"
            );
            return;
        } catch (IllegalArgumentException e) {
            alert.showAlert(
                frame,
                '"' + file.getName() + "\" is not a valid color profile.",
                e.getClass().getName() + ": " + e.getMessage(),
                AlertDialog.WARNING_ALERT,
                "OK"
            );
            return;
        }
        dispatch.accept(new PreferencesMessage.DisplayProfileChosen(
            file.getAbsolutePath()
        ));
        alert.showAlert(
            frame,
            "Display color profile \"" + file.getName() + "\" accepted.",
            "You must now restart LightZone for your new display " +
                "profile to take effect.",
            AlertDialog.WARNING_ALERT,
            "OK"
        );
    }

    private static void confirmClearBrowserCache(
            Component parent, @NotNull Consumer<PreferencesMessage> dispatch
    ) {
        Frame frame = parent instanceof Frame
            ? (Frame) parent
            : parent == null
                ? null
                : (Frame) SwingUtilities.getAncestorOfClass(
                    Frame.class, parent
                );
        var alert = Platform.getPlatform().getAlertDialog();
        int option = alert.showAlert(
            frame,
            LOCALE.get("BrowserCacheItemClearWarningMajor"),
            LOCALE.get("BrowserCacheItemClearWarningMinor"),
            AlertDialog.WARNING_ALERT,
            LOCALE.get("BrowserCacheItemClearOption"),
            LOCALE.get("BrowserCacheItemCancelOption")
        );
        dispatch.accept(option == 0
            ? new PreferencesMessage.BrowserCacheClearConfirmed()
            : new PreferencesMessage.BrowserCacheClearCancelled());
    }

    private static void confirmClearPreferences(
            Component parent, @NotNull Consumer<PreferencesMessage> dispatch
    ) {
        JTextArea text = createText(
            LOCALE.get("ClearPrefsItemWarning1") + "\n\n" +
            LOCALE.get("ClearPrefsItemWarning2") + "\n\n" +
            LOCALE.get("ClearPrefsItemWarning3")
        );
        JButton helpButton = new JButton(
            LOCALE.get("ClearPrefsItemHelpButton")
        );
        helpButton.addActionListener(_ -> showClearPreferencesHelp(parent));
        int option = JOptionPane.showOptionDialog(
            parent,
            text,
            LOCALE.get("ClearPrefsDialogTitle"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            new Object[] {
                helpButton,
                LOCALE.get("ClearPrefsResetOption"),
                LOCALE.get("ClearPrefsDontResetOption")
            },
            LOCALE.get("ClearPrefsDontResetOption")
        );
        dispatch.accept(option == 1
            ? new PreferencesMessage.ClearPreferencesConfirmed()
            : new PreferencesMessage.ClearPreferencesCancelled());
    }

    private static void showClearPreferencesHelp(Component parent) {
        JTextArea text = createText(
            LOCALE.get("ClearPrefsBigHelpProlog") + "\n\n" +
            "    " + LOCALE.get("ClearPrefsBigHelp1") + "\n" +
            "    " + LOCALE.get("ClearPrefsBigHelp2") + "\n" +
            "    " + LOCALE.get("ClearPrefsBigHelp3") + "\n" +
            "    " + LOCALE.get("ClearPrefsBigHelp4") + "\n" +
            "    " + LOCALE.get("ClearPrefsBigHelp5") + "\n" +
            "    " + LOCALE.get("ClearPrefsBigHelp6") + "\n\n" +
            LOCALE.get("ClearPrefsBigHelpEpilog")
        );
        JOptionPane.showMessageDialog(
            parent, text, LOCALE.get("ClearPrefsBigHelpTitle"),
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static @NotNull JTextArea createText(String message) {
        JTextArea text = com.lightcrafts.ui.toolkit.TextAreaFactory
            .createTextArea(message, 40);
        text.setBackground(new JPanel().getBackground());
        return text;
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
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());
        showDialog(null);
        System.exit(0);
    }
}
