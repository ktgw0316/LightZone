/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.Application;

import java.awt.event.ActionEvent;

class NewMenuItem extends UpdatableMenuItem {

    NewMenuItem(ComboFrame frame) {
        super(frame, "NewWindow");
    }

    public void actionPerformed(ActionEvent event) {
        Application.openEmpty();
    }
}
