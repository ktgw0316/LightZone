/*
 * $RCSfile: TileDecoderFactory.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:56 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.tilecodec;

import java.io.InputStream;
import com.lightcrafts.mediax.jai.remote.NegotiableCapability;

/**
 * A factory for creating <code>TileDecoder</code>s.
 *
 * <p> This class stipulates that the capabilities of the 
 * <code>TileDecoder</code> be specified by implementing the
 * <code>getDecodingCapability()</code> method. 
 *
 * @see com.lightcrafts.mediax.jai.remote.NegotiableCapability
 *
 * @since JAI 1.1
 */
public interface TileDecoderFactory {
    
    /** 
     * Creates a <code>TileDecoder</code> capable of decoding the encoded 
     * data from the given <code>InputStream</code> using the specified
     * <code>TileCodecParameterList</code> containing the decoding
     * parameters to be used.
     *
     * <p> This method can return null if the <code>TileDecoder</code> is 
     * not capable of producing output for the given set of parameters.  
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
     * that the <code>InputStream</code> contain the same data on 
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
    TileDecoder createDecoder(InputStream input, TileCodecParameterList param);

    /** 
     * Returns the capabilities of this <code>TileDecoder</code> as a
     * <code>NegotiableCapability</code>.
     *
     * @see com.lightcrafts.mediax.jai.remote.NegotiableCapability
     */
    NegotiableCapability getDecodeCapability();
}
