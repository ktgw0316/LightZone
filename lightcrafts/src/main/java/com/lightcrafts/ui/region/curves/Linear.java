/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

class Linear extends Polynomial {

    private double a, b;      // a + b*t

    // Define a Linear by its real root:
    static Linear createFactored(double z0) {
        return new Linear(-z0, 1.);
    }

    Linear(double a, double b) {
        super(a, b);
        this.a = a;
        this.b = b;
    }

    Polynomial translate(double x) {
        return new Linear(a + b * x, b);
    }

    // Infer a Line2D (control points) from two Linears (polynomial
    // coefficients).  See Line2D.fillEqn().

    static Shape createShape(Polynomial x, Polynomial y) {
        if ((x.getDegree() != 1) || (y.getDegree() != 1)) {
            throw new IllegalArgumentException("Expected degree 1");
        }
        double xa = x.getCoeff(0);
        double xb = x.getCoeff(1);

        double ya = y.getCoeff(0);
        double yb = y.getCoeff(1);
        
        Point2D c1 = new Point2D.Double(xa, ya);
        Point2D c2 = new Point2D.Double(xa + xb, ya + yb);
        return new Line2D.Double(
            c1.getX(), c1.getY(),
            c2.getX(), c2.getY()
        );
    }
}

