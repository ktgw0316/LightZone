/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Line2D;

public class PolygonCurve extends AbstractCurve {

    void updateShape() {
        if (points.size() < 2) {
            shape = null;
            segments.clear();
            return;
        }
        GeneralPath path = new GeneralPath();

        segments.clear();

        Point2D p = (Point2D) points.get(0);
        for (int n=1; n<points.size(); n++) {
            Point2D q = (Point2D) points.get(n);
            Line2D line =
                new Line2D.Double(p.getX(), p.getY(), q.getX(), q.getY());
            segments.add(line);
            path.append(line, true);
            p = q;
        }
        path.closePath();
        shape = path;
    }
}
