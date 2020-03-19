/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.utils.awt.geom.HiDpi;

import java.awt.*;
import java.awt.event.MouseEvent;

class FollowMouseOnceMode extends MinorRegionMode {

    private Curve curve;
    private int index;

    FollowMouseOnceMode(RegionMode oldMode, Curve curve, int index) {
        super(oldMode);
        this.curve = curve;
        this.index = index;
        comp.setCursor(MovePointCursor);
        model.notifyChangeStart(curve);
        model.editStart();
    }

    public void mouseReleased(MouseEvent event) {
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        update(p, false);
        model.setMajorMode(new EditCurveMode(this, curve));
        model.editEnd();
        model.notifyChangeEnd(curve);
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

    public void mouseExited(MouseEvent event) {
        // If the mouse exits, the MagneticPoint wants to stick:
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        update(p, false);
    }

    private void update(Point p, boolean isUpdating) {
        comp.movePoint(curve, index, p, isUpdating);
    }
}
