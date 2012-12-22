/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.Document;

import javax.swing.*;

class RotateLeftMenuItem extends ActionMenuItem {

    RotateLeftMenuItem(ComboFrame frame) {
        super(frame, "Left");
    }

    Action getDocumentAction() {
        final Document doc = getDocument();
        if (doc != null) {
            return doc.getRotateLeftAction();
        }
        else {
            return null;
        }
    }
}
