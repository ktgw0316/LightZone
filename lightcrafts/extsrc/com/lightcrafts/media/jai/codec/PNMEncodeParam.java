/*
 * $RCSfile: PNMEncodeParam.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:55:32 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.codec;

/**
 * An instance of <code>ImageEncodeParam</code> for encoding images in
 * the PNM format.
 *
 * <p> This class allows for the specification of whether to encode
 * in the ASCII or raw variants of the PBM, PGM, and PPM formats.
 * By default, raw encoding is used.
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 */
public class PNMEncodeParam implements ImageEncodeParam {

    private boolean raw = true;
    
    /**
     * Constructs a PNMEncodeParam object with default values for parameters.
     */
    public PNMEncodeParam() {
    }

    /**
     * Sets the representation to be used.  If the <code>raw</code>
     * parameter is <code>true</code>, raw encoding will be used; 
     * otherwise ASCII encoding will be used.
     *
     * @param raw <code>true</code> if raw format is to be used.
     */
    public void setRaw(boolean raw) {
        this.raw = raw;
    }

    /**
     * Returns the value of the <code>raw</code> parameter.
     */
    public boolean getRaw() {
        return raw;
    }
}
