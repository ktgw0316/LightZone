/*
 * $RCSfile: TileEncoderRegistry.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:49 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.registry;

import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoder;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoderFactory;

/**
 * Utility class to provide type-safe interaction with the
 * <code>OperationRegistry</code> for <code>TileEncoderFactory</code> objects.
 *
 * If the <code>OperationRegistry</code> specified as an argument to the
 * methods in this class is null, then <code>JAI.getOperationRegistry()</code>
 * will be used.
 *
 * @since JAI 1.1
 */
public final class TileEncoderRegistry  {

    private static final String MODE_NAME = TileEncoderRegistryMode.MODE_NAME;

    /**
     * Registers the given <code>TileEncoderFactory</code> with the given 
     * <code>OperationRegistry</code> under the given formatName and
     * productName.
     *
     * @param registry    The <code>OperationRegistry</code> to register the 
     *                    <code>TileEncoderFactory</code> with.
     * @param formatName  The formatName to register the 
     *                    <code>TileEncoderFactory</code> under.
     * @param productName The productName to register the 
     *                    <code>TileEncoderFactory</code> under.
     * @param tef         The <code>TileEncoderFactory</code> to register.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if productName is null.
     * @throws IllegalArgumentException if tef is null.   
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>.
     */
    public static void register(OperationRegistry registry,
                                String formatName,
                                String productName,
                                TileEncoderFactory tef) {

	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.registerFactory(MODE_NAME, formatName, productName, tef);
    }

    /**
     * Unregisters the given <code>TileEncoderFactory</code> previously 
     * registered under the given formatName and productName in the given
     * <code>OperationRegistry</code>.
     *
     * @param registry    The <code>OperationRegistry</code> to unregister the 
     *                    <code>TileEncoderFactory</code> from.
     * @param formatName  The formatName to unregister the
     *                    <code>TileEncoderFactory</code> from.
     * @param productName The productName to unregister the
     *                    <code>TileEncoderFactory</code> from.
     * @param tef         The <code>TileEncoderFactory</code> to unregister.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if productName is null.
     * @throws IllegalArgumentException if tef is null.   
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>.
     * @throws IllegalArgumentException if the tef was not previously
     * registered against the given formatName and productName.
     */
    public static void unregister(OperationRegistry registry,
                                  String formatName,
                                  String productName,
                                  TileEncoderFactory tef) {

	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.unregisterFactory(MODE_NAME, formatName, productName, tef);
    }

    /**
     * Sets a preference between the given two <code>TileEncoderFactory</code> 
     * objects in the given <code>OperationRegistry</code> under the given
     * formatName and productName.
     *
     * @param registry     The <code>OperationRegistry</code> to set
     *                     preferences on.
     * @param formatName   The formatName of the two
     *                     <code>TileEncoderFactory</code>s.
     * @param productName  The productName of the two
     *                     <code>TileEncoderFactory</code>s.
     * @param preferredTEF The preferred <code>TileEncoderFactory</code>.
     * @param otherTEF     The other <code>TileEncoderFactory</code>.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if productName is null.
     * @throws IllegalArgumentException if preferredTEF is null.
     * @throws IllegalArgumentException if otherTEF is null.   
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>.
     * @throws IllegalArgumentException if either of the two tef's
     * was not previously registered against formatName and productName.
     */
    public static void setPreference(OperationRegistry registry,
                                     String formatName,
                                     String productName,
                                     TileEncoderFactory preferredTEF,
                                     TileEncoderFactory otherTEF) {

	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.setFactoryPreference(MODE_NAME,
				      formatName, 
				      productName, 
				      preferredTEF, 
				      otherTEF);
    }

