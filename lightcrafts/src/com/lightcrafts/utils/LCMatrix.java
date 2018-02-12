/* Copyright (C) 2017-     Masahiro Kitagawa */

package com.lightcrafts.utils;

import lombok.val;
import org.ejml.data.FMatrixRMaj;
import org.ejml.simple.SimpleMatrix;

public class LCMatrix extends SimpleMatrix {
    public LCMatrix(LCMatrix A) {
        super(A);
    }

    public LCMatrix(int numRows, int numCols) {
        setMatrix(new FMatrixRMaj(numRows, numCols));
    }

    public LCMatrix(float[][] data) {
        setMatrix(new FMatrixRMaj(data));
    }

    public LCMatrix(int numRows, int numCols, float[] data) {
        setMatrix(FMatrixRMaj.wrap(numRows, numCols, data));
    }

    static public double[][] getArrayDouble(SimpleMatrix A) {
        val mat = A.getMatrix();
        val m = mat.getNumRows();
        val n = mat.getNumCols();
        val type = mat.getType();

        val array = new double[m][n];
        switch( type ) {
            case DDRM:
                val doubleData = A.getDDRM().getData();
                for (int i = 0; i < m; i++) {
                    System.arraycopy(doubleData, i * n, array[i], 0, n);
                }
                break;
            case FDRM:
                val floatData = A.getFDRM().getData();
                for (int i = 0, idx = 0; i < m; ++i) {
                    for (int j = 0; j < n; ++j, ++idx) {
                        array[i][j] = floatData[idx];
                    }
                }
                break;
            default:
                throw new RuntimeException("Unsupported matrix type");
        }
        return array;
    }

    static public float[][] getArrayFloat(SimpleMatrix A) {
        val mat = A.getMatrix();
        val m = mat.getNumRows();
        val n = mat.getNumCols();
        val type = mat.getType();

        val array = new float[m][n];
        switch( type ) {
            case DDRM:
                val doubleData = A.getDDRM().getData();
                for (int i = 0, idx = 0; i < m; ++i) {
                    for (int j = 0; j < n; ++j, ++idx) {
                        array[i][j] = (float) doubleData[idx];
                    }
                }
                break;
            case FDRM:
                val floatData = A.getFDRM().getData();
                for (int i = 0; i < m; i++) {
                    System.arraycopy(floatData, i * n, array[i], 0, n);
                }
                break;
            default:
                throw new RuntimeException("Unsupported matrix type");
        }
        return array;
    }
}
