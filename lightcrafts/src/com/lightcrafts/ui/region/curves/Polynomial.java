/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.region.curves;

import java.util.ArrayList;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.GeneralPath;

/** A simple model for Polynomials as a List of coefficients.  There is also
  * a static method to combine two Polynomials into a Shape, by using them as
  * parameter functions on the interval [0, 1].
  */

class Polynomial {

    // Coefficients, starting with the constant:
    private ArrayList coefs = new ArrayList();

    Polynomial(double a) {
        coefs.add(new Double(a));
    }

    Polynomial(double a, double b) {
        this(a);
        coefs.add(new Double(b));
    }

    Polynomial(double a, double b, double c) {
        this(a, b);
        coefs.add(new Double(c));
    }

    Polynomial(double a, double b, double c, double d) {
        this(a, b, c);
        coefs.add(new Double(d));
    }

    Polynomial(ArrayList coefs) {
        this.coefs.addAll(coefs);
    }

    int getDegree() {
        return coefs.size() - 1;
    }

    double getCoeff(int i) {
        return ((Double) coefs.get(i)).doubleValue();
    }

    Polynomial add(Polynomial poly) {
        ArrayList newCoefs = new ArrayList();
        int n = 0;
        while (n <= Math.min(getDegree(), poly.getDegree())) {
            double x = getCoeff(n);
            double y = poly.getCoeff(n++);
            newCoefs.add(new Double(x + y));
        }
        while (n <= getDegree()) {
            double y = getCoeff(n++);
            newCoefs.add(new Double(y));
        }
        while (n <= poly.getDegree()) {
            double y = poly.getCoeff(n++);
            newCoefs.add(new Double(y));
        }
        return new Polynomial(newCoefs);
    }

    Polynomial multiply(double x) {
        ArrayList newCoefs = new ArrayList();
        int n = 0;
        while (n < coefs.size()) {
            double c = getCoeff(n++);
            newCoefs.add(new Double(c * x));
        }
        return new Polynomial(newCoefs);
    }

    Polynomial divide(double x) {
        return multiply(1 / x);
    }

    Polynomial translate(double dt) {
        int degree = getDegree();
        Polynomial p;
        switch (degree) {
            case 0:
                return this;
            case 1:
                p = new Linear(getCoeff(0), getCoeff(1));
                break;
            case 2:
                p = new Quadratic(getCoeff(0), getCoeff(1), getCoeff(2));
                break;
            case 3:
                p = new Cubic(getCoeff(0), getCoeff(1), getCoeff(2), getCoeff(3));
                break;
            default:
                throw new IllegalArgumentException("Unsupported degree");
        }
        p = p.translate(dt);
        return p;
    }

    double evaluate(double t) {
        double sum = 0;
        double x = 1;
        for (int n=0; n<coefs.size(); n++) {
            double c = getCoeff(n);
            sum += c * x;
            x *= t;
        }
        return sum;
    }

    static Shape createShape(Polynomial p1, Polynomial p2) {
        if (p1.getDegree() != p2.getDegree()) {
            throw new IllegalArgumentException("Mismatched degrees");
        }
        int degree = p1.getDegree();
        switch (degree) {
            case 0:
                throw new IllegalArgumentException("Unsupported degree");
            case 1:
                return Linear.createShape(p1, p2);
            case 2:
                return Quadratic.createShape(p1, p2);
            case 3:
                return Cubic.createShape(p1, p2);
            default:
                throw new IllegalArgumentException("Unsupported degree");
        }
    }

    static Shape createRationalShape(
        Polynomial xNum, Polynomial xDen, Polynomial yNum, Polynomial yDen,
        double start, double end, double inc
    ) {
        double oldX = xNum.evaluate(start) / xDen.evaluate(start);
        double oldY = yNum.evaluate(start) / yDen.evaluate(start);

        GeneralPath path = new GeneralPath();
        for (double t=start; t<=end; t+=inc) {
            double newX = xNum.evaluate(t) / xDen.evaluate(t);
            double newY = yNum.evaluate(t) / yDen.evaluate(t);
            Line2D line = new Line2D.Double(oldX, oldY, newX, newY);
            path.append(line, true);
            oldX = newX;
            oldY = newY;
        }
        return path;
    }
}
