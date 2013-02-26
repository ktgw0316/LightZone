/*
 * $RCSfile: FFTmediaLib.java,v $
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

import com.lightcrafts.media.jai.opimage.FFT;
import com.lightcrafts.media.jai.util.MathJAI;

import com.sun.medialib.mlib.*;

/**
 * The Fast Fourier Transform (FFT) class interface to mediaLib.
 *
 * @since EA4
 */
public class FFTmediaLib extends FFT {
    /* Flag to indicate the special case of unitary scaling with a
       length equal to an odd power of 2. */
    private boolean specialUnitaryScaling = false;

    /* The square root of 2. */
    private static final double SQUARE_ROOT_OF_2 = Math.sqrt(2.0);

    /**
     * Construct a new FFTmediaLib object.
     *
     * @param negatedExponent Whether the exponent is negated.
     * @param scaleType The type of scaling to be applied.
     * @param length The length of the FFT; must be a positive power of 2.
     */
    public FFTmediaLib(boolean negatedExponent,
                       Integer scaleType,
                       int length) {
       super(negatedExponent, scaleType, length);
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
            throw new RuntimeException(JaiI18N.getString("FFTmediaLib0"));
        }

        // Cache the length.
        this.length = length;

        // Allocate work buffer memory.
        if(!lengthIsSet || length != real.length) {
            real = new double[length];
            imag = new double[length];
        }

        // Set initialization flag.
        lengthIsSet = true;

        // Set flag for special-case: unitary scaling and length = 2**N, N odd.
        if(scaleType == SCALING_UNITARY) {
            // The following calculation assumes that the length is a
            // positive power of 2 which has been verified above.
            int exponent = 0;
            int powerOfTwo = 1;
            while(powerOfTwo < length) {
                powerOfTwo <<= 1;
                exponent++;
            }

            // Set the special case flag if the exponent is not even.
            specialUnitaryScaling = exponent % 2 != 0;
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
                                // XXX Should clampFloat() be invoked both
                                // in the next two lines and below?
                            realFloat[offsetReal] = (float)this.real[i];
                            imagFloat[offsetReal] = (float)this.imag[i];
                            offsetReal += strideReal;
                        }
                    } else {
                        for(int i = 0; i < length; i++) {
                            realFloat[offsetReal] = (float)this.real[i];
                            imagFloat[offsetImag] = (float)this.imag[i];
                            offsetReal += strideReal;
                            offsetImag += strideImag;
                        }
                    }
                } else { // imagArg == null
                    for(int i = 0; i < length; i++) {
                        realFloat[offsetReal] = (float)this.real[i];
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
                            realDouble[offsetReal] = this.real[i];
                            imagDouble[offsetReal] = this.imag[i];
                            offsetReal += strideReal;
                        }
                    } else {
                        for(int i = 0; i < length; i++) {
                            realDouble[offsetReal] = this.real[i];
                            imagDouble[offsetImag] = this.imag[i];
                            offsetReal += strideReal;
                            offsetImag += strideImag;
                        }
                    }
                } else { // imagArg == null
                    for(int i = 0; i < length; i++) {
                        realDouble[offsetReal] = this.real[i];
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
            throw new RuntimeException(dataType + JaiI18N.getString("FFTmediaLib1"));
        }
    }

    /**
     * Calculate the DFT of a complex sequence using the FFT algorithm.
     */
    public void transform() {
        if(exponentSign < 0) {
            if(scaleType == SCALING_NONE) {
                Image.FFT_1(real, imag);
            } else if(scaleType == SCALING_UNITARY) {
                Image.FFT_3(real, imag);

                if(specialUnitaryScaling) {
                    // Divide by Math.sqrt(2.0) to account for the difference
                    // between the definition of mediaLib Group-III forward
                    // transform scaling when the length is an odd power of 2
                    // and that expected for unitary scaling.
                    for(int i = 0; i < length; i++) {
                        real[i] *= SQUARE_ROOT_OF_2;
                        imag[i] *= SQUARE_ROOT_OF_2;
                    }
                }
            } else if(scaleType == SCALING_DIMENSIONS) {
                Image.FFT_2(real, imag);
            }
        } else {
            if(scaleType == SCALING_NONE) {
                Image.IFFT_2(real, imag);
            } else if(scaleType == SCALING_UNITARY) {
                Image.IFFT_3(real, imag);

                if(specialUnitaryScaling) {
                    // Multiply by Math.sqrt(2.0) to account for the difference
                    // between the definition of mediaLib Group-III forward
                    // transform scaling when the length is an odd power of 2
                    // and that expected for unitary scaling.
                    for(int i = 0; i < length; i++) {
                        real[i] /= SQUARE_ROOT_OF_2;
                        imag[i] /= SQUARE_ROOT_OF_2;
                    }
                }
            } else if(scaleType == SCALING_DIMENSIONS) {
                Image.IFFT_1(real, imag);
            }
        }
    }
}
