/*
 * $RCSfile: RemoteRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:52 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

import java.awt.RenderingHints;
import java.awt.image.renderable.ParameterBlock;

import com.lightcrafts.mediax.jai.OperationNode;
import com.lightcrafts.mediax.jai.PropertyChangeEventJAI;

/**
 * The <code>RemoteRIF</code> interface is intended to be implemented by
 * classes that wish to act as factories to produce different renderings
 * remotely, for example by executing a series of remote operations on
 * a set of sources, depending on a specific set of parameters, properties, 
 * and rendering hints.  
 *
 * <p> All factories that produce renderings for operations remotely 
 * must implement <code>RemoteRIF</code>.
 *
 * <p> Classes that implement this interface must provide a
 * constructor with no arguments.
 *
 * @since JAI 1.1
 */
public interface RemoteRIF {

    /**
     * Creates a <code>RemoteRenderedImage</code> representing the results 
     * of an imaging operation (or chain of operations) for a given
     * <code>ParameterBlock</code> and <code>RenderingHints</code>. The 
     * <code>RemoteRIF</code> may also query any source images
     * referenced by the <code>ParameterBlock</code> for their dimensions,
     * <code>SampleModel</code>s, properties, etc., as necessary.
     *
     * <p> The <code>create()</code> method should return null if the
     * <code>RemoteRIF</code> (representing the server) is not capable of
     * producing output for the given set of source images and parameters.
     * For example, if a server (represented by a <code>RemoteRIF</code>) is
     * only capable of performing a 3x3 convolution on single-banded image
     * data, and the source image has multiple bands or the convolution
     * Kernel is 5x5, null should be returned.
     *
     * <p> Hints should be taken into account, but can be ignored.
     * The created <code>RemoteRenderedImage</code> may have a property
     * identified by the String HINTS_OBSERVED to indicate which
     * <code>RenderingHints</code> were used to create the image. In addition
     * any <code>RenderedImage</code>s that are obtained via the getSources()
     * method on the created <code>RemoteRenderedImage</code> may have such
     * a property.
     *    
     * @param serverName    A <code>String</code> specifying the name of the 
     *                      server to perform the remote operation on.
     * @param operationName The <code>String</code> specifying the name of the
     *                      operation to be performed remotely.
     * @param paramBlock    A <code>ParameterBlock</code> containing sources
     *                      and parameters for the 
     *                      <code>RemoteRenderedImage</code> to be created.
     * @param hints         A <code>RenderingHints</code> object containing
     *                      hints.
     * @return A <code>RemoteRenderedImage</code> containing the desired
     * output.
     */
    RemoteRenderedImage create(String serverName,
			       String operationName,
			       ParameterBlock paramBlock, 
			       RenderingHints hints) 
	throws RemoteImagingException;

    /**
     * Creates a <code>RemoteRenderedImage</code> representing the results
     * of an imaging operation represented by the given 
     * <code>OperationNode</code>, whose given old rendering is updated
     * according to the given <code>PropertyChangeEventJAI</code>. This
     * factory method should be used to create a new rendering updated
     * according to the changes reported by the given
     * <code>PropertyChangeEventJAI</code>. The <code>RemoteRIF</code>
     * can query the supplied <code>OperationNode</code> for
     * references to the server name, operation name, parameter block,
     * and rendering hints. If only a new rendering of the node is desired
     * in order to handle the supplied <code>PropertyChangeEventJAI</code>,
     * the rendering can be obtained by calling the default 
     * <code>create()</code> method, the arguments to which can be
     * retrieved from the supplied <code>OperationNode</code>. 
     * The <code>RemoteRIF</code> may also query
     * any source images referenced by the <code>ParameterBlock</code>
     * for their dimensions, <code>SampleModel</code>s, properties, etc.,
     * as necessary. The supplied <code>OperationNode</code> should
     * not be edited during the creation of the new rendering, otherwise
     * the <code>OperationNode</code> might have an inconsistent state.
     *
     * <p> The <code>create()</code> method can return null if the
     * <code>RemoteRIF</code> (representing the server) is not capable of
     * producing output for the given set of source images and parameters.
     * For example, if a server (represented by a <code>RemoteRIF</code>) is
     * only capable of performing a 3x3 convolution on single-banded image
     * data, and the source image has multiple bands or the convolution
     * Kernel is 5x5, null should be returned.
     *
     * <p> Hints should be taken into account, but can be ignored.
     * The created <code>RemoteRenderedImage</code> may have a property
     * identified by the String HINTS_OBSERVED to indicate which
     * <code>RenderingHints</code> were used to create the image. In addition
     * any <code>RenderedImage</code>s that are obtained via the getSources()
     * method on the created <code>RemoteRenderedImage</code> may have such
     * a property.
     *    
     * @param oldRendering The old rendering of the imaging operation.
     * @param node         The <code>OperationNode</code> that represents the
     *                     imaging operation.
     * @param event        An event that specifies the changes made to the
     *                     imaging operation.
     * @return A <code>RemoteRenderedImage</code> containing the desired
     * output.
     */
    RemoteRenderedImage create(PlanarImageServerProxy oldRendering, 
			       OperationNode node,
			       PropertyChangeEventJAI event) 
	throws RemoteImagingException;
    
    /**
     * Returns the set of capabilities supported by the client object.
     */
    NegotiableCapabilitySet getClientCapabilities();
}



