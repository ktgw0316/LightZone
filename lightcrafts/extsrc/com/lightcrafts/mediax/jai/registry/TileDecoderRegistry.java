/*
 * $RCSfile: TileDecoderRegistry.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:49 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.registry;

import java.awt.Point;
import java.awt.image.Raster;
import java.io.InputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoder;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoderFactory;

/**
 * Utility class to provide type-safe interaction with the
 * <code>OperationRegistry</code> for <code>TileDecoderFactory</code> objects.
 *
 * If the <code>OperationRegistry</code> specified as an argument to the
 * methods in this class is null, then <code>JAI.getOperationRegistry()</code> 
 * will be used.
 *
 * @since JAI 1.1
 */
public final class TileDecoderRegistry  {

    private static final String MODE_NAME = TileDecoderRegistryMode.MODE_NAME;

    /**
     * Registers the given <code>TileDecoderFactory</code> with the given 
     * <code>OperationRegistry</code> under the given formatName and 
     * productName.
     *
     * @param registry    The <code>OperationRegistry</code> to register the 
     *                    <code>TileDecoderFactory</code> with. If this is
     *                    <code>null</code>, then <code>
     *                    JAI.getDefaultInstance().getOperationRegistry()</code>
     *                    will be used.
     * @param formatName  The formatName to register the 
     *                    <code>TileDecoderFactory</code> under.
     * @param productName The productName to register the 
     *                    <code>TileDecoderFactory</code> under.
     * @param tdf         The <code>TileDecoderFactory</code> to register.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if productName is null.
     * @throws IllegalArgumentException if tdf is null.
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>
     */
    public static void register(OperationRegistry registry,
                                String formatName,
                                String productName,
                                TileDecoderFactory tdf) {

	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.registerFactory(MODE_NAME, formatName, productName, tdf);
    }

    /**
     * Unregisters the given <code>TileDecoderFactory</code> previously 
     * registered under the given formatName and productName in the given
     * <code>OperationRegistry</code>.
     *
     * @param registry    The <code>OperationRegistry</code> to unregister the 
     *                    <code>TileDecoderFactory</code> from. If this is
     *                    <code>null</code>, then <code>
     *                    JAI.getDefaultInstance().getOperationRegistry()</code>
     *                    will be used.
     * @param formatName  The formatName to unregister the
     *                    <code>TileDecoderFactory</code> from.
     * @param productName The productName to unregister the
     *                    <code>TileDecoderFactory</code> from.
     * @param tdf         The <code>TileDecoderFactory</code> to unregister.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if productName is null.
     * @throws IllegalArgumentException if tdf is null.   
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>
     * @throws IllegalArgumentException if the tdf was not previously
     * registered against the given formatName and productName.
     */
    public static void unregister(OperationRegistry registry,
                                  String formatName,
                                  String productName,
                                  TileDecoderFactory tdf) {

	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.unregisterFactory(MODE_NAME, formatName, productName, tdf);
    }

    /**
     * Sets a preference between the given two <code>TileDecoderFactory</code>
     * objects in the given <code>OperationRegistry</code> under the given
     * formatName and productName.
     *
     * @param registry     The <code>OperationRegistry</code> to set
     *                     preferences on. If this is
     *                     <code>null</code>, then <code>
     *                     JAI.getDefaultInstance().getOperationRegistry()</code>
     *                     will be used.
     * @param formatName   The formatName of the two
     *                     <code>TileDecoderFactory</code>s.
     * @param productName  The productName of the two
     *                     <code>TileDecoderFactory</code>s.
     * @param preferredTDF The preferred <code>TileDecoderFactory</code>.
     * @param otherTDF     The other <code>TileDecoderFactory</code>.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if productName is null.
     * @throws IllegalArgumentException if preferredTDF is null.
     * @throws IllegalArgumentException if otherTDF is null.   
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>
     * @throws IllegalArgumentException if either of the two tdf's was 
     * not previously registered against the given formatName and productName.
     */
    public static void setPreference(OperationRegistry registry,
                                     String formatName,
                                     String productName,
                                     TileDecoderFactory preferredTDF,
                                     TileDecoderFactory otherTDF) {

	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.setFactoryPreference(MODE_NAME,
				      formatName, 
				      productName, 
				      preferredTDF, 
				      otherTDF);
    }

