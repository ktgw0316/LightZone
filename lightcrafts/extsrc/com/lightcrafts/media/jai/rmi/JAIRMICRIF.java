/*
 * $RCSfile: JAIRMICRIF.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:51 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.rmi;

import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.Vector;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.OperationNode;
import com.lightcrafts.mediax.jai.PropertyChangeEventJAI;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.remote.RemoteCRIF;
import com.lightcrafts.mediax.jai.remote.JAIRMIDescriptor;
import com.lightcrafts.mediax.jai.remote.NegotiableCapabilitySet;
import com.lightcrafts.mediax.jai.remote.PlanarImageServerProxy;
import com.lightcrafts.mediax.jai.remote.RemoteImagingException;
import com.lightcrafts.mediax.jai.remote.RemoteRenderableOp;
import com.lightcrafts.mediax.jai.remote.RemoteRenderedImage;
import com.lightcrafts.mediax.jai.remote.RemoteRenderedOp;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;
import com.lightcrafts.mediax.jai.remote.SerializableRenderedImage;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoderFactory;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * An implementation of the <code>RemoteRIF</code> interface for the
 * "jairmi" remote imaging protocol.
 */
public class JAIRMICRIF implements RemoteCRIF {

    /**
     * No arg constructor.
     */
    public JAIRMICRIF() {
    }

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
    public RenderContext mapRenderContext(String serverName,
					  String operationName,
					  int i,
					  RenderContext renderContext,
					  ParameterBlock paramBlock,
					  RenderableImage image)
	throws RemoteImagingException {

	// Create the RenderableOp on the server and it's sources on the
	// appropriate machines.
	RemoteRenderableOp rrop = (RemoteRenderableOp)image;
	RenderableRMIServerProxy rmisp = createProxy(rrop);

	SerializableState rcs =
	    SerializerFactory.getState(renderContext, null);

	// Perform the mapRenderContext on the server.
	try {
	    SerializableState rcpOut =
		rmisp.getImageServer(serverName).mapRenderContext(
							      i,
							      rmisp.getRMIID(),
							      operationName,
							      rcs);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("JAIRMICRIF5");
            sendExceptionToListener(renderContext, message, re);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}

	return (RenderContext)rcs.getObject();
    }

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
     */
    public Rectangle2D getBounds2D(String serverName,
				   String operationName,
				   ParameterBlock paramBlock)
	throws RemoteImagingException {

	SerializableState bounds = null;

	// Create a RemoteRenderableOp that represents the operation.
	RemoteRenderableOp original = new RemoteRenderableOp("jairmi",
							     serverName,
							     operationName,
							     paramBlock);

	// Create the RenderableOp on the server alongwith creating the
	// sources on appropriate machines.
	RenderableRMIServerProxy rmisp = createProxy(original);

	try {
	    bounds =
		rmisp.getImageServer(serverName).getBounds2D(rmisp.getRMIID(),
							     operationName);
	} catch (RemoteException e) {
            String message = JaiI18N.getString("JAIRMICRIF6");
            sendExceptionToListener(null, message, e);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}

	return (Rectangle2D.Float)bounds.getObject();
    }

    /**
     * Gets the appropriate instance of the property specified by the name
     * parameter.  This method must determine which instance of a property to
     * return when there are multiple sources that each specify the property.
     *
     * @param paramBlock a ParameterBlock containing the operation's
     *        sources and parameters.
     * @param name a String naming the desired property.
     * @return an object reference to the value of the property requested.
     */
    public Object getProperty(String serverName,
			      String operationName,
			      ParameterBlock paramBlock,
			      String name) throws RemoteImagingException {

	ParameterBlock pb = null;
	if(paramBlock == null){
	    pb = new ParameterBlock();
	} else {
	    pb = (ParameterBlock)paramBlock.clone();
	}

	// Create a RemoteRenderableOp that represents the operation.
	RemoteRenderableOp original = new RemoteRenderableOp("jairmi",
							     serverName,
							     operationName,
							     paramBlock);

	// Create the RenderableOp on the server alongwith creating the
	// sources on appropriate machines.
	RenderableRMIServerProxy rmisp = createProxy(original);

 	try {
	    return rmisp.getProperty(name);
	} catch(Exception e) {
            String message = JaiI18N.getString("JAIRMICRIF7");
            sendExceptionToListener(null, message,
                                    new RemoteImagingException(message, e));
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}
        return null;
    }

