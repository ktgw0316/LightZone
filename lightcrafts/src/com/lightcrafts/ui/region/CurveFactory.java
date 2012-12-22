/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region;

import com.lightcrafts.ui.region.curves.*;
import com.lightcrafts.utils.xml.XMLException;
import com.lightcrafts.utils.xml.XmlNode;

import java.awt.geom.Point2D;
import java.util.prefs.Preferences;

/** This factory generates Curve implementations from the curves package
  * according to a per-instance setting.
  * <p>
  * It also handles save/restore, managing mappings among integer curve
  * "types", Curve implementation classes, and String tags for XmlNode.
  */

public class CurveFactory {

    public final static int Polygon = 0;
    public final static int QuadraticBezier = 1;
    public final static int CubicBezier = 2;
    public final static int QuadraticBasis = 3;
    public final static int CubicBasis = 4;
    public final static int QuadraticRational = 5;
    public final static int CubicRational = 6;
    public final static int Elliptic = 7;

    // Make the curve type selection sticky:
    private final static String CurveTypeTag = "CurveType";
    private final static Preferences Prefs = Preferences.userRoot().node(
        "/com/lightcrafts/ui/region"
    );
    private int type = Prefs.getInt(CurveTypeTag, CubicBezier);

    void setCurveType(int type) {
        this.type = type;
        Prefs.putInt(CurveTypeTag, type);
    }

    int getCurveType() {
        return type;
    }

    Curve createCurve() {
        switch (type) {
            case Polygon:
                return new PolygonCurve();
            case QuadraticBezier:
                return null;
            case CubicBezier:
                return new CubicBezierCurve();
            case QuadraticBasis:
                return new QuadraticBasisSpline();
            case CubicBasis:
                return new CubicBasisSpline();
            case QuadraticRational:
                return new QuadraticRationalSpline();
            case CubicRational:
                return new CubicRationalSpline();
        }
        return null;
    }

    // Generate Curves with preset control points, for spot changes.
    Curve createSpotCurve(Point2D center) {
        return new EllipticCurve(center);
    }

    private final static String TypeTag = "Type";

    private static String getTag(int type) {
        switch (type) {
            case Polygon:
                return "Polygon";
            case QuadraticBezier:
                return null;
            case CubicBezier:
                return "CubicBezier";
            case QuadraticBasis:
                return "QuadraticBasis";
            case CubicBasis:
                return "CubicBasis";
            case QuadraticRational:
                return "QuadraticRational";
            case Elliptic:
                return "Elliptic";
        }
        return null;
    }

    private static String getTag(Curve curve) {
        if (curve instanceof PolygonCurve) {
            return getTag(Polygon);
        }
        if (curve instanceof CubicBezierCurve) {
            return getTag(CubicBezier);
        }
        if (curve instanceof QuadraticBasisSpline) {
            return getTag(QuadraticBasis);
        }
        if (curve instanceof CubicBasisSpline) {
            return getTag(CubicBasis);
        }
        if (curve instanceof QuadraticRationalSpline) {
            return getTag(QuadraticRational);
        }
        if (curve instanceof EllipticCurve) {
            return getTag(Elliptic);
        }
        return null;
    }

    static void save(Curve curve, XmlNode node) {
        String tag = getTag(curve);
        node.setAttribute(TypeTag, tag);
        curve.save(node);
    }

    static Curve restore(XmlNode node) throws XMLException {
        String tag = node.getAttribute(TypeTag);
        Curve curve = null;
        if (tag.equals(getTag(Polygon))) {
            curve = new PolygonCurve();
        }
        if (tag.equals(getTag(CubicBezier))) {
            curve = new CubicBezierCurve();
        }
        if (tag.equals(getTag(QuadraticBasis))) {
            curve = new QuadraticBasisSpline();
        }
        if (tag.equals(getTag(CubicBasis))) {
            curve = new CubicBasisSpline();
        }
        if (tag.equals(getTag(QuadraticRational))) {
            curve = new QuadraticRationalSpline();
        }
        if (tag.equals(getTag(Elliptic))) {
            curve = new EllipticCurve();
        }
        if (curve == null) {
            throw new XMLException("Unknown curve type: " + tag);
        }
        curve.restore(node);
        return curve;
    }
}
