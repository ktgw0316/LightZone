/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.utils.awt.geom.HiDpi;

import java.awt.*;
import java.awt.event.MouseEvent;

class MoveCurveMode extends MinorRegionMode {

    private Point startPoint;
    private Point currentPoint;
    private CurveIterator curves;

    MoveCurveMode(RegionMode oldMode, Point initPoint) {
        super(oldMode);
        startPoint = initPoint;
        currentPoint = startPoint;
        CurveSelection selection = model.getSelection();
        curves = selection.iterator();
        comp.setCursor(MovingCurveCursor);
        while (curves.hasNext()) {
            model.notifyChangeStart(curves.nextCurve());
        }
        model.editStart();
    }

    public void mouseReleased(MouseEvent event) {
        if (! currentPoint.equals(startPoint)) {
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
        model.setMajorMode(new NewCurveMode(this));
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
        curves.reset();
        while (curves.hasNext()) {
            comp.moveCurve(curves.nextCurve(), currentPoint, p, isUpdating);
        }
        currentPoint = p;
    }
}
