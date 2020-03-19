/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.Application;

import java.awt.event.ActionEvent;

final class CloseMenuItem extends UpdatableMenuItem {

    CloseMenuItem(ComboFrame frame) {
        super(frame, "Close");
    }

    public void actionPerformed(ActionEvent event) {
        performPreAction( event );
        final ComboFrame frame = getComboFrame();
        Application.close(frame);
    }
}
