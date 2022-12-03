/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2015 Masahiro Kitagawa */

package com.lightcrafts.ui.region.curves;

import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;
import com.lightcrafts.ui.region.Curve;

import java.awt.*;
import java.awt.geom.*;
import java.util.Iterator;
import java.util.LinkedList;

/** This base class does all the work of managing control points and
  * painting.  Derived classes just need to fill in updateShape() to
  * translate the current "points" List into values for "shape" and
  * "segments".
  */

abstract class AbstractCurve implements Curve {

    // Derived classes must implement updateShape(), and in there they should
    // update these member variables to match the current values for control
    // points in the "points" List:
    GeneralPath shape;
    LinkedList<Shape> segments;        // Line2D, QuadCurve2D, or CubicCurve2D
    LinkedList<Point2D> points;          // Point2Ds for interpolation

    private final static float NominalRadius = 2f;  // Default for radius

    private float radius;       // Scale for all Strokes (see updateStrokes())

    private Stroke regularStroke;
    private Stroke backgroundStroke;    // Strokes defined by the radius
    private Stroke highlightStroke;
    private Stroke mouseHitStroke;

    private final static Color ForegroundColor = Color.black;
    private final static Color InnerShapeColor = Color.gray;
    private final static Color BackgroundColor = Color.white;
    private final static Color InteriorColor = new Color(255, 255, 255, 64);

    private final static float MinimumInnerSize = 4;

    private boolean showInnerShape;     // Compute and paint the innerShape
    private Shape innerShape;           // Keep slaved to "shape" above
    private float width;                // Gap between shape and innerShape

    private Integer version;            // Curve version, null if the Curve was
                                        // created with LightZone v4.1.3 or less

    private ClonePoint clonePt;         // The clone point, maybe null

    private int highlightPoint;         // A point for special drawing
    private int highlightSegment;       // A segment for special drawing
    private boolean highlightSegments;  // All segments should be special
    private boolean highlightGlobal;    // Draw everything special
    private boolean highlightInterior;  // Show the Shape interior

    public AbstractCurve() {
        points = new LinkedList<Point2D>();
        segments = new LinkedList<Shape>();
        version = 2;
        updateStrokes(NominalRadius);
        resetHighlights();
    }

    public boolean allowsAddRemovePoints() {
        // This is only false for spot Curves.  See EllipticCurve.
        return true;
    }

    public int addPoint(Point2D p) {
        points.add(p);
        updateShapes();
        return points.size() - 1;
    }

    public void insertPoint(int n, Point2D p) {
        points.add(n, p);
        updateShapes();
    }

    public void removePoint(int n) {
        points.remove(n);
        updateShapes();
    }

    public void movePoint(int n, Point2D p) {
        points.set(n, p);
        updateShapes();
    }

    public void translate(double dx, double dy) {
        for (Iterator<Point2D> i=points.iterator(); i.hasNext(); ) {
            Point2D p = (Point2D) i.next();
            double x = p.getX() + dx;
            double y = p.getY() + dy;
            p.setLocation(x, y);
        }
        if (clonePt != null) {
            clonePt.translate(dx, dy);
        }
        updateShapes();
    }

    public int getPointCount() {
        return points.size();
    }

    public Shape getShape() {
        return shape;
    }

    /**
     * Tell if this CubicCurve has enough structure to define a valid Shape,
     * meaning a Shape with a nontrivial interior.
     */
    public boolean isValidShape() {
        return ((shape != null) && (points.size() >= 3));
    }

    public Rectangle2D getPaintBounds() {
        Rectangle2D bounds = null;
        if (shape != null) {
            bounds = shape.getBounds();
            for (Iterator<Point2D> i=points.iterator(); i.hasNext(); ) {
                Point2D p = (Point2D) i.next();
                Rectangle2D r = getPointBounds(p);
                bounds.add(r);
            }
            if (clonePt != null) {
                bounds.add(clonePt.getPaintBounds());
            }
        }
        else if (points.size() == 1) {
            bounds = getPointBounds((Point2D) points.get(0));
            if (clonePt != null) {
                bounds.add(clonePt.getPaintBounds());
            }
        }
        else if (clonePt != null) {
            bounds = clonePt.getPaintBounds();
        }
        return bounds;
    }

    public boolean isOnCurve(Point2D p) {
        if (shape != null) {
            Shape thickShape = mouseHitStroke.createStrokedShape(shape);
            if (thickShape.contains(p)) {
                return true;
            }
        }
        return (getPointAt(p) >= 0);
    }

