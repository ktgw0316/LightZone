/*
 * $RCSfile: ImageServer.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:51 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.remote.NegotiableCapabilitySet;
import com.lightcrafts.mediax.jai.remote.SerializableState;

/**
 * An interface for server-side imaging.  This interface attempts to
 * mimic the RenderedImage interface as much as possible.  However, there
 * are several unavoidable differences:
 *
 * <ul>
 * <li> Additional setRenderedSource() and setRenderableSource methods
 * are provided to inform the server as to the source of image data for
 * this image.  Sources may be set
 * either from a RenderedImage that is copied over to the server, or
 * from a graph of RenderedOp objects indicating an abstract
 * imaging chain to be instantiated using the server's
 * OperationRegistry.
 *
 * <li> All methods throw RemoteException.  This is a requirement of
 * any Remote interface.
 *
 * <li> The getTile() method does not return a reference to a `live'
 * tile; instead it returns a client-side copy of the server image's
 * tile.  The difference is moot since the server image is immutable.
 * </ul>
 *
 * To instantiate a ImageServer, do the following:
 *
 * <pre>
 * ImageServer im;
 * im = java.rmi.Naming.lookup("//host:1099/com.lightcrafts.mediax.jai.RemoteImageServer");
 * </pre>
 *
 * <p> The hostname and port will of course depend on the local setup.
 * The host must be running an <code>rmiregistry</code> process and have a
 * RemoteImageServer listening at the desired port.
 *
 * <p> This call will result in the creation of a server-side
 * JAIRMIImageServer object and a client-side stub object.
 * The client stub serializes its method arguments and transfers
 * them to the server over a socket; the server serializes it return
 * values and returns them in the same manner.
 *
 * <p> This process implies that all arguments and return values must
 * be serializable.  In the case of a RenderedImage source,
 * serializability is not guaranteed and must be considered on a
 * class-by-class basis.  For RenderedOps, which are basically
 * simple nodes connected by ParameterBlocks, serializability will be
 * determined by the serializabiility of the ultimate
 * (non-RenderedOp) sources of the DAG and the serializability
 * of any ad-hoc Object parameters held in the ParameterBlocks.
 *
 * <p> The return values of the getData(), copyData(), and getTile()
 * methods are various kinds of Rasters; at present, Java2D does not
 * define serialization on Rasters.  We will either need to add this
 * feature to Java2D or else coerce the server-side Rasters into a
 * serializable subclass form.  In any case, we will want to
 * implement lossless (and possibly lossy) compression as part of
 * the serialization process wherever possible.
 *
 * @see java.rmi.Remote
 * @see java.rmi.RemoteException
 * @see java.awt.image.RenderedImage
 *
 *
 */
public interface ImageServer extends Remote {

    /**
     * Returns the identifier of the remote image. This method should be
     * called to return an identifier before any other methods are invoked.
     * The same ID must be used in all subsequent references to the remote
     * image.
     */
    Long getRemoteID() throws RemoteException;

    /**
     * Disposes of any resouces allocated to the client object with
     * the specified ID.
     */
    void dispose(Long id) throws RemoteException;

    /**
     * Increments the reference count for this id, i.e. increments the
     * number of RMIServerProxy objects that currently reference this id.
     */
    void incrementRefCount(Long id) throws RemoteException;


    /// Methods Common To Rendered as well as Renderable modes.


    /**
     * Gets a property from the property set of this image.
     * If the property name is not recognized, java.awt.Image.UndefinedProperty
     * will be returned.
     *
     * @param id An ID for the source which must be unique across all clients.
     * @param name the name of the property to get, as a String.
     * @return a reference to the property Object, or the value
     *         java.awt.Image.UndefinedProperty.
     */
    Object getProperty(Long id, String name) throws RemoteException;

    /**
     * Returns a list of names recognized by getProperty(String).
     *
     * @return an array of Strings representing proeprty names.
     */
    String [] getPropertyNames(Long id) throws RemoteException;

    /**
     * Returns a list of names recognized by getProperty().
     *
     * @return an array of Strings representing property names.
     */
     String[] getPropertyNames(String opName) throws RemoteException;


    /// Rendered Mode Methods


    /** Returns the ColorModel associated with this image. */
    SerializableState getColorModel(Long id) throws RemoteException;

    /** Returns the SampleModel associated with this image. */
    SerializableState getSampleModel(Long id) throws RemoteException;

    /** Returns the width of the image on the ImageServer. */
    int getWidth(Long id) throws RemoteException;

    /** Returns the height of the image on the ImageServer. */
    int getHeight(Long id) throws RemoteException;

    /**
     * Returns the minimum X coordinate of the image on the ImageServer.
     */
    int getMinX(Long id) throws RemoteException;

    /**
     * Returns the minimum Y coordinate of the image on the ImageServer.
     */
    int getMinY(Long id) throws RemoteException;

