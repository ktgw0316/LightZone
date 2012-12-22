/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import static com.lightcrafts.prefs.Locale.LOCALE;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

class AppModeItem extends PreferencesItem {

    private final static String Package = "/com/lightcrafts/app";
    private final static String Key = "AppMode";
    private final static String FullValue = "Full";
    private final static String BasicValue = "Basic";

    private static Preferences Prefs = Preferences.userRoot().node(Package);

    private JCheckBox checkbox;

    AppModeItem(JTextArea help) {
        super(help);
        checkbox = new JCheckBox();
        checkbox.setFocusable(false);
        addHelpListeners();

        if (! ApplicationMode.canSetBasicMode()) {
            checkbox.setEnabled(false);
            checkbox.setSelected(true);
        }
    }

    public String getLabel() {
        return LOCALE.get("AppModeItemLabel");
    }

    public String getHelp(MouseEvent e) {
        return LOCALE.get("AppModeItemHelp");
    }

    public boolean requiresRestart() {
        return false;
    }

    public JComponent getComponent() {
        return checkbox;
    }

    public void commit() {
        if (ApplicationMode.canSetBasicMode()) {
            boolean isSelected = checkbox.isSelected();
            Prefs.put(Key, isSelected ? BasicValue : FullValue);
        }
    }

    public void restore() {
        if (ApplicationMode.canSetBasicMode()) {
            String mode = Prefs.get(Key, FullValue);
            checkbox.setSelected(mode.equals(BasicValue));
        }
    }
}
