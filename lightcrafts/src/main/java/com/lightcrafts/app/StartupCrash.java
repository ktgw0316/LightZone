/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.splash.SplashWindow;
import com.lightcrafts.ui.toolkit.TextAreaFactory;
import com.lightcrafts.utils.filecache.FileCacheFactory;
import com.lightcrafts.utils.filecache.FileCache;

import static com.lightcrafts.app.Locale.LOCALE;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.io.IOException;

/**
 * A global utility used at startup and shutdown to detect initialization
 * errors that prevent LightZone from starting.
 * <p>
 * When an unrecoverable initialization error is detected, this class offers
 * to clear out Preferences in an attempt to allow LightZone to start.
 */
class StartupCrash {

    private final static String StartupKey = "StartupSuccessful";

    static void startupStarted() {
        Preferences prefs = Preferences.userRoot().node("/com/lightcrafts/app");
        prefs.putBoolean(StartupKey, false);
        try {
            prefs.sync();
        }
        catch (BackingStoreException e) {
            System.err.println("Couldn't access Preferences in StartupCrash");
            e.printStackTrace();
        }
        // Just let startup continue; maybe we'll get lucky.
    }

    static void startupEnded() {
        Preferences prefs = Preferences.userRoot().node("/com/lightcrafts/app");
        prefs.putBoolean(StartupKey, true);
    }

    static void checkLastStartupSuccessful() {
        Preferences prefs = Preferences.userRoot().node("/com/lightcrafts/app");
        boolean wasSuccessful = prefs.getBoolean(StartupKey, true);
        if (! wasSuccessful) {
            SplashWindow.disposeSplash(); // because the splash can conceal other dialogs
            showResetDialog(true);
        }
    }

    private static void showResetDialog(boolean timeout) {
        final String helpButtonText = LOCALE.get("StartupErrorHelpOption");
        final String resetButtonText = LOCALE.get("StartupErrorResetOption");
        final String dontResetButtonText = LOCALE.get("StartupErrorDontResetOption");

        final JOptionPane pane = new JOptionPane(
                createText(LOCALE.get("StartupErrorMessage")),
                JOptionPane.WARNING_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION,
                null,
                new Object[] {
                        helpButtonText,
                        resetButtonText,
                        dontResetButtonText
                },
                dontResetButtonText
        );
        final JDialog dialog = pane.createDialog(
                LOCALE.get("StartupErrorHelpDialogTitle"));
        if (timeout) {
            final Timer timer = new Timer(5000, new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    dialog.setVisible(false);
                    dialog.dispose();
                }
            });
            timer.setRepeats(false);
            timer.start();
        }
        dialog.setVisible(true);
        final Object selectedValue = pane.getValue();

        if (helpButtonText.equals(selectedValue)) {
            showHelpDialog();
            dialog.dispose();
            showResetDialog(false);
        }
        else if (resetButtonText.equals(selectedValue)) {
            JButton help = new JButton(helpButtonText);
            help.addActionListener(
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent event) {
                            showHelpDialog();
                        }
                    }
            );
            final int option = JOptionPane.showOptionDialog(
                    null,
                    createText(LOCALE.get("ResetWarningMajor")),
                    LOCALE.get("ResetDialogTitle"),
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    new Object[] {
                            help,
                            resetButtonText,
                            dontResetButtonText
                    },
                    dontResetButtonText
            );
            if (option != 1) {
                return;
            }
            boolean success = true;
            try {
                Preferences root = Preferences.userRoot();
                Preferences node = root.node("/com/lightcrafts");
                node.removeNode();
            }
            catch (BackingStoreException e) {
                System.err.println(
                        "StartupCrash failed to reset Preferences"
                );
                showErrorDialog(e);
                success = false;
            }
            try {
                FileCache cache = FileCacheFactory.getGlobalCache();
                if (cache != null) {
                    cache.clear();
                }
            }
            catch (IOException e) {
                System.err.println(
                        "StartupCrach failed to clear FileCache"
                );
                showErrorDialog(e);
                success = false;
            }
            if (success) {
                showSuccessDialog();
            }
        }
    }

    private static void showHelpDialog() {
        JTextArea text = createText(
            LOCALE.get("StartupErrorHelpText1") + '\n' +
            '\n' +
            LOCALE.get("StartupErrorHelpText2") + '\n' +
            LOCALE.get("StartupErrorHelpText3") + '\n' +
            LOCALE.get("StartupErrorHelpText4") + '\n' +
            LOCALE.get("StartupErrorHelpText5") + '\n' +
            LOCALE.get("StartupErrorHelpText6") + '\n' +
            LOCALE.get("StartupErrorHelpText7") + '\n' +
            '\n' +
            LOCALE.get("StartupErrorHelpText8")
        );
        JOptionPane.showMessageDialog(
            null,
            text,
            LOCALE.get("StartupErrorHelpDialogTitle"),
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static void showSuccessDialog() {
        JOptionPane.showMessageDialog(
            null,
            LOCALE.get("ResetSuccessMessage"),
            LOCALE.get("ResetDialogTitle"),
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static void showErrorDialog(Throwable t) {
        JOptionPane.showMessageDialog(
            null,
            LOCALE.get("ResetErrorMessage") + ": " +
            t.getClass().getName() + " " + t.getMessage(),
            LOCALE.get("ResetErrorDialogTitle"),
            JOptionPane.ERROR_MESSAGE
        );
    }

    private static JTextArea createText(String message) {
        JTextArea text = TextAreaFactory.createTextArea(message, 40);
        text.setBackground(new JPanel().getBackground());
        return text;
    }

    public static void main(String[] args) {
        boolean shouldCrash = false;
        checkLastStartupSuccessful();
        startupStarted();
        if (shouldCrash) {
            System.exit(0);
        }
        startupEnded();
    }
}
