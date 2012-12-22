/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.utils;

/**
 * Created by IntelliJ IDEA.
 * User: fabio
 * Date: Mar 15, 2005
 * Time: 12:25:20 PM
 * To change this template use File | Settings | File Templates.
 */
public class ColorMatrix {
    public static final double RLUM = ColorScience.Wr;
    public static final double GLUM = ColorScience.Wg;
    public static final double BLUM = ColorScience.Wb;

/*
 *	matrixmult -
 *		multiply two matricies
 */

    static void matrixmult(double a[][], double b[][], double c[][]) {
        double temp[][] = new double[4][4];

        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++) {
                for (int k = 0; k < 4; k++)
                    temp[i][j] += b[i][k] * a[k][j];
            }
        for (int i = 0; i < 4; i++)
            for (int j = 0; j < 4; j++)
                c[i][j] = temp[i][j];
    }

/*
 *	lummat -
 *		make a luminance marix
 */
    public static void lummat(double mat[][])
    {
        double mmat[][] = {
            {RLUM, RLUM, RLUM, 0},
            {GLUM, GLUM, GLUM, 0},
            {BLUM, BLUM, BLUM, 0},
            {0,    0,    0,    1}
        };
        matrixmult(mmat,mat,mat);
    }

/*
 *	offsetmat -
 *		offset r, g, and b
 */
    public static void offsetmat(double mat[][], double roffset, double goffset, double boffset)
    {
        double mmat[][] = {
            {1, 0, 0, roffset},
            {0, 1, 0, goffset},
            {0, 0, 1, boffset},
            {0, 0, 0, 1}
        };
        matrixmult(mmat,mat,mat);
    }

/*
 *	xformpnt -
 *		transform a 3D point using a matrix
 */
    static double[] xformpnt(double matrix[][], double x, double y, double z) {
        double result[] = new double[3];
        result[0] = x * matrix[0][0] + y * matrix[1][0] + z * matrix[2][0] + matrix[3][0];
        result[1] = x * matrix[0][1] + y * matrix[1][1] + z * matrix[2][1] + matrix[3][1];
        result[2] = x * matrix[0][2] + y * matrix[1][2] + z * matrix[2][2] + matrix[3][2];
        return result;
    }

/*
 *	xrotate -
 *		rotate about the x (red) axis
 */
    static void xrotatemat(double mat[][], double rs, double rc) {
        double mmat[][] = {
            {1,  0,   0,  0},
            {0,  rc,  rs, 0},
            {0, -rs,  rc, 0},
            {0,  0,   0,  1}
        };
        matrixmult(mmat, mat, mat);
    }

/*
 *	yrotate -
 *		rotate about the y (green) axis
 */
    static void yrotatemat(double mat[][], double rs, double rc) {
        double mmat[][] = {
            {rc, 0, -rs, 0},
            {0,  1,  0,  0},
            {rs, 0,  rc, 0},
            {0,  0,  0,  1}
        };
        matrixmult(mmat, mat, mat);
    }

/*
 *	zrotate -
 *		rotate about the z (blue) axis
 */
    static void zrotatemat(double mat[][], double rs, double rc) {
        double mmat[][] = {
            { rc, rs, 0, 0},
            {-rs, rc, 0, 0},
            { 0,  0,  1, 0},
            { 0,  0,  0, 1}
        };
        matrixmult(mmat, mat, mat);
    }

/*
 *	zshear -
 *		shear z using x and y.
 */
    static void zshearmat(double mat[][], double dx, double dy) {
        double mmat[][] = {
            {1, 0, dx, 0},
            {0, 1, dy, 0},
            {0, 0, 1,  0},
            {0, 0, 0,  1}
        };
        matrixmult(mmat, mat, mat);
    }

/*
 *	huerotatemat -
 *		rotate the hue, while maintaining luminance.
 */
    public static void huerotatemat(double[][] mat, double rot) {
        double mag;
        double xrs, xrc;
        double yrs, yrc;
        double zrs, zrc;
        double zsx, zsy;
        double mmat[][] = {
            {1, 0, 0, 0},
            {0, 1, 0, 0},
            {0, 0, 1, 0},
            {0, 0, 0, 1}
        };

        /* rotate the grey vector into positive Z */
        mag = Math.sqrt(2.0);
        xrs = 1.0 / mag;
        xrc = 1.0 / mag;
        xrotatemat(mmat, xrs, xrc);
        mag = Math.sqrt(3.0);
        yrs = -1.0 / mag;
        yrc = Math.sqrt(2.0) / mag;
        yrotatemat(mmat, yrs, yrc);

        /* shear the space to make the luminance plane horizontal */
        double l[] = new double[3];
        l = xformpnt(mmat, RLUM, GLUM, BLUM);
        zsx = l[0] / l[2];
        zsy = l[1] / l[2];
        zshearmat(mmat, zsx, zsy);

        /* rotate the hue */
        zrs = Math.sin(rot * Math.PI / 180.0);
        zrc = Math.cos(rot * Math.PI / 180.0);
        zrotatemat(mmat, zrs, zrc);

        /* unshear the space to put the luminance plane back */
        zshearmat(mmat, -zsx, -zsy);

        /* rotate the grey vector back into place */
        yrotatemat(mmat, -yrs, yrc);
        xrotatemat(mmat, -xrs, xrc);

        matrixmult(mmat, mat, mat);
    }

/*
 *	saturatemat -
 *		make a saturation marix
 */
    public static void saturatemat(double mat[][], double sat) {
        double mmat[][] = new double[][] {
            {(1.0 - sat) * RLUM + sat, (1.0 - sat) * RLUM,       (1.0 - sat) * RLUM,       0},
            {(1.0 - sat) * GLUM,       (1.0 - sat) * GLUM + sat, (1.0 - sat) * GLUM,       0},
            {(1.0 - sat) * BLUM,       (1.0 - sat) * BLUM,       (1.0 - sat) * BLUM + sat, 0},
            {0,                        0,                        0,                        1}
        };
        matrixmult(mmat, mat, mat);
    }

/*
 *	cscalemat -
 *		make a color scale marix
 */
    public static void cscalemat(double mat[][], double rscale, double gscale, double bscale) {
        double mmat[][] = new double[][] {
            {rscale, 0,      0,      0},
            {0,      gscale, 0,      0},
            {0,      0,      bscale, 0},
            {0,      0,      0,      1}
        };
        matrixmult(mmat, mat, mat);
    }
}
