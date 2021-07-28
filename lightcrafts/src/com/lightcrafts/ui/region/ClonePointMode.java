/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.utils.awt.geom.HiDpi;

import java.awt.*;
import java.awt.event.MouseEvent;

class ClonePointMode extends MinorRegionMode {

    private Point startPoint;
    private Point currentPoint;
    private Curve curve;

    ClonePointMode(RegionMode oldMode, Curve curve, Point initPoint) {
        super(oldMode);
        this.curve = curve;
        startPoint = initPoint;
        currentPoint = startPoint;
        comp.setCursor(MovingPointCursor);
        model.notifyChangeStart(curve);
        model.editStart();
    }

    public void mouseReleased(MouseEvent event) {
        Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        update(p, false);
        model.setMajorMode(new EditCurveMode(this, curve));
        if (! currentPoint.equals(startPoint)) {
            model.editEnd();
        }
        else {
            model.editCancel();
        }
        model.notifyChangeEnd(curve);
    }

    public void mouseMoved(MouseEvent event) {
        Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        update(p, true);
        autoscroll(event);
    }

    public void mouseDragged(MouseEvent event) {
        Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        update(p, true);
        autoscroll(event);
    }

    private void update(Point p, boolean isUpdating) {
        comp.setClonePoint(curve, currentPoint, p, isUpdating);
        currentPoint = p;
    }
}