    /**
     * Unsets a preference previously set amongst the given two 
     * <code>TileDecoderFactory</code> objects in the given
     * <code>OperationRegistry</code> under the given formatName
     * and productName.
     *
     * @param registry     The <code>OperationRegistry</code> to unset
     *                     preferences on. If this is
     *                     <code>null</code>, then <code>
     *                     JAI.getDefaultInstance().getOperationRegistry()</code>
     *                     will be used.
     * @param formatName   The formatName of the two
     *                     <code>TileDecoderFactory</code>s.
     * @param productName  The productName  of the two
     *                     <code>TileDecoderFactory</code>s.
     * @param preferredTDF The preferred <code>TileDecoderFactory</code>.
     * @param otherTDF     The other <code>TileDecoderFactory</code>.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if productName is null.
     * @throws IllegalArgumentException if preferredTDF is null.
     * @throws IllegalArgumentException if otherTDF is null.   
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>
     * @throws IllegalArgumentException if either of the two tdf's was
     * not previously registered against the given formatName and productName.
     */
    public static void unsetPreference(OperationRegistry registry,
                                       String formatName,
                                       String productName,
                                       TileDecoderFactory preferredTDF,
                                       TileDecoderFactory otherTDF) {
	
	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.unsetFactoryPreference(MODE_NAME,
					formatName,
					productName,
					preferredTDF,
					otherTDF);
    }

    /**
     * Clears all preferences set for registered 
     * <code>TileDecoderFactory</code>s under the given formatName and
     * productName in the given <code>OperationRegistry</code>.
     *
     * @param registry    The <code>OperationRegistry</code> to clear
     *                    preferences from. If this is
     *                    <code>null</code>, then <code>
     *                    JAI.getDefaultInstance().getOperationRegistry()</code>
     *                    will be used.
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
     * Returns a List of the <code>TileDecoderFactory</code>s registered
     * in the given <code>OperationRegistry</code> under the given
     * formatName and productName, in an ordering that satisfies
     * all of the pairwise preferences that have been set. Returns
     * <code>null</code> if cycles exist.
     *
     * @param registry    The <code>OperationRegistry</code> to clear
     *                    preferences from. If this is
     *                    <code>null</code>, then <code>
     *                    JAI.getDefaultInstance().getOperationRegistry()</code>
     *                    will be used.
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
      * <code>TileDecoderFactory</code> objects registered under the
      * given format name over all products. The order of the
      * <code>TileDecoderFactory</code> objects in the iteration will
      * be according to the pairwise preferences among products and
      * <code>TileDecoderFactory</code> objects within a product. The
      * <code>remove()</code> method of the <code>Iterator</code>
      * may not be implemented.
      *
      * @param registry    The <code>OperationRegistry</code> to use. If 
      *                     this is <code>null</code>, then <code>
      *                     JAI.getDefaultInstance().getOperationRegistry()</code>
      *                     will be used.
      * @param formatName  The format name.
      *
      * @return an <code>Iterator</code> over <code>TileDecoderFactory</code>
      * objects.
      *
      * @throws IllegalArgumentException if formatName is <code>null</code>
      * @throws IllegalArgumentException if there is no 
      * <code>TileCodecDescriptor</code> registered against the
      * given <code>formatName</code>.
      */
    public static Iterator getIterator(OperationRegistry registry, 
				       String formatName) {

	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	return registry.getFactoryIterator(MODE_NAME, formatName);
    }

    /**
      * Returns the the most preferred <code>TileDecoderFactory</code>
      * object registered against the given format name. This
      * method will return the first <code>TileDecoderFactory</code> that
      * would be encountered by the <code>Iterator</code> returned by the
      * <code>getIterator()</code> method.
      *
      * @param registry The <code>OperationRegistry</code> to use.
      *                 If this is <code>null</code>, then <code>
      *                 JAI.getDefaultInstance().getOperationRegistry()</code>
      *                 will be used.
      * @param formatName The format name as a <code>String</code>
      *
      * @return a registered <code>TileDecoderFactory</code> object
      *
      * @throws IllegalArgumentException if formatName is <code>null</code>.
      * @throws IllegalArgumentException if there is no 
      * <code>TileCodecDescriptor</code> registered against
      * the <code>formatName</code>
      */
    public static TileDecoderFactory get(OperationRegistry registry, 
				         String formatName) {
	
	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	return (TileDecoderFactory)registry.getFactory(MODE_NAME, formatName);
    }

