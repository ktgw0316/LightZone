/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.app;

import static com.lightcrafts.app.Locale.LOCALE;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.ui.toolkit.IconFontFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

class UndoButton extends EditorButton implements PropertyChangeListener {

    private final static Icon Icon = IconFontFactory.buildIcon("undo");

    private final static String ToolTip = LOCALE.get("UndoButtonToolTip");

    private Document doc;

    UndoButton(ComboFrame frame) {
        super(frame, Icon);
        setStyle(ButtonStyle.LEFT);
        setToolTipText(ToolTip);
        setEnabled(false);
        addActionListener(
            new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent event) {
                    doc.getUndoAction().actionPerformed(event);
                }
            }
        );
    }

    @Override
    void updateButton() {
        ComboFrame frame = getComboFrame();
        Document newDoc = frame.getDocument();
        if (newDoc == doc) {
            return;
        }
        if (doc != null) {
            Action action = doc.getUndoAction();
            action.removePropertyChangeListener(this);
        }
        doc = newDoc;
        if (doc != null) {
            Action action = doc.getUndoAction();
            action.addPropertyChangeListener(this);
            setEnabled(action.isEnabled());
        }
        else {
            setEnabled(false);
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        Action action = (Action) evt.getSource();
        setEnabled(action.isEnabled());
    }
}
