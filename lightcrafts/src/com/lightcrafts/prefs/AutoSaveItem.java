/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

import static com.lightcrafts.prefs.Locale.LOCALE;

class AutoSaveItem extends PreferencesItem {

    private final static String Package = "/com/lightcrafts/app";
    private final static String Key = "AutoSave";

    private static Preferences Prefs = Preferences.userRoot().node(Package);

    private JCheckBox checkbox;

    AutoSaveItem(JTextArea help) {
        super(help);
        checkbox = new JCheckBox();
        checkbox.setFocusable(false);
        addHelpListeners();
    }

    public String getLabel() {
        return LOCALE.get("AutoSaveItemLabel");
    }

    public String getHelp(MouseEvent e) {
        return LOCALE.get("AutoSaveItemHelp");
    }

    public boolean requiresRestart() {
        return false;
    }

    public JComponent getComponent() {
        return checkbox;
    }

    public void commit() {
        boolean selected = checkbox.isSelected();
        Prefs.putBoolean(Key, selected);
    }

    public void restore() {
        boolean selected = Prefs.getBoolean(Key, true);
        checkbox.setSelected(selected);
    }
}
