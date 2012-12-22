/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.Document;

import java.awt.event.ActionEvent;

final class ZoomFitMenuItem extends DocumentMenuItem {

    ZoomFitMenuItem(ComboFrame frame) {
        super(frame, "ZoomFit");
    }

    public void actionPerformed(ActionEvent event) {
        Document doc = getDocument();
        doc.zoomToFit();
    }
}
