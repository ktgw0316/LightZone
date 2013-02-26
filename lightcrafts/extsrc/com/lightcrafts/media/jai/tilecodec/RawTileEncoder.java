/*
 * $RCSfile: RawTileEncoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:58 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.tilecodec ;

import java.awt.image.Raster;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList ;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoderImpl ;

/**
 * A concrete implementation of the <code>TileEncoderImpl</code> class
 * for the raw tile codec.
 */
public class RawTileEncoder extends TileEncoderImpl {

    /**
     * Constructs an <code>RawTileEncoder</code>. Concrete implementations
     * of <code>TileEncoder</code> may throw an
     * <code>IllegalArgumentException</code> if the
     * <code>param</code>'s <code>getParameterListDescriptor()</code> method
     * does not return the same descriptor as that from the associated
     * <code>TileCodecDescriptor</code>'s 
     * <code>getParameterListDescriptor</code> method for the "tileEncoder" 
     * registry mode. 
     *
     * <p> If param is null, then the default parameter list for encoding
     * as defined by the associated <code>TileCodecDescriptor</code>'s 
     * <code>getDefaultParameters()</code> method will be used for encoding.
     *
     * @param output The <code>OutputStream</code> to write encoded data to.
     * @param param  The object containing the tile encoding parameters.
     * @throws IllegalArgumentException if param is not the appropriate 
     * Class type.
     * @throws IllegalArgumentException is output is null.
     */
    public RawTileEncoder(OutputStream output, TileCodecParameterList param) {
        super("raw", output, param) ;
    }

    /**
     * Encodes a <code>Raster</code> and writes the output
     * to the <code>OutputStream</code> associated with this 
     * <code>TileEncoder</code>.
     *
     * @param ras the <code>Raster</code> to encode.
     * @throws IOException if an I/O error occurs while writing to the 
     * OutputStream.
     * @throws IllegalArgumentException if ras is null.
     */
    public void encode(Raster ras) throws IOException {
	if(ras == null)
	    throw new IllegalArgumentException(
		JaiI18N.getString("TileEncoder1")) ;

	ObjectOutputStream oos = new ObjectOutputStream(outputStream) ;

	Object object = TileCodecUtils.serializeRaster(ras) ;

	oos.writeObject(object) ;
	oos.close() ;
    }
}
