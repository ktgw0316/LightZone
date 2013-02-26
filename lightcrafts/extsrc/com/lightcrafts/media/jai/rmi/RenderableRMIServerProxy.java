/*
 * $RCSfile: RenderableRMIServerProxy.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:53 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.rmi;

import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderableImage;
import java.awt.image.renderable.RenderContext;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Vector;
import com.lightcrafts.mediax.jai.remote.JAIRMIDescriptor;
import com.lightcrafts.mediax.jai.remote.RemoteImagingException;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.ImageUtil;

/**
 * A class that represents and allows access to a
 * <code>RenderableOp</code> on a remote machine.
 */
public class RenderableRMIServerProxy implements RenderableImage {

    /** The name of the server where the <code>RenderableOp</code> exists. */
    private String serverName;

    /** The name of the operation that is represented by this class. */
    private String operationName;

    /** The <code>ParameterBlock</code> for the operation. */
    private ParameterBlock paramBlock;

    /** A reference to the ImageServer object  */
    private ImageServer imageServer;

    /** The ID that refers to the <code>RenderableOp</code> on the server. */
    public Long id;

    // The class of the serializable representation of a NULL property.
    private static final Class NULL_PROPERTY_CLASS =
    com.lightcrafts.media.jai.rmi.JAIRMIImageServer.NULL_PROPERTY.getClass();

    // Cache the imaging listener
    private ImagingListener listener;

    /**
     * Creates a <code>RenderableRMIServerProxy</code> to access the
     * <code>RenderableOp</code> on the server identified by the
     * supplied <code>opID</code>.
     */
    public RenderableRMIServerProxy(String serverName,
				    String operationName,
				    ParameterBlock paramBlock,
				    Long opID) {

	this.serverName = serverName;
	this.operationName = operationName;
	this.paramBlock = paramBlock;
	imageServer = getImageServer(serverName);
	this.id = opID;
        listener = ImageUtil.getImagingListener((RenderingHints)null);
    }

    /**
     * Returns a vector of RenderableImages that are the sources of
     * image data for this RenderableImage. Note that this method may
     * return an empty vector, to indicate that the image has no sources,
     * or null, to indicate that no information is available.
     *
     * @return a (possibly empty) Vector of RenderableImages, or null.
     */
    public Vector getSources() {
	return null;
    }

    /**
     * Gets a property from the property set of this image.
     * If the property name is not recognized, java.awt.Image.UndefinedProperty
     * will be returned.
     *
     * @param name the name of the property to get, as a String.
     * @return a reference to the property Object, or the value
     *         java.awt.Image.UndefinedProperty.
     */
    public Object getProperty(String name) throws RemoteImagingException {

	try {
	    Object property = imageServer.getProperty(id, name);
	    if (NULL_PROPERTY_CLASS.isInstance(property)) {
		property = Image.UndefinedProperty;
	    }
	    return property;
	} catch (RemoteException re) {
            String message = JaiI18N.getString("JAIRMICRIF7");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}
        return null;
    }

    /**
     * Returns a list of names recognized by getProperty.
     * @return a list of property names.
     */
    public String[] getPropertyNames() throws RemoteImagingException {
	try {
	    return imageServer.getPropertyNames(id);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("JAIRMICRIF8");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}
        return null;
    }

    /**
     * Returns true if successive renderings (that is, calls to
     * createRendering() or createScaledRendering()) with the same arguments
     * may produce different results.  This method may be used to
     * determine whether an existing rendering may be cached and
     * reused.  It is always safe to return true.
     * @return <code>true</code> if successive renderings with the
     *         same arguments might produce different results;
     *         <code>false</code> otherwise.
     */
    public boolean isDynamic() throws RemoteImagingException {
	try {
	    return imageServer.isDynamic(id);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("JAIRMICRIF9");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}
        return true;
    }

