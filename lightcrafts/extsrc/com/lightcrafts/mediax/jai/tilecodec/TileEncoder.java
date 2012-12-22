/*
 * $RCSfile: TileEncoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:56 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.tilecodec;

import java.awt.image.Raster;
import java.io.IOException;
import java.io.OutputStream;

/**
 * An interface describing objects that transform a <code>Raster</code>
 * into an <code>OutputStream</code>.
 *
 * If the <code>TileCodecDescriptor</code> for this format returns true from
 * its <code>includesSampleModelInfo()</code> and 
 * <code>includesLocationInfo()</code> methods, then the encoder must encode
 * this information in the encoded tiles. If these methods return false, then
 * this information (needed to create a <code>Raster</code> on decoding) must
 * be provided to the <code>TileDecoder</code> by setting the 
 * <code>SampleModel</code> on the <code>TileCodecParameterList</code> supplied
 * to the <code>TileDecoder</code> and supplying the tile upper left corner
 * location to the <code>TileDecoder</code> via the <code>TileDecoder</code>'s
 * <code>decode(Point location)</code> method. The <code>SampleModel</code>
 * value set on the <code>TileCodecParameterList</code> for the
 * <code>TileDecoder</code> should be the same as that of the 
 * <code>Raster</code> to be encoded, in order to get a <code>Raster</code>
 * on decoding that is equivalent to the one being encoded.
 *
 * @see TileCodecDescriptor
 * @see TileDecoder
 *
 * @since JAI 1.1
 */
public interface TileEncoder {

    /**
     * Returns the format name of the encoding scheme.
     */
    String getFormatName();

    /**
     * Returns the current parameters as an instance of the
     * <code>TileCodecParameterList</code> interface.
     */
    TileCodecParameterList getEncodeParameterList();

    /** 
     * Returns the <code>OutputStream</code> to which the encoded data
     * will be written.
     */
    public OutputStream getOutputStream();

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
    public void encode(Raster ras) throws IOException;
}