    /** Returns the number of tiles across the image. */
    int getNumXTiles(Long id) throws RemoteException;

    /** Returns the number of tiles down the image. */
    int getNumYTiles(Long id) throws RemoteException;

    /**
     * Returns the index of the minimum tile in the X direction of the image.
     */
    int getMinTileX(Long id) throws RemoteException;

    /**
     * Returns the index of the minimum tile in the Y direction of the image.
     */
    int getMinTileY(Long id) throws RemoteException;

    /** Returns the width of a tile in pixels. */
    int getTileWidth(Long id) throws RemoteException;

    /** Returns the height of a tile in pixels. */
    int getTileHeight(Long id) throws RemoteException;

    /** Returns the X offset of the tile grid relative to the origin. */
    int getTileGridXOffset(Long id) throws RemoteException;

    /** Returns the Y offset of the tile grid relative to the origin. */
    int getTileGridYOffset(Long id) throws RemoteException;

    /**
     * Returns tile (x, y).  Note that x and y are indices into the
     * tile array, not pixel locations.  Unlike in the true RenderedImage
     * interface, the Raster that is returned should be considered a copy.
     *
     * @param id An ID for the source which must be unique across all clients.
     * @param x the x index of the requested tile in the tile array
     * @param y the y index of the requested tile in the tile array
     * @return a copy of the tile as a Raster.
     */
    SerializableState getTile(Long id, int x, int y) throws RemoteException;

    /**
     * Compresses tile (x, y) and returns the compressed tile's contents
     * as a byte array.  Note that x and y are indices into the
     * tile array, not pixel locations.
     *
     * @param id An ID for the source which must be unique across all clients.
     * @param x the x index of the requested tile in the tile array
     * @param y the y index of the requested tile in the tile array
     * @return a byte array containing the compressed tile contents.
     */
    byte[] getCompressedTile(Long id, int x, int y) throws RemoteException;

    /**
     * Returns the entire image as a single Raster.
     *
     * @return a SerializableState containing a copy of this image's data.
     */
    SerializableState getData(Long id) throws RemoteException;

    /**
     * Returns an arbitrary rectangular region of the RenderedImage
     * in a Raster.  The rectangle of interest will be clipped against
     * the image bounds.
     *
     * @param id An ID for the source which must be unique across all clients.
     * @param bounds the region of the RenderedImage to be returned.
     * @return a SerializableState containing a copy of the desired data.
     */
    SerializableState getData(Long id, Rectangle bounds) 
	throws RemoteException;

    /**
     * Returns the same result as getData(Rectangle) would for the
     * same rectangular region.
     */
    SerializableState copyData(Long id, Rectangle bounds)
	throws RemoteException;

    /**
     * Creates a RenderedOp on the server side with a parameter block
     * empty of sources. The sources are set by separate calls depending
     * upon the type and serializabilty of the source.
     */

    void createRenderedOp(Long id, String opName,
			  ParameterBlock pb,
			  SerializableState hints) throws RemoteException;

    /**
     * Calls for Rendering of the Op and returns true if the RenderedOp
     * could be rendered else false
     */
    boolean getRendering(Long id) throws RemoteException;

    /**
     * Retrieve a node from the hashtable.
     */
    RenderedOp getNode(Long id) throws RemoteException;

    /**
     *  Sets the source of the image as a RenderedImage on the server side
     */
    void setRenderedSource(Long id, RenderedImage source, int index)
	throws RemoteException;

    /**
     *  Sets the source of the image as a RenderedOp on the server side
     */
    void setRenderedSource(Long id, RenderedOp source, int index)
	throws RemoteException;

    /**
     * Sets the source of the image which is on the same
     * server
     */
    void setRenderedSource(Long id, Long sourceId, int index)
	throws RemoteException;

    /**
     * Sets the source of the image which is on a different
     * server
     */
    void setRenderedSource(Long id, Long sourceId, String serverName,
			   String opName, int index) throws RemoteException;


    /// Renderable mode methods


    /** 
     * Gets the minimum X coordinate of the rendering-independent image
     * stored against the given ID.
     *
     * @return the minimum X coordinate of the rendering-independent image
     * data.
     */
    float getRenderableMinX(Long id) throws RemoteException;

    /** 
     * Gets the minimum Y coordinate of the rendering-independent image
     * stored against the given ID.
     *
     * @return the minimum X coordinate of the rendering-independent image
     * data.
     */
    float getRenderableMinY(Long id) throws RemoteException;

    /** 
     * Gets the width (in user coordinate space) of the 
     * <code>RenderableImage</code> stored against the given ID.
     *
     * @return the width of the renderable image in user coordinates.
     */
    float getRenderableWidth(Long id) throws RemoteException;
    
