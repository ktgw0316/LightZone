/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;

import java.awt.event.ActionEvent;

class ComboLayoutMenuItem extends UpdatableMenuItem {

    ComboLayoutMenuItem(ComboFrame frame) {
        super(frame, "ComboLayout");
    }

    void update() {
        ComboFrame frame = getComboFrame();
        setEnabled(frame != null);
    }

    public void actionPerformed(ActionEvent event) {
        ComboFrame frame = getComboFrame();
        frame.showComboPerspective();
    }
}
