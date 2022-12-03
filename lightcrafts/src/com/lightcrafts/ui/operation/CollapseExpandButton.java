/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2022-     Masahiro Kitagawa */

package com.lightcrafts.ui.operation;

import com.lightcrafts.ui.action.ToggleAction;
import com.lightcrafts.ui.toolkit.ImageOnlyButton;

import javax.swing.*;
import java.beans.PropertyChangeListener;

class CollapseExpandButton extends JToggleButton {

    CollapseExpandButton(Action action) {
        super(action);
        Icon pressed = (Icon) action.getValue(ToggleAction.PRESSED_ICON);
        setPressedIcon(pressed);
        setText(null);
        ImageOnlyButton.setStyle(this);
    }

    @Override
    protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
        return event -> {
            switch (event.getPropertyName()) {
                case Action.SMALL_ICON:
                    Icon icon = (Icon) event.getNewValue();
                    setIcon(icon);
                    repaint();
                    break;
                case "enabled":
                    Boolean enabled = (Boolean) event.getNewValue();
                    setEnabled(enabled);
                    repaint();
                    break;
                case Action.SHORT_DESCRIPTION:
                    String text = (String) event.getNewValue();
                    setToolTipText(text);
                    break;
            }
        };
    }
}