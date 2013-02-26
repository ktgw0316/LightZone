/*
 * $RCSfile: TileEncoderFactory.java,v $
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
import java.io.OutputStream;
import com.lightcrafts.mediax.jai.remote.NegotiableCapability;

/**
 * A factory for creating <code>TileEncoder</code>s.
 *
 * <p> This class stipulates that the capabilities of the 
 * <code>TileEncoder</code> be specified by implementing the
 * <code>getEncodingCapability()</code> method. 
 *
 * @see com.lightcrafts.mediax.jai.remote.NegotiableCapability
 *
 * @since JAI 1.1
 */
public interface TileEncoderFactory {

    /**
     * Creates a <code>TileEncoder</code> capable of encoding a 
     * <code>Raster</code> with the specified <code>SampleModel</code>
     * using the encoding parameters specified via the
     * <code>TileCodecParameterList</code>, to the given
     * <code>OutputStream</code>.
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
     * @param sampleModel The <code>SampleModel</code> of the
     *                    <code>Raster</code> to be encoded.
     *
     * @throws IllegalArgumentException if output is null.
     * @throws IllegalArgumentException if sampleModel is null.
     */
    TileEncoder createEncoder(OutputStream output, 
			      TileCodecParameterList paramList,
			      SampleModel sampleModel);

    /** 
     * Returns the capabilities of this <code>TileEncoder</code> as a
     * <code>NegotiableCapability</code>.
     *
     * @see com.lightcrafts.mediax.jai.remote.NegotiableCapability
     */
    NegotiableCapability getEncodeCapability();
}
