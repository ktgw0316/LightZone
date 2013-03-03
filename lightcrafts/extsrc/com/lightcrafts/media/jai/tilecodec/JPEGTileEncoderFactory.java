/*
 * $RCSfile: JPEGTileEncoderFactory.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:57 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.tilecodec ;

import java.awt.image.SampleModel ;
import java.awt.image.DataBuffer ;
import java.io.OutputStream;
import java.util.Vector;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.ParameterListDescriptorImpl;
import com.lightcrafts.mediax.jai.remote.NegotiableCapability;
import com.lightcrafts.mediax.jai.remote.NegotiableNumericRange;
import com.lightcrafts.mediax.jai.remote.NegotiableCollection;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList ;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoder ;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoderFactory ;

/**
 * A factory for creating <code>JPEGTileEncoder</code>s.
 *
 * <p> This class stipulates that the capabilities of the
 * <code>TileEncoder</code> be specified by implementing the
 * <code>getEncodingCapability()</code> method.
 *
 * @see com.lightcrafts.mediax.jai.remote.NegotiableCapability
 */
public class JPEGTileEncoderFactory implements TileEncoderFactory {

    /**
     * Creates a <code>TileEncoder</code> capable of encoding a
     * <code>Raster</code> with the specified <code>SampleModel</code>
     * using the specified <code>TileCodecParameterList</code>
     * containing the encoding parameters to the given <code>OutputStream</code>.
     *
     * <p> This method can return null if the <code>TileEncoder</code> is not
     * capable of producing output for the given set of parameters.
     * For example, if a <code>TileEncoder</code> is only capable of dealing
     * with a <code>PixelInterleavedSampleModel</code>, and the supplied
     * <code>SampleModel</code> is not an instance of
     * <code>PixelInterleavedSampleModel</code>, null should be
     * returned. The supplied <code>SampleModel</code> should be used to
     * decide whether it can be encoded by this class, and is not needed
     * to actually construct a <code>TileEncoder</code>.
     *
     * <p> If the supplied <code>TileCodecParameterList</code> is null,
     * a default <code>TileCodecParameterList</code> from the
     * <code>TileCodecDescriptor</code> will be used to create the encoder.
     *
     * <p>Exceptions thrown by the <code>TileEncoder</code>
     * will be caught by this method and will not be propagated.
     *
     * @param output      The <code>OutputStream</code> to write the encoded
     *                    data to.
     * @param paramList   The <code>TileCodecParameterList</code> containing
     *                    the encoding parameters.
     * @param sampleModel The <code>SampleModel</code> of the encoded
     *                    <code>Raster</code>s.
     * @throws IllegalArgumentException if output is null.
     */
    public TileEncoder createEncoder(OutputStream output,
				     TileCodecParameterList paramList,
				     SampleModel sampleModel) {
	if(output == null)
	    throw new IllegalArgumentException(JaiI18N.getString("TileEncoder0"));
	int nbands = sampleModel.getNumBands() ;
	if(nbands != 1 && nbands != 3 && nbands != 4)
	    throw new IllegalArgumentException(
		JaiI18N.getString("JPEGTileEncoder0")) ;

	if(sampleModel.getDataType() != DataBuffer.TYPE_BYTE)
	    throw new IllegalArgumentException(
		JaiI18N.getString("JPEGTileEncoder1")) ;

	return new JPEGTileEncoder(output, paramList) ;
    }

    /**
     * Returns the capabilities of this <code>TileEncoder</code> as a
     * <code>NegotiableCapability</code>.
     */
    public NegotiableCapability getEncodeCapability() {

	Vector generators = new Vector();
	generators.add(JPEGTileEncoderFactory.class);

	ParameterListDescriptor jpegPld = 
	    JAI.getDefaultInstance().getOperationRegistry().getDescriptor("tileEncoder", "jpeg").getParameterListDescriptor("tileEncoder");

	Class paramClasses[] = {
	    com.lightcrafts.mediax.jai.remote.NegotiableNumericRange.class,
	    com.lightcrafts.mediax.jai.remote.NegotiableCollection.class,
	    // XXX How should a negotiable be created to represent int arrays
	    // integer array,  horizontal subsampling
	    // integer array,  vertical subsampling
	    // integer array,  quantization table mapping
	    // integer array,  quantizationTable0
	    // integer array,  quantizationTable1
	    // integer array,  quantizationTable2
	    // integer array,  quantizationTable3
	    com.lightcrafts.mediax.jai.remote.NegotiableNumericRange.class,
	    com.lightcrafts.mediax.jai.remote.NegotiableCollection.class,
	    com.lightcrafts.mediax.jai.remote.NegotiableCollection.class,
	    com.lightcrafts.mediax.jai.remote.NegotiableCollection.class
	};

	String paramNames[] = {
	    "quality",
	    "qualitySet",
	    "restartInterval",
	    "writeImageInfo",
	    "writeTableInfo",
	    "writeJFIFHeader"
	};

	// A collection containing the valid values for a boolean valued
	// parameters
	Vector v = new Vector();
	v.add(new Boolean(true));
	v.add(new Boolean(false));
	NegotiableCollection negCollection = new NegotiableCollection(v);

	NegotiableNumericRange nnr1 = 
	    new NegotiableNumericRange(
				  jpegPld.getParamValueRange(paramNames[0]));

	NegotiableNumericRange nnr2 = 
	    new NegotiableNumericRange(
				  jpegPld.getParamValueRange(paramNames[2]));

	// The default values
	Object defaults[] = {
	    nnr1, 
	    negCollection, 
	    nnr2,
	    negCollection,
	    negCollection,
	    negCollection
	};

	NegotiableCapability encodeCap =
	    new NegotiableCapability("tileCodec",
				     "jpeg",
				     generators,
				     new ParameterListDescriptorImpl(
							  null, // descriptor
							  paramNames,
							  paramClasses,
							  defaults,
							  null), // validValues
				     false); // a non-preference

	// Set the Negotiables representing the valid values on the capability
	encodeCap.setParameter(paramNames[0], nnr1);
	encodeCap.setParameter(paramNames[1], negCollection);
	encodeCap.setParameter(paramNames[2], nnr2);
	encodeCap.setParameter(paramNames[3], negCollection);
	encodeCap.setParameter(paramNames[4], negCollection);
	encodeCap.setParameter(paramNames[5], negCollection);

	return encodeCap;
    }
}
