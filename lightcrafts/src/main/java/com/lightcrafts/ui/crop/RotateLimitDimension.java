/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.ui.crop;

import java.awt.geom.Dimension2D;

class RotateLimitDimension extends Dimension2D {

    double width;
    double height;

    RotateLimitDimension(double width, double height) {
        this.width = width;
        this.height = height;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public void setSize(double width, double height) {
        this.width = width;
        this.height = height;
    }
}
