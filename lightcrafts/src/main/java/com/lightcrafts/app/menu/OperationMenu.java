/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.Document;

import javax.swing.*;
import java.lang.ref.WeakReference;
import java.util.List;

class OperationMenu extends UpdatableDisposableMenu {

    private WeakReference<Document> docRef = new WeakReference<Document>(null);

    OperationMenu(ComboFrame frame) {
        super(frame, "Operation");

        JMenuItem proofItem = new ProofMenuItem(frame);

        proofItem.setEnabled(false);

        add(proofItem);

        put("proof", proofItem);

        addSeparator();

        JMenuItem emptyItem = MenuFactory.createMenuItem("NoOperation");
        emptyItem.setEnabled(false);
        add(emptyItem);
    }

    void update() {
        ComboFrame frame = getComboFrame();
        Document newDoc = (frame != null) ? frame.getDocument() : null;
        Document oldDoc = docRef.get();

        if (newDoc != oldDoc) {
            removeAll();

            JMenuItem proofItem = (JMenuItem) get("proof");

            add(proofItem);

            addSeparator();

            if (newDoc != null) {

                List<Action> opActions = newDoc.getOperations();
                JMenuItem addItem;
                for (Action action : opActions) {
                    addItem = new JMenuItem(action);
                    // These actions normally have SHORT_DESCRIPTION set:
                    addItem.setToolTipText(null);
                    // And they always have icons:
                    addItem.setIcon(null);
                    addItem.setText(addItem.getText());
                    add(addItem);
                }
            }
            else {
                JMenuItem emptyItem = MenuFactory.createMenuItem("NoOperation");
                emptyItem.setEnabled(false);
                add(emptyItem);
            }
            docRef = new WeakReference<Document>(newDoc);
        }
        super.update();
    }
}
