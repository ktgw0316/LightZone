/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.ui.operation.OpStack;
import com.lightcrafts.ui.operation.SelectableControl;

import javax.swing.*;
import java.awt.*;

class OpsScroll extends JScrollPane {

    private OpStack stack;

    OpsScroll(OpStack stack) {
        init();
        this.stack = stack;
        getViewport().setView(stack);
        setBorder(null); // avoid funky borders
    }

    // Create an empty, disabled OpsScroll for the no-Document display mode:
    OpsScroll() {
        init();
    }

    public Dimension getPreferredSize() {
        if (stack != null) {
            return stack.getPreferredSize();
        }
        else {
            // Zero preferred height of a Scrollable on OSX makes for screwy
            // JScrollBar behavior:
            return new Dimension(OpStack.PreferredWidth, 1);
        }
    }

    void addControl(SelectableControl control) {
        stack.addControl(control);
    }

    void removeControl(SelectableControl control) {
        stack.removeControl(control);
    }

    private void init() {
        getViewport().setOpaque(true);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_AS_NEEDED);
        setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER);
    }
}
