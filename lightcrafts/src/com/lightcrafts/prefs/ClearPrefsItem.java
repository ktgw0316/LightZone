/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import static com.lightcrafts.prefs.Locale.LOCALE;
import com.lightcrafts.ui.toolkit.TextAreaFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

class ClearPrefsItem extends PreferencesItem {

    private JButton clearButton;

    ClearPrefsItem(JTextArea help) {
        super(help);
        clearButton = new JButton(LOCALE.get("ClearPrefsItemButton"));
        clearButton.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    clearPrefs();
                }
            }
        );
        addHelpListeners();
    }

    public String getLabel() {
        return LOCALE.get("ClearPrefsItemLabel");
    }

    public String getHelp(MouseEvent e) {
        return LOCALE.get("ClearPrefsItemHelp");
    }

    public boolean requiresRestart() {
        return false;
    }

    public JComponent getComponent() {
        Box box = Box.createHorizontalBox();
        box.add(clearButton);
        box.add(Box.createHorizontalGlue());
        return box;
    }

    public void commit() {
    }

    public void restore() {
    }

    private void clearPrefs() {
        JTextArea text = createText(
            LOCALE.get("ClearPrefsItemWarning1") +
            "\n\n" +
            LOCALE.get("ClearPrefsItemWarning2") +
            "\n\n" +
            LOCALE.get("ClearPrefsItemWarning3")
        );
        text.setBackground(new JPanel().getBackground());

        JButton help = new JButton(LOCALE.get("ClearPrefsItemHelpButton"));
        help.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    showHelpDialog();
                }
            }
        );
        int option = JOptionPane.showOptionDialog(
            null,
            text,
            LOCALE.get("ClearPrefsDialogTitle"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE,
            null,
            new Object[] {
                help,
                LOCALE.get("ClearPrefsResetOption"),
                LOCALE.get("ClearPrefsDontResetOption")
            },
            LOCALE.get("ClearPrefsDontResetOption")
        );
        if (option == 1) {
            boolean success = true;
            try {
                // On Linux, the license state resides in preferences.
                Preferences root = Preferences.userRoot();
                Preferences node = root.node("/com/lightcrafts");
                node.removeNode();
                node.flush();
            }
            catch (BackingStoreException e) {
                System.err.println(
                    "ClearPrefs failed to reset Preferences"
                );
                showErrorDialog(e);
                success = false;
            }
            if (success) {
                showSuccessDialog();

                // This is painful, but it's the only way.
                //
                // LightZone is riddled with static references to preference
                // nodes.  Application.quit() would trigger an avalanche of
                // access to removed nodes, and maybe even allow the user to
                // cancel the shutdown.
                System.exit(0);
            }
        }
    }

    private static void showHelpDialog() {
        JTextArea text = createText(
            LOCALE.get("ClearPrefsBigHelpProlog") + "\n" +
            "\n" +
            "    " + LOCALE.get("ClearPrefsBigHelp1") + "\n" +
            "    " + LOCALE.get("ClearPrefsBigHelp2") + "\n" +
            "    " + LOCALE.get("ClearPrefsBigHelp3") + "\n" +
            "    " + LOCALE.get("ClearPrefsBigHelp4") + "\n" +
            "    " + LOCALE.get("ClearPrefsBigHelp5") + "\n" +
            "    " + LOCALE.get("ClearPrefsBigHelp6") + "\n" +
            "\n" +
            LOCALE.get("ClearPrefsBigHelpEpilog")
        );
        JOptionPane.showMessageDialog(
            null,
            text,
            LOCALE.get("ClearPrefsBigHelpTitle"),
            JOptionPane.INFORMATION_MESSAGE
        );
    }

    private static void showSuccessDialog() {
        JOptionPane.showOptionDialog(
            null,
            LOCALE.get("ClearPrefsSuccessMessage"),
            LOCALE.get("ClearPrefsSuccessTitle"),
            JOptionPane.OK_OPTION,
            JOptionPane.INFORMATION_MESSAGE,
            null,
            new Object[] { LOCALE.get("ClearPrefsSuccessButton") },
            LOCALE.get("ClearPrefsSuccessButton")
        );
    }

    private static void showErrorDialog(Throwable t) {
        JOptionPane.showMessageDialog(
            null,
            LOCALE.get("ClearPrefsErrorMessage") + ": " +
            t.getClass().getName() + " " + t.getMessage(),
            LOCALE.get("ClearPrefsErrorTitle"),
            JOptionPane.ERROR_MESSAGE
        );
    }

    private static JTextArea createText(String message) {
        JTextArea text = TextAreaFactory.createTextArea(message, 40);
        text.setBackground(new JPanel().getBackground());
        return text;
    }
}
