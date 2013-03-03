/*
 * $RCSfile: RawTileCodecDescriptor.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:55 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.tilecodec ;

import java.awt.image.SampleModel ;
import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.ParameterListDescriptorImpl;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecDescriptorImpl ;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList ;

/**
 * This class is the descriptor for the "Raw" tile codec. The "Raw" tile
 * codec scheme involves simply writing out the raw pixel data to the
 * encoded stream. The format name for the raw tile codec is "raw". 
 * Since the encoded stream contains the <code>Raster</code>, it 
 * automatically contains the <code>SampleModel</code> and the tile's
 * upper left corner position. Therefore the
 * <code>includesSampleModelInfo()</code> and 
 * <code>includesLocationInfo()</code> methods in this descriptor return
 * true.
 *
 * <p> The "Raw" codec scheme does not support any parameters.
 *
 * <p><table border=1>
 * <caption>Resource List</caption>
 * <tr><th>Name</th>        <th>Value</th></tr>
 * <tr><td>Vendor</td>      <td>com.lightcrafts.media.jai</td></tr>
 * <tr><td>Description</td> <td>A descriptor to describe the lossless
 *                              "raw" codec scheme. </td></tr>
 * <tr><td>DocURL</td>      <td>http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/tilecodec/RawTileCodecDescriptor.html</td></tr>
 * <tr><td>Version</td>     <td>1.2</td></tr>
 * </table></p>
 *
 * <p><table border=1>
 * <caption>Parameter List</caption>
 * <tr><th>Name</th>          <th>Class Type</th>
 *                            <th>Default Value</th></tr>
 * </table></p>
 *
 * @since JAI 1.1
 */
public class RawTileCodecDescriptor extends TileCodecDescriptorImpl {

    private static ParameterListDescriptorImpl pld = 
        new ParameterListDescriptorImpl();

    /**
     * Creates a <code>RawTileCodecDescriptor</code>.
     */
    public RawTileCodecDescriptor( ) {
	super("raw", true, true) ;
    }

    /**
     * Returns a <code>TileCodecParameterList</code> valid for the 
     * specified modeName and compatible with the supplied
     * <code>TileCodecParameterList</code>. For example, given a
     * <code>TileCodecParameterList</code> used to encode a tile with
     * the modeName being specified as "tileDecoder", this method will return
     * a <code>TileCodecParameterList</code> sufficient to decode that
     * same tile. For the raw tile codec, no parameters are used. So null
     * will be returned for any valid modeName specified.
     *
     * @param modeName       The registry mode to return a valid parameter 
     *                       list for.
     * @param otherParamList The parameter list for which a compatible 
     *                       parameter list for the complementary modeName is
     *                       to be found.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null.
     * @throws IllegalArgumentException if <code>modeName</code> is not
     * one of the modes valid for this descriptor, i.e those returned
     * from the getSupportedNames() method.
     */
    public TileCodecParameterList getCompatibleParameters(
				       String modeName,
				       TileCodecParameterList otherParamList) {
	if (modeName == null)
	    throw new IllegalArgumentException(
				JaiI18N.getString("TileCodecDescriptorImpl1"));

	String validNames[] = getSupportedModes();
	boolean valid = false;

	for (int i=0; i<validNames.length; i++) {
	    if (modeName.equalsIgnoreCase(validNames[i])) {
		valid = true;
		break;
	    }
	}

	if (valid == false) {
	    throw new IllegalArgumentException(
					    JaiI18N.getString("TileCodec1"));
	}

	return null;
    }

    /**
     * Returns the default parameters for the specified modeName as an
     * instance of the <code>TileCodecParameterList</code>. For the 
     * raw tile codec, no parameters are used. So null will be
     * returned for any valid modeName specified.
     *
     * @param modeName       The registry mode to return a valid parameter 
     *                       list for.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null.
     * @throws IllegalArgumentException if <code>modeName</code> is not
     * one of the modes valid for this descriptor, i.e those returned
     * from the getSupportedNames() method.
     */
    public TileCodecParameterList getDefaultParameters(String modeName) {
	if (modeName == null)
	    throw new IllegalArgumentException(
				JaiI18N.getString("TileCodecDescriptorImpl1"));

	String validNames[] = getSupportedModes();
	boolean valid = false;

	for (int i=0; i<validNames.length; i++) {
	    if (modeName.equalsIgnoreCase(validNames[i])) {
		valid = true;
		break;
	    }
	}

	if (valid == false) {
	    throw new IllegalArgumentException(
					    JaiI18N.getString("TileCodec1"));
	}

	return null;
    }

    /**
     * Returns the default parameters for the specified modeName as an
     * instance of the <code>TileCodecParameterList</code>, adding a 
     * "sampleModel" parameter with the specified value to the parameter
     * list. For the raw tile codec, no parameters is used. So null will be
     * returned for any valid modeName specified.
     * 
     * <p> This method should be used when includesSampleModelInfo()
     * returns false. If includesSampleModelInfo() returns true, the
     * supplied <code>SampleModel</code> is ignored.
     *
     * <p> If a parameter named "sampleModel" exists in the default 
     * parameter list, the supplied SampleModel will override the value 
     * associated with this default parameter.
     *
     * @param modeName The registry mode to return a valid parameter list for.
     * @param sm   The <code>SampleModel</code> used to create the 
     *             default decoding parameter list.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null.
     * @throws IllegalArgumentException if <code>modeName</code> is not
     * one of the modes valid for this descriptor, i.e those returned
     * from the getSupportedNames() method.
     */
    public TileCodecParameterList getDefaultParameters(String modeName, 
						       SampleModel sm){
	if (modeName == null)
	    throw new IllegalArgumentException(
				JaiI18N.getString("TileCodecDescriptorImpl1"));

	String validNames[] = getSupportedModes();
	boolean valid = false;

	for (int i=0; i<validNames.length; i++) {
	    if (modeName.equalsIgnoreCase(validNames[i])) {
		valid = true;
		break;
	    }
	}

	if (valid == false) {
	    throw new IllegalArgumentException(
					    JaiI18N.getString("TileCodec1"));
	}

	return null ;
    }

    /**
     * Returns the <code>ParameterListDescriptor</code> that describes
     * the associated parameters (NOT sources). This method returns
     * null if there are no parameters for the specified modeName.
     * If the specified modeName supports parameters but the
     * implementing class does not have parameters, then this method
     * returns a non-null <code>ParameterListDescriptor</code> whose
     * <code>getNumParameters()</code> returns 0.
     *
     * @param modeName The mode to return a ParameterListDescriptor for.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null.
     *
     * @throws IllegalArgumentException if <code>modeName</code> is null.
     * @throws IllegalArgumentException if <code>modeName</code> is not
     * one of the modes valid for this descriptor, i.e those returned
     * from the getSupportedNames() method.
     */
    public ParameterListDescriptor getParameterListDescriptor(String modeName){

	if (modeName == null)
	    throw new IllegalArgumentException(
				JaiI18N.getString("TileCodecDescriptorImpl1"));

	String validNames[] = getSupportedModes();
	boolean valid = false;

	for (int i=0; i<validNames.length; i++) {
	    if (modeName.equalsIgnoreCase(validNames[i])) {
		valid = true;
		break;
	    }
	}

	if (valid == false) {
	    throw new IllegalArgumentException(
					    JaiI18N.getString("TileCodec1"));
	}

	return pld;
    }    
}
