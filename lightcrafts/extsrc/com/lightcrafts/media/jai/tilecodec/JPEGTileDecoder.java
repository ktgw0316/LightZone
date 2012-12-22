/*
 * $RCSfile: JPEGTileDecoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:57 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.tilecodec;

import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.SampleModel;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.RasterFactory;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecDescriptor;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoderImpl;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.sun.image.codec.jpeg.JPEGDecodeParam;
import com.sun.image.codec.jpeg.JPEGImageDecoder;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGQTable;
import com.lightcrafts.media.jai.util.ImageUtil;
/**
 * A concrete implementation of the <code>TileDecoderImpl</code> class
 * for the jpeg tile codec.
 */
public class JPEGTileDecoder extends TileDecoderImpl {
    /* The associated TileCodecDescriptor */
    private TileCodecDescriptor tcd = null;

    /**
     * Constructs a <code>JPEGTileDecoder</code>.
     * <code>JPEGTileDecoder</code> may throw a
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
    public JPEGTileDecoder(InputStream input, TileCodecParameterList param) {
	super("jpeg", input, param);
        tcd = TileCodecUtils.getTileCodecDescriptor("tileDecoder", "jpeg");
    }

    /**
     * Returns a <code>Raster</code> that contains the decoded contents
     * of the <code>InputStream</code> associated with this
     * <code>TileDecoder</code>.
     *
     * <p>This method can perform the decoding correctly only when
     * <code>includesLocationInfoInfo()</code> returns true.
     *
     * @throws IOException if an I/O error occurs while reading from the
     * associated InputStream.
     * @throws IllegalArgumentException if the associated
     * TileCodecDescriptor's includesLocationInfoInfo() returns false.
     */
    public Raster decode() throws IOException{
	if (!tcd.includesLocationInfo())
	    throw new IllegalArgumentException(
		JaiI18N.getString("JPEGTileDecoder0") );
	return decode(null);
    }

    public Raster decode(Point location) throws IOException{
	SampleModel sm = null;
	byte[] data = null;

        ObjectInputStream ois = new ObjectInputStream(inputStream);

        try {
	    // read the quality and qualitySet from the stream
	    paramList.setParameter("quality", ois.readFloat());
	    paramList.setParameter("qualitySet", ois.readBoolean());
            sm = TileCodecUtils.deserializeSampleModel(ois.readObject());
	    location = (Point)ois.readObject();
            data = (byte[]) ois.readObject();
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

	ByteArrayInputStream bais = new ByteArrayInputStream(data);
        JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(bais);

        Raster ras = decoder.decodeAsRaster()
			.createTranslatedChild(location.x, location.y);
	extractParameters(decoder.getJPEGDecodeParam(),
			  ras.getSampleModel().getNumBands());

	// set the original sample model to the decoded raster
	if (sm != null) {
	    int minX = ras.getMinX();
	    int minY = ras.getMinY();
	    int h = ras.getHeight();
	    int w = ras.getWidth();
	    double[] buf = ras.getPixels(minX, minY, w, h, (double[])null);
	    ras = RasterFactory.createWritableRaster(sm,
						     new Point(minX, minY));
	    ((WritableRaster)ras).setPixels(minX, minY, w, h, buf);
	}
	return ras;
    }

    private void extractParameters(JPEGDecodeParam jdp, int bandNum) {

	// extract the horizontal subsampling rates
	int[] horizontalSubsampling = new int[bandNum];
	for (int i = 0; i < bandNum; i++)
	    horizontalSubsampling[i] = jdp.getHorizontalSubsampling(i);
	paramList.setParameter("horizontalSubsampling", horizontalSubsampling);

	// extract the vertical subsampling rates
	int[] verticalSubsampling = new int[bandNum];
	for (int i = 0; i < bandNum; i++)
	    verticalSubsampling[i] = jdp.getVerticalSubsampling(i);
	paramList.setParameter("verticalSubsampling", verticalSubsampling);

	// if the quality is not set, extract the quantization tables from
	// the stream; otherwise, define them with the default values.
	if (!paramList.getBooleanParameter("qualitySet"))
	    for (int i = 0; i < 4; i++) {
		JPEGQTable table = jdp.getQTable(i);
		paramList.setParameter("quantizationTable"+i,
		    (table == null) ? null : table.getTable());
	    }
	else {
	    ParameterListDescriptor pld
		= paramList.getParameterListDescriptor();
	    for (int i = 0; i < 4; i++) {
		paramList.setParameter("quantizationTable"+i,
		    pld.getParamDefaultValue("quantizationTable"+i));
	    }
	}

	// extract the quantizationTableMapping
	int[] quanTableMapping = new int[bandNum];
	for (int i = 0; i < bandNum; i++)
	    quanTableMapping[i] = jdp.getQTableComponentMapping(i);
	paramList.setParameter("quantizationTableMapping", quanTableMapping);

	// extract the writeTableInfo and writeImageInfo
	paramList.setParameter("writeTableInfo", jdp.isTableInfoValid());
	paramList.setParameter("writeImageInfo", jdp.isImageInfoValid());

	// extract the restart interval
	paramList.setParameter("restartInterval", jdp.getRestartInterval());

	// define writeJFIFHeader by examing the APP0_MARKER is set or not
	paramList.setParameter("writeJFIFHeader",
			       jdp.getMarker(JPEGDecodeParam.APP0_MARKER));
    }
}

