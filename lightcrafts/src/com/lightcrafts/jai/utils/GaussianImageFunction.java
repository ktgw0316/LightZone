/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.jai.utils;

import javax.media.jai.ImageFunction;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Jun 2, 2005
 * Time: 4:58:10 PM
 * To change this template use File | Settings | File Templates.
 */

public class GaussianImageFunction implements ImageFunction {
    final double s;

    public GaussianImageFunction(double s) {
        this.s = s;
    }

    static double gauss(double x, double y, double s) {
        return (1/ (s * s * 2 * Math.PI)) * Math.exp(-(x * x + y * y) / (2 * s * s));
    }

    public void getElements(double startX, double startY,
                            double deltaX, double deltaY,
                            int countX, int countY,
                            int element, double[] real, double[] imag) {
        for (int i = 0; i < countX; i++) {
            double x = startX + deltaX * i;
            for (int j = 0; j < countY; j++) {
                double y = startY + deltaY * j;

                real[i + countX * j] = (float) gauss(x, y, s);
            }
        }
    }

    public void getElements(float startX, float startY,
                            float deltaX, float deltaY,
                            int countX, int countY,
                            int element, float[] real, float[] imag) {
        for (int i = 0; i < countX; i++) {
            double x = startX + deltaX * i;
            for (int j = 0; j < countY; j++) {
                double y = startY + deltaY * j;

                real[i + countX * j] = (float) gauss(x, y, s);
            }
        }
    }

    public int getNumElements() {
        return 1;
    }

    public boolean isComplex() {
        return false;
    }
}
