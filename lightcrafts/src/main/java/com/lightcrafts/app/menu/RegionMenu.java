/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.app.menu;

import com.lightcrafts.app.ComboFrame;
import com.lightcrafts.ui.editor.Document;
import com.lightcrafts.ui.editor.RegionManager;
import com.lightcrafts.ui.region.CurveFactory;
import com.lightcrafts.ui.region.CurveIterator;
import com.lightcrafts.ui.region.CurveSelection;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.ref.WeakReference;

class RegionMenu extends UpdatableDisposableMenu {

    // Handle curve type radio button selection changes:

    private ActionListener CurveTypeListener = new ActionListener() {
        public void actionPerformed(ActionEvent event) {
            JMenuItem polygonItem = (JMenuItem) get("polygon");
            JMenuItem basisItem = (JMenuItem) get("basis");
            JMenuItem bezierItem = (JMenuItem) get("bezier");

            RegionManager regions = (RegionManager) get("regions");
            if (regions != null) {
                if (polygonItem.isSelected()) {
                    regions.setCurveType(CurveFactory.Polygon);
                }
                if (basisItem.isSelected()) {
                    regions.setCurveType(CurveFactory.CubicBasis);
                }
                if (bezierItem.isSelected()) {
                    regions.setCurveType(CurveFactory.CubicBezier);
                }
            }
        }
    };

    // Remember region selections in the DisposableMenu,
    // and enable "delete" according to whether the selection is null:

    private CurveSelection.Listener CurveSelectionListener =
        new CurveSelection.Listener() {
            public void selectionChanged(
                CurveIterator oldSelection, CurveIterator newSelection
            ) {
                put("selection", newSelection);
                boolean isSelected = newSelection.hasNext();
                JMenuItem deleteItem = (JMenuItem) get("delete");
                deleteItem.setEnabled(isSelected);
            }
        };

    private WeakReference<Document> docRef = new WeakReference<Document>(null);

    RegionMenu(ComboFrame frame) {
        super(frame, "Region");

        add(new ShowHideRegionsMenuItem(frame));

        final JRadioButtonMenuItem polygonItem =
            MenuFactory.createRadioButtonMenuItem("Polygon");
        final JRadioButtonMenuItem basisItem =
            MenuFactory.createRadioButtonMenuItem("Basis");
        final JRadioButtonMenuItem bezierItem =
            MenuFactory.createRadioButtonMenuItem("Bezier");

        polygonItem.setEnabled(false);
        basisItem.setEnabled(false);
        bezierItem.setEnabled(false);

        ButtonGroup group = new ButtonGroup();
        group.add(polygonItem);
        group.add(basisItem);
        group.add(bezierItem);

        addSeparator();
        add(polygonItem);
        add(basisItem);
        add(bezierItem);

        put("polygon", polygonItem);
        put("basis", basisItem);
        put("bezier", bezierItem);

        polygonItem.addActionListener(CurveTypeListener);
        basisItem.addActionListener(CurveTypeListener);
        bezierItem.addActionListener(CurveTypeListener);

        JMenuItem deleteItem = MenuFactory.createMenuItem("Delete");

        deleteItem.setEnabled(false);

        addSeparator();
        add(deleteItem);

        put("delete", deleteItem);

        // UnShare the selected SharedShapes:
        
        deleteItem.addActionListener(
            new ActionListener() {
                public void actionPerformed(ActionEvent event) {
                    RegionManager regions = (RegionManager) get("regions");
                    CurveIterator selection = (CurveIterator) get("selection");
                    regions.unShareShapes(selection);
                }
            }
        );

        // Sync up menu items with the actual settings when the menu shows,
        // in case they get altered by some other controls:
        addMenuListener(
            new MenuListener() {
                public void menuCanceled(MenuEvent e) {
                }
                public void menuDeselected(MenuEvent e) {
                }
                public void menuSelected(MenuEvent e) {
                    RegionManager regions = (RegionManager) get("regions");
                    if (regions != null) {
                        int curveType = regions.getCurveType();
                        switch (curveType) {
                            case CurveFactory.Polygon:
                                polygonItem.setSelected(true);
                                break;
                            case CurveFactory.CubicBezier:
                                bezierItem.setSelected(true);
                                break;
                            case CurveFactory.CubicBasis:
                                basisItem.setSelected(true);
                                break;
                        }
                    }
                }
            }
        );
    }

    void update() {
        ComboFrame frame = getComboFrame();
        Document newDoc = (frame != null) ? frame.getDocument() : null;
        Document oldDoc = docRef.get();

        if (newDoc != oldDoc) {

            JMenuItem polygonItem = (JMenuItem) get("polygon");
            JMenuItem bezierItem = (JMenuItem) get("bezier");
            JMenuItem basisItem = (JMenuItem) get("basis");

            boolean hasDoc = newDoc != null;

            polygonItem.setEnabled(hasDoc);
            bezierItem.setEnabled(hasDoc);
            basisItem.setEnabled(hasDoc);

            remove("selection");

            if (oldDoc != null) {
                RegionManager regions = oldDoc.getRegionManager();
                regions.removeSelectionListener(CurveSelectionListener);
                remove("regions");
            }

            if (newDoc != null) {
                RegionManager regions = newDoc.getRegionManager();
                regions.addSelectionListener(CurveSelectionListener);
                put("regions", regions);
            }
            docRef = new WeakReference<Document>(newDoc);
        }
        super.update();
    }
}
