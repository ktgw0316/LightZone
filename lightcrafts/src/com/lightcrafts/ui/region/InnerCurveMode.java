/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.utils.awt.geom.HiDpi;

import java.awt.*;
import java.awt.event.MouseEvent;

class InnerCurveMode extends MinorRegionMode {

    private Curve curve;
    private float startWidth;

    InnerCurveMode(RegionMode oldMode, Curve curve) {
        super(oldMode);
        this.curve = curve;
        startWidth = curve.getWidth();
        comp.setCursor(MovingCurveCursor);
        model.notifyChangeStart(curve);
        model.editStart();
    }

    public void mouseReleased(MouseEvent event) {
        model.setMajorMode(new EditCurveMode(this, curve));
        if (curve.getWidth() != startWidth) {
            model.editEnd();
        }
        else {
            model.editCancel();
        }
        final Point p = HiDpi.imageSpacePointFrom(event.getPoint());
        update(p, false);
        
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

    private void update(Point p, boolean isUpdating) {
        comp.setInnerShapeAt(curve, p, isUpdating);
    }
}
