/*
 * $RCSfile: RMIImageImpl.java,v $
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
import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderContext;
import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.RMISecurityManager;
import java.rmi.server.UnicastRemoteObject;
import java.util.Hashtable;
import java.util.Vector;
import com.lightcrafts.mediax.jai.PlanarImage;
import com.lightcrafts.mediax.jai.PropertySource;
import com.lightcrafts.mediax.jai.RenderableOp;
import com.lightcrafts.mediax.jai.RenderedOp;
import com.lightcrafts.mediax.jai.remote.SerializableRenderedImage;
import com.lightcrafts.mediax.jai.remote.RemoteImagingException;
import com.lightcrafts.mediax.jai.util.ImagingListener;
import com.lightcrafts.media.jai.util.ImageUtil;

/* A singleton class representing the serializable version of a null
   property. This required because java.awt.Image.UndefinedProperty
   is not serializable. */
class NullPropertyTag implements Serializable {
    NullPropertyTag() {}
}

/**
 * The server-side implementation of the RMIImage interface.  A
 * RMIImageImpl has a RenderedImage source, acquired via one of three
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
 * construct new instances of RMIImageImpl on demand.
 *
 * @see RMIImage
 * @see RemoteImage
 * @see RenderedOp
 *
 * @since EA3
 *
 */
public class RMIImageImpl implements RMIImage {
    /** Tag to represent a null property. */
    public static final Object NULL_PROPERTY = new NullPropertyTag();

    /** Identifier counter for the remote images. */
    private static long idCounter = 0;

    /**
     * The RenderedImage sources hashed by an ID string which must be unique
     * across all possible clients of this object.
     */
    private static Hashtable sources = null;

    /**
     * The PropertySources hashed by an ID string which must be unique
     * across all possible clients of this object.
     */
    private static Hashtable propertySources = null;

    /**
     * Adds a RenderedImage source to the Hashtable of sources.
     *
     * @param id A unique ID for the source.
     * @param source The source RenderedImage.
     * @param ps The PropertySource.
     */
    private static synchronized void addSource(Long id,
                                               RenderedImage source,
                                               PropertySource ps) {
        // Create the Hashtables "just in time".
        if(sources == null) {
            sources = new Hashtable();
            propertySources = new Hashtable();
        }

        // Add the source and PropertySource.
        sources.put(id, source);
        propertySources.put(id, ps);
    }

