/*
 * $RCSfile: RMIServerProxy.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:52 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.rmi;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.ParameterBlock;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Iterator;
import java.util.Vector;
import com.lightcrafts.mediax.jai.ImageLayout;
import com.lightcrafts.mediax.jai.remote.NegotiableCapability;
import com.lightcrafts.mediax.jai.OperationNode;
import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.PropertyChangeEventJAI;
import com.lightcrafts.mediax.jai.RenderingChangeEvent;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.remote.JAIRMIDescriptor;
import com.lightcrafts.mediax.jai.remote.NegotiableCapabilitySet;
import com.lightcrafts.mediax.jai.remote.RemoteImagingException;
import com.lightcrafts.mediax.jai.remote.RemoteRenderedOp;
import com.lightcrafts.mediax.jai.remote.PlanarImageServerProxy;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializableRenderedImage;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecDescriptor;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoder;
import com.lightcrafts.mediax.jai.tilecodec.TileDecoderFactory;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.ImageUtil;

public class RMIServerProxy extends PlanarImageServerProxy {

    /** The server object our data will come from. */
    private ImageServer remoteImage = null;

    /** The RMI ID of this object. */
    private Long id;

    /**
     * The ID associated with the Rendering of the corresponding
     * RenderableOp.
     */
    private Long renderingID = null;

    // Boolean to indicate whether PlanarImageServerProxy set the
    // negotiation preferences when super(...) was called.
    private boolean preferencesSet;

    // NegotiableCapabilitySet that stores the negotiation preferences
    // that were set by PlanarImageServerProxy when super(...) was called.
    private NegotiableCapabilitySet negPref;

    // The class of the serializable representation of a NULL property.
    private static final Class NULL_PROPERTY_CLASS =
    com.lightcrafts.media.jai.rmi.JAIRMIImageServer.NULL_PROPERTY.getClass();

    // Cache the listener
    private ImagingListener listener;

    /**
     * Construct an RMIServerProxy. This constructor should only be used
     * when the source is a RenderedOp on a different server.
     */
    public RMIServerProxy(String serverName,
			  String opName,
			  RenderingHints hints) {

	super(serverName, "jairmi", opName, null, hints);

	// Look for a separator indicating the remote image chaining hack
	// in which case the serverName argument contains host[:port]::id
	// where id is the RMI ID of the image on the indicated server.
	int index = serverName.indexOf("::");
	boolean remoteChaining = index != -1;

	if(!remoteChaining) {
	    // Don't throw the IllegalArgumentException if it's the hack.
	    throw new
		IllegalArgumentException(JaiI18N.getString("RemoteImage1"));
	}

	if(remoteChaining) {
	    // Extract the RMI ID from the servername string and replace
	    // the original serverName string with one of the usual type.
	    id = Long.valueOf(serverName.substring(index+2));
	    serverName = serverName.substring(0, index);
	    super.serverName = serverName;
	}

        listener = ImageUtil.getImagingListener(hints);

	remoteImage = getImageServer(serverName);

	if (preferencesSet) {
	    super.setNegotiationPreferences(negPref);
	}

	try {
	    // Increment the reference count for this id on the server
	    remoteImage.incrementRefCount(id);
	} catch (RemoteException re) {
	    System.err.println(JaiI18N.getString("RMIServerProxy2"));
	}
    }

    /**
     * Construct an RMIServerProxy. This constructor should only be used
     * when the source is a RenderedOp on a different server and the
     * ParameterBlock for the source itself is available.
     */
    public RMIServerProxy(String serverName,
			  ParameterBlock pb,
			  String opName,
			  RenderingHints hints){

	super(serverName, "jairmi", opName, pb, hints);

	// Look for a separator indicating the remote image chaining hack
	// in which case the serverName argument contains host[:port]::id
	// where id is the RMI ID of the image on the indicated server.
	int index = serverName.indexOf("::");
	boolean remoteChaining = index != -1;

	if(!remoteChaining) {
	    // Don't throw the IllegalArgumentException if it's the hack.
	    throw new
		IllegalArgumentException(JaiI18N.getString("RemoteImage1"));
	}

	if(remoteChaining) {
	    // Extract the RMI ID from the servername string and replace
	    // the original serverName string with one of the usual type.
	    id = Long.valueOf(serverName.substring(index+2));
	    serverName = serverName.substring(0, index);
	    super.serverName = serverName;
	}

        listener = ImageUtil.getImagingListener(hints);

	remoteImage = getImageServer(serverName);

	if (preferencesSet) {
	    super.setNegotiationPreferences(negPref);
	}

	try {
	    // Increment the reference count for this id on the server
	    remoteImage.incrementRefCount(id);
	} catch (RemoteException re) {
	    System.err.println(JaiI18N.getString("RMIServerProxy2"));
	}
    }

    /**
     * Constructs an RMIServerProxy. This constructor creates nodes on
     * the server corresponding to a RemoteRenderedOp on the client.
     */
    public RMIServerProxy(String serverName, String operationName,
			  ParameterBlock paramBlock, RenderingHints hints) {

	super(serverName, "jairmi", operationName, paramBlock, hints);

        listener = ImageUtil.getImagingListener(hints);

	// Construct the remote RMI image.
	remoteImage = getImageServer(serverName);

	// Get the RMI ID for this object.
	getRMIID();

	// If PlanarImageServerProxy had set the preferences during the
	// call to the super constructor, then honor that now.
	if (preferencesSet)
	    super.setNegotiationPreferences(negPref);

	// Create a RenderedOp on the server for this operation.
	ParameterBlock newPB = (ParameterBlock)paramBlock.clone();
	newPB.removeSources();

	// Check to see whether any of the parameters are images
	JAIRMIUtil.checkClientParameters(newPB, serverName);

	try {
	    SerializableState rhs = SerializerFactory.getState(hints, null);
	    remoteImage.createRenderedOp(id, operationName, newPB, rhs);
	} catch (RemoteException e) {
            String message = JaiI18N.getString("RMIServerProxy5");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, e),
                                   this,
                                   false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}

	RenderedImage source;

	int size = getNumSources();
	for (int i=0; i < size; i++) {

	    source = getSource(i);

	    if (source instanceof RMIServerProxy) {
		try {
		    RMIServerProxy rop = (RMIServerProxy)source;
		    if (rop.serverName.equalsIgnoreCase(this.serverName)){
			// Send the id of the source
			remoteImage.setRenderedSource(id, rop.getRMIID(), i);
		    } else {
			remoteImage.setRenderedSource(id,
						      rop.getRMIID(),
						      rop.serverName,
						      rop.operationName,
						      i);
		    }
		} catch (RemoteException e) {
                    String message = JaiI18N.getString("RMIServerProxy6");
                    listener.errorOccurred(message,
                                           new RemoteImagingException(e),
                                           this, false);
//		    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
		}

	    } else if (source instanceof RenderedOp) {
		/// XXX This should not happen, since by the time a
		// RMIServerProxy is created, all its sources should already
		// have been rendered. In any case, the following deals
		// correctly with the situation if it should arise.

		RenderedOp rop = (RenderedOp)source;
		RenderedImage rendering = rop.getRendering();
		if (!(rendering instanceof Serializable))
		    rendering = new SerializableRenderedImage(rendering);

		try {
		    remoteImage.setRenderedSource(id, rendering, i);
		} catch(RemoteException e) {
                    String message = JaiI18N.getString("RMIServerProxy6");
                    listener.errorOccurred(message,
                                           new RemoteImagingException(message, e),
                                           this, false);

/*
		    throw new RemoteImagingException(
					    ImageUtil.getStackTraceString(e));
*/
		}
	    } else if (source instanceof RenderedImage) {
		try {
		    if (source instanceof Serializable) {
			remoteImage.setRenderedSource(id, source, i);
		    } else {
			remoteImage.setRenderedSource(
				       id,
				       new SerializableRenderedImage(source),
				       i);
		    }

		} catch(RemoteException e) {
                    String message = JaiI18N.getString("RMIServerProxy6");
                    listener.errorOccurred(message,
                                           new RemoteImagingException(message, e),
                                           this, false);
/*
		    throw new RemoteImagingException(
					     ImageUtil.getStackTraceString(e));
*/
		}
	    }
	}

	try {
	    // Increment the reference count for this id on the server
	    remoteImage.incrementRefCount(id);
	} catch (RemoteException re) {
	    System.err.println(JaiI18N.getString("RMIServerProxy2"));
	}
    }

    /**
     * Creates a <code>RMIServerProxy</code> which is the new rendering
     * produced when the serverName is updated.
     */
    public RMIServerProxy(PlanarImageServerProxy oldRendering,
			  OperationNode node,
			  String newServerName) {

	// Simply create a new RMIServerProxy that creates a new node
	// on the new server.
	this(newServerName,
	     node.getOperationName(),
	     node.getParameterBlock(),
	     node.getRenderingHints());
    }

    /**
     * Creates a <code>RMIServerProxy</code> which is the new rendering
     * produced by updating the given old rendering by the changes
     * specified by the given <code>PropertyChangeEventJAI</code>.
     */
    public RMIServerProxy(PlanarImageServerProxy oldRendering,
			  OperationNode node,
			  PropertyChangeEventJAI event) {

	super (((RemoteRenderedOp)node).getServerName(),
	       "jairmi",
	       node.getOperationName(),
	       node.getParameterBlock(),
	       node.getRenderingHints());

        listener =
            ImageUtil.getImagingListener(node.getRenderingHints());

	remoteImage = getImageServer(serverName);

	RMIServerProxy oldRMISP = null;
	if (oldRendering instanceof RMIServerProxy) {
	    oldRMISP = (RMIServerProxy)oldRendering;
	} else {
	    System.err.println(JaiI18N.getString("RMIServerProxy3"));
	}

	Long opID = oldRMISP.getRMIID();

	String propName = event.getPropertyName();
	if (event instanceof RenderingChangeEvent) {
	    // Event is a RenderingChangeEvent
	    RenderingChangeEvent rce = (RenderingChangeEvent)event;

	    // Get index of source which changed.
	    int idx = ((RenderedOp)node).getSources().indexOf(rce.getSource());

	    PlanarImage oldSrcRendering = (PlanarImage)event.getOldValue();

	    Object oldSrc = null;
	    String serverNodeDesc = null;
	    if (oldSrcRendering instanceof RMIServerProxy) {

		RMIServerProxy oldSrcRMISP = (RMIServerProxy)oldSrcRendering;

		if (oldSrcRMISP.getServerName().equalsIgnoreCase(
						 this.serverName) == false) {
		    serverNodeDesc = oldSrcRMISP.getServerName() + "::" +
			oldSrcRMISP.getRMIID();
		} else {
		    serverNodeDesc = oldSrcRMISP.getRMIID().toString();
		}
		oldSrc = serverNodeDesc;
	    } else if (oldSrcRendering instanceof Serializable) {
		oldSrc = oldSrcRendering;
	    } else {
		oldSrc = new SerializableRenderedImage(oldSrcRendering);
	    }

	    Object srcInvalidRegion = rce.getInvalidRegion();
	    SerializableState shapeState =
		SerializerFactory.getState((Shape)srcInvalidRegion, null);

	    Long oldRenderingID = null;
	    try {
		oldRenderingID =
		    remoteImage.handleEvent(opID,
					    idx,
					    shapeState,
					    oldSrc);
	    } catch (RemoteException re) {
                String message = JaiI18N.getString("RMIServerProxy7");
                listener.errorOccurred(message,
                                       new RemoteImagingException(message, re),
                                       this, false);

//		throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	    }

	    oldRMISP.id = oldRenderingID;
	    this.id = opID;

	} else {

	    // Changes to operationName, operationRegistry, protocolName
	    // and protocolAndServerName should never be sent to this
	    // constructor and thus don't need to be handled here.

	    // Changes to serverName should be sent only to the previous
	    // constructor and thus do not need to be handled here.

	    Object oldValue = null, newValue = null;

	    if (propName.equals("operationname")) {

		oldValue = event.getOldValue();
		newValue = event.getNewValue();

	    } else if (propName.equals("parameterblock")) {

		ParameterBlock oldPB = (ParameterBlock)event.getOldValue();
		Vector oldSrcs = oldPB.getSources();
		oldPB.removeSources();

		ParameterBlock newPB = (ParameterBlock)event.getNewValue();
		Vector newSrcs = newPB.getSources();
		newPB.removeSources();

		// XXX Check serverName is correct thing to pass
		JAIRMIUtil.checkClientParameters(oldPB, serverName);
		JAIRMIUtil.checkClientParameters(newPB, serverName);

		oldPB.setSources(JAIRMIUtil.replaceSourcesWithId(oldSrcs,
								 serverName));
		newPB.setSources(JAIRMIUtil.replaceSourcesWithId(newSrcs,
								 serverName));

		oldValue = oldPB;
		newValue = newPB;

	    } else if (propName.equals("sources")) {

		Vector oldSrcs = (Vector)event.getOldValue();
		Vector newSrcs = (Vector)event.getNewValue();

		oldValue = JAIRMIUtil.replaceSourcesWithId(oldSrcs,
							   serverName);
		newValue = JAIRMIUtil.replaceSourcesWithId(newSrcs,
							   serverName);

	    } else if (propName.equals("parameters")) {

		Vector oldParameters = (Vector)event.getOldValue();
		Vector newParameters = (Vector)event.getNewValue();

		// XXX Check serverName is correct thing to pass
		JAIRMIUtil.checkClientParameters(oldParameters, serverName);
		JAIRMIUtil.checkClientParameters(newParameters, serverName);

		oldValue = oldParameters;
		newValue = newParameters;

	    } else if (propName.equals("renderinghints")) {

		RenderingHints oldRH = (RenderingHints)event.getOldValue();
		RenderingHints newRH = (RenderingHints)event.getNewValue();

		oldValue = SerializerFactory.getState(oldRH, null);
		newValue = SerializerFactory.getState(newRH, null);
	    } else {
		throw new RemoteImagingException(
					 JaiI18N.getString("RMIServerProxy4"));
	    }

	    Long oldRenderingID = null;

	    try {
		oldRenderingID = remoteImage.handleEvent(opID,
							 propName,
							 oldValue,
							 newValue);
		// Increment the reference count for this id on the server
		remoteImage.incrementRefCount(oldRenderingID);
	    } catch (RemoteException re) {
                String message = JaiI18N.getString("RMIServerProxy7");
                listener.errorOccurred(message,
                                       new RemoteImagingException(message, re),
                                       this, false);
//		throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	    }

	    oldRMISP.id = oldRenderingID;
	    this.id = opID;
	}

	// If PlanarImageServerProxy had set the preferences during the
	// call to the super constructor, then honor that now.
	if (preferencesSet)
	    super.setNegotiationPreferences(negPref);
    }

    /**
     * Create an RMIServerProxy to access an already created operation
     * (as specified by the supplied id) on the server.
     */
    public RMIServerProxy(String serverName,
			  String operationName,
			  ParameterBlock pb,
			  RenderingHints hints,
			  Long id) {

	super (serverName, "jairmi", operationName, pb, hints);

	listener = ImageUtil.getImagingListener(hints);

	//Construct the the remote ImageServer
	remoteImage = getImageServer(serverName);

	this.id = id;
    }


    /**
     *  the RMIServerProxy for the Renderable Layer
     */
    public RMIServerProxy(String serverName,
			  String operationName,
			  ParameterBlock paramBlock,
			  RenderContext rc,
			  boolean isRender) {

	super(serverName, "jairmi", operationName, paramBlock, null);

	listener = ImageUtil.getImagingListener(rc.getRenderingHints());

	//Construct the the remote ImageServer
	remoteImage = getImageServer(serverName);

	// get the Remote ID
	getRMIID();

	if (preferencesSet)
	    super.setNegotiationPreferences(negPref);

	// Create a RenderableOp on the server for this operation.

	ParameterBlock newPB = (ParameterBlock)paramBlock.clone();
	newPB.removeSources();

	// XXX Since checking to see whether any of the parameters are images
	// causes problems with the "renderable" operator (the RenderedOp
	// downsampler chain needs to be sent to the server as a RenderedOp,
	// and checkClientParameters would make it a RenderedImage), we do
	// not do checkClientParameters here (in renderable). This currently
	// works because there are no renderable operations which have images
	// as parameters. aastha 09/26/01

	try {
	    remoteImage.createRenderableOp(id, operationName, newPB);
	} catch(RemoteException e) {
            String message = JaiI18N.getString("RMIServerProxy8");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, e),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}

	Object source;
	int size = getNumSources();

	for (int i=0; i < size; i++) {

	    Vector sources = paramBlock.getSources();
	    source =  sources.elementAt(i);

	    if (source instanceof RMIServerProxy) {
		try {
		    RMIServerProxy rop = (RMIServerProxy)source;
		    // Send the id of the source
		    if ((rop.serverName).equals(this.serverName)){
			remoteImage.setRenderableSource(id, rop.getRMIID(), i);
		    } else {
			remoteImage.setRenderableSource(id, rop.getRMIID(),
							rop.serverName,
							rop.operationName, i);
		    }
		} catch (RemoteException e) {
                    String message = JaiI18N.getString("RMIServerProxy6");
                    listener.errorOccurred(message,
                                       new RemoteImagingException(message, e),
                                       this, false);
/*
		    throw new RemoteImagingException(
				           ImageUtil.getStackTraceString(e));
*/
		}
	    } else
		if (source instanceof RenderableOp) {
		    try {
			remoteImage.setRenderableSource(id,
							(RenderableOp)source,
							i);
		    } catch(RemoteException e) {
                        String message = JaiI18N.getString("RMIServerProxy6");
                        listener.errorOccurred(message,
                                       new RemoteImagingException(message, e),
                                       this, false);
/*
			throw new RemoteImagingException(
					    ImageUtil.getStackTraceString(e));
*/
		    }
		}
		else if (source instanceof RenderedImage) {
		    try {
			remoteImage.setRenderableSource(
			  id,
			  new SerializableRenderedImage((RenderedImage)source),
			  i);

		    } catch(RemoteException e) {
                        String message = JaiI18N.getString("RMIServerProxy6");
                        listener.errorOccurred(message,
                                       new RemoteImagingException(message, e),
                                       this, false);
/*
			throw new RemoteImagingException(
					    ImageUtil.getStackTraceString(e));
*/
		    }
		}
	}

	try {
	    // Increment the reference count for this id on the server
	    remoteImage.incrementRefCount(id);
	} catch (RemoteException e) {
            String message = JaiI18N.getString("RMIServerProxy9");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, e),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}

	// If this was a call for Rendering of this RenderableOp
	// then render it and store the associated id in
	// renderingID and then RMICRIF will return a new RMISP with reference
	// to that rendering ID
	// This will not be executed at the time of calls to getBounds2D and
	// mapRenderContext

	if (isRender){
	    try {
		renderingID =
		    remoteImage.getRendering(id,
					     SerializerFactory.getState(rc,
									null));
		// Increment the reference count for this id on the server
		remoteImage.incrementRefCount(renderingID);
	    } catch (RemoteException e) {
                String message = JaiI18N.getString("RMIServerProxy10");
                listener.errorOccurred(message,
                                       new RemoteImagingException(message, e),
                                       this, false);
//		throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	    }
	}
    }

    /**
     * Construct an ImageServer on the indicated server.
     *
     * <p> The name of the server must be supplied in the form
     * <pre>
     * host:port
     * </pre>
     * where the port number is optional and may be supplied only if
     * the host name is supplied. If this parameter is null the default
     * is to search for the ImageServer service on the local host at the
     * default <i>rmiregistry</i> port (1099).
     *
     * <p> The result is cached in the instance variable "remoteImage".
     *
     * @param serverName The name of the server in the format described.
     */
    protected synchronized ImageServer getImageServer(String serverName) {

	if (remoteImage == null) {

	    if(serverName == null) {
		try {
		    serverName = InetAddress.getLocalHost().getHostAddress();
		} catch(Exception e) {
                    String message = JaiI18N.getString("RMIServerProxy11");
                    listener.errorOccurred(message,
                                           new RemoteImagingException(message, e),
                                           this, false);
//		    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
		}
            }

	    // Derive the service name.
	    String serviceName =
		new String("rmi://"+serverName+"/"+
			   JAIRMIDescriptor.IMAGE_SERVER_BIND_NAME);

	    // Look up the remote object.
	    remoteImage = null;
	    try {
		remoteImage = (ImageServer)Naming.lookup(serviceName);
	    } catch(Exception e) {
                String message = JaiI18N.getString("RMIServerProxy12");
                listener.errorOccurred(message,
                                       new RemoteImagingException(message, e),
                                       this, false);
//		throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	    }
	}

	return remoteImage;
    }

    /**
     * Get the unique ID to be used to refer to this object on the server.
     * The result is cached in the instance variable "id".
     */
    public synchronized Long getRMIID() {

	if (id != null) {
	    return id;
	}

	try {
	    id = remoteImage.getRemoteID();
	    return id;
	} catch(Exception e) {
            String message = JaiI18N.getString("RMIServerProxy13");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, e),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}

        return id;
    }

    public Long getRenderingID(){
	return renderingID;
    }

    public boolean canBeRendered(){

	boolean cbr = true;  //XXX: please verify
	getImageServer(serverName);
	try {
	    cbr =  remoteImage.getRendering(getRMIID());
	} catch (RemoteException re){
            String message = JaiI18N.getString("RMIServerProxy10");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}

	return cbr;
    }

    /*
     * Disposes of any resources allocated for remote operation.
     */
    protected void finalize() {

	try {
            remoteImage.dispose(id);
	} catch(Exception e) {
	    // Ignore the Exception.
	}

	super.dispose();
    }

    /**
     * Gets the image layout variables from the server and creates
     * an ImageLayout object initialized with these values.
     *
     * @throws RemoteImagingException if a RemoteException is thrown
     *         during the RMI communication.
     */
    public ImageLayout getImageLayout() throws RemoteImagingException {

	ImageLayout layout = new ImageLayout();
	try {
	    layout.setMinX(remoteImage.getMinX(id));
	    layout.setMinY(remoteImage.getMinY(id));
	    layout.setWidth(remoteImage.getWidth(id));
	    layout.setHeight(remoteImage.getHeight(id));
	    layout.setTileWidth(remoteImage.getTileWidth(id));
	    layout.setTileHeight(remoteImage.getTileHeight(id));
	    layout.setTileGridXOffset(remoteImage.getTileGridXOffset(id));
	    layout.setTileGridYOffset(remoteImage.getTileGridYOffset(id));

	    SerializableState smState = remoteImage.getSampleModel(id);
	    layout.setSampleModel((SampleModel)(smState.getObject()));
	    SerializableState cmState = remoteImage.getColorModel(id);
	    layout.setColorModel((ColorModel)(cmState.getObject()));
            return layout;
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RMIServerProxy14");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
            return null;
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}
    }

    /**
     * Gets the requested tile from the server, which does the processing
     * to produce the desired tile.
     *
     * @throws a RemoteImagingException if a RemoteException is thrown
     *         during the RMI communication.
     */
    public Raster computeTile(int tileX, int tileY)
	throws RemoteImagingException {

	// Return null if the requested tile is outside this image's boundary.
        if (tileX < getMinTileX() || tileX > getMaxTileX() ||
            tileY < getMinTileY() || tileY > getMaxTileY()) {
	    return null;
	}

	// Since "tileCodec" is the only category that we care about or honor
	// currently in the remote communication.
	NegotiableCapability codecCap = getNegotiatedValue("tileCodec");

	TileDecoderFactory tdf = null;
	TileCodecParameterList tcpl = null;

	if (codecCap != null) {

	    String category = codecCap.getCategory();
	    String capabilityName = codecCap.getCapabilityName();
	    List generators = codecCap.getGenerators();

	    Class factory;
	    for (Iterator i=generators.iterator(); i.hasNext(); ) {
		factory = (Class)i.next();
		if (tdf == null &&
		    TileDecoderFactory.class.isAssignableFrom(factory)) {

		    try {
			tdf = (TileDecoderFactory)factory.newInstance();
		    } catch (InstantiationException ie) {
			throw new RemoteImagingException(ImageUtil.getStackTraceString(ie));
		    } catch (IllegalAccessException iae) {
			throw new RemoteImagingException(ImageUtil.getStackTraceString(iae));
		    }
		}
	    }

	    if (tdf == null) {
		throw new RemoteImagingException(
				     JaiI18N.getString("RMIServerProxy0"));
	    }

	    TileCodecDescriptor tcd =
		(TileCodecDescriptor)registry.getDescriptor("tileDecoder",
							    capabilityName);

	    if (tcd.includesSampleModelInfo() == false ||
		tcd.includesLocationInfo() == false) {
		throw new RemoteImagingException(
				     JaiI18N.getString("RMIServerProxy1"));
	    }

	    ParameterListDescriptor pld =
		tcd.getParameterListDescriptor("tileDecoder");

	    tcpl = new TileCodecParameterList(capabilityName,
					      new String[] {"tileDecoder"},
					      pld);

	    // Set parameters on TileCodecParameterList only if there are any
	    // parameters defined.
	    if (pld != null) {

		String paramNames[] = pld.getParamNames();
		String currParam;
		Object currValue;
		if (paramNames != null) {
		    for (int i=0; i<paramNames.length; i++) {
			currParam = paramNames[i];
			try {
			    currValue = codecCap.getNegotiatedValue(currParam);
			} catch (IllegalArgumentException iae) {
			    // If this parameter is not defined on the
			    // NegotiableCapability, then move onto the next
			    continue;
			}

			tcpl.setParameter(currParam, currValue);
		    }
		}
	    }
	}

	try {
	    // If a compression hint was set, use it
	    if (codecCap != null) {
		byte ctile[] = remoteImage.getCompressedTile(id,
							     tileX,
							     tileY);
		ByteArrayInputStream stream = new ByteArrayInputStream(ctile);
		TileDecoder decoder = tdf.createDecoder(stream, tcpl);
		try {
		    return decoder.decode();
		} catch (java.io.IOException ioe) {
		    throw new RemoteImagingException(ImageUtil.getStackTraceString(ioe));
		}
	    } else {
		// Ask for uncompressed tiles.
		SerializableState rp = remoteImage.getTile(id, tileX, tileY);
		return (Raster)(rp.getObject());
	    }
	} catch (RemoteException e) {
            String message = JaiI18N.getString("RMIServerProxy15");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, e),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	}

        return null;
    }

    public Object getRemoteProperty(String name)
	throws RemoteImagingException {
	try {
	    Object property = remoteImage.getProperty(id, name);
	    if(NULL_PROPERTY_CLASS.isInstance(property)) {
		property = Image.UndefinedProperty;
	    }
	    return property;
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RMIServerProxy16");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}

        return Image.UndefinedProperty;
    }

    /**
     * Returns a list of names recognized by the <code>getRemoteProperty</code>
     * method. Network errors encountered should be signalled by
     * throwing a RemoteImagingException.
     *
     * @throws RemoteImagingException if an error condition during remote
     *         image processing occurs
     */
    public String[] getRemotePropertyNames() throws RemoteImagingException {
	try {
	    return remoteImage.getPropertyNames(id);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RMIServerProxy17");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}

        return null;
    }

    /**
     * Returns a conservative estimate of the destination region that
     * can potentially be affected by the pixels of a rectangle of a
     * given source. This can be implemented by either asking the server
     * to compute the destination region, or by having the client compute
     * the destination region. Network errors encountered should be
     * signalled by throwing a <code>RemoteImagingException</code>.
     *
     * @param sourceRect  The <code>Rectangle</code> in source coordinates.
     * @param sourceIndex  The index of the source image.
     *
     * @return A <code>Rectangle</code> indicating the potentially
     *         affected destination region, or <code>null</code> if
     *         the region is unknown.
     *
     * @throws IllegalArgumentException  If the source index is
     *         negative or greater than that of the last source.
     * @throws IllegalArgumentException  If <code>sourceRect</code> is
     *         <code>null</code>.
     */
    public Rectangle mapSourceRect(Rectangle sourceRect,
				   int sourceIndex)
	throws RemoteImagingException {

	Rectangle dstRect = null;

	try {
	    dstRect = remoteImage.mapSourceRect(id, sourceRect, sourceIndex);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RMIServerProxy18");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}

	return dstRect;
    }

    /**
     * Returns a conservative estimate of the region of a specified
     * source that is required in order to compute the pixels of a
     * given destination rectangle. Either the server or the client can
     * compute the source region to implement this method. Network errors
     * encountered should be signalled by throwing a
     * <code>RemoteImagingException</code>.
     *
     * @param destRect  The <code>Rectangle</code> in destination coordinates.
     * @param sourceIndex  The index of the source image.
     *
     * @return A <code>Rectangle</code> indicating the required source region.
     *
     * @throws IllegalArgumentException  If the source index is
     *         negative or greater than that of the last source.
     * @throws IllegalArgumentException  If <code>destRect</code> is
     *         <code>null</code>.
     */
    public Rectangle mapDestRect(Rectangle destRect,
				 int sourceIndex)
	throws RemoteImagingException {

	Rectangle srcRect = null;

	try {
	    srcRect = remoteImage.mapDestRect(id, destRect, sourceIndex);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RMIServerProxy18");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}

	return srcRect;
    }

    public void setNegotiationPreferences(NegotiableCapabilitySet preferences)
    {
	if (remoteImage == null) {
	    this.negPref = preferences;
	    preferencesSet = true;
	} else {
	    super.setNegotiationPreferences(preferences);
	}
    }

    /**
     * Informs the server of the negotiated values that are the result of
     * a successful negotiation.
     *
     * @param negotiatedValues    The result of the negotiation.
     */
    public synchronized void setServerNegotiatedValues(NegotiableCapabilitySet
						       negotiatedValues)
	throws RemoteImagingException {
	try {
	    remoteImage.setServerNegotiatedValues(id, negotiatedValues);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RMIServerProxy19");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}
    }
}


