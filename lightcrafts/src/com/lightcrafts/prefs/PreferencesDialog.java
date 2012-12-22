/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import com.lightcrafts.platform.Platform;
import static com.lightcrafts.prefs.Locale.LOCALE;
import com.lightcrafts.utils.Version;

import javax.swing.*;
import java.awt.*;

public class PreferencesDialog extends JPanel {

    public static void showDialog(Component parent) {
        PreferencesPanel prefs = new PreferencesPanel();
        SavePrefsPanel save = new SavePrefsPanel();
        CopyrightPrefsPanel copy = new CopyrightPrefsPanel();

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab(LOCALE.get("GeneralPreferencesTab"), prefs);
        tabs.addTab(LOCALE.get("SavePreferencesTab"), save);
        tabs.addTab(LOCALE.get("CopyrightPreferencesTab"), copy);

        int result = JOptionPane.showOptionDialog(
            parent,
            tabs,
            Version.getApplicationName() + " " +
                LOCALE.get("PreferencesDialogTitle"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE,
            null,
            new Object[] {
                LOCALE.get("PreferencesDialogOkButton"),
                LOCALE.get("PreferencesDialogCancelButton")
            },
            LOCALE.get("PreferencesDialogOkButton")
        );
        if (result == 0) {
            prefs.commit();
            save.commit();
            copy.commit();
        }
    }

    public static void main(String[] args) throws Exception {
        UIManager.setLookAndFeel(Platform.getPlatform().getLookAndFeel());
        showDialog(null);
        System.exit(0);
    }
}
