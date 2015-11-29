/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** This interface defines an abstraction for Contour behavior in the context
  * of this package.  The only reason this is public is so that the various
  * Contour implementations (polygon, cubic basis, etc.) can be factored into
  * a separate package.
  * <p>
  * Users should not access this interface directly.
  */

public interface Curve extends SharedShape, Cloneable {

    boolean allowsAddRemovePoints();

    int addPoint(Point2D p);

    void insertPoint(int n, Point2D p);

    void removePoint(int n);

    void movePoint(int n, Point2D p);

    void translate(double dx, double dy);

    boolean isValidShape();

    Rectangle2D getPaintBounds();

    boolean isOnCurve(Point2D p);

    int getSegmentAt(Point2D p);

    int getPointAt(Point2D p);

    void setShowInnerShape(boolean show);

    boolean getShowInnerShape();

    boolean isInnerShape(Point2D p);

    void setInnerShape(Point2D p);

    float getWidth();

    void setWidth(float width);

    Integer getVersion();

    void setClonePoint(Point2D p);

    Point2D getClonePoint();

    boolean isClonePoint(Point2D p);

    void highlightPoint(int index);

    void highlightSegment(int index);

    void highlightAllSegments();

    void highlightAll();

    void highlightInterior(boolean on);

    void resetHighlights();

    void paint(Graphics2D g, boolean showClonePoint);

    boolean matches(Curve curve);

    Object clone();

    void save(XmlNode node);

    void restore(XmlNode node) throws XMLException;
}
