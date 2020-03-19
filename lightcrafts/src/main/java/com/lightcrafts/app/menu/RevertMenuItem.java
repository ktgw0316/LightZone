/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.app.Application;
import com.lightcrafts.ui.editor.Document;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.lang.ref.WeakReference;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

class RevertMenuItem
    extends DocumentMenuItem implements PropertyChangeListener
{
    WeakReference docRef = new WeakReference(null);

    RevertMenuItem(ComboFrame frame) {
        super(frame, "Revert");
        Document doc = getDocument();
        if (doc != null) {
            Action undo = doc.getUndoAction();
            undo.addPropertyChangeListener(this);
            setEnabled(undo.isEnabled());
        }
        else {
            setEnabled(false);
        }
        docRef = new WeakReference(doc);
    }

    void update() {
        Document newDoc = getDocument();
        Document oldDoc = (Document) docRef.get();
        if (newDoc != oldDoc) {
            if (oldDoc != null) {
                Action oldUndo = oldDoc.getUndoAction();
                oldUndo.removePropertyChangeListener(this);
            }
            if (newDoc != null) {
                Action newUndo = newDoc.getUndoAction();
                newUndo.addPropertyChangeListener(this);
                setEnabled(newUndo.isEnabled());
            }
            else {
                setEnabled(false);
            }
            docRef = new WeakReference(newDoc);
        }
    }

    public void actionPerformed(ActionEvent event) {
        performPreAction( event );
        final ComboFrame frame = getComboFrame();
        Application.reOpen(frame);
    }

    // Listen for enable/disable on the undo action, and use this to determine
    // whether this menu item should be enabled.
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals("enabled")) {
            setEnabled(evt.getNewValue().equals(Boolean.TRUE));
        }
    }
}
