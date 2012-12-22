/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

import java.awt.*;
import java.awt.geom.*;
import java.util.*;

/** Handle the logic of inner shapes for AbstractCurve:
  * <ul>
  * <li>1) derive a width number from an outer Shape and a Point2D;</li>
  * <li>2) derive an inner Shape from an outer Shape and a width;</li>
  * <li>3) and pick a default width number for a given outer Shape</li>
  * </ul>
  * in such a way that it is guaranteed that the inner Shape from (2) will pass
  * through the Point2D in (1), and that the default inner Shape from (3)
  * will be correctly proportioned for easy of use.
  */

class InnerShape {

    // Here is a short-term cache of precomputed blurs for a single, static
    // Shape.  The idea is that mouse interaction may thrash getWidth() but
    // will only do so on a single thread and only for one Shape at a time:

    private static Shape Shape;
    private static ArrayList Blurs = new ArrayList();

    // Get a width such that an inner curve inset by that amount will pass
    // through the given point.
    static float getWidth(Shape outer, Point2D p) {
        if (! outer.contains(p)) {
            return 0;
        }
        Rectangle bounds = outer.getBounds();
        int maxDim = Math.max(bounds.width, bounds.height) / 2;

        if (Shape != outer) {
            Blurs.clear();
            Shape = outer;
        }
        for (int w=0; w<maxDim; w++) {
            if (Blurs.size() <= w) {
                Stroke stroke = new BasicStroke(
                    2 * (w + 1), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND
                );
                Shape blur = stroke.createStrokedShape(outer);
                Blurs.add(blur);
            }
            Shape blur = (Shape) Blurs.get(w);
            if (blur.contains(p)) {
                return w;
            }
        }
        // This should never happen:
        return 0;
    }

    // Get an inner curve inset from the given curve by the given width.
    static Shape getInnerShape(Shape outer, float width) {
        Stroke stroke = new BasicStroke(
            2 * width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND
        );
        Shape thickShape = stroke.createStrokedShape(outer);
        Area inner = new Area(outer);
        inner.subtract(new Area(thickShape));
        return getFilteredShape(inner);
    }

    // Get a natural value for the inner curve width, based on the Shape
    // of the curve.
    static float getDefaultWidth(Shape outer) {
        float thickness = getThickness(outer);
        return thickness * .2f;
    }

    // Get a natural value for the inner curve width, and use an estimate
    // of where that width should lie to speed the calculation.
    static float getDefaultWidth(Shape outer, float width) {
        float thickness = width / .2f;
        thickness = getThickness(outer, thickness);
        return thickness * .2f;
    }

    // "Thickness" is the greatest normal distance between any point inside
    // the Shape and the Shape's boundary.  The returned value has uncertainty
    // +/- 1.
    private static float getThickness(Shape shape) {
        return getThickness(shape, 0, getThicknessBound(shape));
    }

    // Compute thickness assuming it lies within the given range by bifurcation.
    private static float getThickness(Shape shape, float min, float max) {
        float thickness = (max + min) / 2;
        while ((max - min) > 2) {
            Shape inner = InnerShape.getInnerShape(shape, thickness);
            double size = getCharacteristicSize(inner);

            if (size > 0) {
                min = thickness;
            }
            else {
                max = thickness;
            }
            thickness = (max + min) / 2;
        }
        return thickness;
    }

    // Use an initial guess at the thickness to pick optimized bounds for
    // the bifurcation method.
    private static float getThickness(Shape shape, float thickness) {
        float min = 0;
        float max = getThicknessBound(shape);

        // Improve max and a min by exploring outwards from the guess:
        Shape inner = InnerShape.getInnerShape(shape, thickness);
        double size = getCharacteristicSize(inner);

        if (size > 0) {
            min = thickness;
            float delta = 2;
            while ((size > 0) && (thickness + delta < max)) {
                thickness += delta;
                inner = InnerShape.getInnerShape(shape, thickness);
                size = getCharacteristicSize(inner);
                delta *= 2;
            }
            max = thickness;
        }
        else {
            max = thickness;
            float delta = 2;
            while ((size == 0) && (thickness - delta > min)) {
                thickness -= delta;
                inner = InnerShape.getInnerShape(shape, thickness);
                size = getCharacteristicSize(inner);
                delta *= 2;
            }
            min = thickness;
        }
        // Now, with the improved max and min, follow the bifurcation procedure:
        return getThickness(shape, min, max);
    }

    // An upper bound on the thickness of the given Shape.
    private static float getThicknessBound(Shape shape) {
        return getCharacteristicSize(shape) / 2;
    }

    // The height or width of the bounding box of the given Shape,
    // whichever is smaller.
    private static float getCharacteristicSize(Shape shape) {
        Rectangle2D bounds = shape.getBounds2D();
        return (float) Math.min(bounds.getWidth(), bounds.getHeight());
    }

    // Decompose the given Shape into connected components, then discard
    // any connected component whose bounds are not empty.
    private static Shape getFilteredShape(Shape shape) {
        Set comps = getConnectedComponents(shape);
        GeneralPath filtered = new GeneralPath();
        for (Iterator i=comps.iterator(); i.hasNext(); ) {
            Shape comp =(Shape) i.next();
            Rectangle2D compRect = comp.getBounds2D();
            int compSize = (int) Math.min(compRect.getWidth(), compRect.getHeight());
            if (compSize > 0) {
                filtered.append(comp, false);
            }
        }
        return filtered;
    }

    // Return a Set of Shapes, comprising the connected components of
    // a given Shape.
    private static Set getConnectedComponents(Shape shape) {
        PathIterator i = shape.getPathIterator(new AffineTransform());
        HashSet components = new HashSet();
        if (i.isDone()) {
            return components;
        }
        float[] pts = new float[6];
        GeneralPath component = new GeneralPath();
        int type = i.currentSegment(pts);
        assert type == PathIterator.SEG_MOVETO;
        component.moveTo(pts[0], pts[1]);
        i.next();
        while (! i.isDone()) {
            type = i.currentSegment(pts);
            if (type == PathIterator.SEG_MOVETO) {
                components.add(component);
                component = new GeneralPath();
                component.moveTo(pts[0], pts[1]);
            }
            else if (type == PathIterator.SEG_LINETO) {
                component.lineTo(pts[0], pts[1]);
            }
            else if (type == PathIterator.SEG_QUADTO) {
                component.quadTo(pts[0], pts[1], pts[2], pts[3]);
            }
            else if (type == PathIterator.SEG_CUBICTO) {
                component.curveTo(pts[0], pts[1], pts[2], pts[3], pts[4], pts[5]);
            }
            else if (type == PathIterator.SEG_CLOSE) {
                component.closePath();
            }
            i.next();
        }
        components.add(component);

        return components;
    }

    private final static String[] PathTypeNames = new String[] {
        "MOVE", "LINE", "QUAD", "CUBIC", "CLOSE"
    };

    // For development and debugging, show the structure of a
    // Shape's PathIterator.
    private static String segmentsToString(Shape shape) {
        StringBuffer buffer = new StringBuffer();
        PathIterator i = shape.getPathIterator(new AffineTransform());
        float[] pts = new float[6];
        while (! i.isDone()) {
            int type = i.currentSegment(pts);
            buffer.append(PathTypeNames[type]);
            buffer.append(" ");
            i.next();
        }
        return buffer.toString();
    }
}
