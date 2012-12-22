/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.Document;

import javax.swing.*;

class UndoMenuItem extends ActionMenuItem {
    
    UndoMenuItem(ComboFrame frame) {
        super(frame, "Undo");
    }

    Action getDocumentAction() {
        Document doc = getDocument();
        if (doc != null) {
            return doc.getUndoAction();
        }
        else {
            return null;
        }
    }
}
