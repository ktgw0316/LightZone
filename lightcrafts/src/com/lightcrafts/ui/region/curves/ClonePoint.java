/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

import com.lightcrafts.utils.awt.geom.HiDpi;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

/**
 * This class models the "clone point" associated with some Curves.  It
 * holds the coordinates of the point, does hit testing for mouse events on
 * the point, and it renders the point.
 */
class ClonePoint implements Cloneable {

    // rendering size at 1-1
    private static final float NominalRadius =
            8 * (float) Math.sqrt(HiDpi.defaultTransform.getDeterminant());

    private Stroke foregroundStroke = new BasicStroke(1f);
    private Stroke backgroundStroke = new BasicStroke(3f);

    private float radius;       // The actual rendering size, after scaling
    private Point2D point;      // The special point location
    private GeneralPath shape;  // The Shape to draw, centered on the point

    // Default constructor is for save/restore.
    ClonePoint() {
        this(new Point2D.Double());
    }

    ClonePoint(Point2D p) {
        radius = NominalRadius;
        setPoint(p);
    }

    void setPoint(Point2D p) {
        point = (Point2D) p.clone();
        updateShape();
    }

    Point2D getPoint() {
        return (Point2D) point.clone();
    }

    void translate(double dx, double dy) {
        point.setLocation(point.getX() + dx, point.getY() + dy);
        updateShape();
    }

    void paint(Graphics2D g) {
        AffineTransform xform = g.getTransform();
        float scale = (float) Math.sqrt(Math.abs(xform.getDeterminant()));
        scale = Math.min(scale, 1);
        float r = NominalRadius / scale;
        if (r != radius) {
            radius = r;
            updateShape();
        }
        Color oldColor = g.getColor();
        Stroke oldStroke = g.getStroke();
        g.setColor(Color.black);
        g.setStroke(backgroundStroke);
        g.draw(shape);
        g.setColor(Color.white);
        g.setStroke(foregroundStroke);
        g.draw(shape);
        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    boolean isAt(Point2D p) {
        return shape.getBounds2D().contains(p);
    }

    Rectangle2D getPaintBounds() {
        Shape stroked = backgroundStroke.createStrokedShape(shape);
        return stroked.getBounds2D();
    }

    public Object clone() {
        return new ClonePoint(point);
    }

    void save(XmlNode node) {
        node.setAttribute("x", Double.toString(point.getX()));
        node.setAttribute("y", Double.toString(point.getY()));
    }

    void restore(XmlNode node) throws XMLException {
        double x = Double.parseDouble(node.getAttribute("x"));
        double y = Double.parseDouble(node.getAttribute("y"));
        Point2D p = new Point2D.Double(x, y);
        setPoint(p);
    }

    private void updateShape() {
        foregroundStroke = new BasicStroke(radius / 8f);
        backgroundStroke = new BasicStroke(3f * radius / 8f);

        float x = (float) point.getX();
        float y = (float) point.getY();

        shape = new GeneralPath();
        shape.moveTo(x, y - radius);
        shape.lineTo(x, y - radius / 2);
        shape.moveTo(x, y + radius / 2);
        shape.lineTo(x, y + radius);
        shape.moveTo(x - radius, y);
        shape.lineTo(x - radius / 2, y);
        shape.moveTo(x + radius / 2, y);
        shape.lineTo(x + radius, y);
    }
}
