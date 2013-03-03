/*
 * $RCSfile: FFT.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:26 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;

import java.awt.image.DataBuffer;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Locale;
import com.lightcrafts.mediax.jai.operator.DFTDescriptor;
import com.lightcrafts.media.jai.util.MathJAI;

/**
 * The Fast Fourier Transform (FFT) class.
 *
 * @since EA3
 */
public class FFT {
    /**
     * A flag indicating that the transform is not to be scaled.
     */
    public static final int SCALING_NONE =
        DFTDescriptor.SCALING_NONE.getValue();

    /**
     * A flag indicating that the transform is to be scaled by the square
     * root of the product of its dimensions.
     */
    public static final int SCALING_UNITARY =
        DFTDescriptor.SCALING_UNITARY.getValue();

    /**
     * A flag indicating that the transform is to be scaled by the product
     * of its dimensions.
     */
    public static final int SCALING_DIMENSIONS =
        DFTDescriptor.SCALING_DIMENSIONS.getValue();

    /** Initialization flag. */
    protected boolean lengthIsSet = false;

    /** The sign of the exponential. */
    protected int exponentSign;

    /** The type of scaling. */
    protected int scaleType;

    /** The length of the FFT. */
    protected int length;

    /** The number of bits required to represent the length. */
    private int nbits;

    /** Indices to map between normal and bit reversed order. */
    private int[] index;

    /** The scale factor. */
    private double scaleFactor;

    /** Lookup table of cosines. */
    private double[] wr;

    /** Lookup table of sines. */
    private double[] wi;

    /** Lookup table of cosines for FCT. */
    private double[] wrFCT;

    /** Lookup table of sines for FCT. */
    private double[] wiFCT;

    /** Work array for real part. */
    protected double[] real;

    /** Work array for imaginary part. */
    protected double[] imag;

    /**
     * Construct a new FFT object.
     *
     * @param negatedExponent Whether the exponent is negated.
     * @param scaleType The type of scaling to be applied.
     * @param length The length of the FFT; must be a positive power of 2.
     */
    public FFT(boolean negatedExponent, Integer scaleType, int length) {
        // Set the exponential sign.
        exponentSign = negatedExponent ? -1 : 1;

        // Set the scaling type.
        this.scaleType = scaleType.intValue();

        // Set the sequence length and quantities dependent thereon.
        setLength(length);
    }

    /**
     * Initialize the length-dependent fields.
     *
     * @param length The length of the FFT; must be a positive power of 2.
     */
    public void setLength(int length) {
        // Check whether it's necessary to continue.
        if(lengthIsSet && length == this.length) {
            return;
        }

        // Ensure that the length is a positive power of two.
        if(!MathJAI.isPositivePowerOf2(length)) {
            throw new RuntimeException(JaiI18N.getString("FFT0"));
        }

        // Cache the length.
        this.length = length;

        // Set the scale factor.
        if(scaleType == SCALING_NONE) {
            scaleFactor = 1.0;
        } else if(scaleType == SCALING_UNITARY) {
            scaleFactor = 1.0/Math.sqrt(length);
        } else if(scaleType == SCALING_DIMENSIONS) {
            scaleFactor = 1.0/length;
        } else {
            // NB: This statement should be unreachable if the scaling
            // type is properly verified in the operation descriptor.
            throw new RuntimeException(JaiI18N.getString("FFT1"));
        }

        // Calculate the number of bits required to represent the length.
        int power = 1;
        nbits = 0;
        while(power < length) {
            nbits++;
            power <<= 1;
        }

        // Initialize the bit-reversal LUT.
        initBitReversalLUT();

        // Calculate lookup tables of the W values.
        calculateCoefficientLUTs();

        // Allocate work buffer memory.
        if(!lengthIsSet || length > real.length) {
            real = new double[length];
            imag = new double[length];
        }

        // Set initialization flag.
        lengthIsSet = true;
    }

    /**
     * Initialize the bit-reversal lookup table.
     */
    private void initBitReversalLUT() {
        // Calculate elements of index[] and fill.
        index = new int[length];
        for(int i = 0; i < length; ++i) {
            int l = i;
            int power = length >> 1;
            int irev = 0;
            for(int k = 0; k < nbits; ++k) {
                int j = (l & 1);
                if( j != 0 ) {
                    irev = irev + power;
                }
                l >>= 1;
                power >>= 1;

                index[i] = irev;
            }
        }
    }

    /**
     * Calculate the sine and cosine lookup tables.
     */
    private void calculateCoefficientLUTs() {
        wr = new double[nbits];
        wi = new double[nbits];

        int inode = 1;
        double cons = exponentSign*Math.PI;

        for(int bit = 0; bit < nbits; bit++) {
            wr[bit] = Math.cos(cons/inode);
            wi[bit] = Math.sin(cons/inode);
            inode *= 2;
        }
    }

