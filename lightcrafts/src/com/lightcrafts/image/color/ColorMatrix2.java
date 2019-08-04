/* Copyright (C) 2005-2011 Fabio Riccardi */

package com.lightcrafts.image.color;

/**
 * Copyright (C) 2007 Light Crafts, Inc.
 * User: fabio
 * Date: May 23, 2007
 * Time: 10:45:38 PM
 */
public class ColorMatrix2 {
    public static final float RLUM = ColorScience.Wr;
    public static final float GLUM = ColorScience.Wg;
    public static final float BLUM = ColorScience.Wb;

    /*
    *	matrixmult -
    *		multiply two matricies
    */
    static void matrixmult(float[][] a, float[][] b, float[][] c) {
        int x, y;
        float[][] temp = new float[4][4];

        for (y = 0; y < 4; y++)
            for (x = 0; x < 4; x++) {
                temp[y][x] = b[y][0] * a[0][x]
                             + b[y][1] * a[1][x]
                             + b[y][2] * a[2][x]
                             + b[y][3] * a[3][x];
            }
        for (y = 0; y < 4; y++)
            for (x = 0; x < 4; x++)
                c[y][x] = temp[y][x];
    }

    /*
    *	xformpnt -
    *		transform a 3D point using a matrix
    */
    public static void xformpnt(float[][] matrix, float[] in, float[] out) {
        out[0] = in[0] * matrix[0][0] + in[1] * matrix[1][0] + in[2] * matrix[2][0] + matrix[3][0];
        out[1] = in[0] * matrix[0][1] + in[1] * matrix[1][1] + in[2] * matrix[2][1] + matrix[3][1];
        out[2] = in[0] * matrix[0][2] + in[1] * matrix[1][2] + in[2] * matrix[2][2] + matrix[3][2];
    }

    /*
    *	cscalemat -
    *		make a color scale marix
    */
    public static void cscalemat(float[][] mat, float rscale, float gscale, float bscale) {
        float[][] mmat = new float[4][4];

        mmat[0][0] = rscale;
        mmat[0][1] = 0;
        mmat[0][2] = 0;
        mmat[0][3] = 0;

        mmat[1][0] = 0;
        mmat[1][1] = gscale;
        mmat[1][2] = 0;
        mmat[1][3] = 0;


        mmat[2][0] = 0;
        mmat[2][1] = 0;
        mmat[2][2] = bscale;
        mmat[2][3] = 0;

        mmat[3][0] = 0;
        mmat[3][1] = 0;
        mmat[3][2] = 0;
        mmat[3][3] = 1;
        matrixmult(mmat, mat, mat);
    }

    /*
    *	lummat -
    *		make a luminance marix
    */
    public static void lummat(float[][] mat) {
        float[][] mmat = new float[4][4];
        float rwgt, gwgt, bwgt;

        rwgt = RLUM;
        gwgt = GLUM;
        bwgt = BLUM;
        mmat[0][0] = rwgt;
        mmat[0][1] = rwgt;
        mmat[0][2] = rwgt;
        mmat[0][3] = 0;

        mmat[1][0] = gwgt;
        mmat[1][1] = gwgt;
        mmat[1][2] = gwgt;
        mmat[1][3] = 0;

        mmat[2][0] = bwgt;
        mmat[2][1] = bwgt;
        mmat[2][2] = bwgt;
        mmat[2][3] = 0;

        mmat[3][0] = 0;
        mmat[3][1] = 0;
        mmat[3][2] = 0;
        mmat[3][3] = 1;
        matrixmult(mmat, mat, mat);
    }

    /*
    *	saturatemat -
    *		make a saturation marix
    */
    public static void saturatemat(float[][] mat, float sat) {
        float[][] mmat = new float[4][4];
        float a, b, c, d, e, f, g, h, i;
        float rwgt, gwgt, bwgt;

        rwgt = RLUM;
        gwgt = GLUM;
        bwgt = BLUM;

        a = (1 - sat) * rwgt + sat;
        b = (1 - sat) * rwgt;
        c = (1 - sat) * rwgt;
        d = (1 - sat) * gwgt;
        e = (1 - sat) * gwgt + sat;
        f = (1 - sat) * gwgt;
        g = (1 - sat) * bwgt;
        h = (1 - sat) * bwgt;
        i = (1 - sat) * bwgt + sat;
        mmat[0][0] = a;
        mmat[0][1] = b;
        mmat[0][2] = c;
        mmat[0][3] = 0;

        mmat[1][0] = d;
        mmat[1][1] = e;
        mmat[1][2] = f;
        mmat[1][3] = 0;

        mmat[2][0] = g;
        mmat[2][1] = h;
        mmat[2][2] = i;
        mmat[2][3] = 0;

        mmat[3][0] = 0;
        mmat[3][1] = 0;
        mmat[3][2] = 0;
        mmat[3][3] = 1;
        matrixmult(mmat, mat, mat);
    }

    /*
    *	offsetmat -
    *		offset r, g, and b
    */
    public static void offsetmat(float[][] mat, float roffset, float goffset, float boffset) {
        float[][] mmat = new float[4][4];

        mmat[0][0] = 1;
        mmat[0][1] = 0;
        mmat[0][2] = 0;
        mmat[0][3] = 0;

        mmat[1][0] = 0;
        mmat[1][1] = 1;
        mmat[1][2] = 0;
        mmat[1][3] = 0;

        mmat[2][0] = 0;
        mmat[2][1] = 0;
        mmat[2][2] = 1;
        mmat[2][3] = 0;

        mmat[3][0] = roffset;
        mmat[3][1] = goffset;
        mmat[3][2] = boffset;
        mmat[3][3] = 1;
        matrixmult(mmat, mat, mat);
    }

