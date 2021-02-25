/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2021-     Masahiro Kitagawa */

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
    private static double EllipticCloneX = Prefs.getDouble(EllipticCloneXTag, EllipticX);
    private static double EllipticCloneY = Prefs.getDouble(EllipticCloneYTag, 0);

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
        final var upperLeft = new Point2D.Double(
            center.getX() - EllipticX, center.getY() - EllipticY
        );
        final var lowerRight = new Point2D.Double(
            center.getX() + EllipticX, center.getY() + EllipticY
        );
        addPoint(upperLeft);
        addPoint(lowerRight);

        final var scale = Math.min(2 * Math.abs(EllipticX), 2 * Math.abs(EllipticY));
        setWidth((float) scale / 6);
        
        final var clonePt = new Point2D.Double(
            center.getX() + EllipticCloneX, center.getY() + EllipticCloneY
        );
        setClonePoint(clonePt);
    }

    @Override
    public void movePoint(int n, Point2D p) {
        // Preserve the center:
        var p1 = points.get(0);
        var p2 = points.get(1);
        final var centerX = (p1.getX() + p2.getX()) / 2;
        final var centerY = (p1.getY() + p2.getY()) / 2;

        // Update the control points to the complementary locations, to
        // preserve the center.
        final var sign = (n == 0) ? -1.0 : 1.0;
        final var dx = sign * Math.max(sign * (p.getX() - centerX), MIN_RADIUS);
        final var dy = sign * Math.max(sign * (p.getY() - centerY), MIN_RADIUS);
        final var q1 = new Point2D.Double(centerX + dx, centerY + dy);
        super.movePoint(n, q1);
        final var q2 = new Point2D.Double(centerX - dx, centerY - dy);
        super.movePoint(1 - n, q2);

        // Revise the default values.
        p1 = points.get(0);
        p2 = points.get(1);
        EllipticX = (p1.getX() - p2.getX()) / 2;
        EllipticY = (p1.getY() - p2.getY()) / 2;

        // Adjust the inner curve width automatically.
        if (! isManualWidthSet) {
            Rectangle2D bounds = shape.getBounds();
            final var width = bounds.getWidth();
            final var height = bounds.getHeight();
            final var scale = Math.min(width, height);
            setWidth((float) scale / 6);
            isManualWidthSet = false;
        }
        savePrefs();
    }

    @Override
    public void setClonePoint(Point2D p) {
        super.setClonePoint(p);

        // Revise the default clone point offsets.
        final var p1 = points.get(0);
        final var p2 = points.get(1);
        EllipticCloneX = p.getX() - (p1.getX() + p2.getX()) / 2;
        EllipticCloneY = p.getY() - (p1.getY() + p2.getY()) / 2;

        savePrefs();
    }

    @Override
    public void setInnerShape(Point2D p) {
        super.setInnerShape(p);
        isManualWidthSet = true;
    }

    /**
     * This is the only Curve implementation with a fixed number of points.
     */
    @Override
    public boolean allowsAddRemovePoints() {
        return false;
    }

    /**
     * This is the only Curve that is valid with only two points.
     */
    @Override
    public boolean isValidShape() {
        return (points.size() == 2);
    }

    /**
     * Turn off automatic inner curve width updates if this Curve has ever
     * been saved.
     */
    @Override
    public void restore(XmlNode node) throws XMLException {
        super.restore(node);
        isManualWidthSet = true;
    }

    @Override
    void updateShape() {
        if (points.size() != 2) {
            shape = new GeneralPath();
            return;
        }
        final var p1 = points.get(0);
        final var p2 = points.get(1);

        final var minX = Math.min(p1.getX(), p2.getX());
        final var minY = Math.min(p1.getY(), p2.getY());
        final var maxX = Math.max(p1.getX(), p2.getX());
        final var maxY = Math.max(p1.getY(), p2.getY());

        final var w = maxX - minX;
        final var h = maxY - minY;

        final var bounds = new Rectangle2D.Double(minX, minY, w, h);

        final Shape ne = new Arc2D.Double(bounds, -45, 180, Arc2D.OPEN);
        final Shape sw = new Arc2D.Double(bounds, 135, 180, Arc2D.OPEN);

        segments.clear();
        segments.add(ne);
        segments.add(sw);

        final var start = new float[6];
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
