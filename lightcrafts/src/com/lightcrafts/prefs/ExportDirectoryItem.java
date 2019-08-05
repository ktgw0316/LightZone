/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.prefs.Preferences;

import static com.lightcrafts.prefs.Locale.LOCALE;

class ExportDirectoryItem extends PreferencesItem {

    private final static String Package = "/com/lightcrafts/app";
    private final static String Key = "ExportByOriginal";

    private static Preferences Prefs = Preferences.userRoot().node(Package);

    private JRadioButton originalButton;
    private JRadioButton recentButton;
    private ButtonGroup group;

    ExportDirectoryItem(JTextArea help) {
        super(help);
        originalButton = new JRadioButton(
            LOCALE.get("ExportDirectoryOriginalButton")
        );
        recentButton = new JRadioButton(
            LOCALE.get("ExportDirectoryRecentButton")
        );
        originalButton.setFocusable(false);
        recentButton.setFocusable(false);
        group = new ButtonGroup();
        group.add(originalButton);
        group.add(recentButton);
        addHelpListeners();
    }

    public String getLabel() {
        return LOCALE.get("ExportDirectoryItemLabel");
    }

    public String getHelp(MouseEvent event) {
        return LOCALE.get("ExportDirectoryItemHelp");
    }

    public boolean requiresRestart() {
        return false;
    }

    public JComponent getComponent() {
        Box box = Box.createHorizontalBox();
        box.add(originalButton);
        box.add(recentButton);
        return box;
    }

    public void commit() {
        if (originalButton.isSelected()) {
            Prefs.putBoolean(Key, true);
        }
        else {
            Prefs.putBoolean(Key, false);
        }
    }

    public void restore() {
        boolean byOriginal = Prefs.getBoolean(Key, true);
        if (byOriginal) {
            originalButton.setSelected(true);
        }
        else {
            recentButton.setSelected(true);
        }
    }
}
