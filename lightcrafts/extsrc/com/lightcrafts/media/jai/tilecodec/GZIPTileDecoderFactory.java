/*
 * $RCSfile: GZIPTileDecoderFactory.java,v $
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
import com.lightcrafts.mediax.jai.ParameterListDescriptorImpl;
import com.lightcrafts.mediax.jai.remote.NegotiableCapability;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoder ;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoderFactory ;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList ;

/**
 * A factory for creating <code>GZIPTileDecoder</code>s.
 *
 * <p> This class stipulates that the capabilities of the 
 * <code>TileDecoder</code> be specified by implementing the
 * <code>getDecodingCapability()</code> method. 
 *
 * @see com.lightcrafts.mediax.jai.remote.NegotiableCapability
 */
public class GZIPTileDecoderFactory implements TileDecoderFactory {
    
    /** 
     * Creates a <code>GZIPTileDecoder</code> capable of decoding the encoded 
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

	return new GZIPTileDecoder(input, param) ;
    }

    /** 
     * Returns the capabilities of this <code>TileDecoder</code> as a
     * <code>NegotiableCapability</code>.
     */
    public NegotiableCapability getDecodeCapability() {

	Vector generators = new Vector();
	generators.add(GZIPTileDecoderFactory.class);

	return new NegotiableCapability("tileCodec",
					"gzip",
					generators,
					new ParameterListDescriptorImpl(null,
									null,
									null,
									null,
									null),
					false);
    }
}
