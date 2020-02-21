/* Copyright (C) 2005-2011 Fabio Riccardi */
/* Copyright (C) 2018-     Masahiro Kitagawa */

package com.lightcrafts.ui.region.curves;

import lombok.val;

import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.ArrayList;

/** A spline whose updateShape() actually generates a closed
  * rational B-spline.  It uses Polynomial.createRationalShape(), which
  * performs flattening.
  */

public abstract class RationalSpline extends AbstractCurve {

    static ArrayList<BasisFunction> Bases = new ArrayList<BasisFunction>();

    private final static double Weight = 1;

    abstract void ensureBases(int count);

    // Update "shape" and "segments" from the current value of "points":
    @Override
    void updateShape() {
        if (points.size() < 2) {
            shape = null;
            segments.clear();
            return;
        }

        // Pad out the control point list using wraparound,
        // to make a closed spline:
        val extraPoints = new ArrayList<Point2D>(points);
        extraPoints.add(0, points.get(points.size() - 2));
        extraPoints.add(1, points.get(points.size() - 1));
        extraPoints.add(points.get(0));
        extraPoints.add(points.get(1));

        // Rotate the padded points so generated segment indices will align
        // with indices in the points ArrayList:

        val first = extraPoints.get(0);
        extraPoints.remove(0);
        extraPoints.add(first);

        val path = new GeneralPath();

        val m = extraPoints.size();
        ensureBases(m);

        segments.clear();

        // Iterate over segments:
        for (int i = 3; i < m - 1; i++) {
            Polynomial xNum = new Polynomial(0);
            Polynomial yNum = new Polynomial(0);

            Polynomial xDen = new Polynomial(0);
            Polynomial yDen = new Polynomial(0);

            // Iterate over control points:
            for (int j = 0; j < m; j++) {
                val p = extraPoints.get(j);
                val px = p.getX();
                val py = p.getY();

                val basis = Bases.get(j);

                Polynomial xSegNum = basis.getSegment(i).multiply(px * Weight);
                Polynomial ySegNum = basis.getSegment(i).multiply(py * Weight);

                Polynomial xSegDen = basis.getSegment(i).multiply(Weight);
                Polynomial ySegDen = basis.getSegment(i).multiply(Weight);

                xNum = xNum.add(xSegNum);
                yNum = yNum.add(ySegNum);

                xDen = xDen.add(xSegDen);
                yDen = yDen.add(ySegDen);
            }
            val curve = Polynomial.createRationalShape(
                xNum, xDen, yNum, yDen, i, i + 1, .1
            );
            segments.add(curve);
            path.append(curve, true);
        }
        path.closePath();
        shape = path;
    }
}
