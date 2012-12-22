/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.util.Iterator;

/** This class represents a closed sequence of cubic parametric curve segments
  * that smoothly interpolate a list of points.  It may be accessed as a
  * Shape by <code>getShape()</code>, and it can paint itself by
  * <code>paint()</code>
  * <p>
  * It is not a Component though, so that two CubicCurves may overlap.
  * <p>
  * The linear algebra at the core is copied from Tim Lambert,
  * http://www.cse.unsw.edu.au/~lambert/splines/source.html,
  * who transcribed the formulae from
  * "Two Dimensional Spline Interpolation Algorithms" by Helmuth Spath.
  */

public class CubicBezierCurve extends AbstractCurve {

    // Update "shape" and "segments" from the current value of "points":
    void updateShape() {
        if (points.size() < 2) {
            shape = null;
            segments.clear();
            return;
        }
        GeneralPath path = new GeneralPath();

        int[] xPts = new int[points.size()];
        int[] yPts = new int[points.size()];

        int count = 0;
        for (Iterator i=points.iterator(); i.hasNext(); ) {
            Point2D p = (Point2D) i.next();
            xPts[count] = (int) p.getX();
            yPts[count] = (int) p.getY();
            count++;
        }
        Cubic[] xPoly = calcNaturalCubic(xPts);
        Cubic[] yPoly = calcNaturalCubic(yPts);

        segments.clear();
        for (int n=0; n<count; n++) {
            Shape curve = Cubic.createShape(xPoly[n], yPoly[n]);
            segments.add(curve);
            path.append(curve, true);
        }
        path.closePath();
        shape = path;
    }

    /** Calculates the closed natural cubic spline that interpolates
     * x[0], x[1], ... x[n].  The first segment is returned as
     *
     *     C[0].a + C[0].b*u + C[0].c*u^2 + C[0].d*u^3
     *
     * on the interval u in [0,1).  The other segments are in C[1],C[2],...C[n].
     */

    private Cubic[] calcNaturalCubic(int[] x) {
        int n = x.length - 1;
        double[] w = new double[n+1];
        double[] v = new double[n+1];
        double[] y = new double[n+1];
        double[] D = new double[n+1];
        double z, F, G, H;
        int k;

        /* We solve the equation
        [4 1      1] [D[0]]   [3(x[1] - x[n])  ]
        |1 4 1     | |D[1]|   |3(x[2] - x[0])  |
        |  1 4 1   | | .  | = |      .         |
        |    ..... | | .  |   |      .         |
        |     1 4 1| | .  |   |3(x[n] - x[n-2])|
        [1      1 4] [D[n]]   [3(x[0] - x[n-1])]

        by decomposing the matrix into upper triangular and lower matrices
        and then back sustitution.  See Spath "Spline Algorithms for Curves
        and Surfaces" pp 19--21. The D[i] are the derivatives at the knots.
        */

        w[1] = v[1] = z = 1. / 4.;
        y[0] = z * 3 * (x[1] - x[n]);
        H = 4;
        F = 3 * (x[0] - x[n-1]);
        G = 1;
        for ( k = 1; k < n; k++) {
            v[k+1] = z = 1 / (4 - v[k]);
            w[k+1] = - z * w[k];
            y[k] = z * (3 * (x[k+1] - x[k-1]) - y[k-1]);
            H = H - G * w[k];
            F = F - G * y[k-1];
            G = - v[k] * G;
        }
        H = H - (G + 1) * (v[n] + w[n]);
        y[n] = F - (G + 1) * y[n-1];

        D[n] = y[n] / H;
        /* This equation is WRONG! in my copy of Spath: */
        D[n-1] = y[n-1] - (v[n] + w[n]) * D[n];
        for (k=n-2; k>=0; k--) {
            D[k] = y[k] - v[k+1] * D[k+1] - w[k+1] * D[n];
        }
        /* now compute the coefficients of the cubics */
        Cubic[] C = new Cubic[n+1];
        for (k=0; k<n; k++) {
            C[k] = new Cubic(
                (double) x[k],
                D[k],
                3 * (x[k+1] - x[k]) - 2 * D[k] - D[k+1],
                2 * (x[k] - x[k+1]) + D[k] + D[k+1]
            );
        }
        C[n] = new Cubic(
            (double) x[n],
            D[n],
            3 * (x[0] - x[n]) - 2*D[n] - D[0],
            2 * (x[n] - x[0]) + D[n] + D[0]
        );
        return C;
    }
}
