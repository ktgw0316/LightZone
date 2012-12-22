/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;

import java.awt.event.ActionEvent;

final class SaveTemplateMenuItem extends DocumentMenuItem {

    SaveTemplateMenuItem(ComboFrame frame) {
        super(frame, "SaveTemplate");
    }

    public void actionPerformed(ActionEvent event) {
        performPreAction( event );
        final ComboFrame frame = getComboFrame();
        frame.addTemplate();
    }
}
