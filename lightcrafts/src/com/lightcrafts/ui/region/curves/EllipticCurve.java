/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

import com.lightcrafts.utils.xml.XmlNode;
import com.lightcrafts.utils.xml.XMLException;

import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.prefs.Preferences;

/**
 * An EllipticCurve is an AbstractCurve with exactly two control points, which
 * it interprets as defining an ellipse.  This is used to make Regions
 * for the spot tool.
 * <p>
 * This Curve has to have a default constructor, because of the way restore
 * works.  (Instantiates the Curve, then adds points.)  But it's not valid
 * unless it has exactly two points.
 */
public class EllipticCurve extends AbstractCurve {

    // Remember the radius and eccentricity of recent EllipticCurves so these
    // can be used for constructing default EllipticCurves:
    private final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/ui/region/curves"
    );
    // Default values for new EllipticCurves.  See EllipticCurve(Point2D).
    private final static String EllipticXTag = "SpotX";
    private final static String EllipticYTag = "SpotY";
    private final static String EllipticCloneXTag = "SpotCloneX";
    private final static String EllipticCloneYTag = "SpotCloneY";

    // Minimum value for width and height of EllipticCurves
    private static final double MIN_RADIUS = 0.5; 
    // Default width for new EllipticCurves.
    private static double EllipticX = Math.max(Prefs.getDouble(EllipticXTag, 30), MIN_RADIUS);
    // Default height for new EllipticCurves.
    private static double EllipticY = Math.max(Prefs.getDouble(EllipticYTag, 30), MIN_RADIUS);

    // Default clone point offsets for new EllipticCurves.
    private static double EllipticCloneX =
        Prefs.getDouble(EllipticCloneXTag, EllipticX);
    private static double EllipticCloneY =
        Prefs.getDouble(EllipticCloneYTag, 0);

    // If the width is directly manipulated (user interaction, restore), then
    // stop updating it automatically when control points change.
    private boolean isManualWidthSet;

    public EllipticCurve() {
        // Called during restore.
    }

    public EllipticCurve(Point2D upperLeft, Point2D lowerRight, Point2D clone) {
        addPoint(upperLeft);
        addPoint(lowerRight);
        setClonePoint(clone);
    }

    public EllipticCurve(Point2D center) {
        Point2D upperLeft = new Point2D.Double(
            center.getX() - EllipticX, center.getY() - EllipticY
        );
        Point2D lowerRight = new Point2D.Double(
            center.getX() + EllipticX, center.getY() + EllipticY
        );
        addPoint(upperLeft);
        addPoint(lowerRight);

        double scale = Math.min(
            2 * Math.abs(EllipticX), 2 * Math.abs(EllipticY)
        );
        setWidth((float) scale / 6);
        
        Point2D clonePt = new Point2D.Double(
            center.getX() + EllipticCloneX, center.getY() + EllipticCloneY
        );
        setClonePoint(clonePt);
    }

    public void movePoint(int n, Point2D p) {
        // Preserve the center:
        Point2D p1 = (Point2D) points.get(0);
        Point2D p2 = (Point2D) points.get(1);
        double centerX = (p1.getX() + p2.getX()) / 2;
        double centerY = (p1.getY() + p2.getY()) / 2;

        // Update the control points to the complementary locations, to
        // preserve the center.
        double dx = Math.max(p.getX() - centerX, MIN_RADIUS);
        double dy = Math.max(p.getY() - centerY, MIN_RADIUS);
        Point2D q1 = new Point2D.Double(centerX + dx, centerY + dy);
        super.movePoint(n, q1);
        Point2D q2 = new Point2D.Double(centerX - dx, centerY - dy);
        super.movePoint(1 - n, q2);

        // Revise the default values.
        p1 = (Point2D) points.get(0);
        p2 = (Point2D) points.get(1);
        EllipticX = (p1.getX() - p2.getX()) / 2;
        EllipticY = (p1.getY() - p2.getY()) / 2;

        // Adjust the inner curve width automatically.
        if (! isManualWidthSet) {
            Rectangle2D bounds = shape.getBounds();
            double width = bounds.getWidth();
            double height = bounds.getHeight();
            double scale = Math.min(width, height);
            setWidth((float) scale / 6);
            isManualWidthSet = false;
        }
        savePrefs();
    }

    public void setClonePoint(Point2D p) {
        super.setClonePoint(p);

        // Revise the default clone point offsets.
        Point2D p1 = (Point2D) points.get(0);
        Point2D p2 = (Point2D) points.get(1);
        EllipticCloneX = p.getX() - (p1.getX() + p2.getX()) / 2;
        EllipticCloneY = p.getY() - (p1.getY() + p2.getY()) / 2;

        savePrefs();
    }

    public void setInnerShape(Point2D p) {
        super.setInnerShape(p);
        isManualWidthSet = true;
    }

    /**
     * This is the only Curve implementation with a fixed number of points.
     */
    public boolean allowsAddRemovePoints() {
        return false;
    }

    /**
     * This is the only Curve that is valid with only two points.
     */
    public boolean isValidShape() {
        return (points.size() == 2);
    }

    /**
     * Turn off automatic inner curve width updates if this Curve has ever
     * been saved.
     */
    public void restore(XmlNode node) throws XMLException {
        super.restore(node);
        isManualWidthSet = true;
    }

    void updateShape() {
        if (points.size() != 2) {
            shape = new GeneralPath();
            return;
        }
        Point2D p1 = (Point2D) points.get(0);
        Point2D p2 = (Point2D) points.get(1);

        double minX = Math.min(p1.getX(), p2.getX());
        double minY = Math.min(p1.getY(), p2.getY());
        double maxX = Math.max(p1.getX(), p2.getX());
        double maxY = Math.max(p1.getY(), p2.getY());

        double x = minX;
        double y = minY;
        double w = maxX - minX;
        double h = maxY - minY;

        Rectangle2D bounds = new Rectangle2D.Double(x, y, w, h);

        Shape ne = new Arc2D.Double(bounds, -45, 180, Arc2D.OPEN);
        Shape sw = new Arc2D.Double(bounds, 135, 180, Arc2D.OPEN);

        segments.clear();
        segments.add(ne);
        segments.add(sw);

        float[] start = new float[6];
        ne.getPathIterator(null).currentSegment(start);

        GeneralPath path = new GeneralPath();
        path.moveTo(start[0], start[1]);
        path.append(ne, true);
        path.append(sw, true);
        path.closePath();

        shape = path;
    }

    private static void savePrefs() {
        Prefs.putDouble(EllipticXTag, EllipticX);
        Prefs.putDouble(EllipticYTag, EllipticY);
        Prefs.putDouble(EllipticCloneXTag, EllipticCloneX);
        Prefs.putDouble(EllipticCloneYTag, EllipticCloneY);
    }
}
