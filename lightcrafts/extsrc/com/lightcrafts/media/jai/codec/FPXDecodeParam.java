/*
 * $RCSfile: FPXDecodeParam.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:29 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;

/**
 * An instance of <code>ImageDecodeParam</code> for decoding images
 * in the FlashPIX format.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 */
public class FPXDecodeParam implements ImageDecodeParam {
    
    private int resolution = -1;

    /** Constructs a default instance of <code>FPXDecodeParam</code>. */
    public FPXDecodeParam() {}

    /**
     * Constructs an instance of <code>FPXDecodeParam</code>
     * to decode a given resolution.
     *
     * @param resolution The resolution number to be decoded.
     */
    public FPXDecodeParam(int resolution) {
        this.resolution = resolution;
    }

    /**
     * Sets the resolution to be decoded.
     *
     * @param resolution The resolution number to be decoded.
     */
    public void setResolution(int resolution) {
        this.resolution = resolution;
    }

    /**
     * Returns the resolution to be decoded.
     */
    public int getResolution() {
        return resolution;
    }
}
