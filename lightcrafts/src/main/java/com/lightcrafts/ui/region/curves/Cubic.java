/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.CubicCurve2D;

class Cubic extends Polynomial {

    private double a, b, c, d;      // a + b*t + c*t^2 + d*t^3

    // Define a Cubic by its real roots:
    static Cubic createFactored(double z0, double z1, double z2) {
        return new Cubic(
            - z0 * z1 * z2, z0 * z1 + z1 * z2 + z2 * z0, - z0 - z1 - z2, 1.
        );
    }

    Cubic(double a, double b, double c, double d) {
        super(a, b, c, d);
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    Polynomial translate(double x) {
        return new Cubic(
            a + b * x + c * x * x + d * x * x * x,
            b + 2 * c * x + 3 * d * x * x,
            c + 3 * d * x,
            d
        );
    }

    // Infer a CubicCurve2D (control points) from two Cubics (polynomial
    // coefficients).  See CubicCurve2D.fillEqn().

    static Shape createShape(Polynomial x, Polynomial y) {
        if ((x.getDegree() != 3) || (y.getDegree() != 3)) {
            throw new IllegalArgumentException("Expected degree 3");
        }
        double xa = x.getCoeff(0);
        double xb = x.getCoeff(1);
        double xc = x.getCoeff(2);
        double xd = x.getCoeff(3);

        double ya = y.getCoeff(0);
        double yb = y.getCoeff(1);
        double yc = y.getCoeff(2);
        double yd = y.getCoeff(3);

        Point2D p1 = new Point2D.Double(
            xa,
            ya
        );
        Point2D cp1 = new Point2D.Double(
            (3 * xa + xb) / 3,
            (3 * ya + yb) / 3
        );
        Point2D cp2 = new Point2D.Double(
            (3 * xa + 2 * xb + xc) / 3,
            (3 * ya + 2 * yb + yc) / 3
        );
        Point2D p2 = new Point2D.Double(
            xa + xb + xc + xd,
            ya + yb + yc + yd
        );
        return new CubicCurve2D.Double(
            p1.getX(), p1.getY(),
            cp1.getX(), cp1.getY(),
            cp2.getX(), cp2.getY(),
            p2.getX(), p2.getY()
        );
    }
}
