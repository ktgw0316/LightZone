/*
 * $RCSfile: FCTmediaLib.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:46 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.mlib;

import java.awt.image.DataBuffer;
import java.util.Arrays;
import com.lightcrafts.media.jai.opimage.FCT;
import com.lightcrafts.media.jai.util.MathJAI;

import com.sun.medialib.mlib.*;

/**
 * The Fast Cosine Transform (FCT) class. This classes calculates the FCT
 * using mediaLib's 1D double precision FFT.
 *
 * @since EA4
 */
public class FCTmediaLib extends FCT {
    /** The length of the FCT. */
    private int length;

    /** Initialization flag. */
    private boolean lengthIsSet = false;

    /** Lookup table of cosine scale factors. */
    private double[] wr;

    /** Lookup table of sine scale factors. */
    private double[] wi;

    /** Work array for real part. */
    protected double[] real;

    /** Work array for imaginary part. */
    protected double[] imag;

    /**
     * Construct a new FCT object.
     *
     * @param length The length of the FCT; must be a positive power of 2.
     */
    public FCTmediaLib(boolean isForwardTransform, int length) {
        this.isForwardTransform = isForwardTransform;
        setLength(length);
    }

    // ***** FCT inner class public methods. *****

    /**
     * Initialize the length-dependent fields.
     *
     * @param length The length of the FCT; must be a positive power of 2.
     */
    public void setLength(int length) {
        // Check whether it's necessary to continue.
        if(lengthIsSet && length == this.length) {
            return;
        }

        // Ensure that the length is a positive power of two.
        if(!MathJAI.isPositivePowerOf2(length)) {
            throw new RuntimeException(JaiI18N.getString("FCTmediaLib0"));
        }

        // Cache the length.
        this.length = length;

        // Allocate cache memory.
        if(real == null || length != real.length) {
            real = new double[length];
            imag = new double[length];
        }

        // Calculate lookup tables of the cosine coefficients.
        calculateFCTLUTs();

        // Set initialization flag.
        lengthIsSet = true;
    }

    /**
     * Calculate the FCT sine and cosine lookup tables.
     */
    private void calculateFCTLUTs() {
        wr = new double[length];
        wi = new double[length];

        for(int i = 0; i < length; i++) {
            double factor = ((i == 0) ?
                             Math.sqrt(1.0/length) :
                             Math.sqrt(2.0/length));
            double freq = Math.PI*i/(2.0*length);
            wr[i] = factor*Math.cos(freq);
            wi[i] = factor*Math.sin(freq);
        }
    }

    /**
     * Set the internal work data array of the FCT object.
     *
     * @param dataType The data type of the source data according to
     * one of the DataBuffer TYPE_* flags. This should be either
     * DataBuffer.TYPE_FLOAT or DataBuffer.TYPE_DOUBLE.
     * @param data Float or double array of data.
     * @param offset Offset into the data array.
     * @param stride The data array stride value.
     * @param count The number of values to copy.
     */
    public void setData(int dataType, Object data,
                        int offset, int stride,
                        int count) {
        if(isForwardTransform) {
            setFCTData(dataType, data, offset, stride, count);
        } else {
            setIFCTData(dataType, data, offset, stride, count);
        }
    }

    /**
     * Get data from the internal work data array of the FCT object.
     *
     * @param dataType The data type of the source data according to
     * one of the DataBuffer TYPE_* flags. This should be either
     * DataBuffer.TYPE_FLOAT or DataBuffer.TYPE_DOUBLE.
     * @param data Float or double array of data.
     * @param offset Offset into the data array.
     * @param stride The data array stride value.
     */
    public void getData(int dataType, Object data,
                        int offset, int stride) {
        if(isForwardTransform) {
            getFCTData(dataType, data, offset, stride);
        } else {
            getIFCTData(dataType, data, offset, stride);
        }
    }

    /**
     * Set the internal work data arrays of the FFT object.
     *
     * @param dataType The data type of the source data according to
     * one of the DataBuffer TYPE_* flags. This should be either
     * DataBuffer.TYPE_FLOAT or DataBuffer.TYPE_DOUBLE.
     * @param data Float or double array of data.
     * @param offset Offset into the data array.
     * @param stride The data array stride value.
     * @param count The number of values to copy.
     */
    private void setFCTData(int dataType,
                            Object data, int offset, int stride,
                            int count) {
        // Copy the parameter arrays.
        switch(dataType) {
        case DataBuffer.TYPE_FLOAT:
            {
                float[] realFloat = (float[])data;
                for(int i = 0; i < count; i++) {
                    imag[i] = realFloat[offset];
                    offset += stride;
                }
                for(int i = count; i < length; i++) {
                    imag[i] = 0.0;
                }
                int k = length - 1;
                int j = 0;
                for(int i = 0; i < k; i++) {
                    real[i] = imag[j++];
                    real[k--] = imag[j++];
                }
            }
        break;
        case DataBuffer.TYPE_DOUBLE:
            {
                double[] realDouble = (double[])data;
                for(int i = 0; i < count; i++) {
                    imag[i] = realDouble[offset];
                    offset += stride;
                }
                for(int i = count; i < length; i++) {
                    imag[i] = 0.0;
                }
                int k = length - 1;
                int j = 0;
                for(int i = 0; i < k; i++) {
                    real[i] = imag[j++];
                    real[k--] = imag[j++];
                }
            }
        break;
        default:
            // NB: This statement should be unreachable as the destination
            // image is required to be a floating point type and the
            // RasterAccessor is supposed to promote the data type of
            // all rasters to the "minimum" data type of all source
            // and destination rasters involved.
            throw new RuntimeException(dataType + JaiI18N.getString("FCTmediaLib1"));
        }

        // Always clear imaginary part.
        Arrays.fill(imag, 0, length, 0.0);
    }

