/*
 * $RCSfile: JPEGTileDecoderFactory.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:57 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.tilecodec ;

import java.io.InputStream;
import java.util.Vector;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.ParameterListDescriptorImpl;
import com.lightcrafts.mediax.jai.remote.NegotiableCapability;
import com.lightcrafts.mediax.jai.remote.NegotiableNumericRange;
import com.lightcrafts.mediax.jai.remote.NegotiableCollection;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoder;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoderFactory;

/**
 * A factory for creating <code>JPEGTileDecoder</code>s.
 *
 * <p> This class stipulates that the capabilities of the 
 * <code>TileDecoder</code> be specified by implementing the
 * <code>getDecodingCapability()</code> method. 
 *
 * @see com.lightcrafts.mediax.jai.remote.NegotiableCapability
 */
public class JPEGTileDecoderFactory implements TileDecoderFactory {
    
    /** 
     * Creates a <code>JPEGTileDecoder</code> capable of decoding the encoded 
     * data from the given <code>InputStream</code> using the specified
     * <code>TileCodecParameterList</code> containing the decoding
     * parameters to be used.
     *
     * <p> This method can return null if the <code>TileDecoder</code> is not
     * capable of producing output for the given set of parameters.  
     * For example, if a <code>TileDecoder</code> is only capable of dealing
     * with a jpeg quality factor of 0.5, and the associated
     * <code>TileCodecParameterList</code> specifies a quality factor of 0.75,
     * null should be returned.
     *
     * <p>It is recommended that the data in the supplied 
     * <code>InputStream</code> not be used as a factor in determining
     * whether this <code>InputStream</code> can be successfully decoded,
     * unless the supplied <code>InputStream</code> is known to be rewindable
     * (i.e. its <code>markSupported()</code> method returns true or it has
     * additional functionality that allows backward seeking). It is required
     * that <code>the</code> InputStream contain the same data on 
     * returning from this method as before this method was called.
     * In other words, the <code>InputStream</code> should only be used as a
     * discriminator if it can be rewound to its starting position
     * before returning from this method. Note that wrapping the
     * incoming <code>InputStream</code> in a <code>PushbackInputStream</code>
     * and then rewinding the <code>PushbackInputStream</code> before returning
     * does not rewind the wrapped <code>InputStream</code>.
     *
     * <p> If the supplied <code>TileCodecParameterList</code> is null,
     * a default <code>TileCodecParameterList</code> from the
     * <code>TileCodecDescriptor</code> will be used to create the decoder.
     *
     * <p> Exceptions thrown by the <code>TileDecoder</code> will be
     * caught by this method and will not be propagated.
     *
     * @param input The <code>InputStream</code> containing the encoded data
     *              to decode.
     * @param param The parameters to be be used in the decoding process.
     * @throws IllegalArgumentException if input is null.
     */
    public TileDecoder createDecoder(InputStream input, 
	TileCodecParameterList param) {

        if(input == null)
	    throw new IllegalArgumentException(JaiI18N.getString("TileDecoder0"));

	return new JPEGTileDecoder(input, param ) ;
    }

    /** 
     * Returns the capabilities of this <code>TileDecoder</code> as a
     * <code>NegotiableCapability</code>.
     */
    public NegotiableCapability getDecodeCapability() {
	
	Vector generators = new Vector();
	generators.add(JPEGTileDecoderFactory.class);

	ParameterListDescriptor jpegPld = JAI.getDefaultInstance().getOperationRegistry().getDescriptor("tileDecoder", "jpeg").getParameterListDescriptor("tileDecoder");
	
	Class paramClasses[] = {
	    NegotiableNumericRange.class,
	    NegotiableCollection.class,
	    // XXX Find a way to create a negotiable representing integer
	    // arrays
	    // integer array,  horizontal subsampling
	    // integer array,  vertical subsampling
	    // integer array,  quantization table mapping
	    // integer array,  quantizationTable0
	    // integer array,  quantizationTable1
	    // integer array,  quantizationTable2
	    // integer array,  quantizationTable3
	    NegotiableNumericRange.class,
	    NegotiableCollection.class,
	    NegotiableCollection.class,
	    NegotiableCollection.class
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

	NegotiableCapability decodeCap = 
	    new NegotiableCapability("tileCodec",
				     "jpeg",
				     generators,
				     new ParameterListDescriptorImpl(
							 null, //descriptor
							 paramNames,
							 paramClasses,
							 defaults,
							 null), // validValues
				     false); // non-preference

	// Set the Negotiables representing the valid values on the capability
	decodeCap.setParameter(paramNames[0], nnr1);
	decodeCap.setParameter(paramNames[1], negCollection);
	decodeCap.setParameter(paramNames[2], nnr2);
	decodeCap.setParameter(paramNames[3], negCollection);
	decodeCap.setParameter(paramNames[4], negCollection);
	decodeCap.setParameter(paramNames[5], negCollection);

	return decodeCap;
    }
}
