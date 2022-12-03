/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.operation.zone;

import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.MouseEvent;

/** This MouseInputListener tracks mouse drags on a single Component and
 * updates a ZoneModel accordingly.
 */

class DragListener extends MouseInputAdapter {

    private ZoneModel model;

    private Point startCursor;
    private Point startComponent;
    private boolean dragging;

    void setModel(ZoneModel model) {
        this.model = model;
    }

    void addSelfTo(Spacer spacer) {
        spacer.addMouseListener(this);
        spacer.addMouseMotionListener(this);
    }

    void removeSelfFrom(Spacer spacer) {
        spacer.removeMouseListener(this);
        spacer.removeMouseMotionListener(this);
    }

    public void mousePressed(MouseEvent event) {
        Spacer spacer = (Spacer) event.getComponent();
        event.translatePoint(spacer.getX(), spacer.getY());
        startCursor = event.getPoint();
        startComponent = spacer.getLocation();
        dragging = true;
        notifyDragStart();
        spacer.updateModel();
    }

    public void mouseDragged(MouseEvent event) {
        if (dragging) {
            Component c = event.getComponent();

            event.translatePoint(c.getX(), c.getY());

            Point p1 = startCursor;
            Point p2 = event.getPoint();

            Point interval = new Point(p2.x - p1.x, p2.y - p1.y);

            Spacer spacer = (Spacer) c;
            spacer.moveTo(startComponent.y + interval.y);
        }
    }

    public void mouseReleased(MouseEvent event) {
        if (dragging) {
            notifyDragEnd();
            dragging = false;
        }
    }

    void notifyDragStart() {
        model.batchStart();
    }

    void notifyDragEnd() {
        model.batchEnd();
    }
}