    /**
     * Get data from the internal work data arrays of the FFT object.
     *
     * @param dataType The data type of the source data according to
     * one of the DataBuffer TYPE_* flags. This should be either
     * DataBuffer.TYPE_FLOAT or DataBuffer.TYPE_DOUBLE.
     * @param data The data as a float[] or double[].
     * @param offset Offset into the data array.
     * @param stride The array stride value.
     * @param count The number of values to copy.
     */
    private void getFCTData(int dataType,
                            Object data, int offset, int stride) {
        switch(dataType) {
        case DataBuffer.TYPE_FLOAT:
            {
                float[] realFloat = (float[])data;
                for(int i = 0; i < length; i++) {
                    realFloat[offset] =
                        (float)(wr[i]*real[i] + wi[i]*imag[i]);
                    offset += stride;
                }
            }
        break;
        case DataBuffer.TYPE_DOUBLE:
            {
                double[] realDouble = (double[])data;
                for(int i = 0; i < length; i++) {
                    realDouble[offset] =
                        wr[i]*real[i] + wi[i]*imag[i];
                    offset += stride;
                }
            }
        break;
        default:
            // NB: This statement should be unreachable as the destination
            // image is required to be a floating point type and the
            // RasterAccessor is supposed to promote the data type of
            // all rasters to the "minimum" data type of all source
            // and destination rasters involved.
            throw new RuntimeException(dataType + JaiI18N.getString("FCTmediaLib1"));
        }
    }

    /**
     * Set the internal work data arrays of the FFT object.
     *
     * @param dataType The data type of the source data according to
     * one of the DataBuffer TYPE_* flags. This should be either
     * DataBuffer.TYPE_FLOAT or DataBuffer.TYPE_DOUBLE.
     * @param data The data as a float[] or double[].
     * @param offset Offset into the data array.
     * @param stride The array stride value.
     * @param count The number of values to copy.
     */
    private void setIFCTData(int dataType, Object data,
                             int offset, int stride,
                             int count) {
        // Copy the parameter arrays.
        switch(dataType) {
        case DataBuffer.TYPE_FLOAT:
            {
                float[] realFloat = (float[])data;
                for(int i = 0; i < count; i++) {
                    float r = realFloat[offset];
                    real[i] = r*wr[i];
                    imag[i] = r*wi[i];
                    offset += stride;
                }
            }
        break;
        case DataBuffer.TYPE_DOUBLE:
            {
                double[] realDouble = (double[])data;
                for(int i = 0; i < count; i++) {
                    double r = realDouble[offset];
                    real[i] = r*wr[i];
                    imag[i] = r*wi[i];
                    offset += stride;
                }
            }
        break;
        default:
            // NB: This statement should be unreachable as the destination
            // image is required to be a floating point type and the
            // RasterAccessor is supposed to promote the data type of
            // all rasters to the "minimum" data type of all source
            // and destination rasters involved.
            throw new RuntimeException(dataType + JaiI18N.getString("FCTmediaLib1"));
        }

        // If fewer input than target points fill target with zeros.
        if(count < length) {
            Arrays.fill(real, count, length, 0.0);
            Arrays.fill(imag, count, length, 0.0);
        }
    }

    /**
     * Get data from the internal work data arrays of the FFT object.
     *
     * @param dataType The data type of the source data according to
     * one of the DataBuffer TYPE_* flags. This should be either
     * DataBuffer.TYPE_FLOAT or DataBuffer.TYPE_DOUBLE.
     * @param data The data as a float[] or double[].
     * @param offset Offset into the data array.
     * @param stride The array stride value.
     * @param count The number of values to copy.
     */
    private void getIFCTData(int dataType,
                             Object data, int offset, int stride) {
        switch(dataType) {
        case DataBuffer.TYPE_FLOAT:
            {
                float[] realFloat = (float[])data;
                int k = length - 1;
                for(int i = 0; i < k; i++) {
                    realFloat[offset] = (float)real[i];
                    offset += stride;
                    realFloat[offset] = (float)real[k--];
                    offset += stride;
                }
            }
        break;
        case DataBuffer.TYPE_DOUBLE:
            {
                double[] realDouble = (double[])data;
                int k = length - 1;
                for(int i = 0; i < k; i++) {
                    realDouble[offset] = (float)real[i];
                    offset += stride;
                    realDouble[offset] = (float)real[k--];
                    offset += stride;
                }
            }
        break;
        default:
            // NB: This statement should be unreachable as the destination
            // image is required to be a floating point type and the
            // RasterAccessor is supposed to promote the data type of
            // all rasters to the "minimum" data type of all source
            // and destination rasters involved.
            throw new RuntimeException(dataType + JaiI18N.getString("FCTmediaLib1"));
        }
    }

    /**
     * Calculate the DCT of a sequence using the FFT algorithm.
     */
    public void transform() {
        if(isForwardTransform) {
            Image.FFT_1(real, imag);
        } else {
            Image.IFFT_2(real, imag);
        }
    }
}