    /**
     * Gets the height (in user coordinate space) of the 
     * <code>RenderableImage</code> stored against the given ID.
     *
     * @return the height of the renderable image in user coordinates.
     */
    float getRenderableHeight(Long id) throws RemoteException;

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
    RenderedImage createScaledRendering(Long id, 
					int w, 
					int h, 
					SerializableState hintsState) 
	throws RemoteException;
  
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
    RenderedImage createDefaultRendering(Long id) throws RemoteException;
  
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
    RenderedImage createRendering(Long id, 
				  SerializableState renderContextState) 
	throws RemoteException;

    /**
     * Creates a RenderableOp on the server side with a parameter block
     * empty of sources. The sources are set by separate calls depending
     * upon the type and serializabilty of the source.
     */
    void createRenderableOp(Long id, String opName, ParameterBlock pb)
	throws RemoteException;

    /**
     * Calls for rendering of a RenderableOp with the given SerializableState
     * which should be a RenderContextState.
     */
    Long getRendering(Long id, SerializableState rcs) throws RemoteException;

    /**
     * Sets the source of the image which is on the same
     * server
     */
    void setRenderableSource(Long id, Long sourceId, int index)
	throws RemoteException;

    /**
     * Sets the source of the image which is on a different
     * server
     */
    void setRenderableSource(Long id, Long sourceId, String serverName,
			     String opName, int index) throws RemoteException;

    /**
     * Sets the source of the operation refered to by the supplied 
     * <code>id</code> to the <code>RenderableRMIServerProxy</code>
     * that exists on the supplied <code>serverName</code> under the
     * supplied <code>sourceId</code>. 
     */
    void setRenderableRMIServerProxyAsSource(Long id,
					     Long sourceId, 
					     String serverName,
					     String opName,
					     int index) throws RemoteException;

    /**
     *  Sets the source of the image as a RenderableOp on the server side.
     */
    void setRenderableSource(Long id, RenderableOp source,
			     int index) throws RemoteException;

    /**
     *  Sets the source of the image as a RenderableImage on the server side.
     */
    void setRenderableSource(Long id, SerializableRenderableImage source,
			     int index) throws RemoteException;

    /**
     *  Sets the source of the image as a RenderedImage on the server side
     */
    void setRenderableSource(Long id, RenderedImage source, int index)
	throws RemoteException;

    /**
     * Maps the RenderContext for the remote Image
     */
    SerializableState mapRenderContext(int id, Long nodeId,
				       String operationName,
				       SerializableState rcs)
	throws RemoteException;

    /**
     * Gets the Bounds2D of the specified Remote Image
     */
    SerializableState getBounds2D(Long nodeId, String operationName)
	throws RemoteException;

    /**
     * Returns <code>true</code> if successive renderings with the same
     * arguments may produce different results for this opName
     *
     * @return <code>false</code> indicating that the rendering is static.
     */
    public boolean isDynamic(String opName) throws RemoteException;

    /**
     * Returns <code>true</code> if successive renderings with the same
     * arguments may produce different results for this opName
     *
     * @return <code>false</code> indicating that the rendering is static.
     */
    public boolean isDynamic(Long id) throws RemoteException;

    /**
     * Gets the operation names supported on the Server
     */
    String[] getServerSupportedOperationNames() throws RemoteException;

    /**
     * Gets the <code>OperationDescriptor</code>s of the operations
     * supported on this server.
     */
    List getOperationDescriptors() throws RemoteException;

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
    SerializableState getInvalidRegion(Long id,
				       ParameterBlock oldParamBlock,
				       SerializableState oldHints,
				       ParameterBlock newParamBlock,
				       SerializableState newHints)
	throws RemoteException;

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
    Rectangle mapSourceRect(Long id, Rectangle sourceRect, int sourceIndex)
	throws RemoteException;

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
    Rectangle mapDestRect(Long id, Rectangle destRect, int sourceIndex)
	throws RemoteException;

    /**
     * A method that handles a change in some critical parameter.
     */
    Long handleEvent(Long renderedOpID, 
		     String propName,
		     Object oldValue, 
		     Object newValue) throws RemoteException;

    /**
     * A method that handles a change in one of it's source's rendering,
     * i.e. a change that would be signalled by RenderingChangeEvent.
     */
    Long handleEvent(Long renderedOpID, 
		     int srcIndex,
		     SerializableState srcInvalidRegion, 
		     Object oldRendering) throws RemoteException;

    /**
     * Returns the server's capabilities as a
     * <code>NegotiableCapabilitySet</code>. Currently the only capabilities
     * that are returned are those of TileCodecs.
     */
    NegotiableCapabilitySet getServerCapabilities() throws RemoteException;

    /**
     * Informs the server of the negotiated values that are the result of
     * a successful negotiation.
     *
     * @param id An ID for the node which must be unique across all clients.
     * @param negotiatedValues    The result of the negotiation.
     */
    void setServerNegotiatedValues(Long id, 
				   NegotiableCapabilitySet negotiatedValues)
	throws RemoteException; 
}