    /**
     * Unsets a preference previously set amongst the given two 
     * <code>TileEncoderFactory</code> objects in the given
     * <code>OperationRegistry</code> under the given formatName and productName.
     *
     * @param registry     The <code>OperationRegistry</code> to unset
     *                     preferences on.
     * @param formanName   The formatName of the two
     *                     <code>TileEncoderFactory</code>s.
     * @param productName  The productName  of the two
     *                     <code>TileEncoderFactory</code>s.
     * @param preferredTEF The preferred <code>TileEncoderFactory</code>.
     * @param otherTEF     The other <code>TileEncoderFactory</code>.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if productName is null.
     * @throws IllegalArgumentException if preferredTEF is null.
     * @throws IllegalArgumentException if otherTEF is null.   
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>.
     * @throws IllegalArgumentException if either of the two tef's
     * was not previously registered against formatName and productName.
     */
    public static void unsetPreference(OperationRegistry registry,
                                       String formatName,
                                       String productName,
                                       TileEncoderFactory preferredTEF,
                                       TileEncoderFactory otherTEF) {
	
	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.unsetFactoryPreference(MODE_NAME,
					formatName,
					productName,
					preferredTEF,
					otherTEF);
    }

    /**
     * Clears all preferences set for registered <code>TileEncoderFactory</code>s
     * under the given formatName and productName in the given
     * <code>OperationRegistry</code>.
     *
     * @param registry    The <code>OperationRegistry</code> to clear
     *                    preferences from.
     * @param formatName  The format name to clear preferences under.
     * @param productName The productName to clear preferences under.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if productName is null.   
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>.
     */
    public static void clearPreferences(OperationRegistry registry,
					String formatName,
					String productName) {
	
	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.clearFactoryPreferences(MODE_NAME, formatName, productName);
    }

