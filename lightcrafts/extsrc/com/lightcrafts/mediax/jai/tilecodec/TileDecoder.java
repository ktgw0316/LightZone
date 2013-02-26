/*
 * $RCSfile: TileDecoder.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:55 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.tilecodec;

import java.awt.Point;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;

/**
 * An interface describing objects that transform an <code>InputStream</code>
 * into a <code>Raster</code>.
 *
 * <p>This interface is designed to allow decoding of formats that
 * include information about the format of the encoded tile as well as ones
 * that don't. In order to create a <code>Raster</code>, at the very least,
 * a <code>Point</code> specifying the top left corner of the 
 * <code>Raster</code>, a <code>SampleModel</code> specifying the data layout
 * and a <code>DataBuffer</code> with the decoded pixel data are
 * needed. The <code>DataBuffer</code> can be created from the  
 * information from the <code>SampleModel</code> and the decoded data. 
 * Therefore the absolute minimum information that is required in order to
 * create a <code>Raster</code> on decoding (aside from the decoded data 
 * itself) is a <code>Point</code> specifying the top left corner of the
 * <code>Raster</code> and a <code>SampleModel</code> specifying the data
 * layout. The formats that do include this information should return true
 * from the <code>includesSampleModelInfo()</code> and 
 * <code>includesLocationInfo()</code> from the associated 
 * <code>TileCodecDescriptor</code> if they include information needed to
 * create a <code>SampleModel</code> and information needed to
 * create the <code>Point</code> respectively. The formats that do not
 * include this information in the encoded stream should return false. The
 * <code>TileCodecParameterList</code> providing the decoding parameters
 * will in this case be expected to contain a parameter named "sampleModel"
 * with a non-null <code>SampleModel</code> as its value. This 
 * <code>SampleModel</code> will be used to create the decoded 
 * <code>Raster</code>.
 *
 * <p> The formats that return true from <code>includesSampleModelInfo()</code>
 * should use the <code>decode()</code> method to cause the decoding to take
 * place, the ones that return false should specify the <code>Point</code>
 * location to the decoding process by using the <code>decode(Point)</code>
 * method. Similarly the <code>SampleModel</code> must be specified as a
 * parameter with a non-null value on the <code>TileCodecParameterList</code>
 * passed to this <code>TileDecoder</code> if
 * <code>includesSampleModelInfo()</code> returns false. It is expected that
 * the <code>SampleModel</code> specified in the parameter list is the
 * <code>SampleModel</code> of the encoded tiles, in order to get a
 * decoded <code>Raster</code> that is equivalent to the one encoded. If the
 * <code>SampleModel</code> specified through the parameter list is different 
 * from those of the encoded tiles, the result of decoding is undefined.
 *
 * <p> If <code>includesSampleModelInfo()</code> returns true, the
 * <code>SampleModel</code> (if present) on the
 * <code>TileCodecParameterList</code> is ignored.
 *
 * @see TileCodecDescriptor
 * @see TileEncoder
 *
 * @since JAI 1.1
 */
public interface TileDecoder {

    /**
     * Returns the name of the format.
     */
    String getFormatName();

    /**
     * Returns the current parameters as an instance of the
     * <code>TileCodecParameterList</code> interface.
     */
    TileCodecParameterList getDecodeParameterList();

    /** 
     * Returns the <code>InputStream</code> containing the encoded data.
     */
    InputStream getInputStream();

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
    Raster decode() throws IOException;

    /**
     * Returns a <code>Raster</code> that contains the decoded contents 
     * of the <code>InputStream</code> associated with this 
     * <code>TileDecoder</code>. 
     *
     * <p>This method should be used when <code>includesLocationInfo()</code>
     * returns false. If <code>includesLocationInfo()</code> returns true, then
     * the supplied <code>Point</code> is ignored.
     *
     * @param location The <code>Point</code> specifying the upper
     *                 left corner of the Raster. 
     * @throws IOException if an I/O error occurs while reading from the
     * associated InputStream.
     * @throws IllegalArgumentException if includesLocationInfo() returns false 
     * and location is null.
     */
    Raster decode(Point location) throws IOException;
}



