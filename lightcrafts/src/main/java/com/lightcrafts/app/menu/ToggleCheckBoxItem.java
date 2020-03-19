/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.*;

import com.lightcrafts.ui.action.ToggleAction;

/**
 * This checkbox menu item takes its properties from a ToggleAction instead
 * of a normal Action, and listens on TOGGLE_STATE property changes from the
 * ToggleAction to set its check mark.
 */
final class ToggleCheckBoxItem
    extends JCheckBoxMenuItem implements PropertyChangeListener
{
    ToggleCheckBoxItem(ToggleAction action) {
        super(action);
        setSelected(action.getState());
        action.addPropertyChangeListener(this);
    }

    public void propertyChange(PropertyChangeEvent event) {
        final String propName = event.getPropertyName();
        final ToggleAction action = (ToggleAction) event.getSource();
        if (propName.equals(ToggleAction.TOGGLE_STATE)) {
            setSelected(action.getState());
        }
    }
}
/* vim:set et sw=4 ts=4: */
