/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.Application;

import java.awt.event.ActionEvent;

final class OpenMenuItem extends UpdatableMenuItem {

    OpenMenuItem(ComboFrame frame) {
        super(frame, "Open");
        setEnabled(frame != null);
    }

    public void actionPerformed(ActionEvent event) {
        performPreAction( event );
        final ComboFrame frame = getComboFrame();
        Application.open(frame);
    }
}