    /**
     * Creates a <code>TileDecoder</code> for the specified format that is
     * capable of handling the supplied arguments.  
     *
     * <p> The preferences set amongst the <code>TileDecoderFactory</code>
     * objects registered with the <code>OperationRegistry</code> are used
     * to select the most prefered <code>TileDecoderFactory</code> whose
     * <code>createDecoder()</code> method returns a non-null value.
     *
     * <p> In order to do the decoding correctly, the caller should
     * retrieve the <code>TileCodecDescriptor</code> associated with the
     * returned <code>TileDecoder</code> from the
     * <code>OperationRegistry</code> and use it's
     * <code>includesLocationInfo()</code> method's return value to decide
     * which of the two versions of the <code>decode()</code> method on the
     * returned <code>TileDecoder</code> should be used.
     *
     * <p>Since this class is a simple type-safe wrapper around
     * <code>OperationRegistry</code>'s type-unsafe methods, no additional
     * argument validation is performed in this method. Thus errors/exceptions
     * may occur if incorrect values are provided for the input arguments.
     *
     * <p>Exceptions thrown by the <code>TileDecoderFactory</code>s used to
     * create the <code>TileDecoder</code> will be caught by this method
     * and will not be propagated.
     *
     * @param registry   The <code>OperationRegistry</code> to use to create
     *                   the <code>TileDecoder</code>. If 
     *                   this is <code>null</code>, then <code>
     *                   JAI.getDefaultInstance().getOperationRegistry()</code>
     *                   will be used.
     * @param formatName The format for which the <code>TileDecoder</code> is
     *                   to be created.
     * @param input      The <code>InputStream</code> to read encoded data from.
     * @param paramList  The object containing the tile decoding parameters.
     *
     * @throws IllegalArgumentException if formatName is null.
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>.
     */
    public static TileDecoder create(OperationRegistry registry,
				     String formatName,
				     InputStream input,
				     TileCodecParameterList paramList) {

	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	Object args[] = {input, paramList};

	return (TileDecoder)registry.invokeFactory(MODE_NAME, 
						   formatName,
						   args);
    }

    // Decode methods 

    /**
     * Decodes the data from the specified <code>InputStream</code>
     * using the given formatName and <code>TileCodecParameterList</code>. 
     * The <code>TileDecoder</code> which performs the decoding is the
     * one created from the most prefered <code>TileDecoderFactory</code>
     * whose <code>create</code> method returns a non-null result.
     *
     * <p> An <code>IllegalArgumentException</code> will be thrown if 
     * the specified format's <code>TileCodecDescriptor</code>'s
     * <code>includesLocationInfo()</code> method returns false, as no
     * location information is provided in this method.
     *     
     * <p> If the specified <code>TileCodecParameterList</code> is null, the 
     * default <code>TileCodecParameterList</code> retrieved by the specific
     * <code>TileDecoder.getDefaultParameters</code>() method for the
     * "tileDecoder" registry mode will be used.
     *
     * <p> For the specified format, if the associated 
     * <code>TileCodecDescriptor</code>'s <code>includesSampleModelInfo()</code> 
     * method returns false, and either the specified
     * <code>TileCodecParameterList</code> is null or if it doesn't contain
     * a non-value for the "sampleModel" parameter, an 
     * <code>IllegalArgumentException</code> will be thrown. 
     *
     * <p> If there are no <code>TileDecoder</code> objects that can decode
     * the specified <code>InputStream</code> according to the decoding
     * parameters supplied, null will be returned from this method.
     * 
     * <p> If multiple tiles are to be decoded from the same
     * <code>InputStream</code> in the same format using the same
     * <code>TileCodecParameterList</code>, it is advisable to create a
     * <code>TileDecoder</code> object and use the <code>decode()</code> method
     * on this decoder for each tile, thus creating and using only a single
     * <code>TileDecoder</code> object. The <code>decode()</code>
     * method on <code>TileDecoderRegistry</code> creates a new
     * <code>TileDecoder</code> object each time it is called.
     *
     * <p>Since this class is a simple type-safe wrapper around
     * <code>OperationRegistry</code>'s type-unsafe methods, no additional
     * argument validation is performed in this method. Thus errors/exceptions
     * may occur if incorrect values are provided for the input arguments.
     *
     * <p>Exceptions thrown by the <code>TileDecoderFactory</code>s used to
     * create the <code>TileDecoder</code> will be caught by this method
     * and will not be propagated.
     *
     * @param registry   The <code>OperationRegistry</code> to use to create
     *                   the <code>TileDecoder</code>. If 
     *                   this is <code>null</code>, then <code>
     *                   JAI.getDefaultInstance().getOperationRegistry()</code>
     *                   will be used.
     * @param formatName The format name associated with the decoder.
     * @param input      The <code>InputStream</code> containing the data 
     *                   to be decoded.
     * @param param      The <code>TileCodecParameterList</code> to be used.
     *
     * @throws IllegalArgumentException if formatName is null.  
     * @throws IOException if an I/O error occurs while reading from the
     * associated InputStream.   
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>.
     *
     * @return The associated <code>TileDecoder</code>, or <code>null</code>.
     */
    public static Raster decode(OperationRegistry registry, 
				String formatName,
				InputStream input,
				TileCodecParameterList param) 
	throws IOException {
	
	TileDecoder decoder = create(registry, formatName, input, param);

	if (decoder == null) {
	    return null;
	}

	return decoder.decode();
    }
    
