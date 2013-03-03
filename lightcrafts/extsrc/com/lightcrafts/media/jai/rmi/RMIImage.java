/*
 * $RCSfile: RMIImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:56:52 $
 * $State: Exp $
 */
package com.lightcrafts.media.jai.rmi;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Vector;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;

/**
 * An interface for server-side imaging.  This interface attempts to
 * mimic the RenderedImage interface as much as possible.  However, there
 * are several unavoidable differences:
 *
 * <ul>
 * <li> Additional setSource() methods are provided to inform the server
 * as to the source of image data for this image.  Sources may be set
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
 * To instantiate a RMIImage, do the following:
 *
 * <pre>
 * RMIImage im;
 * im = java.rmi.Naming.lookup("//host:1099/com.lightcrafts.mediax.jai.RemoteImageServer");
 * </pre>
 *
 * <p> The hostname and port will of course depend on the local setup.
 * The host must be running an <code>rmiregistry</code> process and have a
 * RemoteImageServer listening at the desired port.
 *
 * <p> This call will result in the creation of a server-side
 * RMIImageImpl object and a client-side stub object.
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
 * @see RemoteImage
 *
 * @since EA3
 *
 */
public interface RMIImage extends Remote {
    /**
     * The name to which the remote image server should be bound.
     */
    public static final String RMI_IMAGE_SERVER_NAME = "RemoteImageServer";

    /**
     * Returns the identifier of the remote image. This method should be
     * called to return an identifier before any other methods are invoked.
     * The same ID must be used in all subsequent references to the remote
     * image.
     */
    Long getRemoteID() throws RemoteException;

    /**
     * Sets the source of the image on the server side.  This source
     * should ideally be a lightweight reference to an image available
     * locally on the server or over a further network link (for
     * example, an IIPOpImage that contains a URL but not actual image
     * data).
     *
     * <p> Although it is legal to use any RenderedImage, one should be
     * aware that a deep copy might be made and transmitted to the server.
     *
     * @param id An ID for the source which must be unique across all clients.
     * @param source a RenderedImage source.
     */
    void setSource(Long id, RenderedImage source) throws RemoteException;

    /**
     * Sets the source to a RenderedOp (i.e., an imaging DAG).
     * This DAG will be copied over to the server where it will be
     * transformed into an OpImage chain using the server's local
     * OperationRegistry and available RenderedImageFactory objects.
     *
     * @param id An ID for the source which must be unique across all clients.
     * @param source a RenderedOp source.
     */
    void setSource(Long id, RenderedOp source) throws RemoteException;

    /**
     * Sets the source to a RenderableOp defined by a renderable imaging
     * DAG and a rendering context.  The entire RenderableImage
     * DAG will be copied over to the server.
     */
    void setSource(Long id, RenderableOp source, RenderContextProxy renderContextProxy)
        throws RemoteException;

    /**
     * Disposes of any resouces allocated to the client object with
     * the specified ID.
     */
    void dispose(Long id) throws RemoteException;

    /**
     * Returns a vector of RenderedImages that are the sources of
     * image data for this RMIImage.  Note that this method
     * will often return an empty vector.
     */
    Vector getSources(Long id) throws RemoteException;

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

    /** Returns the ColorModel associated with this image. */
    ColorModelProxy getColorModel(Long id) throws RemoteException;

    /** Returns the SampleModel associated with this image. */
    SampleModelProxy getSampleModel(Long id) throws RemoteException;

    /** Returns the width of the RMIImage. */
    int getWidth(Long id) throws RemoteException;

    /** Returns the height of the RMIImage. */
    int getHeight(Long id) throws RemoteException;

    /**
     * Returns the minimum X coordinate of the RMIImage.
     */
    int getMinX(Long id) throws RemoteException;

    /**
     * Returns the minimum Y coordinate of the RMIImage.
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
    RasterProxy getTile(Long id, int x, int y) throws RemoteException;

    /**
     * Returns the entire image as a single Raster.
     *
     * @return a RasterProxy containing a copy of this image's data.
     */
    RasterProxy getData(Long id) throws RemoteException;

    /**
     * Returns an arbitrary rectangular region of the RenderedImage
     * in a Raster.  The rectangle of interest will be clipped against
     * the image bounds.
     *
     * @param id An ID for the source which must be unique across all clients.
     * @param bounds the region of the RenderedImage to be returned.
     * @return a RasterProxy containing a copy of the desired data.
     */
    RasterProxy getData(Long id, Rectangle bounds) throws RemoteException;

    /**
     * Returns the same result as getData(Rectangle) would for the
     * same rectangular region.
     */
    RasterProxy copyData(Long id, Rectangle bounds) throws RemoteException;
}
