/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.QuadCurve2D;

class Quadratic extends Polynomial {

    private double a, b, c;      // a + b*t + c*t^2

    // Define a Quadratic by its real roots:
    static Quadratic createFactored(double z0, double z1) {
        return new Quadratic(z0 * z1, - z0 - z1, 1.);
    }

    Quadratic(double a, double b, double c) {
        super(a, b, c);
        this.a = a;
        this.b = b;
        this.c = c;
    }

    Polynomial translate(double x) {
        return new Quadratic(
            a + b * x + c * x * x, b + 2 * c * x, c
        );
    }

    // Infer a QuadCurve2D (control points) from two Quadratics (polynomial
    // coefficients).  See QuadCurve2D.fillEqn().

    static Shape createShape(Polynomial x, Polynomial y) {
        if ((x.getDegree() != 2) || (y.getDegree() != 2)) {
            throw new IllegalArgumentException("Expected degree 2");
        }
        double xa = x.getCoeff(0);
        double xb = x.getCoeff(1);
        double xc = x.getCoeff(2);

        double ya = y.getCoeff(0);
        double yb = y.getCoeff(1);
        double yc = y.getCoeff(2);
        
        Point2D c1 = new Point2D.Double(
            xa,
            ya
        );
        Point2D cp = new Point2D.Double(
            (xb + 2 * xa) / 2,
            (yb + 2 * ya) / 2
        );
        Point2D c2 = new Point2D.Double(
            xa + xb + xc,
            ya + yb + yc
        );
        return new QuadCurve2D.Double(
            c1.getX(), c1.getY(),
            cp.getX(), cp.getY(),
            c2.getX(), c2.getY()
        );
    }
}
