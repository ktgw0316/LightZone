/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.Document;

import javax.swing.*;

class RotateRightMenuItem extends ActionMenuItem {

    RotateRightMenuItem(ComboFrame frame) {
        super(frame, "Right");
    }

    Action getDocumentAction() {
        Document doc = getDocument();
        if (doc != null) {
            return doc.getRotateRightAction();
        }
        else {
            return null;
        }
    }
}
