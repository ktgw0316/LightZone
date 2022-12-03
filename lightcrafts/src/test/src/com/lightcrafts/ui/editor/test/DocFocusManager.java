/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.editor.test;

import java.awt.*;
import javax.swing.*;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * A developer class to observe and develop policies for keyboard focus
 * management.
 */
class DocFocusManager {

    static void start() {

        KeyboardFocusManager focus =
            KeyboardFocusManager.getCurrentKeyboardFocusManager();

        focus.addPropertyChangeListener(
            new PropertyChangeListener() {
                public void propertyChange(PropertyChangeEvent evt) {
                    System.out.println(getString(evt));
                }
            }
        );
    }

    private static String getString(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        Object oldValue = evt.getOldValue();
        String oldName = (oldValue != null) ?
            oldValue.getClass().getName() : "null";
        Object newValue = evt.getNewValue();
        String newName = (newValue != null) ?
            newValue.getClass().getName() : "null";
        return (propName + ": " + oldName + " -> " + newName);
    }

    private static boolean isInterestingFocusChange(PropertyChangeEvent evt) {
        Object oldValue = evt.getOldValue();
        Object newValue = evt.getNewValue();
        return (oldValue instanceof JDialog) && (newValue == null);
    }
}
