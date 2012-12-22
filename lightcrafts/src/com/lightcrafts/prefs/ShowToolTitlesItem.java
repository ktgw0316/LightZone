/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.prefs;

import static com.lightcrafts.prefs.Locale.LOCALE;
import com.lightcrafts.ui.layout.ToggleTitleBorder;

import javax.swing.*;
import java.awt.event.MouseEvent;

class ShowToolTitlesItem extends PreferencesItem {

    private JCheckBox checkbox;

    ShowToolTitlesItem(JTextArea help) {
        super(help);
        checkbox = new JCheckBox();
        checkbox.setFocusable(false);
        addHelpListeners();
    }

    public String getLabel() {
        return LOCALE.get("ShowToolTitlesItemLabel");
    }

    public String getHelp(MouseEvent e) {
        return LOCALE.get("ShowToolTitlesItemHelp");
    }

    public boolean requiresRestart() {
        return false;
    }

    public JComponent getComponent() {
        return checkbox;
    }

    public void commit() {
        boolean selected = checkbox.isSelected();
        ToggleTitleBorder.setShowBorders(selected);
    }

    public void restore() {
        boolean selected = ToggleTitleBorder.isShowBorders();
        checkbox.setSelected(selected);
    }
}
