/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;

import java.awt.event.ActionEvent;

class RescanMenuItem extends UpdatableMenuItem {

    RescanMenuItem(ComboFrame frame) {
        super(frame, "Rescan");
    }

    public void actionPerformed(ActionEvent event) {
        ComboFrame frame = getComboFrame();
        frame.refresh();
    }
}
