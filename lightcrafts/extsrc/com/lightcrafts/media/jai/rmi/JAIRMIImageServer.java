/*
 * $RCSfile: JAIRMIImageServer.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:51 $
 * $State: Exp $
 */package com.lightcrafts.media.jai.rmi;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ContextualRenderedImageFactory;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderContext;
import java.awt.image.renderable.RenderableImage;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;
import java.util.List;
import java.util.Iterator;
import java.util.Vector;
import com.lightcrafts.mediax.jai.CollectionImage;
import com.lightcrafts.mediax.jai.JAI;
import com.lightcrafts.mediax.jai.OperationDescriptor;
import com.lightcrafts.mediax.jai.OperationRegistry;
import com.lightcrafts.mediax.jai.OpImage;
import com.lightcrafts.mediax.jai.ParameterListDescriptor;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.PropertySource;
import com.lightcrafts.mediax.jai.RenderingChangeEvent;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.registry.CRIFRegistry;
import com.lightcrafts.mediax.jai.remote.JAIRMIDescriptor;
import com.lightcrafts.mediax.jai.remote.RemoteImagingException;
import com.lightcrafts.mediax.jai.remote.NegotiableCapability;
import com.lightcrafts.mediax.jai.remote.NegotiableCapabilitySet;
import com.lightcrafts.mediax.jai.remote.SerializableRenderedImage;
import com.lightcrafts.mediax.jai.remote.SerializableState;
import com.lightcrafts.mediax.jai.remote.SerializerFactory;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecDescriptor;
import com.lightcrafts.mediax.jai.tilecodec.TileCodecParameterList;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoder;
import com.lightcrafts.mediax.jai.tilecodec.TileEncoderFactory;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.ImageUtil;
import com.lightcrafts.media.jai.util.Service;
import com.lightcrafts.media.jai.remote.JAIServerConfigurationSpi;

/**
 * The server-side implementation of the ImageServer interface.  A
 * JAIRMIImageServer has a RenderedImage source, acquired via one of three
 * setSource() methods.  The first takes a RenderedImage directly as
 * its parameter; this image is simply copied over the network using
 * the normal RMI mechanisms.  Note that not every image can be
 * transferred in this way -- for example, attempting to pass an
 * OpImage that uses native code or that depends on the availability
 * of a class not resident on the server as a parameter will cause an
 * exception to be thrown.
 *
 * <p> The second and third ways of setting sources make use of the
 * RenderedOp and RenderableOp classes to send a high-level
 * description of an image chain based on operation names.  This
 * chain will be copied over to the server using RMI, where it will be
 * expanded into an OpImage chain using the server's registry.  This
 * is the preferred method since it requires less data transfer and
 * offers a better chance of success.  It may still fail if the
 * sources or parameters of any operation in the chain are not
 * serializable.
 *
 * <p> RMI requires all remote methods to declare `throws
 * RemoteException' in their signatures.  It is up to the client to
 * deal with errors.  A simple implementation of error handling may be
 * found in the RemoteRenderedImage class.
 *
 * <p> This class contains a main() method that should be run on the
 * server after starting the RMI registry.  The registry will then
 * construct new instances of JAIRMIImageServer on demand.
 *
 * @see ImageServer
 * @see RenderedOp
 *
 * @since 1.1
 */
