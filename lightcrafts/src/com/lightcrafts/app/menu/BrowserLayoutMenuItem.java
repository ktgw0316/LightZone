/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;

import java.awt.event.ActionEvent;

final class BrowserLayoutMenuItem extends UpdatableMenuItem {

    BrowserLayoutMenuItem(ComboFrame frame) {
        super(frame, "BrowserLayout");
    }

    void update() {
        final ComboFrame frame = getComboFrame();
        setEnabled(frame != null);
    }

    public void actionPerformed(ActionEvent event) {
        performPreAction( event );
        final ComboFrame frame = getComboFrame();
        frame.showBrowserPerspective();
    }
}