    /**
     * Calculate the FCT sine and cosine lookup tables.
     */
    private void calculateFCTLUTs() {
        wrFCT = new double[length];
        wiFCT = new double[length];

        for(int i = 0; i < length; i++) {
            double factor = ((i == 0) ?
                             Math.sqrt(1.0/length) :
                             Math.sqrt(2.0/length));
            double freq = Math.PI*i/(2.0*length);
            wrFCT[i] = factor*Math.cos(freq);
            wiFCT[i] = factor*Math.sin(freq);
        }
    }

    /**
     * Set the internal work data arrays of the FFT object.
     *
     * @param dataType The data type of the source data according to
     * one of the DataBuffer TYPE_* flags. This should be either
     * DataBuffer.TYPE_FLOAT or DataBuffer.TYPE_DOUBLE.
     * @param realArg Float or double array of real parts.
     * @param offsetReal Offset into the array of real parts.
     * @param strideReal The real array stride value.
     * @param imagArg Float or double array of imaginary parts.
     * @param offsetImag Offset into the array of imaginary parts.
     * @param strideImag The imaginary array stride value.
     * @param count The number of values to copy.
     */
    public void setData(int dataType,
                        Object realArg, int offsetReal, int strideReal,
                        Object imagArg, int offsetImag, int strideImag,
                        int count) {
        // Copy the parameter arrays.
        switch(dataType) {
        case DataBuffer.TYPE_FLOAT:
            {
                float[] realFloat = (float[])realArg;
                if(imagArg != null) {
                    float[] imagFloat = (float[])imagArg;
                    if(offsetReal == offsetImag &&
                       strideReal == strideImag) {
                        for(int i = 0; i < count; i++) {
                            real[i] = realFloat[offsetReal];
                            imag[i] = imagFloat[offsetReal];
                            offsetReal += strideReal;
                        }
                    } else {
                        for(int i = 0; i < count; i++) {
                            real[i] = realFloat[offsetReal];
                            imag[i] = imagFloat[offsetImag];
                            offsetReal += strideReal;
                            offsetImag += strideImag;
                        }
                    }
                } else {
                    for(int i = 0; i < count; i++) {
                        real[i] = realFloat[offsetReal];
                        offsetReal += strideReal;
                    }
                }
            }
        break;
        case DataBuffer.TYPE_DOUBLE:
            {
                double[] realDouble = (double[])realArg;
                if(strideReal == 1 && strideImag == 1) {
                    System.arraycopy(realDouble, offsetReal,
                                     real, 0, count);
                    if(imagArg != null) {
                        System.arraycopy((double[])imagArg, offsetImag,
                                         imag, 0, count);
                    }
                } else if(imagArg != null) {
                    double[] imagDouble = (double[])imagArg;
                    if(offsetReal == offsetImag &&
                       strideReal == strideImag) {
                        for(int i = 0; i < count; i++) {
                            real[i] = realDouble[offsetReal];
                            imag[i] = imagDouble[offsetReal];
                            offsetReal += strideReal;
                        }
                    } else {
                        for(int i = 0; i < count; i++) {
                            real[i] = realDouble[offsetReal];
                            imag[i] = imagDouble[offsetImag];
                            offsetReal += strideReal;
                            offsetImag += strideImag;
                        }
                    }
                } else {
                    for(int i = 0; i < count; i++) {
                        real[i] = realDouble[offsetReal];
                        offsetReal += strideReal;
                    }
                }
            }
        break;
        default:
            // NB: This statement should be unreachable as the destination
            // image is required to be a floating point type and the
            // RasterAccessor is supposed to promote the data type of
            // all rasters to the "minimum" data type of all source
            // and destination rasters involved.
            throw new RuntimeException(dataType + JaiI18N.getString("FFT2"));
        }

        // If fewer input than target points fill target with zeros.
        if(count < length) {
            Arrays.fill(real, count, length, 0.0);
            if(imagArg != null) {
                Arrays.fill(imag, count, length, 0.0);
            }
        }

        if(imagArg == null) {
            Arrays.fill(imag, 0, length, 0.0);
        }
    }

