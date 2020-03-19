/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.utils.awt.geom.HiDpi;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

class FollowMouseMode extends MajorRegionMode {

    private Curve curve;
    private int index;

    // We apply our own definition of a double-click, for purposes of exiting
    // this mode.  See isSloppyDoubleClick().
    private MouseEvent recentClick;

    // Maximum double-click distance thresholds:
    private static final int MaxDoubleClickDistance = 20;  // pixels
    private static final int MaxDoubleClickTime = 500;    // milliseconds

    // Minimum-drag-distance threshold:
    private static final int NewPointDistance = 5;

    // Exits this RegionMode:
    private static KeyStroke ExitKey1 =
        KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
    private static KeyStroke ExitKey2 =
        KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);

    // If this is not null, then we're waiting to to cross the threshold:
    private Point lastMousePress;

    FollowMouseMode(RegionMode oldMode, Curve curve, int index) {
        super(oldMode);
        this.curve = curve;
        this.index = index;
    }

    void modeEntered() {
        comp.setCursor(NewPointCursor);
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
        model.editStart();
        model.notifyChangeStart(curve);
    }

    void modeExited() {
        comp.unregisterKeyboardAction(ExitKey1);
        comp.unregisterKeyboardAction(ExitKey2);
        model.notifyChangeEnd(curve);
        model.editEnd();
    }

    void save(XmlNode node) {
        CurveFactory.save(curve, node);
        node.setAttribute("index", Integer.toString(index));
    }

    void restore(XmlNode node) throws XMLException {
        curve = CurveFactory.restore(node);
        curve = model.getRestoredCurve(curve);
        index = Integer.parseInt(node.getAttribute("index"));
        lastMousePress = null;
    }

    Curve getEditingCurve() {
        return curve;
    }

    public void mousePressed(MouseEvent event) {
        if (isModified(event)) {
            finish();
        }
        else if (event.getClickCount() == 1) {
            lastMousePress = HiDpi.imageSpacePointFrom(event.getPoint());
        }
    }

    public void mouseClicked(MouseEvent event) {
        if (isSloppyDoubleClick(event)) {
            finish();
        }
    }

    // Keep track of mouse clicks, and identify double-clicks that are too
    // far apart for AWT to label as system double-clicks.
    private boolean isSloppyDoubleClick(MouseEvent click) {
        if (click.getClickCount() == 0) {
            return false;
        }
        if (click.getClickCount() == 2) {
            return true;
        }
        if (recentClick == null) {
            recentClick = click;
            return false;
        }
        long t1 = click.getWhen();
        long t2 = recentClick.getWhen();
        final Point p1 = HiDpi.imageSpacePointFrom(click.getPoint());
        final Point p2 = HiDpi.imageSpacePointFrom(recentClick.getPoint());

        recentClick = click;

        double dt = Math.abs(t1 - t2);
        double dx = p1.distance(p2);

        return ((dt < MaxDoubleClickTime) && (dx < MaxDoubleClickDistance));
    }

    private void finish() {
        MajorRegionMode nextMode;
        if (curve.isValidShape()) {
            // Switch to the edit mode:
            nextMode = new EditCurveMode(this, curve);
        }
        else {
            nextMode = new NewCurveMode(this);
            model.editCancel();
            model.editStart();
            comp.removeCurveWithoutModeChange(curve);
        }
        model.setMajorModeWithoutExitOrEnter(nextMode);
        modeExited();
        nextMode.modeEntered();
    }

    public void mouseMoved(MouseEvent event) {
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        maybeAddPoint(p);
        update(p);
        autoscroll(event);
    }

    public void mouseDragged(MouseEvent event) {
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        maybeAddPoint(p);
        update(p);
        autoscroll(event);
    }

    public void mouseExited(MouseEvent event) {
        // If the mouse exits, the MagneticPoint wants to stick:
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        update(p);
    }

    // Test if we've moved outside the minimum radius from the location of the
    // last mouse press, and if we have, then add a point to the curve:
    private void maybeAddPoint(Point p) {
        if (lastMousePress != null) {
            if (lastMousePress.distance(p) >= NewPointDistance) {
                index = comp.addPoint(curve, p);
                lastMousePress = null;
                model.editEnd();
                model.editStart();
            }
        }
    }

    private void update(Point p) {
        if (lastMousePress == null) {
            comp.movePoint(curve, index, p);
        }
        // Keep the blur width at a nice value, while the curve is small:
        if (curve.isValidShape()) {
            float width = curve.getWidth();
            if (width < 30) {
                comp.setInnerShapeAt(curve, null);
            }
        }
    }
}
