/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

import java.util.ArrayList;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.*;

/** A CubicBasisSpline whose updateShape() actually generates a closed cubic
  * rational B-spline.  It uses Polynomial.createRationalShape(), which
  * performs flattening.
  */

public class CubicRationalSpline extends CubicBasisSpline {

//    private final static double[] Weights = new double[] {
//    };
    private final static double Weight = 1;

    // Update "shape" and "segments" from the current value of "points":
    void updateShape() {
        if (points.size() < 2) {
            shape = null;
            segments.clear();
            return;
        }
        // Pad out the control point list using wraparound,
        // to make a closed spline:

        ArrayList extraPoints = new ArrayList(points);
        extraPoints.add(0, points.get(points.size() - 2));
        extraPoints.add(1, points.get(points.size() - 1));
        extraPoints.add(points.get(0));
        extraPoints.add(points.get(1));

        // Rotate the padded points so generated segment indices will align
        // with indices in the points ArrayList:

        Object first = extraPoints.get(0);
        extraPoints.remove(0);
        extraPoints.add(first);

        GeneralPath path = new GeneralPath();

        int m = extraPoints.size();
        ensureBases(m);

        segments.clear();

        // Iterate over segments:
        for (int i=3; i<m-1; i++) {

            Polynomial xNum = new Polynomial(0);
            Polynomial yNum = new Polynomial(0);

            Polynomial xDen = new Polynomial(0);
            Polynomial yDen = new Polynomial(0);

            // Iterate over control points:
            for (int j=0; j<m; j++) {

                Point2D p = (Point2D) extraPoints.get(j);

                BasisFunction basis = (BasisFunction) Bases.get(j);

                double px = p.getX();
                double py = p.getY();

                Polynomial xSegNum = basis.getSegment(i).multiply(px * Weight);
                Polynomial ySegNum = basis.getSegment(i).multiply(py * Weight);

                Polynomial xSegDen = basis.getSegment(i).multiply(Weight);
                Polynomial ySegDen = basis.getSegment(i).multiply(Weight);

                xNum = xNum.add(xSegNum);
                yNum = yNum.add(ySegNum);

                xDen = xDen.add(xSegDen);
                yDen = yDen.add(ySegDen);
            }
            Shape curve = Polynomial.createRationalShape(
                xNum, xDen, yNum, yDen, i, i + 1, .1
            );
            segments.add(curve);
            path.append(curve, true);
        }
        path.closePath();
        shape = path;
    }
}
