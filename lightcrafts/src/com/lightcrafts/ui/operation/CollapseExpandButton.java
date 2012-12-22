/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation;

import com.lightcrafts.ui.action.ToggleAction;
import com.lightcrafts.ui.toolkit.ImageOnlyButton;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.jvnet.substance.SubstanceLookAndFeel;

class CollapseExpandButton extends JToggleButton {

    CollapseExpandButton(Action action) {
        super(action);
        Icon pressed = (Icon) action.getValue(ToggleAction.PRESSED_ICON);
        setPressedIcon(pressed);
        setText(null);
        ImageOnlyButton.setStyle(this);
        putClientProperty(SubstanceLookAndFeel.BUTTON_NO_MIN_SIZE_PROPERTY, Boolean.TRUE);
    }

    protected PropertyChangeListener createActionPropertyChangeListener(
        Action a
    ) {
        return new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                String propName = e.getPropertyName();
                if (propName.equals(Action.SMALL_ICON)) {
                    Icon icon = (Icon) e.getNewValue();
                    setIcon(icon);
                    repaint();
                }
                else if (propName.equals("enabled")) {
                    Boolean enabled = (Boolean) e.getNewValue();
                    setEnabled(enabled.booleanValue());
                    repaint();
                }
                else if (propName.equals(Action.SHORT_DESCRIPTION)) {
                    String text = (String) e.getNewValue();
                    setToolTipText(text);
                }
            }
        };
    }
}