    /**
     * Decodes the data from the specified <code>InputStream</code>
     * using the given formatName and <code>TileCodecParameterList</code>.
     * The <code>TileDecoder</code> which performs the decoding is the
     * one created from the most prefered <code>TileDecoderFactory</code>
     * whose <code>create</code> method returns a non-null result. If
     * there are no <code>TileDecoder</code> objects that can decode
     * the specified <code>InputStream</code> according to the decoding
     * parameters supplied, null will be returned from this method.
     *
     * <p> If the specified <code>TileCodecParameterList</code> is null, the 
     * default <code>TileCodecParameterList</code> retrieved by the specific
     * <code>TileDecoder.getDefaultParameters()</code> method will be used.
     *
     * <p> If the specified location is null, and the associated 
     * <code>TileCodecDescriptor</code>'s <code>includesLocationInfo()</code>
     * method returns false, <code>IllegalArgumentException</code> will be
     * thrown. 
     *
     * <p> For the specified format, if the associated 
     * <code>TileCodecDescriptor</code>'s <code>includesSampleModelInfo()</code> 
     * method returns false, and if the specified
     * <code>TileCodecParameterList</code> is null or if it doesn't contain
     * a non-value for the "sampleModel" parameter, an 
     * <code>IllegalArgumentException</code> will be thrown. 
     *
     * <p> If multiple tiles are to be decoded from the same
     * <code>InputStream</code> in the same format using the same
     * <code>TileCodecParameterList</code>, it is advisable to create a
     * <code>TileDecoder</code> object and use the <code>decode()</code> method
     * on this decoder for each tile, thus creating and using only a single
     * <code>TileDecoder</code> object. The <code>decode()</code>
     * method on <code>TileDecoderRegistry</code> creates a new
     * <code>TileDecoder</code> object each time it is called.
     *
     * <p>Since this class is a simple type-safe wrapper around
     * <code>OperationRegistry</code>'s type-unsafe methods, no additional
     * argument validation is performed in this method. Thus errors/exceptions
     * may occur if incorrect values are provided for the input arguments.
     *
     * <p>Exceptions thrown by the <code>TileDecoderFactory</code>s used to
     * create the <code>TileDecoder</code> will be caught by this method
     * and will not be propagated.
     *
     * @param registry   The <code>OperationRegistry</code> to use to create
     *                   the <code>TileDecoder</code>. If 
     *                   this is <code>null</code>, then <code>
     *                   JAI.getDefaultInstance().getOperationRegistry()</code>
     *                   will be used.
     * @param formatName The format name associated with the decoder.
     * @param input      The <code>InputStream</code> containing the data to
     *                   be decoded.
     * @param param      The <code>TileCodecParameterList</code> to be used.
     * @param location   The <code>Point</code> specifying the upper left
     *                   corner of the <code>Raster</code>. 
     *
     * @throws IllegalArgumentException if formatName is null.  
     * @throws IOException if an inout/output error occurs while reading from
     * the associated InputStream or during decoding.
     * @throws IllegalArgumentException if there is no 
     * <code>TileCodecDescriptor</code> registered against the 
     * given <code>formatName</code>.

     * @return The associated <code>TileDecoder</code>, or <code>null</code>.
     */
    public static Raster decode(OperationRegistry registry,
				String formatName,
				InputStream input,
				TileCodecParameterList param,
				Point location) throws IOException {

	TileDecoder decoder = create(registry, formatName, input, param);

	if (decoder == null) {
	    return null;
	}

	return decoder.decode(location);
    }
}

