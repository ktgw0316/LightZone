/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app;

import com.lightcrafts.ui.editor.Document;

import javax.swing.*;

/**
 * A FrameButton that enables and disables according to whether the frame has
 * an open document.
 */
class EditorButton extends FrameButton {

    EditorButton(ComboFrame frame, String text) {
        super(frame, text);
    }

    EditorButton(ComboFrame frame, Icon icon) {
        super(frame, icon);
    }

    void updateButton() {
        final ComboFrame frame = getComboFrame();
        final Document doc = frame.getDocument();
        setEnabled(doc != null);
    }
}
