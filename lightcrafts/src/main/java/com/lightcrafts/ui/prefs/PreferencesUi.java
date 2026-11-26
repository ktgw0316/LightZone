package com.lightcrafts.ui.prefs;

import com.lightcrafts.application.prefs.PreferencesModel;
import com.lightcrafts.image.export.ImageExportOptions;
import com.lightcrafts.platform.AlertDialog;
import com.lightcrafts.platform.FileChooser;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.layout.ToggleTitleBorder;

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
import java.util.prefs.BackingStoreException;

import static com.lightcrafts.ui.prefs.Locale.LOCALE;

public final class PreferencesUi {
    private final Component parent;

    public PreferencesUi(Component parent) {
        this.parent = parent;
    }

    public void showDialog(
        PreferencesModel model,
        ImageExportOptions defaultExportOptions,
        PreferencesEffectHandler effectHandler
    ) {
        PreferencesDialog.showDialog(
            parent, model, defaultExportOptions, effectHandler, this
        );
    }

    public boolean confirmClearBrowserCache() {
        int option = Platform.getPlatform().getAlertDialog().showAlert(
            frameForParent(),
            LOCALE.get("BrowserCacheItemClearWarningMajor"),
            LOCALE.get("BrowserCacheItemClearWarningMinor"),
            AlertDialog.WARNING_ALERT,
            LOCALE.get("BrowserCacheItemClearOption"),
            LOCALE.get("BrowserCacheItemCancelOption")
        );
        return option == 0;
    }

    public boolean confirmClearPreferences() {
        JTextArea text = createText(
            LOCALE.get("ClearPrefsItemWarning1") + "\n\n" +
            LOCALE.get("ClearPrefsItemWarning2") + "\n\n" +
            LOCALE.get("ClearPrefsItemWarning3")
        );
        JButton helpButton = new JButton(
            LOCALE.get("ClearPrefsItemHelpButton")
        );
        helpButton.addActionListener(_ -> showClearPreferencesHelp());
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
        return option == 1;
    }

    public void showBrowserCacheClearError(IOException error) {
        Platform.getPlatform().getAlertDialog().showAlert(
            frameForParent(),
            LOCALE.get("BrowserCacheItemError"),
            error.getMessage(),
            AlertDialog.ERROR_ALERT,
            LOCALE.get("BrowserCacheItemErrorButton")
        );
    }

    public void showClearPreferencesError(BackingStoreException error) {
        JOptionPane.showMessageDialog(
            parent,
            LOCALE.get("ClearPrefsErrorMessage") + ": " +
                error.getClass().getName() + " " + error.getMessage(),
            LOCALE.get("ClearPrefsErrorTitle"),
            JOptionPane.ERROR_MESSAGE
        );
    }

    public void showClearPreferencesSuccess() {
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
    }

    public void showDisplayProfileNotFile(String name) {
        Platform.getPlatform().getAlertDialog().showAlert(
            frameForParent(),
            '"' + name + "\" is not a file.",
            "A color profile is a file, usually ending with \".icc\" or \".icm\".",
            AlertDialog.WARNING_ALERT,
            "OK"
        );
    }

    public void showDisplayProfileReadError(String name, IOException error) {
        Platform.getPlatform().getAlertDialog().showAlert(
            frameForParent(),
            '"' + name + "\" could not be read.",
            error.getClass().getName() + ": " + error.getMessage(),
            AlertDialog.WARNING_ALERT,
            "OK"
        );
    }

    public void showDisplayProfileInvalid(
        String name, IllegalArgumentException error
    ) {
        Platform.getPlatform().getAlertDialog().showAlert(
            frameForParent(),
            '"' + name + "\" is not a valid color profile.",
            error.getClass().getName() + ": " + error.getMessage(),
            AlertDialog.WARNING_ALERT,
            "OK"
        );
    }

    public void showDisplayProfileAccepted(String name) {
        Platform.getPlatform().getAlertDialog().showAlert(
            frameForParent(),
            "Display color profile \"" + name + "\" accepted.",
            "You must now restart LightZone for your new display " +
                "profile to take effect.",
            AlertDialog.WARNING_ALERT,
            "OK"
        );
    }

    private Frame frameForParent() {
        return parent instanceof Frame
            ? (Frame) parent
            : parent == null
                ? null
                : (Frame) SwingUtilities.getAncestorOfClass(
                    Frame.class, this.parent
                );
    }

    private Window windowForParent() {
        return parent == null ? null : SwingUtilities.getWindowAncestor(parent);
    }

    public File chooseScratchDirectory(String currentPath) {
        FileChooser chooser = Platform.getPlatform().getFileChooser();
        return chooser.chooseDirectory(
            scratchDirectoryDialogTitle(),
            new File(currentPath),
            windowForParent(),
            true
        );
    }

    public File chooseDisplayProfile(
        String currentPath, boolean useHomeDirectory
    ) {
        FileChooser chooser = Platform.getPlatform().getFileChooser();
        File current = useHomeDirectory
            ? new File(System.getProperty("user.home"))
            : new File(currentPath);
        return chooser.openFile(
            displayProfileDialogTitle(), current, null, null
        );
    }

    public boolean isShowToolTitles() {
        return ToggleTitleBorder.isShowBorders();
    }

    public void setShowToolTitles(boolean show) {
        ToggleTitleBorder.setShowBorders(show);
    }

    private static String scratchDirectoryDialogTitle() {
        return LOCALE.get("ScratchFileDialogTitle");
    }

    private static String displayProfileDialogTitle() {
        return LOCALE.get("DisplayProfileDialogTitle");
    }

    public static void setLookAndFeel() throws Exception {
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());
    }

    private void showClearPreferencesHelp() {
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

    private static JTextArea createText(String message) {
        JTextArea text = com.lightcrafts.ui.toolkit.TextAreaFactory
            .createTextArea(message, 40);
        text.setBackground(new JPanel().getBackground());
        return text;
    }
}
