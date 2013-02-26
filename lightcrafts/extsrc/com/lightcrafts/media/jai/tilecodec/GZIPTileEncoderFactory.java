/*
 * $RCSfile: GZIPTileEncoderFactory.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:57 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.tilecodec ;

import java.awt.image.SampleModel;
import java.io.OutputStream;
import java.util.Vector;
import com.lightcrafts.mediax.jai.ParameterListDescriptorImpl;
import com.lightcrafts.mediax.jai.remote.NegotiableCapability;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList ;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoder ;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoderFactory ;

/**
 * A factory for creating <code>GZIPTileEncoder</code>s.
 *
 * <p> This class stipulates that the capabilities of the 
 * <code>TileEncoder</code> be specified by implementing the
 * <code>getEncodingCapability()</code> method. 
 *
 * @see com.lightcrafts.mediax.jai.remote.NegotiableCapability
 */
public class GZIPTileEncoderFactory implements TileEncoderFactory {

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
	    throw new IllegalArgumentException( JaiI18N.getString("TileEncoder0") );

	return new GZIPTileEncoder(output, paramList) ;
    }

    /** 
     * Returns the capabilities of this <code>TileEncoder</code> as a
     * <code>NegotiableCapability</code>.
     */
    public NegotiableCapability getEncodeCapability() {

	Vector generators = new Vector();
	generators.add(GZIPTileEncoderFactory.class);

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
