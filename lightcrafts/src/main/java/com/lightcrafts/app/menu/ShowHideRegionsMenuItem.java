/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import static com.lightcrafts.app.menu.Locale.LOCALE;
import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.ui.editor.EditorMode;
import com.lightcrafts.ui.action.ToggleAction;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.*;

class ShowHideRegionsMenuItem extends ActionMenuItem {

    private final static String ShowRegionsText =
        LOCALE.get("ShowRegionsMenuItemText");
    private final static String HideRegionsText =
        LOCALE.get("HideRegionsMenuItemText");

    ShowHideRegionsMenuItem(ComboFrame frame) {
        super(frame, "ShowHideRegions");
    }

    Action getDocumentAction() {
        final Document doc = getDocument();
        if (doc != null) {
            Action action = new ToggleAction(HideRegionsText, ShowRegionsText) {
                protected void onActionPerformed(ActionEvent event) {
                    EditorMode mode = doc.getEditor().getMode();
                    if (mode != EditorMode.REGION) {
                        doc.getEditor().setMode(EditorMode.REGION);
                    }
                    else {
                        // We're out of phase with the ModeManager--
                        // toggle the mode anyway, but leave properties fixed.
                        doc.getEditor().setMode(EditorMode.ARROW);
                        EventQueue.invokeLater(
                            new Runnable() {
                                public void run() {
                                    setState(false);
                                }
                            }
                        );
                    }
                }
                protected void offActionPerformed(ActionEvent event) {
                    EditorMode mode = doc.getEditor().getMode();
                    if (mode == EditorMode.REGION) {
                        doc.getEditor().setMode(EditorMode.ARROW);
                    }
                    else {
                        // We're out of phase with the ModeManager--
                        // toggle the mode anyway, but leave properties fixed.
                        doc.getEditor().setMode(EditorMode.REGION);
                        EventQueue.invokeLater(
                            new Runnable() {
                                public void run() {
                                    setState(true);
                                }
                            }
                        );
                    }
                }
            };
            return action;
        }
        else {
            return null;
        }
    }
}
