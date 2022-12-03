/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor;

import com.lightcrafts.ui.operation.OpActions;
import com.lightcrafts.ui.operation.OpStack;
import com.lightcrafts.ui.toolkit.CoolButton;
import com.lightcrafts.ui.toolkit.IconFactory;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.List;

class OpsToolbar extends JPanel {

    // Add space below, to tune the layout:
    private static Border ToolBorder =
        BorderFactory.createEmptyBorder(0, 0, 3, 0);

    private List actions;

    OpsToolbar(OpStack stack) {
        this(stack.getAddActions(), true);
    }

    // Make a disabled OpToolbar, for the no-Document display mode:
    OpsToolbar() {
        this(OpStack.getStaticAddActions(), false);
    }

    private OpsToolbar(List actions, boolean enabled) {
        this.actions = actions;
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setEnabled(enabled);
        initButtons();
        setBorder(ToolBorder);
    }

    private void initButtons() {
        boolean enabled = isEnabled();
        add(Box.createHorizontalGlue());

        CoolButton.ButtonStyle style = CoolButton.ButtonStyle.LEFT;
        for (Iterator i=actions.iterator(); i.hasNext(); ) {
            Action action = (Action) i.next();
            if (! i.hasNext()) {
                style = CoolButton.ButtonStyle.RIGHT;
            }
            JButton button = createButton(action, style);
            button.setEnabled(enabled);
            add(button);
            style = CoolButton.ButtonStyle.CENTER;
        }
        add(Box.createHorizontalGlue());
    }

    private static JButton createButton(
        Action action, CoolButton.ButtonStyle style
    ) {
        BufferedImage image =
            (BufferedImage) action.getValue(OpActions.IconImageKey);
        image = IconFactory.getScaledImage(image, IconFactory.StandardSize + 1);
        Icon icon = new ImageIcon(image);
        
        CoolButton button = new CoolButton(style);
        action.putValue(Action.SMALL_ICON, icon);
        button.setAction(action);
        button.setIcon(icon);

        return button;
    }
}
