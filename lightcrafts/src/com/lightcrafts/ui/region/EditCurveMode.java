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

class EditCurveMode extends MajorRegionMode {

    // Keys to delete the editing Curve and exit this RegionMode:
    private static KeyStroke DeleteKey1 =
        KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0);
    private static KeyStroke DeleteKey2 =
        KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0);

    // Keys to exit this RegionMode:
    private static KeyStroke ExitKey1 =
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
    private static KeyStroke ExitKey2 =
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);

    private Curve curve;

    EditCurveMode(RegionMode oldMode, Curve curve) {
        super(oldMode);
        this.curve = curve;
    }

    void modeEntered() {
        Action deleteAction = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                model.editStart();
                CurveSelection selection = model.getSelection();
                CurveIterator curves = selection.iterator();
                while (curves.hasNext()) {
                    model.removeCurve(comp, curves.nextCurve());
                }
                model.setMajorMode(new NewCurveMode(EditCurveMode.this));
                model.editEnd();
            }
        };
        comp.registerKeyboardAction(
            deleteAction, DeleteKey1, JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        comp.registerKeyboardAction(
            deleteAction, DeleteKey2, JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        Action exitAction = new AbstractAction() {
            public void actionPerformed(ActionEvent event) {
                finish();
            }
        };
        comp.registerKeyboardAction(
            exitAction, ExitKey1, JComponent.WHEN_IN_FOCUSED_WINDOW
        );
        comp.registerKeyboardAction(
            exitAction, ExitKey2, JComponent.WHEN_IN_FOCUSED_WINDOW
        );
    }

    void modeExited() {
        comp.unregisterKeyboardAction(DeleteKey1);
        comp.unregisterKeyboardAction(DeleteKey1);
        comp.unregisterKeyboardAction(ExitKey1);
        comp.unregisterKeyboardAction(ExitKey2);
    }

    void save(XmlNode node) {
        CurveFactory.save(curve, node);
    }

    void restore(XmlNode node) throws XMLException {
        curve = CurveFactory.restore(node);
        curve = model.getRestoredCurve(curve);
    }

    Curve getEditingCurve() {
        return curve;
    }

    public void mousePressed(MouseEvent event) {
        if (event.isPopupTrigger() || event.getButton() != MouseEvent.BUTTON1) {
            return;     // popups are handled in CurveComponent
        }
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        Curve c = comp.getCurveAt(p);

        if (comp.isClonePointAt(curve, p)) {
            model.setMinorMode(new ClonePointMode(this, curve, p));
        }
        else if (c == curve) {   // the point is on our Curve
            int index = comp.getPointAt(curve, p);
            if (index < 0) {
                if (comp.isInnerShapeAt(curve, p)) {
                    model.setMinorMode(new InnerCurveMode(this, curve));
                    return;
                }
                if (curve.allowsAddRemovePoints()) {
                    index = comp.getSegmentAt(curve, p) + 1;
                    comp.insertPoint(curve, index, p);
                }
                else {
                    // A click on the boundary of a Curve that does not
                    // allow point editing does nothing.
                    return;
                }
            }
            model.setMinorMode(new FollowMouseOnceMode(this, curve, index));
        }
        else {
            c = comp.getCurveAround(p);
            if (c == curve) {   // the point is in our Curve
                if (comp.isInnerShapeAt(curve, p)) {
                    model.setMinorMode(new InnerCurveMode(this, curve));
                }
                else {
                    // not the inner curve, so translate
                    model.setMinorMode(new MoveEditingCurveMode(this, curve));
                }
            }
            else { // the point is not in or on our Curve:
                finish();
            }
        }
    }

    private void finish() {
        // Take an undo snapshot after the mode change:
        model.editStart();
        model.setMajorMode(new NewCurveMode(this));
        model.editEnd();
    }

    public void mouseMoved(MouseEvent event) {
        updateCursor(event);
    }

    public void mouseDragged(MouseEvent event) {
        updateCursor(event);
    }

    private void updateCursor(MouseEvent event) {
        Cursor newCursor;
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        if (comp.isClonePointAt(curve, p)) {
            newCursor = MovePointCursor;
        }
        else if (comp.isInnerShapeAt(curve, p)) {
            newCursor = MovePointCursor;
        }
        else if (comp.getCurveAt(p) == curve) {
            int index = comp.getPointAt(curve, p);
            if (index >= 0) {
                newCursor = MovePointCursor;
            }
            else if (curve.allowsAddRemovePoints()) {
                newCursor = NewPointCursor;
            }
            else {
                newCursor = DefaultCursor;
            }
        }
        else if (comp.getCurveAround(p) == curve) {
            newCursor = MoveCurveCursor;
        }
        else {
            newCursor = DefaultCursor;
        }
        if (newCursor != cursor) {
            comp.setCursor(newCursor);
            cursor = newCursor;
        }
    }
}
