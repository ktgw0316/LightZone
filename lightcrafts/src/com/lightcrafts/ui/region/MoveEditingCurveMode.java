/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.utils.awt.geom.HiDpi;

import java.awt.*;
import java.awt.event.MouseEvent;

class MoveEditingCurveMode extends MinorRegionMode {

    private Point currentPoint;
    private CurveIterator curves;
    private Curve editingCurve;

    MoveEditingCurveMode(RegionMode oldMode, Curve curve) {
        super(oldMode);
        editingCurve = curve;
        CurveSelection selection = model.getSelection();
        curves = selection.iterator();
        comp.setCursor(MovingCurveCursor);
        while (curves.hasNext()) {
            model.notifyChangeStart(curves.nextCurve());
        }
        model.editStart();
    }

    public void mouseReleased(MouseEvent event) {
        model.setMajorMode(new EditCurveMode(this, editingCurve));
        if (currentPoint != null) {
            model.editEnd();
        }
        else {
            model.editCancel();
        }
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        update(p, false);

        curves.reset();
        while (curves.hasNext()) {
            model.notifyChangeEnd(curves.nextCurve());
        }
    }

    public void mouseMoved(MouseEvent event) {
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        update(p, true);
        autoscroll(event);
    }

    public void mouseDragged(MouseEvent event) {
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        update(p, true);
        autoscroll(event);
    }

    private void update(Point p, boolean isUpdating) {
        if (currentPoint != null) {
            curves.reset();
            while (curves.hasNext()) {
                comp.moveCurve(curves.nextCurve(), currentPoint, p, isUpdating);
            }
        }
        currentPoint = p;
    }
}