    public int getSegmentAt(Point2D p) {
        int n = 0;
        for (Iterator<Shape> i=segments.iterator(); i.hasNext(); ) {
            Shape thinSegment = (Shape) i.next();
            Shape thickSegment =
                mouseHitStroke.createStrokedShape(thinSegment);
            if (thickSegment.contains(p)) {
                return n;
            }
            n++;
        }
        return -1;
    }

    public int getPointAt(Point2D p) {
        for (int n=0; n<points.size(); n++) {
            Point2D q = (Point2D) points.get(n);
            if (p.distance(q) < 4 * radius) {
                return n;
            }
        }
        return -1;
    }

    public void setShowInnerShape(boolean show) {
        if (showInnerShape != show) {
            showInnerShape = show;
            if (showInnerShape) {
                updateInnerShape();
            }
            else {
                innerShape = null;
            }
        }
    }

    public boolean getShowInnerShape() {
        return showInnerShape;
    }

    public boolean isInnerShape(Point2D p) {
        if (showInnerShape) {
            Shape thickInnerShape =
                mouseHitStroke.createStrokedShape(innerShape);
            return thickInnerShape.contains(p);
        }
        return false;
    }

    public void setInnerShape(Point2D p) {
        if (p != null) {
            width = InnerShape.getWidth(shape, p);
        }
        else {
            width = InnerShape.getDefaultWidth(shape, width);
        }
        if (showInnerShape) {
            updateInnerShape();
        }
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
        if (showInnerShape) {
            updateInnerShape();
        }
    }

    private void updateInnerShape() {
        if (isValidShape()) {
            double size;
            float w = width;
            do {
                innerShape = InnerShape.getInnerShape(shape, w--);
                // Don't allow the innerShape be so small that
                // it can't be manipulated:
                Rectangle2D bounds = innerShape.getBounds2D();
                size = Math.min(bounds.getWidth(), bounds.getHeight());
            } while ((size < MinimumInnerSize) && (w >=1));
        }
        else {
            innerShape = null;
        }
    }

    public void setClonePoint(Point2D p) {
        if (clonePt == null) {
            clonePt = new ClonePoint(p);
        }
        clonePt.setPoint(p);
    }

    public Point2D getClonePoint() {
        return (clonePt != null) ? clonePt.getPoint() : null;
    }

    public boolean isClonePoint(Point2D p) {
        return (clonePt != null) ? clonePt.isAt(p) : false;
    }

    public void resetHighlights() {
        highlightPoint = -1;
        highlightSegment = -1;
        highlightSegments = false;
        highlightGlobal = false;
    }

    public void highlightPoint(int index) {
        resetHighlights();
        highlightPoint = index;
    }

    public void highlightSegment(int index) {
        resetHighlights();
        highlightSegment = index;
    }

    public void highlightAllSegments() {
        resetHighlights();
        highlightSegments = true;
    }

    public void highlightAll() {
        resetHighlights();
        highlightGlobal = true;
    }

    public void highlightInterior(boolean on) {
        highlightInterior = on;
    }

    public void paint(Graphics2D g, boolean showClonePoint) {
        saveGraphics(g);

        // The thicknesses of all our strokes depend on the scale at which
        // we are drawn.
        //
        // When the scale is less than one, then draw wider strokes.

        AffineTransform xform = g.getTransform();
        float scale = (float) Math.sqrt(Math.abs(xform.getDeterminant()));
        // Make the curve look magnified when the image is magnified:
//        scale = Math.min(scale, 1);
        float r = NominalRadius / scale;
        if (r != radius) {
            updateStrokes(r);
        }
        Rectangle clip = g.getClipBounds();
        if (shape != null) {
            Rectangle2D bounds = getPaintBounds();
            if (bounds.intersects(clip)) {
                if (highlightGlobal || highlightSegments) {
                    drawHighlight(g, shape);
                }
                else {
                    drawRegular(g, shape);
                }
                if (highlightGlobal) {
                    for (Iterator<Point2D> i=points.iterator(); i.hasNext(); ) {
                        Point2D p = (Point2D) i.next();
                        drawPointRegular(g, p);
                    }
                    if (innerShape != null) {
                        drawInner(g, innerShape);
                    }
                }
            }
        }
        else if (points.size() == 1) {
            Point2D p = (Point2D) points.iterator().next();
            drawPointRegular(g, p);
        }
        if (highlightPoint >= 0) {
            g.setStroke(highlightStroke);
            Point2D p = (Point2D) points.get(highlightPoint);
            drawPointHighlight(g, p);
        }
        if (highlightSegment >= 0) {
            Shape segment = (Shape) segments.get(highlightSegment);
            drawHighlight(g, segment);
        }
        if (highlightInterior) {
            g.setColor(InteriorColor);
            g.fill(shape);
        }
        if (showClonePoint) {
            clonePt.paint(g);
        }
        restoreGraphics(g);
    }

