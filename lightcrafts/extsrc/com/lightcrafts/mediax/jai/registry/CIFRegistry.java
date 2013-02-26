/*
 * $RCSfile: CIFRegistry.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:47 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai.registry;

import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;
import java.util.Iterator;
import java.util.List;

import com.lightcrafts.mediax.jai.CollectionImage;
import com.lightcrafts.mediax.jai.CollectionImageFactory;
import com.lightcrafts.mediax.jai.CollectionOp;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationNode;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.PropertySource;

/**
 * Utility class to provide type-safe interaction with the
 * <code>OperationRegistry</code> for <code>CollectionImageFactory</code>
 * objects.
 *
 * If the <code>OperationRegistry</code> is <code>null</code>, then
 * <code>JAI.getDefaultInstance().getOperationRegistry()</code> will be used.
 *
 * @since JAI 1.1
 */
public final class CIFRegistry  {

    private static final String MODE_NAME = CollectionRegistryMode.MODE_NAME;

    /**
      * Register a CIF with a particular product and operation
      * against a specified mode. This is JAI 1.0.x equivalent
      * of <code>registry.registerCIF(...)</code>
      *
      * @param registry the <code>OperationRegistry</code> to register with.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      * @param cif the <code>CollectionImageFactory</code> to be registered
      *
      * @throws IllegalArgumentException if operationName, productName,
      *             or cif is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      */
    public static void register(OperationRegistry registry,
                                String operationName,
                                String productName,
                                CollectionImageFactory cif) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.registerFactory(MODE_NAME, operationName, productName, cif);
    }

    /**
      * Unregister a CIF previously registered with a product
      * and operation against the specified mode.
      *
      * @param registry the <code>OperationRegistry</code> to unregister from.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      * @param cif the <code>CollectionImageFactory</code> to be unregistered
      *
      * @throws IllegalArgumentException if operationName, productName,
      *             or cif is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      * @throws IllegalArgumentException if the cif was not previously
      *             registered against operationName and productName
      */
    public static void unregister(OperationRegistry registry,
                                  String operationName,
                                  String productName,
                                  CollectionImageFactory cif) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.unregisterFactory(MODE_NAME, operationName, productName, cif);
    }

    /**
      * Sets a preference between two cifs for a given operation under a
      * specified product.
      *
      * @param registry the <code>OperationRegistry</code> to use.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      * @param preferredCIF the preferred cif
      * @param otherCIF the other cif
      *
      * @throws IllegalArgumentException if operationName, productName,
      *             preferredCIF or otherCIF is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      * @throws IllegalArgumentException if either of the cifs
      *             were not previously registered against
      *             operationName and productName
      */
    public static void setPreference(OperationRegistry registry,
                                     String operationName,
                                     String productName,
                                     CollectionImageFactory preferredCIF,
                                     CollectionImageFactory otherCIF) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.setFactoryPreference(MODE_NAME,
		operationName, productName, preferredCIF, otherCIF);
    }

    /**
      * Unsets a preference between two cifs for a given operation under
      * a specified product.
      *
      * @param registry the <code>OperationRegistry</code> to use.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param productName the product name as a <code>String</code>
      * @param preferredCIF the factory object formerly preferred
      * @param otherCIF the other factory object
      *
      * @throws IllegalArgumentException if operationName, productName,
      *             preferredCIF or otherCIF is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      * @throws IllegalArgumentException if either of the cifs
      *             were not previously registered against
      *             operationName and productName
      */
    public static void unsetPreference(OperationRegistry registry,
                                       String operationName,
                                       String productName,
                                       CollectionImageFactory preferredCIF,
                                       CollectionImageFactory otherCIF) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.unsetFactoryPreference(MODE_NAME,
		operationName, productName, preferredCIF, otherCIF);
    }

    /**
      * Removes all preferences between CIFs within a product registered
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
      * Returns a list of the CIFs of a product registered under a
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
      * @return an ordered <code>List</code> of CIFs
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
      * CollectionImageFactory</code> objects registered under the
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
      * @return an <code>Iterator</code> over <code>CollectionImageFactory</code> objects
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
      * Returns the the most preferred <code>CollectionImageFactory</code>
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
      * @return a registered <code>CollectionImageFactory</code> object
      *
      * @throws IllegalArgumentException if operationName is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      */
    public static CollectionImageFactory get(OperationRegistry registry,
					     String operationName) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	return (CollectionImageFactory)
		    registry.getFactory(MODE_NAME, operationName);
    }

    /**
      * Constructs a <code>CollectionImageFactory</code> (usually a
      * <code>CollectionOp</code>) representing the results of applying
      * a given operation to a particular ParameterBlock and rendering
      * hints. The registry is used to determine the CIF to be used to
      * instantiate the operation.
      *
      * <p> If none of the CIFs registered with this
      * <code>OperationRegistry</code> returns a non-null value, null is
      * returned. Exceptions thrown by the CIFs will be caught by this
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
    public static CollectionImage create(OperationRegistry registry,
                                       String operationName,
                                       ParameterBlock paramBlock,
                                       RenderingHints renderHints) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	Object args[] = { paramBlock, renderHints };

	return (CollectionImage)
		registry.invokeFactory(MODE_NAME, operationName, args);
    }

    /**
     * Constructs and returns a <code>PropertySource</code> suitable for
     * use by a given <code>CollectionOp</code>.  The 
     * <code>PropertySource</code> includes properties copied from prior
     * nodes as well as those generated at the node itself. Additionally, 
     * property suppression is taken into account. The actual implementation
     * of <code>getPropertySource()</code> may make use of deferred
     * execution and caching.
     *
     * @param op the <code>CollectionOp</code> requesting its 
     *        <code>PropertySource</code>.
     *
     * @throws IllegalArgumentException if <code>op</code> is <code>null</code>
     * @throws IllegalArgumentException if <code>op.isRenderable()</code>
     *	    returns <code>true</code>
     */
    public static PropertySource getPropertySource(CollectionOp op) {

	if (op == null)
	    throw new IllegalArgumentException("op - " +
			JaiI18N.getString("Generic0"));

	if (op.isRenderable())
	    throw new IllegalArgumentException("op - " +
			JaiI18N.getString("CIFRegistry0"));

	return op.getRegistry().getPropertySource((OperationNode)op);
    }
}