public class JAIRMIImageServer extends UnicastRemoteObject
    implements ImageServer {

    private boolean DEBUG = true;

    /** Tag to represent a null property. */
    public static final Object NULL_PROPERTY = RMIImageImpl.NULL_PROPERTY;

    /** Identifier counter for the remote images. */
    private static long idCounter = 0;

    /**
     * The RenderedImage nodes hashed by an ID string which must be unique
     * across all possible clients of this object.
     */
    private static Hashtable nodes = new Hashtable();

    /**
     * Hashtable to store the negotiated values for each id.
     */
    private static Hashtable negotiated = new Hashtable();

    /**
     * Hashtable to store the number of references existing to a
     * particular id on this server.
     */
    private static Hashtable refCount = new Hashtable();

    /**
     * Retrieve a PlanarImage source from the Hashtable of sources.
     *
     * @param id The unique ID of the source.
     * @return The source.
     */
    private static PlanarImage getSource(Long id) throws RemoteException {
	Object obj = null;
	if(nodes == null ||
	   (obj = nodes.get(id)) == null) {
	    throw new RemoteException(JaiI18N.getString("RMIImageImpl2"));
	}
	return (PlanarImage)obj;
    }

    /**
     * Retrieve a PropertySource from the Hashtable of PropertySources.
     *
     * @param id The unique ID of the source.
     * @return The PropertySource.
     */
    private static PropertySource getPropertySource(Long id)
        throws RemoteException {

	Object obj = nodes.get(id);
	return (PropertySource)obj;
    }

    /**
     * Constructs a JAIRMIImageServer with a source to be specified
     * later.
     */
    public JAIRMIImageServer(int serverport) throws RemoteException {
        super(serverport);
    }

    /**
     * Returns the identifier of the remote image. This method should be
     * called to return an identifier before any other methods are invoked.
     * The same ID must be used in all subsequent references to the remote
     * image.
     */
    public synchronized Long getRemoteID() throws RemoteException {
        return new Long(++idCounter);
    }

    /**
     * Disposes of any resouces allocated to the client object with
     * the specified ID.
     */
    public synchronized void dispose(Long id)  throws RemoteException {

	int count = ((Integer)refCount.get(id)).intValue();

	if (count == 1) {

	    // If this was the last reference, remove all Objects
	    // associated with this id in various Hashtables.
	    if (nodes != null) {
		nodes.remove(id);
		negotiated.remove(id);
	    }

	    refCount.remove(id);

	} else {

	    // Decrement count of references to this id.
	    count--;
	    if (count == 0) {
		refCount.remove(id);
	    }
	    refCount.put(id, new Integer(count));
	}
    }

    /**
     * Increments the reference count for this id, i.e. increments the
     * number of RMIServerProxy objects that currently reference this id.
     */
    public void incrementRefCount(Long id) throws RemoteException {
	Integer iCount = (Integer)refCount.get(id);
	int count = 0;
	if (iCount != null) {
	    count = iCount.intValue();
	}
	count++;
	refCount.put(id, new Integer(count));
    }

    /** Gets a property from the property set of this image.  If the
     *	property is undefined the constant NULL_PROPERTY is returned.
     */
    public Object getProperty(Long id, String name) throws RemoteException {

	PropertySource ps = getPropertySource(id);
	Object property = ps.getProperty(name);

	if (property == null ||
	    property.equals(java.awt.Image.UndefinedProperty)) {
	    property = NULL_PROPERTY;
	}

	return property;
    }

    /**
     * Returns a list of names recognized by getProperty().
     *
     * @return an array of Strings representing property names.
     */
    public String[] getPropertyNames(Long id) throws RemoteException {

	PropertySource ps = getPropertySource(id);
	return ps.getPropertyNames();

    }

    /**
     * Returns a list of names recognized by getProperty().
     *
     * @return an array of Strings representing property names.
     */
    public String[] getPropertyNames(String opName) throws RemoteException {

	return (CRIFRegistry.get(null, opName)).getPropertyNames();
    }

    /** Returns the minimum X coordinate of the ImageServer. */
    public int getMinX(Long id) throws RemoteException {
	return getSource(id).getMinX();
    }

    /** Returns the smallest X coordinate to the right of the ImageServer. */
    public int getMaxX(Long id) throws RemoteException {

	return getSource(id).getMaxX();
    }

    /** Returns the minimum Y coordinate of the ImageServer. */
    public int getMinY(Long id) throws RemoteException {

	return getSource(id).getMinY();
    }

    /** Returns the smallest Y coordinate below the ImageServer. */
    public int getMaxY(Long id) throws RemoteException {

	return getSource(id).getMaxY();
    }

    /** Returns the width of the ImageServer. */
    public int getWidth(Long id) throws RemoteException {

	return getSource(id).getWidth();
    }

    /** Returns the height of the ImageServer. */
    public int getHeight(Long id) throws RemoteException {

	return getSource(id).getHeight();
    }

    /** Returns the width of a tile in pixels. */
    public int getTileWidth(Long id) throws RemoteException {

	return getSource(id).getTileWidth();
    }

    /** Returns the height of a tile in pixels. */
    public int getTileHeight(Long id) throws RemoteException {

	return getSource(id).getTileHeight();
    }

    /**
     * Returns the X coordinate of the upper-left pixel of tile (0, 0).
     */
    public int getTileGridXOffset(Long id) throws RemoteException {

	return getSource(id).getTileGridXOffset();
    }

    /**
     * Returns the Y coordinate of the upper-left pixel of tile (0, 0).
     */
    public int getTileGridYOffset(Long id) throws RemoteException {

        return getSource(id).getTileGridYOffset();
    }

    /** Returns the index of the leftmost column of tiles. */
    public int getMinTileX(Long id) throws RemoteException {

	return getSource(id).getMinTileX();
    }

    /**
     * Returns the number of tiles along the tile grid in the horizontal
     * direction.
     */
    public int getNumXTiles(Long id) throws RemoteException {

	return getSource(id).getNumXTiles();
    }

    /** Returns the index of the uppermost row of tiles. */
    public int getMinTileY(Long id) throws RemoteException {

	return getSource(id).getMinTileY();
    }

    /**
     * Returns the number of tiles along the tile grid in the vertical
     * direction.
     */
    public int getNumYTiles(Long id) throws RemoteException {

	return getSource(id).getNumYTiles();
    }

    /** Returns the index of the rightmost column of tiles. */
    public int getMaxTileX(Long id) throws RemoteException {

	return getSource(id).getMaxTileX();
    }

    /** Returns the index of the bottom row of tiles. */
    public int getMaxTileY(Long id) throws RemoteException {

	return getSource(id).getMaxTileY();
    }

    /** Returns the SampleModel associated with this image. */
    public SerializableState getSampleModel(Long id) throws RemoteException {
        return SerializerFactory.getState(getSource(id).getSampleModel(),
					  null);
    }

    /** Returns the ColorModel associated with this image. */
    public SerializableState getColorModel(Long id) throws RemoteException {
        return SerializerFactory.getState(getSource(id).getColorModel(), null);
    }

    /** Returns a Rectangle indicating the image bounds. */
    public Rectangle getBounds(Long id) throws RemoteException {

	return getSource(id).getBounds();
    }

    /**
     * Returns tile (x, y).  Note that x and y are indices into the
     * tile array, not pixel locations.  Unlike in the true RenderedImage
     * interface, the Raster that is returned should be considered a copy.
     *
     * @param id An ID for the source which must be unique across all clients.
     * @param tileX the X index of the requested tile in the tile array.
     * @param tileY the Y index of the requested tile in the tile array.
     * @return the tile as a Raster.
     */
    public SerializableState getTile(Long id, int tileX, int tileY)
        throws RemoteException {

	Raster r = getSource(id).getTile(tileX, tileY);
	return SerializerFactory.getState(r, null);
    }

    /**
     * Compresses tile (x, y) and returns the compressed tile's contents
     * as a byte array.  Note that x and y are indices into the
     * tile array, not pixel locations.  Unlike in the true RenderedImage
     * interface, the Raster that is returned should be considered a copy.
     *
     * @param id An ID for the source which must be unique across all clients.
     * @param x the x index of the requested tile in the tile array
     * @param y the y index of the requested tile in the tile array
     * @return a byte array containing the compressed tile contents.
     */
    public byte[] getCompressedTile(Long id, int x, int y)
	throws RemoteException {

	TileCodecParameterList tcpl = null;
	TileEncoderFactory tef = null;
	NegotiableCapability codecCap = null;

	if (negotiated != null) {
	    codecCap = ((NegotiableCapabilitySet)negotiated.get(id)).
		getNegotiatedValue("tileCodec");
	}

	if (codecCap != null) {

	    String category = codecCap.getCategory();
	    String capabilityName = codecCap.getCapabilityName();
	    List generators = codecCap.getGenerators();

	    Class factory;
	    for (Iterator i=generators.iterator(); i.hasNext(); ) {

		factory = (Class)i.next();
		if (tef == null &&
		    TileEncoderFactory.class.isAssignableFrom(factory)) {
		    try {
			tef = (TileEncoderFactory)factory.newInstance();
		    } catch (InstantiationException ie) {
			throw new RuntimeException(ie.getMessage());
		    } catch (IllegalAccessException iae) {
			throw new RuntimeException(iae.getMessage());
		    }
		}
	    }

	    if (tef == null) {
		throw new RuntimeException(
				     JaiI18N.getString("JAIRMIImageServer0"));
	    }

	    TileCodecDescriptor tcd =
		(TileCodecDescriptor)JAI.getDefaultInstance().
		getOperationRegistry().getDescriptor("tileEncoder",
						     capabilityName);

	    if (tcd.includesSampleModelInfo() == false ||
		tcd.includesLocationInfo() == false) {
		throw new RuntimeException(
				     JaiI18N.getString("JAIRMIImageServer1"));
	    }

	    ParameterListDescriptor pld =
		tcd.getParameterListDescriptor("tileEncoder");

	    tcpl = new TileCodecParameterList(capabilityName,
					      new String[] {"tileEncoder"},
					      pld);

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
			    continue;
			}
			tcpl.setParameter(currParam, currValue);
		    }
		}
	    }

	    Raster r = getSource(id).getTile(x, y);
	    ByteArrayOutputStream stream = new ByteArrayOutputStream();
	    TileEncoder encoder = tef.createEncoder(stream, tcpl,
						    r.getSampleModel());

	    try {
		encoder.encode(r);
	    } catch (java.io.IOException ioe) {
		throw new RuntimeException(ioe.getMessage());
	    }

	    return stream.toByteArray();
	} else {
	    throw new RuntimeException(
				     JaiI18N.getString("JAIRMIImageServer2"));
	}
    }

    /**
     * Returns the entire image as a single Raster.
     *
     * @return a Raster containing a copy of this image's data.
     */
    public SerializableState getData(Long id) throws RemoteException {
        return SerializerFactory.getState(getSource(id).getData(), null);
    }

    /**
     * Returns an arbitrary rectangular region of the RenderedImage
     * in a Raster.  The rectangle of interest will be clipped against
     * the image bounds.
     *
     * @param id An ID for the source which must be unique across all clients.
     * @param rect the region of the RenderedImage to be returned.
     * @return a SerializableState containing a copy of the desired data.
     */
    public SerializableState getData(Long id, Rectangle bounds)
        throws RemoteException {
        if (bounds == null) {
            return getData(id);
        } else {
            bounds = bounds.intersection(getBounds(id));
            return SerializerFactory.getState(getSource(id).getData(bounds),
					      null);
        }
    }

    /**
     * Returns the same result as getData(Rectangle) would for the
     * same rectangular region.
     */
    public SerializableState copyData(Long id, Rectangle bounds)
        throws RemoteException {
        return getData(id, bounds);
    }




    /**
     * Creates a RenderedOp on the server side with a parameter block
     * empty of sources. The sources are set by separate calls depending
     * upon the type and serializabilty of the source.
     */
    public void createRenderedOp(Long id, String opName,
				 ParameterBlock pb,
				 SerializableState hints)
	throws RemoteException {

	RenderingHints rh = (RenderingHints)hints.getObject();

	// Check whether any of the parameters are Strings which represent
	// images either on this server or another server.
	JAIRMIUtil.checkServerParameters(pb, nodes);

	RenderedOp node = new RenderedOp(opName, pb, rh);

	// Remove all sinks so that no events are sent automatically
	// to the sinks
	node.removeSinks();

	nodes.put(id, node);
    }

    /**
     * Calls for Rendering of the Op and returns true if the RenderedOp
     * could be rendered else false
     */
    public boolean getRendering(Long id) throws RemoteException {

	RenderedOp op = getNode(id);
	if (op.getRendering() == null) {
	    return false;
	} else {
	    return true;
	}
    }

    /**
     * Retrieve a node from the hashtable
     *
     */
    public RenderedOp getNode(Long id) throws RemoteException {
	return (RenderedOp)nodes.get(id);
    }

    /**
     *  Sets the source of the image as a RenderedImage on the server side
     */
    public synchronized void setRenderedSource(Long id,
					       RenderedImage source,
					       int index)
	throws RemoteException {

	PlanarImage pi = PlanarImage.wrapRenderedImage(source);

	Object obj = nodes.get(id);

	if (obj instanceof RenderedOp) {
	    RenderedOp op = (RenderedOp)obj;
	    op.setSource(pi, index);
	    ((PlanarImage)op.getSourceObject(index)).removeSinks();
	} else if (obj instanceof RenderableOp) {
	    ((RenderableOp)obj).setSource(pi, index);
	}
    }

    /**
     *  Sets the source of the image as a RenderedOp on the server side
     */
    public synchronized void setRenderedSource(Long id,
					       RenderedOp source,
					       int index)
	throws RemoteException {

	Object obj = nodes.get(id);
	if (obj instanceof RenderedOp) {
	    RenderedOp op = (RenderedOp)obj;
	    op.setSource(source.getRendering(), index);
	    ((PlanarImage)op.getSourceObject(index)).removeSinks();
	} else if (obj instanceof RenderableOp) {
	    ((RenderableOp)obj).setSource(source.getRendering(), index);
	}
    }

    /**
     * Sets the source of the image which is on the same
     * server
     */
    public synchronized void setRenderedSource(Long id,
					       Long sourceId,
					       int index)
	throws RemoteException {

	Object obj = nodes.get(id);
	if (obj instanceof RenderedOp) {
	    RenderedOp op = (RenderedOp)obj;
	    op.setSource(nodes.get(sourceId), index);
	    ((PlanarImage)nodes.get(sourceId)).removeSinks();
	} else if (obj instanceof RenderableOp) {
	    ((RenderableOp)obj).setSource(nodes.get(sourceId), index);
	}
    }

    /**
     * Sets the source of the image which is on a different
     * server
     */
    public synchronized void setRenderedSource(Long id,
					       Long sourceId,
					       String serverName,
					       String opName,
					       int index)
	throws RemoteException {

	Object obj = nodes.get(id);

	if (obj instanceof RenderedOp) {
	    RenderedOp node = (RenderedOp)obj;
	    node.setSource(new RMIServerProxy((serverName+"::"+sourceId),
					      opName,
					      null),
			   index);
	    ((PlanarImage)node.getSourceObject(index)).removeSinks();
	} else if (obj instanceof RenderableOp) {
	    ((RenderableOp)obj).setSource(new RMIServerProxy((serverName +
							      "::" + sourceId),
							     opName,
							     null),
					  index);
	}
    }

    /// Renderable Mode Methods

    /**
     * Gets the minimum X coordinate of the rendering-independent image
     * stored against the given ID.
     *
     * @return the minimum X coordinate of the rendering-independent image
     * data.
     */
    public float getRenderableMinX(Long id) throws RemoteException {

	RenderableImage ri = (RenderableImage)nodes.get(id);
	return ri.getMinX();
    }

    /**
     * Gets the minimum Y coordinate of the rendering-independent image
     * stored against the given ID.
     *
     * @return the minimum X coordinate of the rendering-independent image
     * data.
     */
    public float getRenderableMinY(Long id) throws RemoteException {
	RenderableImage ri = (RenderableImage)nodes.get(id);
	return ri.getMinY();
    }

    /**
     * Gets the width (in user coordinate space) of the
     * <code>RenderableImage</code> stored against the given ID.
     *
     * @return the width of the renderable image in user coordinates.
     */
    public float getRenderableWidth(Long id) throws RemoteException {
	RenderableImage ri = (RenderableImage)nodes.get(id);
	return ri.getWidth();
    }

    /**
     * Gets the height (in user coordinate space) of the
     * <code>RenderableImage</code> stored against the given ID.
     *
     * @return the height of the renderable image in user coordinates.
     */
    public float getRenderableHeight(Long id) throws RemoteException {
	RenderableImage ri = (RenderableImage)nodes.get(id);
	return ri.getHeight();
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
    public RenderedImage createScaledRendering(Long id,
					       int w,
					       int h,
					       SerializableState hintsState)
	throws RemoteException {

	RenderableImage ri = (RenderableImage)nodes.get(id);
	RenderingHints hints = (RenderingHints)hintsState.getObject();
	RenderedImage rendering = ri.createScaledRendering(w, h, hints);
	if (rendering instanceof Serializable) {
	    return rendering;
	} else {
	    return new SerializableRenderedImage(rendering);
	}
    }

    /**
     * Returnd a RenderedImage instance of this image with a default
     * width and height in pixels.  The RenderContext is built
     * automatically with an appropriate usr2dev transform and an area
     * of interest of the full image.  The rendering hints are
     * empty.  createDefaultRendering may make use of a stored
     * rendering for speed.
     *
     * @return a RenderedImage containing the rendered data.
     */
    public RenderedImage createDefaultRendering(Long id)
	throws RemoteException {

	RenderableImage ri = (RenderableImage)nodes.get(id);
	RenderedImage rendering = ri.createDefaultRendering();
	if (rendering instanceof Serializable) {
	    return rendering;
	} else {
	    return new SerializableRenderedImage(rendering);
	}
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
    public RenderedImage createRendering(Long id,
					 SerializableState renderContextState)
	throws RemoteException {

	RenderableImage ri = (RenderableImage)nodes.get(id);
	RenderContext renderContext =
	    (RenderContext)renderContextState.getObject();
	RenderedImage rendering = ri.createRendering(renderContext);
	if (rendering instanceof Serializable) {
	    return rendering;
	} else {
	    return new SerializableRenderedImage(rendering);
	}
    }

    /**
     * Creates a RenderableOp on the server side with a parameter block
     * empty of sources. The sources are set by separate calls depending
     * upon the type and serializabilty of the source.
     */
    public synchronized void createRenderableOp(Long id,
						String opName,
						ParameterBlock pb)
	throws RemoteException {

	// XXX Since RMIServerProxy does not do a checkClientParameters, this
	// side obviously does not do the corresponding
	// checkServerParameters. Look at RMIServerProxy's renderable
	// constructor for reasoning. aastha, 09/26/01

	RenderableOp node = new RenderableOp(opName, pb);
	nodes.put(id, node);
    }

    /**
     * Calls for rendering of a RenderableOp with the given SerializableState
     */
    public synchronized Long getRendering(Long id, SerializableState rcs)
	throws RemoteException {

	RenderableOp op = (RenderableOp)nodes.get(id);
	PlanarImage pi = PlanarImage.wrapRenderedImage(op.createRendering(
					     (RenderContext)rcs.getObject()));

	Long renderingID = getRemoteID();
	nodes.put(renderingID, pi);

	// Put the op's negotiated result values for its rendering too.
	setServerNegotiatedValues(renderingID, (NegotiableCapabilitySet)
				  negotiated.get(id));
	return renderingID;
    }

    /**
     * Sets the source of the image which is on the same
     * server
     */
    public synchronized void setRenderableSource(Long id,
						 Long sourceId,
						 int index)
	throws RemoteException {

	RenderableOp node = (RenderableOp)nodes.get(id);
	Object obj = nodes.get(sourceId);
	if (obj instanceof RenderableOp){
	    node.setSource((RenderableOp)obj, index);
	} else if (obj instanceof RenderedImage) {
	    node.setSource(PlanarImage.wrapRenderedImage((RenderedImage)obj),
			   index);
	}
    }

    /**
     * Sets the source of the image which is on a different
     * server
     */
    public synchronized void setRenderableSource(Long id,
						 Long sourceId,
						 String serverName,
						 String opName,
						 int index)
	throws RemoteException {

	RenderableOp node = (RenderableOp)nodes.get(id);
	node.setSource(new RMIServerProxy((serverName+"::"+sourceId),
					  opName,
					  null),
		       index);

    }

    /**
     * Sets the source of the image which is on a different
     * server
     */
    public synchronized void setRenderableRMIServerProxyAsSource(
							    Long id,
							    Long sourceId,
							    String serverName,
							    String opName,
							    int index)
	throws RemoteException {

	RenderableOp node = (RenderableOp)nodes.get(id);
	node.setSource(new RenderableRMIServerProxy(serverName, opName, null,
						    sourceId), index);
    }

    /**
     * when source is set to a  RenderableOp and isnt supposed to be
     * rendered yet. like at the time of getBounds2D
     *
     * Sets the source of the image as a RenderableOp on the server side
     *
     */
    public synchronized void setRenderableSource(Long id, RenderableOp source,
						 int index)
	throws RemoteException {
	RenderableOp op = (RenderableOp)nodes.get(id);
	op.setSource(source, index);
    }

    /**
     * Sets the source of the image as a RenderableImage on the server side
     */
    public synchronized void setRenderableSource(Long id,
						 SerializableRenderableImage s,
						 int index)
	throws RemoteException {
	RenderableOp op = (RenderableOp)nodes.get(id);
	op.setSource(s, index);
    }

    /**
     *  Sets the source of the image as a RenderedImage on the server side
     */
    public synchronized void setRenderableSource(Long id,
						 RenderedImage source,
						 int index)
	throws RemoteException {

	PlanarImage pi = PlanarImage.wrapRenderedImage(source);
	RenderableOp op = (RenderableOp)nodes.get(id);
	op.setSource(pi, index);
    }

    /**
     * Maps the RenderContext for the remote Image
     */
    public SerializableState mapRenderContext(int id, Long nodeId,
					      String operationName,
					      SerializableState rcs)
	throws RemoteException {

	// Retrieve the RenderableOp for the rendering of which
	// the mapRenderContext call is being made.
	RenderableOp rop = (RenderableOp)nodes.get(nodeId);

	//Find the CRIF for the respective operation
	ContextualRenderedImageFactory crif =
	    CRIFRegistry.get(rop.getRegistry(), operationName);

	if (crif == null) {
	    throw new RuntimeException(
				    JaiI18N.getString("JAIRMIImageServer3"));
	}

	RenderContext rc =
	    crif.mapRenderContext(id,
				  (RenderContext)rcs.getObject(),
				  (ParameterBlock)rop.getParameterBlock(),
				  rop);
	return SerializerFactory.getState(rc, null);
    }

    /**
     * Gets the Bounds2D of the specified Remote Image
     */
    public SerializableState getBounds2D(Long nodeId, String operationName)
	throws RemoteException {

	// Retrieve the RenderableOp for whose RIF
	// the mapRenderContext call is being made.
	RenderableOp rop = (RenderableOp)nodes.get(nodeId);

	//Find the CRIF for the respective operation
	ContextualRenderedImageFactory crif =
	    CRIFRegistry.get(rop.getRegistry(), operationName);

	if (crif == null) {
	    throw new RuntimeException(
				    JaiI18N.getString("JAIRMIImageServer3"));
	}

	Rectangle2D r2D =
	    crif.getBounds2D((ParameterBlock)rop.getParameterBlock());

	return SerializerFactory.getState(r2D, null);
    }

    /**
     * Returns <code>true</code> if successive renderings with the same
     * arguments may produce different results for this opName
     *
     * @return <code>false</code> indicating that the rendering is static.
     */
    public boolean isDynamic(String opName) throws RemoteException {

	return (CRIFRegistry.get(null, opName)).isDynamic();
    }

    /**
     * Returns <code>true</code> if successive renderings with the same
     * arguments may produce different results for the node represented
     * by the given id.
     */
    public boolean isDynamic(Long id) throws RemoteException {

	RenderableImage node = (RenderableImage)nodes.get(id);
	return node.isDynamic();
    }

    /**
     * Gets the operation names supported on the Server
     */
    public String[] getServerSupportedOperationNames()
	throws RemoteException {
	return JAI.getDefaultInstance().getOperationRegistry().
	    getDescriptorNames(OperationDescriptor.class);
    }

    /**
     * Gets the <code>OperationDescriptor</code>s of the operations
     * supported on this server.
     */
    public List getOperationDescriptors() throws RemoteException {
	return JAI.getDefaultInstance().getOperationRegistry().
	    getDescriptors(OperationDescriptor.class);
    }

    /**
     * Calculates the region over which two distinct renderings
     * of an operation may be expected to differ.
     *
     * <p> The class of the returned object will vary as a function of
     * the nature of the operation.  For rendered and renderable two-
     * dimensional images this should be an instance of a class which
     * implements <code>java.awt.Shape</code>.
     *
     * @return The region over which the data of two renderings of this
     *         operation may be expected to be invalid or <code>null</code>
     *         if there is no common region of validity.
     */
    public synchronized SerializableState getInvalidRegion(
						 Long id,
						 ParameterBlock oldParamBlock,
						 SerializableState oldRHints,
						 ParameterBlock newParamBlock,
						 SerializableState newRHints)
	throws RemoteException {

	RenderingHints oldHints = (RenderingHints)oldRHints.getObject();
	RenderingHints newHints = (RenderingHints)newRHints.getObject();

	RenderedOp op = (RenderedOp)nodes.get(id);

	OperationDescriptor od = (OperationDescriptor)
	    JAI.getDefaultInstance().getOperationRegistry().
	    getDescriptor("rendered", op.getOperationName());

	boolean samePBs = false;
	if (oldParamBlock == newParamBlock)
	    samePBs = true;

	Vector oldSources = oldParamBlock.getSources();
	oldParamBlock.removeSources();
	Vector oldReplacedSources =
	    JAIRMIUtil.replaceIdWithSources(oldSources,
					    nodes,
					    op.getOperationName(),
					    op.getRenderingHints());
	oldParamBlock.setSources(oldReplacedSources);

	if (samePBs) {
	    newParamBlock = oldParamBlock;
	} else {
	    Vector newSources = newParamBlock.getSources();
	    newParamBlock.removeSources();
	    Vector newReplacedSources =
		JAIRMIUtil.replaceIdWithSources(newSources,
						nodes,
						op.getOperationName(),
						op.getRenderingHints());

	    newParamBlock.setSources(newReplacedSources);
	}

	Object invalidRegion = od.getInvalidRegion("rendered",
						   oldParamBlock,
						   oldHints,
						   newParamBlock,
						   newHints,
						   op);

	SerializableState shapeState =
	    SerializerFactory.getState((Shape)invalidRegion, null);

	return shapeState;
    }

    /**
     * Returns a conservative estimate of the destination region that
     * can potentially be affected by the pixels of a rectangle of a
     * given source.
     *
     * @param id          A <code>Long</code> identifying the node for whom
     *                    the destination region needs to be calculated .
     * @param sourceRect  The <code>Rectangle</code> in source coordinates.
     * @param sourceIndex The index of the source image.
     *
     * @return A <code>Rectangle</code> indicating the potentially
     *         affected destination region, or <code>null</code> if
     *         the region is unknown.
     */
    public Rectangle mapSourceRect(Long id,
				   Rectangle sourceRect,
				   int sourceIndex) throws RemoteException {

	RenderedOp op = (RenderedOp)nodes.get(id);
	OpImage rendering = (OpImage)(op.getRendering());
	return rendering.mapSourceRect(sourceRect, sourceIndex);
    }

    /**
     * Returns a conservative estimate of the region of a specified
     * source that is required in order to compute the pixels of a
     * given destination rectangle.
     *
     * @param id         A <code>Long</code> identifying the node for whom
     *                   the source region needs to be calculated .
     * @param destRect   The <code>Rectangle</code> in destination coordinates.
     * @param sourceIndex The index of the source image.
     *
     * @return A <code>Rectangle</code> indicating the required source region.
     */
    public Rectangle mapDestRect(Long id, Rectangle destRect, int sourceIndex)
	throws RemoteException {

	RenderedOp op = (RenderedOp)nodes.get(id);
	OpImage rendering = (OpImage)(op.getRendering());
	return rendering.mapDestRect(destRect, sourceIndex);
    }

    /**
     * A method that handles the given event.
     */
    public synchronized Long handleEvent(Long renderedOpID, String propName,
					 Object oldValue, Object newValue)
	throws RemoteException {

	RenderedOp op = (RenderedOp)nodes.get(renderedOpID);
	PlanarImage rendering = op.getRendering();

	// Get a new unique ID
	Long id = getRemoteID();
	// Cache the old rendering against the new id
	nodes.put(id, rendering);

	// Put the op's negotiated result values for its rendering too.
	setServerNegotiatedValues(id, (NegotiableCapabilitySet)
				  negotiated.get(renderedOpID));

	// A PropertyChangeEventJAI with name "operationregistry",
	// "protocolname", "protocolandservername" or "servername" should
	// never be received here, since it is handled entirely on the
	// client side, so we don't handle those here.

	if (propName.equals("operationname")) {

	    op.setOperationName((String)newValue);

	} else if (propName.equals("parameterblock")) {

	    ParameterBlock newPB = (ParameterBlock)newValue;
	    Vector newSrcs = newPB.getSources();
	    newPB.removeSources();

	    JAIRMIUtil.checkServerParameters(newPB, nodes);

	    Vector replacedSources =
		JAIRMIUtil.replaceIdWithSources(newSrcs,
						nodes,
						op.getOperationName(),
						op.getRenderingHints());
	    newPB.setSources(replacedSources);

	    op.setParameterBlock(newPB);

	    // Remove the newly created sinks of the srcs in the newPB
	    Vector newSources = newPB.getSources();
            if(newSources != null && newSources.size() > 0) {
                Iterator it = newSources.iterator();
                while(it.hasNext()) {
                    Object src = it.next();
                    if(src instanceof PlanarImage) {
                        ((PlanarImage)src).removeSinks();
                    } else if(src instanceof CollectionImage) {
                        ((CollectionImage)src).removeSinks();
                    }
                }
            }

	} else if (propName.equals("sources")) {

	    Vector replacedSources =
		JAIRMIUtil.replaceIdWithSources((Vector)newValue,
						nodes,
						op.getOperationName(),
						op.getRenderingHints());
	    op.setSources(replacedSources);

	    // Remove the newly created sinks for the replacedSources
	    if(replacedSources != null && replacedSources.size() > 0) {
                Iterator it = replacedSources.iterator();
                while(it.hasNext()) {
                    Object src = it.next();
                    if(src instanceof PlanarImage) {
                        ((PlanarImage)src).removeSinks();
                    } else if(src instanceof CollectionImage) {
                        ((CollectionImage)src).removeSinks();
                    }
                }
            }


	} else if (propName.equals("parameters")) {

	    Vector parameters = (Vector)newValue;
	    JAIRMIUtil.checkServerParameters(parameters, nodes);
	    op.setParameters(parameters);

	} else if (propName.equals("renderinghints")) {

	    SerializableState newState = (SerializableState)newValue;
	    op.setRenderingHints((RenderingHints)newState.getObject());
	}

	return id;
    }

    /**
     * A method that handles a change in one of it's source's rendering,
     * i.e. a change that would be signalled by RenderingChangeEvent.
     */
    public synchronized Long handleEvent(Long renderedOpID,
					 int srcIndex,
					 SerializableState srcInvalidRegion,
					 Object oldRendering)
	throws RemoteException {

	RenderedOp op = (RenderedOp)nodes.get(renderedOpID);
	PlanarImage rendering = op.getRendering();

	// Get a new unique ID
	Long id = getRemoteID();
	// Cache the old rendering against the new id
	nodes.put(id, rendering);

	// Put the op's negotiated result values for its rendering too.
	setServerNegotiatedValues(id, (NegotiableCapabilitySet)
				  negotiated.get(renderedOpID));

	PlanarImage oldSrcRendering = null, newSrcRendering = null;
	String serverNodeDesc = null;
	Object src = null;

	if (oldRendering instanceof String) {

	    serverNodeDesc = (String)oldRendering;
	    int index = serverNodeDesc.indexOf("::");
	    boolean diffServer = index != -1;

	    if (diffServer) {
		// Create an RMIServerProxy to access the node on a
		// different server
		oldSrcRendering = new RMIServerProxy(serverNodeDesc,
						     op.getOperationName(),
						     op.getRenderingHints());
	    } else {

		src = nodes.get(Long.valueOf(serverNodeDesc));

		if (src instanceof RenderedOp) {
		    oldSrcRendering = ((RenderedOp)src).getRendering();
		} else {
		    oldSrcRendering =
			PlanarImage.wrapRenderedImage((RenderedImage)src);
		}
	    }

	} else {
	    oldSrcRendering =
		PlanarImage.wrapRenderedImage((RenderedImage)oldRendering);
	}

	Object srcObj = op.getSource(srcIndex);
	if (srcObj instanceof RenderedOp) {
	    newSrcRendering = ((RenderedOp)srcObj).getRendering();
	} else if (srcObj instanceof RenderedImage) {
	    newSrcRendering =
		PlanarImage.wrapRenderedImage((RenderedImage)srcObj);
	}

	Shape invalidRegion = (Shape)srcInvalidRegion.getObject();

	RenderingChangeEvent rcEvent =
	    new RenderingChangeEvent((RenderedOp)op.getSource(srcIndex),
				     oldSrcRendering,
				     newSrcRendering,
				     invalidRegion);
	op.propertyChange(rcEvent);

	return id;
    }

    /**
     * Returns the server's capabilities. Currently the only capabilities
     * that are reported are those dealing with TileCodecs.
     */
    public synchronized NegotiableCapabilitySet getServerCapabilities() {

	OperationRegistry registry =
	    JAI.getDefaultInstance().getOperationRegistry();

	// Note that only the tileEncoder capabilities are returned from
	// this method since there is no way to distinguish between NC's
	// for the encoder and the decoder.
	String modeName = "tileEncoder";
	String[] descriptorNames = registry.getDescriptorNames(modeName);
	TileEncoderFactory tef = null;

	// Only non-preference NC's can be added.
	NegotiableCapabilitySet capabilities =
	    new NegotiableCapabilitySet(false);

	Iterator it;
	for (int i=0; i<descriptorNames.length; i++) {

	    it = registry.getFactoryIterator(modeName, descriptorNames[i]);
	    for (; it.hasNext(); ) {
		tef = (TileEncoderFactory)it.next();
		capabilities.add(tef.getEncodeCapability());
	    }
	}

	return capabilities;
    }

    /**
     * Informs the server of the negotiated values that are the result of
     * a successful negotiation.
     *
     * @param negotiatedValues    The result of the negotiation.
     */
    public void setServerNegotiatedValues(Long id,
					  NegotiableCapabilitySet
					  negotiatedValues)
	throws RemoteException {
	if (negotiatedValues != null)
	    negotiated.put(id, negotiatedValues);
	else
	    negotiated.remove(id);
    }

    /**
     * Starts a server on a given port.  The RMI registry must be running
     * on the server host.
     *
     * <p> The usage of this class is
     *
     * <pre>
     * java -Djava.rmi.server.codebase=file:$JAI/lib/jai.jar \
     * -Djava.rmi.server.useCodebaseOnly=false \
     * -Djava.security.policy=\
     * file:`pwd`/policy com.lightcrafts.media.jai.rmi.JAIRMIImageServer \
     * [-host hostName] [-port portNumber]
     * </pre>
     *
     * The default host is the local host and the default port is 1099.
     *
     * @param args the port number as a command-line argument.
     */
    public static void main(String [] args) {

        // Set the security manager.
        if(System.getSecurityManager() == null) {
            System.setSecurityManager(new RMISecurityManager());
        }

	// Load all JAIServerConfigurationSpi implementations on the CLASSPATH
	Iterator spiIter = Service.providers(JAIServerConfigurationSpi.class);
	JAI jai = JAI.getDefaultInstance();

	while (spiIter.hasNext()) {

	    JAIServerConfigurationSpi serverSpi =
		(JAIServerConfigurationSpi)spiIter.next();
	    serverSpi.updateServer(jai);
	}

        // Set the host name and port number.
        String host = null;
	int rmiRegistryPort = 1099; // default port is 1099
	int serverport = 0;

	if (args.length != 0) {

	    String value;

	    for (int i=0; i<args.length; i++) {

		if (args[i].equalsIgnoreCase("-help")) {

		    System.out.println("Usage: java -Djava.rmi.server.codebase=file:$JAI/lib/jai.jar \\");
		    System.out.println("-Djava.rmi.server.useCodebaseOnly=false \\");
		    System.out.println("-Djava.security.policy=file:`pwd`/policy \\");
		    System.out.println("com.lightcrafts.media.jai.rmi.JAIRMIImageServer \\");
		    System.out.println("\nwhere options are:");
		    System.out.println("\t-host <string> The server name or server IP address");
		    System.out.println("\t-port <integer> The port that rmiregistry is running on");
		    System.out.println("\t-rmiRegistryPort <integer> Same as -port option");
		    System.out.println("\t-serverPort <integer> The port that the server should listen on, for connections from clients");
		    System.out.println("\t-cacheMemCapacity <long> The memory capacity in bytes.");
		    System.out.println("\t-cacheMemThreshold <float> The memory threshold, which is the fractional amount of cache memory to retain during tile removal");
		    System.out.println("\t-disableDefaultCache Disable use of default tile cache. Tiles are not stored.");
		    System.out.println("\t-schedulerParallelism <integer> The degree of parallelism of the default TileScheduler");
		    System.out.println("\t-schedulerPrefetchParallelism <integer> The degree of parallelism of the default TileScheduler for tile prefetching");
		    System.out.println("\t-schedulerPriority <integer> The priority of tile scheduling for the default TileScheduler");
		    System.out.println("\t-schedulerPrefetchPriority <integer> The priority of tile prefetch scheduling for the default TileScheduler");
		    System.out.println("\t-defaultTileSize <integer>x<integer> The default tile dimensions in the form <xSize>x<ySize>");
		    System.out.println("\t-defaultRenderingSize <integer>x<integer> The default size to render a RenderableImage to, in the form <xSize>x<ySize>");
		    System.out.println("\t-serializeDeepCopy <boolean> Whether a deep copy of the image data should be used when serializing images");
		    System.out.println("\t-tileCodecFormat <string> The default format to be used for tile serialization via TileCodecs");
		    System.out.println("\t-retryInterval <integer> The retry interval value to be used for dealing with network errors during remote imaging");
		    System.out.println("\t-numRetries <integer> The number of retries to be used for dealing with network errors during remote imaging");

		} else if (args[i].equalsIgnoreCase("-host")) {

		    host = args[++i];

		} else if (args[i].equalsIgnoreCase("-port") ||
			   args[i].equalsIgnoreCase("-rmiRegistryPort")) {

		    rmiRegistryPort = Integer.parseInt(args[++i]);

		} else if (args[i].equalsIgnoreCase("-serverport")) {

		    serverport = Integer.parseInt(args[++i]);

		} else if (args[i].equalsIgnoreCase("-cacheMemCapacity")) {

		    jai.getTileCache().setMemoryCapacity(
						  Long.parseLong(args[++i]));

		} else if (args[i].equalsIgnoreCase("-cacheMemThreshold")) {

		    jai.getTileCache().setMemoryThreshold(
						  Float.parseFloat(args[++i]));

		} else if (args[i].equalsIgnoreCase("-disableDefaultCache")) {

		    jai.disableDefaultTileCache();

		} else if (args[i].equalsIgnoreCase("-schedulerParallelism")) {

		    jai.getTileScheduler().setParallelism(
						  Integer.parseInt(args[++i]));

		} else if (args[i].equalsIgnoreCase("-schedulerPrefetchParallelism")) {

		    jai.getTileScheduler().setPrefetchParallelism(
						  Integer.parseInt(args[++i]));

		} else if (args[i].equalsIgnoreCase("-schedulerPriority")) {

		    jai.getTileScheduler().setPriority(
						  Integer.parseInt(args[++i]));

		} else if (args[i].equalsIgnoreCase("-schedulerPrefetchPriority")) {

		    jai.getTileScheduler().setPrefetchPriority(
						  Integer.parseInt(args[++i]));

		} else if (args[i].equalsIgnoreCase("-defaultTileSize")) {

		    value = args[++i].toLowerCase();
		    int xpos = value.indexOf("x");
		    int xSize = Integer.parseInt(value.substring(0, xpos));
		    int ySize = Integer.parseInt(value.substring(xpos+1));

		    jai.setDefaultTileSize(new Dimension(xSize, ySize));

		} else if (args[i].equalsIgnoreCase("-defaultRenderingSize")) {

		    value = args[++i].toLowerCase();
		    int xpos = value.indexOf("x");
		    int xSize = Integer.parseInt(value.substring(0, xpos));
		    int ySize = Integer.parseInt(value.substring(xpos+1));

		    jai.setDefaultRenderingSize(new Dimension(xSize, ySize));

		} else if (args[i].equalsIgnoreCase("-serializeDeepCopy")) {

		    jai.setRenderingHint(JAI.KEY_SERIALIZE_DEEP_COPY,
					 Boolean.valueOf(args[++i]));

		} else if (args[i].equalsIgnoreCase("-tileCodecFormat")) {

		    jai.setRenderingHint(JAI.KEY_TILE_CODEC_FORMAT, args[++i]);

		} else if (args[i].equalsIgnoreCase("-retryInterval")) {

		    jai.setRenderingHint(JAI.KEY_RETRY_INTERVAL,
					 Integer.valueOf(args[++i]));

		} else if (args[i].equalsIgnoreCase("-numRetries")) {

		    jai.setRenderingHint(JAI.KEY_NUM_RETRIES,
					 Integer.valueOf(args[++i]));
		}
	    }
	}

        // Default to the local host if the host was not specified.
        if(host == null) {
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch(java.net.UnknownHostException e) {
                String message = JaiI18N.getString("RMIImageImpl1");
                sendExceptionToListener(message,
                                        new RemoteImagingException(message, e));
/*
                System.err.println(JaiI18N.getString("RMIImageImpl1") +
                                   e.getMessage());
                e.printStackTrace();
*/
            }
        }

        System.out.println(JaiI18N.getString("RMIImageImpl3")+" "+
                           host + ":" + rmiRegistryPort);

        try {
            JAIRMIImageServer im = new JAIRMIImageServer(serverport);
            String serverName =
		new String("rmi://" +
                           host + ":" + rmiRegistryPort + "/" +
                           JAIRMIDescriptor.IMAGE_SERVER_BIND_NAME);
            System.out.println(JaiI18N.getString("RMIImageImpl4")+" \""+
                               serverName+"\".");
            Naming.rebind(serverName, im);
            System.out.println(JaiI18N.getString("RMIImageImpl5"));
        } catch (Exception e) {
            String message = JaiI18N.getString("RMIImageImpl1");
            sendExceptionToListener(message,
                                   new RemoteImagingException(message, e));
/*
            System.err.println(JaiI18N.getString("RMIImageImpl0") +
                               e.getMessage());
            e.printStackTrace();
*/
        }
    }

    private static void sendExceptionToListener(String message, Exception e) {
        ImagingListener listener =
            ImageUtil.getImagingListener((RenderingHints)null);
        listener.errorOccurred(message,
                               new RemoteImagingException(message, e),
                               JAIRMIImageServer.class, false);
    }
}