    /**
     * Gets the width in user coordinate space.  By convention, the
     * usual width of a RenderableImage is equal to the image's aspect
     * ratio (width divided by height).
     *
     * @return the width of the image in user coordinates.
     */
    public float getWidth() throws RemoteImagingException {
	try {
	    return imageServer.getRenderableWidth(id);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RenderableRMIServerProxy0");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}
        return 0;
    }

    /**
     * Gets the height in user coordinate space.  By convention, the
     * usual height of a RenderedImage is equal to 1.0F.
     *
     * @return the height of the image in user coordinates.
     */
    public float getHeight() throws RemoteImagingException {
	try {
	    return imageServer.getRenderableHeight(id);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RenderableRMIServerProxy0");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}
        return 0;
    }

    /**
     * Gets the minimum X coordinate of the rendering-independent image data.
     * @return the minimum X coordinate of the rendering-independent image
     * data.
     */
    public float getMinX() throws RemoteImagingException {
	try {
	    return imageServer.getRenderableMinX(id);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RenderableRMIServerProxy1");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}
        return 0;
    }

    /**
     * Gets the minimum Y coordinate of the rendering-independent image data.
     * @return the minimum Y coordinate of the rendering-independent image
     * data.
     */
    public float getMinY() throws RemoteImagingException {
	try {
	    return imageServer.getRenderableMinY(id);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RenderableRMIServerProxy1");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}
        return 0;
    }

    /**
     * Returns the ID that refers to the <code>RenderableOp</code> on the
     * server.
     */
    public Long getRMIID() {
	return id;
    }

    /**
     * Returns the name of the server on which the RenderableOp exists.
     */
    public String getServerName() {
	return serverName;
    }

    /**
     * Returns the operation name.
     */
    public String getOperationName() {
	return operationName;
    }

    /**
     * Creates a RenderedImage instance of this image with width w, and
     * height h in pixels.  The RenderContext is built automatically
     * with an appropriate usr2dev transform and an area of interest
     * of the full image.  All the rendering hints come from hints
     * passed in.
     *
     * <p> If w == 0, it will be taken to equal
     * Math.round(h*(getWidth()/getHeight())).
     * Similarly, if h == 0, it will be taken to equal
     * Math.round(w*(getHeight()/getWidth())).  One of
     * w or h must be non-zero or else an IllegalArgumentException
     * will be thrown.
     *
     * <p> The created RenderedImage may have a property identified
     * by the String HINTS_OBSERVED to indicate which RenderingHints
     * were used to create the image.  In addition any RenderedImages
     * that are obtained via the getSources() method on the created
     * RenderedImage may have such a property.
     *
     * @param w the width of rendered image in pixels, or 0.
     * @param h the height of rendered image in pixels, or 0.
     * @param hints a RenderingHints object containg hints.
     * @return a RenderedImage containing the rendered data.
     */
    public RenderedImage createScaledRendering(int w,
					       int h,
					       RenderingHints hints)
	throws RemoteImagingException {

	SerializableState ss = SerializerFactory.getState(hints, null);

	try {
	    return imageServer.createScaledRendering(id, w, h, ss);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RMIServerProxy10");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}
        return null;
    }

    /**
     * Returns a RenderedImage instance of this image with a default
     * width and height in pixels.  The RenderContext is built
     * automatically with an appropriate usr2dev transform and an area
     * of interest of the full image.  The rendering hints are
     * empty.  createDefaultRendering may make use of a stored
     * rendering for speed.
     *
     * @return a RenderedImage containing the rendered data.
     */
    public RenderedImage createDefaultRendering()
	throws RemoteImagingException {
	try {
	    return imageServer.createDefaultRendering(id);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RMIServerProxy10");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}
        return null;
    }

    /**
     * Creates a RenderedImage that represented a rendering of this image
     * using a given RenderContext.  This is the most general way to obtain a
     * rendering of a RenderableImage.
     *
     * <p> The created RenderedImage may have a property identified
     * by the String HINTS_OBSERVED to indicate which RenderingHints
     * (from the RenderContext) were used to create the image.
     * In addition any RenderedImages
     * that are obtained via the getSources() method on the created
     * RenderedImage may have such a property.
     *
     * @param renderContext the RenderContext to use to produce the rendering.
     * @return a RenderedImage containing the rendered data.
     */
    public RenderedImage createRendering(RenderContext renderContext)
	throws RemoteImagingException {

	SerializableState ss = SerializerFactory.getState(renderContext, null);
	try {
	    return imageServer.createRendering(id, ss);
	} catch (RemoteException re) {
            String message = JaiI18N.getString("RMIServerProxy10");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, re),
                                   this, false);
//	    throw new RemoteImagingException(ImageUtil.getStackTraceString(re));
	}
        return null;
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

	if (imageServer == null) {

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
	    imageServer = null;
	    try {
		imageServer = (ImageServer)Naming.lookup(serviceName);
	    } catch(Exception e) {
                String message = JaiI18N.getString("RMIServerProxy12");
                listener.errorOccurred(message,
                                       new RemoteImagingException(message, e),
                                       this, false);
//		throw new RemoteImagingException(ImageUtil.getStackTraceString(e));
	    }
	}

	return imageServer;
    }
}
