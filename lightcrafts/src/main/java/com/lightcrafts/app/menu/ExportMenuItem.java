/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.Application;

import java.awt.event.ActionEvent;

class ExportMenuItem extends DocumentMenuItem {

    ExportMenuItem(ComboFrame frame) {
        super(frame, "Export");
    }

    void update() {
        super.update();
        // FIXME: allow export based on browser selection
    }

    public void actionPerformed(ActionEvent event) {
        performPreAction( event );
        ComboFrame frame = getComboFrame();
        Application.export(frame);
    }
}
