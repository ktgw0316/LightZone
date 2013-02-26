/*
 * $RCSfile: RemoteCRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:52 $
 * $State: Exp $
 */package com.lightcrafts.mediax.jai.remote;

import java.awt.geom.Rectangle2D;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.ParameterBlock;

/**
 * The <code>RemoteCRIF</code> interface is equivalent to the 
 * <code>ContextualRenderedImageFactory</code> for operations that are
 * intended to be performed remotely. 
 * 
 * <code>RemoteCRIF</code> provides an interface for the functionality
 * that may differ between instances of <code>RemoteRenderableOp</code>. 
 * Thus different remote operations on <code>RenderableImages</code> may be
 * performed by a single class such as <code>RemoteRenderedOp</code> through
 * the use of multiple instances of <code>RemoteCRIF</code>.
 *
 * <p> All remote operations that are to be used in a rendering-independent
 * chain must provide a factory that implements <code>RemoteCRIF</code>.
 *
 * <p> Classes that implement this interface must provide a
 * constructor with no arguments.
 *
 * @since JAI 1.1
 */
public interface RemoteCRIF extends RemoteRIF {
    
    /**
     * Maps the operation's output <code>RenderContext</code> into a 
     * <code>RenderContext</code> for each of the operation's sources.
     * This is useful for operations that can be expressed in whole or in
     * part simply as alterations in the <code>RenderContext</code>, such as
     * an affine mapping, or operations that wish to obtain lower quality
     * renderings of their sources in order to save processing effort or
     * transmission bandwith.  Some operations, such as blur, can also
     * use this mechanism to avoid obtaining sources of higher quality
     * than necessary.
     *     
     * @param serverName    A <code>String</code> specifying the name of the
     *                      server to perform the remote operation on.
     * @param operationName The <code>String</code> specifying the name of the
     *                      operation to be performed remotely.
     * @param i             The index of the source image.
     * @param renderContext The <code>RenderContext</code> being applied to the
     *                      operation.
     * @param paramBlock    A <code>ParameterBlock</code> containing the
     *                      operation's sources and parameters.
     * @param image the <code>RenderableImage</code> being rendered.
     */
    RenderContext mapRenderContext(String serverName,
				   String operationName,
				   int i,
                                   RenderContext renderContext,
                                   ParameterBlock paramBlock,
                                   RenderableImage image) throws RemoteImagingException;
       
    /**
     * Creates a rendering, given a <code>RenderContext</code> and a 
     * <code>ParameterBlock</code> containing the operation's sources
     * and parameters.  The output is a <code>RemoteRenderedImage</code>
     * that takes the <code>RenderContext</code> into account to
     * determine its dimensions and placement on the image plane.
     * This method houses the "intelligence" that allows a
     * rendering-independent operation to adapt to a specific
     * <code>RenderContext</code>.
     *
     * @param serverName    A <code>String</code> specifying the name of the
     *                      server to perform the remote operation on.
     * @param operationName The <code>String</code> specifying the name of the
     *                      operation to be performed remotely.
     * @param renderContext The <code>RenderContext</code> specifying the
     *                      rendering context.
     * @param paramBlock    A <code>ParameterBlock</code> containing the 
     *                      operation's sources and parameters.
     */
    RemoteRenderedImage create(String serverName,
			       String operationName,
			       RenderContext renderContext,
			       ParameterBlock paramBlock) throws RemoteImagingException;
    
    /**
     * Returns the bounding box for the output of the operation,
     * performed on a given set of sources, in rendering-independent
     * space.  The bounds are returned as a <code>Rectangle2D</code>, 
     * that is, an axis-aligned rectangle with floating-point corner
     * coordinates.
     * 
     * @param serverName    A <code>String</code> specifying the name of the
     *                      server to perform the remote operation on.
     * @param operationName The <code>String</code> specifying the name of the
     *                      operation to be performed remotely.
     * @param paramBlock    A <code>ParameterBlock</code> containing the 
     *                      operation's sources and parameters.
     * @return a <code>Rectangle2D</code> specifying the rendering-independent 
     * bounding box of the output.
     *
     */
    Rectangle2D getBounds2D(String serverName,
			    String operationName,
			    ParameterBlock paramBlock) throws RemoteImagingException;

    /**
     * Gets the appropriate instance of the property specified by the name 
     * parameter.  This method must determine which instance of a property to
     * return when there are multiple sources that each specify the property.
     *     
     * @param serverName    A <code>String</code> specifying the name of the
     *                      server to perform the remote operation on.
     * @param operationName The <code>String</code> specifying the name of the
     *                      operation to be performed remotely.
     * @param paramBlock    A <code>ParameterBlock</code> containing the 
     *                      operation's sources and parameters.
     * @param name          A <code>String</code> naming the desired property.
     * @return an object reference to the value of the property requested.
     */
    Object getProperty(String serverName,
		       String operationName,
		       ParameterBlock paramBlock, 
		       String name) throws RemoteImagingException;

    /** 
     * Returns a list of names recognized by <code>getProperty</code>. 
     *
     * @param serverName    A <code>String</code> specifying the name of the
     *                      server to perform the remote operation on.
     * @param operationName The <code>String</code> specifying the name of the
     *                      operation to be performed remotely.
     */
    String[] getPropertyNames(String serverName, String operationName) throws RemoteImagingException;

    /**
     * Returns true if successive renderings (that is, calls to
     * create(<code>RenderContext</code>, <code>ParameterBlock</code>))
     * with the same arguments may produce different results.  This method
     * may be used to determine whether an existing rendering may be cached
     * and reused. It is always safe to return true.
     *
     * @param serverName    A <code>String</code> specifying the name of the
     *                      server to perform the remote operation on.
     * @param operationName The <code>String</code> specifying the name of the
     *                      operation to be performed remotely.
     */
    boolean isDynamic(String serverName, String operationName) throws RemoteImagingException;
}