    /**
     * Get data from the internal work data arrays of the FFT object.
     *
     * @param dataType The data type of the source data according to
     * one of the DataBuffer TYPE_* flags. This should be either
     * DataBuffer.TYPE_FLOAT or DataBuffer.TYPE_DOUBLE.
     * @param realArg Float or double array of real parts.
     * @param offsetReal Offset into the array of real parts.
     * @param strideReal The real array stride value.
     * @param imagArg Float or double array of imaginary parts.
     * @param offsetImag Offset into the array of imaginary parts.
     * @param strideImag The imaginary array stride value.
     * @param count The number of values to copy.
     */
    public void getData(int dataType,
                        Object realArg, int offsetReal, int strideReal,
                        Object imagArg, int offsetImag, int strideImag) {
        switch(dataType) {
        case DataBuffer.TYPE_FLOAT:
            {
                float[] realFloat = (float[])realArg;
                if(imagArg != null) {
                    float[] imagFloat = (float[])imagArg;
                    if(offsetReal == offsetImag &&
                       strideReal == strideImag) {
                        for(int i = 0; i < length; i++) {
                            int idx = index[i];
                            realFloat[offsetReal] = (float)this.real[idx];
                            imagFloat[offsetReal] = (float)this.imag[idx];
                            offsetReal += strideReal;
                        }
                    } else {
                        for(int i = 0; i < length; i++) {
                            int idx = index[i];
                            realFloat[offsetReal] = (float)this.real[idx];
                            imagFloat[offsetImag] = (float)this.imag[idx];
                            offsetReal += strideReal;
                            offsetImag += strideImag;
                        }
                    }
                } else { // imagArg == null
                    for(int i = 0; i < length; i++) {
                        realFloat[offsetReal] = (float)this.real[index[i]];
                        offsetReal += strideReal;
                    }
                }
            }
        break;
        case DataBuffer.TYPE_DOUBLE:
            {
                double[] realDouble = (double[])realArg;
                if(imagArg != null) {
                    double[] imagDouble = (double[])imagArg;
                    if(offsetReal == offsetImag &&
                       strideReal == strideImag) {
                        for(int i = 0; i < length; i++) {
                            int idx = index[i];
                            realDouble[offsetReal] = this.real[idx];
                            imagDouble[offsetReal] = this.imag[idx];
                            offsetReal += strideReal;
                        }
                    } else {
                        for(int i = 0; i < length; i++) {
                            int idx = index[i];
                            realDouble[offsetReal] = this.real[idx];
                            imagDouble[offsetImag] = this.imag[idx];
                            offsetReal += strideReal;
                            offsetImag += strideImag;
                        }
                    }
                } else { // imagArg == null
                    for(int i = 0; i < length; i++) {
                        realDouble[offsetReal] = this.real[index[i]];
                        offsetReal += strideReal;
                    }
                }
            }
        break;
        default:
            // NB: This statement should be unreachable as the destination
            // image is required to be a floating point type and the
            // RasterAccessor is supposed to promote the data type of
            // all rasters to the "minimum" data type of all source
            // and destination rasters involved.
            throw new RuntimeException(dataType + JaiI18N.getString("FFT2"));
        }
    }

    /**
     * Set the internal work data arrays of the FFT object for use with
     * an FCT operation.
     *
     * @param dataType The data type of the source data according to
     * one of the DataBuffer TYPE_* flags. This should be either
     * DataBuffer.TYPE_FLOAT or DataBuffer.TYPE_DOUBLE.
     * @param data The data as a float[] or double[].
     * @param offset Offset into the data array.
     * @param stride The array stride value.
     * @param count The number of values to copy.
     */
    public void setFCTData(int dataType,
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
            throw new RuntimeException(dataType + JaiI18N.getString("FFT2"));
        }

