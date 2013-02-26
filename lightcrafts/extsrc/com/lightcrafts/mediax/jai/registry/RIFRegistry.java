/*
 * $RCSfile: RIFRegistry.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:48 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.registry;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;
import java.util.Iterator;
import java.util.List;

import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationNode;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.PropertySource;
import com.lightcrafts.mediax.jai.RenderedOp;

/**
 * Utility class to provide type-safe interaction with the
 * <code>OperationRegistry</code> for <code>RenderedImageFactory</code>
 * objects.
 *
 * If the <code>OperationRegistry</code> is <code>null</code>, then
 * <code>JAI.getDefaultInstance().getOperationRegistry()</code> will be used.
 *
 * @since JAI 1.1
 */
public final class RIFRegistry  {

    private static final String MODE_NAME = RenderedRegistryMode.MODE_NAME;

    /**
      * Register a RIF with a particular product and operation
      * against a specified mode. This is JAI 1.0.x equivalent
      * of <code>registry.registerRIF(...)</code>
      *
      * @param registry the <code>OperationRegistry</code> to register with.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      * @param rif the <code>RenderedImageFactory</code> to be registered
      *
      * @throws IllegalArgumentException if operationName, productName,
      *             or rif is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      */
    public static void register(OperationRegistry registry,
                                String operationName,
                                String productName,
                                RenderedImageFactory rif) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.registerFactory(MODE_NAME, operationName, productName, rif);
    }

    /**
      * Unregister a RIF previously registered with a product
      * and operation against the specified mode.
      *
      * @param registry the <code>OperationRegistry</code> to unregister from.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      * @param rif the <code>RenderedImageFactory</code> to be unregistered
      *
      * @throws IllegalArgumentException if operationName, productName,
      *             or rif is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      * @throws IllegalArgumentException if the rif was not previously
      *             registered against operationName and productName
      */
    public static void unregister(OperationRegistry registry,
                                  String operationName,
                                  String productName,
                                  RenderedImageFactory rif) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.unregisterFactory(MODE_NAME, operationName, productName, rif);
    }

    /**
      * Sets a preference between two rifs for a given operation under a
      * specified product.
      *
      * @param registry the <code>OperationRegistry</code> to use.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      * @param preferredRIF the preferred rif
      * @param otherRIF the other rif
      *
      * @throws IllegalArgumentException if operationName, productName,
      *             preferredRIF or otherRIF is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      * @throws IllegalArgumentException if either of the rifs
      *             were not previously registered against
      *             operationName and productName
      */
    public static void setPreference(OperationRegistry registry,
                                     String operationName,
                                     String productName,
                                     RenderedImageFactory preferredRIF,
                                     RenderedImageFactory otherRIF) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.setFactoryPreference(MODE_NAME,
		operationName, productName, preferredRIF, otherRIF);
    }

    /**
      * Unsets a preference between two rifs for a given operation under
      * a specified product.
      *
      * @param registry the <code>OperationRegistry</code> to use.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      * @param preferredRIF the factory object formerly preferred
      * @param otherRIF the other factory object
      *
      * @throws IllegalArgumentException if operationName, productName,
      *             preferredRIF or otherRIF is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      * @throws IllegalArgumentException if either of the rifs
      *             were not previously registered against
      *             operationName and productName
      */
    public static void unsetPreference(OperationRegistry registry,
                                       String operationName,
                                       String productName,
                                       RenderedImageFactory preferredRIF,
                                       RenderedImageFactory otherRIF) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.unsetFactoryPreference(MODE_NAME,
		operationName, productName, preferredRIF, otherRIF);
    }

    /**
      * Removes all preferences between RIFs within a product registered
      * under a particular <code>OperationDescriptor</code>.
      *
      * @param registry the <code>OperationRegistry</code> to use.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      *
      * @throws IllegalArgumentException if operationName or productName
      *             is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      */
    public static void clearPreferences(OperationRegistry registry,
                                        String operationName,
                                        String productName) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.clearFactoryPreferences(MODE_NAME, operationName, productName);
    }

    /**
      * Returns a list of the RIFs of a product registered under a
      * particular <code>OperationDescriptor</code>, in an ordering
      * that satisfies all of the pairwise preferences that have
      * been set. Returns <code>null</code> if cycles exist. Returns
      * <code>null</code>, if the product does not exist under this
      * operationName.
      *
      * @param registry the <code>OperationRegistry</code> to use.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      *
      * @return an ordered <code>List</code> of RIFs
      *
      * @throws IllegalArgumentException if operationName or productName
      *             is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      */
    public static List getOrderedList(OperationRegistry registry,
                                      String operationName,
                                      String productName) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	return registry.getOrderedFactoryList(MODE_NAME,
				    operationName, productName);
    }

    /**
      * Returns an <code>Iterator</code> over all <code>
      * RenderedImageFactory</code> objects registered under the
      * operation name over all products. The order of objects in
      * the iteration will be according to the pairwise preferences
      * among products and image factories within a product. The
      * <code>remove()</code> method of the <code>Iterator</code>
      * may not be implemented.
      *
      * @param registry the <code>OperationRegistry</code> to use.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      *
      * @return an <code>Iterator</code> over <code>RenderedImageFactory</code> objects
      *
      * @throws IllegalArgumentException if operationName is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      *
      * @since JAI 1.1
      */
    public static Iterator getIterator(OperationRegistry registry,
				       String operationName) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	return registry.getFactoryIterator(MODE_NAME, operationName);
    }

    /**
      * Returns the the most preferred <code>RenderedImageFactory</code>
      * object registered against the operation name. This
      * method will return the first object that would be
      * encountered by the <code>Iterator</code> returned by the
      * <code>getIterator()</code> method.
      *
      * @param registry the <code>OperationRegistry</code> to use.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      *
      * @return a registered <code>RenderedImageFactory</code> object
      *
      * @throws IllegalArgumentException if operationName is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      */
    public static RenderedImageFactory get(OperationRegistry registry,
					   String operationName) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	return (RenderedImageFactory)
		    registry.getFactory(MODE_NAME, operationName);
    }

    /**
      * Constructs a <code>RenderedImage</code> (usually a
      * <code>RenderedOp</code>) representing the results of applying
      * a given operation to a particular ParameterBlock and rendering
      * hints. The registry is used to determine the RIF to be used to
      * instantiate the operation.
      *
      * <p> If none of the RIFs registered with this
      * <code>OperationRegistry</code> returns a non-null value, null is
      * returned. Exceptions thrown by the RIFs will be caught by this
      * method and will not be propagated.
      *
      * @param registry the <code>OperationRegistry</code> to use.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param paramBlock the operation's ParameterBlock.
      * @param renderHints a <code>RenderingHints</code> object
      *         containing rendering hints.
      *
      * @throws IllegalArgumentException if operationName is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      */
    public static RenderedImage create(OperationRegistry registry,
                                       String operationName,
                                       ParameterBlock paramBlock,
                                       RenderingHints renderHints) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	Object args[] = { paramBlock, renderHints };

	return (RenderedImage)
		registry.invokeFactory(MODE_NAME, operationName, args);
    }

    /**
     * Constructs and returns a <code>PropertySource</code> suitable for
     * use by a given <code>RenderedOp</code>.  The 
     * <code>PropertySource</code> includes properties copied from prior 
     * nodes as well as those generated at the node itself. Additionally,
     * property suppression is taken into account. The actual 
     * implementation of <code>getPropertySource()</code> may make use
     * of deferred execution and caching.
     *
     * @param op the <code>RenderedOp</code> requesting its 
     *        <code>PropertySource</code>.
     *
     * @throws IllegalArgumentException if <code>op</code> is <code>null</code>
     */
    public static PropertySource getPropertySource(RenderedOp op) {

	if (op == null)
	    throw new IllegalArgumentException("op - " +
			JaiI18N.getString("Generic0"));

	return op.getRegistry().getPropertySource((OperationNode)op);
    }
}
