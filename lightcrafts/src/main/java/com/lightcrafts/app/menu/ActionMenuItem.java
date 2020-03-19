/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

// This is a DocumentMenuItem with a customized Action PropertyChangeListener
// that ensures that this JMenuItem will not inherit an icon, a mnemonic, or an
// accelerator reset from its Action.

abstract class ActionMenuItem extends DocumentMenuItem {

    // These get cleared by setAction() and must be restored every time:
    private String text;
    private KeyStroke accelerator;
    private int mnemonic;

    @SuppressWarnings({"OverridableMethodCallInConstructor"})
    protected ActionMenuItem(ComboFrame frame, String key) {
        super(frame, key);
        text = getText();
        accelerator = getAccelerator();
        mnemonic = getMnemonic();
        setEnabled(false);
    }

    void update() {
        final Action oldAction = getAction();
        final Action newAction = getDocumentAction();
        if (newAction != oldAction) {
            if (newAction != null) {
                setAction(newAction);
                setIcon(null);
            }
            else {
                setAction(null);
                setText(text);
                setEnabled(false);
            }
            setAccelerator(accelerator);
            setMnemonic(mnemonic);
        }
    }

    abstract Action getDocumentAction();

    public void actionPerformed(ActionEvent event) {
        // The Action from getDocumentAction() is invoked automatically.
    }

    protected PropertyChangeListener createActionPropertyChangeListener(
        Action a
    ) {
        return new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent e) {
                final String propName = e.getPropertyName();
                if (propName.equals(Action.NAME)) {
                    final String text = (String) e.getNewValue();
                    setText(text);
                    repaint();
                }
                else if (propName.equals("enabled")) {
                    final Boolean enabled = (Boolean) e.getNewValue();
                    setEnabled(enabled.booleanValue());
                    repaint();
                }
            }
        };
    }
}
