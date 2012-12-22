/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.Document;

/**
 * An UpdatableMenuItem which simply enables and disables depending on
 * whether there is a current Document.
 */
abstract class DocumentMenuItem extends UpdatableMenuItem {

    protected DocumentMenuItem(ComboFrame frame, String key) {
        super(frame, key);
    }

    Document getDocument() {
        final ComboFrame frame = getComboFrame();
        return (frame != null) ? frame.getDocument() : null;
    }

    void update() {
        setEnabled(getDocument() != null);
    }
}