    public boolean matches(Curve c) {
        if (! (c.getClass().equals(getClass()))) {
            return false;
        }
        AbstractCurve curve = (AbstractCurve) c;
        LinkedList<Point2D> p = curve.points;
        if (points.size() != p.size()) {
            return false;
        }
        Iterator<Point2D> i = points.iterator();
        Iterator<Point2D> j = p.iterator();
        while (i.hasNext()) {
            if (! i.next().equals(j.next())) {
                return false;
            }
        }
        return true;
    }

    public Object clone() {
        AbstractCurve clone = null;
        try {
            clone = (AbstractCurve) super.clone();
            if (clonePt != null) {
                clone.clonePt = (ClonePoint) clonePt.clone();
            }
        } catch (CloneNotSupportedException e) {
            // Can't happen: all members are Cloneable
            System.err.println("Broken Curve.clone(): " + e.getMessage());
        }
        clone.segments = new LinkedList<Shape>();
        for (Iterator<Shape> i=segments.iterator(); i.hasNext(); ) {
            Shape segment = (Shape) i.next();
            if (segment instanceof Line2D) {
                segment = (Line2D) ((Line2D) segment).clone();
            }
            else if (segment instanceof QuadCurve2D) {
                segment = (QuadCurve2D) ((QuadCurve2D) segment).clone();
            }
            else if (segment instanceof CubicCurve2D) {
                segment = (CubicCurve2D) ((CubicCurve2D) segment).clone();
            }
            else {
                System.err.println("Curve.clone(): non-Cloneable segment");
            }
            clone.segments.add(segment);
        }
        clone.points = new LinkedList<Point2D>();
        for (Iterator<Point2D> i=points.iterator(); i.hasNext(); ) {
            Point2D p = (Point2D) i.next();
            p = (Point2D) p.clone();
            clone.points.add(p);
        }
        clone.updateShapes();

        return clone;
    }

    private void updateStrokes(float r) {
        radius = r;
        regularStroke = new BasicStroke(radius * .6f);
        backgroundStroke = new BasicStroke(radius);
        highlightStroke = new BasicStroke(
            radius * .6f,
            BasicStroke.CAP_ROUND,
            BasicStroke.JOIN_ROUND,
            radius * 2,
            new float[] {radius, radius},
            0f
        );
        mouseHitStroke = new BasicStroke(4 * radius);
    }

    private void drawHighlight(Graphics2D g, Shape shape) {
        setGraphicsBackground(g);
        g.draw(shape);
        setGraphicsHighlight(g);
        g.draw(shape);
    }

    private void drawRegular(Graphics2D g, Shape shape) {
        setGraphicsBackground(g);
        g.draw(shape);
        setGraphicsRegular(g);
        g.draw(shape);
    }

    private void drawInner(Graphics2D g, Shape shape) {
        setGraphicsBackground(g);
        g.draw(shape);
        setGraphicsInner(g);
        g.draw(shape);
    }

    private void setGraphicsBackground(Graphics2D g) {
        g.setColor(BackgroundColor);
        g.setStroke(backgroundStroke);
    }

    private void setGraphicsRegular(Graphics2D g) {
        g.setColor(ForegroundColor);
        g.setStroke(regularStroke);
    }

    private void setGraphicsInner(Graphics2D g) {
        g.setColor(InnerShapeColor);
        g.setStroke(regularStroke);
    }

    private void setGraphicsHighlight(Graphics2D g) {
        g.setColor(ForegroundColor);
        g.setStroke(highlightStroke);
    }

    private void drawPointRegular(Graphics2D g, Point2D p) {
        setGraphicsBackground(g);
        fillPoint(g, p);
        setGraphicsRegular(g);
        drawPoint(g, p);
    }

    private void drawPointHighlight(Graphics2D g, Point2D p) {
        setGraphicsBackground(g);
        fillPoint(g, p);
        setGraphicsHighlight(g);
        drawPoint(g, p);
    }

