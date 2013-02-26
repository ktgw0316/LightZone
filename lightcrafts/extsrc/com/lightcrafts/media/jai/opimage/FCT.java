/*
 * $RCSfile: FCT.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:25 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.opimage;

import com.lightcrafts.mediax.jai.operator.DFTDescriptor;

/**
 * The Fast Cosine Transform (FCT) class.
 *
 * @since EA3
 */
public class FCT {
    /*
     * Flag indicating whether the transform is forward (true)
     * or inverse (false).
     */
    protected boolean isForwardTransform;

    /** The FFT object by which the FCT is actually calculated. */
    private FFT fft = null;

    /** Default constructor */
    public FCT() {}

    /**
     * Construct a new FCT object.
     *
     * @param length The length of the FCT; must be a positive power of 2.
     */
    public FCT(boolean isForwardTransform, int length) {
        // Cache the directional flag.
        this.isForwardTransform = isForwardTransform;

        // Create the FFT object.
        fft = new FFT(isForwardTransform,
                      new Integer(DFTDescriptor.SCALING_NONE.getValue()),
                      length);
    }

    /**
     * Initialize the length-dependent fields.
     *
     * @param length The length of the FCT; must be a positive power of 2.
     */
    public void setLength(int length) {
        fft.setLength(length);
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
            fft.setFCTData(dataType, data, offset, stride, count);
        } else {
            fft.setIFCTData(dataType, data, offset, stride, count);
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
            fft.getFCTData(dataType, data, offset, stride);
        } else {
            fft.getIFCTData(dataType, data, offset, stride);
        }
    }

    /**
     * Calculate the DCT of a sequence using the FCT algorithm.
     */
    public void transform() {
        fft.transform();
    }
}
