/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.rotate;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

// A model for a Euclidean line (as opposed to a line segment).

class InfiniteLine {

    private Point2D p;
    private double angle;
    private boolean isMoreHorizontal;

    InfiniteLine(double x, double y, double angle) {
        p = new Point2D.Double(x, y);
        this.angle = angle;
        isMoreHorizontal = (Math.abs(Math.sin(angle)) < Math.sqrt(.5));
    }

    double getY(double x) {
        return p.getY() + (x - p.getX()) * Math.tan(angle);
    }

    double getX(double y) {
        return p.getX() + (y - p.getY()) / Math.tan(angle);
    }

    Line2D getSegment(Rectangle2D rect) {
        if (isMoreHorizontal) {
            double x1 = rect.getX();
            double x2 = rect.getX() + rect.getWidth();
            Line2D line = getHorizontalSegment(x1, x2);
            if (rect.intersectsLine(line)) {
                return line;
            }
        }
        else {
            double y1 = rect.getY();
            double y2 = rect.getY() + rect.getHeight();
            Line2D line = getVerticalSegment(y1, y2);
            if (rect.intersectsLine(line)) {
                return line;
            }
        }
        return null;
    }

    boolean intersects(Rectangle2D rect) {
        return getSegment(rect) != null;
    }

    private Line2D getHorizontalSegment(double x1, double x2) {
        double y1 = getY(x1);
        double y2 = getY(x2);
        return new Line2D.Double(x1, y1, x2, y2);
    }

    private Line2D getVerticalSegment(double y1, double y2) {
        double x1 = getX(y1);
        double x2 = getX(y2);
        return new Line2D.Double(x1, y1, x2, y2);
    }
}
