/*
 * $RCSfile: GZIPTileDecoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:56 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.tilecodec;

import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.Raster;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoderImpl;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * A concrete implementation of the <code>TileDecoderImpl</code> class
 * for the gzip tile codec.
 */
public class GZIPTileDecoder extends TileDecoderImpl {
    /**
     * Constructs a <code>GZIPTileDecoder</code>.
     * <code>GZIPTileDecoder</code> may throw a
     * <code>IllegalArgumentException</code> if <code>param</code>'s
     * <code>getParameterListDescriptor()</code> method does not return
     * the same descriptor as that from the associated
     * <code>TileCodecDescriptor</code>'s
     * <code>getParameterListDescriptor</code> method for the "tileDecoder"
     * registry mode.
     *
     * <p> If param is null, then the default parameter list for decoding
     * as defined by the associated <code>TileCodecDescriptor</code>'s
     * <code>getDefaultParameters()</code> method will be used for decoding.
     *
     * @param input The <code>InputStream</code> to decode data from.
     * @param param  The object containing the tile decoding parameters.
     * @throws IllegalArgumentException if input is null.
     * @throws IllegalArgumentException if param is not appropriate.
     */
    public GZIPTileDecoder(InputStream input, TileCodecParameterList param) {
	super("gzip", input, param);
    }

    /**
     * Returns a <code>Raster</code> that contains the decoded contents
     * of the <code>InputStream</code> associated with this
     * <code>TileDecoder</code>.
     *
     * <p>This method can perform the decoding correctly only when
     * <code>includesLocationInfo()</code> returns true.
     *
     * @throws IOException if an I/O error occurs while reading from the
     * associated InputStream.
     * @throws IllegalArgumentException if the associated
     * TileCodecDescriptor's includesLocationInfo() returns false.
     */
    public Raster decode() throws IOException{

	ObjectInputStream ois
	    = new ObjectInputStream(new GZIPInputStream(inputStream));

	try {
	    Object object = ois.readObject();
	    return TileCodecUtils.deserializeRaster(object);
	}
	catch (ClassNotFoundException e) {
            ImagingListener listener =
                ImageUtil.getImagingListener((RenderingHints)null);
            listener.errorOccurred(JaiI18N.getString("ClassNotFound"),
                                   e, this, false);

//            e.printStackTrace();
	    return null;
	}
	finally {
	    ois.close();
	}
    }

    public Raster decode(Point location) throws IOException{
        return decode();
    }
}