    /**
     * Retrieve a PlanarImage source from the Hashtable of sources.
     *
     * @param id The unique ID of the source.
     * @return The source.
     */
    private static PlanarImage getSource(Long id) throws RemoteException {
        Object obj = null;
        if(sources == null ||
           (obj = sources.get(id)) == null) {
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
        Object obj = null;
        if(propertySources == null ||
           (obj = propertySources.get(id)) == null) {
            throw new RemoteException(JaiI18N.getString("RMIImageImpl2"));
        }

        return (PropertySource)obj;
    }

    /**
     * Constructs a RMIImageImpl with a source to be specified
     * later.
     */
    public RMIImageImpl() throws RemoteException {
        super();
        try {
            UnicastRemoteObject.exportObject(this);
        } catch(RemoteException e) {
            ImagingListener listener =
                ImageUtil.getImagingListener((RenderingHints)null);
            String message = JaiI18N.getString("RMIImageImpl0");
            listener.errorOccurred(message,
                                   new RemoteImagingException(message, e),
                                   this, false);
/*
            e.printStackTrace();
            throw new RuntimeException(JaiI18N.getString("RMIImageImpl0") +
                                       e.getMessage());
*/
        }
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
    public void setSource(Long id, RenderedImage source)
        throws RemoteException {
            PlanarImage pi = PlanarImage.wrapRenderedImage(source);
            addSource(id, pi, pi);
    }

    /**
     * Sets the source to a RenderedOp (i.e., an imaging DAG).
     * This DAG will be copied over to the server where it will be
     * transformed into an OpImage chain using the server's local
     * OperationRegistry and available RenderedImageFactory objects.
     *
     * @param id An ID for the source which must be unique across all clients.
     * @param source a RenderedOp source.
     */
    public void setSource(Long id,
                          RenderedOp source)
        throws RemoteException {
        addSource(id, source.getRendering(), source);
    }

    /**
     * Sets the source to a RenderableOp defined by a renderable imaging
     * DAG and a rendering context.  The entire RenderableImage
     * DAG will be copied over to the server.
     */
    public void setSource(Long id,
                          RenderableOp source,
                          RenderContextProxy renderContextProxy)
        throws RemoteException {
            RenderContext renderContext =
                renderContextProxy.getRenderContext();
            RenderedImage r = source.createRendering(renderContext);
            PlanarImage pi = PlanarImage.wrapRenderedImage(r);
            addSource(id, pi, pi);
    }

    /**
     * Disposes of any resouces allocated to the client object with
     * the specified ID.
     */
    public void dispose(Long id)  throws RemoteException {
        if(sources != null) {
            sources.remove(id);
            propertySources.remove(id);
        }
    }

    /** Gets a property from the property set of this image.  If the
      property is undefined the constant NULL_PROPERTY is returned. */
    public Object getProperty(Long id, String name) throws RemoteException {
        PropertySource ps = getPropertySource(id);
        Object property = ps.getProperty(name);
        if(property == null ||
           property.equals(java.awt.Image.UndefinedProperty)) {
            property = NULL_PROPERTY;
        }
        return property;
    }

    /**
     * Returns a list of names recognized by getProperty().
     *
     * @return an array of Strings representing proeprty names.
     */
    public String[] getPropertyNames(Long id) throws RemoteException {
        PropertySource ps = getPropertySource(id);
        return ps.getPropertyNames();
    }

    /** Returns the minimum X coordinate of the RMIImage. */
    public int getMinX(Long id) throws RemoteException {
        return getSource(id).getMinX();
    }

    /** Returns the smallest X coordinate to the right of the RMIImage. */
    public int getMaxX(Long id) throws RemoteException {
        return getSource(id).getMaxX();
    }

    /** Returns the minimum Y coordinate of the RMIImage. */
    public int getMinY(Long id) throws RemoteException {
        return getSource(id).getMinY();
    }

    /** Returns the smallest Y coordinate below the RMIImage. */
    public int getMaxY(Long id) throws RemoteException {
        return getSource(id).getMaxY();
    }

    /** Returns the width of the RMIImage. */
    public int getWidth(Long id) throws RemoteException {
        return getSource(id).getWidth();
    }

    /** Returns the height of the RMIImage. */
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
    public SampleModelProxy getSampleModel(Long id) throws RemoteException {
        return new SampleModelProxy(getSource(id).getSampleModel());
    }

    /** Returns the ColorModel associated with this image. */
    public ColorModelProxy getColorModel(Long id) throws RemoteException {
        return new ColorModelProxy(getSource(id).getColorModel());

    }

    /**
     * Returns a vector of RenderedImages that are the sources of
     * image data for this RMIImage.  Note that this method
     * will often return an empty vector.
     */
    public Vector getSources(Long id) throws RemoteException {
        Vector sourceVector = getSource(id).getSources();
        int size = sourceVector.size();
        boolean isCloned = false;
        for(int i = 0; i < size; i++) {
            RenderedImage img = (RenderedImage)sourceVector.get(i);
            if(!(img instanceof Serializable)) {
                if(!isCloned) {
                    sourceVector = (Vector)sourceVector.clone();
                }
                sourceVector.set(i, new SerializableRenderedImage(img, false));
            }
        }
        return sourceVector;
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
    public RasterProxy getTile(Long id, int tileX, int tileY)
        throws RemoteException {
        return new RasterProxy(getSource(id).getTile(tileX, tileY));
    }

    /**
     * Returns the entire image as a single Raster.
     *
     * @return a Raster containing a copy of this image's data.
     */
    public RasterProxy getData(Long id) throws RemoteException {
        return new RasterProxy(getSource(id).getData());
    }

    /**
     * Returns an arbitrary rectangular region of the RenderedImage
     * in a Raster.  The rectangle of interest will be clipped against
     * the image bounds.
     *
     * @param id An ID for the source which must be unique across all clients.
     * @param rect the region of the RenderedImage to be returned.
     * @return a Raster containing a copy of the desired data.
     */
    public RasterProxy getData(Long id, Rectangle bounds)
        throws RemoteException {
        RasterProxy rp = null;
        if(bounds == null) {
            rp = getData(id);
        } else {
            bounds = bounds.intersection(getBounds(id));
            rp = new RasterProxy(getSource(id).getData(bounds));
        }
        return rp;
    }

    /**
     * Returns the same result as getData(Rectangle) would for the
     * same rectangular region.
     */
    public RasterProxy copyData(Long id, Rectangle bounds)
        throws RemoteException {
        return getData(id, bounds);
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
     * file:`pwd`/policy com.lightcrafts.media.jai.rmi.RMIImageImpl \
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

        // Set the host name and port number.
        String host = null;
        int port = 1099; // default port is 1099
        for(int i = 0; i < args.length; i++) {
            if(args[i].equalsIgnoreCase("-host")) {
                host = args[++i];
            } else if(args[i].equalsIgnoreCase("-port")) {
                port = Integer.parseInt(args[++i]);
            }
        }

        // Default to the local host if the host was not specified.
        if(host == null) {
            try {
                host = InetAddress.getLocalHost().getHostAddress();
            } catch(java.net.UnknownHostException e) {
                System.err.println(JaiI18N.getString("RMIImageImpl1") +
                                   e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.println(JaiI18N.getString("RMIImageImpl3")+" "+
                           host+":"+port);

        try {
            RMIImageImpl im = new RMIImageImpl();
            String serverName =
                new String("rmi://" +
                           host + ":" + port + "/" +
                           RMIImage.RMI_IMAGE_SERVER_NAME);
            System.out.println(JaiI18N.getString("RMIImageImpl4")+" \""+
                               serverName+"\".");
            Naming.rebind(serverName, im);
            System.out.println(JaiI18N.getString("RMIImageImpl5"));
        } catch (Exception e) {
            System.err.println(JaiI18N.getString("RMIImageImpl0") +
                               e.getMessage());
            e.printStackTrace();
        }
    }
}
