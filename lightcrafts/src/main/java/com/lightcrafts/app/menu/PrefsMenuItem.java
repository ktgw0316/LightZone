/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.Application;

import java.awt.event.ActionEvent;

class PrefsMenuItem extends UpdatableMenuItem {

    PrefsMenuItem(ComboFrame frame) {
        super(frame, "Preferences");
    }

    public void actionPerformed(ActionEvent event) {
        Application.showPreferences();
    }
}
