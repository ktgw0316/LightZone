/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.geom.Point2D;

/**
 * Manage the arrow key bindings for nudging selected Curves, which must be
 * registered and unregistered in response to Curve selection events.  This
 * allows arrow key events to be intercepted by ancestor components (like
 * scroll panes) when there are no selected Curves to nudge.
 */

class CurveNudger {

    private final static KeyStroke LeftKey1 =
        KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0);
    private final static KeyStroke LeftKey2 =
        KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0);
    private final static KeyStroke RightKey1 =
        KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0);
    private final static KeyStroke RightKey2 =
        KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0);
    private final static KeyStroke UpKey1 =
        KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0);
    private final static KeyStroke UpKey2 =
        KeyStroke.getKeyStroke(KeyEvent.VK_KP_UP, 0);
    private final static KeyStroke DownKey1 =
        KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0);
    private final static KeyStroke DownKey2 =
        KeyStroke.getKeyStroke(KeyEvent.VK_KP_DOWN, 0);

    private CurveComponent comp;
    private RegionModel model;

    private Action leftAction, rightAction, upAction, downAction;

    private boolean registered;

    CurveNudger(RegionModel model, CurveComponent comp) {
        this.comp = comp;
        this.model = model;
        leftAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveSelectedCurves(-1, 0);
            }
        };
        rightAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveSelectedCurves(1, 0);
            }
        };
        upAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveSelectedCurves(0, -1);
            }
        };
        downAction = new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                moveSelectedCurves(0, 1);
            }
        };
    }

    void registerKeys() {
        if (! registered) {
            comp.registerKeyboardAction(
                leftAction, LeftKey1, JComponent.WHEN_FOCUSED
            );
            comp.registerKeyboardAction(
                leftAction, LeftKey2, JComponent.WHEN_FOCUSED
            );
            comp.registerKeyboardAction(
                rightAction, RightKey1, JComponent.WHEN_FOCUSED
            );
            comp.registerKeyboardAction(
                rightAction, RightKey2, JComponent.WHEN_FOCUSED
            );
            comp.registerKeyboardAction(
                upAction, UpKey1, JComponent.WHEN_FOCUSED
            );
            comp.registerKeyboardAction(
                upAction, UpKey2, JComponent.WHEN_FOCUSED
            );
            comp.registerKeyboardAction(
                downAction, DownKey1, JComponent.WHEN_FOCUSED
            );
            comp.registerKeyboardAction(
                downAction, DownKey2, JComponent.WHEN_FOCUSED
            );
            registered = true;
        }
    }

    void unregisterKeys() {
        if (registered) {
            comp.unregisterKeyboardAction(LeftKey1);
            comp.unregisterKeyboardAction(LeftKey2);
            comp.unregisterKeyboardAction(RightKey1);
            comp.unregisterKeyboardAction(RightKey2);
            comp.unregisterKeyboardAction(UpKey1);
            comp.unregisterKeyboardAction(UpKey2);
            comp.unregisterKeyboardAction(DownKey1);
            comp.unregisterKeyboardAction(DownKey2);
            registered = false;
        }
    }

    // Perform nudging on the selected Curves.
    private void moveSelectedCurves(int x, int y) {
        model.editStart();
        CurveSelection selection = model.getSelection();
        CurveIterator curves = selection.iterator();
        Point2D p1 = new Point2D.Double(0, 0);
        Point2D p2 = new Point2D.Double(x, y);
        while (curves.hasNext()) {
            comp.moveCurve(curves.nextCurve(), p1, p2);
        }
        model.editEnd();
    }
}
