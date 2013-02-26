/*
 * $RCSfile: RCIFRegistry.java,v $
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

import java.awt.image.renderable.ParameterBlock;

import com.lightcrafts.mediax.jai.CollectionImage;
import com.lightcrafts.mediax.jai.CollectionOp;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationNode;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.PropertySource;
import com.lightcrafts.mediax.jai.RenderableCollectionImageFactory;


/**
 * Utility class to provide type-safe interaction
 * with the <code>OperationRegistry</code> for
 * <code>RenderableCollectionImageFactory</code> objects.
 *
 * If the <code>OperationRegistry</code> is <code>null</code>, then
 * <code>JAI.getDefaultInstance().getOperationRegistry()</code> will be used.
 *
 * @see CollectionImage
 * @since JAI 1.1
 */
public final class RCIFRegistry {

    private static final String MODE_NAME = RenderableCollectionRegistryMode.MODE_NAME;

    /**
      * Register a RCIF with a particular operation against a specified
      * mode.
      *
      * @param registry the <code>OperationRegistry</code> to register with.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param rcif the <code>RenderableCollectionImageFactory</code> to be registered
      *
      * @throws IllegalArgumentException if operationName or rcif is
      *		    <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      */
    public static void register(OperationRegistry registry,
                                String operationName,
                                RenderableCollectionImageFactory rcif) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.registerFactory(MODE_NAME, operationName, null, rcif);
    }

    /**
      * Unregister a RCIF previously registered with a operation against
      * the specified mode.
      *
      * @param registry the <code>OperationRegistry</code> to unregister from.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param rcif the <code>RenderableCollectionImageFactory</code> to be unregistered
      *
      * @throws IllegalArgumentException if operationName or rcif is
      *		    <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      * @throws IllegalArgumentException if the rcif was not previously
      *             registered against operationName
      */
    public static void unregister(OperationRegistry registry,
                                  String operationName,
                                  RenderableCollectionImageFactory rcif) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.unregisterFactory(MODE_NAME, operationName, null, rcif);
    }

    /**
      * Returns the <code>RenderableCollectionImageFactory</code> object
      * registered against the operation name.
      *
      * @param registry the <code>OperationRegistry</code> to use.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      *
      * @return a registered <code>RenderableCollectionImageFactory</code> object
      *
      * @throws IllegalArgumentException if operationName is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      */
    public static RenderableCollectionImageFactory get(
		    OperationRegistry registry, String operationName) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	return (RenderableCollectionImageFactory)
		    registry.getFactory(MODE_NAME, operationName);
    }

    /**
      * Creates a renderable <code>CollectionImage</code> given
      * a ParameterBlock containing the operation's sources and
      * parameters. The registry is used to determine the RCIF to be
      * used to instantiate the operation.
      *
      * @param registry the <code>OperationRegistry</code> to use.
      *         if this is <code>null</code>, then <code>
      *         JAI.getDefaultInstance().getOperationRegistry()</code>
      *         will be used.
      * @param operationName the operation name as a <code>String</code>
      * @param paramBlock the operation's ParameterBlock.
      *
      * @throws IllegalArgumentException if operationName is <code>null</code>
      * @throws IllegalArgumentException if there is no <code>
      *             OperationDescriptor</code> registered against
      *             the <code>operationName</code>
      */
    public static CollectionImage create(OperationRegistry registry,
                                         String operationName,
                                         ParameterBlock paramBlock) {

        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	Object args[] = { paramBlock };

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
     *	    returns <code>false</code>
     */
    public static PropertySource getPropertySource(CollectionOp op) {

	if (op == null)
	    throw new IllegalArgumentException("op - " +
			JaiI18N.getString("Generic0"));

	if (!op.isRenderable())
	    throw new IllegalArgumentException("op - " +
			JaiI18N.getString("CIFRegistry1"));

	return op.getRegistry().getPropertySource((OperationNode)op);
    }
}