    /*
    *	xrotate -
    *		rotate about the x (red) axis
    */
    public static void xrotatemat(float[][] mat, float rs, float rc) {
        float[][] mmat = new float[4][4];

        mmat[0][0] = 1;
        mmat[0][1] = 0;
        mmat[0][2] = 0;
        mmat[0][3] = 0;

        mmat[1][0] = 0;
        mmat[1][1] = rc;
        mmat[1][2] = rs;
        mmat[1][3] = 0;

        mmat[2][0] = 0;
        mmat[2][1] = -rs;
        mmat[2][2] = rc;
        mmat[2][3] = 0;

        mmat[3][0] = 0;
        mmat[3][1] = 0;
        mmat[3][2] = 0;
        mmat[3][3] = 1;
        matrixmult(mmat, mat, mat);
    }

    /*
    *	yrotate -
    *		rotate about the y (green) axis
    */
    public static void yrotatemat(float[][] mat, float rs, float rc) {
        float[][] mmat = new float[4][4];

        mmat[0][0] = rc;
        mmat[0][1] = 0;
        mmat[0][2] = -rs;
        mmat[0][3] = 0;

        mmat[1][0] = 0;
        mmat[1][1] = 1;
        mmat[1][2] = 0;
        mmat[1][3] = 0;

        mmat[2][0] = rs;
        mmat[2][1] = 0;
        mmat[2][2] = rc;
        mmat[2][3] = 0;

        mmat[3][0] = 0;
        mmat[3][1] = 0;
        mmat[3][2] = 0;
        mmat[3][3] = 1;
        matrixmult(mmat, mat, mat);
    }

    /*
    *	zrotate -
    *		rotate about the z (blue) axis
    */
    public static void zrotatemat(float[][] mat, float rs, float rc) {
        float[][] mmat = new float[4][4];

        mmat[0][0] = rc;
        mmat[0][1] = rs;
        mmat[0][2] = 0;
        mmat[0][3] = 0;

        mmat[1][0] = -rs;
        mmat[1][1] = rc;
        mmat[1][2] = 0;
        mmat[1][3] = 0;

        mmat[2][0] = 0;
        mmat[2][1] = 0;
        mmat[2][2] = 1;
        mmat[2][3] = 0;

        mmat[3][0] = 0;
        mmat[3][1] = 0;
        mmat[3][2] = 0;
        mmat[3][3] = 1;
        matrixmult(mmat, mat, mat);
    }

    /*
    *	zshear -
    *		shear z using x and y.
    */
    public static void zshearmat(float[][] mat, float dx, float dy) {
        float[][] mmat = new float[4][4];

        mmat[0][0] = 1;
        mmat[0][1] = 0;
        mmat[0][2] = dx;
        mmat[0][3] = 0;

        mmat[1][0] = 0;
        mmat[1][1] = 1;
        mmat[1][2] = dy;
        mmat[1][3] = 0;

        mmat[2][0] = 0;
        mmat[2][1] = 0;
        mmat[2][2] = 1;
        mmat[2][3] = 0;

        mmat[3][0] = 0;
        mmat[3][1] = 0;
        mmat[3][2] = 0;
        mmat[3][3] = 1;
        matrixmult(mmat, mat, mat);
    }

    /*
    *	simplehuerotatemat -
    *		simple hue rotation. This changes luminance
    */
    public static void simplehuerotatemat(float[][] mat, float rot) {
        float mag;
        float xrs, xrc;
        float yrs, yrc;
        float zrs, zrc;

/* rotate the grey vector into positive Z */
        mag = (float) Math.sqrt(2.0);
        xrs = 1 / mag;
        xrc = 1 / mag;
        xrotatemat(mat, xrs, xrc);

        mag = (float) Math.sqrt(3.0);
        yrs = -1 / mag;
        yrc = (float) Math.sqrt(2.0) / mag;
        yrotatemat(mat, yrs, yrc);

/* rotate the hue */
        zrs = (float) Math.sin(rot * Math.PI / 180.0);
        zrc = (float) Math.cos(rot * Math.PI / 180.0);
        zrotatemat(mat, zrs, zrc);

/* rotate the grey vector back into place */
        yrotatemat(mat, -yrs, yrc);
        xrotatemat(mat, -xrs, xrc);
    }

    /*
    *	huerotatemat -
    *		rotate the hue, while maintaining luminance.
    */
    public static void huerotatemat(float[][] mat, float rot) {
        float[][] mmat = new float[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1}
        };
        float mag;
        float[] l = new float[3];
        float xrs, xrc;
        float yrs, yrc;
        float zrs, zrc;
        float zsx, zsy;

/* rotate the grey vector into positive Z */
        mag = (float) Math.sqrt(2.0);
        xrs = 1 / mag;
        xrc = 1 / mag;
        xrotatemat(mmat, xrs, xrc);
        mag = (float) Math.sqrt(3.0);
        yrs = -1 / mag;
        yrc = (float) Math.sqrt(2.0) / mag;
        yrotatemat(mmat, yrs, yrc);

/* shear the space to make the luminance plane horizontal */
        xformpnt(mmat, new float[]{RLUM, GLUM, BLUM}, l);
        zsx = l[0] / l[2];
        zsy = l[1] / l[2];
        zshearmat(mmat, zsx, zsy);

/* rotate the hue */
        zrs = (float) Math.sin(rot * Math.PI / 180.0);
        zrc = (float) Math.cos(rot * Math.PI / 180.0);
        zrotatemat(mmat, zrs, zrc);

/* unshear the space to put the luminance plane back */
        zshearmat(mmat, -zsx, -zsy);

/* rotate the grey vector back into place */
        yrotatemat(mmat, -yrs, yrc);
        xrotatemat(mmat, -xrs, xrc);

        matrixmult(mmat, mat, mat);
    }
}
