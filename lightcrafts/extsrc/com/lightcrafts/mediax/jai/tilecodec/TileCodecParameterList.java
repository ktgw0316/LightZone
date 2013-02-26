/*
 * $RCSfile: TileCodecParameterList.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:55 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.tilecodec;

import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.ParameterListImpl;

/**
 * A subclass of <code>ParameterListImpl</code> that is specific to 
 * tile codecs. This class functions in either one or both of the two
 * registry modes supported by the <code>TileCodecDescriptor</code>
 * - "tileEncoder" and "tileDecoder".
 *
 * <p> This class is not intended to be subclassed for each individual
 * <code>TileEncoder</code> or <code>TileDecoder</code>. This is a generic
 * class which can be used as is for representing a parameter list for
 * any tile encoding/decoding format. The <code>ParameterListDescriptor</code>
 * provided as argument to the constructor should be the one returned from
 * the <code>getParameterListDescriptor()</code> method of the 
 * <code>TileCodecDescriptor</code> for the given format name. 
 *
 * <p> If the associated <code>TileCodecDescriptor</code>'s
 * <code>includesSampleModelInfo()</code> method returns false, then for the
 * "tileDecoder" mode, this class will be expected to contain a parameter
 * named "sampleModel" with a non-null <code>SampleModel</code> as its value.
 *
 * @since JAI 1.1
 */
public class TileCodecParameterList extends ParameterListImpl {

    // The name of the format
    private String formatName;

    // The modes valid for this class
    private String validModes[];

    /**
     * Creates a <code>TileCodecParameterList</code>. The
     * <code>validModes</code> argument specifies the registry modes valid
     * for this <code>TileCodecParameterList</code>. This should contain 
     * the "tileEncoder" registry mode or the "tileDecoder" registry
     * mode or both. The supplied descriptor object specifies the names
     * and number of the valid parameters, their <code>Class</code> types, 
     * as well as the <code>Range</code> of valid values for each parameter.
     *
     * @param formatName The name of the format, parameters for which are
     *                   specified through this parameter list.
     * @param validModes An array of <code>String</code> objects specifying
     *                   which registry modes are valid for this parameter list.
     * @param descriptor The <code>ParameterListDescriptor</code> object that
     *                   describes all valid parameters for this format. This
     *                   must be the the same descriptor that is returned from
     *                   the <code>getParameterListDescriptor()</code> method of
     *                   the <code>TileCodecDescriptor</code> for the given
     *                   formatName.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if validModes is null.
     * @throws IllegalArgumentException if descriptor is null.
     */
    public TileCodecParameterList(String formatName,
                                  String validModes[],
                                  ParameterListDescriptor descriptor) {
        super(descriptor);

	// Cause IllegalArgumentException to be thrown if any of the 
	// arguments is null.
	if (formatName == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("TileCodecDescriptorImpl0"));
	}

	if (validModes == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("TileCodecParameterList0"));
	}

	if (descriptor == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("TileCodecParameterList1"));
	}

        this.formatName = formatName;
	this.validModes = validModes;
    }

    /**
     * Returns the name of the format which this parameter list describes.
     */
    public String getFormatName() {
	return formatName;			  
    }

    /**
     * Returns true if the parameters in this 
     * <code>TileCodecParameterList</code> are valid for the specified 
     * registry mode name, false otherwise. The valid modes for
     * this class are the "tileEncoder" registry mode, and the
     * "tileDecoder" registry mode.
     */
    public boolean isValidForMode(String registryModeName) {
	for (int i=0; i<validModes.length; i++) {
	    if (validModes[i].equalsIgnoreCase(registryModeName)) {
		return true;
	    }
	}

	return false;
    }

    /**
     * Returns all the modes that this <code>TileCodecParameterList</code>
     * is valid for, as a <code>String</code> array.
     */
    public String[] getValidModes() {
	return (String[])validModes.clone();
    }
}
