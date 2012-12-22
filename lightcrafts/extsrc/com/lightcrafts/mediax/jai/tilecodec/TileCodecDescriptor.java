/*
 * $RCSfile: TileCodecDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:55 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.tilecodec;

import java.awt.image.SampleModel;
import com.lightcrafts.mediax.jai.RegistryElementDescriptor;

/**
 * A class to describe a particular tile codec format. The <code>getName</code>
 * method of <code>RegistryElementDescriptor</code> should be implemented 
 * to return the name of the format in an implementation of this interface.
 * This name is also the String with which this
 * <code>TileCodecDescriptor</code> is associated in the 
 * <code>OperationRegistry</code>. There are two 
 * complemetary modes that TileCodecs are expected to function in, the
 * decoding mode specified by the "tileDecoder" String and the encoding
 * mode specified by the "tileEncoder" String. It is not recommended that
 * separate classes be used to implement the different modes, but if this
 * is done, then <code>includesSampleModelInfo()</code> and 
 * <code>includesLocationInfo()</code> methods must return the same values
 * from both the implementing classes.
 *
 * <p> In order to successfully decode an encoded tile data stream into a
 * decoded <code>Raster</code>, at the very least, a <code>Point</code>
 * specifying the top left corner of the <code>Raster</code>, a
 * <code>SampleModel</code> specifying the data layout described minimally
 * by the dataType, number of bands, width and height and a
 * <code>DataBuffer</code> with the decoded pixel data are needed. The
 * <code>DataBuffer</code> can be created from the information from the
 * <code>SampleModel</code> and the decoded data. Therefore the absolute
 * minimum information that is required in order to create a
 * <code>Raster</code> upon decoding (aside from the decoded data itself)
 * is the <code>Point</code> specifying the top left corner of the
 * <code>Raster</code>, the <code>SampleModel</code> specifying the data
 * layout. Some formats include this information about the layout of the
 * tile while others don't. The formats that do include this information
 * needed to create a <code>SampleModel</code> and a <code>Point</code>
 * should return true from the <code>includesSampleModelInfo()</code> and
 * <code>includesLocationInfo()</code> methods respectively. The formats 
 * that do not include this information in the encoded stream should return
 * false. For decoding, the <code>TileCodecParameterList</code> providing the
 * decoding parameters will in this case be expected to contain a parameter
 * named "sampleModel" with a non-null <code>SampleModel</code> as its value. 
 * This <code>SampleModel</code> will be used to create the decoded
 * <code>Raster</code> and is expected to be the same as the
 * <code>SampleModel</code> of the tiles to be encoded.
 *
 * <p> All <code>String</code>s are treated in a case-retentive and 
 * case-insensitive manner.
 *
 * @see TileDecoder
 * @see TileEncoder
 * @since JAI 1.1
 */
public interface TileCodecDescriptor extends RegistryElementDescriptor {

    /**
     * Returns true if the format encodes layout information generally
     * specified via the <code>SampleModel</code> in the encoded data stream.
     */
    boolean includesSampleModelInfo();

    /**
     * Returns true if the format encodes in the data stream the location of 
     * the <code>Raster</code> with respect to its enclosing image.
     */
    boolean includesLocationInfo();

    /**
     * Returns the default parameters for the specified modeName as an
     * instance of the <code>TileCodecParameterList</code>.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null.
     * @throws IllegalArgumentException if <code>modeName</code> is not
     * one of the modes valid for this descriptor, i.e those returned
     * from the getSupportedNames() method.
     */
    TileCodecParameterList getDefaultParameters(String modeName);

    /**
     * Returns the default parameters for the specified modeName as an
     * instance of the <code>TileCodecParameterList</code>, adding a 
     * "sampleModel" parameter with the specified value to the parameter
     * list.
     * 
     * <p> This method should be used when includesSampleModelInfo()
     * returns false. If includesSampleModelInfo() returns true, the
     * supplied <code>SampleModel</code> is ignored.
     *
     * <p> If a parameter named "sampleModel" exists in the default 
     * parameter list, the supplied <code>SampleModel</code> will override
     * the value associated with this default parameter.
     *
     * @param sm The <code>SampleModel</code> used to create the 
     *           default decoding parameter list.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null.
     * @throws IllegalArgumentException if <code>modeName</code> is not
     * one of the modes valid for this descriptor, i.e those returned
     * from the getSupportedNames() method.
     */
    TileCodecParameterList getDefaultParameters(String modeName, 
						SampleModel sm);

    /**
     * Returns a <code>TileCodecParameterList</code> valid for the 
     * specified modeName and compatible with the supplied
     * <code>TileCodecParameterList</code>.
     * For example, given a <code>TileCodecParameterList</code> used to
     * encode a tile with the modeName being specified as "tileDecoder", this
     * method will return a <code>TileCodecParameterList</code> sufficient
     * to decode that same encoded tile.
     *
     * @param modeName       The registry mode to return a valid parameter 
     *                       list for.
     * @param otherParamList The parameter list for which a compatible
     *                       parameter list for the specified modeName is
     *                       to be returned.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null.
     * @throws IllegalArgumentException if <code>modeName</code> is not
     * one of the modes valid for this descriptor, i.e those returned
     * from the getSupportedNames() method.
     */
    TileCodecParameterList getCompatibleParameters(String modeName, 
						   TileCodecParameterList 
                                                   otherParamList);
}

