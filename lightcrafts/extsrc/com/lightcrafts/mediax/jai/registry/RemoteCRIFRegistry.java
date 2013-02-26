/*
 * $RCSfile: RemoteCRIFRegistry.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:48 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.registry;

import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderContext;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.remote.RemoteCRIF;
import com.lightcrafts.mediax.jai.remote.RemoteRenderedImage;

/**
 * Utility class to provide type-safe interaction with the
 * <code>OperationRegistry</code> for <code>RemoteCRIF</code> objects.
 *
 * If the <code>OperationRegistry</code> specified as an argument to the
 * methods in this class is null, then <code>JAI.getOperationRegistry()</code>
 * will be used.
 *
 * @since JAI 1.1
 */
public final class RemoteCRIFRegistry  {

    private static final String MODE_NAME = RemoteRenderableRegistryMode.MODE_NAME;

    /**
     * Registers the given <code>RemoteCRIF</code> with the given 
     * <code>OperationRegistry</code> under the given protocolName
     *
     * @param registry     The <code>OperationRegistry</code> to register the 
     *                     <code>RemoteCRIF</code> with. If this is
     *                     <code>null</code>, then <code>
     *                     JAI.getDefaultInstance().getOperationRegistry()</code>
     *                     will be used.
     * @param protocolName The protocolName to register the 
     *                     <code>RemoteCRIF</code> under.
     * @param rcrif        The <code>RemoteCRIF</code> to register.
     *
     * @throws IllegalArgumentException if protocolName is null.
     * @throws IllegalArgumentException if rcrif is null.
     * @throws IllegalArgumentException if there is no 
     * <code>RemoteDescriptor</code> registered against the 
     * given protocolName.
     */
    public static void register(OperationRegistry registry,
                                String protocolName,
                                RemoteCRIF rcrif) {
		
        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.registerFactory(MODE_NAME, protocolName, null, rcrif);
    }

    /**
     * Unregisters the given <code>RemoteCRIF</code> previously registered 
     * under the given protocolName in the given
     * <code>OperationRegistry</code>.
     *
     * @param registry     The <code>OperationRegistry</code> to unregister the 
     *                     <code>RemoteCRIF</code> from. If this is
     *                     <code>null</code>, then <code>
     *                     JAI.getDefaultInstance().getOperationRegistry()</code>
     *                     will be used.
     * @param protocolName The protocolName to unregister the
     *                     <code>RemoteCRIF</code> from under.
     * @param rcrif        The <code>RemoteCRIF</code> to unregister.
     *
     * @throws IllegalArgumentException if protocolName is null.
     * @throws IllegalArgumentException if rcrif is null.
     * @throws IllegalArgumentException if there is no 
     * <code>RemoteDescriptor</code> registered against the 
     * given protocolName.
     * @throws IllegalArgumentException if the rcrif was not previously
     * registered against protocolName.
     */
    public static void unregister(OperationRegistry registry,
                                  String protocolName,
                                  RemoteCRIF rcrif) {
		
        registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	registry.unregisterFactory(MODE_NAME, protocolName, null, rcrif);
    }

    /**
     * Returns the <code>RemoteCRIF</code> registered under the given
     * protocol name in the specified <code>OperationRegistry</code>.
     *
     * @param registry     The <code>OperationRegistry</code> to use. 
     *                     If this is <code>null</code>, then <code>
     *                     JAI.getDefaultInstance().getOperationRegistry()</code>
     *                     will be used.
     * @param protocolName The name of the remote imaging protocol.
     *
     * @throws IllegalArgumentException if protocolName is null.
     * @throws IllegalArgumentException if there is no
     * <code>RemoteDescriptor</code> registered against the given
     * <code>protocolName</code>.
     */
    public static RemoteCRIF get(OperationRegistry registry,
				 String protocolName) {
	
	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	return (RemoteCRIF)registry.getFactory(MODE_NAME, protocolName);
    }

    /**
     * Creates a rendering remotely, given the serverName, protocolName,
     * name of the operation to be performed, a <code>RenderContext</code> and
     * a <code>ParameterBlock</code> containing the operation's sources
     * and parameters. The registry is used to determine the 
     * <code>RemoteCRIF</code> to be used to instantiate the operation.
     *
     * <p>Since this class is a simple type-safe wrapper around 
     * <code>OperationRegistry</code>'s type-unsafe methods, no additional
     * argument validation is performed in this method. Thus errors/exceptions
     * may occur if incorrect values are provided for the input arguments.
     * If argument validation is desired as part of creating a rendering,
     * <code>RemoteJAI.createRenderable()</code> may be used instead to 
     * create a <code>RemoteRenderableOp</code> which can then be asked for
     * a rendering.
     *
     * <p>Exceptions thrown by the <code>RemoteRIF</code>s used to create
     * the rendering will be caught by this method and will not be propagated.
     *
     * @param registry      The <code>OperationRegistry</code> to use to
     *                      create the rendering. If this is
     *                      <code>null</code>, then <code>
     *                     JAI.getDefaultInstance().getOperationRegistry()</code>
     *                      will be used.
     * @param protocolName  The protocol to be used for remote imaging.
     * @param serverName    The name of the server.
     * @param operationName The name of the operation to be performed remotely.
     * @param renderContext A <code>RenderContext</code> specifying the
     *                      context in which the rendering should be requested.
     * @param paramBlock    The <code>ParameterBlock</code> specifying the
     *                      sources and parameters required for the operation.
     *
     * @throws IllegalArgumentException if protocolName is null.
     * @throws IllegalArgumentException if there is no 
     * <code>RemoteDescriptor</code> registered against the given
     * protocolName.
     */
    public static RemoteRenderedImage create(OperationRegistry registry,
					     String protocolName,
					     String serverName,
					     String operationName,
					     RenderContext renderContext,
					     ParameterBlock paramBlock) {

	registry = (registry != null) ? registry :
	    JAI.getDefaultInstance().getOperationRegistry();

	Object args[] = { serverName, 
			  operationName,
			  renderContext,
			  paramBlock };

	return (RemoteRenderedImage)
		registry.invokeFactory(MODE_NAME, protocolName, args);
    }

}