        // Always clear imaginary part.
        Arrays.fill(imag, 0, length, 0.0);
    }

    /**
     * Get data from the internal work data arrays of the FFT object after
     * an IFCT operation.
     *
     * @param dataType The data type of the source data according to
     * one of the DataBuffer TYPE_* flags. This should be either
     * DataBuffer.TYPE_FLOAT or DataBuffer.TYPE_DOUBLE.
     * @param data The data as a float[] or double[].
     * @param offset Offset into the data array.
     * @param stride The array stride value.
     * @param count The number of values to copy.
     */
    public void getFCTData(int dataType,
                           Object data, int offset, int stride) {
        if(wrFCT == null || wrFCT.length != length) {
            calculateFCTLUTs();
        }

        switch(dataType) {
        case DataBuffer.TYPE_FLOAT:
            {
                float[] realFloat = (float[])data;
                for(int i = 0; i < length; i++) {
                    int idx = index[i];
                    realFloat[offset] =
                        (float)(wrFCT[i]*real[idx] + wiFCT[i]*imag[idx]);
                    offset += stride;
                }
            }
        break;
        case DataBuffer.TYPE_DOUBLE:
            {
                double[] realDouble = (double[])data;
                for(int i = 0; i < length; i++) {
                    int idx = index[i];
                    realDouble[offset] =
                        wrFCT[i]*real[idx] + wiFCT[i]*imag[idx];
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
            throw new RuntimeException(dataType + JaiI18N.getString("FFT2"));
        }
    }

    /**
     * Set the internal work data arrays of the FFT object for use with
     * an IFCT operation.
     *
     * @param dataType The data type of the source data according to
     * one of the DataBuffer TYPE_* flags. This should be either
     * DataBuffer.TYPE_FLOAT or DataBuffer.TYPE_DOUBLE.
     * @param data The data as a float[] or double[].
     * @param offset Offset into the data array.
     * @param stride The array stride value.
     * @param count The number of values to copy.
     */
    public void setIFCTData(int dataType, Object data,
                            int offset, int stride,
                            int count) {
        if(wrFCT == null || wrFCT.length != length) {
            calculateFCTLUTs();
        }

        // Copy the parameter arrays.
        switch(dataType) {
        case DataBuffer.TYPE_FLOAT:
            {
                float[] realFloat = (float[])data;
                for(int i = 0; i < count; i++) {
                    float r = realFloat[offset];
                    real[i] = r*wrFCT[i];
                    imag[i] = r*wiFCT[i];
                    offset += stride;
                }
            }
        break;
        case DataBuffer.TYPE_DOUBLE:
            {
                double[] realDouble = (double[])data;
                for(int i = 0; i < count; i++) {
                    double r = realDouble[offset];
                    real[i] = r*wrFCT[i];
                    imag[i] = r*wiFCT[i];
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
            throw new RuntimeException(dataType + JaiI18N.getString("FFT2"));
        }

        // If fewer input than target points fill target with zeros.
        if(count < length) {
            Arrays.fill(real, count, length, 0.0);
            Arrays.fill(imag, count, length, 0.0);
        }
    }

    /**
     * Get data from the internal work data arrays of the FFT object after
     * an IFCT operation.
     *
     * @param dataType The data type of the source data according to
     * one of the DataBuffer TYPE_* flags. This should be either
     * DataBuffer.TYPE_FLOAT or DataBuffer.TYPE_DOUBLE.
     * @param data The data as a float[] or double[].
     * @param offset Offset into the data array.
     * @param stride The array stride value.
     * @param count The number of values to copy.
     */
    public void getIFCTData(int dataType,
                            Object data, int offset, int stride) {
        switch(dataType) {
        case DataBuffer.TYPE_FLOAT:
            {
                float[] realFloat = (float[])data;
                int k = length - 1;
                for(int i = 0; i < k; i++) {
                    realFloat[offset] = (float)real[index[i]];
                    offset += stride;
                    realFloat[offset] = (float)real[index[k--]];
                    offset += stride;
                }
            }
        break;
        case DataBuffer.TYPE_DOUBLE:
            {
                double[] realDouble = (double[])data;
                int k = length - 1;
                for(int i = 0; i < k; i++) {
                    realDouble[offset] = (float)real[index[i]];
                    offset += stride;
                    realDouble[offset] = (float)real[index[k--]];
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
            throw new RuntimeException(dataType + JaiI18N.getString("FFT2"));
        }
    }

    /**
     * Calculate the DFT of a complex sequence using the FFT algorithm.
     */
    public void transform() {
        int i, k, j, l; // Index variables

        Integer i18n = new Integer(length);
        NumberFormat numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault());
	    
        if(real.length < length || imag.length < length) {
            throw new RuntimeException(numberFormatter.format(i18n) + JaiI18N.getString("FFT3"));
        }

        int inode = 1;
        int ipair;
        for(l = 0; l < nbits; ++l) {
            double cosp = 1.0; // initial w values
            double sinp = 0.0;
            ipair = 2 *  inode; // calc pair separation
            for(k = 0; k < inode; ++k) {// sequence through array
                for(i = k; i < length; i += ipair) {
                    j = i + inode; // calc other node index
                    int iIndex = index[i];
                    int jIndex = index[j];
                    double rtemp = real[jIndex]*cosp - (imag[jIndex]*sinp);
                    double itemp = imag[jIndex]*cosp + (real[jIndex]*sinp);
                    real[jIndex] = real[iIndex] - rtemp; // calc butterfly
                    imag[jIndex] = imag[iIndex] - itemp;
                    real[iIndex] = real[iIndex] + rtemp;
                    imag[iIndex] = imag[iIndex] + itemp;
                }
                double costmp = cosp;
                cosp = cosp * wr[l] - sinp * wi[l]; // update cosp, sinp
                sinp = costmp * wi[l] + sinp * wr[l];
            }
            inode = inode * 2; // new nodal dist
        }

        if(scaleFactor != 1.0) { // multiply by non-unity scale factor
            for(i = 0; i < length; ++i) {
                real[i] = real[i]*scaleFactor;
                imag[i] = imag[i]*scaleFactor;
            }
        }
    }
}
