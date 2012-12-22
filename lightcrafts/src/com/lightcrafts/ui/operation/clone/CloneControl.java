/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.clone;

import com.lightcrafts.model.CloneOperation;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.ui.operation.OpStack;
import com.lightcrafts.ui.help.HelpConstants;

import javax.swing.*;
import java.awt.*;

/**
 * A featureless OpControl to represent the CloneOperation in an OpStack.
 * This OpControl has no specialized controls.  The CloneOperation takes all
 * its settings from its Region.
 */

public class CloneControl extends OpControl {

    // This control has no children and inherits no preferred size from
    // library components.  So we make one up:

    private final static Dimension PreferredSize = new Dimension(160, 20);

    public CloneControl(CloneOperation op, OpStack stack) {
        super(op, stack);
        readyForUndo();
        JPanel content = new JPanel();
        content.setPreferredSize(PreferredSize);
        setContent(content);
    }

    protected String getHelpTopic() {
        return HelpConstants.HELP_TOOL_CLONE;
    }
}