    /**
     * Returns a List of the <code>TileEncoderFactory</code>s registered
     * in the given <code>OperationRegistry</code> under the given
     * formatName and productName, in an ordering that satisfies
     * all of the pairwise preferences that have been set. Returns
     * <code>null</code> if cycles exist.
     *
     * @param registry    The <code>OperationRegistry</code> to clear
     *                    preferences from.
     * @param formatName  The format name to clear preferences under.
     * @param productName The productName to clear preferences under.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if productName is null.   
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>.
     */
    public static List getOrderedList(OperationRegistry registry,
				      String formatName,
				      String productName) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	return registry.getOrderedFactoryList(MODE_NAME,
					      formatName,
					      productName);
    }

    /**
      * Returns an <code>Iterator</code> over all 
      * <code>TileEncoderFactory</code> objects registered under the
      * given format name over all products. The order of the
      * <code>TileEncoderFactory</code> objects in the iteration will
      * be according to the pairwise preferences among products and
      * <code>TileEncoderFactory</code> objects within a product. The
      * <code>remove()</code> method of the <code>Iterator</code>
      * may not be implemented.
      *
      * @param registry    The <code>OperationRegistry</code> to use.
      * @param formatName  The format name.
      *
      * @return an <code>Iterator</code> over <code>TileEncoderFactory</code>
      * objects.
      *
      * @throws IllegalArgumentException if formatName is <code>null</code>
      * @throws IllegalArgumentException if there is no
      * <code>TileCodecDescriptor</code> registered against
      * the <code>formatName</code>.
      */
    public static Iterator getIterator(OperationRegistry registry, 
				       String formatName) {

	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	return registry.getFactoryIterator(MODE_NAME, formatName);
    }

    /**
      * Returns the the most preferred <code>TileEncoderFactory</code>
      * object registered against the given format name. This
      * method will return the first <code>TileEncoderFactory</code> that
      * would be encountered by the <code>Iterator</code> returned by the
      * <code>getIterator()</code> method.
      *
      * @param registry The <code>OperationRegistry</code> to use.
      *                 If this is <code>null</code>, then <code>
      *                 JAI.getDefaultInstance().getOperationRegistry()</code>
      *                 will be used.
      * @param formatName The format name as a <code>String</code>
      *
      * @return a registered <code>TileEncoderFactory</code> object
      *
      * @throws IllegalArgumentException if formatName is <code>null</code>.
      * @throws IllegalArgumentException if there is no <code>
      * TileCodecDescriptor</code> registered against the <code>formatName</code>
      */
    public static TileEncoderFactory get(OperationRegistry registry, 
				         String formatName) {

	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	return (TileEncoderFactory)registry.getFactory(MODE_NAME, formatName);
    }

    /**
     * Creates a <code>TileEncoder</code> for the specified format that is
     * capable of handling the supplied arguments.  
     *
     * <p> The preferences set amongst the <code>TileEncoderFactory</code>
     * objects registered with the <code>OperationRegistry</code> are used
     * to select the most prefered <code>TileEncoderFactory</code> whose
     * <code>createEncoder()</code> method returns a non-null value.
     *
     * <p>Since this class is a simple type-safe wrapper around
     * <code>OperationRegistry</code>'s type-unsafe methods, no additional
     * argument validation is performed in this method. Thus errors/exceptions
     * may occur if incorrect values are provided for the input arguments.
     *
     * <p>Exceptions thrown by the <code>TileEncoderFactory</code>s used to
     * create the <code>TileEncoder</code> will be caught by this method
     * and will not be propagated.
     *
     * @param registry    The <code>OperationRegistry</code> to use to create
     *                    the <code>TileEncoder</code>.
     * @param formatName  The format for which the <code>TileEncoder</code> is
     *                    to be created.
     * @param output      The <code>OutputStream</code> to write encoded data to.
     * @param paramList   The object containing the tile encoding parameters.
     * @param sampleModel The <code>SampleModel</code> of the 
     *                    <code>Raster</code> to be encoded.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>.
     */
    public static TileEncoder create(OperationRegistry registry,
				     String formatName,
				     OutputStream output,
				     TileCodecParameterList paramList,
				     SampleModel sampleModel) {

	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();
	
	Object args[] = {output, paramList, sampleModel};

	return (TileEncoder)registry.invokeFactory(MODE_NAME,
						   formatName,
						   args);
    }

    /**
     * Encodes the given <code>Raster</code> using the given formatName and 
     * <code>TileCodecParameterList</code> and writes the encoded data to the 
     * specified <code>OutputStream</code>. 
     * The <code>TileEncoder</code> which performs the encoding is the
     * one created from the most prefered <code>TileEncoderFactory</code> 
     * whose <code>create</code> method returns a non-null result. If
     * there are no <code>TileEncoder</code> objects that can encode
     * the specified <code>Raster</code> according to the encoding
     * parameters supplied, nothing will be written to the specified
     * <code>OutputStream</code>.
     *
     * <p> If the specified <code>TileCodecParameterList</code> is null, the 
     * default <code>TileCodecParameterList</code> retrieved by the specific
     * <code>TileEncoder.getDefaultParameters()</code> method for the 
     * "tileEncoder" registry mode will be used.
     *
     * <p> If multiple tiles are to be encoded to the same 
     * <code>OutputStream</code> in the same format using the same
     * <code>TileCodecParameterList</code>, it is advisable to create a
     * <code>TileEncoder</code> object and use the <code>encode()</code> method
     * on this encoder to encode each tile, thus creating and using only a
     * single <code>TileEncoder</code> object. The <code>encode()</code>
     * method on <code>TileEncoderRegistry</code> creates a new
     * <code>TileEncoder</code> object each time it is called.
     *
     * <p>Since this class is a simple type-safe wrapper around
     * <code>OperationRegistry</code>'s type-unsafe methods, no additional
     * argument validation is performed in this method. Thus errors/exceptions
     * may occur if incorrect values are provided for the input arguments.
     *
     * <p>Exceptions thrown by the <code>TileEncoderFactory</code>s used to
     * create the <code>TileEncoder</code> will be caught by this method
     * and will not be propagated.
     *
     * @param registry   The <code>OperationRegistry</code> to use to create
     *                   the <code>TileEncoder</code>.
     * @param formatName The name of the format to encode the data in.
     * @param raster     The <code>Raster</code> to be encoded.
     * @param output     The <code>OutputStream</code> to write the encoded
     *                   data to.
     * @param param      The <code>TileCodecParameterList</code> to be used.
     * @throws IllegalArgumentException if formatName is null.  
     * @throws IOException if an input/output error occurs during the encoding.
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>.
     *
     * @return The associated <code>TileEncoder</code>, or <code>null</code>.  
     */
    public static void encode(OperationRegistry registry,
			      String formatName, 
			      Raster raster,
			      OutputStream output,
			      TileCodecParameterList param) 
	throws IOException {

	TileEncoder encoder = create(registry,
				     formatName,
				     output, 
				     param,
				     raster.getSampleModel());

	if (encoder != null)
	    encoder.encode(raster);
    }
}
