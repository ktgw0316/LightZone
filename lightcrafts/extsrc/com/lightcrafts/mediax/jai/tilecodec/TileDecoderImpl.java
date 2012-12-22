/*
 * $RCSfile: TileDecoderImpl.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:56 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.tilecodec;

import java.awt.image.SampleModel;
import java.io.InputStream;
import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecDescriptor;
import com.lightcrafts.media.jai.tilecodec.TileCodecUtils;

/**
 * A partial implementation of the <code>TileDecoder</code> interface
 * useful for subclassing.
 *
 * @since JAI 1.1
 */
public abstract class TileDecoderImpl implements TileDecoder {

    /**
     * The name of the format.
     */
    protected String formatName;

    /**
     * The <code>InputStream</code> containing the encoded data to decode.
     */
    protected InputStream inputStream;

    /**
     * The <code>TileCodecParameterList</code> object containing the
     * decoding parameters. 
     */
    protected TileCodecParameterList paramList;

    /**
     * Constructs a <code>TileDecoderImpl</code>. An
     * <code>IllegalArgumentException</code> will be thrown if
     * <code>param</code>'s <code>getParameterListDescriptor()</code> 
     * method does not return the same descriptor as that from
     * the associated <code>TileCodecDescriptor</code>'s 
     * <code>getParameterListDescriptor</code> method for the "tileDecoder" 
     * registry mode. 
     *
     * <p> If param is null, then the default parameter list for decoding
     * as defined by the associated <code>TileCodecDescriptor</code>'s 
     * <code>getDefaultParameters()</code> method will be used for decoding.
     * If this too is null, an <code>IllegalArgumentException</code> will
     * be thrown if the <code>ParameterListDescriptor</code> associated
     * with the associated <code>TileCodecDescriptor</code> for the
     * "tileDecoder" registry mode, reports that the number of parameters
     * for this format is non-zero.
     *
     * @param formatName The name of the format.
     * @param input The <code>InputStream</code> to decode data from.
     * @param param  The object containing the tile decoding parameters.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if input is null.
     * @throws IllegalArgumentException if param's getFormatName() method does
     * not return the same formatName as the one specified to this method.
     * @throws IllegalArgumentException if the ParameterListDescriptor 
     * associated with the param and the associated TileCodecDescriptor are
     * not equal.
     * @throws IllegalArgumentException if param does not have "tileDecoder"
     * as one of the valid modes that it supports.
     * @throws IllegalArgumentException if the associated TileCodecDescriptor's
     * includesSampleModelInfo() returns false and a non-null value for the
     * "sampleModel" parameter is not supplied in the supplied parameter list.
     */
    public TileDecoderImpl(String formatName,
			   InputStream input, 
			   TileCodecParameterList param) {

	// Cause IllegalArgumentException to be thrown if formatName,
	// input is null
	if (formatName == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("TileCodecDescriptorImpl0"));
	}

	if (input == null) {
	    throw new IllegalArgumentException(
				JaiI18N.getString("TileDecoderImpl0"));
	}

        TileCodecDescriptor tcd = 
	    TileCodecUtils.getTileCodecDescriptor("tileDecoder", formatName);

	// If param is null, get the default parameter list.
        if (param == null)
            param = tcd.getDefaultParameters("tileDecoder");

	if (param != null) {

	    // Check whether the formatName from the param is the same as the
	    // one supplied to this method.
	    if (param.getFormatName().equalsIgnoreCase(formatName) == false) {
		throw new IllegalArgumentException(
					  JaiI18N.getString("TileDecoderImpl1"));
	    }
	    
	    // Check whether the supplied parameterList supports the 
	    // "tileDecoder" mode.
	    if (param.isValidForMode("tileDecoder") == false) {
		throw new IllegalArgumentException(
					  JaiI18N.getString("TileDecoderImpl2"));
	    }

	    // Check whether the ParameterListDescriptors are the same.
	    if (param.getParameterListDescriptor().equals(
			tcd.getParameterListDescriptor("tileDecoder")) == false)
            throw new IllegalArgumentException(JaiI18N.getString("TileCodec0"));

	    SampleModel sm = null;

	    // Check whether a non-null samplemodel value is needed
	    if (tcd.includesSampleModelInfo() == false) {
		try {
		    sm = (SampleModel)param.getObjectParameter("sampleModel");
		} catch (IllegalArgumentException iae) {
		    // There is no parameter named sampleModel defined on the
		    // supplied parameter list
		    throw new IllegalArgumentException(
					  JaiI18N.getString("TileDecoderImpl3"));
		}

		if (sm == null || 
		    sm == ParameterListDescriptor.NO_PARAMETER_DEFAULT) {
		    
		    if (tcd.getParameterListDescriptor("tileDecoder").
			        getParamDefaultValue("sampleModel") == null) {
			// If a non-null value was not set on the parameter list
			// and wasn't available thru the descriptor either 
			throw new IllegalArgumentException(
					  JaiI18N.getString("TileDecoderImpl4"));
		    }
		}
	    }

	} else {

	    // If the supplied parameterList is null and the default one is 
	    // null too, then check whether this format supports no parameters
	    ParameterListDescriptor pld = 
		tcd.getParameterListDescriptor("tileDecoder");

	    // Check whether a non-null samplemodel value is needed
	    if (tcd.includesSampleModelInfo() == false) {
		// SampleModel must be specified via the parameter list
		throw new IllegalArgumentException(
					  JaiI18N.getString("TileDecoderImpl5"));
	    }

	    // If the PLD is not null and says that there are supposed to 
	    // be some parameters (numParameters returns non-zero value)
	    // throw an IllegalArgumentException
	    if (pld != null && pld.getNumParameters() != 0) {
		throw new IllegalArgumentException(
					JaiI18N.getString("TileDecoderImpl6"));
	    }
	}

	this.formatName = formatName;
	this.inputStream = input;
	this.paramList = param;
    }
    
    /**
     * Returns the format name.
     */
    public String getFormatName() {
	return formatName;
    }

    /**
     * Returns the current parameters as an instance of the
     * <code>TileCodecParameterList</code> interface.
     */
    public TileCodecParameterList getDecodeParameterList() {
	return paramList;
    }

    /**
     * Returns the <code>InputStream</code> associated with this 
     * <code>TileDecoder</code>.
     */
    public InputStream getInputStream() {
        return inputStream;
    } 
}