    /**
     * Returns a list of names recognized by getProperty.
     */
    public String[] getPropertyNames(String serverName,
				     String operationName)
	throws RemoteImagingException {

	ImageServer remoteImage = getImageServer(serverName);
	try {
	    return remoteImage.getPropertyNames(operationName);
	} catch (RemoteException e){
	    // Should we be catching Exception or RemoteException
            String message = JaiI18N.getString("JAIRMICRIF8");
            sendExceptionToListener(null, message,
                                    new RemoteImagingException(message, e));
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}
        return null;
    }

    private ImageServer getImageServer(String serverName){

	if (serverName == null) {
	    try {
		serverName = InetAddress.getLocalHost().getHostAddress();
	    } catch(java.net.UnknownHostException e) {
                String message = JaiI18N.getString("RMIServerProxy11");
                sendExceptionToListener(null, message,
                                        new RemoteImagingException(message, e));
//		throw new RuntimeException(e.getMessage());
	    }
	}

	// Derive the service name.
	String serviceName =
	    new String("rmi://"+serverName+"/"+
		       JAIRMIDescriptor.IMAGE_SERVER_BIND_NAME);

	// Look up the remote object.
	try {
	    return (ImageServer)Naming.lookup(serviceName);
	} catch(java.rmi.NotBoundException e) {
            String message = JaiI18N.getString("RMIServerProxy12");
            sendExceptionToListener(null, message,
                                    new RemoteImagingException(message, e));
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	} catch (java.net.MalformedURLException e) {
            String message = JaiI18N.getString("RMIServerProxy12");
            sendExceptionToListener(null, message,
                                    new RemoteImagingException(message, e));
        } catch (java.rmi.RemoteException e) {
            String message = JaiI18N.getString("RMIServerProxy12");
            sendExceptionToListener(null, message,
                                    new RemoteImagingException(message, e));
        }

        return null;
    }

    /**
     * Returns true if successive renderings (that is, calls to
     * create(RenderContext, ParameterBlock)) with the same arguments
     * may produce different results.  This method may be used to
     * determine whether an existing rendering may be cached and
     * reused.  It is always safe to return true.
     */
    public boolean isDynamic(String serverName,
			     String operationName)
	throws RemoteImagingException {

	ImageServer remoteImage = getImageServer(serverName);
	try {
	    return remoteImage.isDynamic(operationName);
	} catch (RemoteException e){
            String message = JaiI18N.getString("JAIRMICRIF9");
            sendExceptionToListener(null, message,
                                    new RemoteImagingException(message, e));
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}
        return true;
    }

    /**
     * Creates a <code>RemoteRenderedImage</code> representing the results
     * of an imaging operation (or chain of operations) for a given
     * <code>ParameterBlock</code> and <code>RenderingHints</code>. The
     * <code>RemoteRIF</code> may also query any source images
     * referenced by the <code>ParameterBlock</code> for their dimensions,
     * <code>SampleModel</code>s, properties, etc., as necessary.
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
    public RemoteRenderedImage create(String serverName,
				      String operationName,
				      ParameterBlock paramBlock,
				      RenderingHints hints)
	throws RemoteImagingException {

	// Create the RenderedOp on the server
	RMIServerProxy rmisp = new RMIServerProxy(serverName,
						  operationName,
						  paramBlock,
						  hints);

	// Check whether there is a RIF on the server that can satisfy this
	// rendering request.
	boolean cbr = rmisp.canBeRendered();

	if (!cbr){
	    return null;
	} else {
	    return rmisp;
	}
    }

    /**
     * Creates a <code>RemoteRenderedImage</code> representing the results
     * of an imaging operation, whose given old rendering is updated
     * according to the given <code>PropertyChangeEventJAI</code>. This
     * factory method should be used to create a new rendering updated
     * according to the changes reported by the given
     * <code>PropertyChangeEventJAI</code>. The <code>RemoteRIF</code>
     * can query the supplied <code>PlanarImageServerProxy</code> for
     * references to the server name, operation name, parameter block,
     * and rendering hints. The <code>RemoteRIF</code> may also query
     * any source images referenced by the <code>ParameterBlock</code>
     * for their dimensions, <code>SampleModel</code>s, properties, etc.,
     * as necessary.
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
     * @param event        An event that specifies the changes made to the
     *                     imaging operation.
     * @return A <code>RemoteRenderedImage</code> containing the desired
     * output.
     */
    public RemoteRenderedImage create(PlanarImageServerProxy oldRendering,
				      OperationNode node,
			       	      PropertyChangeEventJAI event)
	throws RemoteImagingException {

	if (!(node instanceof RemoteRenderedOp))
	    return null;

	String propName = event.getPropertyName();
	RMIServerProxy rmisp;
	if (propName.equals("servername")) {
	    rmisp = new RMIServerProxy(oldRendering,
				       node,
				       (String)event.getNewValue());
	} else if (propName.equals("operationregistry") ||
		   propName.equals("protocolname") ||
		   propName.equals("protocolandservername")) {

	    return create(((RemoteRenderedOp)node).getServerName(),
			  node.getOperationName(),
			  node.getParameterBlock(),
			  node.getRenderingHints());

	} else {
	    rmisp = new RMIServerProxy(oldRendering, node, event);
	}

	return rmisp;
    }

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
    public RemoteRenderedImage create(String serverName,
				      String operationName,
				      RenderContext renderContext,
				      ParameterBlock paramBlock)
	throws RemoteImagingException {

	// Create the RenderableOp on the server.
	RMIServerProxy rmisp = new RMIServerProxy(serverName,
						  operationName,
						  paramBlock,
						  renderContext,
						  true);

	// Cause a rendering to take place, so we can return a RenderedImage
	// since we can't return a RenderableOp.
	Long renderingID = rmisp.getRenderingID();

	// Return an RMIServerProxy that is a link to the rendering on the
	// server.
	return new RMIServerProxy((serverName+"::"+renderingID),
				  paramBlock,
				  operationName,
				  renderContext.getRenderingHints());
    }

