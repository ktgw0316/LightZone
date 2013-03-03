/*
 * $RCSfile: TileEncoderImpl.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:56 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.tilecodec;

import java.io.OutputStream;
import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecDescriptor;
import com.lightcrafts.media.jai.tilecodec.TileCodecUtils;

/**
 * A partial implementation of the <code>TileEncoder</code> interface
 * useful for subclassing.
 *
 * @since JAI 1.1
 */
public abstract class TileEncoderImpl implements TileEncoder {
    
    /**
     * The name of the format.
     */
    protected String formatName;

    /** 
     * The <code>OutputStream</code>  to write the encoded data to.
     */
    protected OutputStream outputStream;

    /**
     * The <code>TileCodecParameterList</code> object containing the
     * encoding parameters.
     */
    protected TileCodecParameterList paramList;

    /**
     * Constructs an <code>TileEncoderImpl</code>. An
     * <code>IllegalArgumentException</code> will be thrown if
     * <code>param</code>'s <code>getParameterListDescriptor()</code> method
     * does not return the same descriptor as that from the associated
     * <code>TileCodecDescriptor</code>'s 
     * <code>getParameterListDescriptor</code> method for the "tileEncoder" 
     * registry mode. 
     *
     * <p> If param is null, then the default parameter list for encoding
     * as defined by the associated <code>TileCodecDescriptor</code>'s 
     * <code>getDefaultParameters()</code> method will be used for encoding.
     * If this too is null, an <code>IllegalArgumentException</code> will
     * be thrown if the <code>ParameterListDescriptor</code> associated
     * with the associated <code>TileCodecDescriptor</code> for the
     * "tileEncoder" registry mode, reports that the number of parameters 
     * for this format is non-zero.
     *
     * @param formatName The name of the format.
     * @param output The <code>OutputStream</code> to write encoded data to.
     * @param param  The object containing the tile encoding parameters.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if output is null.
     * @throws IllegalArgumentException if param's getFormatName() method does
     * not return the same formatName as the one specified to this method.
     * @throws IllegalArgumentException if the ParameterListDescriptors 
     * associated with the param and the associated TileCodecDescriptor are
     * not equal.
     * @throws IllegalArgumentException if param does not have "tileEncoder"
     * as one of the valid modes that it supports. 
     */
    public TileEncoderImpl(String formatName,
			   OutputStream output,
			   TileCodecParameterList param) {

	// Cause a IllegalArgumentException to be thrown if formatName, output
	// is null
	if (formatName == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("TileCodecDescriptorImpl0"));
	}

	if (output == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("TileEncoderImpl0"));
	}

        TileCodecDescriptor tcd = 
	    TileCodecUtils.getTileCodecDescriptor("tileEncoder", formatName);

	// If param is null, get the default parameter list.
        if (param == null)
            param = tcd.getDefaultParameters("tileEncoder");

	if (param != null) {

	    // Check whether the formatName from the param is the same as the
	    // one supplied to this method.
	    if (param.getFormatName().equalsIgnoreCase(formatName) == false) {
		throw new IllegalArgumentException(
					  JaiI18N.getString("TileEncoderImpl1"));
	    }
	    
	    // Check whether the supplied parameterList supports the 
	    // "tileDecoder" mode.
	    if (param.isValidForMode("tileEncoder") == false) {
		throw new IllegalArgumentException(
					  JaiI18N.getString("TileEncoderImpl2"));
	    }

	    // Check whether the ParameterListDescriptors are the same.
	    if (param.getParameterListDescriptor().equals( 
			tcd.getParameterListDescriptor("tileEncoder")) == false)
            throw new IllegalArgumentException(JaiI18N.getString("TileCodec0"));

	} else {

	    // If the supplied parameterList is null and the default one is 
	    // null too, then check whether this format supports no parameters
	    ParameterListDescriptor pld = 
		tcd.getParameterListDescriptor("tileEncoder");

	    // If the PLD is not null and says that there are supposed to 
	    // be some parameters (numParameters returns non-zero value)
	    // throw an IllegalArgumentException
	    if (pld != null && pld.getNumParameters() != 0) {
		throw new IllegalArgumentException(
					JaiI18N.getString("TileDecoderImpl6"));
	    }
	}

	this.formatName = formatName;
	this.outputStream = output;
	this.paramList = param;
    }
    
    /**
     * Returns the format name of the encoding scheme.
     */
    public String getFormatName() {
	return formatName;
    }

    /**
     * Returns the current parameters as an instance of the
     * <code>TileCodecParameterList</code> interface.
     */
    public TileCodecParameterList getEncodeParameterList() {
	return paramList;
    }

    /** 
     * Returns the <code>OutputStream</code> to which the encoded data will
     * be written.
     */
    public OutputStream getOutputStream() {
        return outputStream;
    }

}
