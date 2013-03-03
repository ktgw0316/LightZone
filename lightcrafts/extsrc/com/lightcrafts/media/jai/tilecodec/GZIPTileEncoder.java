/*
 * $RCSfile: GZIPTileEncoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:57 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.tilecodec ;

import java.awt.image.Raster;
import java.io.IOException;
import java.io.OutputStream;
import java.io.ObjectOutputStream;
import java.util.zip.GZIPOutputStream ;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoderImpl ;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList ;

/**
 * A concrete implementation of the <code>TileEncoderImpl</code> class
 * for the gzip tile codec.
 */
public class GZIPTileEncoder extends TileEncoderImpl {

    /**
     * Constructs an <code>GZIPTileEncoder</code>. 
     *
     * @param output The <code>OutputStream</code> to write encoded data to.
     * @param param  The object containing the tile encoding parameters.
     * @throws IllegalArgumentException if param is not the appropriate 
     * Class type.
     * @throws IllegalArgumentException is output is null.
     */
    public GZIPTileEncoder(OutputStream output, TileCodecParameterList param) {
        super("gzip", output, param) ;
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

	ObjectOutputStream oos 
	    = new ObjectOutputStream(new GZIPOutputStream(outputStream)) ;
	Object object = TileCodecUtils.serializeRaster(ras);
	oos.writeObject(object);
	oos.close();
    }
}