    /**
     * Returns the set of capabilities supported by the client object.
     */
    public  NegotiableCapabilitySet getClientCapabilities() {

       OperationRegistry registry =
           JAI.getDefaultInstance().getOperationRegistry();
       String modeName = "tileDecoder";
       String[] descriptorNames = registry.getDescriptorNames(modeName);
       TileDecoderFactory tdf = null;

       // Only non-preference NegotiableCapability objects can be added
       // to this NCS, which is ok, since these represent the capabilities
       // of the client and thus are not preferences as much as they are
       // hard capabilities (i.e. non-preferences)
       NegotiableCapabilitySet capabilities =
           new NegotiableCapabilitySet(false);

       // Note that tileEncoder capabilities cannot be added since there is
       // no way to differentiate between a encoding and a decoding capability
       // within an NCS, and since the client should only be expected to
       // decode, this should be ok.

       Iterator it;
       for (int i=0; i<descriptorNames.length; i++) {

	   it = registry.getFactoryIterator(modeName, descriptorNames[i]);
	   for (; it.hasNext(); ) {
	       tdf = (TileDecoderFactory)it.next();
	       capabilities.add(tdf.getDecodeCapability());
	   }
       }

       return capabilities;
    }

    // Recursive evaluation of sources.

    /**
     * Create a <code>RenderableRMIServerProxy</code> that represents
     * a remote Renderable operation. This method examines the operation's
     * sources recursively and creates them as needed.
     */
    private RenderableRMIServerProxy createProxy(RemoteRenderableOp rop) {

	ParameterBlock oldPB = rop.getParameterBlock();
	ParameterBlock newPB = (ParameterBlock)oldPB.clone();
	Vector sources = oldPB.getSources();
	newPB.removeSources();

	// Create a new RenderableOp on the server
	ImageServer remoteImage = getImageServer(rop.getServerName());
        ImagingListener listener =
                ImageUtil.getImagingListener(rop.getRenderingHints());
	Long opID = new Long(0L);
	try {
	    opID = remoteImage.getRemoteID();
	    remoteImage.createRenderableOp(opID,
					   rop.getOperationName(),
					   newPB);
	} catch (RemoteException e){
            String message = JaiI18N.getString("RMIServerProxy8");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, e),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}

