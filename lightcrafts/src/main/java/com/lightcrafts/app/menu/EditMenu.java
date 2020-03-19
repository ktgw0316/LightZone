/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.platform.Platform;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.ui.editor.RegionManager;
import com.lightcrafts.ui.editor.Editor;
import com.lightcrafts.ui.editor.EditorControls;
import com.lightcrafts.ui.region.CurveIterator;
import com.lightcrafts.ui.region.CurveSelection;
import com.lightcrafts.ui.operation.OpStackListener;
import com.lightcrafts.ui.operation.OpControl;
import com.lightcrafts.ui.operation.SelectableControl;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;

final class EditMenu extends UpdatableDisposableMenu {

    // Remember region selections and enable "copy" according to
    // whether the selection is null:

    private CurveSelection.Listener CurveSelectionListener =
        new CurveSelection.Listener() {
            public void selectionChanged( CurveIterator oldSelection,
                                          CurveIterator newSelection ) {
                put("selection", newSelection);
                final boolean isSelected = newSelection.hasNext();
                final JMenuItem cutItem = (JMenuItem) get("cut");
                final JMenuItem copyItem = (JMenuItem) get("copy");
                cutItem.setEnabled(isSelected);
                copyItem.setEnabled(isSelected);
            }
        };

    // Make sure the "paste" item is disabled when the selection is a default
    // RAW adjustment.

    private OpStackListener ToolListener =
        new OpStackListener() {
            public void opAdded(OpControl control) {
            }
            public void opChanged(OpControl control) {
                isRegionToolSelected = ! control.isRawCorrection();
                updatePasteEnabled();
            }
            public void opChanged(SelectableControl control) {
                isRegionToolSelected = false;
                updatePasteEnabled();
            }
            public void opLockChanged(OpControl control) {
            }
            public void opRemoved(OpControl control) {
            }
        };

    private boolean isRegionToolSelected;

    private WeakReference<Document> docRef = new WeakReference<Document>(null);

    EditMenu(ComboFrame frame) {
        super(frame, "Edit");

        add(new UndoMenuItem(frame));
        add(new RedoMenuItem(frame));

        addSeparator();

        // Copy and paste is an elaborate dance:

        final JMenuItem cutItem = MenuFactory.createMenuItem("Cut");
        final JMenuItem copyItem = MenuFactory.createMenuItem("Copy");
        final JMenuItem pasteItem = MenuFactory.createMenuItem("Paste");
        final JMenuItem pasteRefItem = MenuFactory.createMenuItem("PasteRef");

        cutItem.setEnabled(false);
        copyItem.setEnabled(false);
        pasteItem.setEnabled(false);
        pasteRefItem.setEnabled(false);

        put("cut", cutItem);
        put("copy", copyItem);
        put("paste", pasteItem);
        put("pasteRef", pasteRefItem);

        add(cutItem);
        add(copyItem);
        add(pasteItem);
        add(pasteRefItem);

        // Enable "paste" after "cut":

        cutItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    RegionManager regions = (RegionManager) get("regions");
                    CurveIterator selection = (CurveIterator) get("selection");
                    regions.unShareShapes(selection);
                    put("clipboard", selection);
                    updatePasteEnabled();
                }
            }
        );
        // Enable "paste" after "copy":

        copyItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    final Object selection = get("selection");
                    put("clipboard", selection);
                    updatePasteEnabled();
                }
            }
        );

        // And "paste" does the deed:

        pasteItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    CurveIterator selection = (CurveIterator) get("clipboard");
                    RegionManager regions = (RegionManager) get("regions");
                    regions.shareShapes(selection, true);
                }
            }
        );

        pasteRefItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    CurveIterator selection = (CurveIterator) get("clipboard");
                    RegionManager regions = (RegionManager) get("regions");
                    regions.shareShapes(selection, false);
                }
            }
        );
        if (!Platform.isMac()) {
            // On the Mac, the "Preferences" item lies under the app menu.
            addSeparator();
            add(new PrefsMenuItem(frame));
        }
    }

    void update() {
        final ComboFrame frame = getComboFrame();
        final Document newDoc = (frame != null) ? frame.getDocument() : null;
        final Document oldDoc = docRef.get();

        if (newDoc != oldDoc) {
            if (oldDoc != null) {
                final RegionManager oldRegions = oldDoc.getRegionManager();
                oldRegions.removeSelectionListener(CurveSelectionListener);
                remove("regions");
                Editor editor = oldDoc.getEditor();
                EditorControls controls = editor.getToolStack();
                controls.removeOpStackListener(ToolListener);
            }
            if (newDoc != null) {
                final RegionManager newRegions = newDoc.getRegionManager();
                newRegions.addSelectionListener(CurveSelectionListener);
                put("regions", newRegions);
                Editor editor = newDoc.getEditor();
                EditorControls controls = editor.getToolStack();
                controls.addOpStackListener(ToolListener);
            }
            else {
                remove("regions");
                remove("selection");
                remove("clipboard");
            }
            docRef = new WeakReference<Document>(newDoc);
        }
        super.update();
    }

    private void updatePasteEnabled() {
        Object clipboard = get("clipboard");
        boolean canPaste = (clipboard != null) && isRegionToolSelected;
        JMenuItem pasteItem = (JMenuItem) get("paste");
        JMenuItem pasteRefItem = (JMenuItem) get("pasteRef");
        pasteItem.setEnabled(canPaste);
        pasteRefItem.setEnabled(canPaste);
    }
}
/* vim:set et sw=4 ts=4: */
