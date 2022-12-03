/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import static com.lightcrafts.prefs.Locale.LOCALE;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

class OtherAppIntegrationItem extends PreferencesItem {

    private final static String Package = "/com/lightcrafts/app/other";
    private final static String Key = "IntegrationEnabled";

    private static Preferences Prefs = Preferences.userRoot().node(Package);

    private JCheckBox checkbox;

    OtherAppIntegrationItem(JTextArea help) {
        super(help);
        checkbox = new JCheckBox();
        checkbox.setFocusable(false);
        addHelpListeners();
    }

    public String getLabel() {
        return LOCALE.get("IntegrationLabel");
    }

    public String getHelp(MouseEvent e) {
        return LOCALE.get("IntegrationHelp");
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