	// Now look at the sources of the RenderableOp
	if (sources != null) {

	    for (int i = 0; i < sources.size(); i++) {

		Object source = sources.elementAt(i);

		if (source instanceof RemoteRenderedOp) {

		    // If source is a RenderedOp on a remote machine, create
		    // it on the remote machine.
		    RMIServerProxy rmisp =
		(RMIServerProxy)(((RemoteRenderedOp)source).getRendering());

		    try {
			if (rmisp.getServerName().equalsIgnoreCase(
							rop.getServerName())) {

			    // Both the RenderableOp and this source are on
			    // the same server.
			    remoteImage.setRenderedSource(opID,
							  rmisp.getRMIID(),
							  i);
			    newPB.setSource(rmisp, i);
			} else {

			    // The RenderableOp and this source are on
			    // different servers.
			    remoteImage.setRenderedSource(
						   opID,
						   rmisp.getRMIID(),
						   rmisp.getServerName(),
						   rmisp.getOperationName(),
						   i);
			    newPB.setSource(rmisp, i);
			}
		    } catch (RemoteException e) {
                        String message = JaiI18N.getString("RMIServerProxy6");
                        listener.errorOccurred(message,
                                               new RemoteImagingException(message, e),
                                               this, false);
//			throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
		    }

		} else if (source instanceof RenderedOp) {

		    // If the source is a local RenderedOp, then the only way
		    // to access it from a remote machine is to render it and
		    // create a SerializableRenderedImage wrapping the
		    // rendering, which is set as the source of the remote op.
		    RenderedImage ri = ((RenderedOp)source).getRendering();
		    try {
			SerializableRenderedImage sri =
			    new SerializableRenderedImage(ri);
			remoteImage.setRenderedSource(opID, sri, i);
			newPB.setSource(sri, i);
		    } catch (RemoteException e) {
                        String message = JaiI18N.getString("RMIServerProxy6");
                        listener.errorOccurred(message,
                                               new RemoteImagingException(message, e),
                                               this, false);
//			throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
		    }

		} else if (source instanceof RenderedImage) {

		    // If the source is a local RenderedImage, then the only
		    // way to access it from a remote machine is by wrapping
		    // it in SerializableRenderedImage and then setting the
		    // SRI as the source.
		    RenderedImage ri = (RenderedImage)source;
		    try {
			SerializableRenderedImage sri =
			    new SerializableRenderedImage(ri);
			remoteImage.setRenderedSource(opID, sri, i);
			newPB.setSource(sri, i);
		    } catch (RemoteException e) {
                        String message = JaiI18N.getString("RMIServerProxy6");
                        listener.errorOccurred(message,
                                               new RemoteImagingException(message, e),
                                               this, false);
//			throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
		    }

		} else if (source instanceof RemoteRenderableOp) {

		    // If the source is a RenderableOp on a remote machine,
		    // cause the RenderableOp to be created on the remote
		    // machine.
		    RenderableRMIServerProxy rrmisp =
			createProxy((RemoteRenderableOp)source);

		    try {

			// If the source RenderableOp is on the same server
			// as the RenderableOp that represents the operation
			if (rrmisp.getServerName().equalsIgnoreCase(
							rop.getServerName())) {
			    remoteImage.setRenderableSource(opID,
							    rrmisp.getRMIID(),
							    i);
			    newPB.setSource(rrmisp, i);
			} else {
			    // If the source RenderableOp is on a different
			    // server than the RenderableOp that represents
			    // the operation
			    remoteImage.setRenderableRMIServerProxyAsSource(
					      opID,
					      rrmisp.getRMIID(),
					      rrmisp.getServerName(),
					      rrmisp.getOperationName(),
					      i);
			    newPB.setSource(rrmisp, i);
			}
		    } catch (RemoteException e) {
                        String message = JaiI18N.getString("RMIServerProxy6");
                        listener.errorOccurred(message,
                                               new RemoteImagingException(message, e),
                                               this, false);
//			throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
		    }

		} else if (source instanceof RenderableImage) {

		    // If the source is a local RenderableImage, the only way
		    // to access it from a remote machine is to wrap it in
		    // a SerializableRenderableImage and then set the SRI as
		    // the source on the remote operation.
		    RenderableImage ri = (RenderableImage)source;
		    try {
			SerializableRenderableImage sri =
			    new SerializableRenderableImage(ri);
			remoteImage.setRenderableSource(opID, sri, i);
			newPB.setSource(sri, i);
		    } catch (RemoteException e) {
                        String message = JaiI18N.getString("RMIServerProxy6");
                        listener.errorOccurred(message,
                                               new RemoteImagingException(message, e),
                                               this, false);
//			throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
		    }
		}

	    }
	}

	// Create a new RMIServerProxy to access the RenderableOp
	// created at the beginning of this method.
	RenderableRMIServerProxy finalRmisp =
	    new RenderableRMIServerProxy(rop.getServerName(),
					 rop.getOperationName(),
					 newPB,
					 opID);
	return finalRmisp;
    }

    private void sendExceptionToListener(RenderContext renderContext,
                                         String message,
                                         Exception e) {
        ImagingListener listener =
            ImageUtil.getImagingListener(renderContext);
        listener.errorOccurred(message,
                               new RemoteImagingException(message, e),
                               this, false);
    }
}
