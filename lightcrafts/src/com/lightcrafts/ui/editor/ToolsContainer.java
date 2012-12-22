/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.ui.operation.OpStack;

import javax.swing.*;
import java.awt.*;

/**
 * The top-level layout container for the Editor tools area.
 */
class ToolsContainer extends JPanel {

    ToolsContainer(OpStack stack) {
        super(new BorderLayout());
        OpsToolbar toolbar = new OpsToolbar(stack);
        add(toolbar, BorderLayout.NORTH);
        add(new OpsScroll(stack));
    }

    ToolsContainer() {
        super(new BorderLayout());
        OpsToolbar toolbar = new OpsToolbar();
        add(toolbar, BorderLayout.NORTH);
    }
}
