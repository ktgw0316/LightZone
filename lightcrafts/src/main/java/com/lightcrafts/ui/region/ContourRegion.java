/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.model.Contour;
import com.lightcrafts.model.Region;

import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

class ContourRegion implements Region {

    private Collection contours;
    private Area outerArea;

    ContourRegion(Collection contours) {
        this.contours = contours;
        outerArea = new Area();
        for (Iterator i=contours.iterator(); i.hasNext(); ) {
            Contour contour = (Contour) i.next();
            Shape outerShape = contour.getOuterShape();
            outerArea.add(new Area(outerShape));
        }
    }

    public Collection getContours() {
        return new ArrayList(contours);
    }

    public Shape getOuterShape() {
        return outerArea;
    }

    public float getWidth() {
        // This doesn't make any sense.  The Region interface should stop
        // extending Contour since the getWidth() method was added.
        return Float.NaN;
    }

    public Point2D getTranslation() {
        // This doesn't make any sense.  The Region interface should stop
        // extending Contour since the getTranslation() method was added.
        return null;
    }
}
