/*
 * $RCSfile: RemoteImage.java,v $
 *
 * Copyright (c) 2005 Sun Microsystems, Inc. All rights reserved.
 *
 * Use is subject to license terms.
 *
 * $Revision: 1.1 $
 * $Date: 2005/02/11 04:57:19 $
 * $State: Exp $
 */
package com.lightcrafts.mediax.jai;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.image.renderable.RenderContext;
import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Vector;
import com.lightcrafts.mediax.jai.remote.SerializableRenderedImage;
import com.lightcrafts.media.jai.rmi.RMIImage;
import com.lightcrafts.media.jai.rmi.RasterProxy;
import com.lightcrafts.media.jai.rmi.RenderContextProxy;

/**
 * A sub-class of <code>PlanarImage</code> which represents an image on a
 * remote server machine.
 *
 * <p> The image may be constructed from a <code>RenderedImage</code> or from
 * an imaging chain in either the rendered or renderable mode. Network errors
 * (detected via throws of <code>RemoteException</code>s) are dealt with through retries;
 * when the limit of retries is exceeded, a <code>null</code> Raster may be returned.
 * The default number of retries is set to 5 and the default timeout is set
 * to 1 second.
 *
 * <p> Note that the registry of the server will be used. In particular if
 * an <code>OperationRegistry</code> was present in the
 * <code>RenderingHints</code> used to construct a <code>RenderedOp</code>
 * or <code>RenderableOp</code> it will not be serialized and transmitted
 * to the server.
 *
 * <p> Image layout attributes, once requested, are cached locally for speed.
 *
 * @deprecated as of JAI 1.1 in favor of
 * <code>com.lightcrafts.mediax.jai.remote.RemoteJAI</code>.
 *
 */
public class RemoteImage extends PlanarImage {

    /** The amount of time to wait between retries. */
    static final int DEFAULT_TIMEOUT = 1000; // Milliseconds

    /** The default number of retries. */
    static final int DEFAULT_NUM_RETRIES = 5;

    /** Index of local variable. */
    static final int VAR_MIN_X              =  0;
    /** Index of local variable. */
    static final int VAR_MIN_Y              =  1;
    /** Index of local variable. */
    static final int VAR_WIDTH              =  2;
    /** Index of local variable. */
    static final int VAR_HEIGHT             =  3;
    /** Index of local variable. */
    static final int VAR_TILE_WIDTH         =  4;
    /** Index of local variable. */
    static final int VAR_TILE_HEIGHT        =  5;
    /** Index of local variable. */
    static final int VAR_TILE_GRID_X_OFFSET =  6;
    /** Index of local variable. */
    static final int VAR_TILE_GRID_Y_OFFSET =  7;
    /** Index of local variable. */
    static final int VAR_SAMPLE_MODEL       =  8;
    /** Index of local variable. */
    static final int VAR_COLOR_MODEL        =  9;
    /** Index of local variable. */
    static final int VAR_SOURCES            = 10;
    /** Index of local variable. */
    static final int NUM_VARS               = 11;

    /** The class of the serializable representation of a NULL property. */
    private static final Class NULL_PROPERTY_CLASS =
        com.lightcrafts.media.jai.rmi.RMIImageImpl.NULL_PROPERTY.getClass();

    /** The RMIImage our data will come from. */
    protected RMIImage remoteImage;

    /** The RMI ID of this object. */
    private Long id = null;

    /** Valid bits for locally cached variables. */
    protected boolean [] fieldValid = new boolean[NUM_VARS];

    /** Locally cached version of properties. */
    protected String[] propertyNames = null;

    /** The amount of time between retries (milliseconds). */
    protected int timeout = DEFAULT_TIMEOUT;

    /** The number of retries. */
    protected int numRetries = DEFAULT_NUM_RETRIES;

    /** The bounds of this image. */
    private Rectangle imageBounds = null;

    private static Vector vectorize(RenderedImage image) {
        Vector v = new Vector(1);
        v.add(image);
        return v;
    }

