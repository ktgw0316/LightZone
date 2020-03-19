/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.Document;

import javax.swing.*;

class RedoMenuItem extends ActionMenuItem {

    RedoMenuItem(ComboFrame frame) {
        super(frame, "Redo");
    }

    Action getDocumentAction() {
        Document doc = getDocument();
        if (doc != null) {
            return doc.getRedoAction();
        }
        else {
            return null;
        }
    }
}