    private void drawPoint(Graphics2D g, Point2D p) {
        Shape shape = getPointShape(p);
        g.draw(shape);
    }

    private void fillPoint(Graphics2D g, Point2D p) {
        Shape shape = getPointShape(p);
        g.fill(shape);
    }

    private Shape getPointShape(Point2D p) {
        return new Arc2D.Double(
            p.getX() - 2 * radius,
            p.getY() - 2 * radius,
            4 * radius,
            4 * radius,
            0,
            360,
            Arc2D.CHORD
        );
    }

    private Rectangle2D getPointBounds(Point2D p) {
        Shape pointShape = getPointShape(p);
        Shape thickShape = backgroundStroke.createStrokedShape(pointShape);
        return thickShape.getBounds();
    }

    private void updateShapes() {
        updateShape();
        if (showInnerShape) {
            updateInnerShape();
        }
    }

    private Color oldColor;
    private Stroke oldStroke;

    private void saveGraphics(Graphics2D g) {
        oldColor = g.getColor();
        oldStroke = g.getStroke();
    }

    private void restoreGraphics(Graphics2D g) {
        g.setColor(oldColor);
        g.setStroke(oldStroke);
    }

    private final static String PointTag = "Point";
    private final static String WidthTag = "Width";
    private final static String VersionTag = "Version";
    private final static String CloneTag = "Clone";

    public void save(XmlNode node) {
        node.setAttribute(WidthTag, Float.toString(width));
        for (Iterator<Point2D> i=points.iterator(); i.hasNext(); ) {
            Point2D p = (Point2D) i.next();
            XmlNode pointNode = node.addChild(PointTag);
            pointNode.setAttribute("x", Double.toString(p.getX()));
            pointNode.setAttribute("y", Double.toString(p.getY()));
        }
        if (version != null) {
            node.setAttribute(VersionTag, Integer.toString(version));
        }
        if (clonePt != null) {
            node = node.addChild(CloneTag);
            clonePt.save(node);
        }
    }

    public void restore(XmlNode node) throws XMLException {
        try {
            width = Float.parseFloat(node.getAttribute(WidthTag));
        }
        catch (NumberFormatException e) {
            throw new XMLException("Invalid curve width", e);
        }
        points.clear();
        XmlNode[] children = node.getChildren();
        for (int n=0; n<children.length; n++) {
            XmlNode child = children[n];
            if (child.getName().equals(PointTag)) {
                try {
                    double x = Double.parseDouble(child.getAttribute("x"));
                    double y = Double.parseDouble(child.getAttribute("y"));
                    Point2D p = new Point2D.Double(x, y);
                    points.add(p);
                }
                catch (NumberFormatException e) {
                    throw new XMLException("Invalid curve coordinates", e);
                }
            }
        }
        updateShapes();

        if (node.hasAttribute(VersionTag)) {
            try {
                version = Integer.parseInt(node.getAttribute(VersionTag));
            }
            catch (NumberFormatException e) {
                throw new XMLException("Invalid curve version", e);
            }
        }
        else {
            version = null;
        }

        if (node.hasChild(CloneTag)) {
            clonePt = new ClonePoint();
            node = node.getChild(CloneTag);
            clonePt.restore(node);
        }
    }

    public Integer getVersion() {
        return version;
    }

    // Derived Curves must override this method, to update "shape" and
    // "segments" from the current value of "points":
    abstract void updateShape();

    // Begin passthrough implementation of Shape:

    public Rectangle getBounds() {
        return shape.getBounds();
    }

    public Rectangle2D getBounds2D() {
        return shape.getBounds2D();
    }

    public boolean contains(double x, double y) {
        return shape.contains(x, y);
    }

    public boolean contains(Point2D p) {
        return shape.contains(p);
    }

    public boolean intersects(double x, double y, double w, double h) {
        return shape.intersects(x, y, w, h);
    }

    public boolean intersects(Rectangle2D r) {
        return shape.intersects(r);
    }

    public boolean contains(double x, double y, double w, double h) {
        return shape.contains(x, y, w, h);
    }

    public boolean contains(Rectangle2D r) {
        return shape.contains(r);
    }

    public PathIterator getPathIterator(AffineTransform at) {
        return shape.getPathIterator(at);
    }

    public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return shape.getPathIterator(at, flatness);
    }

    // End passthrough implementation of Shape.
}
