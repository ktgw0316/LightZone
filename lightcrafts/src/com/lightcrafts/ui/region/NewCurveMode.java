/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.utils.awt.geom.HiDpi;
import com.lightcrafts.utils.xml.XmlNode;
import com.lightcrafts.utils.xml.XMLException;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

class NewCurveMode extends MajorRegionMode {

    // Keys to delete the selected Curve:
    private static KeyStroke DeleteKey1 =
        KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
    private static KeyStroke DeleteKey2 =
        KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);

    // A way to detect mouse pressed events that are associated with window
    // focus changes, so a click on the unfocused window doesn't also initiate
    // a region mode change.
    private long mouseEnteredTime;

    NewCurveMode(RegionModel model, CurveComponent comp) {
        super(model, comp);
    }

    NewCurveMode(RegionMode mode) {
        super(mode);
    }

    void modeEntered() {
        comp.setCursor(NewPointCursor);
        Action deleteAction = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                CurveSelection selection = model.getSelection();
                CurveIterator curves = selection.iterator();
                while (curves.hasNext()) {
                    model.removeCurve(comp, curves.nextCurve());
                }
            }
        };
        comp.registerKeyboardAction(
            deleteAction, DeleteKey1, JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        comp.registerKeyboardAction(
            deleteAction, DeleteKey2, JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    void modeExited() {
        comp.unregisterKeyboardAction(DeleteKey1);
        comp.unregisterKeyboardAction(DeleteKey2);
    }

    void save(XmlNode node) {
        // holds no Curve references, therefore do nothing
    }

    void restore(XmlNode node) throws XMLException {
        // holds no Curve references, therefore do nothing
    }

    Curve getEditingCurve() {
        return null;
    }

    public void mousePressed(MouseEvent event) {
        if (System.currentTimeMillis() - mouseEnteredTime < 100) {
            // If a user clicks on the image to focus the window, don't
            // interpret that same click as the start of a new curve.
            return;
        }
        if (event.isPopupTrigger() || event.getButton() != MouseEvent.BUTTON1) {
            return;     // popups are handled in CurveComponent
        }
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        Curve curve = comp.getCurveAround(p);

        CurveSelection selection = model.getSelection();
        CurveIterator curves = selection.iterator();

        // A nowhere click with selections means unselect:
        if ((curve == null) && curves.hasNext()) {
            selection.clear();
            return;
        }

        // Double-click means to edit the curve:
        if ((curve != null) && event.getClickCount() == 2) {
            if (event.getClickCount() == 2) {
                // Take an undo snapshot after the mode change:
                model.editStart();
                model.setMajorMode(new EditCurveMode(this, curve));
                model.editEnd();
                return;
            }
        }

        if (curve != null) {
            // Click inside a curve means move the curve:
            if ((event.getModifiers() & MouseEvent.SHIFT_MASK) == 0) {
                if (! selection.isSelected(curve)) {
                    selection.clear();
                }
            }
            model.getSelection().addCurve(curve);
            model.setMinorMode(new MoveCurveMode(this, p));
        }
        else {
            // A nowhere click without a selection means create a new curve:
            model.editStart();

            // If it's a spot, then there's no mode change;
            // just add the Curve, post the edit, and return:
            if (comp.makesSpotCurves()) {
                comp.addSpotCurve(p);
                model.editEnd();
                return;
            }
            // Otherwise, initialize a Curve and set up the mode change:
            Curve newCurve;
            if (comp.showsClonePoints()) {
                newCurve = comp.addCloneCurve(p);
            }
            else {
                newCurve = comp.addCurve();
            }
            comp.addPoint(newCurve, p);
            int index = comp.addPoint(newCurve, p);

            // Set the next mode, without initializing it:
            MajorRegionMode nextMode =
                new FollowMouseMode(this, newCurve, index);
            model.setMajorModeWithoutExitOrEnter(nextMode);
            modeExited();

            // Then take the undo snapshot:
            model.editEnd();

            // And finally initialize the new mode:
            nextMode.modeEntered();
        }
    }

    public void mouseEntered(MouseEvent event) {
        mouseEnteredTime = System.currentTimeMillis();
    }

    public void mouseMoved(MouseEvent event) {
        mouseEnteredTime = 0;   // it's save to handle mouse pressed events
        updateCursor(event);
    }

    public void mouseDragged(MouseEvent event) {
        updateCursor(event);
    }

    private void updateCursor(MouseEvent event) {
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        Curve curve = comp.getCurveAround(p);
        Cursor newCursor = (curve != null) ? MoveCurveCursor : NewPointCursor;
        if (newCursor != cursor) {
            comp.setCursor(newCursor);
            cursor = newCursor;
        }
    }
}
