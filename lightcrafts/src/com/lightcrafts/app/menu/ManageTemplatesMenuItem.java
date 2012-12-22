/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.templates.TemplateList;

import java.awt.event.ActionEvent;

class ManageTemplatesMenuItem extends UpdatableMenuItem {

    ManageTemplatesMenuItem(ComboFrame frame) {
        super(frame, "ManageTemplate");
    }

    public void actionPerformed(ActionEvent event) {
        ComboFrame frame = getComboFrame();
        TemplateList.showDialog(frame);
    }
}
