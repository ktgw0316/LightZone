/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.drag;

import javax.swing.event.MouseInputAdapter;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;

class StackDragListener extends MouseInputAdapter {

    private final static int DragThreshold = 5;

    private DraggableStack stack;

    private Point startCursor;
    private Point startComponent;
    private boolean dragging;

    StackDragListener(DraggableStack stack) {
        this.stack = stack;
    }

    @Override
    public void mousePressed(MouseEvent event) {
        JComponent comp = getAncestorJComponent(event);
        startCursor = event.getPoint();
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        JComponent comp = getAncestorJComponent(event);
        if ((startCursor != null) && (! dragging)) {
            Point p = event.getPoint();
            if (startCursor.distance(p) > DragThreshold) {
                startComponent = comp.getLocation();
                boolean isSwappable = ((StackableComponent) comp).isSwappable();
                stack.dragStart(comp, isSwappable);
                dragging = true;
            }
        }
        if (dragging) {
            Point p1 = startCursor;
            Point p2 = event.getPoint();
            Point interval = new Point(p2.x - p1.x, p2.y - p1.y);
            stack.dragTo(comp, startComponent.y + interval.y);
        }
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (dragging) {
            tearDown(event);
            dragging = false;
        }
        startCursor = null;
    }

    private JComponent getAncestorJComponent(MouseEvent event) {
        JComponent comp = (JComponent) event.getComponent();
        comp = (JComponent) SwingUtilities.getAncestorOfClass(
                StackableComponent.class, comp
                );
        event.translatePoint(comp.getX(), comp.getY());
        return comp;
    }

    private void tearDown(MouseEvent event) {
        JComponent comp = getAncestorJComponent(event);
        stack.dragEnd(comp);
    }
}
