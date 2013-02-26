/*
 * $RCSfile: BMPEncodeParam.java,v $
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
 * An instance of <code>ImageEncodeParam</code> for encoding images in
 * the BMP format.
 *
 * <p> This class allows for the specification of various parameters
 * while encoding (writing) a BMP format image file.  By default, the
 * version used is VERSION_3, no compression is used, and the data layout
 * is bottom_up, such that the pixels are stored in bottom-up order, the
 * first scanline being stored last. 
 *
 * <p><b> This class is not a committed part of the JAI API.  It may
 * be removed or changed in future releases of JAI.</b>
 * 
 */
public class BMPEncodeParam implements ImageEncodeParam {

    // version constants

    /** Constant for BMP version 2. */
    public static final int VERSION_2 = 0;

    /** Constant for BMP version 3. */
    public static final int VERSION_3 = 1;

    /** Constant for BMP version 4. */
    public static final int VERSION_4 = 2;

    // Default values
    private int version = VERSION_3;
    private boolean compressed = false;
    private boolean topDown = false;
    
    /**
     * Constructs an BMPEncodeParam object with default values for parameters.
     */
    public BMPEncodeParam() {}

    /** Sets the BMP version to be used. */
    public void setVersion(int versionNumber) {
	checkVersion(versionNumber);
	this.version = versionNumber;
    }

    /** Returns the BMP version to be used. */
    public int getVersion() {
	return version;
    }

    /** If set, the data will be written out compressed, if possible. */
    public void setCompressed(boolean compressed) {
	this.compressed = compressed;
    }

    /** 
     * Returns the value of the parameter <code>compressed</code>.
     */
    public boolean isCompressed() {
	return compressed;
    }

    /** 
     * If set, the data will be written out in a top-down manner, the first
     * scanline being written first.
     */
    public void setTopDown(boolean topDown) {
	this.topDown = topDown;
    }

    /**
     * Returns the value of the <code>topDown</code> parameter.
     */
    public boolean isTopDown() {
	return topDown;
    }

    // Method to check whether we can handle the given version.
    private void checkVersion(int versionNumber) {
	if ( !(versionNumber == VERSION_2 ||
	       versionNumber == VERSION_3 ||
	       versionNumber == VERSION_4) ) {
	    throw new RuntimeException(JaiI18N.getString("BMPEncodeParam0")); 
	}
    }

}