    /**
     * Constructs a <code>RemoteImage</code> from a <code>RenderedImage</code>.
     *
     * <p> The <code>RenderedImage</code> source should ideally be a lightweight
     * reference to an image available locally on the server or over a
     * further network link.
     *
     * <p> Although it is legal to use any <code>RenderedImage</code>, one should be
     * aware that this will require copying of the image data via transmission
     * over a network link.
     *
     * <p> The name of the server must be supplied in the form appropriate to
     * the implementation. In the reference port of JAI, RMI is used to
     * implement remote imaging so that the server name must be supplied in
     * the format
     * <pre>
     * host:port
     * </pre>
     * where the port number is optional and may be supplied only if
     * the host name is supplied. If this parameter is <code>null</code> the default
     * is to search for the RMIImage service on the local host at the
     * default <i>rmiregistry</i> port (1099).
     *
     * @param serverName The name of the server in the appropriate format.
     * @param source A <code>RenderedImage</code> source which must not be <code>null</code>.
     * @throws IllegalArgumentException if <code>source</code> is
     *         <code>null</code>.
     */
    public RemoteImage(String serverName, RenderedImage source) {
        super(null, null, null);

        if(serverName == null) 
            serverName = getLocalHostAddress();

	// Look for a separator indicating the remote image chaining hack
	// in which case the serverName argument contains host[:port]::id
	// where id is the RMI ID of the image on the indicated server.
	int index = serverName.indexOf("::");
	boolean remoteChainingHack = index != -1;

	if(!remoteChainingHack && source == null) {
	    // Don't throw the NullPointerException if it's the hack.
	    throw new
		IllegalArgumentException(JaiI18N.getString("RemoteImage1"));
	}

	if(remoteChainingHack) {
	    // Extract the RMI ID from the servername string and replace
	    // the original serverName string with one of the usual type.
	    id = Long.valueOf(serverName.substring(index+2));
	    serverName = serverName.substring(0, index);
	}

        // Construct the remote RMI image.
        getRMIImage(serverName);

        if(!remoteChainingHack) {
            // Get the RMI ID for this object.
            getRMIID();
        }

        // Cache the server name and RMI ID in a property.
        setRMIProperties(serverName);

        if(source != null) { // Source may be null only for the hack.
            try {
                if(source instanceof Serializable) {
                    remoteImage.setSource(id, source);
                } else {
                    remoteImage.setSource(id,
                                          new
                                          SerializableRenderedImage(source));
                }
            } catch(RemoteException e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }

    /**
     * Constructs a <code>RemoteImage</code> from a <code>RenderedOp</code>,
     * i.e., an imaging directed acyclic graph (DAG).
     *
     * <p> This DAG will be copied over to the server where it will be
     * transformed into an <code>OpImage</code> chain using the server's local
     * <code>OperationRegistry</code> and available <code>RenderedImageFactory</code> objects.
     *
     * <p> The name of the server must be supplied in the form appropriate to
     * the implementation. In the reference port of JAI, RMI is used to
     * implement remote imaging so that the server name must be supplied in
     * the format
     * <pre>
     * host:port
     * </pre>
     * where the port number is optional and may be supplied only if
     * the host name is supplied. If this parameter is <code>null</code> the default
     * is to search for the RMIImage service on the local host at the
     * default <i>rmiregistry</i> port (1099).
     *
     * <p> Note that the properties of the <code>RemoteImage</code> will be
     * those of the <code>RenderedOp</code> node and not of its rendering.
     *
     * @param serverName The name of the server in the appropriate format.
     * @param source A <code>RenderedOp</code> source which must not be <code>null</code>.
     * @throws IllegalArgumentException if <code>source</code> is
     *         <code>null</code>.
     */
    public RemoteImage(String serverName, RenderedOp source) {
        super(null, null, null);

        if(serverName == null) 
            serverName = getLocalHostAddress();

        if(source == null) {
            throw new
		IllegalArgumentException(JaiI18N.getString("RemoteImage1"));
        }

        // Construct the remote RMI image.
        getRMIImage(serverName);

        // Get the RMI ID for this object.
        getRMIID();

        // Cache the server name and RMI ID in a property.
        setRMIProperties(serverName);

        try {
            remoteImage.setSource(id, source);
        } catch(RemoteException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Constructs a <code>RemoteImage</code> from a <code>RenderableOp</code>
     * and <code>RenderContext</code>.
     * The entire <code>RenderableOp</code> DAG will be copied over to the server.
     *
     * <p> The name of the server must be supplied in the form appropriate to
     * the implementation. In the reference port of JAI, RMI is used to
     * implement remote imaging so that the server name must be supplied in
     * the format
     * <pre>
     * host:port
     * </pre>
     * where the port number is optional and may be supplied only if
     * the host name is supplied. If this parameter is <code>null</code> the default
     * is to search for the RMIImage service on the local host at the
     * default <i>rmiregistry</i> port (1099).
     *
     * <p> Note that the properties of the <code>RemoteImage</code> will be
     * those of the <code>RenderableOp</code> node and not of its rendering.
     *
     * @param serverName The name of the server in the appropriate format.
     * @param source A <code>RenderableOp</code> source which must not be <code>null</code>.
     * @param renderContext The rendering context which may be <code>null</code>.
     * @throws IllegalArgumentException if <code>source</code> is
     *         <code>null</code>.
     */
    public RemoteImage(String serverName,
                       RenderableOp source,
                       RenderContext renderContext) {
        super(null, null, null);

        if(serverName == null) 
            serverName = getLocalHostAddress();

        if(source == null) {
            throw new
		IllegalArgumentException(JaiI18N.getString("RemoteImage1"));
        }

	if (renderContext == null) {
	    renderContext = new RenderContext(new AffineTransform());
	}

        // Construct the remote RMI image.
        getRMIImage(serverName);

        // Get the RMI ID for this object.
        getRMIID();

        // Cache the server name and RMI ID in a property.
        setRMIProperties(serverName);

        // Create the serializable form of the <code>RenderContext</code>.
        RenderContextProxy rcp = new RenderContextProxy(renderContext);

        try {
            remoteImage.setSource(id, source, rcp);
        } catch(RemoteException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Construct an RMIImage on the indicated server.
     *
     * <p> The name of the server must be supplied in the form
     * <pre>
     * host:port
     * </pre>
     * where the port number is optional and may be supplied only if
     * the host name is supplied. If this parameter is <code>null</code> the default
     * is to search for the RMIImage service on the local host at the
     * default <i>rmiregistry</i> port (1099).
     *
     * <p> The result is cached in the instance variable "remoteImage".
     *
     * @param serverName The name of the server in the format described.
     */
    private void getRMIImage(String serverName) {
        // Set the server name to the local host if null.
        if(serverName == null)
	    serverName = getLocalHostAddress(); 

        // Derive the service name.
        String serviceName = new String("rmi://"+serverName+"/"+
                                        RMIImage.RMI_IMAGE_SERVER_NAME);

        // Look up the remote object.
        remoteImage = null;
        try {
            remoteImage = (RMIImage)Naming.lookup(serviceName);
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Get the default server name, that is, the local host.
     * When the provided server name is <code>null</code>, use
     * this method to obtain the local host IP address as the server name.
     */
    private String getLocalHostAddress() {
	String serverName;
        try {
            serverName = InetAddress.getLocalHost().getHostAddress();
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage());
        }
	return serverName;
    }
    /**
     * Get the unique ID to be used to refer to this object on the server.
     * The result is cached in the instance variable "id".
     */
    private void getRMIID() {
        try {
            id = remoteImage.getRemoteID();
        } catch(Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * Cache the argument and the RMI ID as local properties.
     * This is a gross hack to permit chaining of remote images.
     *
     * @param serverName The server name as described in the constructors.
     */
    private void setRMIProperties(String serverName) {
        setProperty(getClass().getName()+".serverName", serverName);
        setProperty(getClass().getName()+".id", id);
    }

    /**
     * Disposes of any resources allocated for remote operation.
     */
    protected void finalize() {
        try {
            remoteImage.dispose(id);
        } catch(Exception e) {
            // Ignore the Exception.
        }
    }

    /**
     * Set the amount of time between retries.
     *
     * @param timeout The time interval between retries (milliseconds). If
     * this is non-positive the time interval is not changed.
     */
    public void setTimeout(int timeout) {
        if(timeout > 0) {
            this.timeout = timeout;
        }
    }

    /**
     * Gets the amount of time between retries.
     */
    public int getTimeout() {
        return timeout;
    }

    /**
     * Set the number of retries.
     *
     * @param numRetries The number of retries. If this is non-positive the
     * number of retries is not changed.
     */
    public void setNumRetries(int numRetries) {
        if(numRetries > 0) {
            this.numRetries = numRetries;
        }
    }

    /**
     * Gets the number of retries.
     */
    public int getNumRetries() {
        return numRetries;
    }

    /**
     * Cause an instance variable of the remote object to be cached
     * locally, retrying a given number of times with a given timeout.
     *
     * @param fieldIndex the index of the desired field.
     * @param retries the maximum number of retries; must be positive.
     * @param timeout the timeout interval between retries, in milliseconds;
     *                must be positive.
     * @throws <code>ArrayIndexOutOfBoundsException</code> if fieldIndex
     * is negative or >= NUM_VARS.
     * @throws <code>IllegalArgumentException</code> if retries or timeout
     * is non-positive.
     */
    protected void requestField(int fieldIndex, int retries, int timeout) {
        if(retries < 0) {
            throw new IllegalArgumentException(JaiI18N.getString("RemoteImage3"));
        } else if(timeout < 0) {
            throw new IllegalArgumentException(JaiI18N.getString("RemoteImage4"));
        }

        int count = 0;

        if (fieldValid[fieldIndex])
            return;

        while (count++ < retries) {
            try {
                switch (fieldIndex) {
                case VAR_MIN_X:
                    minX = remoteImage.getMinX(id);
                    break;
                case VAR_MIN_Y:
                    minY = remoteImage.getMinY(id);
                    break;
                case VAR_WIDTH:
                    width = remoteImage.getWidth(id);
                    break;
                case VAR_HEIGHT:
                    height = remoteImage.getHeight(id);
                    break;
                case VAR_TILE_WIDTH:
                    tileWidth = remoteImage.getTileWidth(id);
                    break;
                case VAR_TILE_HEIGHT:
                    tileHeight = remoteImage.getTileHeight(id);
                    break;
                case VAR_TILE_GRID_X_OFFSET:
                    tileGridXOffset = remoteImage.getTileGridXOffset(id);
                    break;
                case VAR_TILE_GRID_Y_OFFSET:
                    tileGridYOffset = remoteImage.getTileGridYOffset(id);
                    break;
                case VAR_SAMPLE_MODEL:
                    sampleModel =
                        (SampleModel)remoteImage.getSampleModel(id).getSampleModel();
                    break;
                case VAR_COLOR_MODEL:
                    colorModel = (ColorModel)remoteImage.getColorModel(id).getColorModel();
                    break;
                case VAR_SOURCES:
                    {
                        Vector localSources = remoteImage.getSources(id);
                        int numSources = localSources.size();
                        for(int i = 0; i < numSources; i++) {
                            RenderedImage src =
                                (RenderedImage)localSources.get(i);
                            addSource(PlanarImage.wrapRenderedImage(src));
                        }
                    }
                    break;
                }

                fieldValid[fieldIndex] = true;
                return;
            } catch (RemoteException e) {
                System.err.println(JaiI18N.getString("RemoteImage0"));
                try {
                    java.lang.Thread.sleep(timeout);
                } catch (java.lang.InterruptedException f) {
                }
            }
        }
    }

    /**
     * Causes an instance variable of the remote object to be cached
     * locally, retrying with a default/user specified timeout.
     *
     * @param fieldIndex the index of the desired field.
     * @throws <code>ArrayIndexOutOfBoundsException</code> if fieldIndex
     * is negative or >= NUM_VARS.
     */
    protected void requestField(int fieldIndex) {
        requestField(fieldIndex, numRetries, timeout);
    }

    /**
     * Returns the X coordinate of the leftmost column of the image.
     */
    public int getMinX() {
        requestField(VAR_MIN_X);
        return minX;
    }

    /**
     * Returns the X coordinate of the column immediately to the right
     * of the rightmost column of the image.
     */
    public int getMaxX() {
        requestField(VAR_MIN_X);
        requestField(VAR_WIDTH);
	return minX + width;
    }

    /** Returns the Y coordinate of the uppermost row of the image. */
    public int getMinY() {
        requestField(VAR_MIN_Y);
        return minY;
    }

    /**
     * Returns the Y coordinate of the row immediately below the
     * bottom row of the image.
     */
    public int getMaxY() {
        requestField(VAR_MIN_Y);
        requestField(VAR_HEIGHT);
	return minY + height;
    }

    /** Returns the width of the <code>RemoteImage</code> in pixels. */
    public int getWidth() {
        requestField(VAR_WIDTH);
        return width;
    }

    /** Returns the height of the <code>RemoteImage</code> in pixels. */
    public int getHeight() {
        requestField(VAR_HEIGHT);
        return height;
    }

    /** Returns the width of a tile in pixels. */
    public int getTileWidth() {
        requestField(VAR_TILE_WIDTH);
        return tileWidth;
    }

    /** Returns the height of a tile in pixels. */
    public int getTileHeight() {
        requestField(VAR_TILE_HEIGHT);
        return tileHeight;
    }

    /** Returns the X offset of the tile grid. */
    public int getTileGridXOffset() {
        requestField(VAR_TILE_GRID_X_OFFSET);
        return tileGridXOffset;
    }

    /** Returns the Y offset of the tile grid. */
    public int getTileGridYOffset() {
        requestField(VAR_TILE_GRID_Y_OFFSET);
        return tileGridYOffset;
    }

    /** Returns the <code>SampleModel</code> associated with this image. */
    public SampleModel getSampleModel() {
        requestField(VAR_SAMPLE_MODEL);
        return sampleModel;
    }

    /** Returns the <code>ColorModel</code> associated with this image. */
    public ColorModel getColorModel() {
        requestField(VAR_COLOR_MODEL);
        return colorModel;
    }

    /**
     * Returns a vector of <code>RenderedImage</code>s that are the sources of
     * image data for this <code>RenderedImage</code>.  Note that this method
     * will often return <code>null</code>.
     */
    public Vector getSources() {
        requestField(VAR_SOURCES);
        return super.getSources();
    }

    /**
     * Gets a property from the property set of this image.
     * If the property name is not recognized, java.awt.Image.UndefinedProperty
     * will be returned.
     *
     * @param name the name of the property to get, as a String.
     * @return a reference to the property <code>Object</code>, or the value
     *         java.awt.Image.UndefinedProperty.
     *
     * @exception IllegalArgumentException if <code>propertyName</code>
     *                                     is <code>null</code>.
     */
    public Object getProperty(String name) {
        // Try to get property locally.
        Object property = super.getProperty(name);

        if (property == null || property == Image.UndefinedProperty) {
            // We have never requested this property, get it from the server.
            int count = 0;
            while (count++ < numRetries) {
                try {
                    property = remoteImage.getProperty(id, name);
                    if(NULL_PROPERTY_CLASS.isInstance(property)) {
                        property = Image.UndefinedProperty;
                    }
                    break;
                } catch (RemoteException e) {
                    try {
                        java.lang.Thread.sleep(timeout);
                    } catch (java.lang.InterruptedException f) {
                    }
                }
            }

            if (property == null) {
                property = Image.UndefinedProperty;
            }

            if(property != Image.UndefinedProperty) {
                setProperty(name, property); // Cache property locally
            }
        }

        return property;
    }

    /** Returns a list of names recognized by getProperty. */
    public String[] getPropertyNames() {
        // Retrieve local property names.
        String[] localPropertyNames = super.getPropertyNames();

        // Put local names in a Vector.
        Vector names = new Vector();
	if (localPropertyNames != null) {
	    for(int i = 0; i < localPropertyNames.length; i++) {
		names.add(localPropertyNames[i]);
	    }
	}

        // Get the remote property names.
        int count = 0;
        String[] remotePropertyNames = null;
        while (count++ < numRetries) {
            try {
                remotePropertyNames = remoteImage.getPropertyNames(id);
                break;
            } catch (RemoteException e) {
                try {
                    java.lang.Thread.sleep(timeout);
                } catch (java.lang.InterruptedException f) {
                }
            }
        }

        // Put the remote names, if any, in the Vector.
        if(remotePropertyNames != null) {
            for(int i = 0; i < remotePropertyNames.length; i++) {
                if(!names.contains(remotePropertyNames[i])) {
                    names.add(remotePropertyNames[i]);
                }
            }
        }

        // Set the return value from the vector.
        propertyNames = names.size() == 0 ?
            null : (String[])names.toArray(new String[names.size()]);

        return propertyNames;
    }

    /**
     * Returns tile (x, y).  Note that x and y are indexes into the
     * tile array not pixel locations.  The <code>Raster</code> that is returned
     * is a copy.
     *
     * @param x the X index of the requested tile in the tile array
     * @param y the Y index of the requested tile in the tile array
     */
    public Raster getTile(int x, int y) {
        int count = 0;

        while (count++ < numRetries) {
            try {
                RasterProxy rp = remoteImage.getTile(id, x, y);
                return rp.getRaster();
            } catch (RemoteException e) {
                try {
                    java.lang.Thread.sleep(timeout);
                } catch (java.lang.InterruptedException f) {
                }
            }
        }
        return null;
    }

    /**
     * Returns the image as one large tile.
     */
    public Raster getData() {
        int count = 0;

        while (count++ < numRetries) {
            try {
                RasterProxy rp = remoteImage.getData(id);
                return rp.getRaster();
            } catch (RemoteException e) {
                try {
                    java.lang.Thread.sleep(timeout);
                } catch (java.lang.InterruptedException f) {
                }
            }
        }
        return null;
    }

    /**
     * Returns an arbitrary rectangular region of the <code>RemoteImage</code>.
     *
     * <p> The <code>rect</code> parameter may be
     * <code>null</code>, in which case the entire image data is
     * returned in the <code>Raster</code>.
     *
     * <p> If <code>rect</code> is non-<code>null</code> but does
     * not intersect the image bounds at all, an
     * <code>IllegalArgumentException</code> will be thrown.
     *
     * @param rect  The <code>Rectangle</code> of interest.
     */

    public Raster getData(Rectangle rect) {
        if(imageBounds == null) {
            imageBounds = getBounds();
        }
        if (rect == null) {
            rect = imageBounds;
        } else if (!rect.intersects(imageBounds)) {
            throw new IllegalArgumentException(JaiI18N.getString("RemoteImage2"));
        }

        int count = 0;

        while (count++ < numRetries) {
            try {
                RasterProxy rp = remoteImage.getData(id, rect);
                return rp.getRaster();
            } catch (RemoteException e) {
                try {
                    java.lang.Thread.sleep(timeout);
                } catch (java.lang.InterruptedException f) {
                }
            }
        }
        return null;
    }

    /**
     * Returns an arbitrary rectangular region of the <code>RemoteImage</code>
     * in a user-supplied <code>WritableRaster</code>.
     * The rectangular region is the entire image if the argument is
     * <code>null</code> or the intersection of the argument bounds with the image
     * bounds if the region is non-<code>null</code>.
     * If the argument is non-<code>null</code> but has bounds which have an empty
     * intersection with the image bounds the return value will be <code>null</code>.
     * The return value may also be <code>null</code> if the argument is non-<code>null</code> but
     * is incompatible with the <code>Raster</code> returned from the remote image.
     */
    public WritableRaster copyData(WritableRaster raster) {
        int count = 0;

        Rectangle bounds = ((raster == null) ?
                            new Rectangle(getMinX(), getMinY(),
                                          getWidth(), getHeight()) :
                            raster.getBounds());

        while (count++ < numRetries) {
            try {
                RasterProxy rp = remoteImage.copyData(id, bounds);
                try {
                    if(raster == null) {
                        raster = (WritableRaster)rp.getRaster();
                    } else {
                        raster.setDataElements(bounds.x, bounds.y,
                                               (Raster)rp.getRaster());
                    }
                    break;
                } catch(ArrayIndexOutOfBoundsException e) {
                    raster = null;
                    break;
                }
            } catch (RemoteException e) {
                try {
                    java.lang.Thread.sleep(timeout);
                } catch (java.lang.InterruptedException f) {
                }
            }
        }

        return raster;
    }
}
