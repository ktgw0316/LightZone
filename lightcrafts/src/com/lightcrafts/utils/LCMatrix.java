/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.utils;

import Jama.Matrix;

public class LCMatrix extends Matrix {

    public LCMatrix(float[][] A) {
        super(A.length, A[0].length);

        final int m = A.length;
        final int n = A[0].length;

        for (final float[] row : A) {
            if (row.length != n) {
                throw new IllegalArgumentException("All rows must have the same length.");
            }
        }
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                set(i, j, A[i][j]);
            }
        }
    }

    static public float[][] getArrayFloat(Matrix mat) {
        final int m = mat.getRowDimension();
        final int n = mat.getColumnDimension();
        final double[][] A = mat.getArray();

        float FA[][] = new float[m][n];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < n; j++) {
                FA[i][j] = (float) A[i][j];
            }
        }
        return FA;
    }
}
