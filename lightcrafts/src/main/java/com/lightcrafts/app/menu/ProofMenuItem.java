/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.Document;

import javax.swing.*;

class ProofMenuItem extends ActionMenuItem {

    ProofMenuItem(ComboFrame frame) {
        super(frame, "Proof");
    }

    Action getDocumentAction() {
        Document doc = getDocument();
        if (doc != null) {
            return doc.getProofAction();
        }
        else {
            return null;
        }
    }
}
