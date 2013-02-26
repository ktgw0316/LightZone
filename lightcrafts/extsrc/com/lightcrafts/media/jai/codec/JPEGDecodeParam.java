/*
 * $RCSfile: JPEGDecodeParam.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:31 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;

/**
 * An instance of <code>ImageDecodeParam</code> for decoding images in
 * the JPEG format.
 *
 * <p> This class allows for the specification of whether to decode the
 * JPEG data into an image having a <code>SampleModel</code> which is a
 * <code>ComponentSampleModel</code> or a subclass thereof.  By default
 * data are decoded into an image having a <code>ComponentSampleModel</code>.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 */
public class JPEGDecodeParam implements ImageDecodeParam {

    /**
     * Flag indicating whether to decode the data into an image with
     * a <code>ComponentSampleModel</code>.
     */
    private boolean decodeToCSM = true;
    
    /**
     * Constructs a <code>JPEGDecodeParam</code> object with default
     * parameter values.
     */
    public JPEGDecodeParam() {
    }

    /**
     * Sets the data formatting flag to the value of the supplied parameter.
     * If <code>true</code> the data will be decoded into an image which has
     * a <code>SampleModel</code> which is a <code>ComponentSampleModel</code>.
     * The default setting of this flag is <code>true</code>.  If the flag is
     * set to <code>false</code> then memory may be saved during decoding but
     * the resulting image is not in that case guaranteed to have a
     * <code>ComponentSampleModel</code>.
     *
     * @param decodeToCSM <code>true</code> if a
     * <code>ComponentSampleModel</code> layout is preferred for the
     * decoded image.
     */
    public void setDecodeToCSM(boolean decodeToCSM) {
        this.decodeToCSM = decodeToCSM;
    }

    /**
     * Returns the value of the <code>ComponentSampleModel</code> flag
     * which is by default <code>true</code>.
     */
    public boolean getDecodeToCSM() {
        return decodeToCSM;
    }
}
