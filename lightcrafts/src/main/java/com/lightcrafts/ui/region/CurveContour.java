/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.model.CloneContour;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;

class CurveContour implements CloneContour {

    private Shape outer;
    private float width;
    private Point2D clonePt;
    private Integer version;

    private Point2D translation;    // non-null means translated

    CurveContour(Curve curve) {
        outer = curve;
        width = curve.getWidth();
        clonePt = curve.getClonePoint();
        version = curve.getVersion();

        // Clone, to be isolated from changes to the Curve:
        if (curve.isValidShape()) {
            outer = new GeneralPath(curve);
        }
    }

    public Shape getOuterShape() {
        return outer;
    }

    public float getWidth() {
        return width;
    }

    public synchronized Point2D getTranslation() {
        return translation;
    }

    // This method may return null, if this CurveContour's original Curve
    // had no clone point.
    public Point2D getClonePoint() {
        return clonePt;
    }

    public Integer getVersion() {
        return version;
    }

    synchronized void addTranslation(Point2D p) {
        if (translation == null) {
            translation = p;
        }
        else {
            double x = translation.getX();
            double y = translation.getY();
            translation = new Point2D.Double(x + p.getX(), y + p.getY());
        }
    }
}
