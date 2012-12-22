/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.Application;

import java.awt.event.ActionEvent;

final class PrintMenuItem extends DocumentMenuItem {

    PrintMenuItem(ComboFrame frame) {
        super(frame, "Print");
    }

    void update() {
        super.update();
        // FIXME: allow printing based on browser selection
    }

    public void actionPerformed(ActionEvent event) {
        performPreAction( event );
        final ComboFrame frame = getComboFrame();
        Application.print(frame);
    }
}
